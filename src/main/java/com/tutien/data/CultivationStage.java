package com.tutien.data;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * He thong Canh Gioi CHOT (v2) - 25 canh gioi, chia thanh 5 GIOI lon:
 *
 *   Linh Gioi  (0- 4): Luyen Khi -> Truc Co -> Ket Dan -> Nguyen Anh -> Hoa Than        [3 ky/canh gioi]
 *   De Gioi    (5- 9): De Tu -> De Hau -> De Vuong -> De Ton -> De Chu                  [4 ky/canh gioi]
 *   Thanh Gioi (10-14): Ban Thanh -> Chuan Thanh -> Dai Thanh -> Thanh Ton -> Thanh De  [5 ky/canh gioi] [BAY DUOC]
 *   Tien Gioi  (15-19): Chan Tien -> Kim Tien -> Thai At -> Dai La -> Tien De           [6 ky/canh gioi]
 *   Thiên Giới (20-24): Thiên Tôn -> Thiên Đạo -> Thiên Đế -> Hỗn Nguyên -> Vô Thượng Chí Tôn [6 kỳ, riêng cảnh giới cuối là 1 bước duy nhất]
 *
 * Quy tac "ky" (rank nho) trong 1 canh gioi: cang len GIOI cao thi so ky cang nhieu,
 * toi da 6 ky (So Ky / Trung Ky / Hau Ky / Dinh Phong / Vien Man / Cuc Han).
 * Cảnh giới cuối cùng "Vô Thượng Chí Tôn" chỉ có 1 bước duy nhất - đỉnh cao tuyệt đối.
 */
public enum CultivationStage {

    // ===== LINH GIOI (3 ky/canh gioi) =====
    LUYEN_KHI ("Luyện Khí",  "\u00a7f", 2_000L,             3, "Linh Giới", "\u00a7a"),
    TRUC_CO   ("Trúc Cơ",    "\u00a7a", 20_000L,            3, "Linh Giới", "\u00a7a"),
    KET_DAN   ("Kết Đan",    "\u00a7e", 200_000L,           3, "Linh Giới", "\u00a7a"),
    NGUYEN_ANH("Nguyên Anh", "\u00a7b", 2_000_000L,         3, "Linh Giới", "\u00a7a"),
    HOA_THAN  ("Hóa Thần",   "\u00a7d", 20_000_000L,        3, "Linh Giới", "\u00a7a"),

    // ===== DE GIOI (4 ky/canh gioi) =====
    DE_TU    ("Đế Tu",    "\u00a7c",       200_000_000L,   4, "Đế Giới", "\u00a7c"),
    DE_HAU   ("Đế Hậu",   "\u00a7c",     1_000_000_000L,   4, "Đế Giới", "\u00a7c"),
    DE_VUONG ("Đế Vương", "\u00a7c\u00a7l", 5_000_000_000L, 4, "Đế Giới", "\u00a7c"),
    DE_TON   ("Đế Tôn",   "\u00a74\u00a7l", 20_000_000_000L,4, "Đế Giới", "\u00a7c"),
    DE_CHU   ("Đế Chủ",   "\u00a74\u00a7l", 80_000_000_000L,4, "Đế Giới", "\u00a7c"),

    // ===== THANH GIOI (5 ky/canh gioi) [BAY DUOC tu day] =====
    BAN_THANH  ("Bán Thánh",   "\u00a7f",        300_000_000_000L,     5, "Thánh Giới", "\u00a7f"),
    CHUAN_THANH("Chuẩn Thánh", "\u00a7f\u00a7l", 1_000_000_000_000L,   5, "Thánh Giới", "\u00a7f"),
    DAI_THANH  ("Đại Thánh",   "\u00a7b\u00a7l", 4_000_000_000_000L,   5, "Thánh Giới", "\u00a7f"),
    THANH_TON  ("Thánh Tôn",   "\u00a76\u00a7l", 15_000_000_000_000L,  5, "Thánh Giới", "\u00a76"),
    THANH_DE   ("Thánh Đế",    "\u00a76\u00a7l", 50_000_000_000_000L,  5, "Thánh Giới", "\u00a76"),

    // ===== TIEN GIOI (6 ky/canh gioi) =====
    CHAN_TIEN("Chân Tiên", "\u00a79",        200_000_000_000_000L,  6, "Tiên Giới", "\u00a7b"),
    KIM_TIEN ("Kim Tiên",  "\u00a7e",        800_000_000_000_000L,  6, "Tiên Giới", "\u00a7b"),
    THAI_AT  ("Thái Ất",   "\u00a7b",      3_000_000_000_000_000L,  6, "Tiên Giới", "\u00a7b"),
    DAI_LA   ("Đại La",    "\u00a7d",     12_000_000_000_000_000L,  6, "Tiên Giới", "\u00a7b"),
    TIEN_DE  ("Tiên Đế",   "\u00a7c\u00a7l", 50_000_000_000_000_000L,6, "Tiên Giới", "\u00a7c"),

