package com.tutien.worldboss;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;

/**
 * BOSS TỐI THƯỢNG — world boss tự động xuất hiện theo chu kỳ/giờ cố định,
 * dùng MythicMobs (qua MythicMobsHook đã có sẵn) cho model + skill riêng.
 *
 * THIẾT KẾ QUAN TRỌNG (khác BloodMoonManager): thưởng "Boss Tối Thượng
 * kill" cho MỌI người chơi đóng góp đủ % sát thương (không chỉ người ra
 * đòn cuối) — vì đây là world boss cho nhiều người đánh chung, thưởng
 * theo kiểu "ai đấm nhát cuối mới có" sẽ rất bất công và làm nản lòng
 * người tham gia từ đầu. Ngưỡng % được cấu hình ở
 * world-boss.min-damage-percent-for-credit.
 *
 * NHẬN DIỆN BOSS: dùng UUID entity spawn ra TỪ CHÍNH manager này (biến
 * currentBossUuid), KHÔNG dùng tên hiển thị — nên không thể giả mạo bằng
 * cách đặt tên (name tag) cho quái thường như lỗi cũ trong CombatListener.
 */
public class WorldBossManager implements Listener {

    private final TuTienPlugin plugin;

    private UUID currentBossUuid = null;
    private final Map<UUID, Double> damageContribution = new HashMap<>();
    private BossBar bossBar = null;
    private BukkitTask timeoutTask = null;
    private BukkitTask barUpdateTask = null;

    private final File stateFile;
    private long lastSpawnMillis = 0L;
    private String lastFixedSlot = ""; // "yyyy-MM-dd HH:mm" cua lan spawn gan nhat (che do fixed-times)

