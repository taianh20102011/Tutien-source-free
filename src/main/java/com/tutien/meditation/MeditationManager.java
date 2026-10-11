package com.tutien.meditation;

import com.tutien.TuTienPlugin;
import com.tutien.zone.ZoneManager;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * MeditationManager - He thong Thien Dinh.
 *
 * Nguoi choi gõ /thiendinh (hoac /td) de bat dau ngoi thien dinh.
 * Trong luc thien dinh:
 *  - LK tang moi 3 giay (ty le theo canh gioi + linh can)
 *  - Hieu ung particles xung quanh
 *  - Neu di chuyen / bi danh → thoat thien dinh
 *  - Dong tu: neu co >= 2 nguoi tu luyen gan nhau (<10m) → bonus them 30%
 *
 * Lenh: /thiendinh  (bat/tat)
 */
public class MeditationManager implements Listener {

    private final TuTienPlugin plugin;
    private final Set<UUID> meditating = new HashSet<>();

    // LK tick moi 3 giay (60 tick)
    private static final long TICK_INTERVAL = 60L;

    // Base LK per tick (% of maxLK)
    private static final double BASE_PCT = 0.005; // 0.5% moi 3 giay

    public MeditationManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startTickLoop();
    }

    // ========================
    //  TOGGLE
    // ========================

    public void toggle(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cCần chọn class trước! /tutien chon");
            return;
        }
        if (data.isMeditating()) {
            stopMeditation(player, data, false);
        } else {
            startMeditation(player, data);
        }
    }

    private void startMeditation(Player player, PlayerData data) {
        data.setMeditating(true);
        meditating.add(player.getUniqueId());

        player.sendMessage(plugin.prefix() + "§9✦ Bắt đầu §bThiền Định§9...");
        player.sendMessage(plugin.prefix() + "§7LK tăng dần. Di chuyển hoặc bị đánh để thoát.");
        player.sendTitle(plugin.msg("auto_b2fd963d6c"), plugin.msg("auto_588792be41"),
            5, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.5f);

        // Particles bat dau
        spawnEnterEffect(player);
    }

    public void stopMeditation(Player player, PlayerData data, boolean interrupted) {
        data.setMeditating(false);
        meditating.remove(player.getUniqueId());

        if (interrupted) {
            player.sendMessage(plugin.prefix() + "§c✗ Thiền Định bị gián đoạn!");
            player.sendTitle(plugin.msg("auto_1961c9b133"), plugin.msg("auto_10a9b7d54f"), 5, 30, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        } else {
            long secs = (System.currentTimeMillis() - data.getMeditateStart()) / 1000;
            player.sendMessage(plugin.prefix() + "§9✦ Thoát Thiền Định. §7Đã tu luyện §e"
                + secs + "§7 giây.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.2f);
        }
    }

    // ========================
    //  TICK LOOP
    // ========================

    private void startTickLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!meditating.contains(p.getUniqueId())) continue;
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null || !data.isMeditating()) {
                        meditating.remove(p.getUniqueId());
                        continue;
                    }
                    tickMeditation(p, data);
                }
            }
        }.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    private void tickMeditation(Player player, PlayerData data) {
        double pct = BASE_PCT;

        // Linh can bonus
        if (data.hasLinhCan()) {
            int cnt = data.getLinhCan().size();
            pct *= switch (cnt) {
                case 1 -> 3.0;
                case 2 -> 2.0;
                case 3 -> 1.5;
                case 4 -> 1.0;
                default -> 0.7;
            };
        }

        // Dong tu bonus: tim nguoi choi gan (<10m) cung dang thien dinh
        long groupBonusPlayers = player.getNearbyEntities(10, 5, 10).stream()
            .filter(e -> e instanceof Player other
                && meditating.contains(other.getUniqueId()))
            .count();
        if (groupBonusPlayers >= 1) pct *= 1.3; // +30% dong tu
        if (groupBonusPlayers >= 2) pct *= 1.1; // +10% them moi nguoi

        // Khu vuc bonus (zone) check
        if (plugin.getZoneManager() != null
            && plugin.getZoneManager().isInZone(player.getLocation())) {
            pct *= 1.5; // +50% trong khu vuc thien linh
        }

        long gain = (long)(data.getMaxLinhKhi() * pct);
        if (gain <= 0) gain = 1;
        data.addLinhKhi(gain);

        if (data.getLinhKhi() >= data.getMaxLinhKhi()) {
            plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
        }
        plugin.getSkillItemManager().updateInfoItem(player, data);

        // Particles dep
        spawnMeditateParticles(player);
    }

    // ========================
    //  EVENTS
    // ========================

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!meditating.contains(p.getUniqueId())) return;
        // Cho phep quay dau, chi can giữ nguyen vi tri
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
         || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
         || event.getFrom().getBlockY() != event.getTo().getBlockY()) {
            PlayerData data = plugin.getDataManager().get(p);
            if (data != null) stopMeditation(p, data, true);
        }
    }

    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!meditating.contains(p.getUniqueId())) return;
        PlayerData data = plugin.getDataManager().get(p);
        if (data != null) stopMeditation(p, data, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (!meditating.contains(p.getUniqueId())) return;
        PlayerData data = plugin.getDataManager().get(p);
        if (data != null) { data.setMeditating(false); meditating.remove(p.getUniqueId()); }
    }

    // ========================
    //  PARTICLES
    // ========================

    private void spawnEnterEffect(Player p) {
        Location loc = p.getLocation().clone();
        World w = p.getWorld();
        for (double a = 0; a < 360; a += 20) {
            double rad = Math.toRadians(a);
            Location pt = loc.clone().add(Math.cos(rad)*2, 0.1, Math.sin(rad)*2);
            w.spawnParticle(Particle.DUST, pt, 3, 0.1,0.1,0.1,0,
                new Particle.DustOptions(Color.fromRGB(80,160,255), 1.2f));
        }
        w.spawnParticle(Particle.ENCHANTED_HIT, loc.clone().add(0,1,0), 20, 0.5,0.5,0.5,0.1);
    }

    private void spawnMeditateParticles(Player p) {
        Location loc = p.getLocation().clone().add(0, 0.1, 0);
        World w = p.getWorld();
        // Xoay quanh nguoi choi
        long t = System.currentTimeMillis() / 100;
        double angle = Math.toRadians((t * 8) % 360);
        for (int i = 0; i < 3; i++) {
            double a = angle + Math.toRadians(i * 120);
            Location pt = loc.clone().add(Math.cos(a)*1.2, 0.8 + Math.sin(a*2)*0.3, Math.sin(a)*1.2);
            w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(100,180,255), 1.0f));
        }
    }
}