    // ===== THIEN GIOI (6 ky/canh gioi, rieng canh gioi cuoi 1 buoc) =====
    THIEN_TON ("Thiên Tôn",  "\u00a7f\u00a7l", 200_000_000_000_000_000L, 6, "Thiên Giới", "\u00a7f"),
    THIEN_DAO ("Thiên Đạo",  "\u00a7b\u00a7l", Long.MAX_VALUE / 20,      6, "Thiên Giới", "\u00a7f"),
    THIEN_DE  ("Thiên Đế",   "\u00a76\u00a7l", Long.MAX_VALUE / 8,       6, "Thiên Giới", "\u00a76"),
    HON_NGUYEN("Hỗn Nguyên", "\u00a7c\u00a7l", Long.MAX_VALUE / 3,       6, "Thiên Giới", "\u00a7c"),

    // ===== VO THUONG CHI TON - CANH GIOI CUOI CUNG, DINH CAO TUYET DOI =====
    VO_THUONG_CHI_TON("Vô Thượng Chí Tôn", "\u00a76\u00a7l", Long.MAX_VALUE, 1, "Thiên Giới", "\u00a76\u00a7l");

    private static final String[] KY_NAMES = {
        "Sơ Kỳ", "Trung Kỳ", "Hậu Kỳ", "Đỉnh Phong", "Viên Mãn", "Cực Hạn"
    };

    /**
     * Bản Free cho phép máy chủ tuỳ biến tên/màu/ngưỡng tu luyện của 25 slot
     * cảnh giới có sẵn qua plugins/TuTienPlugin/custom-canh-gioi.yml.
     * ID enum vẫn được giữ nguyên để không làm hỏng dữ liệu người chơi đã lưu.
     */
    private static volatile Map<CultivationStage, StageOverride> CUSTOM_OVERRIDES = Map.of();

