package com.tutien.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Data-driven cultivation path. Built-in constants preserve source compatibility
 * with the older enum API; additional paths can be declared in plugins/TuTienPlugin/dao.yml.
 */
public final class DaoPath {
    private static final Map<String, DaoPath> REGISTRY = new LinkedHashMap<>();
    private static final Set<String> BUILT_INS = new LinkedHashSet<>();

    public static final DaoPath CHINH_DAO = builtin("CHINH_DAO", "Chính Đạo", "§b", "☯",
            "Công lực thuần khiết, phòng thủ vững, hồi máu tốt. Thiên Kiếp nhẹ hơn 15%.",
            0.85, 1.10, 1.05, 1.00, 1.0,
            "§bChính §7Đạo - Con đường thuần khiết, hướng thiện");
    public static final DaoPath MA_DAO = builtin("MA_DAO", "Ma Đạo", "§4", "☠",
            "Sát thương cao, hút máu, debuff mạnh. Thiên Kiếp nặng hơn 20% nhưng reward gấp đôi.",
            1.20, 0.90, 0.85, 1.35, 1.2,
            "§4Ma §7Đạo - Con đường tà ác, mạo hiểm");
    public static final DaoPath SI_VUONG = builtin("SI_VUONG", "Sĩ Vương", "§e", "📖",
            "Học giả tu đạo: tọa thiền mạnh, đột phá dễ hơn, sức tấn công thấp.",
            0.60, 1.00, 1.20, 0.60, 2.0,
            "§e§lSĨ VƯƠNG §7- Học Giả Thiên Hạ Đệ Nhất");
    public static final DaoPath LO_VUONG = builtin("LO_VUONG", "Lọ Vương", "§5", "🏺",
            "Đạo phòng thủ, chống chịu cao nhưng di chuyển và tấn công chậm.",
            1.00, 1.60, 1.10, 0.80, 0.85,
            "§5§lLỌ VƯƠNG §7- Thiên Hạ Đệ Nhất Lọ Pháp");

    private final String name;
    private String displayName;
    private String color;
    private String icon;
    private String description;
    private double breakDifficultyMult;
    private double defBonus;
    private double healBonus;
    private double atkBonus;
    private double lkMult;
    private String loreDisplay;
    private boolean enabled = true;

    private DaoPath(String name, String displayName, String color, String icon, String description,
                    double breakMult, double def, double heal, double atk, double lkMult, String lore) {
        this.name = name;
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.breakDifficultyMult = clamp(breakMult, 0.10, 5.0, 1.0);
        this.defBonus = clamp(def, 0.10, 5.0, 1.0);
        this.healBonus = clamp(heal, 0.10, 5.0, 1.0);
        this.atkBonus = clamp(atk, 0.10, 5.0, 1.0);
        this.lkMult = clamp(lkMult, 0.10, 10.0, 1.0);
        this.loreDisplay = lore;
    }

    private static DaoPath builtin(String id, String name, String color, String icon, String desc,
                                   double breakMult, double def, double heal, double atk, double lk, String lore) {
        DaoPath path = new DaoPath(id, name, color, icon, desc, breakMult, def, heal, atk, lk, lore);
        REGISTRY.put(id, path);
        BUILT_INS.add(id);
        return path;
    }

