package com.tutien.season;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.DaoPath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * SeasonManager — hệ thống "meta mùa": admin chọn 1 ClassType + 1 DaoPath
 * làm "mạnh nhất mùa này" bằng lệnh "/ttadmin season set <class> <dao>"
 * (xem AdminCommand). Người chơi ĐÚNG class và/hoặc ĐÚNG đạo đó được cộng
 * thêm % sức mạnh (ATK/DEF/tu luyện), tỉ lệ % cấu hình ở config.yml mục
 * "season".
 *
 * Lựa chọn hiện tại (class/đạo nào đang là meta) được lưu ở
 * season_state.yml — KHÔNG lưu trong config.yml — vì:
 *   1. Đổi season KHÔNG cần gọi plugin.saveConfig() (việc này sẽ xóa hết
 *      comment trong config.yml, rất tệ vì file đó có rất nhiều giải thích).
 *   2. Season vẫn sống sót qua restart/reload giống hệt worldboss_state.yml.
 *
 * Chỉ set 1 trong 2 (chỉ class hoặc chỉ đạo) cũng được — cái còn lại để
 * null thì không cộng bonus phần đó.
 */
public class SeasonManager {

    private final TuTienPlugin plugin;
    private final File stateFile;

    private ClassType currentClass = null; // null = chưa chọn class nào cho mùa này
    private DaoPath currentDao = null;     // null = chưa chọn đạo nào cho mùa này

    public SeasonManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "season_state.yml");
        loadState();
    }

    // ========================
    //  ADMIN SET / CLEAR
    // ========================

    /** Đặt season mới. classType hoặc daoPath có thể null (không đổi phần đó thành "không có ai mạnh"). */
    public void setSeason(ClassType classType, DaoPath daoPath) {
        this.currentClass = classType;
        this.currentDao = daoPath;
        saveState();
        announce();
    }

    public void clearSeason() {
        this.currentClass = null;
        this.currentDao = null;
        saveState();
    }

    private void announce() {
        if (!plugin.getConfig().getBoolean("season.announce-on-set", true)) return;
        String raw = plugin.getConfig().getString("season.announce-message",
                "&6&l✦ MÙA MỚI! &eClass mạnh nhất: &b{class} &e| Đạo mạnh nhất: &b{dao}");
        String msg = ChatColor.translateAlternateColorCodes('&', raw
                .replace("{class}", currentClass != null ? currentClass.getDisplayName() : "—")
                .replace("{dao}", currentDao != null ? currentDao.getDisplayName() : "—"));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        plugin.getLogger().info("[Season] " + ChatColor.stripColor(msg));
    }

    public ClassType getCurrentClass() { return currentClass; }
    public DaoPath   getCurrentDao()   { return currentDao; }
    public boolean   hasActiveSeason() { return currentClass != null || currentDao != null; }

    /** Dòng trạng thái ngắn gọn, dùng cho "/ttadmin season info" và có thể lệnh xem cho player. */
    public String status() {
        if (!plugin.getConfig().getBoolean("season.enabled", true))
            return "§7Season đang bị tắt (season.enabled: false trong config.yml).";
        if (!hasActiveSeason())
            return "§7Chưa có season nào được thiết lập. Dùng §e/ttadmin season set <class> <dao>";
        return "§6✦ Season hiện tại: §eClass §b" + (currentClass != null ? currentClass.getDisplayName() : "—")
                + " §7(+" + plugin.getConfig().getInt("season.class-atk-bonus-percent", 0) + "% ATK / +"
                + plugin.getConfig().getInt("season.class-def-bonus-percent", 0) + "% DEF / +"
                + plugin.getConfig().getInt("season.class-cultivation-bonus-percent", 0) + "% tu luyện)"
                + " §7| §eĐạo §b" + (currentDao != null ? currentDao.getDisplayName() : "—")
                + " §7(+" + plugin.getConfig().getInt("season.dao-atk-bonus-percent", 0) + "% ATK / +"
                + plugin.getConfig().getInt("season.dao-def-bonus-percent", 0) + "% DEF)";
    }

    // ========================
    //  HE SO NHAN (goi tu PlayerData)
    // ========================

    private boolean enabled() { return plugin.getConfig().getBoolean("season.enabled", true); }

    public double getClassAtkMultiplier(ClassType type) {
        if (!enabled() || type == null || type != currentClass) return 1.0;
        return 1.0 + plugin.getConfig().getDouble("season.class-atk-bonus-percent", 0) / 100.0;
    }

    public double getClassDefMultiplier(ClassType type) {
        if (!enabled() || type == null || type != currentClass) return 1.0;
        return 1.0 + plugin.getConfig().getDouble("season.class-def-bonus-percent", 0) / 100.0;
    }

    public double getClassCultivationMultiplier(ClassType type) {
        if (!enabled() || type == null || type != currentClass) return 1.0;
        return 1.0 + plugin.getConfig().getDouble("season.class-cultivation-bonus-percent", 0) / 100.0;
    }

    public double getDaoAtkMultiplier(DaoPath dao) {
        if (!enabled() || dao == null || dao != currentDao) return 1.0;
        return 1.0 + plugin.getConfig().getDouble("season.dao-atk-bonus-percent", 0) / 100.0;
    }

    public double getDaoDefMultiplier(DaoPath dao) {
        if (!enabled() || dao == null || dao != currentDao) return 1.0;
        return 1.0 + plugin.getConfig().getDouble("season.dao-def-bonus-percent", 0) / 100.0;
    }

    // ========================
    //  PERSISTENCE (season_state.yml — KHONG dung config.yml)
    // ========================

    private void loadState() {
        if (!stateFile.exists()) return;
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(stateFile);
            String cls = yml.getString("current-class", "");
            String dao = yml.getString("current-dao", "");
            if (cls != null && !cls.isEmpty()) {
                try { currentClass = ClassType.valueOf(cls); }
                catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[Season] current-class '" + cls + "' trong season_state.yml khong hop le, bo qua.");
                }
            }
            if (dao != null && !dao.isEmpty()) {
                try { currentDao = DaoPath.valueOf(dao); }
                catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[Season] current-dao '" + dao + "' trong season_state.yml khong hop le, bo qua.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Season] Khong doc duoc season_state.yml: " + e.getMessage());
        }
    }

    private void saveState() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("current-class", currentClass != null ? currentClass.name() : "");
        yml.set("current-dao", currentDao != null ? currentDao.name() : "");
        try {
            stateFile.getParentFile().mkdirs();
            yml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Season] Khong luu duoc season_state.yml: " + e.getMessage());
        }
    }
}