    public static int loadCustomizations(File file, Logger logger) {
        if (file == null || !file.isFile()) {
            CUSTOM_OVERRIDES = Map.of();
            if (logger != null) logger.warning("Không tìm thấy custom-canh-gioi.yml; dùng cảnh giới mặc định.");
            return 0;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection stages = yaml.getConfigurationSection("stages");
        if (stages == null) {
            CUSTOM_OVERRIDES = Map.of();
            if (logger != null) logger.warning("custom-canh-gioi.yml thiếu mục 'stages'; dùng cảnh giới mặc định.");
            return 0;
        }

        Map<CultivationStage, StageOverride> loadedOverrides = new EnumMap<>(CultivationStage.class);
        int loaded = 0;
        for (String rawId : stages.getKeys(false)) {
            CultivationStage stage;
            try {
                stage = CultivationStage.valueOf(rawId.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                if (logger != null) logger.warning("Bỏ qua ID cảnh giới không hợp lệ trong custom-canh-gioi.yml: " + rawId);
                continue;
            }

            ConfigurationSection section = stages.getConfigurationSection(rawId);
            if (section == null) continue;
            StageOverride override = new StageOverride();
            override.displayName = nonBlank(section.getString("name"));
            override.color = color(section.getString("color"));
            override.realm = nonBlank(section.getString("realm"));
            override.realmColor = color(section.getString("realm-color"));

            if (section.contains("base-linh-khi")) {
                long base = section.getLong("base-linh-khi", stage.baseLinhKhi);
                if (base > 0L) override.baseLinhKhi = base;
                else if (logger != null) logger.warning("base-linh-khi phải > 0 ở cảnh giới " + rawId + "; giữ giá trị mặc định.");
            }
            if (section.contains("max-rank")) {
                int ranks = section.getInt("max-rank", stage.maxRank);
                if (ranks >= 1 && ranks <= KY_NAMES.length) override.maxRank = ranks;
                else if (logger != null) logger.warning("max-rank của " + rawId + " phải từ 1 đến " + KY_NAMES.length + "; giữ giá trị mặc định.");
            }
            if (section.contains("breakthrough-difficulty")) {
                double difficulty = section.getDouble("breakthrough-difficulty", -1.0);
                if (Double.isFinite(difficulty) && difficulty > 0.0 && difficulty <= 1.0) {
                    override.breakthroughDifficulty = difficulty;
                } else if (logger != null) {
                    logger.warning("breakthrough-difficulty của " + rawId + " phải > 0 và <= 1; dùng công thức mặc định.");
                }
            }
            if (section.contains("can-fly")) override.canFly = section.getBoolean("can-fly");
            if (section.contains("can-use-all-skills")) override.canUseAllSkills = section.getBoolean("can-use-all-skills");
            loadedOverrides.put(stage, override);
            loaded++;
        }

        CUSTOM_OVERRIDES = Map.copyOf(loadedOverrides);
        if (logger != null) logger.info("Đã nạp tuỳ biến cho " + loaded + " slot cảnh giới từ custom-canh-gioi.yml.");
        return loaded;
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String color(String value) {
        return value == null || value.isBlank() ? null : ChatColor.translateAlternateColorCodes('&', value);
    }

    private StageOverride custom() { return CUSTOM_OVERRIDES.get(this); }

    private static final class StageOverride {
        private String displayName;
        private String color;
        private String realm;
        private String realmColor;
        private Long baseLinhKhi;
        private Integer maxRank;
        private Double breakthroughDifficulty;
        private Boolean canFly;
        private Boolean canUseAllSkills;
    }

    private final String displayName;
    private final String color;
    private final long   baseLinhKhi;
    private final int    maxRank;
    private final String realm;
    private final String realmColor;

    CultivationStage(String dn, String c, long b, int mr, String realm, String rc) {
        this.displayName = dn; this.color = c; this.baseLinhKhi = b;
        this.maxRank = mr; this.realm = realm; this.realmColor = rc;
    }

    public long getMaxLinhKhiForRank(int rank) {
        rank = Math.max(1, Math.min(rank, getMaxRank()));
        StageOverride override = custom();
        long result = override != null && override.baseLinhKhi != null ? override.baseLinhKhi : baseLinhKhi;
        for (int i = 1; i < rank; i++) {
            if (result > Long.MAX_VALUE / 2) { result = Long.MAX_VALUE; break; }
            result *= 2;
        }
        return result;
    }

    public boolean hasNextRank(int rank)    { return rank < getMaxRank(); }
    public CultivationStage nextStage() {
        int n = ordinal() + 1;
        return n < values().length ? values()[n] : null;
    }
    public CultivationStage prevStage() {
        int p = ordinal() - 1;
        return p >= 0 ? values()[p] : null;
    }

    public double getAttackMultiplier(int rank) {
        return 1.0 + (ordinal() * 1.5) + (rank * 0.15);
    }
    public double getHealthMultiplier(int rank) {
        return 1.0 + (ordinal() * 2.5) + (rank * 0.25);
    }

    /**
     * Do kho dot pha (0.0 = kho nhat, 1.0 = de nhat). Chi anh huong den
     * co the that bai khi HP xuong thap trong luc chiu Thien Kiep.
     *
     *   Linh Gioi  : 0.95 -> 0.75  (de, hoc co che)
     *   De Gioi    : 0.68 -> 0.50  (trung binh)
     *   Thanh Gioi : 0.45 -> 0.25  (kha kho)
     *   Tien Gioi  : 0.20 -> 0.05  (kho, can chuan bi ky)
     *   Thien Gioi : 0.04 -> 0.013 (rat kho, can phap bao + dan)
     *   Vô Thượng Chí Tôn: 0.0005 (cực khó - địch thủ không thua)
     */
    public double getBreakthroughDifficulty() {
        StageOverride override = custom();
        if (override != null && override.breakthroughDifficulty != null) return override.breakthroughDifficulty;
        int ord = ordinal();
        if (ord <= 19) return Math.max(0.05, 0.95 - ord * 0.045);
        if (ord <= 23) return 0.04 - (ord - 20) * 0.009;
        return 0.0005; // Vô Thượng Chí Tôn
    }

    /**
     * Ten ky nho trong 1 canh gioi, scale theo so ky toi da cua canh gioi do
     * (3 ky o Linh Gioi -> 4 o De Gioi -> 5 o Thanh Gioi -> 6 o Tien/Thien Gioi).
     * Canh gioi cuoi cung (maxRank=1) khong co ten ky phu.
     */
    public String getRankName(int rank) {
        int maxRank = getMaxRank();
        if (maxRank <= 1) return "";
        int idx = Math.max(1, Math.min(rank, maxRank)) - 1;
        idx = Math.min(idx, KY_NAMES.length - 1);
        return KY_NAMES[idx];
    }

    public String getDisplayName() {
        StageOverride override = custom();
        return override != null && override.displayName != null ? override.displayName : displayName;
    }
    public String getColor() {
        StageOverride override = custom();
        return override != null && override.color != null ? override.color : color;
    }
    public int getMaxRank() {
        StageOverride override = custom();
        return override != null && override.maxRank != null ? override.maxRank : maxRank;
    }
    public String getRealm() {
        StageOverride override = custom();
        return override != null && override.realm != null ? override.realm : realm;
    }
    public String getRealmColor() {
        StageOverride override = custom();
        return override != null && override.realmColor != null ? override.realmColor : realmColor;
    }
    public String getFullDisplay(int rank) {
        if (getMaxRank() == 1) return getColor() + getDisplayName();
        return getColor() + getDisplayName() + " \u00a7f" + getRankName(rank);
    }
    public boolean isTopStage() { return this == VO_THUONG_CHI_TON; }
    public boolean canUseAllSkills() {
        StageOverride override = custom();
        return override != null && override.canUseAllSkills != null
                ? override.canUseAllSkills : ordinal() >= THIEN_DE.ordinal();
    }
    public boolean canFly() {
        StageOverride override = custom();
        return override != null && override.canFly != null
                ? override.canFly : ordinal() >= BAN_THANH.ordinal();
    }
}