    /** Load or override paths using dao.yml. Must run before PlayerDataManager loads player files. */
    public static synchronized void loadFrom(File file, Logger logger) {
        if (file == null || !file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : new ArrayList<>(REGISTRY.keySet())) {
            if (!BUILT_INS.contains(id)) REGISTRY.remove(id);
        }
        ConfigurationSection builtInSection = yaml.getConfigurationSection("paths");
        if (builtInSection != null) {
            for (String id : builtInSection.getKeys(false)) {
                DaoPath path = REGISTRY.get(id.toUpperCase(Locale.ROOT));
                if (path == null || !BUILT_INS.contains(path.name)) continue;
                path.apply(builtInSection.getConfigurationSection(id));
            }
        }
        ConfigurationSection custom = yaml.getConfigurationSection("custom-paths");
        if (custom != null) {
            for (String rawId : custom.getKeys(false)) {
                String id = rawId.toUpperCase(Locale.ROOT);
                if (!id.matches("[A-Z][A-Z0-9_]{1,31}") || REGISTRY.containsKey(id)) {
                    if (logger != null) logger.warning("[TuTien] Bỏ qua Đạo custom ID không hợp lệ/trùng: " + rawId);
                    continue;
                }
                ConfigurationSection s = custom.getConfigurationSection(rawId);
                if (s == null || !s.getBoolean("enabled", true)) continue;
                DaoPath path = new DaoPath(id,
                        s.getString("display-name", id),
                        color(s.getString("color", "§f")),
                        s.getString("icon", "☯"),
                        s.getString("description", "Đạo tùy chỉnh của máy chủ."),
                        s.getDouble("break-difficulty-multiplier", 1.0),
                        s.getDouble("defense-multiplier", 1.0),
                        s.getDouble("healing-multiplier", 1.0),
                        s.getDouble("attack-multiplier", 1.0),
                        s.getDouble("linh-khi-multiplier", 1.0),
                        s.getString("lore-display", s.getString("display-name", id)));
                path.enabled = true;
                REGISTRY.put(id, path);
            }
        }
        if (logger != null) logger.info("[TuTien] Đã nạp " + REGISTRY.size() + " con đường Đạo từ dao.yml.");
    }

    private void apply(ConfigurationSection section) {
        if (section == null) return;
        displayName = section.getString("display-name", displayName);
        color = color(section.getString("color", color));
        icon = section.getString("icon", icon);
        description = section.getString("description", description);
        breakDifficultyMult = clamp(section.getDouble("break-difficulty-multiplier", breakDifficultyMult), 0.10, 5.0, breakDifficultyMult);
        defBonus = clamp(section.getDouble("defense-multiplier", defBonus), 0.10, 5.0, defBonus);
        healBonus = clamp(section.getDouble("healing-multiplier", healBonus), 0.10, 5.0, healBonus);
        atkBonus = clamp(section.getDouble("attack-multiplier", atkBonus), 0.10, 5.0, atkBonus);
        lkMult = clamp(section.getDouble("linh-khi-multiplier", lkMult), 0.10, 10.0, lkMult);
        loreDisplay = section.getString("lore-display", loreDisplay);
        enabled = section.getBoolean("enabled", true);
    }

    public static DaoPath[] values() {
        return REGISTRY.values().stream().filter(DaoPath::isEnabled).toArray(DaoPath[]::new);
    }

    public static DaoPath valueOf(String name) {
        if (name == null) throw new IllegalArgumentException("DaoPath name is null");
        DaoPath path = REGISTRY.get(name.trim().toUpperCase(Locale.ROOT));
        if (path == null || !path.isEnabled()) throw new IllegalArgumentException("No DaoPath named " + name);
        return path;
    }

    public String name() { return name; }
    @Override public String toString() { return name; }

    public String getColoredName() { return color + displayName; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public double getBreakDifficultyMult() { return breakDifficultyMult; }
    public double getBreakDifficultyMultOrDefault() { return breakDifficultyMult; }
    public double getDefBonus() { return defBonus; }
    public double getHealBonus() { return healBonus; }
    public double getAtkBonus() { return atkBonus; }
    public double getAtkMult() { return atkBonus; }
    public double getDefMult() { return defBonus; }
    public double getBreakDiffMult() { return breakDifficultyMult; }
    public double getLkMult() { return lkMult; }
    public String getLoreDisplay() { return loreDisplay; }
    public boolean isEnabled() { return enabled; }
    public boolean isFunClass() { return this == SI_VUONG || this == LO_VUONG; }

    private static String color(String value) {
        if (value == null || value.isBlank()) return "§f";
        return value.replace('&', '§');
    }
    private static double clamp(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
