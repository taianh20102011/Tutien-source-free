package com.tutien.skills;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * FlyingSwordManager - Kiem bay quanh nguoi choi (Kiem Tu, tu Thanh Gioi tro len).
 *
 * So luong kiem phụ thuộc vào canh gioi:
 *   Ban Thanh    → 1 kiem
 *   Chuan Thanh  → 2 kiem
 *   Dai Thanh    → 3 kiem
 *   Tien Gioi    → 4 kiem
 *   Thien Gioi   → 5 kiem
 *   Thien Tien De→ 6 kiem (toi da)
 *
 * Kiem bay quanh voi particle trail dep.
 * Kiem tan cong ke thu gan (<6m) moi 2 giay neu nguoi choi dang chien dau.
 *
 * Lenh: /kiemdao (bat/tat hieu ung kiem bay)
 */
public class FlyingSwordManager implements Listener {

    private final TuTienPlugin plugin;

    // UUID -> so kiem hien tai dang bay
    private final Map<UUID, BukkitRunnable> swordTasks = new HashMap<>();
    private final Set<UUID> swordEnabled = new HashSet<>();

    // Min ordinal de bat kiem bay (BAN_THANH = 10 trong he thong 25 canh gioi moi)
    private static final int MIN_ORD = 10; // BAN_THANH

    public FlyingSwordManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  TOGGLE / UPDATE
    // ========================

    /** Goi khi dot pha thanh cong hoac player login */
    public void updateSwords(Player player, PlayerData data) {
        if (!data.hasClass() || data.getClassType() != ClassType.KIEM_TU) {
            removeSwords(player);
            return;
        }
        int ord = data.getStage().ordinal();
        if (ord < MIN_ORD) {
            removeSwords(player);
            return;
        }
        if (swordEnabled.contains(player.getUniqueId())) {
            startSwordEffect(player, data);
        }
    }

    public void toggleSwords(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;
        if (data.getClassType() != ClassType.KIEM_TU) {
            player.sendMessage(plugin.prefix() + "§cChỉ Kiếm Tu mới có khả năng này!");
            return;
        }
        if (data.getStage().ordinal() < MIN_ORD) {
            player.sendMessage(plugin.prefix() + "§cCần đạt §fBán Thánh §cđể triệu hồi kiếm bay!");
            return;
        }
        UUID uid = player.getUniqueId();
        if (swordEnabled.contains(uid)) {
            swordEnabled.remove(uid);
            removeSwords(player);
            player.sendMessage(plugin.prefix() + "§7Kiếm bay đã bị thu về.");
        } else {
            swordEnabled.add(uid);
            startSwordEffect(player, data);
            player.sendMessage(plugin.prefix() + "§b⚔ Triệu hồi " + getSwordCount(data) + " cây kiếm bay!");
        }
    }

    private int getSwordCount(PlayerData data) {
        int ord = data.getStage().ordinal();
        if (ord >= 24) return 6; // VO_THUONG_CHI_TON
        if (ord >= 20) return 5; // THIEN_GIOI
        if (ord >= 15) return 4; // TIEN_GIOI
        if (ord >= 12) return 3; // DAI_THANH
        if (ord >= 11) return 2; // CHUAN_THANH
        return 1;                // BAN_THANH
    }

