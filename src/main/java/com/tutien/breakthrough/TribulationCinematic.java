package com.tutien.breakthrough;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TribulationCinematic — Hệ thống hiệu ứng độ kiếp điện ảnh (Cinematic Tribulation).
 *
 * Tính năng chính:
 * 1. Mid-Air Ascension: Nâng bổng tu sĩ lơ lửng ngự không (3.5 - 5.0 block), kèm Pháp Trận Thái Cực
 *    Lôi Đình và luồng lốc xoáy lôi điện cuộn từ mặt đất lên thân thể.
 * 2. Atmospheric Storm: Bầu trời nổi giông bão sấm chớp, tầng Lôi Vân đen kịt cuồn cuộn trên đỉnh đầu.
 * 3. Vạn Lôi Oanh Đỉnh: Sét khổng lồ giáng trúng tu sĩ, 2-3 sét phụ oanh tạc mặt đất kèm sóng xung kích lôi điện.
 * 4. Soft Landing: Tiên quang phá mây, mưa tạnh mây tan, Đóa Sen Vàng nở rộ dưới mặt đất,
 *    hiệu ứng Rơi Chậm và miễn trừ 100% sát thương rơi ngã.
 *
 * Tác giả: Hanita - Phạm Ngọc Tài Anh
 */
public class TribulationCinematic implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    /** Session đang độ kiếp của từng người chơi */
    private final Map<UUID, CinematicSession> activeSessions = new ConcurrentHashMap<>();

    /** Thời hạn miễn sát thương rơi ngã (epoch ms) */
    private final Map<UUID, Long> fallImmunity = new ConcurrentHashMap<>();

    public TribulationCinematic(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Thông tin phiên độ kiếp điện ảnh của 1 người chơi
     */
    public static class CinematicSession {
        final UUID playerUuid;
        final Location groundLoc;
        final Location hoverLoc;
        final double elevation;
        final boolean originalStorm;
        final boolean originalThundering;
        BukkitTask hoverTask;
        int tickCount = 0;

        public CinematicSession(UUID playerUuid, Location groundLoc, Location hoverLoc,
                                double elevation, boolean originalStorm, boolean originalThundering) {
            this.playerUuid = playerUuid;
            this.groundLoc = groundLoc;
            this.hoverLoc = hoverLoc;
            this.elevation = elevation;
            this.originalStorm = originalStorm;
            this.originalThundering = originalThundering;
        }
    }

    // ================================================================
    //  1. KHỞI ĐỘNG HIỆU ỨNG ĐỘ KIẾP (MID-AIR ASCENSION & ATMOSPHERIC STORM)
    // ================================================================

    public void startCinematic(Player player, CultivationStage stage, int totalWaves) {
        if (player == null || !player.isOnline()) return;

        // Dọn dẹp session cũ nếu có
        cleanup(player.getUniqueId());

        Location groundLoc = player.getLocation().clone();
        World w = player.getWorld();

        // 1. Kiểm tra trần nhà / vật cản an toàn bên trên (an toàn từ 3.5 đến 5.0 block)
        double elevationCalc = 4.0;
        for (double dy = 1.0; dy <= 5.5; dy += 0.5) {
            Location check = groundLoc.clone().add(0, dy + 1.8, 0); // Kiểm tra qua đỉnh đầu
            if (check.getBlock().getType().isSolid()) {
                elevationCalc = Math.max(1.8, dy - 0.5);
                break;
            }
        }
        final double targetElevation = elevationCalc;
        final Location hoverLoc = groundLoc.clone().add(0, targetElevation, 0);

        // 2. Kích hoạt Giông Bão Sấm Sét bao phủ thế giới
        boolean wasStorm = w.hasStorm();
        boolean wasThundering = w.isThundering();
        w.setStorm(true);
        w.setThundering(true);
        w.setWeatherDuration(72000);
        w.setThunderDuration(72000);

        CinematicSession session = new CinematicSession(
                player.getUniqueId(), groundLoc, hoverLoc, targetElevation, wasStorm, wasThundering);

        // Âm thanh khởi động Thiên Kiếp huyền bí
        w.playSound(groundLoc, Sound.AMBIENT_CAVE, 1.8f, 0.5f);
        w.playSound(groundLoc, Sound.ITEM_TRIDENT_THUNDER, 2.0f, 0.7f);

        // 3. Task lơ lửng ngự không & hiệu ứng hình ảnh liên tục
        session.hoverTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                t++;
                session.tickCount = t;

                // Giai đoạn 1: 20 tick đầu (1 giây) nâng bổng tu sĩ nhẹ nhàng lên trời
                if (t <= 20) {
                    double progress = t / 20.0;
                    double currentY = groundLoc.getY() + targetElevation * progress;
                    Location liftLoc = new Location(w, groundLoc.getX(), currentY, groundLoc.getZ(),
                            player.getLocation().getYaw(), player.getLocation().getPitch());
                    player.teleport(liftLoc);
                    player.setFallDistance(0f);
                    player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

                    // Hạt gió lốc xoáy khi đang nâng bổng
                    double ang = t * 0.4;
                    w.spawnParticle(Particle.CLOUD, liftLoc.clone().add(Math.cos(ang) * 0.8, -0.5, Math.sin(ang) * 0.8),
                            2, 0.1, 0.1, 0.1, 0.02);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, liftLoc.clone().add(Math.cos(-ang) * 0.8, -0.3, Math.sin(-ang) * 0.8),
                            2, 0.1, 0.1, 0.1, 0.03);
                    return;
                }

                // Giai đoạn 2: Lơ lửng ngự không vững chãi giữa trời
                player.setFallDistance(0f);
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

                // Giữ vị trí không cho rơi rớt, cho phép xoay góc nhìn
                if (t % 2 == 0) {
                    Location cur = player.getLocation();
                    // Nếu người chơi bị xê dịch khỏi tâm quá 1.5 block thì kéo nhẹ về
                    double distSq = cur.distanceSquared(hoverLoc);
                    if (distSq > 1.2) {
                        Location lockLoc = new Location(w, hoverLoc.getX(), hoverLoc.getY(), hoverLoc.getZ(),
                                cur.getYaw(), cur.getPitch());
                        player.teleport(lockLoc);
                    }
                }

                // A. TẦNG LÔI VÂN ĐEN KỊT TRÊN ĐỈNH ĐẦU (cách 8 block)
                if (t % 2 == 0) {
                    renderOverheadThunderCloud(w, hoverLoc, t);
                }

                // B. PHÁP TRẬN THÁI CỰC LÔI ĐÌNH DƯỚI CHÂN
                renderTaijiThunderArray(w, groundLoc, t);

                // C. LUỒNG LỐC XOÁY LÔI ĐIỆN NÂNG ĐỠ THÂN THỂ
                renderLightningVortex(w, groundLoc, targetElevation, t);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeSessions.put(player.getUniqueId(), session);
    }

    // ================================================================
    //  2. VẠN LÔI OANH ĐỈNH TRONG MỖI ĐỢT SÓNG LÔI KIẾP (WAVE STRIKE)
    // ================================================================

    public void onWaveStrike(Player player, int wave, int totalWaves) {
        if (player == null || !player.isOnline()) return;

        CinematicSession session = activeSessions.get(player.getUniqueId());
        Location strikeLoc = session != null ? session.hoverLoc : player.getLocation();
        Location groundLoc = session != null ? session.groundLoc : player.getLocation();
        World w = player.getWorld();

        // 1. Tia sét Thiên Lôi khổng lồ giáng thẳng vào thân thể đang lơ lửng
        w.strikeLightningEffect(strikeLoc);

        // 2. Kèm 2-3 tia sét phụ oanh tạc xung quanh mặt đất (bán kính 3-6 block)
        int subLightningCount = 2 + random.nextInt(2); // 2 hoặc 3 tia
        for (int i = 0; i < subLightningCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 3.0 + random.nextDouble() * 3.5;
            Location subLoc = groundLoc.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            subLoc.setY(w.getHighestBlockYAt(subLoc));
            w.strikeLightningEffect(subLoc);
        }

        // 3. Âm thanh sấm rền dội uy mãnh
        float pitch = 0.65f + ((float) wave / Math.max(1, totalWaves)) * 0.45f;
        w.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.8f, pitch);
        w.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.2f, 1.0f);

        // 4. Bung tỏa sóng xung kích lôi điện cực đại
        renderLightningShockwave(w, strikeLoc);
    }

    // ================================================================
    //  3. TIÊN QUANG PHÁ MÂY & HẠ CÁNH AN TOÀN (SUCCESS SOFT LANDING)
    // ================================================================

    public void onSuccess(Player player, CultivationStage newStage) {
        if (player == null || !player.isOnline()) return;

        CinematicSession session = activeSessions.remove(player.getUniqueId());
        Location groundLoc = session != null ? session.groundLoc : player.getLocation();
        World w = player.getWorld();

        if (session != null && session.hoverTask != null) {
            session.hoverTask.cancel();
        }

        // 1. Mây tan mưa tạnh, trời quang mây tạnh
        if (activeSessions.isEmpty()) {
            w.setStorm(false);
            w.setThundering(false);
        }

        // 2. Vụ nổ Kim Quang cực đại tỏa ra
        Location pLoc = player.getLocation().add(0, 1.0, 0);
        w.spawnParticle(Particle.FLASH, pLoc, 2);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, pLoc, 120, 2.5, 2.0, 2.5, 0.4);
        w.spawnParticle(Particle.FIREWORK, pLoc, 60, 2.0, 1.8, 2.0, 0.25);
        w.spawnParticle(Particle.END_ROD, pLoc, 40, 1.5, 1.5, 1.5, 0.15);
        w.playSound(pLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.8f, 1.1f);
        w.playSound(pLoc, Sound.ITEM_TOTEM_USE, 1.5f, 1.2f);

        // 3. Dưới mặt đất nở rộ ĐÓA SEN VÀNG THẦN THÁNH
        renderGoldenLotus(w, groundLoc);

        // 4. Tu sĩ nhận hiệu ứng Rơi Chậm (SLOW_FALLING) và miễn trừ sát thương ngã
        grantSoftLanding(player);
    }

    // ================================================================
    //  4. THẤT BẠI HOẶC THOÁT GAME (FAIL / CANCEL CLEANUP)
    // ================================================================

    public void onFail(Player player) {
        if (player == null) return;
        CinematicSession session = activeSessions.remove(player.getUniqueId());
        if (session != null && session.hoverTask != null) {
            session.hoverTask.cancel();
        }

        if (activeSessions.isEmpty() && player.isOnline()) {
            player.getWorld().setStorm(false);
            player.getWorld().setThundering(false);
        }

        if (player.isOnline()) {
            // Dù thất bại vẫn cho hạ cánh an toàn tránh bị rơi chết do ngã
            grantSoftLanding(player);
        }
    }

    public void cleanup(UUID playerUuid) {
        CinematicSession session = activeSessions.remove(playerUuid);
        if (session != null && session.hoverTask != null) {
            session.hoverTask.cancel();
        }
    }

    public void cleanupAll() {
        for (CinematicSession session : activeSessions.values()) {
            if (session.hoverTask != null) session.hoverTask.cancel();
        }
        activeSessions.clear();
        fallImmunity.clear();
    }

    private void grantSoftLanding(Player player) {
        // Hiệu ứng rơi chậm 12 giây
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 240, 1, false, false, true));

        // Miễn 100% sát thương rơi ngã trong 25 giây
        fallImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 25_000L);
    }

    public boolean isFallImmune(UUID uuid) {
        Long expire = fallImmunity.get(uuid);
        return expire != null && expire > System.currentTimeMillis();
    }

    // ================================================================
    //  5. CÁC HÀM HIỆU ỨNG HÌNH HỌC (PARTICLE EFFECTS)
    // ================================================================

    /**
     * Tầng Lôi Vân đen kịt cuồn cuộn trên đỉnh đầu (cách 8 block)
     */
    private void renderOverheadThunderCloud(World w, Location hoverLoc, int tick) {
        Location cloudCenter = hoverLoc.clone().add(0, 8.0, 0);

        // Tạo đĩa mây đường kính 10 block (bán kính 5.0)
        double radius = 4.8;
        for (int i = 0; i < 7; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(random.nextDouble()) * radius;
            Location p = cloudCenter.clone().add(Math.cos(angle) * r, (random.nextDouble() - 0.5) * 0.8, Math.sin(angle) * r);

            // Mây đen dầy đặc
            w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.1, 0.2, 0.01);
            if (i % 2 == 0) {
                w.spawnParticle(Particle.SQUID_INK, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Chớp sáng điện tích cuộn xoáy trong lôi vân
        if (tick % 4 == 0) {
            double a = (tick * 0.2) % (Math.PI * 2);
            Location sparkLoc = cloudCenter.clone().add(Math.cos(a) * 3.0, 0, Math.sin(a) * 3.0);
            w.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 4, 0.4, 0.2, 0.4, 0.08);
        }
    }

    /**
     * Pháp Trận Thái Cực Lôi Đình dưới chân
     */
    private void renderTaijiThunderArray(World w, Location groundLoc, int tick) {
        Location base = groundLoc.clone().add(0, 0.15, 0);
        double radius = 2.6;
        double rot = (tick * 0.06) % (Math.PI * 2);

        // Vòng ngoài pháp trận
        for (int i = 0; i < 24; i += 2) {
            double a = rot + (Math.PI * 2 * i / 24);
            Location rim = base.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            w.spawnParticle(Particle.ELECTRIC_SPARK, rim, 1, 0, 0, 0, 0.01);
            if (i % 4 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, rim, 1, 0, 0, 0, 0.01);
            }
        }

        // Đường cong chữ S Thái Cực ở tâm
        for (double t = -1.2; t <= 1.2; t += 0.3) {
            double sX = t;
            double sZ = Math.sin(t * 1.8) * 0.6;
            // Xoay theo góc rot
            double rx = sX * Math.cos(rot) - sZ * Math.sin(rot);
            double rz = sX * Math.sin(rot) + sZ * Math.cos(rot);
            Location curveLoc = base.clone().add(rx, 0, rz);
            w.spawnParticle(Particle.ELECTRIC_SPARK, curveLoc, 1, 0, 0, 0, 0);
        }

        // 2 Mắt Thái Cực (Âm Dương)
        double eyeDist = 1.1;
        double eye1X = Math.cos(rot + Math.PI / 2) * eyeDist;
        double eye1Z = Math.sin(rot + Math.PI / 2) * eyeDist;
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(eye1X, 0, eye1Z), 1, 0, 0, 0, 0);

        double eye2X = Math.cos(rot - Math.PI / 2) * eyeDist;
        double eye2Z = Math.sin(rot - Math.PI / 2) * eyeDist;
        w.spawnParticle(Particle.FIREWORK, base.clone().add(eye2X, 0, eye2Z), 1, 0, 0, 0, 0);
    }

    /**
     * Luồng lốc xoáy lôi điện cuộn từ mặt đất lên nâng đỡ thân thể
     */
    private void renderLightningVortex(World w, Location groundLoc, double height, int tick) {
        // Tạo 2 luồng xoắn ốc (vortex) cuộn ngược lên
        for (double h = 0.2; h <= height; h += 0.45) {
            double progress = h / height;
            double r = 0.5 + 0.45 * Math.sin(progress * Math.PI); // Bán kính phễu lốc xoáy

            // Luồng 1
            double a1 = (tick * 0.22) + (h * 1.6);
            Location v1 = groundLoc.clone().add(Math.cos(a1) * r, h, Math.sin(a1) * r);
            w.spawnParticle(Particle.ELECTRIC_SPARK, v1, 1, 0.05, 0.05, 0.05, 0.02);

            // Luồng 2 (đối xứng 180 độ)
            double a2 = a1 + Math.PI;
            Location v2 = groundLoc.clone().add(Math.cos(a2) * r, h, Math.sin(a2) * r);
            w.spawnParticle(Particle.CRIT, v2, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    /**
     * Sóng xung kích lôi điện khi sét đánh
     */
    private void renderLightningShockwave(World w, Location center) {
        Location mid = center.clone().add(0, 0.8, 0);
        w.spawnParticle(Particle.FLASH, mid, 1);
        w.spawnParticle(Particle.ELECTRIC_SPARK, mid, 35, 1.2, 0.5, 1.2, 0.25);
        w.spawnParticle(Particle.SWEEP_ATTACK, mid, 3, 0.8, 0.2, 0.8, 0);

        // Vòng tròn sóng lan tỏa nhanh
        new BukkitRunnable() {
            double r = 0.5;
            int step = 0;

            @Override
            public void run() {
                if (step++ >= 5) { cancel(); return; }
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16;
                    Location ringLoc = mid.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, ringLoc, 1, 0, 0, 0, 0.02);
                }
                r += 0.7;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Đóa Sen Vàng thần thánh nở rộ dưới mặt đất
     */
    private void renderGoldenLotus(World w, Location groundLoc) {
        Location center = groundLoc.clone().add(0, 0.15, 0);

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step++ >= 60) { cancel(); return; } // Nở rộ và duy trì trong 3 giây (60 tick)

                double progress = Math.min(1.0, step / 20.0);
                double baseRadius = 1.8 * progress;

                // Vẽ 8 cánh hoa sen nở bung
                for (int i = 0; i < 48; i++) {
                    double theta = Math.PI * 2 * i / 48;
                    // Đường cong hình cánh sen 8 cánh: r = R * (1 + 0.35 * cos(8 * theta))
                    double r = baseRadius * (1.0 + 0.35 * Math.cos(8 * theta));
                    Location petalLoc = center.clone().add(Math.cos(theta) * r, 0.05, Math.sin(theta) * r);

                    if (step % 2 == 0) {
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, petalLoc, 1, 0, 0, 0, 0);
                    }
                    if (step % 4 == 0) {
                        w.spawnParticle(Particle.END_ROD, petalLoc, 1, 0, 0.02, 0, 0.01);
                    }
                }

                // Nhụy sen vàng rực ở tâm
                if (step % 3 == 0) {
                    w.spawnParticle(Particle.FIREWORK, center.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.03);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ================================================================
    //  6. EVENT LISTENERS: CHỐNG RƠI NGÃ VÀ CLEANUP
    // ================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player player) {
            if (isFallImmune(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }
}
