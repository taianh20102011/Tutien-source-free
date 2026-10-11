package com.tutien.effects;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import com.tutien.lingcan.LinhCanElement;

import org.bukkit.*;

import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EffectEngine {
    // Cinematic upgrade: public method signatures are preserved so existing
    // callers do not need to change. Visuals are layered rather than spammed.

    private final TuTienPlugin plugin;
    private final Random rng = new Random();

    private static final Color C_WHITE  = Color.fromRGB(255,255,255);
    private static final Color C_GOLD   = Color.fromRGB(255,200, 30);
    private static final Color C_RED    = Color.fromRGB(255, 50, 30);
    private static final Color C_BLUE   = Color.fromRGB( 80,160,255);
    private static final Color C_CYAN   = Color.fromRGB( 50,230,230);
    private static final Color C_GREEN  = Color.fromRGB( 50,220, 80);
    private static final Color C_PURPLE = Color.fromRGB(180, 60,255);
    private static final Color C_ORANGE = Color.fromRGB(255,140, 10);
    private static final Color C_PINK   = Color.fromRGB(255,120,200);
    private static final Color C_SILVER = Color.fromRGB(200,200,220);
    private static final Color C_LIME   = Color.fromRGB(120,255,100);
    private static final Color C_TEAL   = Color.fromRGB( 20,200,180);
    private static final Color C_ROSE   = Color.fromRGB(255, 80,130);
    private static final Color C_VIOLET = Color.fromRGB(130,  0,255);
    private static final Color C_AMBER  = Color.fromRGB(255,170,  0);

    public EffectEngine(TuTienPlugin plugin) { this.plugin = plugin; }

    // ---- particle helpers ----
    private void dust(World w, Location l, Color c, float s, int n) {
        w.spawnParticle(Particle.DUST, l, n, 0,0,0,0, new Particle.DustOptions(c, s));
    }
    private void dustS(World w, Location l, Color c, float s, int n, double sx, double sy, double sz) {
        w.spawnParticle(Particle.DUST, l, n, sx,sy,sz,0, new Particle.DustOptions(c, s));
    }
    private void ring(World w, Location ctr, Color c, float sz, double r, double y, double off) {
        for (double a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a+off);
            dust(w, ctr.clone().add(Math.cos(rad)*r, y, Math.sin(rad)*r), c, sz, 1);
        }
    }
    private void sphere(World w, Location ctr, Color c, float sz, double r, int n) {
        for (int i = 0; i < n; i++) {
            double theta = rng.nextDouble()*Math.PI*2;
            double phi   = Math.acos(2*rng.nextDouble()-1);
            dust(w, ctr.clone().add(r*Math.sin(phi)*Math.cos(theta),r*Math.cos(phi),r*Math.sin(phi)*Math.sin(theta)), c, sz, 1);
        }
    }

    // ================================================================
    //  CINEMATIC HELPERS - layered particles, halos, helixes, sparks
    // ================================================================

    private void halo(World w, Location center, Color c1, Color c2,
                      double radius, double y, int points, double rotation) {
        for (int i = 0; i < points; i++) {
            double a = rotation + (Math.PI * 2 * i / points);
            Color c = (i & 1) == 0 ? c1 : c2;
            dust(w, center.clone().add(Math.cos(a) * radius, y,
                    Math.sin(a) * radius), c, 1.15f, 1);
        }
    }

    private void arc(World w, Location center, Color c, double radius,
                     double y, double start, double end, int points, float size) {
        for (int i = 0; i <= points; i++) {
            double p = i / (double) points;
            double a = start + (end - start) * p;
            dust(w, center.clone().add(Math.cos(a) * radius, y,
                    Math.sin(a) * radius), c, size, 1);
        }
    }

    private void burst(World w, Location center, Color c1, Color c2,
                       int count, double spread, double ySpread) {
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double r = Math.pow(rng.nextDouble(), 0.55) * spread;
            double y = (rng.nextDouble() - 0.5) * ySpread;
            dust(w, center.clone().add(Math.cos(a) * r, y,
                            Math.sin(a) * r), i % 3 == 0 ? c2 : c1,
                    0.8f + rng.nextFloat() * 1.2f, 1);
        }
    }

    private void verticalBeam(World w, Location base, Color c1, Color c2,
                              double height, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double y = height * i / Math.max(1, points - 1);
            double a = i * 0.75;
            double r = radius * (0.35 + 0.65 * Math.sin(i * 0.16) * Math.sin(i * 0.16));
            dust(w, base.clone().add(Math.cos(a) * r, y, Math.sin(a) * r),
                    i % 2 == 0 ? c1 : c2, 1.25f, 1);
        }
    }

    private void cinematicFlash(World w, Location loc, Color c) {
        w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.15, 0.15, 0.15, 0);
        w.spawnParticle(Particle.ENCHANTED_HIT, loc, 35, 1.8, 1.2, 1.8, 0.18);
        dustS(w, loc, c, 2.8f, 55, 2.8, 2.2, 2.8);
        dustS(w, loc, C_WHITE, 1.7f, 30, 1.8, 1.4, 1.8);
    }

    private void delayedFirework(Location loc, Color c1, Color c2,
                                 FireworkEffect.Type type, boolean flicker, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> fw(loc, c1, c2, type, flicker), delay);
    }

    private void pillar(World w, Location base, Color c, float sz, double h, double wd) {        for (double y = 0; y < h; y += 0.25) {
            dust(w, base.clone().add((rng.nextDouble()-0.5)*wd, y, (rng.nextDouble()-0.5)*wd), c, sz, 1);
        }
    }
    private void spiral(World w, Location base, Color c1, Color c2, int steps, double r, double h) {
        for (int i = 0; i < steps; i++) {
            final int si = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double a = Math.toRadians(si*14);
                double y = si*(h/steps);
                dust(w, base.clone().add(r*Math.cos(a),y,r*Math.sin(a)), c1, 1.2f, 1);
                dust(w, base.clone().add(-r*Math.cos(a),y,-r*Math.sin(a)), c2, 1.2f, 1);
            }, (long)i);
        }
    }
    /** Key danh dau 1 Firework la hieu ung TRANG TRI cua plugin (khong phai
     *  firework nguoi choi tu ban/dung de combo elytra) - dung de HUY damage
     *  no gay ra, xem CombatListener#onFireworkEffectDamage(). */
    public static final org.bukkit.NamespacedKey COSMETIC_FIREWORK_KEY =
            new org.bukkit.NamespacedKey("tutien", "cosmetic-firework");

    private void fw(Location loc, Color pri, Color fade, FireworkEffect.Type type, boolean flicker) {
        try {
            Firework rocket = loc.getWorld().spawn(loc, Firework.class, r -> {
                FireworkMeta m = r.getFireworkMeta();
                m.addEffect(FireworkEffect.builder().withColor(pri).withFade(fade).with(type).flicker(flicker).trail(true).build());
                m.setPower(1); r.setFireworkMeta(m);
                // Danh dau la firework TRANG TRI - de CombatListener biet ma
                // huy sat thuong no gay ra khi no (firework vanilla mac dinh
                // GAY DAME cho nguoi xung quanh luc no, ke ca khong phai
                // PvP - day la hieu ung TRANG TRI, khong nen lam dau ai ca).
                r.getPersistentDataContainer().set(COSMETIC_FIREWORK_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            });
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> { if(rocket.isValid()) rocket.detonate(); }, 2L);
        } catch(Exception ignored){}
    }

    // ================================================================
    //  "NET RIENG" THEO CLASS (DaoPath: Chinh Dao / Ma Dao / Si Vuong /
    //  Lo Vuong) - lop hieu ung PHU THEM ben tren hieu ung theo canh gioi
    //  da co (khong thay the), de nguoi choi nhin phat la nhan ra minh
    //  dang choi class nao ngay ca trong luc Dot Pha:
    //    - Chinh Dao : thanh khiet, doi xung, hoa sen 8 canh + anh sang
    //                  sach se (mau xanh lam nhat).
    //    - Ma Dao    : hon loan, khong doi xung, no tung lech tam + lua
    //                  hon mau do sam/tim.
    //    - Si Vuong  : hoc gia - hao quang "trang sach" bay linh tinh
    //                  quanh nguoi, mau vang kim.
    //    - Lo Vuong  : "lo dai phap" - 1 lop vo cau day dac (tuong trung
    //                  cho lop lo bao boc) mau tim, cham rai, chac chan.
    // ================================================================
    private void daoPathFlourish(World w, Location loc, PlayerData data, double scale) {
        if (data == null || !data.hasDaoPath()) return;
        com.tutien.data.DaoPath dp = data.getDaoPath();
        switch (dp) {
            case CHINH_DAO -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    dust(w, loc.clone().add(Math.cos(a) * 1.4 * scale, 0.3, Math.sin(a) * 1.4 * scale), C_CYAN, 1.3f, 1);
                }
                w.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1, 0), (int) (10 * scale), 0.6, 0.8, 0.6, 0.4);
            }
            case MA_DAO -> {
                burst(w, loc, Color.fromRGB(140, 0, 0), C_PURPLE, (int) (16 * scale), 1.3 * scale, 0.5);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, (int) (6 * scale), 0.5, 0.4, 0.5, 0.02);
            }
            case SI_VUONG -> {
                w.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.2, 0), (int) (14 * scale), 0.7, 0.9, 0.7, 0.5);
                dustS(w, loc.clone().add(0, 1.4, 0), C_AMBER, 1.4f, (int) (8 * scale), 0.5, 0.3, 0.5);
            }
            case LO_VUONG -> {
                sphere(w, loc.clone().add(0, 1, 0), C_VIOLET, 1.5f, 1.3 * scale, (int) (12 * scale));
                w.spawnParticle(Particle.COMPOSTER, loc, (int) (6 * scale), 0.4, 0.3, 0.4, 0);
            }
        }
    }

    // ================================================================
    //  DOT PHA CHARGE - xoay helo kep tuyet dep
    // ================================================================
    public void breakthroughCharge(Player player, PlayerData data) {
        Location loc = player.getLocation().clone();
        Color col = getRealmColor(data.getStage()), col2 = getRealmColor2(data.getStage());
        World w = player.getWorld();
        int so = data.getStage().ordinal();

        if (plugin.getItemsAdderEffectHook().tryPlay("dot-pha-tich-luy", loc)) return;

        // Net rieng theo class - choi mot lan ngay khi bat dau tich luy.
        daoPathFlourish(w, loc, data, 1.0);

        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t >= 80 || !player.isOnline()) { cancel(); return; }
                for (int arm = 0; arm < 2; arm++) {
                    double a = Math.toRadians(t*9 + arm*180);
                    double r = 1.5 + 2.5*Math.sin(Math.toRadians(t*2));
                    dust(w, loc.clone().add(Math.cos(a)*r, 0.1+t*0.025, Math.sin(a)*r), arm==0?col:col2, 1.4f, 1);
                }
                if (t%8==0) ring(w, loc, col, 1.1f, 2.5+(t/80.0)*2.5, 0.05, t*5);
                if (t%4==0) { double ang=rng.nextDouble()*Math.PI*2; double d=5+rng.nextDouble()*5;
                    w.spawnParticle(Particle.ENCHANTED_HIT, loc.clone().add(Math.cos(ang)*d,rng.nextDouble()*4,Math.sin(ang)*d),1,0,0,0,0); }
                if (t%3==0) dust(w, loc.clone().add((rng.nextDouble()-0.5)*0.2, t*0.03, (rng.nextDouble()-0.5)*0.2), C_WHITE, 0.7f, 1);
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            fw(loc.clone().add(0,2,0), col, C_WHITE, FireworkEffect.Type.BURST, false);
            dustS(w, loc.clone().add(0,1.5,0), col, 2.5f, 40, 2,2,2);
            dustS(w, loc.clone().add(0,1.5,0), C_WHITE, 1.5f, 25, 2,2,2);
            daoPathFlourish(w, loc, data, 1.3); // net rieng lan 2, luc len dinh diem
            w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);
            w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f);
        }, 75L);
    }

    // ================================================================
    //  TRIAL WAVE - sam set that su + song xung AoE
    // ================================================================
    public void breakthroughTrialWave(Player player, int wave, int totalWaves) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone();
        World w = player.getWorld();
        float prog = (float) wave / totalWaves;
        int so = 0;
        try { so = plugin.getDataManager().get(player).getStage().ordinal(); } catch(Exception ignored){}
        final int stageOrd = so;

        if (plugin.getItemsAdderEffectHook().tryPlay("thi-luyen-song", loc)) return;

        // Sam set that su
        int nStrikes = 1 + (int)(prog*3) + (stageOrd>52?3:0) + (stageOrd>70?3:0);
        for (int i = 0; i < nStrikes; i++) {
            final int di = i*4;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                double a = rng.nextDouble()*Math.PI*2; double d = 2+rng.nextDouble()*6;
                w.strikeLightning(loc.clone().add(Math.cos(a)*d, 0, Math.sin(a)*d));
            }, di);
        }

        // Song xung mo rong
        new BukkitRunnable() {
            double r = 0.3; int t = 0;
            public void run() {
                if (t++ >= 20 || r > 11) { cancel(); return; }
                Color wc  = prog>0.8f?C_RED:prog>0.5f?C_ORANGE:(stageOrd>70?C_VIOLET:C_GOLD);
                Color wc2 = prog>0.6f?C_RED:C_AMBER;
                ring(w, loc, wc,  1.0f+prog*0.9f, r, 0.05, t*10);
                if (t%2==0) ring(w, loc, wc2, 0.7f, r*0.7, 0.12, -t*8);
                if (stageOrd>70 && t%3==0) ring(w, loc, C_VIOLET, 0.5f, r*1.15, 0.2, t*15);
                r += prog>0.7f?0.65:0.5;
            }
        }.runTaskTimer(plugin, 0, 1);

        // Lua bong (canh gioi cao)
        if (stageOrd > 30) {
            new BukkitRunnable() {
                int t = 0;
                public void run() {
                    if (t++ >= 10 || !player.isOnline()) { cancel(); return; }
                    for (int i = 0; i < 6; i++) {
                        double a = rng.nextDouble()*Math.PI*2; double d = rng.nextDouble()*4;
                        Location fl = loc.clone().add(Math.cos(a)*d, rng.nextDouble()*3, Math.sin(a)*d);
                        w.spawnParticle(Particle.FLAME, fl, 1, 0.1,0.2,0.1,0.05);
                        if (stageOrd>52) w.spawnParticle(Particle.SOUL_FIRE_FLAME, fl, 1, 0.1,0.2,0.1,0.04);
                    }
                }
            }.runTaskTimer(plugin, 2, 2);
        }
        if (stageOrd>70) {
            dustS(w, loc.clone().add(0,1,0), C_VIOLET, 2.5f, 20, 1.5f,1f,1.5f);
            dustS(w, loc.clone().add(0,1,0), C_AMBER,  2.0f, 15, 1.5f,1f,1.5f);
        }

        w.spawnParticle(Particle.CRIT, loc.clone().add(0,1.5,0), 8, 0.6f,0.4f,0.6f,0.3f);
        w.spawnParticle(Particle.ENCHANTED_HIT, loc.clone().add(0,1,0), 6, 0.5f,0.3f,0.5f,0.2f);
        float pitch = 0.6f + prog*0.7f;
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.6f, pitch);
        if (prog>0.4f) plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, pitch*0.55f), 6L);
        if (prog>0.75f && stageOrd>52) w.playSound(loc, Sound.ENTITY_WITHER_HURT, 0.7f, 0.5f);

        int blocks = Math.min(10,(int)(prog*10));
        String bar = "§c"+"█".repeat(blocks)+"§8"+"█".repeat(10-blocks);
        String danger = prog>0.85f?" §c§l⚡⚠ CỰC NGUY HIỂM!":prog>0.6f?" §6⚡ Nguy hiểm":"";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize("§4☁ §cSóng §e"+wave+"§c/§e"+totalWaves+"  "+bar+danger));
    }

    // ================================================================
    //  BREAKTHROUGH SUCCESS - phao hoa + tru sang + phat no
    // ================================================================
    public void breakthroughSuccess(Player player, PlayerData data) {
        if (!player.isOnline()) return;

        final Location loc = player.getLocation().clone();
        final World w = player.getWorld();
        final Color col = getRealmColor(data.getStage());
        final Color col2 = getRealmColor2(data.getStage());
        final int ord = data.getStage().ordinal();

        if (plugin.getItemsAdderEffectHook().tryPlay("dot-pha-thanh-cong", loc)) return;

        // PHASE 1: compressed spiritual energy.
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t++ >= 42 || !player.isOnline()) {
                    cancel();
                    return;
                }

                double p = t / 42.0;
                double r = 0.8 + p * 5.2;

                // Triple rotating halos.
                ring(w, loc, col, 1.8f, r, 0.05, t * 8);
                ring(w, loc, col2, 1.15f, r * 0.72, 0.18, -t * 11);
                if (t > 14) ring(w, loc, C_WHITE, 0.65f, r * 0.46, 0.30, t * 16);

                // Rising energy column.
                verticalBeam(w, loc, col, col2, 3.8 + p * 3.5, 0.42, 10);

                // Spiral particles.
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(t * (14 + i * 5) + i * 120);
                    double rr = 1.0 + p * 3.6;
                    double y = 0.4 + p * 4.5;
                    dust(w, loc.clone().add(Math.cos(a) * rr, y,
                            Math.sin(a) * rr), i == 1 ? C_WHITE : col2, 1.6f, 1);
                }

                if (t % 4 == 0) {
                    burst(w, loc.clone().add(0, 1.2 + p * 2, 0),
                            col, C_WHITE, 8, 2.2 + p * 2.5, 2.0);
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        // PHASE 2: sky-opening shockwave.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            cinematicFlash(w, loc.clone().add(0, 2.2, 0), col);
            daoPathFlourish(w, loc, data, 1.6); // net rieng theo class - o dung khoanh khac thanh cong

            for (int i = 0; i < 3; i++) {
                final int fi = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    double r = 2.5 + fi * 1.8;
                    ring(w, loc, fi == 1 ? C_WHITE : col,
                            2.2f - fi * 0.3f, r, 0.08 + fi * 0.18, fi * 32);
                }, fi * 3L);
            }

            delayedFirework(loc.clone().add(0, 2.5, 0),
                    col, C_WHITE, FireworkEffect.Type.BALL_LARGE, true, 0);
            delayedFirework(loc.clone().add(2.5, 2.0, 0),
                    col2, col, FireworkEffect.Type.STAR, true, 5);
            delayedFirework(loc.clone().add(-2.5, 2.0, 0),
                    C_WHITE, col2, FireworkEffect.Type.BURST, true, 10);

            w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.7f, 0.55f);
            w.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1.15f);
            w.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 0.72f);
        }, 42L);

        // PHASE 3: realm aura.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t++ >= 46 || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    double r = 1.2 + t * 0.18;

                    ring(w, loc, col, 1.45f, r, 0.05, t * 10);
                    ring(w, loc, col2, 0.9f, r * 0.67, 0.28, -t * 13);

                    // Four rotating satellites.
                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(t * 7 + i * 90);
                        Location s = loc.clone().add(
                                Math.cos(a) * (2.2 + Math.sin(t * 0.08) * 0.5),
                                1.0 + Math.sin(t * 0.12 + i) * 1.2,
                                Math.sin(a) * (2.2 + Math.sin(t * 0.08) * 0.5)
                        );
                        dust(w, s, i % 2 == 0 ? col : col2, 1.8f, 1);
                    }

                    if (t % 5 == 0) {
                        dustS(w, loc.clone().add(0, 2.2, 0),
                                col, 1.9f, 9, 1.8, 2.0, 1.8);
                    }
                }
            }.runTaskTimer(plugin, 0, 1);

            spawnRealmEffect(player, data.getStage(), loc, col);
        }, 62L);

        // PHASE 4: high-realm finisher.
        if (ord > 52) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    Location p = loc.clone().add(
                            Math.cos(a) * (3.0 + (ord > 70 ? 3.0 : 0)),
                            1.5 + (i % 3) * 0.55,
                            Math.sin(a) * (3.0 + (ord > 70 ? 3.0 : 0))
                    );

                    delayedFirework(p, i % 2 == 0 ? col : col2,
                            C_WHITE,
                            i % 3 == 0 ? FireworkEffect.Type.STAR
                                    : FireworkEffect.Type.BALL_LARGE,
                            true, i * 4L);
                }

                sphere(w, loc.clone().add(0, 3.5, 0), col, 2.6f, 6.0, 90);
                sphere(w, loc.clone().add(0, 3.5, 0), col2, 1.8f, 4.2, 65);
                sphere(w, loc.clone().add(0, 3.5, 0), C_WHITE, 1.0f, 2.4, 35);

                if (ord > 70) {
                    spiral(w, loc, col, col2, 100, 4.8, 9.0);

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;

                        for (int i = 0; i < 6; i++) {
                            double a = Math.toRadians(i * 60);
                            w.strikeLightningEffect(loc.clone().add(
                                    Math.cos(a) * 6.5, 0, Math.sin(a) * 6.5));
                        }

                        cinematicFlash(w, loc.clone().add(0, 4.5, 0), col);
                        w.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.1f, 1.65f);
                        w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.22f);
                    }, 32L);
                }
            }, 88L);
        }
    }

    // ================================================================
    //  BREAKTHROUGH FAIL
    // ================================================================
    public void breakthroughFail(Player player) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone();
        World w = player.getWorld();
        if (plugin.getItemsAdderEffectHook().tryPlay("dot-pha-that-bai", loc)) return;
        // Ham nay khong nhan PlayerData qua tham so (giu nguyen signature cu
        // cho cac noi da goi) - tu lay rieng de choi net rieng theo class,
        // neu loi (vd data chua load) thi bo qua, khong lam hong hieu ung chinh.
        PlayerData failData = null;
        try { failData = plugin.getDataManager().get(player); } catch (Exception ignored) {}
        daoPathFlourish(w, loc, failData, 0.8); // nhe hon binh thuong - vi la that bai, khong phai an mung
        new BukkitRunnable() {
            double r = 0.4; int t = 0;
            public void run() {
                if (t++ >= 22 || r > 14) { cancel(); return; }
                ring(w, loc, C_RED, 2.2f-(float)(r/14), r, 0.04, 0);
                ring(w, loc, Color.fromRGB(80,0,0), 1.0f, r*0.65, 0.12, 30);
                if (t%3==0) ring(w, loc, C_PURPLE, 0.7f, r*1.1, 0.08, t*15);
                r += 0.65;
            }
        }.runTaskTimer(plugin, 0, 1);
        dustS(w, loc.clone().add(0,1,0), C_RED, 2.5f, 50, 3,2,3);
        dustS(w, loc.clone().add(0,1,0), C_PURPLE, 1.5f, 25, 2.5,2,2.5);
        w.spawnParticle(Particle.EXPLOSION, loc.clone().add(0,1.5,0), 10, 1.2,0.5,1.2,0);
        w.spawnParticle(Particle.CRIT, loc.clone().add(0,2,0), 20, 1,0.8,1,0.4);
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t++ >= 20 || !player.isOnline()) { cancel(); return; }
                dust(w, loc.clone().add((rng.nextDouble()-0.5)*0.4,t*0.12,(rng.nextDouble()-0.5)*0.4), Color.fromRGB(80,0,0), 1.8f, 1);
            }
        }.runTaskTimer(plugin, 0, 1);
        w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.5f, 0.25f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.8f, 0.35f);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
    }

    // ================================================================
    //  BOSS SPAWN EFFECT - dep nhat co the
    // ================================================================
    public void bossSpawnEffect(Location center, int bossLevel, String bossName) {
        World w = center.getWorld();
        if (w == null) return;

        if (plugin.getItemsAdderEffectHook().tryPlay("boss-xuat-hien", center)) return;

        final Color bc = getBossColor(bossLevel);
        final Color bc2 = getBossColor2(bossLevel);

        // Phase 1 — cursed seal expanding from the ground.
        new BukkitRunnable() {
            double r = 0.35;
            int t = 0;

            @Override
            public void run() {
                if (t++ >= 34 || r > 18) {
                    cancel();
                    return;
                }

                ring(w, center, bc, 2.7f, r, 0.04, t * 15);
                ring(w, center, bc2, 1.45f, r * 0.66, 0.16, -t * 18);
                ring(w, center, Color.fromRGB(70, 0, 95),
                        0.8f, r * 1.14, 0.09, t * 25);

                if (t % 2 == 0) {
                    arc(w, center, bc2, r * 0.88, 0.24,
                            t * 0.17, t * 0.17 + Math.PI * 1.15, 18, 1.0f);
                }
                r += 0.58;
            }
        }.runTaskTimer(plugin, 0, 1);

        // Phase 2 — dark column and orbiting embers.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t++ >= 44) {
                        cancel();
                        return;
                    }

                    verticalBeam(w, center, bc, bc2,
                            Math.min(7.5, 0.8 + t * 0.22), 0.8, 16);

                    for (int i = 0; i < 5; i++) {
                        double a = Math.toRadians(t * 11 + i * 72);
                        double r = 2.4 + Math.sin(t * 0.15 + i) * 0.55;
                        Location p = center.clone().add(
                                Math.cos(a) * r,
                                0.5 + (t % 18) * 0.12,
                                Math.sin(a) * r
                        );
                        w.spawnParticle(
                                bossLevel >= 11 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME,
                                p, 1, 0.03, 0.10, 0.03, 0.015
                        );
                        dust(w, p, i % 2 == 0 ? bc : bc2, 1.35f, 1);
                    }

                    if (t % 4 == 0) {
                        w.spawnParticle(Particle.LARGE_SMOKE,
                                center.clone().add(
                                        (rng.nextDouble() - 0.5) * 5,
                                        rng.nextDouble() * 4,
                                        (rng.nextDouble() - 0.5) * 5
                                ), 3, 0.25, 0.12, 0.25, 0.035);
                    }

                    if (t % 9 == 0) {
                        double a = rng.nextDouble() * Math.PI * 2;
                        double d = 3 + rng.nextDouble() * 5;
                        w.strikeLightningEffect(center.clone().add(
                                Math.cos(a) * d, 0, Math.sin(a) * d));
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }, 8L);

        // Phase 3 — boss reveal flash.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cinematicFlash(w, center.clone().add(0, 2.3, 0), bc);

            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60);
                Location p = center.clone().add(
                        Math.cos(a) * 4.2, 2.0 + (i % 2), Math.sin(a) * 4.2);
                delayedFirework(p, i % 2 == 0 ? bc : bc2, C_WHITE,
                        i % 3 == 0 ? FireworkEffect.Type.STAR
                                : FireworkEffect.Type.BALL_LARGE,
                        true, i * 4L);
            }

            sphere(w, center.clone().add(0, 2.4, 0), bc, 3.2f, 4.8, 75);
            sphere(w, center.clone().add(0, 2.4, 0), C_WHITE, 1.3f, 2.5, 35);
            sphere(w, center.clone().add(0, 2.4, 0), bc2, 2.0f, 5.8, 45);

            w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2f, 0.32f);
            w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.7f, 0.48f);
            w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.35f);
        }, 48L);

        // Phase 4 — persistent intimidation aura.
        if (bossLevel >= 10) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new BukkitRunnable() {
                    int t = 0;

                    @Override
                    public void run() {
                        if (t++ >= 38) {
                            cancel();
                            return;
                        }

                        double pulse = 3.5 + Math.sin(t * 0.17) * 1.25;
                        ring(w, center, bc, 1.9f, pulse, 1.25, t * 17);
                        ring(w, center, bc2, 1.1f, 2.5, 2.6, -t * 14);

                        double a = Math.toRadians(t * 14);
                        dust(w, center.clone().add(
                                Math.cos(a) * 3.1, 2.0,
                                Math.sin(a) * 3.1), C_WHITE, 1.4f, 1);
                        w.spawnParticle(Particle.SOUL,
                                center.clone().add(Math.cos(a + Math.PI) * 3,
                                        1.7, Math.sin(a + Math.PI) * 3),
                                1, 0.05, 0.08, 0.05, 0.015);
                    }
                }.runTaskTimer(plugin, 0, 1);

                w.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.38f);
            }, 60L);
        }
    }

    // ================================================================
    //  REALM EFFECTS
    // ================================================================
    private void spawnRealmEffect(Player player, CultivationStage stage, Location loc, Color col) {
        World w = player.getWorld();
        int ord = stage.ordinal();
        Color col2 = getRealmColor2(stage);

        if (ord == 52) { // Vo Thuong Chi Ton (canh gioi cuoi cung): TUYET DINH EFFECT
            for (int i=0;i<8;i++) { final int fi=i;
                plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                    double a=fi*45*(Math.PI/180);
                    fw(loc.clone().add(Math.cos(a)*6,fi*0.3+1,Math.sin(a)*6), col, col2, FireworkEffect.Type.BALL_LARGE, true);
                    fw(loc.clone().add(Math.cos(a)*3,fi*0.3+2.5,Math.sin(a)*3), col2, col, FireworkEffect.Type.STAR, fi%2==0);
                }, fi*8L);
            }
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                sphere(w,loc.clone().add(0,3,0),col,  3.5f,8.0,100);
                sphere(w,loc.clone().add(0,3,0),col2, 2.5f,5.5, 70);
                sphere(w,loc.clone().add(0,3,0),C_WHITE,1.5f,3.0, 40);
            },10L);
            spiral(w, loc, col, col2, 80, 4.0, 8.0);
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                for (int i=0;i<4;i++){double a=i*90*(Math.PI/180);w.strikeLightningEffect(loc.clone().add(Math.cos(a)*5,0,Math.sin(a)*5));}
                dustS(w,loc.clone().add(0,5,0),col,5.0f,150,8,8,8);
                w.spawnParticle(Particle.EXPLOSION,loc.clone().add(0,4,0),12,2.5,2,2.5,0);
                w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,2f,0.25f);
                w.playSound(loc,Sound.ENTITY_WITHER_DEATH,1f,1.8f);
                w.playSound(loc,Sound.UI_TOAST_CHALLENGE_COMPLETE,2f,1.2f);
                w.playSound(loc,Sound.BLOCK_BEACON_POWER_SELECT,2f,0.2f);
            },25L);
            return;
        }

        if (ord >= 44) { // Hong Mong Gioi tro len (3 tang lon truoc dinh cao)
            for (int i=0;i<6;i++) { final int fi=i;
                plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                    double a=fi*60*(Math.PI/180);
                    fw(loc.clone().add(Math.cos(a)*4,fi*0.5,Math.sin(a)*4),col,col2,FireworkEffect.Type.values()[fi%FireworkEffect.Type.values().length],true);
                },fi*10L);
            }
            sphere(w,loc.clone().add(0,3,0),col,3.0f,6.0,80);
            sphere(w,loc.clone().add(0,3,0),C_WHITE,1.5f,4.0,40);
            dustS(w,loc.clone().add(0,4,0),col,4.0f,100,6,6,6);
            w.spawnParticle(Particle.EXPLOSION,loc.clone().add(0,3,0),10,2,1.5,2,0);
            w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,2f,0.3f);
            w.playSound(loc,Sound.UI_TOAST_CHALLENGE_COMPLETE,2f,1f);
            return;
        }

        switch (stage.getRealm()) {
            case "Nhân Giới" -> { new BukkitRunnable(){int t=0;public void run(){if(t++>=20){cancel();return;}dustS(w,loc.clone().add(0,t*0.1,0),col,1.2f,3,0.4,0.1,0.4);}}.runTaskTimer(plugin,0,2); w.playSound(loc,Sound.BLOCK_NOTE_BLOCK_CHIME,1f,1.5f); }
            case "Linh Giới" -> { new BukkitRunnable(){int t=0;public void run(){if(t++>=30){cancel();return;}ring(w,loc,col,1.4f,t*0.15,0.05,t*8);dustS(w,loc.clone().add(0,t*0.12,0),C_WHITE,0.9f,2,0.3,0.1,0.3);}}.runTaskTimer(plugin,0,1); w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_FLAP,0.8f,1.8f); }
            case "Tiên Giới" -> { fw(loc.clone().add(0,2,0),col,C_WHITE,FireworkEffect.Type.BALL,true); new BukkitRunnable(){int t=0;public void run(){if(t++>=40){cancel();return;}double r=t*0.18;ring(w,loc,col,1.6f,r,0.05,t*6);ring(w,loc,C_WHITE,0.8f,r*0.55,0.15,-t*9);}}.runTaskTimer(plugin,0,1); w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,0.5f,1.8f); }
            case "Đế Giới"   -> { fw(loc.clone().add(0,3,0),C_RED,C_ORANGE,FireworkEffect.Type.STAR,true); new BukkitRunnable(){int t=0;public void run(){if(t++>=45){cancel();return;}double r=t*0.15;ring(w,loc,C_RED,1.8f,r,0.05,t*6);ring(w,loc,C_ORANGE,1.1f,r*0.75,0.15,-t*9);if(t%4==0)w.spawnParticle(Particle.FLAME,loc.clone().add(0,0.5,0),5,r,0.3,r,0.05);}}.runTaskTimer(plugin,0,1); w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,0.7f,0.5f); }
            case "Thánh Giới"-> { fw(loc.clone().add(0,4,0),C_WHITE,C_GOLD,FireworkEffect.Type.BALL_LARGE,true); fw(loc.clone().add(1,3,0),C_GOLD,C_WHITE,FireworkEffect.Type.STAR,false); new BukkitRunnable(){int t=0;public void run(){if(t++>=55){cancel();return;}for(double y=0;y<12;y+=0.6){if(rng.nextDouble()<0.35)dust(w,loc.clone().add((rng.nextDouble()-0.5)*0.4,y,(rng.nextDouble()-0.5)*0.4),C_WHITE,1.6f,1);}double r=t*0.12;ring(w,loc,C_WHITE,2.0f,r,0.05,t*4);ring(w,loc,C_GOLD,1.2f,r*0.8,0.15,-t*6);}}.runTaskTimer(plugin,0,1); w.playSound(loc,Sound.ENTITY_PLAYER_LEVELUP,2f,0.4f); w.playSound(loc,Sound.BLOCK_BEACON_POWER_SELECT,1.5f,0.3f); }
            case "Thiên Giới"-> { for(int i=0;i<4;i++){final int fi=i;plugin.getServer().getScheduler().runTaskLater(plugin,()->{double a=fi*90*(Math.PI/180);fw(loc.clone().add(Math.cos(a)*3,fi+1,Math.sin(a)*3),C_GOLD,C_WHITE,FireworkEffect.Type.BALL_LARGE,true);},fi*12L);} new BukkitRunnable(){int t=0;public void run(){if(t++>=50){cancel();return;}double r=t*0.2;ring(w,loc,C_GOLD,2.2f,r,0.05,t*6);ring(w,loc,C_WHITE,1.2f,r*0.7,0.18,-t*8);}}.runTaskTimer(plugin,0,1); w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,1.0f,0.4f); }
            default          -> { sphere(w,loc.clone().add(0,2,0),col,2.2f,4.0,50); fw(loc.clone().add(0,3,0),col,C_WHITE,FireworkEffect.Type.BURST,true); }
        }
    }

    // ================================================================
    //  SKILL / PHAP BAO / DAN DUOC EFFECTS
    // ================================================================
    public void skillCast(Player player, String skillId) {
        if (!player.isOnline()) return;

        final Location loc = player.getLocation().clone().add(0, 1, 0);
        final World w = player.getWorld();
        final Color col = getSkillColor(skillId);

        // Uu tien hieu ung RIENG cho tung skill (map."skill-<skillId>"), neu
        // admin chua cau hinh rieng thi roi ve hieu ung DUNG CHUNG cho moi
        // active skill (map."thi-trien-ky-nang") nhu truoc gio.
        if (plugin.getItemsAdderEffectHook().tryPlay("skill-" + skillId, loc)) return;
        if (plugin.getItemsAdderEffectHook().tryPlay("thi-trien-ky-nang", loc)) return;

        // MOI: tung NHOM ky nang (theo tu khoa trong id, dong bo voi mau o
        // getSkillColor ben duoi) co MOT BO HIEU UNG RIENG - khong con dung
        // chung 1 "vong tron xoay ra" phang le cho MOI ky nang nhu truoc.
        // Moi bo ket hop nhieu lop: vung/lop bao phu quanh nguoi choi
        // (sphere - hinh cau 3D om lay nguoi), tia/cot sang (beam), cung
        // (arc - kieu chem kiem), va hat toe ra (burst) - de co chieu sau
        // thay vi chi 1 mat phang vong tron lap lai.
        if (skillId.contains("hoa") || skillId.contains("lua") || skillId.contains("nghiep")) {
            castFireSkill(w, loc, player, col);
        } else if (skillId.contains("kiem") || skillId.contains("sat")) {
            castBladeSkill(w, loc, player, col);
        } else if (skillId.contains("bang") || skillId.contains("dong")) {
            castFrostSkill(w, loc, player, col);
        } else if (skillId.contains("loi") || skillId.contains("sam")) {
            castLightningSkill(w, loc, player, col);
        } else if (skillId.contains("doc") || skillId.contains("am")) {
            castPoisonSkill(w, loc, player, col);
        } else if (skillId.contains("bich") || skillId.contains("kim")) {
            castShieldSkill(w, loc, player, col);
        } else if (skillId.contains("duoc") || skillId.contains("hoi")) {
            castHealSkill(w, loc, player, col);
        } else if (skillId.contains("quy") || skillId.contains("vuong")) {
            castDemonSkill(w, loc, player, col);
        } else if (skillId.contains("phong") || skillId.contains("pha")) {
            castWindSkill(w, loc, player, col);
        } else {
            castArcaneSkill(w, loc, player, col); // phap/linh + mac dinh
        }

        w.playSound(loc, getSkillSound(skillId), 0.95f, getSkillPitch(skillId));
    }

    // ---- 9 bo hieu ung theo nhom nguyen to/phong cach ky nang ----
    // Moi bo tu huy sau ~0.5-1 giay, khong dung tick lau de tranh lag khi
    // nhieu nguoi cung thi trien ky nang mot luc.

    private void castFireSkill(World w, Location loc, Player player, Color col) {
        // Toe lua duoi chan + cot lua boc len - vung anh huong ro rang
        burst(w, loc.clone().add(0, 0.05, 0), col, C_ORANGE, 26, 1.6, 0.3);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 16 || !player.isOnline()) { cancel(); return; }
            double h = t * 0.16;
            dustS(w, loc.clone().add((rng.nextDouble()-0.5)*0.6, h, (rng.nextDouble()-0.5)*0.6),
                    t % 2 == 0 ? col : C_ORANGE, 1.4f, 3, 0.15, 0.05, 0.15);
            if (t % 4 == 0) w.spawnParticle(Particle.FLAME, loc.clone().add(0, h * 0.5, 0), 6, 0.5, 0.2, 0.5, 0.02);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.LAVA, loc, 4, 0.4, 0.1, 0.4, 0);
    }

    private void castBladeSkill(World w, Location loc, Player player, Color col) {
        // 2 duong cung cheo nhau kieu che-kiem thay vi vong tron dong tam
        double base = Math.toRadians(rng.nextDouble() * 360);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 10 || !player.isOnline()) { cancel(); return; }
            double p = t / 10.0;
            arc(w, loc, col, 1.1 + p * 0.6, 0.1, base, base + Math.PI * 0.9, 10, 1.4f);
            arc(w, loc, C_WHITE, 0.6 + p * 0.4, 0.4, base + Math.PI, base + Math.PI * 1.9, 8, 1.0f);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.3, 0.2, 0.3, 0);
        w.spawnParticle(Particle.CRIT, loc, 14, 0.6, 0.4, 0.6, 0.3);
    }

    private void castFrostSkill(World w, Location loc, Player player, Color col) {
        // Lop vo bang (sphere) om quanh nguoi - dung "lop bao phu" ro rang
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 14 || !player.isOnline()) { cancel(); return; }
            sphere(w, loc, t % 2 == 0 ? col : C_WHITE, 1.3f, 1.1, 10);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.8, 0.8, 0.8, 0.02);
    }

    private void castLightningSkill(World w, Location loc, Player player, Color col) {
        // Cot sang doc thay vi vong tron ngang
        Location base = loc.clone().subtract(0, 1, 0);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 8 || !player.isOnline()) { cancel(); return; }
            verticalBeam(w, base, col, C_WHITE, 4.0, 0.35, 10);
        }}.runTaskTimer(plugin, 0, 1);
        cinematicFlash(w, loc.clone().add(0, 1.5, 0), col);
    }

    private void castPoisonSkill(World w, Location loc, Player player, Color col) {
        // Vung khi doc lan duoi chan (ground zone) thay vi hieu ung tren dau
        Location ground = loc.clone().subtract(0, 0.9, 0);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 16 || !player.isOnline()) { cancel(); return; }
            double r = 0.4 + t * 0.13;
            ring(w, ground, col, 1.5f, r, 0.0, t * 10);
            if (t % 3 == 0) w.spawnParticle(Particle.WITCH, ground, 4, r * 0.5, 0.1, r * 0.5, 0);
        }}.runTaskTimer(plugin, 0, 1);
    }

    private void castShieldSkill(World w, Location loc, Player player, Color col) {
        // Lop khien (sphere) giu quanh nguoi mot chut roi thu lai - dung
        // nghia den "lop bao phu"/vung ho ve
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 20 || !player.isOnline()) { cancel(); return; }
            double r = t < 12 ? 1.3 : Math.max(0.2, 1.3 - (t - 12) * 0.16);
            sphere(w, loc, t % 2 == 0 ? col : C_WHITE, 1.2f, r, 8);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.END_ROD, loc, 6, 0.5, 0.5, 0.5, 0.02);
    }

    private void castHealSkill(World w, Location loc, Player player, Color col) {
        // Xoan oc bay len nhe nhang thay vi vong tron gat gao
        spiral(w, loc.clone().subtract(0, 1, 0), col, C_WHITE, 16, 0.7, 2.2);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 10 || !player.isOnline()) { cancel(); return; }
            if (t % 2 == 0) w.spawnParticle(Particle.HAPPY_VILLAGER, loc, 4, 0.5, 0.4, 0.5, 0);
        }}.runTaskTimer(plugin, 0, 1);
    }

    private void castDemonSkill(World w, Location loc, Player player, Color col) {
        // Vang toe u am + cong xoan mau tim - cam giac tho tan/quy di
        burst(w, loc, col, C_PURPLE, 24, 1.4, 0.6);
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 12 || !player.isOnline()) { cancel(); return; }
            ring(w, loc, t % 2 == 0 ? col : C_VIOLET, 1.3f, 0.6 + t * 0.15, Math.sin(t * 0.3) * 0.3, -t * 20);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.PORTAL, loc, 20, 0.6, 0.6, 0.6, 0.4);
    }

    private void castWindSkill(World w, Location loc, Player player, Color col) {
        // Vong xoay MONG va NHANH hon han cac nhom khac - cam giac tan gio
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 10 || !player.isOnline()) { cancel(); return; }
            double r = t * 0.35;
            ring(w, loc, col, 0.9f, r, 0.0, t * 26);
            ring(w, loc, C_WHITE, 0.6f, r * 0.7, 0.25, -t * 33);
        }}.runTaskTimer(plugin, 0, 1);
        w.spawnParticle(Particle.CLOUD, loc, 10, 0.6, 0.3, 0.6, 0.05);
    }

    private void castArcaneSkill(World w, Location loc, Player player, Color col) {
        // Mac dinh (phap thuat/linh khi chung): 2 halo dan lop, ke ca 1 lop
        // cau hao quang gan sat nguoi - phong phu hon 1 vong tron don le cu
        new BukkitRunnable() { int t = 0; public void run() {
            if (t++ >= 14 || !player.isOnline()) { cancel(); return; }
            double p = t / 14.0;
            halo(w, loc, col, C_WHITE, 0.3 + p * 1.7, 0.0, 10, t * 0.35);
            halo(w, loc, C_WHITE, col, 0.2 + p * 1.2, 0.3, 8, -t * 0.5);
        }}.runTaskTimer(plugin, 0, 1);
        sphere(w, loc, col, 1.1f, 0.7, 10);
        w.spawnParticle(Particle.ENCHANTED_HIT, loc, 18, 0.55, 0.45, 0.55, 0.16);
    }


    public void phaBaoCast(Player player, String abilityId, String tierName) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone().add(0,1,0);
        World w = player.getWorld();
        // Uu tien hieu ung RIENG cho tung Phap Bao (map."phabao-<abilityId>"),
        // neu admin chua cau hinh rieng thi roi ve hieu ung DUNG CHUNG cho moi
        // Phap Bao (map."pha-bao-kich-hoat") nhu truoc gio.
        if (plugin.getItemsAdderEffectHook().tryPlay("phabao-" + abilityId, loc)) return;
        if (plugin.getItemsAdderEffectHook().tryPlay("pha-bao-kich-hoat", loc)) return;
        Color col = switch(tierName.toLowerCase()) {
            case "tien pham"->C_GOLD; case "de pham"->C_VIOLET; case "thuong pham"->C_CYAN;
            case "trung pham"->C_BLUE; case "ha pham"->C_GREEN; default->C_SILVER;
        };
        int rings = switch(tierName.toLowerCase()) {
            case "tien pham"->5; case "de pham"->4; case "thuong pham"->3; case "trung pham"->2; default->1;
        };
        boolean caoCap = rings >= 3; // Thuong/De/Tien Pham

        // Vong xoay dong tam (nhu cu, van giu vi da dep) + THEM lop "hao
        // quang" (sphere) om sat nguoi choi trong vai tick dau - the hien
        // ro "vung/lop bao phu" cua Phap Bao dang kich hoat, khong con la
        // MOI vong tron phang le nhu truoc.
        new BukkitRunnable(){int t=0;public void run(){
            if(t++>=16||!player.isOnline()){cancel();return;}
            double r=t*0.24;
            for(int ri=0;ri<rings;ri++) ring(w,loc,ri%2==0?col:C_WHITE,1.5f+ri*0.2f,r,ri*0.25,t*(8+ri*3));
            if (t <= 8) sphere(w, loc, t % 2 == 0 ? col : C_WHITE, 1.1f, 0.65, 4 + rings * 2);
        }}.runTaskTimer(plugin,0,1);

        // Cap CAO (Thuong Pham tro len): them cot sang thang dung - cang cao
        // cap cang hoanh trang, khac han voi Ha/Trung Pham chi co vong+cau.
        if (caoCap) {
            verticalBeam(w, loc.clone().subtract(0,1,0), col, C_WHITE, 3.0, 0.4, 12);
        }
        if (rings >= 5) {
            fw(loc.clone().add(0,1.5,0), col, C_WHITE, FireworkEffect.Type.BALL, true);
        }

        burst(w, loc, col, C_WHITE, 10 + rings * 4, 0.9 + rings * 0.15, 0.5);
        w.spawnParticle(Particle.ENCHANTED_HIT,loc,18,0.6,0.5,0.6,0.22);
        w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
        if (rings >= 4) w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.6f);
    }

    public void linhCanReveal(Player player, List<LinhCanElement> roots) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone().add(0,0.1,0);
        World w = player.getWorld();
        if (plugin.getItemsAdderEffectHook().tryPlay("linh-can-lo-dien", loc)) return;
        int cnt = roots.size();
        Color col = cnt==1?C_GOLD:cnt==2?C_CYAN:cnt==3?C_GREEN:cnt==4?C_SILVER:C_VIOLET;
        boolean hasSieuCap = roots.stream().anyMatch(LinhCanElement::isSieuCap);
        if (hasSieuCap) col = C_AMBER;
        final Color fc = col;
        new BukkitRunnable(){int t=0;public void run(){
            if(t++>=50||!player.isOnline()){cancel();return;}
            double r=t*0.1+0.5;
            ring(w,loc,fc,1.6f,r,0.08,t*8);
            if(t<25)ring(w,loc,C_WHITE,0.9f,r*0.5,0.18,-t*6);
            if(t%6==0)dustS(w,loc.clone().add(0,1,0),fc,2.0f,6,0.9,0.9,0.9);
            if(t%8==0)w.spawnParticle(Particle.ENCHANTED_HIT,loc.clone().add(0,1.5,0),3,0.5,0.3,0.5,0.12);
        }}.runTaskTimer(plugin,0,1);
        if(hasSieuCap){fw(loc.clone().add(0,2,0),C_AMBER,C_WHITE,FireworkEffect.Type.BALL_LARGE,true);w.playSound(loc,Sound.ENTITY_ENDER_DRAGON_DEATH,0.4f,1.8f);}
        else{w.spawnParticle(Particle.ENCHANTED_HIT,loc.clone().add(0,1.5,0),20,0.8,0.8,0.8,0.22);w.playSound(loc,Sound.BLOCK_BEACON_POWER_SELECT,1f,cnt>=3?1.6f:0.9f);}
    }

    public void tocReveal(Player player, com.tutien.toc.TocType toc) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone().add(0,0.1,0);
        World w = player.getWorld();
        if (plugin.getItemsAdderEffectHook().tryPlay("toc-lo-dien", loc)) return;
        Color col = getTocColor(toc);
        new BukkitRunnable(){int t=0;public void run(){
            if(t++>=35||!player.isOnline()){cancel();return;}
            double r=2.5+Math.sin(Math.toRadians(t*10))*1.8;
            ring(w,loc,col,2.0f,r,0.04,t*11);
            if(t%5==0)dustS(w,loc.clone().add(0,1.5,0),col,2.8f,9,1.6,1.6,1.6);
        }}.runTaskTimer(plugin,0,1);
        if(toc.isUltraRare()){
            for(int i=0;i<3;i++){final int fi=i;plugin.getServer().getScheduler().runTaskLater(plugin,()->fw(loc.clone().add(0,fi*1.2+1,0),col,C_WHITE,FireworkEffect.Type.BALL_LARGE,true),fi*10L);}
            w.spawnParticle(Particle.ENCHANTED_HIT,loc.clone().add(0,2,0),30,1.5,1.5,1.5,0.3);
            w.playSound(loc, toc.name().contains("LONG")||toc.name().contains("THUY_TINH")?Sound.ENTITY_ENDER_DRAGON_GROWL:Sound.ENTITY_WITHER_SPAWN, 1.2f, 1.4f);
        } else {
            w.spawnParticle(Particle.ENCHANTED_HIT,loc.clone().add(0,1.5,0),15,0.8,0.8,0.8,0.22);
            w.playSound(loc,Sound.BLOCK_BEACON_POWER_SELECT,0.9f,1.2f);
        }
    }

    public void danDuocUse(Player player, String danId, String colorCode) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().clone().add(0,1,0);
        World w = player.getWorld();
        if (plugin.getItemsAdderEffectHook().tryPlay("dung-dan-duoc", loc)) return;
        Color col = getDanColor(colorCode);
        new BukkitRunnable(){int t=0;public void run(){
            if(t++>=12||!player.isOnline()){cancel();return;}
            ring(w,loc,col,1.4f,t*0.2,0.1,t*12);
            if(t%3==0){dustS(w,loc.clone().add(0,t*0.12,0),col,1.8f,3,0.4,0.1,0.4);w.spawnParticle(Particle.ENCHANTED_HIT,loc.clone().add(0,t*0.1,0),2,0.2,0.1,0.2,0.05);}
        }}.runTaskTimer(plugin,0,1);
        w.spawnParticle(Particle.COMPOSTER,loc,8,0.4,0.3,0.4,0.1);
        w.playSound(loc,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1.3f);
    }

    // helpers
    private Color getDanColor(String c) {
        return switch(c) {
            case "§a"->C_GREEN; case "§b"->C_CYAN; case "§c"->C_RED; case "§d"->C_PINK;
            case "§e"->C_AMBER; case "§5"->C_PURPLE; case "§6"->C_GOLD; case "§9"->C_BLUE;
            case "§3"->C_TEAL; case "§4"->Color.fromRGB(180,0,0); default->C_WHITE;
        };
    }
    private Color getBossColor(int lv) {
        if(lv>=16)return Color.fromRGB(200,0,0); if(lv>=13)return Color.fromRGB(180,0,220);
        if(lv>=10)return Color.fromRGB(80,0,200); if(lv>=7)return Color.fromRGB(0,0,180);
        if(lv>=4) return Color.fromRGB(180,60,0); return Color.fromRGB(120,120,120);
    }
    private Color getBossColor2(int lv) {
        if(lv>=16)return Color.fromRGB(255,100,0); if(lv>=13)return Color.fromRGB(255,200,0);
        if(lv>=10)return Color.fromRGB(100,200,255); return Color.fromRGB(255,220,100);
    }
    private Color getSkillColor(String id) {
        if(id.contains("hoa")||id.contains("lua")||id.contains("nghiep"))return C_ORANGE;
        if(id.contains("kiem")||id.contains("sat"))return C_SILVER;
        if(id.contains("phap")||id.contains("linh"))return C_BLUE;
        if(id.contains("duoc")||id.contains("hoi"))return C_GREEN;
        if(id.contains("doc")||id.contains("am"))return C_PURPLE;
        if(id.contains("bich")||id.contains("kim"))return C_CYAN;
        if(id.contains("quy")||id.contains("vuong"))return C_VIOLET;
        if(id.contains("phong")||id.contains("pha"))return C_WHITE;
        if(id.contains("bang")||id.contains("dong"))return C_CYAN;
        if(id.contains("loi")||id.contains("sam"))return C_AMBER;
        return C_GOLD;
    }
    private Sound getSkillSound(String id) {
        if(id.contains("hoa")||id.contains("lua"))return Sound.ENTITY_BLAZE_SHOOT;
        if(id.contains("kiem"))return Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        if(id.contains("phap"))return Sound.BLOCK_ENCHANTMENT_TABLE_USE;
        if(id.contains("duoc")||id.contains("hoi"))return Sound.ENTITY_PLAYER_LEVELUP;
        if(id.contains("doc"))return Sound.ENTITY_WITCH_DRINK;
        if(id.contains("khien")||id.contains("bich"))return Sound.ENTITY_GENERIC_EXPLODE;
        if(id.contains("quy"))return Sound.ENTITY_ENDERMAN_STARE;
        if(id.contains("phong"))return Sound.ENTITY_BLAZE_AMBIENT;
        if(id.contains("loi")||id.contains("sam"))return Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
        return Sound.BLOCK_BEACON_POWER_SELECT;
    }
    private float getSkillPitch(String id) {
        if(id.contains("hoa"))return 0.7f; if(id.contains("kiem"))return 1.4f;
        if(id.contains("phong"))return 1.8f; if(id.contains("quy"))return 1.5f;
        if(id.contains("loi"))return 0.5f; return 1.0f;
    }
    private Color getRealmColor(CultivationStage s) {
        int ord=s.ordinal();
        if(ord>70)return C_AMBER; if(ord>52)return C_VIOLET;
        return switch(s.getRealm()) {
            case "Nhân Giới"->Color.fromRGB(180,180,255); case "Linh Giới"->Color.fromRGB(255,160,30);
            case "Tiên Giới"->Color.fromRGB(80,220,255);  case "Đế Giới"->Color.fromRGB(255,60,60);
            case "Thánh Giới"->C_WHITE; case "Thiên Giới","Thiên Đế"->C_GOLD;
            case "Thiên Tiên Đế"->Color.fromRGB(255,200,0); default->C_WHITE;
        };
    }
    private Color getRealmColor2(CultivationStage s) {
        int ord=s.ordinal();
        if(ord>70)return C_ROSE; if(ord>52)return C_CYAN;
        return switch(s.getRealm()) {
            case "Đế Giới"->C_ORANGE; case "Thánh Giới","Thiên Giới"->C_GOLD; default->C_GOLD;
        };
    }
    private Color getTocColor(com.tutien.toc.TocType toc) {
        return switch(toc) {
            case LONG_TOC->C_GOLD; case LINH_TOC->C_CYAN; case THAN_TOC->C_PURPLE;
            case YEU_TOC->C_PINK; case THU_HOA->C_ORANGE; default->{
                String n=toc.name();
                if(n.contains("PHUONG"))yield C_ROSE; if(n.contains("THUONG_THAN"))yield C_AMBER;
                if(n.contains("HON_HIEP"))yield C_VIOLET; if(n.contains("KIM_CUONG"))yield C_CYAN;
                if(n.contains("DAO_TOC"))yield C_TEAL; if(n.contains("THUY_TINH"))yield C_GOLD;
                yield C_SILVER;
            }
        };
    }
}