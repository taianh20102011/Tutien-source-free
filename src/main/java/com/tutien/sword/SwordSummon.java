package com.tutien.sword;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * SwordSummon v4 - Trieu hoi toi da 5 kiem bay, dung CO DINH SAU LUNG nguoi
 * choi (mui kiem huong thang len troi), chi bay ra danh roi bay ve khi can.
 *
 * Yeu cau: Kiem Tu + Ban Thanh (ordinal 10) tro len.
 *
 * So kiem theo canh gioi:
 *   Ban Thanh = 1 | Chuan Thanh = 2 | Dai Thanh = 3 | Thanh De = 4 | Tien Gioi+ = 5 (toi da)
 *
 * 3 CHE DO CHIEN DAU (doi bang "/trieukiem mode" hoac "/tk mode"):
 *   AUTO - tu dong bay danh bat ky quai nao lai gan (nhu truoc gio)
 *   SEMI - CHI bay danh khi ban vua tan cong 1 con vat, HOAC vua bi 1 con
 *          vat tan cong (trong vai giay sau do) - khong tu tim quai
 *   OFF  - tat han co che chien dau, kiem chi dung yen lam dep sau lung
 *
 * Co che tu the (moi kiem la 1 state machine rieng):
 *   REST       -> vi tri CO DINH sau lung nguoi choi, mui kiem thang dung
 *                 huong len troi. Chi doi cooldown + (tuy che do) co muc
 *                 tieu thi chuyen DASH_OUT. KHONG con xoay vong quanh nguoi
 *                 choi nhu truoc (do la nguyen nhan kiem "quay huong khac"
 *                 khi nguoi choi quay nguoi truoc day).
 *   DASH_OUT   -> bay thang toi vi tri quai trong ~5 tick (huong kiem theo
 *                 huong bay, khong con thang dung nua trong luc bay danh)
 *   IMPACT     -> dung lai 3 tick, gay sát thương + hieu ung chem trung
 *   DASH_BACK  -> bay nguoc ve dung vi tri sau lung hien tai (bam theo
 *                 nguoi choi di chuyen/quay huong), tro lai thang dung
 *   -> quay lai REST, cooldown rieng cho tung kiem (lech pha nhau)
 *
 *   Sát thương = atk_mult x (0.25 + num_swords x 0.1) x 4.0
 *   Kiếm nâng cấp chất liệu theo GIỚI: gỗ/đá/sắt/kim cương/huyền thiết
 *   /trieukiem hoặc /tk để toggle, /trieukiem mode để đổi chế độ chiến đấu
 */
public class SwordSummon implements Listener {

    private final TuTienPlugin plugin;
    private final Map<UUID, List<SwordUnit>> playerSwords = new HashMap<>();
    private static final int MAX_SWORDS = 5;

    private static final double SEARCH_RADIUS   = 8.0;
    private static final int DASH_OUT_TICKS     = 5;
    private static final int IMPACT_TICKS       = 3;
    private static final int DASH_BACK_TICKS    = 5;
    private static final int BASE_COOLDOWN_TICKS = 30; // 1.5s giữa 2 lần bay đánh của CÙNG 1 kiếm

    /** Che do chien dau - xem giai thich day du o dau file. */
    public enum CombatMode { AUTO, SEMI, OFF }
    private final Map<UUID, CombatMode> combatModes = new HashMap<>();
    // Con bao nhieu tick nua het "cua so combat" cho che do SEMI (dat lai
    // moi khi nguoi choi vua danh trung ai do HOAC vua bi ai do danh trung).
    private final Map<UUID, Integer> semiWindowTicks = new HashMap<>();
    private static final int SEMI_WINDOW_TICKS = 60; // 3 giay

    private enum State { REST, DASH_OUT, IMPACT, DASH_BACK }

