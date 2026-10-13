package com.tutien.lingcan;

import java.util.List;

/**
 * Phan loai linh can theo so luong thuoc tinh:
 *   1 loai  = Thien Linh Can  (cuc hiem, tu luyen x3)
 *   2 loai  = Nhi Linh Can    (hiem, tu luyen x2)
 *   3 loai  = Chan Linh Can   (tot, tu luyen x1.5)
 *   4 loai  = Tam Linh Can    (trung binh, tu luyen x1)
 *   5 loai  = Nguy Linh Can   (pho bien, tu luyen x0.7)
 * Bien di bat ky so luong: tinh nhu 1 loai (hiem nhat)
 */
public enum LinhCanGrade {

    THIEN_LINH_CAN (1, "Thiên Linh Căn",  "§6§l", 3.00, 0.70,
        "§7Đơn linh căn, cực hiếm! Tu luyện tốc độ x3, thiên kiếp dễ hơn 30%.",
        "§6Thiên tài tuyệt thế! Các môn phái sẽ tranh nhau thu nạp!"),

    NHI_LINH_CAN   (2, "Nhị Linh Căn",   "§e§l",  2.00, 0.80,
        "§7Song linh căn, hiếm gặp. Tu luyện tốc độ x2.",
        "§eTư chất xuất sắc!"),

    CHAN_LINH_CAN   (3, "Chân Linh Căn",  "§a",    1.50, 0.90,
        "§7Tam linh căn, tư chất tốt. Tu luyện tốc độ x1.5.",
        "§aTư chất khá tốt, có thể vào môn phái tốt."),

    TAM_LINH_CAN    (4, "Tứ Linh Căn",   "§f",    1.00, 1.00,
        "§7Tứ linh căn, bình thường. Tu luyện bình thường.",
        "§7Tư chất bình thường, cần cố gắng hơn."),

    NGUY_LINH_CAN   (5, "Ngụy Linh Căn", "§8",    0.70, 1.20,
        "§8Ngũ linh căn, hấp thụ linh khí hỗn tạp. Tu luyện x0.7, khó đột phá.",
        "§8Tư chất kém, khó đạt Kim Đan trong đời thường...");

    private final int   rootCount;
    private final String displayName;
    private final String color;
    private final double lkMultiplier;      // Nhan vao LK nhan duoc
    private final double breakDifficulty;   // Nhan vao do kho dot pha
    private final String description;
    private final String flavorText;

    LinhCanGrade(int cnt, String dn, String c, double lk, double bt, String desc, String flavor) {
        this.rootCount = cnt; this.displayName = dn; this.color = c;
        this.lkMultiplier = lk; this.breakDifficulty = bt;
        this.description = desc; this.flavorText = flavor;
    }

    public static LinhCanGrade fromCount(int count) {
        return switch (count) {
            case 1  -> THIEN_LINH_CAN;
            case 2  -> NHI_LINH_CAN;
            case 3  -> CHAN_LINH_CAN;
            case 4  -> TAM_LINH_CAN;
            default -> NGUY_LINH_CAN;
        };
    }

    /** Bonus tong hop tu nhieu loai linh can */
    public static double combinedAtkBonus(List<LinhCanElement> elements) {
        return elements.stream().mapToDouble(LinhCanElement::getAtkBonus).average().orElse(1.0);
    }

    public static double combinedDefBonus(List<LinhCanElement> elements) {
        return elements.stream().mapToDouble(LinhCanElement::getDefBonus).average().orElse(1.0);
    }

    public static double combinedLkBonus(List<LinhCanElement> elements) {
        return elements.stream().mapToDouble(LinhCanElement::getLkSpeedBonus).average().orElse(1.0);
    }

    public static double combinedBreakEase(List<LinhCanElement> elements) {
        return elements.stream().mapToDouble(LinhCanElement::getBreakthroughEase).average().orElse(1.0);
    }

    public int    getRootCount()       { return rootCount; }
    public String getDisplayName()     { return displayName; }
    public String getColor()           { return color; }
    public double getLkMultiplier()    { return lkMultiplier; }
    public double getBreakDifficulty() { return breakDifficulty; }
    public String getDescription()     { return description; }
    public String getFlavorText()      { return flavorText; }
    public String getColoredName()     { return color + displayName; }
}
