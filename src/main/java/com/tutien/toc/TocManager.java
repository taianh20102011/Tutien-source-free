package com.tutien.toc;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TocManager implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    // Tập hợp UUID đang có Long Uy active
    private final Set<UUID> longUyActive = new HashSet<>();
    // Thời gian hồi khi kích hoạt khiên khẩn cấp của Thần Tộc
    private final Set<UUID> thanShieldCd = new HashSet<>();

    public TocManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startPassiveTasks();
    }

    // ========================
    //  QUAY TỘC
    // ========================

    public TocType rollToc() {
        double r = random.nextDouble() * 100.0;
        // Tích lũy xác suất
        if (r < 0.0001) return TocType.LONG_TOC;
        r -= 0.0001;
        if (r < 0.0900) return TocType.LINH_TOC;
        r -= 0.0900;
        if (r < 0.5000) return TocType.THAN_TOC;
        r -= 0.5000;
        if (r < 1.5000) return TocType.YEU_TOC;
        r -= 1.5000;
        if (r < 8.0000) return TocType.THU_HOA;
        return TocType.NGUOI;
    }

    // ========================
    //  HIỂN THỊ TỘC
    // ========================

    public void revealToc(Player player, PlayerData data) {
        TocType toc = data.getToc();
        if (toc == null) return;

        plugin.getEffectEngine().tocReveal(player, toc);
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_69f3b8a673"));
        player.sendMessage(plugin.msg("auto_dd8a4d98e6"));
        player.sendMessage(plugin.msg("auto_2d4f97386a"));
        player.sendMessage("  §7Tộc: " + toc.getColoredName());
        player.sendMessage("  §7" + toc.getDescription());
        player.sendMessage(plugin.msg("auto_2d4f97386a"));
        player.sendMessage("  §eATK: §cx" + String.format("%.2f", toc.getAtkMult()));
        player.sendMessage("  §eDEF: §bx" + String.format("%.2f", toc.getDefMult()));
        player.sendMessage("  §eLK Speed: §ax" + String.format("%.2f", toc.getLkMult()));
        player.sendMessage("  §6Bẩm Sinh: §f" + toc.getPassiveDesc());
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));

        Location loc = player.getLocation();

        if (toc == TocType.LONG_TOC) {
            // Thông báo đặc biệt khi Rồng xuất hiện - gửi tới TẤT CẢ
            String broadcast = "§6§l╔═══════════════════════════════════╗\n"
                    + "§c§l  🐉 LONG TỘC XUẤT THẾ! 🐉\n"
                    + "§e§l  " + player.getName() + " là hậu duệ RỒNG LINH!\n"
                    + "§6§l  Xác suất: 0.0001% - CỰC KỲ HIẾM!\n"
                    + "§6§l╚═══════════════════════════════════╝";
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(broadcast));
            // Hiệu ứng khi xuất hiện Rồng
            new BukkitRunnable() {
                int tick = 0;
                @Override public void run() {
                    if (tick++ >= 80) { cancel(); return; }
                    for (double a = 0; a < 360; a += 10) {
                        double rad = Math.toRadians(a + tick * 4.5);
                        double r = 3 + Math.sin(Math.toRadians(tick * 3)) * 1.5;
                        Location p = loc.clone().add(Math.cos(rad)*r, tick*0.05, Math.sin(rad)*r);
                        player.getWorld().spawnParticle(Particle.FLAME, p, 2, 0.1, 0.1, 0.1, 0.02);
                        player.getWorld().spawnParticle(Particle.LAVA, p, 1, 0.1, 0.1, 0.1, 0);
                    }
                    if (tick % 10 == 0)
                        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f + tick*0.01f);
                }
            }.runTaskTimer(plugin, 0, 1);
            player.getWorld().strikeLightningEffect(loc);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    player.getWorld().strikeLightningEffect(loc), 5L);
            player.sendTitle(plugin.msg("auto_981adccd1b"), plugin.msg("auto_746321d1da"), 10, 100, 20);
            player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.5f);

        } else if (toc == TocType.LINH_TOC) {
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc.add(0,1,0), 60, 1,1,1,0.2);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 1, 0,0,0,0);
            player.sendTitle(plugin.msg("auto_e683bab00f"), plugin.msg("auto_19062d8f24"), 10, 60, 20);
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(
                    plugin.prefix() + "§d✦ " + player.getName() + " là " + toc.getColoredName() + "§d! Hiếm gặp!"));

        } else if (toc == TocType.THAN_TOC) {
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc.add(0,1,0), 50, 0.8,0.8,0.8,0.1);
            player.sendTitle(plugin.msg("auto_8b6446a1dc"), plugin.msg("auto_23edd301d4"), 10, 60, 20);

        } else if (toc == TocType.THU_HOA) {
            player.getWorld().spawnParticle(Particle.CRIT, loc.add(0,1,0), 30, 0.5,0.5,0.5,0.2);
            player.sendTitle(plugin.msg("auto_91ceb1f183"), plugin.msg("auto_e61115abc8"), 10, 60, 20);

        } else if (toc == TocType.YEU_TOC) {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0,1,0), 30, 0.5,0.5,0.5,0.08);
            player.sendTitle(plugin.msg("auto_747863a009"), plugin.msg("auto_398aead518"), 10, 60, 20);

        } else {
            player.getWorld().spawnParticle(Particle.POOF, loc.add(0,1,0), 10, 0.5,0.5,0.5,0.05);
            player.sendTitle(plugin.msg("auto_45cda0333e"), plugin.msg("auto_b8da3e0292"), 10, 60, 20);
        }

        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
    }

    // ========================
    //  SỰ KIỆN CHIẾN ĐẤU
    // ========================

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        PlayerData data = plugin.getDataManager().get(defender);
        if (data == null || data.getToc() == null || !data.isModuleEnabled("toc")) return;

        TocType toc = data.getToc();

        // Yêu Tộc: hút máu khi tấn công
        if (toc == TocType.YEU_TOC && event.getDamager() instanceof Player atk) {
            PlayerData atkData = plugin.getDataManager().get((Player)event.getDamager());
            // Đã được xử lý ở phía người tấn công bên dưới
        }

        // Thần Tộc: khiên khẩn cấp khi HP < 20%
        if (toc == TocType.THAN_TOC && !thanShieldCd.contains(defender.getUniqueId())) {
            double hpAfterHit = defender.getHealth() - event.getFinalDamage();
            if (hpAfterHit > 0 && hpAfterHit / defender.getMaxHealth() < 0.20) {
                thanShieldCd.add(defender.getUniqueId());
                data.addBuff("than_shield", 5000);
                defender.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 100, 4, false, true, true));
                defender.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 60, 2, false, true, true));
                defender.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                        defender.getLocation().add(0,1,0), 30, 0.6, 0.6, 0.6, 0.1);
                defender.sendMessage(plugin.prefix() + "§b✨ Thần Tộc Khai Dũng! Khiên bảo vệ trong 5 giây!");
                // Hồi chiêu 60s
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        thanShieldCd.remove(defender.getUniqueId()), 1200L);
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerData data = plugin.getDataManager().get(attacker);
        if (data == null || data.getToc() == null || !data.isModuleEnabled("toc")) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        TocType toc = data.getToc();

        // Yêu Tộc: hút 5% sát thương
        if (toc == TocType.YEU_TOC) {
            double heal = event.getDamage() * 0.05;
            attacker.setHealth(Math.min(attacker.getHealth() + heal, attacker.getMaxHealth()));
        }

        // Thú Hóa: bộc phát khi HP thấp
        if (toc == TocType.THU_HOA) {
            double hpRatio = attacker.getHealth() / attacker.getMaxHealth();
            if (hpRatio < 0.50 && !data.hasBuff("thuhoaburst")) {
                event.setDamage(event.getDamage() * 1.30);
                data.addBuff("thuhoaburst", 1000); // 1s thời gian chờ
                if (random.nextInt(10) == 0) {
                    attacker.getWorld().spawnParticle(Particle.CRIT,
                            attacker.getLocation().add(0,1,0), 5, 0.3,0.3,0.3,0.2);
                }
            }
        }

        // Long Tộc: Long Uy (Uy áp rồng) gây hiệu ứng sợ hãi thụ động
        if (toc == TocType.LONG_TOC && !data.hasBuff("longuy_cd")) {
            for (Entity e : attacker.getNearbyEntities(6, 4, 6)) {
                if (e instanceof LivingEntity le && e != attacker) {
                    le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 40, 1, false, true, true));
                    le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 40, 0, false, true, true));
                }
            }
            data.addBuff("longuy_cd", 5000);
            if (random.nextInt(20) == 0) {
                attacker.getWorld().spawnParticle(Particle.FLAME,
                        attacker.getLocation().add(0,1,0), 10, 0.5,0.3,0.5,0.08);
            }
        }
    }

    // ========================
    //  NHIỆM VỤ BỊ ĐỘNG
    // ========================

    private void startPassiveTasks() {
        // Long Tộc: kháng lửa + hồi 2% HP mỗi 5 giây
        // Linh Tộc: thông báo đột phá tự động
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null || data.getToc() == null || !data.isModuleEnabled("toc")) continue;
                    TocType toc = data.getToc();

                    if (toc == TocType.LONG_TOC) {
                        // Kháng lửa
                        if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("fire_resistance")))) {
                            p.addPotionEffect(new PotionEffect(
                                    PotionEffectType.getByKey(NamespacedKey.minecraft("fire_resistance")),
                                    200, 0, true, false, false));
                        }
                        // Hồi 2% HP tối đa mỗi 5s
                        if (p.getHealth() < p.getMaxHealth()) {
                            double heal = p.getMaxHealth() * 0.02;
                            p.setHealth(Math.min(p.getHealth() + heal, p.getMaxHealth()));
                        }
                        // Tốc độ rồng
                        if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")))) {
                            p.addPotionEffect(new PotionEffect(
                                    PotionEffectType.getByKey(NamespacedKey.minecraft("speed")),
                                    200, 2, true, false, false));
                        }
                    } else if (toc == TocType.THU_HOA && toc.getSpeedBonus() > 0) {
                        if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")))) {
                            p.addPotionEffect(new PotionEffect(
                                    PotionEffectType.getByKey(NamespacedKey.minecraft("speed")),
                                    200, toc.getSpeedBonus() - 1, true, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // mỗi 5 giây
    }

    // ========================
    //  HỆ SỐ
    // ========================

    public double getAtkMult(PlayerData data) {
        return data.getToc() != null ? data.getToc().getAtkMult() : 1.0;
    }

    public double getDefMult(PlayerData data) {
        return data.getToc() != null ? data.getToc().getDefMult() : 1.0;
    }

    public double getLkMult(PlayerData data) {
        return data.getToc() != null ? data.getToc().getLkMult() : 1.0;
    }

    // Long Tộc: hiệu ứng bùng nổ Long Uy khi đột phá
    public void onBreakthrough(Player player, PlayerData data) {
        if (data.getToc() != TocType.LONG_TOC) return;
        Location loc = player.getLocation();
        // Long Uy bùng phát
        for (Entity e : player.getNearbyEntities(10, 5, 10)) {
            if (e instanceof LivingEntity le && e != player) {
                le.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 200, 3, false, true, true));
                le.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 100, 4, false, true, true));
            }
        }
        loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0,1,0), 80, 3, 3, 3, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 2, 2, 2, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.3f);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(
                plugin.prefix() + "§6§l🐉 " + player.getName() + " Long Uy khai phát! Kẻ thù xung quanh hoảng loạn!"));
    }
}