    /** 1 kiếm đang bay + trạng thái tấn công riêng của nó. */
    private static class SwordUnit {
        final ArmorStand stand;
        final int slot;
        State state = State.REST;
        int stateTimer = 0;
        int cooldown = 0;
        UUID targetId = null;
        Location dashFrom = null;
        SwordUnit(ArmorStand stand, int slot) { this.stand = stand; this.slot = slot; }
    }

    public SwordSummon(TuTienPlugin plugin) {
        this.plugin = plugin;
        startMainLoop();
    }

    // ========================
    //  TOGGLE
    // ========================

    public void toggleSwords(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        if (data.getClassType() != ClassType.KIEM_TU) {
            player.sendMessage(plugin.prefix() + "§cChỉ §eKiếm Tú §cmới có thể triệu hồi kiếm bay!");
            return;
        }
        if (data.getStage().ordinal() < CultivationStage.BAN_THANH.ordinal()) {
            player.sendMessage(plugin.prefix() + "§cCần tu đến §fBán Thánh §cmới mở khóa triệu kiếm!");
            player.sendMessage(plugin.prefix() + "§7(Hiện tại: " + data.getStage().getFullDisplay(data.getRank()) + ")");
            return;
        }

        if (hasSwords(player)) {
            dismissSwords(player);
            player.sendMessage(plugin.prefix() + "§7Thu hồi kiếm bay.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.6f);
        } else {
            summonSwords(player, data);
        }
    }

    /** Doi che do chien dau: AUTO -> SEMI -> OFF -> AUTO. Goi tu lenh "/trieukiem mode". */
    public void cycleMode(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || data.getClassType() != ClassType.KIEM_TU) {
            player.sendMessage(plugin.prefix() + "§cChỉ §eKiếm Tu §cmới có thể chỉnh chế độ kiếm bay!");
            return;
        }
        UUID uid = player.getUniqueId();
        CombatMode cur = getMode(uid);
        CombatMode next = switch (cur) {
            case AUTO -> CombatMode.SEMI;
            case SEMI -> CombatMode.OFF;
            case OFF  -> CombatMode.AUTO;
        };
        combatModes.put(uid, next);
        String desc = switch (next) {
            case AUTO -> "§a§lAUTO §7- kiếm tự động bay đánh mọi quái ở gần";
            case SEMI -> "§e§lBÁN TỰ ĐỘNG §7- kiếm chỉ bay đánh khi bạn vừa tấn công hoặc vừa bị tấn công";
            case OFF  -> "§7§lTẮT §7- kiếm chỉ đứng yên làm đẹp, không tự chiến đấu";
        };
        player.sendMessage(plugin.prefix() + "§b⚔ Chế độ kiếm bay: " + desc);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private CombatMode getMode(UUID uid) {
        return combatModes.getOrDefault(uid, CombatMode.AUTO);
    }

    // ========================
    //  SUMMON
    // ========================

    private void summonSwords(Player player, PlayerData data) {
        int count = getSwordCount(data.getStage().ordinal());
        Material mat = getSwordMaterial(data.getStage());
        List<SwordUnit> swords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ArmorStand as = spawnSword(player, i, mat);
            if (as != null) {
                SwordUnit u = new SwordUnit(as, i);
                u.cooldown = i * 8; // lệch pha cooldown ban đầu để không bay đồng loạt
                swords.add(u);
            }
        }
        playerSwords.put(player.getUniqueId(), swords);

        player.sendMessage(plugin.prefix() + "§6⚔ Triệu hồi §e" + count + "§6/" + MAX_SWORDS + " kiếm " + matName(mat) + "!");
        player.sendTitle(plugin.msg("auto_8d1d21a72b"), "§7" + count + " kiếm bảo vệ ngươi!", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        spawnEffect(player, count);
    }

    /**
     * Kiếm nâng cấp theo GIỚI (không phải theo số lượng nữa):
     *   Linh Giới -> gỗ | Đế Giới -> đá | Thánh Giới -> sắt
     *   Tiên Giới -> kim cương | Thiên Giới -> netherite
     * Đạt càng cao GIỚI sẽ được đổi kiếm mới thay thế kiếm cũ tự động.
     */
    private Material getSwordMaterial(CultivationStage stage) {
        return switch (stage.getRealm()) {
            case "Linh Giới"  -> Material.WOODEN_SWORD;
            case "Đế Giới"    -> Material.STONE_SWORD;
            case "Thánh Giới" -> Material.IRON_SWORD;
            case "Tiên Giới"  -> Material.DIAMOND_SWORD;
            case "Thiên Giới" -> Material.NETHERITE_SWORD;
            default -> Material.WOODEN_SWORD;
        };
    }

    private String matName(Material mat) {
        return switch (mat) {
            case WOODEN_SWORD -> "§fGỗ";
            case STONE_SWORD -> "§7Đá";
            case IRON_SWORD -> "§fSắt";
            case DIAMOND_SWORD -> "§bKim Cương";
            case NETHERITE_SWORD -> "§8Huyền Thiết";
            default -> "";
        };
    }

    /**
     * Gọi sau khi đột phá thành công: nếu người chơi đang bật kiếm bay,
     * thu về và triệu lại ngay với số lượng + chất liệu kiếm mới theo cảnh giới mới.
     * Nếu chưa bật, không làm gì cả (sẽ áp dụng lần kế tiếp họ bật lên).
     */
    public void refreshIfActive(Player player) {
        if (!hasSwords(player)) return;
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || data.getClassType() != ClassType.KIEM_TU) return;
        dismissSwords(player);
        summonSwords(player, data);
        player.sendMessage(plugin.prefix() + "§6⚔ Kiếm bay đã được nâng cấp theo cảnh giới mới!");
    }

