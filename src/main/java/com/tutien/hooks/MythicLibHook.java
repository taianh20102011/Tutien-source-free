package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.lingcan.LinhCanElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * MythicLibHook — Tích hợp TuTiên stats với MythicLib.
 * Dùng reflection — không crash nếu MythicLib không được cài.
 *
 * 6 custom stats đăng ký vào MythicLib:
 *   linh_khi_regen   — tốc độ tích LK
 *   phap_cong        — phép công (nhân damage skill)
 *   phap_phong       — phép phòng (giảm spell damage)
 *   linh_khi_max     — LK tối đa
 *   boss_dmg_reduce  — giảm damage từ Boss Arena
 *   crit_lk_mult     — nhân LK khi critical
 */
public class MythicLibHook implements Listener {

    private final TuTienPlugin plugin;
    private boolean available = false;
    private BukkitTask syncTask;

    public static final String STAT_LK_REGEN   = "linh_khi_regen";
    public static final String STAT_PHAP_CONG  = "phap_cong";
    public static final String STAT_PHAP_PHONG = "phap_phong";
    public static final String STAT_LK_MAX     = "linh_khi_max";
    public static final String STAT_BOSS_REDUCE= "boss_dmg_reduce";
    public static final String STAT_CRIT_LK    = "crit_lk_mult";

    public MythicLibHook(TuTienPlugin plugin) {
        this.plugin = plugin;
        tryLoad();
    }

