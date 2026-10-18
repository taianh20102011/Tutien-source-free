package com.tutien.aura;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * AuraManager v2 - Vong tu vi xuat hien o CHAN nguoi choi.
 * Nguoi dung co the TAT bang /aura off
 */
public class AuraManager {

    private final TuTienPlugin plugin;
    private final Map<UUID, Double> auraAngles = new HashMap<>();
    private final Set<UUID> auraDisabled = new HashSet<>();  // Players da tat aura

    public AuraManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startAuraLoop();
    }

    // ========================
    //  TOGGLE
    // ========================

    public boolean toggleAura(Player player) {
        UUID uid = player.getUniqueId();
        if (auraDisabled.contains(uid)) {
            auraDisabled.remove(uid);
            player.sendMessage(plugin.prefix() + "§a✦ Vòng Tu Vi §ađã bật §a(bật lại)!");
            return true;
        } else {
            auraDisabled.add(uid);
            player.sendMessage(plugin.prefix() + "§7✦ Vòng Tu Vi §cđã tắt§7.");
            return false;
        }
    }

    public boolean isDisabled(UUID uid) { return auraDisabled.contains(uid); }

    // ========================
    //  MAIN LOOP
    // ========================

    private void startAuraLoop() {
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                tick++;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (auraDisabled.contains(p.getUniqueId())) continue;
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null || !data.hasClass()) continue;
                    double angle = auraAngles.compute(p.getUniqueId(),
                        (k, v) -> v == null ? 0 : (v + 4.0) % 360);
                    renderAura(p, data, angle, tick);
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    // ========================
    //  RENDER
    // ========================

    private void renderAura(Player player, PlayerData data, double angle, int tick) {
        // Xuat hien o CHAN nguoi choi (y = 0.05 tren mat dat)
        Location base = player.getLocation().clone();
        base.setY(base.getY() + 0.05);

        World w = player.getWorld();
        int stageOrd = data.getStage().ordinal();

        // So vong tang theo canh gioi
        int numRings = 1 + Math.min(stageOrd / 6, 3);
        // Ban kinh vong tang theo canh gioi
        double radius = 0.8 + stageOrd * 0.04;
        // Mat do diem tang theo canh gioi
        int density = 1 + Math.min(stageOrd / 5, 4);

        // Render vong chinh theo class
        switch (data.getClassType()) {
            case KIEM_TU  -> renderKiemAura(w, base, angle, tick, radius, numRings, density);
            case PHAP_TU  -> renderPhapAura(w, base, angle, tick, radius, numRings, density);
            case Y_TU     -> renderYAura(w, base, angle, tick, radius, numRings, density);
            case DOC_TU   -> renderDocAura(w, base, angle, tick, radius, numRings, density);
            case THE_TU   -> renderTheAura(w, base, angle, tick, radius, numRings, density);
            case QUY_TU   -> renderQuyAura(w, base, angle, tick, radius, numRings, density);
            case PHONG_TU -> renderPhongAura(w, base, angle, tick, radius, numRings, density);
            case HOA_TU   -> renderHoaAura(w, base, angle, tick, radius, numRings, density);
        }

        // Thien De+ co them vong vang ngoai cung
        if (data.getStage().canUseAllSkills() && tick % 2 == 0) {
            renderThienDeRing(w, base, angle, tick, radius + 0.6, stageOrd);
        }
    }

    // ========================
    //  CLASS AURAS (at feet)
    // ========================

    private void renderKiemAura(World w, Location base, double angle, int tick,
                                  double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color col = Color.fromRGB(180, 210, 255);
        Color col2 = Color.fromRGB(255, 255, 255);
        // Vong kiem quang bac
        for (int r = 0; r < numRings; r++) {
            double rd = radius - r * 0.3;
            for (int i = 0; i < 3 + density; i++) {
                double a = Math.toRadians(angle + i * (360.0/(3+density)) + r*45);
                dust(w, base.clone().add(Math.cos(a)*rd, 0, Math.sin(a)*rd), col, 1.3f);
            }
        }
        // Kiem bay - 1 diem sang di chuyen nhanh
        if (tick % 3 == 0) {
            double a = Math.toRadians(tick * 12);
            dust(w, base.clone().add(Math.cos(a)*(radius+0.2), 0.1, Math.sin(a)*(radius+0.2)), col2, 1.8f);
        }
    }

    private void renderPhapAura(World w, Location base, double angle, int tick,
                                  double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color col = Color.fromRGB(60, 100, 255);
        Color col2 = Color.fromRGB(150, 180, 255);
        // Vong xanh duong kep quay nguoc chieu
        for (int i = 0; i < 4 + density; i++) {
            double a1 = Math.toRadians(angle + i*(360.0/(4+density)));
            double a2 = Math.toRadians(-angle*0.8 + i*(360.0/(4+density)));
            dust(w, base.clone().add(Math.cos(a1)*radius, 0, Math.sin(a1)*radius), col, 1.1f);
            dust(w, base.clone().add(Math.cos(a2)*(radius*0.65), 0, Math.sin(a2)*(radius*0.65)), col2, 0.7f);
        }
    }

    private void renderYAura(World w, Location base, double angle, int tick,
                               double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color col = Color.fromRGB(50, 210, 80);
        // Vong xanh la nhe nhang
        for (int i = 0; i < 3 + density; i++) {
            double a = Math.toRadians(angle * 0.7 + i*(360.0/(3+density)));
            dust(w, base.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius), col, 1.0f);
        }
        // Tim bay len ngau nhien
        if (tick % 18 == 0) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * radius;
            w.spawnParticle(Particle.HEART, base.clone().add(Math.cos(a)*r, 0.3, Math.sin(a)*r),
                1, 0, 0, 0, 0);
        }
    }

    private void renderDocAura(World w, Location base, double angle, int tick,
                                 double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color c1 = Color.fromRGB(140, 20, 180);
        Color c2 = Color.fromRGB(50, 180, 30);
        for (int i = 0; i < 2 + density; i++) {
            double a = Math.toRadians(angle + i*(360.0/(2+density)));
            dust(w, base.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius), c1, 1.0f);
            double a2 = Math.toRadians(-angle*0.7 + i*180);
            dust(w, base.clone().add(Math.cos(a2)*(radius*0.7), 0, Math.sin(a2)*(radius*0.7)), c2, 0.8f);
        }
        if (tick % 8 == 0) {
            w.spawnParticle(Particle.WITCH, base.clone().add((Math.random()-0.5)*radius*2, 0.1,
                (Math.random()-0.5)*radius*2), 1, 0.1, 0.1, 0.1, 0);
        }
    }

    private void renderTheAura(World w, Location base, double angle, int tick,
                                 double radius, int numRings, int density) {
        if (tick % 3 != 0) return;
        Color col = Color.fromRGB(160, 165, 180);
        // Vong xam day chac, xoay cham
        for (int r = 0; r < numRings; r++) {
            double rd = radius - r*0.3;
            for (int i = 0; i < 5 + density; i++) {
                double a = Math.toRadians(angle*0.4 + i*(360.0/(5+density)) + r*36);
                dust(w, base.clone().add(Math.cos(a)*rd, 0, Math.sin(a)*rd),
                    r%2==0 ? col : Color.fromRGB(90,90,100), r%2==0 ? 1.2f : 0.9f);
            }
        }
    }

    private void renderQuyAura(World w, Location base, double angle, int tick,
                                 double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color col = Color.fromRGB(180, 20, 25);
        // Vong do toi nhap nho
        double pulse = 1.0 + Math.sin(Math.toRadians(tick*3)) * 0.2;
        for (int i = 0; i < 2 + density; i++) {
            double a = Math.toRadians(angle + i*(360.0/(2+density)));
            double h = Math.sin(Math.toRadians(tick*4 + i*90)) * 0.08;
            dust(w, base.clone().add(Math.cos(a)*radius*pulse, h, Math.sin(a)*radius*pulse), col, 1.1f);
        }
        if (tick % 10 == 0) {
            w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                base.clone().add((Math.random()-0.5)*radius*2, 0.1, (Math.random()-0.5)*radius*2),
                1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void renderPhongAura(World w, Location base, double angle, int tick,
                                   double radius, int numRings, int density) {
        // Gio xoay nhanh - update moi tick
        Color col = Color.fromRGB(215, 240, 255);
        for (int i = 0; i < 3 + density; i++) {
            double a = Math.toRadians(angle*1.8 + i*(360.0/(3+density)));
            double h = Math.sin(Math.toRadians(tick*5 + i*120)) * 0.1;
            dust(w, base.clone().add(Math.cos(a)*radius, h, Math.sin(a)*radius), col, 0.9f);
        }
        if (tick % 4 == 0) {
            w.spawnParticle(Particle.CLOUD, base.clone().add(
                (Math.random()-0.5)*radius*2, 0.05, (Math.random()-0.5)*radius*2),
                1, 0.1, 0.02, 0.1, 0.02);
        }
    }

    private void renderHoaAura(World w, Location base, double angle, int tick,
                                 double radius, int numRings, int density) {
        if (tick % 2 != 0) return;
        Color col = Color.fromRGB(255, 120, 10);
        for (int i = 0; i < 3 + density; i++) {
            double a = Math.toRadians(angle + i*(360.0/(3+density)));
            dust(w, base.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius), col, 1.1f);
        }
        if (tick % 5 == 0) {
            double a = Math.random()*Math.PI*2;
            double r = Math.random()*radius;
            w.spawnParticle(Particle.FLAME, base.clone().add(Math.cos(a)*r, 0.05, Math.sin(a)*r),
                1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void renderThienDeRing(World w, Location base, double angle, int tick,
                                     double radius, int stageOrd) {
        Color gold = Color.fromRGB(255, 210, 30);
        Color white = Color.fromRGB(255, 255, 255);
        // Vong vang ngoai cung, xoay nguoc
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(-angle*0.5 + i*30);
            dust(w, base.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius), gold, 1.5f);
        }
        if (tick % 6 == 0) {
            double a = Math.toRadians(tick*15);
            dust(w, base.clone().add(Math.cos(a)*(radius+0.15), 0.05, Math.sin(a)*(radius+0.15)), white, 2.0f);
        }
    }

    // ========================
    //  DUST HELPER
    // ========================

    private void dust(World w, Location l, Color c, float size) {
        w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, new Particle.DustOptions(c, size));
    }

    public void cleanup(UUID uuid) {
        auraAngles.remove(uuid);
    }

    public void saveDisabled(UUID uuid, boolean disabled) {
        if (disabled) auraDisabled.add(uuid);
        else auraDisabled.remove(uuid);
    }
}