    private ArmorStand spawnSword(Player player, int idx, Material mat) {
        try {
            ArmorStand as = (ArmorStand) player.getWorld()
                    .spawnEntity(player.getLocation().clone().add(0,1.5,0), EntityType.ARMOR_STAND);
            as.setVisible(false); as.setGravity(false);
            as.setInvulnerable(true); as.setSilent(true);
            as.setSmall(true); as.setMarker(true);

            ItemStack sword = new ItemStack(mat);
            ItemMeta m = sword.getItemMeta();
            m.setDisplayName("§6⚔ Linh Kiếm " + (idx+1));
            m.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            m.getPersistentDataContainer().set(new NamespacedKey(plugin,"sword_owner"),
                    PersistentDataType.STRING, player.getUniqueId().toString());
            sword.setItemMeta(m);
            as.getEquipment().setItemInMainHand(sword);

            as.getPersistentDataContainer().set(new NamespacedKey(plugin,"tutien_sword"),
                    PersistentDataType.STRING, player.getUniqueId().toString());
            return as;
        } catch (Exception e) { return null; }
    }

    private void spawnEffect(Player player, int count) {
        World w = player.getWorld();
        for (int i = 0; i < 40; i++) {
            final int t = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                double a = Math.toRadians(t * 9);
                double r = 1.0 + t * 0.06;
                Location pos = player.getLocation().add(Math.cos(a)*r, 0.5+Math.sin(t*0.3)*0.5, Math.sin(a)*r);
                w.spawnParticle(Particle.ENCHANTED_HIT, pos, 2, 0.05,0.05,0.05,0.2);
                if (count >= 3) w.spawnParticle(Particle.CRIT, pos, 1, 0,0,0,0);
                if (count == 5) w.spawnParticle(Particle.DUST, pos, 1, 0,0,0,0,
                        new Particle.DustOptions(Color.fromRGB(255,200,30), 1.5f));
            }, t);
        }
    }

    // ========================
    //  DISMISS
    // ========================

    public void dismissSwords(Player player) {
        List<SwordUnit> list = playerSwords.remove(player.getUniqueId());
        if (list == null) return;
        for (SwordUnit u : list) {
            if (!u.stand.isDead()) {
                u.stand.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                        u.stand.getLocation().add(0,0.5,0), 10, 0.3,0.3,0.3,0.4);
                u.stand.remove();
            }
        }
    }

    public boolean hasSwords(Player player) {
        List<SwordUnit> l = playerSwords.get(player.getUniqueId());
        return l != null && !l.isEmpty();
    }

    public int getSwordCount(Player player) {
        List<SwordUnit> l = playerSwords.get(player.getUniqueId());
        return l == null ? 0 : (int) l.stream().filter(u -> !u.stand.isDead()).count();
    }

    // ========================
    //  MAIN LOOP (mỗi tick) - orbit + tự bay tới đánh
    // ========================

    private void startMainLoop() {
        new BukkitRunnable() {
            double tick = 0;
            @Override public void run() {
                tick += 0.07;
                // Dem nguoc "cua so combat" cho che do SEMI cua tat ca nguoi choi dang co kiem.
                semiWindowTicks.replaceAll((uid, v) -> Math.max(0, v - 1));
                for (Map.Entry<UUID,List<SwordUnit>> entry : new HashMap<>(playerSwords).entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) { playerSwords.remove(entry.getKey()); continue; }
                    List<SwordUnit> swords = entry.getValue();
                    swords.removeIf(u -> u.stand.isDead());
                    if (swords.isEmpty()) { playerSwords.remove(entry.getKey()); continue; }

                    PlayerData data = plugin.getDataManager().get(p);
                    int total = swords.size();

                    // Danh sách quái gần để gán cho kiếm đang rảnh (mỗi kiếm 1 quái khác nhau)
                    List<LivingEntity> availableTargets = null;

                    for (SwordUnit u : swords) {
                        Location restPos = formationPosition(p, tick, u.slot, total);

                        switch (u.state) {
                            case REST -> {
                                u.stand.teleport(restPos);
                                faceVertical(u.stand, p);
                                trailParticle(p, u.stand, tick, total);

                                if (u.cooldown > 0) { u.cooldown--; break; }
                                if (data == null) break;

                                CombatMode mode = getMode(p.getUniqueId());
                                if (mode == CombatMode.OFF) break; // Tat han - khong bao gio tu danh
                                if (mode == CombatMode.SEMI
                                        && semiWindowTicks.getOrDefault(p.getUniqueId(), 0) <= 0) {
                                    break; // Ban tu dong - chua co "cua so combat" nao dang mo
                                }

                                if (availableTargets == null) {
                                    availableTargets = new ArrayList<>();
                                    for (Entity e : p.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS * 0.7, SEARCH_RADIUS)) {
                                        // instanceof ArmorStand bi loai vi cac cay kiem trieu hoi (va item
                                        // display cua Phap Bao, pet...) CUNG LA LivingEntity trong Bukkit -
                                        // neu khong loai, kiem se tu chon "quai" la kiem khac trong doi hinh
                                        // cua chinh minh (hoac cua nguoi choi khac) de bay toi "danh". Ngoai
                                        // ra Linh Thu (pet) cua CHINH nguoi choi cung la LivingEntity that su
                                        // nhung duoc set invulnerable(true) - loai luon de kiem khong tu
                                        // "tan cong" pet cua chu.
                                        if (isValidHostileTarget(e)) {
                                            availableTargets.add((LivingEntity) e);
                                        }
                                    }
                                }
                                if (!availableTargets.isEmpty()) {
                                    LivingEntity target = (LivingEntity) availableTargets.remove(0);
                                    u.targetId = target.getUniqueId();
                                    u.dashFrom = restPos.clone();
                                    u.state = State.DASH_OUT;
                                    u.stateTimer = 0;
                                }
                            }
                            case DASH_OUT -> {
                                LivingEntity target = getTarget(p, u.targetId);
                                if (target == null) { u.state = State.DASH_BACK; u.stateTimer = 0; u.dashFrom = u.stand.getLocation(); break; }

                                u.stateTimer++;
                                double prog = Math.min(1.0, u.stateTimer / (double) DASH_OUT_TICKS);
                                Location strikePos = target.getLocation().add(0, target.getHeight()*0.6, 0);
                                Location cur = lerp(u.dashFrom, strikePos, prog);
                                u.stand.teleport(cur);
                                faceDirection(u.stand, u.dashFrom, strikePos);
                                p.getWorld().spawnParticle(Particle.CRIT, cur, 2, 0.05,0.05,0.05,0.02);

                                if (prog >= 1.0) {
                                    // Va chạm - gây sát thương
                                    int numSwords = total;
                                    double dmg = data != null
                                            ? 4.0 * data.calculateDamageMultiplier() * (0.25 + numSwords*0.1)
                                            : 2.0;
                                    target.damage(dmg, p);
                                    target.getWorld().spawnParticle(Particle.CRIT,
                                            target.getLocation().add(0,1,0), 10, 0.4,0.4,0.4,0.3);
                                    target.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                                            target.getLocation().add(0,1,0), 6, 0.3,0.3,0.3,0.2);
                                    p.getWorld().playSound(target.getLocation(),
                                            Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f + u.slot*0.1f);
                                    u.state = State.IMPACT;
                                    u.stateTimer = 0;
                                }
                            }
                            case IMPACT -> {
                                u.stateTimer++;
                                if (u.stateTimer >= IMPACT_TICKS) {
                                    u.dashFrom = u.stand.getLocation();
                                    u.state = State.DASH_BACK;
                                    u.stateTimer = 0;
                                }
                            }
                            case DASH_BACK -> {
                                u.stateTimer++;
                                double prog = Math.min(1.0, u.stateTimer / (double) DASH_BACK_TICKS);
                                Location cur = lerp(u.dashFrom, restPos, prog);
                                u.stand.teleport(cur);
                                faceDirection(u.stand, u.dashFrom, restPos);
                                p.getWorld().spawnParticle(Particle.ENCHANTED_HIT, cur, 1, 0.03,0.03,0.03,0.05);

                                if (prog >= 1.0) {
                                    u.state = State.REST;
                                    u.targetId = null;
                                    u.cooldown = BASE_COOLDOWN_TICKS + u.slot * 6;
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Vi tri CO DINH sau lung nguoi choi (khong con bay vong quanh nua).
     * Nhieu kiem duoc dan hang ngang sau lung, bam theo huong nhin cua
     * nguoi choi (quay nguoi thi vi tri sau lung cung quay theo, nhung
     * BAN THAN cay kiem khong tu xoay huong rieng).
     */
    private static final double BACK_DISTANCE = 0.9;
    private static final double HEIGHT_OFFSET = 0.95;

    private Location formationPosition(Player p, double tick, int slot, int total) {
        Vector facing = p.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.0001) facing = new Vector(0, 0, 1);
        facing.normalize();
        Vector back = facing.clone().multiply(-1);
        Vector side = new Vector(-back.getZ(), 0, back.getX()); // vuong goc, truc trai-phai

        double spacing = 0.36;
        double sideOffset = (slot - (total - 1) / 2.0) * spacing;
        // Dao dong rat nhe theo tung kiem de trong "song dong", khong lam mat
        // cam giac dung yen/co dinh sau lung.
        double bob = Math.sin(tick * 1.2 + slot) * 0.03;

        return p.getLocation().clone()
                .add(back.getX() * BACK_DISTANCE + side.getX() * sideOffset,
                     HEIGHT_OFFSET + bob,
                     back.getZ() * BACK_DISTANCE + side.getZ() * sideOffset);
    }

    private boolean isValidHostileTarget(Entity e) {
        if (e.isDead()) return false;
        if (e instanceof Player) return false;
        if (!(e instanceof LivingEntity le)) return false;
        if (e instanceof ArmorStand) return false;
        if (le.isInvulnerable()) return false;
        return true;
    }

    /**
     * Tu the DUNG YEN: mui kiem huong THANG LEN TROI (goc 90 do), khong
     * con xoay theo quy dao/huong nhin nua. Yaw van bam theo huong nguoi
     * choi de luoi kiem nhin tu nhien khi dung sau lung.
     */
    private void faceVertical(ArmorStand as, Player p) {
        double yaw = -Math.toRadians(p.getLocation().getYaw());
        as.setBodyPose(new EulerAngle(-Math.PI / 2, yaw, 0));
    }

    /** Hướng mũi kiếm theo hướng đang bay (từ -> đến), có tính cả pitch dọc (dy). */
    private void faceDirection(ArmorStand as, Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        // pitch: nghiêng lên/xuống theo dy (âm = mũi kiếm hướng lên, dương = hướng xuống)
        double pitch = -Math.atan2(dy, horizDist);
        // yaw: xoay trên mặt phẳng ngang
        double yaw = Math.atan2(dx, dz) + Math.PI;
        as.setBodyPose(new EulerAngle(pitch, yaw, 0));
    }

    private Location lerp(Location a, Location b, double t) {
        return new Location(a.getWorld(),
                a.getX() + (b.getX()-a.getX())*t,
                a.getY() + (b.getY()-a.getY())*t,
                a.getZ() + (b.getZ()-a.getZ())*t);
    }

    private LivingEntity getTarget(Player p, UUID id) {
        if (id == null) return null;
        Entity e = Bukkit.getEntity(id);
        if (e == null || e.isDead() || !(e instanceof LivingEntity le)) return null;
        if (le.getLocation().distanceSquared(p.getLocation()) > (SEARCH_RADIUS*2)*(SEARCH_RADIUS*2)) return null;
        return le;
    }

    private void trailParticle(Player p, ArmorStand as, double tick, int total) {
        if ((int)(tick*10) % 4 == 0) {
            p.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    as.getLocation().add(0,0.5,0), 1, 0.03,0.03,0.03,0.08);
        }
        if (total == MAX_SWORDS && (int)(tick*10) % 6 == 0) {
            p.getWorld().spawnParticle(Particle.DUST,
                    as.getLocation().add(0,0.5,0), 1, 0.1,0.1,0.1, 0,
                    new Particle.DustOptions(Color.fromRGB(255,200,0), 1.0f));
        }
    }

    // ========================
    //  HELPERS
    // ========================

    private int getSwordCount(int ord) {
        if (ord >= CultivationStage.CHAN_TIEN.ordinal()) return 5; // Tiên Giới+ = 5 (max)
        if (ord >= CultivationStage.THANH_DE.ordinal()) return 4;
        if (ord >= CultivationStage.DAI_THANH.ordinal()) return 3;
        if (ord >= CultivationStage.CHUAN_THANH.ordinal()) return 2;
        return 1; // Bán Thánh
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        dismissSwords(e.getPlayer());
        UUID uid = e.getPlayer().getUniqueId();
        combatModes.remove(uid);
        semiWindowTicks.remove(uid);
    }

    /**
     * Che do SEMI: mo "cua so combat" khi nguoi choi VUA tan cong 1 sinh vat,
     * HOAC VUA bi 1 sinh vat tan cong. Chi can 1 su kien nay la du cho ca 2
     * truong hop (damager la nguoi choi, hoac victim la nguoi choi).
     */
    @EventHandler(ignoreCancelled = true)
    public void onCombatTrigger(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player attacker && hasSwords(attacker)
                && getMode(attacker.getUniqueId()) == CombatMode.SEMI) {
            semiWindowTicks.put(attacker.getUniqueId(), SEMI_WINDOW_TICKS);
        }
        if (e.getEntity() instanceof Player victim && hasSwords(victim)
                && getMode(victim.getUniqueId()) == CombatMode.SEMI) {
            semiWindowTicks.put(victim.getUniqueId(), SEMI_WINDOW_TICKS);
        }
    }
}
