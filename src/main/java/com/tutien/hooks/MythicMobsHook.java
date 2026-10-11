package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * MythicMobsHook — Tích hợp TuTiên với MythicMobs.
 *
 * TÍNH NĂNG:
 * ─────────────────────────────────────────────────────────
 * 1. BOSS ARENA: Khi đột phá tầng siêu cấp, Boss Arena dùng MythicMob
 *    thay vì vanilla mob — đẹp hơn, mạnh hơn, có skill riêng.
 *
 *    Mapping cấp độ → tên MythicMob (định nghĩa trong mobs/*.yml):
 *      Lv 1-3   → TT_KiepNan_WitherSkeleton
 *      Lv 4-6   → TT_KiepNan_Wither
 *      Lv 7-9   → TT_VeThan_Warden
 *      Lv 10-12 → TT_SongVeThan
 *      Lv 13-15 → TT_UltraVeThan
 *      Lv 16-18 → TT_TuyetDinh_Boss
 *      Lv 19-24 → TT_Omega_DualBoss
 *      Lv 25-30 → TT_Omega_Boss
 *      Lv 31-36 → TT_SongOmega
 *      Lv 37-42 → TT_TamOmega
 *      Lv 43-48 → TT_TuTuyetDinh
 *
 * 2. REALM AURA: Mỗi cảnh giới lớn có Aura Skill MythicMobs riêng
 *    — hiệu ứng particle + buff phát ra liên tục trong lúc chơi.
 *    Lệnh: /skill cast <aura_skill_name> on <player>
 *
 * 3. LINH CĂN / TỘC EFFECT: Một số linh căn/tộc đặc biệt sẽ cast
 *    MythicMobs skill tương ứng (vd: Long Tộc kích hoạt dragon_aura).
 *
 * 4. SKILL DAMAGE SCALING: Khi TuTiên skill gây damage, nhân thêm
 *    PHAP_CONG stat từ MythicLib (nếu có).
 *
 * ─────────────────────────────────────────────────────────
 * TẤT CẢ dùng REFLECTION — không crash nếu MythicMobs không cài.
 */
public class MythicMobsHook implements Listener {

    private final TuTienPlugin plugin;
    private boolean available = false;

    // MythicMobs boss name prefix (định nghĩa trong Mobs/*.yml)
    private static final String PREFIX = "TT_";

    // Active MythicMobs bosses spawned by TuTien (uuid → bossLevel)
    private final Map<UUID, Integer> mmBosses = new HashMap<>();
    // Player → active aura skill name
    private final Map<UUID, String>  playerAuras = new HashMap<>();

    // Cached reflection refs
    private Object mmAPI; // MythicBukkit.inst()
    private Object mmApiHelperInstance; // cached BukkitAPIHelper instance (dung chung cho spawn + skill)
    private Method mmSpawnMob;       // BukkitAPIHelper.spawnMythicMob(name, loc, level)
    private Method mmGetEntityType;  // MythicEntity#getEntity
    private Method mmCastSkill;      // SkillTriggers.castSkill(entity, skillName)
    private Method mmIsMMEntity;     // MythicBukkit#isMMEntity(entity)

    public MythicMobsHook(TuTienPlugin plugin) {
        this.plugin = plugin;
        tryLoad();
    }

    // ================================================================
    //  INIT
    // ================================================================
    private void tryLoad() {
        try {
            if (!plugin.getConfig().getBoolean("mythicmobs.enabled", true)) {
                plugin.getLogger().info("[Hook] MythicMobs hook bị tắt trong config.yml.");
                return;
            }
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                plugin.getLogger().info("[Hook] MythicMobs không tìm thấy — dùng vanilla boss.");
                return;
            }
            // io.lumine.mythic.bukkit.MythicBukkit (MM5)
            Class<?> mmClass;
            try {
                mmClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            } catch (ClassNotFoundException e2) {
                mmClass = Class.forName("net.elseland.xikage.MythicMobs.API.IMythicMobsAPI");
            }
            mmAPI = mmClass.getMethod("inst").invoke(null);

            // ── Dùng BukkitAPIHelper (PUBLIC API, ổn định qua các bản) ──
            // KHÔNG reflect vào io.lumine.mythic.core.* (class nội bộ, đổi
            // signature liên tục giữa các snapshot -> gây lỗi "cannot find symbol"
            // hoặc NoSuchMethodException khi MythicMobs update).
            Class<?> apiHelperClass;
            try {
                apiHelperClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            } catch (ClassNotFoundException e2) {
                apiHelperClass = Class.forName("io.lumine.mythic.bukkit.adapters.BukkitAPIHelper");
            }
            mmApiHelperInstance = apiHelperClass.getConstructor().newInstance();
            // Test xem method spawnMythicMob(String, Location, int) co ton tai khong
            mmSpawnMob = apiHelperClass.getMethod("spawnMythicMob", String.class, Location.class, int.class);

            plugin.getLogger().info(
                    "[Hook] ✅ MythicMobs kết nối thành công — Boss Arena & Aura Effects đã sẵn sàng!");
            available = true;

            // Listen for MythicMobs death events via Bukkit event bus
            Bukkit.getPluginManager().registerEvents(this, plugin);

        } catch (Exception e) {
            plugin.getLogger().warning("[Hook] MythicMobs hook thất bại: " + e.getMessage()
                    + " — Boss Arena sẽ dùng vanilla mobs.");
            available = false;
        }
    }

    public boolean isAvailable() { return available; }

    // ================================================================
    //  SPAWN MYTHICMOBS BOSS (thay cho vanilla boss trong TribulationBossArena)
    // ================================================================

    /**
     * Spawn MythicMob boss tại location, trả về entity UUID (null nếu thất bại).
     * @param loc        Vị trí spawn
     * @param bossLevel  Cấp độ boss (1-48)
     * @param stageName  Tên cảnh giới (dùng để đặt tên hiển thị)
     */
    public UUID spawnBoss(Location loc, int bossLevel, String stageName) {
        if (!available) return null;
        try {
            String mobName = getBossName(bossLevel);
            int    level   = Math.round(getBossLevel(bossLevel));

            if (!plugin.getConfig().getBoolean("mythicmobs.use-mm-boss", true)) return null;
            // MythicMobs 5 public API normally returns a Bukkit Entity directly.
            // Older builds/wrappers may instead return ActiveMob -> AbstractEntity -> Bukkit Entity.
            Object spawnResult = mmSpawnMob.invoke(mmApiHelperInstance, mobName, loc, level);
            LivingEntity le = unwrapLivingEntity(spawnResult);

            if (le != null) {
                UUID uuid = le.getUniqueId();
                mmBosses.put(uuid, bossLevel);
                // Override display name with stage info
                le.setCustomName("§c§l" + getTierPrefix(bossLevel)
                        + " §e[" + stageName + "]");
                le.setCustomNameVisible(true);
                return uuid;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[MythicMobsHook] spawnBoss lỗi: " + e.getMessage());
        }
        return null;
    }

    /** Kiểm tra UUID có phải TuTiên MM boss không */
    public boolean isTuTienBoss(UUID uuid) { return mmBosses.containsKey(uuid); }

    /** Remove tracking khi boss chết */
    public void removeBoss(UUID uuid) { mmBosses.remove(uuid); }

    // ================================================================
    //  SPAWN MYTHICMOBS BAT KY (dung cho Linh Thu/pet — KHONG dang ky
    //  vao danh sach boss, khong override ten). Cho phep bat cu he thong
    //  nao khac trong plugin (PetSummon, sau nay co the la mount, mob
    //  trang tri...) trieu hoi 1 MythicMob theo ID lay tu config.yml,
    //  hoan toan KHONG hardcode ten mob trong Java.
    // ================================================================

    /**
     * Spawn 1 MythicMob theo ID bat ky, tra ve LivingEntity de noi goi tu
     * xu ly tiep (invulnerable, gan chu so huu, doi ten hien thi...).
     * Tra ve null neu MythicMobs khong cai, mobId rong, hoac spawn that bai
     * (vd sai ten mob trong Mobs/*.yml) — noi goi nen fallback ve vanilla.
     */
    public LivingEntity spawnGenericMob(String mobId, Location loc, int level) {
        if (!available || mobId == null || mobId.isBlank()) return null;
        try {
            Object spawnResult = mmSpawnMob.invoke(mmApiHelperInstance, mobId, loc, Math.max(1, level));
            return unwrapLivingEntity(spawnResult);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[MythicMobsHook] spawnGenericMob('" + mobId + "') lỗi: " + e.getMessage());
            return null;
        }
    }

    /** Cast 1 skill MythicMobs lên entity bat ky (dung cho pet). */
    public boolean castSkillOnEntity(LivingEntity entity, String skillName) {
        if (!available || entity == null || skillName == null || skillName.isBlank()) return false;
        try {
            return castSkillOnEntityInternal(entity, skillName);
        } catch (Exception e) {
            plugin.getLogger().fine("[MythicMobsHook] cast pet skill loi: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    //  REALM AURA — cast MythicMob skill liên tục cho người chơi
    // ================================================================

    /**
     * Bật aura MythicMobs cho người chơi theo cảnh giới.
     * Aura định nghĩa trong Skills/TT_Auras.yml
     */
    public void applyRealmAura(Player player, CultivationStage stage) {
        if (!available || !plugin.getConfig().getBoolean("mythicmobs.use-mm-aura", true) || player == null || stage == null) return;
        String auraSkill = getAuraSkillName(stage);
        String current = playerAuras.get(player.getUniqueId());
        if (auraSkill.equals(current)) return; // Đã đang dùng aura này rồi
        // Remove old aura
        if (current != null) removeAura(player);
        try {
            if (castSkillOnPlayer(player, auraSkill)) playerAuras.put(player.getUniqueId(), auraSkill);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[MythicMobsHook] applyAura lỗi: " + e.getMessage());
        }
    }

    public void removeAura(Player player) {
        playerAuras.remove(player.getUniqueId());
        // Không có API công khai để "tắt" skill effect — timeout tự hết
    }

    // ================================================================
    //  LINH CĂN / TỘC SPECIAL EFFECT
    // ================================================================

    /**
     * Gọi khi player thay đổi tộc/linh căn hoặc đột phá thành công.
     * Cast skill visual effect tương ứng.
     */
    public void castRaceEffect(Player player, String tocName) {
        if (!available || !plugin.getConfig().getBoolean("mythicmobs.use-mm-race-effect", true) || player == null || tocName == null) return;
        String skill = switch (tocName) {
            case "LONG_TOC"          -> "TT_Effect_DragonAura";
            case "PHUONG_HOANG_TOC"  -> "TT_Effect_PhoenixFlame";
            case "THUONG_THAN_TOC"   -> "TT_Effect_ThunderSurge";
            case "HON_HIEP_TOC"      -> "TT_Effect_ShadowPulse";
            case "KIM_CUONG_TOC"     -> "TT_Effect_DiamondShield";
            case "DAO_TOC"           -> "TT_Effect_DaoHalo";
            case "THUY_TINH_TOC"     -> "TT_Effect_CrystalBurst";
            default -> null;
        };
        if (skill == null) return;
        try { castSkillOnPlayer(player, skill); }
        catch (Exception ignored) {}
    }

    public void castLinhCanEffect(Player player, String linhCanName) {
        if (!available || !plugin.getConfig().getBoolean("mythicmobs.use-mm-race-effect", true) || player == null || linhCanName == null) return;
        String skill = switch (linhCanName) {
            case "HON"           -> "TT_LC_HonDon";
            case "HUYET"         -> "TT_LC_HuyetMach";
            case "CO_HO"         -> "TT_LC_CoHo";
            case "THIEN_NHAT"    -> "TT_LC_ThienNhat";
            case "NGUYEN_SHI"    -> "TT_LC_NguyenShi";
            case "VO_THUONG_CAN" -> "TT_LC_VoThuong";
            default              -> null;
        };
        if (skill == null) return;
        try { castSkillOnPlayer(player, skill); }
        catch (Exception ignored) {}
    }

    // ================================================================
    //  MYTHICMOBS DEATH EVENT (via Bukkit event bus)
    // ================================================================
    /**
     * Lắng nghe MythicMobs death event để xử lý boss TuTiên chết.
     * MythicMobs 5.x: io.lumine.mythic.bukkit.events.MythicMobDeathEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMythicDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // Chỉ xử lý entity là TuTiên MM boss
        UUID uuid = event.getEntity().getUniqueId();
        if (!mmBosses.containsKey(uuid)) return;
        // Clear drops — TuTiên tự xử lý rewards
        event.getDrops().clear();
        event.setDroppedExp(0);
        mmBosses.remove(uuid);
        // TribulationBossArena.onBossDeath sẽ được trigger bởi EntityDeathEvent thông thường
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private LivingEntity unwrapLivingEntity(Object result) throws Exception {
        if (result instanceof LivingEntity living) return living;
        if (result == null) return null;

        // Compatibility with older API variants that return ActiveMob wrappers.
        Object entity = result;
        try {
            Method getEntity = result.getClass().getMethod("getEntity");
            entity = getEntity.invoke(result);
        } catch (NoSuchMethodException ignored) {
            // Modern API returns the Bukkit entity directly.
        }
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof org.bukkit.entity.Entity bukkitEntity) {
            return bukkitEntity instanceof LivingEntity living ? living : null;
        }
        try {
            Method getBukkitEntity = entity.getClass().getMethod("getBukkitEntity");
            Object bukkitEntity = getBukkitEntity.invoke(entity);
            return bukkitEntity instanceof LivingEntity living ? living : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private boolean castSkillOnEntityInternal(LivingEntity entity, String skillName) throws Exception {
        // Prefer the public API's castSkill(Entity, String) overload.
        for (Method method : mmApiHelperInstance.getClass().getMethods()) {
            if (!method.getName().equals("castSkill")) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[0].isAssignableFrom(entity.getClass()) && params[1] == String.class) {
                Object result = method.invoke(mmApiHelperInstance, entity, skillName);
                return !(result instanceof Boolean value) || value;
            }
        }
        // Compatibility with APIs exposing castSkill(Entity, String, Location).
        for (Method method : mmApiHelperInstance.getClass().getMethods()) {
            if (!method.getName().equals("castSkill")) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 3 && params[0].isAssignableFrom(entity.getClass()) && params[1] == String.class) {
                Object third;
                if (params[2].isAssignableFrom(org.bukkit.Location.class)) third = entity.getLocation();
                else if (params[2].isAssignableFrom(entity.getClass())) third = entity;
                else continue;
                Object result = method.invoke(mmApiHelperInstance, entity, skillName, third);
                return !(result instanceof Boolean value) || value;
            }
        }
        throw new NoSuchMethodException("MythicMobs BukkitAPIHelper.castSkill(Entity, String) không tồn tại");
    }

    private boolean castSkillOnPlayer(Player player, String skillName) throws Exception {
        return castSkillOnEntityInternal(player, skillName);
    }

    /** Lấy tên MythicMob theo cấp độ boss */
    public String getBossName(int bossLevel) {
        if (bossLevel <= 3)  return PREFIX + "KiepNan_WitherSkeleton";
        if (bossLevel <= 6)  return PREFIX + "KiepNan_Wither";
        if (bossLevel <= 9)  return PREFIX + "VeThan_Warden";
        if (bossLevel <= 12) return PREFIX + "SongVeThan";
        if (bossLevel <= 15) return PREFIX + "UltraVeThan";
        if (bossLevel <= 18) return PREFIX + "TuyetDinh_Boss";
        if (bossLevel <= 24) return PREFIX + "Omega_DualBoss";
        if (bossLevel <= 30) return PREFIX + "Omega_Boss";
        if (bossLevel <= 36) return PREFIX + "SongOmega";
        if (bossLevel <= 42) return PREFIX + "TamOmega";
        return PREFIX + "TuTuyetDinh";
    }

    /** Chuyển bossLevel → MythicMobs level (scale 1-10) */
    private float getBossLevel(int bossLevel) {
        return 1.0f + (bossLevel - 1) * (9.0f / 47.0f);
    }

    private String getTierPrefix(int level) {
        if (level >= 43) return "✦✦✦✦ TỨ TUYỆT ĐỈNH OMEGA";
        if (level >= 37) return "✦✦✦ TAM OMEGA THẦN";
        if (level >= 31) return "✦✦ SONG OMEGA";
        if (level >= 25) return "✦ OMEGA THẦN";
        if (level >= 19) return "☠ SONG TUYỆT ĐỈNH";
        if (level >= 16) return "☠ TUYỆT ĐỈNH VÔ THƯỢNG";
        if (level >= 13) return "⚡ KIẾP VỆ THẦN";
        if (level >= 10) return "⚡ SONG VỆ THẦN";
        if (level >= 7)  return "⚡ VỆ THẦN";
        if (level >= 4)  return "☁ THIÊN KIẾP";
        return "☁ KIẾP NẠN";
    }

    /** Tên aura skill MythicMobs theo cảnh giới */
    private String getAuraSkillName(CultivationStage stage) {
        int ord = stage.ordinal();
        if (ord == 52) return "TT_Aura_SieuCap";       // Vo Thuong Chi Ton - canh gioi cuoi cung
        return switch (stage.getRealm()) {
            case "Nhân Giới"     -> "TT_Aura_Nhan";
            case "Linh Giới"     -> "TT_Aura_Linh";
            case "Đế Giới"       -> "TT_Aura_De";
            case "Thánh Giới"    -> "TT_Aura_Thanh";
            case "Tiên Giới"     -> "TT_Aura_Tien";
            case "Thiên Giới",
                 "Thiên Đế"      -> "TT_Aura_Thien";
            case "Thiên Tiên Đế" -> "TT_Aura_ThienTienDe";
            default              -> "TT_Aura_Base";
        };
    }
}