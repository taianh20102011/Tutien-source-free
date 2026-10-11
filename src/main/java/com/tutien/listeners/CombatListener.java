package com.tutien.listeners;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.DaoPath;
import com.tutien.data.PlayerData;
import com.tutien.phabao.PhaBaoManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CombatListener implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    public CombatListener(TuTienPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {

        // Firework TRANG TRI (do EffectEngine tu spawn cho hieu ung dot pha/
        // pha bao/boss...) khong duoc GAY SAT THUONG - vanilla mac dinh
        // firework no GAY DAME cho nguoi dung gan, ke ca khong lien quan
        // PvP, day chi la hieu ung hinh anh. Firework do NGUOI CHOI tu ban
        // (vd combo no cung/elytra) KHONG bi anh huong vi khong co the danh
        // dau nay.
        if (event.getDamager() instanceof org.bukkit.entity.Firework firework
                && firework.getPersistentDataContainer().has(
                        com.tutien.effects.EffectEngine.COSMETIC_FIREWORK_KEY,
                        org.bukkit.persistence.PersistentDataType.BYTE)) {
            event.setCancelled(true);
            return;
        }

        // === ATTACKER ===
        if (event.getDamager() instanceof Player attacker) {
            PlayerData atkData = plugin.getDataManager().get(attacker);
            if (atkData != null && atkData.hasClass()) {
                double mult = atkData.calculateDamageMultiplier();
                if (atkData.hasBuff("stealth_bonus")) {
                    mult *= 2.0;
                    atkData.removeBuff("stealth_bonus");
                    atkData.setInvisible(false);
                    attacker.sendMessage(plugin.prefix() + "§5Tan cong tang hinh §dx2 sát thương!");
                }
                event.setDamage(event.getDamage() * mult);
                // Phabao passive ATK
                double pbAtk = plugin.getPhaBaoManager().getPassiveAtkMult(attacker);
                if (pbAtk > 1.0) event.setDamage(event.getDamage() * pbAtk);
            }
        }

        // === DEFENDER ===
        if (event.getEntity() instanceof Player defender) {
            PlayerData defData = plugin.getDataManager().get(defender);
            if (defData == null) return;

            // Phương Hoàng Trọng Sinh: neo HP khi sap chet
            if (defData.hasBuff("phuonghoang_rebirth")) {
                double newDmg = event.getDamage();
                if (defender.getHealth() - newDmg <= 0) {
                    event.setDamage(0);
                    defData.removeBuff("phuonghoang_rebirth");
                    // Hoi phuc 80% HP
                    double healHp = defender.getMaxHealth() * 0.8;
                    defender.setHealth(Math.min(healHp, defender.getMaxHealth()));
                    // Dot ke thu xung quanh
                    for (Entity e : defender.getNearbyEntities(5, 3, 5)) {
                        if (e instanceof LivingEntity le && e != defender) {
                            le.setFireTicks(120);
                            le.damage(healHp * 0.5, defender);
                        }
                    }
                    defender.getWorld().spawnParticle(Particle.FLAME,
                        defender.getLocation().add(0,1,0), 80, 1, 0.5, 1, 0.15);
                    defender.getWorld().playSound(defender.getLocation(),
                        Sound.ENTITY_ENDER_DRAGON_FLAP, 2f, 1.5f);
                    defender.sendTitle(plugin.msg("auto_86327eb073"), plugin.msg("auto_e22a14b619"), 10, 50, 20);
                    defender.sendMessage(plugin.prefix() + "§6🔥 Phượng Hoàng cuu song! HP: " + (int)healHp);
                    // Thong bao server
                    String msg = plugin.prefix() + "§e" + defender.getName() + " §6Phượng Hoàng Trọng Sinh!";
                    org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
                    return;
                }
            }

            // Phabao shield passive DEF
            double pbDef = plugin.getPhaBaoManager().getPassiveDefMult(defender);
            if (pbDef > 1.0) event.setDamage(event.getDamage() / pbDef);

            // Phabao Ho The active shield
            if (defData.hasBuff("phabao_shield")) {
                event.setDamage(event.getDamage() * 0.5);
            }

            // ─── TRẤN THIÊN GIÁP: chỉ cần trong túi → giảm 50% damage ───
            if (hasTranThienGiapInInventory(defender)) {
                event.setDamage(event.getDamage() * 0.5);
                // Hiệu ứng nhỏ mỗi 5 giây để biết đang hoạt động
                if (!defData.hasBuff("ttg_particle_cd")) {
                    defData.addBuff("ttg_particle_cd", 5_000L);
                    defender.getWorld().spawnParticle(
                        org.bukkit.Particle.ENCHANTED_HIT,
                        defender.getLocation().add(0,1,0), 8, 0.5, 0.5, 0.5, 0.1);
                }
            }

            // ─── LỌ VƯƠNG: Bất Tử Lọ Pháp (HP < 20% → bất tử 3 giây, CD 5 phút) ───
            if (defData.getDaoPath() == DaoPath.LO_VUONG) {
                double newHp = defender.getHealth() - event.getFinalDamage();
                if (newHp <= 0 && !defData.hasBuff("lo_batu_cooldown")) {
                    // Chui vào lọ — ngăn chết
                    event.setDamage(0);
                    defData.addBuff("lo_batu_active",   3_000L);    // bất tử 3 giây = 3000ms
                    defData.addBuff("lo_batu_cooldown", 300_000L);  // CD 5 phút = 300000ms
                    defender.setHealth(Math.max(1.0, defender.getMaxHealth() * 0.05));
                    defender.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        defender.getLocation().add(0,1,0), 25, 1, 0.5, 1, 0.08);
                    defender.getWorld().spawnParticle(Particle.PORTAL,
                        defender.getLocation().add(0,1,0), 20, 1, 1, 1, 0.2);
                    defender.getWorld().playSound(defender.getLocation(),
                        Sound.BLOCK_BARREL_CLOSE, 1.5f, 0.4f);
                    defender.sendTitle(plugin.msg("auto_7fa577b34a"), plugin.msg("auto_f7d5eaac84"), 5, 40, 15);
                    defender.sendMessage(plugin.prefix() + "§5🏺 Lọ Vương kích hoạt! Bất tử 3s (CD 5 phút)");
                    if (plugin.hasMythicMobs()) {
                        try { plugin.getMythicMobsHook().castRaceEffect(defender, "LO_VUONG_BATU"); }
                        catch (Exception ignored) {}
                    }
                }
                // Nếu đang trong bất tử → miễn damage hoàn toàn
                if (defData.hasBuff("lo_batu_active")) {
                    event.setDamage(0);
                    return;
                }
                // 10% chance phản đòn 30% damage
                if (event.getDamager() instanceof LivingEntity attLE
                        && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.10) {
                    double reflect = event.getFinalDamage() * 0.3;
                    attLE.damage(reflect);  // Paper 1.21: chỉ truyền damage amount
                    defender.getWorld().spawnParticle(Particle.WITCH,
                        attLE.getLocation().add(0,1,0), 10, 0.5, 0.5, 0.5, 0);
                    defender.sendMessage(plugin.prefix() + "§5🏺 Lọ Vương phản đòn §e"
                        + String.format("%.0f", reflect) + "§5 damage!");
                }
            }

            // ─── SĨ VƯƠNG: Sách Đỡ Đòn (giảm 25% damage khi đang thiền) ───
            if (defData.getDaoPath() == DaoPath.SI_VUONG && defData.hasBuff("sivuong_thiendinh")) {
                event.setDamage(event.getDamage() * 0.75);
            }

            // Kim Cuong Bat Hoai
            if (defData.hasBuff("kimcuong_invincible")) {
                event.setDamage(0);
                return;
            }

            // Dodge (Phong Tu)
            if (defData.hasDodgeBuff() && defData.hasBuff("dodge") && random.nextDouble() < 0.70) {
                event.setCancelled(true);
                defender.getWorld().spawnParticle(Particle.CLOUD,
                    defender.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                defender.playSound(defender.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 2f);
                defender.sendMessage(plugin.prefix() + "§b💨 Né đòn!");
                return;
            }

            if (defData.hasClass()) {
                event.setDamage(event.getDamage() / defData.calculateDefenseMultiplier());
            }

            // Phan Don
            if (defData.hasCounterBuff() && defData.hasBuff("phandon")
                    && event.getDamager() instanceof Player atk) {
                double counterDmg = event.getDamage() * 2.0;
                atk.damage(counterDmg, defender);
                defData.setCounterBuff(false);
                defData.removeBuff("phandon");
                defender.getWorld().spawnParticle(Particle.CRIT,
                    defender.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
                defender.sendMessage(plugin.prefix() + "§7⚡ Phản Đòn §c" + (int)counterDmg + " §7sát thương!");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        PlayerData data = plugin.getDataManager().get(killer);
        if (data == null) return;

        LivingEntity dead = event.getEntity();
        // SUA LOI CAN BANG GAME: TRUOC DAY chi can dat.customName() != null
        // la duoc tinh la "boss" (thuong x10 linh khi + bossKills). Bat ky
        // nguoi choi nao cung co the dat ten mot con quai THUONG (vi du con
        // ga) bang bien danh (name tag) trong de => quai ten do khi giet se
        // duoc tinh la boss, tao dupe/cay linh khi + boss-kill gia mao vo
        // han. Them dieu kien mau toi thieu: quai that (TribulationBossArena
        // >= 400hp, Huyet Cot Ma Vuong 250-500hp) luon cao hon RAT NHIEU so
        // voi mau mac dinh cua bat ky mob vanilla nao (Iron Golem/Ravager
        // toi da ~100hp) - nen dat nguong 150 la an toan, khong anh huong
        // toi cac boss that, nhung chan dung viec dat ten quai thuong.
        double bossMinHealth = plugin.getConfig().getDouble("cultivation.boss-min-health", 150.0);
        boolean isNamedRealBoss = dead.customName() != null && dead.getMaxHealth() >= bossMinHealth;
        boolean isBoss = isNamedRealBoss
            || dead.getType().name().contains("WITHER")
            || dead.getType().name().contains("ENDER_DRAGON")
            || dead.getType().name().contains("ELDER_GUARDIAN");

        long baseGain;
        if (dead instanceof Player) {
            baseGain = plugin.getConfig().getLong("cultivation.linh-khi-per-player-kill", 500);
            data.addKill();
            data.addPvpKill(); // rieng cho leaderboard "kill nguoi choi khac"
        } else {
            baseGain = plugin.getConfig().getLong("cultivation.linh-khi-per-mob", 50);
            if (isBoss) {
                baseGain *= 10;
                data.addBossKill();
                // Boss Toi Thuong: KHONG con tinh o day nua. Truoc day dung
                // dieu kien "ten chua Toi Thuong" (de bi gia mao bang cach
                // dat ten quai thuong). Gio da chuyen sang WorldBossManager,
                // dung UUID cua chinh con boss no tu spawn ra (khong the gia
                // mao) va chia cong theo % sat thuong cho TAT CA nguoi tham
                // gia, khong chi rieng nguoi ra don cuoi nhu o day.
            }
            else data.addKill();
        }

        long gain = scaleLK(dead, baseGain);
        // Ma Dao nhan them 20% LK - xu ly trong addLinhKhi
        // Uy ap reduction
        double uyapMult = plugin.getUyApManager() != null
            ? plugin.getUyApManager().getLKMultiplier(killer) : 1.0;
        // Zone bonus
        double zoneMult = plugin.getZoneManager() != null
            ? plugin.getZoneManager().getLKMultiplier(killer.getLocation()) : 1.0;
        data.addLinhKhi((long)(gain * uyapMult * zoneMult));

        // === THUONG TIEN (100% khi giet quai) ===
        long tienAmount = plugin.getTienManager().calculateMobDrop(dead, data);
        if (tienAmount > 0) {
            plugin.getTienManager().give(killer, tienAmount);
        }

        // === DROP PHAP BAO (5% chance) - CAP THANG VAO TUI DO ===
        if (random.nextInt(20) == 0) {
            var pb = plugin.getPhaBaoManager().randomPhaBao(data.getStage().ordinal());
            // Cap thang vao inventory thay vi drop xuong dat (tranh bi nguoi khac lay)
            var leftover = killer.getInventory().addItem(pb);
            if (leftover.isEmpty()) {
                killer.sendMessage(plugin.prefix() + "§6◉ Pháp Bảo rơi vào túi đồ!");
            } else {
                // Inventory day -> drop xuong dat nhung thong bao ro rang
                dead.getWorld().dropItemNaturally(dead.getLocation(), pb);
                killer.sendMessage(plugin.prefix() + "§6◉ Pháp Bảo rơi ra đất (túi đồ đầy)! Nhanh lấy lên!");
            }
        }

        // Update info item
        if (killer.isOnline()) plugin.getSkillItemManager().updateInfoItem(killer, data);

        // Thong bao
        long max = data.getMaxLinhKhi();
        long cur = data.getLinhKhi();
        double pct = max > 0 ? (double) cur / max * 100 : 0;
        killer.sendMessage(plugin.prefix()
            + "§e+" + fmt(gain) + " LK §7| "
            + data.getStage().getColor() + fmt(cur) + "§8/§e" + fmt(max)
            + " §7(" + String.format("%.1f", pct) + "%)");

        // Kiem tra du LK -> thong bao dot pha
        if (cur >= max && !data.isPendingBreakthrough() && !data.isInBreakthroughTrial()
                && !data.getStage().isTopStage()) {
            plugin.getBreakthroughManager().notifyBreakthroughReady(killer, data);
        }
    }

    // ========================
    //  STAT UPDATE
    // ========================
    public void updatePlayerStats(Player player, PlayerData data) {
        try {
            double hp = data.calculateMaxHealth();
            var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(hp);
                if (player.getHealth() > hp) player.setHealth(hp);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi cập nhật stats: " + e.getMessage());
        }
    }

    // ========================
    //  HELPERS
    // ========================
    private long scaleLK(LivingEntity e, long base) {
        return switch (e.getType()) {
            case ZOMBIE, SKELETON, SPIDER          -> base;
            case CREEPER, CAVE_SPIDER              -> (long)(base * 1.2);
            case WITCH, PILLAGER, VINDICATOR       -> (long)(base * 1.5);
            case BLAZE, GHAST                      -> (long)(base * 2.0);
            case ENDERMAN, DROWNED                 -> (long)(base * 1.8);
            case PIGLIN_BRUTE                      -> (long)(base * 2.5);
            case RAVAGER, WARDEN                   -> (long)(base * 5.0);
            default                                -> base;
        };
    }

    private String fmt(long n) {
        if (n >= 1_000_000_000_000L) return String.format("%.2fT", n / 1_000_000_000_000.0);
        if (n >= 1_000_000_000)      return String.format("%.2fB", n / 1_000_000_000.0);
        if (n >= 1_000_000)          return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000)              return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    /**
     * Kiểm tra người chơi có Trấn Thiên Giáp trong túi đồ không.
     * Chỉ cần ở trong inventory (bất kỳ slot nào) — không cần mặc.
     * Dùng PhaBaoManager.getType() để đọc đúng PDC key (pb_type) — chính xác 100%.
     * Public để các hệ thống khác (vd: BreakthroughManager khi tính Thiên Kiếp) dùng được.
     */
    public boolean hasTranThienGiapInInventory(org.bukkit.entity.Player player) {
        com.tutien.phabao.PhaBaoManager pbm = plugin.getPhaBaoManager();
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (pbm.getType(item) == com.tutien.phabao.PhaBaoType.TRAN_THIEN_GIAP) {
                return true;
            }
        }
        return false;
    }
}