    private void tryLoad() {
        if (!plugin.getConfig().getBoolean("mythiclib.enabled", true)) {
            plugin.getLogger().info("[Hook] MythicLib hook bị tắt trong config.yml.");
            return;
        }
        try {
            if (Bukkit.getPluginManager().getPlugin("MythicLib") == null) {
                plugin.getLogger().info("[Hook] MythicLib không tìm thấy — bỏ qua stats hook.");
                return;
            }
            Class<?> cls = Class.forName("io.lumine.mythic.lib.MythicLib");
            Object instance = cls.getMethod("inst").invoke(null);
            Object statManager = instance.getClass().getMethod("getStats").invoke(instance);
            Method registerDefault = statManager.getClass().getMethod("registerDefaultStat", String.class, double.class);
            registerDefaultSafely(registerDefault, statManager, STAT_LK_REGEN, 1.0D);
            registerDefaultSafely(registerDefault, statManager, STAT_PHAP_CONG, 1.0D);
            registerDefaultSafely(registerDefault, statManager, STAT_PHAP_PHONG, 1.0D);
            registerDefaultSafely(registerDefault, statManager, STAT_LK_MAX, 100.0D);
            registerDefaultSafely(registerDefault, statManager, STAT_BOSS_REDUCE, 0.0D);
            registerDefaultSafely(registerDefault, statManager, STAT_CRIT_LK, 1.0D);
            available = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            long intervalSeconds = Math.max(5L, plugin.getConfig().getLong("mythiclib.sync-interval-seconds", 30L));
            syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncOnlinePlayers, 20L, intervalSeconds * 20L);
            plugin.getLogger().info("[Hook] ✅ MythicLib kết nối — đã đăng ký 6 custom stats và bật đồng bộ mỗi " + intervalSeconds + " giây.");
        } catch (Exception e) {
            available = false;
            plugin.getLogger().warning("[Hook] MythicLib không tương thích hoặc không đăng ký được custom stats: " + e.getMessage());
        }
    }

    private void registerDefaultSafely(Method method, Object statManager, String id, double defaultValue) {
        try {
            method.invoke(statManager, id, defaultValue);
        } catch (Exception e) {
            // Reloads can encounter a stat already registered by this plugin version.
            // Keep the hook alive; sync each stat independently so one bad entry won't block the rest.
            plugin.getLogger().log(Level.FINE, "[MythicLibHook] Không thể đăng ký default stat " + id + ": " + e.getMessage());
        }
    }

    private void syncOnlinePlayers() {
        if (!available || plugin.getDataManager() == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerData data = plugin.getDataManager().getOrCreate(player);
                syncStats(player, data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[MythicLibHook] Không thể đồng bộ stats của " + player.getName(), e);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!available) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline() && plugin.getDataManager() != null) {
                syncStats(event.getPlayer(), plugin.getDataManager().getOrCreate(event.getPlayer()));
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Intentionally keep no per-player cache; stat values are rebuilt from PlayerData at next join.
    }

    public void shutdown() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
    }

    public boolean isAvailable() { return available; }

    // ── Push TuTiên stats → MythicLib stat cache ──────────────────
    public void syncStats(Player player, PlayerData data) {
        if (!available) return;
        if (player == null || data == null) return;
        pushStatSafely(player, STAT_LK_REGEN, calculateLkRegen(data), "linh-khi-regen");
        pushStatSafely(player, STAT_PHAP_CONG, calculatePhapCong(data), "phap-cong");
        pushStatSafely(player, STAT_PHAP_PHONG, calculatePhapPhong(data), "phap-phong");
        pushStatSafely(player, STAT_LK_MAX, (double) data.getMaxLinhKhi(), "linh-khi-max");
        pushStatSafely(player, STAT_BOSS_REDUCE, calculateBossReduce(data), "boss-dmg-reduce");
        pushStatSafely(player, STAT_CRIT_LK, calculateCritLk(data), "crit-lk-mult");
    }

    /** Đọc bonus từ MythicLib (do MM equipment cộng thêm) */
    public double readExternalStat(Player player, String statId) {
        if (!available) return 0.0;
        try {
            Class<?> mmPD = Class.forName("io.lumine.mythic.lib.api.player.MMPlayerData");
            Object pd  = mmPD.getMethod("get", Player.class).invoke(null, player);
            Object sm  = pd.getClass().getMethod("getStatMap").invoke(pd);
            return (double) sm.getClass().getMethod("getStat", String.class).invoke(sm, statId);
        } catch (Exception e) { return 0.0; }
    }

    private void pushStatSafely(Player player, String statId, double value, String weightKey) {
        try {
            double weight = plugin.getConfig().getDouble("mythiclib.stat-weights." + weightKey, 1.0D);
            pushStat(player, statId, value * Math.max(0.0D, Math.min(100.0D, weight)));
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[MythicLibHook] Stat '" + statId + "' sync lỗi: " + e.getMessage());
        }
    }

    private void pushStat(Player player, String statId, double value) throws Exception {
        Class<?> mmPD = Class.forName("io.lumine.mythic.lib.api.player.MMPlayerData");
        Object pd   = mmPD.getMethod("get", Player.class).invoke(null, player);
        Object sm   = pd.getClass().getMethod("getStatMap").invoke(pd);
        Object inst = sm.getClass().getMethod("getInstance", String.class).invoke(sm, statId);
        inst.getClass().getMethod("setBase", double.class).invoke(inst, value);
    }

    // ── Stat formulas ──────────────────────────────────────────────

    private double calculateLkRegen(PlayerData data) {
        double base = 1.0 + data.getStage().ordinal() * 0.035;
        // Linh Căn bonus
        List<LinhCanElement> lc = data.getLinhCan(); // ← đúng tên method
        if (lc != null) {
            for (LinhCanElement e : lc) base += e.getLkSpeedBonus();
        }
        // Toc
        if (data.getToc()    != null) base *= data.getToc().getLkMult();
        return Math.min(base, 3.5);
    }

    private double calculatePhapCong(PlayerData data) {
        double base = 1.0 + data.getStage().ordinal() * 0.04;
        List<LinhCanElement> lc = data.getLinhCan();
        if (lc != null) {
            for (LinhCanElement e : lc) {
                base += e.isSieuCap() ? 0.25 : 0.08;
            }
        }
        if (data.getToc()    != null) base *= data.getToc().getAtkMult();
        return Math.min(base, 5.0);
    }

    private double calculatePhapPhong(PlayerData data) {
        double base = 1.0 + data.getStage().ordinal() * 0.028;
        if (data.getToc()    != null) base *= data.getToc().getDefMult();
        return Math.min(base, 4.0);
    }

    private double calculateBossReduce(PlayerData data) {
        double base = data.getStage().ordinal() * 0.005;
        if (data.getToc() != null) {
            String tn = data.getToc().name();
            if      (tn.contains("DAO_TOC"))   base += 0.20;
            else if (tn.contains("KIM_CUONG")) base += 0.25;
            else if (tn.contains("LONG"))      base += 0.12;
        }
        return Math.min(base, 0.60);
    }

    private double calculateCritLk(PlayerData data) {
        double base = 1.0;
        if (data.getToc() != null && data.getToc().name().contains("THUONG_THAN"))
            base += 0.5;
        return Math.min(base, 2.5);
    }
}
