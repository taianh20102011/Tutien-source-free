package com.tutien.karma;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.data.DaoPath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * KarmaManager — Hệ thống Công Đức & Nghiệp Lực.
 *
 * Công Đức (cong_duc ≥ 0):  giúp người mới, giết mob, làm nhiệm vụ tốt
 *   → Breakthrough difficulty -max 30%
 *
 * Nghiệp Lực (nghiep_luc ≥ 0): PK, phá hoại, giết người
 *   → ATK +max 40% nhưng breakthrough +max 80% khó hơn
 *
 * Hai chỉ số độc lập, lưu trong PlayerData extra map.
 */
public class KarmaManager implements Listener {

    private final TuTienPlugin plugin;

    public static final String KEY_CONG_DUC  = "karma_cong_duc";
    public static final String KEY_NGHIEP_LUC = "karma_nghiep_luc";

    // Mức karma tối đa
    public static final long MAX_CONG_DUC  = 100_000L;
    public static final long MAX_NGHIEP_LUC= 100_000L;

    public KarmaManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Tự giảm nghiệp lực dần theo thời gian (1 điểm/phút)
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData d = plugin.getDataManager().get(p);
                    long nl = getNghiepLuc(d);
                    if (nl > 0) setNghiepLuc(d, Math.max(0, nl - 1));
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // every 60s
    }

    // ── Event: giết mob → +Công Đức ──────────────────────────────
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (event.getEntity() instanceof Player) return; // PvP handled below
        PlayerData d = plugin.getDataManager().get(player);
        addCongDuc(d, 2L); // +2 Công Đức mỗi mob
    }

    // ── Event: giết người chơi → +Nghiệp Lực ────────────────────
    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return;

        PlayerData kData = plugin.getDataManager().get(killer);
        PlayerData vData = plugin.getDataManager().get(victim);

        // Killer nhận Nghiệp Lực
        addNghiepLuc(kData, 500L);
        // Victim nếu là Ma Đạo → killer ít nghiệp hơn
        if (vData.getDaoPath() == DaoPath.MA_DAO) addNghiepLuc(kData, -200L);

        killer.sendMessage(plugin.prefix() + "§4☠ Nghiệp Lực +500! Tổng: §c" + getNghiepLuc(kData));
    }

    // ── API ──────────────────────────────────────────────────────
    public long getCongDuc(PlayerData d)   { return d.getExtraLong(KEY_CONG_DUC, 0L); }
    public long getNghiepLuc(PlayerData d) { return d.getExtraLong(KEY_NGHIEP_LUC, 0L); }

    public void addCongDuc(PlayerData d, long amt) {
        d.setExtraLong(KEY_CONG_DUC, Math.min(MAX_CONG_DUC, getCongDuc(d) + amt));
    }
    public void addNghiepLuc(PlayerData d, long amt) {
        d.setExtraLong(KEY_NGHIEP_LUC, Math.max(0, Math.min(MAX_NGHIEP_LUC, getNghiepLuc(d) + amt)));
    }
    public void setNghiepLuc(PlayerData d, long val) {
        d.setExtraLong(KEY_NGHIEP_LUC, Math.max(0, val));
    }

    /**
     * Tính hệ số Thiên Kiếp dựa theo Karma.
     * Công Đức cao → giảm độ khó (min 0.70)
     * Nghiệp Lực cao → tăng độ khó (max 1.80)
     */
    public double getBreakthroughDiffMult(PlayerData d) {
        double mult = 1.0;
        long cd = getCongDuc(d);
        long nl = getNghiepLuc(d);
        // Công Đức: mỗi 10.000 giảm 3%, tối đa -30%
        mult -= Math.min(0.30, cd / 10_000.0 * 0.03);
        // Nghiệp Lực: mỗi 10.000 tăng 8%, tối đa +80%
        mult += Math.min(0.80, nl / 10_000.0 * 0.08);
        return Math.max(0.50, mult);
    }

    /** ATK bonus từ Nghiệp Lực (Ma Đạo tận dụng được) */
    public double getAtkBonusFromKarma(PlayerData d) {
        long nl = getNghiepLuc(d);
        return Math.min(0.40, nl / 100_000.0 * 0.40); // max +40%
    }

    public String getKarmaDisplay(PlayerData d) {
        long cd = getCongDuc(d), nl = getNghiepLuc(d);
        String grade;
        if (nl > 50_000)      grade = "§4§lĐại Ma Nhân";
        else if (nl > 20_000) grade = "§c§lMa Nhân";
        else if (nl > 5_000)  grade = "§4Hung Thủ";
        else if (cd > 50_000) grade = "§b§lĐại Thiện Nhân";
        else if (cd > 20_000) grade = "§a§lThiện Nhân";
        else if (cd > 5_000)  grade = "§aNgười Tốt";
        else                  grade = "§7Trung Lập";
        return grade + " §8(CD:" + cd + " NL:" + nl + ")";
    }
}