    public WorldBossManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "worldboss_state.yml");
        loadState();
    }

    // ================================================================
    //  KHOI DONG LICH TRINH
    // ================================================================

    public void start() {
        if (!cfg().getBoolean("world-boss.enabled", true)) return;

        String mode = cfg().getString("world-boss.spawn-mode", "interval");
        if (mode.equalsIgnoreCase("fixed-times")) {
            // Kiem tra moi phut xem co khop 1 trong cac gio cau hinh khong.
            new BukkitRunnable() {
                @Override public void run() { checkFixedTimeSlot(); }
            }.runTaskTimer(plugin, 20L, 20L * 60L);
        } else {
            long intervalMillis = cfg().getLong("world-boss.interval-hours", 6) * 3_600_000L;
            long elapsed = System.currentTimeMillis() - lastSpawnMillis;
            long delayMillis = Math.max(30_000L, intervalMillis - elapsed); // it nhat 30s de server on dinh sau khi khoi dong
            long delayTicks = delayMillis / 50L;
            long periodTicks = intervalMillis / 50L;

            new BukkitRunnable() {
                @Override public void run() { trySpawn(); }
            }.runTaskTimer(plugin, delayTicks, periodTicks);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void checkFixedTimeSlot() {
        List<String> times = cfg().getStringList("world-boss.fixed-times");
        if (times.isEmpty()) return;

        LocalTime now = LocalTime.now();
        String nowHm = String.format("%02d:%02d", now.getHour(), now.getMinute());
        if (!times.contains(nowHm)) return;

        String today = java.time.LocalDate.now().toString();
        String slotKey = today + " " + nowHm;
        if (slotKey.equals(lastFixedSlot)) return; // da spawn cho khung gio nay roi

        lastFixedSlot = slotKey;
        saveState();
        trySpawn();
    }

    // ================================================================
    //  SPAWN
    // ================================================================

    public void trySpawn() {
        if (currentBossUuid != null) return; // Da co 1 con dang song, khong spawn chong

        if (plugin.getMythicMobsHook() == null || !plugin.getMythicMobsHook().isAvailable()) {
            plugin.getLogger().warning("[WorldBoss] MythicMobs chưa sẵn sàng - bỏ qua lần spawn Boss Tối Thượng này.");
            return;
        }

        String worldName = cfg().getString("world-boss.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[WorldBoss] World '" + worldName + "' không tồn tại - kiểm tra lại config world-boss.world.");
            return;
        }

        double x = cfg().getDouble("world-boss.x", 0.5);
        double y = cfg().getDouble("world-boss.y", 100);
        double z = cfg().getDouble("world-boss.z", 0.5);
        Location loc = new Location(world, x, y, z);

        if (cfg().getBoolean("world-boss.find-safe-ground", true)) {
            loc = findSafeGround(loc);
        }

        String mobId = cfg().getString("world-boss.mob-id", "TT_BossToiThuong");
        int level = cfg().getInt("world-boss.mob-level", 5);
        final Location spawnLoc = loc;

        int warnSeconds = cfg().getInt("world-boss.warning-seconds-before", 60);
        if (warnSeconds <= 0) {
            doSpawn(spawnLoc, mobId, level);
            return;
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§4§l☠ BOSS TỐI THƯỢNG SẮP XUẤT HIỆN! §7(" + warnSeconds + "s)");
        Bukkit.broadcastMessage("§7Vị trí: §e" + formatLoc(spawnLoc) + " §7- §fchuẩn bị tập hợp!");
        Bukkit.broadcastMessage("");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> doSpawn(spawnLoc, mobId, level), warnSeconds * 20L);
    }

    private void doSpawn(Location loc, String mobId, int level) {
        LivingEntity boss = plugin.getMythicMobsHook().spawnGenericMob(mobId, loc, level);
        if (boss == null) {
            plugin.getLogger().warning("[WorldBoss] Spawn thất bại (mobId=\"" + mobId + "\") - kiểm tra file "
                    + "plugins/MythicMobs/Mobs/TT_BossToiThuong.yml đã được cài đúng ID chưa.");
            return;
        }

        currentBossUuid = boss.getUniqueId();
        damageContribution.clear();
        lastSpawnMillis = System.currentTimeMillis();
        saveState();

        createBossBar(boss);

        Bukkit.broadcastMessage("§4§l⚔ BOSS TỐI THƯỢNG ĐÃ XUẤT HIỆN! ⚔");
        Bukkit.broadcastMessage("§7Vị trí: §e" + formatLoc(loc));
        Bukkit.broadcastMessage("§7Đóng góp đủ sát thương sẽ được tính công (không cần ra đòn cuối)!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        }

        int timeoutMin = cfg().getInt("world-boss.timeout-minutes", 45);
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, this::onTimeout, timeoutMin * 60L * 20L);

        barUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentBossUuid == null) { cancel(); return; }
                Entity e = Bukkit.getEntity(currentBossUuid);
                if (!(e instanceof LivingEntity le) || le.isDead()) { cancel(); return; }
                updateBossBar(le);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /** Do xuong (roi len) tu vi tri cau hinh de tim mat dat trong, tranh spawn ket trong khoi. */
    private Location findSafeGround(Location start) {
        Location loc = start.clone();
        for (int i = 0; i < 40; i++) {
            Block feet = loc.getBlock();
            Block below = loc.clone().subtract(0, 1, 0).getBlock();
            if (!feet.getType().isSolid() && below.getType().isSolid()) return loc;
            loc.subtract(0, 1, 0);
            if (loc.getY() < loc.getWorld().getMinHeight() + 1) break;
        }
        return start; // Khong tim duoc, dung toa do goc
    }

    // ================================================================
    //  THEO DOI SAT THUONG
    // ================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (currentBossUuid == null) return;
        if (!event.getEntity().getUniqueId().equals(currentBossUuid)) return;

        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker == null) return;

        damageContribution.merge(attacker.getUniqueId(), event.getFinalDamage(), Double::sum);
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    // ================================================================
    //  BOSS CHET -> CHIA CONG
    // ================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent event) {
        if (currentBossUuid == null) return;
        if (!event.getEntity().getUniqueId().equals(currentBossUuid)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        double totalDamage = damageContribution.values().stream().mapToDouble(Double::doubleValue).sum();
        double minPercent = cfg().getDouble("world-boss.min-damage-percent-for-credit", 0.02);
        long linhKhiReward = cfg().getLong("world-boss.linh-khi-reward", 5000);
        long tienReward = cfg().getLong("world-boss.tien-reward", 20000);

        List<String> credited = new ArrayList<>();
        UUID mvpUuid = null;
        double mvpDamage = -1;

        for (Map.Entry<UUID, Double> e : damageContribution.entrySet()) {
            if (e.getValue() > mvpDamage) { mvpDamage = e.getValue(); mvpUuid = e.getKey(); }

            double percent = totalDamage > 0 ? e.getValue() / totalDamage : 0;
            if (percent < minPercent) continue;

            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue; // Da thoat server giua tran - khong the trao thuong truc tiep, bo qua

            PlayerData data = plugin.getDataManager().get(p);
            if (data == null) continue;

            data.addBossToiThuongKill();
            data.addLinhKhi(linhKhiReward);
            data.addTien(tienReward);
            credited.add(p.getName());

            p.sendTitle("§4§l✦ BOSS TỐI THƯỢNG GỤC NGÃ ✦", "§7Bạn nhận công đánh boss!", 10, 80, 20);
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§4§l☠ BOSS TỐI THƯỢNG ĐÃ BỊ TIÊU DIỆT! ☠");
        if (!credited.isEmpty()) {
            Bukkit.broadcastMessage("§7Được công: §e" + String.join("§7, §e", credited));
        }
        if (mvpUuid != null) {
            OfflinePlayer mvp = Bukkit.getOfflinePlayer(mvpUuid);
            Bukkit.broadcastMessage("§6MVP (sát thương cao nhất): §e" + mvp.getName());
        }
        Bukkit.broadcastMessage("");

        cleanupAfterFight();
    }

    private void onTimeout() {
        if (currentBossUuid == null) return;
        Entity e = Bukkit.getEntity(currentBossUuid);
        if (e != null) e.remove();

        Bukkit.broadcastMessage("§7☠ Boss Tối Thượng đã bỏ đi vì không ai hạ được trong thời gian quy định...");
        cleanupAfterFight();
    }

    private void cleanupAfterFight() {
        currentBossUuid = null;
        damageContribution.clear();
        if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; }
        if (barUpdateTask != null) { barUpdateTask.cancel(); barUpdateTask = null; }
        removeBossBar();
    }

    // ================================================================
    //  BOSSBAR
    // ================================================================

    private void createBossBar(LivingEntity boss) {
        bossBar = BossBar.bossBar(
                Component.text("☠ BOSS TỐI THƯỢNG").color(TextColor.color(200, 0, 0)),
                1.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bossBar);
        updateBossBar(boss);
    }

    private void updateBossBar(LivingEntity boss) {
        if (bossBar == null) return;
        float pct = (float) Math.max(0, Math.min(1, boss.getHealth() / boss.getMaxHealth()));
        bossBar.progress(pct);
        bossBar.name(Component.text("☠ BOSS TỐI THƯỢNG — HP: " + (int) boss.getHealth() + "/" + (int) boss.getMaxHealth())
                .color(TextColor.color(200, 0, 0)));
    }

    private void removeBossBar() {
        if (bossBar == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bossBar);
        bossBar = null;
    }

    // ================================================================
    //  LENH ADMIN (goi tu AdminCommand)
    // ================================================================

    public void forceSpawn() {
        trySpawn();
    }

    public void forceDespawn() {
        if (currentBossUuid == null) return;
        Entity e = Bukkit.getEntity(currentBossUuid);
        if (e != null) e.remove();
        cleanupAfterFight();
    }

    public String status() {
        if (currentBossUuid == null) return "§7Không có Boss Tối Thượng nào đang hoạt động.";
        Entity e = Bukkit.getEntity(currentBossUuid);
        if (!(e instanceof LivingEntity le)) return "§cBoss đang track nhưng entity không tồn tại (lỗi).";
        return "§aĐang hoạt động §7- HP: §e" + (int) le.getHealth() + "/" + (int) le.getMaxHealth()
                + " §7- Người tham gia: §e" + damageContribution.size();
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private org.bukkit.configuration.file.FileConfiguration cfg() {
        return plugin.getConfig();
    }

    private String formatLoc(Location loc) {
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    private void loadState() {
        if (!stateFile.exists()) return;
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(stateFile);
            lastSpawnMillis = yml.getLong("last-spawn-millis", 0L);
            lastFixedSlot = yml.getString("last-fixed-slot", "");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[WorldBoss] Khong doc duoc worldboss_state.yml: " + e.getMessage());
        }
    }

    private void saveState() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("last-spawn-millis", lastSpawnMillis);
        yml.set("last-fixed-slot", lastFixedSlot);
        try {
            stateFile.getParentFile().mkdirs();
            yml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[WorldBoss] Khong luu duoc worldboss_state.yml: " + e.getMessage());
        }
    }
}