    private void removeSwords(Player player) {
        BukkitRunnable task = swordTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void startSwordEffect(Player player, PlayerData data) {
        removeSwords(player); // Huy task cu neu co

        int count = getSwordCount(data);
        double dmgPerSword = 2.0 * data.calculateDamageMultiplier();

        BukkitRunnable task = new BukkitRunnable() {
            long tick = 0;

            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                PlayerData d = plugin.getDataManager().get(player);
                if (d == null || !swordEnabled.contains(player.getUniqueId())) { cancel(); return; }

                Location base = player.getLocation().clone().add(0, 1.2, 0);
                World w = player.getWorld();
                tick++;

                // Ve tung cay kiem xung quanh
                for (int i = 0; i < count; i++) {
                    double offset = (2.0 * Math.PI * i) / count;
                    double angle  = Math.toRadians(tick * 5) + offset;
                    double radius = 1.4 + Math.sin(tick * 0.05) * 0.2; // dao dong nhe
                    double yOff   = Math.sin(tick * 0.08 + offset) * 0.4;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location swordLoc = base.clone().add(x, yOff, z);

                    // Trail particles dep
                    Color swordColor = getSwordColor(d, i);
                    w.spawnParticle(Particle.DUST, swordLoc, 2, 0.04, 0.04, 0.04, 0,
                        new Particle.DustOptions(swordColor, 1.4f));
                    
                    // Vệt đuôi kiếm khí lưu quang
                    double prevAngle = angle - Math.toRadians(8);
                    Location trailLoc = base.clone().add(Math.cos(prevAngle) * radius, yOff, Math.sin(prevAngle) * radius);
                    w.spawnParticle(Particle.DUST, trailLoc, 1, 0.01, 0.01, 0.01, 0,
                        new Particle.DustOptions(swordColor, 0.8f));

                    // Mũi kiếm sắc bén sáng rực
                    w.spawnParticle(Particle.END_ROD, swordLoc.clone().add(0, 0.16, 0), 1, 0.01, 0.01, 0.01, 0.01);
                }

                // Tan cong ke thu moi 40 tick (2 giay)
                if (tick % 40 == 0) {
                    attackNearbyWithSwords(player, d, count, dmgPerSword);
                }
            }
        };
        task.runTaskTimer(plugin, 0, 1);
        swordTasks.put(player.getUniqueId(), task);
    }

    private Color getSwordColor(PlayerData data, int index) {
        // Canh gioi cao hon = mau dep hon
        int ord = data.getStage().ordinal();
        if (ord >= 25) { // Thien
            Color[] thien = {Color.WHITE, Color.fromRGB(255,215,0), Color.fromRGB(200,200,255),
                Color.fromRGB(255,165,0), Color.fromRGB(150,255,150), Color.fromRGB(255,100,100)};
            return thien[index % thien.length];
        }
        if (ord >= 15) { // Tien
            Color[] tien = {Color.fromRGB(100,200,255), Color.fromRGB(200,255,200),
                Color.fromRGB(255,200,100), Color.fromRGB(200,150,255)};
            return tien[index % tien.length];
        }
        // Thanh Gioi
        return Color.fromRGB(150, 200, 255);
    }

    private boolean isValidHostileTarget(Entity e) {
        if (e.isDead()) return false;
        if (e instanceof Player) return false;
        if (!(e instanceof LivingEntity le)) return false;
        if (e instanceof ArmorStand) return false;
        if (le.isInvulnerable()) return false;
        return true;
    }

    private void attackNearbyWithSwords(Player player, PlayerData data, int count, double dmgPerSword) {
        double range = 5.0 + count * 0.5; // Tam tan cong lon hon khi co nhieu kiem
        for (Entity e : player.getNearbyEntities(range, range/2, range)) {
            if (e == player) continue;
            if (!isValidHostileTarget(e)) continue;
            LivingEntity le = (LivingEntity) e;
            double totalDmg = dmgPerSword * count * 0.3; // Moi kiem gay 30% sát thương co ban
            le.damage(totalDmg, player);
            Location hitLoc = le.getLocation().add(0, 1.0, 0);
            le.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hitLoc, 1, 0, 0, 0, 0);
            le.getWorld().spawnParticle(Particle.CRIT, hitLoc, 8, 0.3, 0.3, 0.3, 0.15);
            le.getWorld().spawnParticle(Particle.FIREWORK, hitLoc, 4, 0.2, 0.2, 0.2, 0.05);
            le.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
            le.getWorld().playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.6f, 1.6f);
            break; // Chi tan cong 1 ke thu moi 2 giay de can bang
        }
    }

    // ========================
    //  EVENTS
    // ========================

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeSwords(e.getPlayer());
        swordEnabled.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = e.getPlayer();
            PlayerData data = plugin.getDataManager().get(p);
            if (data != null && data.getClassType() == ClassType.KIEM_TU
                && data.getStage().ordinal() >= MIN_ORD) {
                swordEnabled.add(p.getUniqueId());
                updateSwords(p, data);
                p.sendMessage(plugin.prefix() + "§b⚔ Kiếm bay tự động kích hoạt! /kiemdao để tắt.");
            }
        }, 40L);
    }
}
