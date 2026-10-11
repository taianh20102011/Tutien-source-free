package com.tutien.data;

public enum DaoPath {

    CHINH_DAO(
        "Chính Đạo", "§b", "☯",
        "Công lực thuần khiết, phòng thủ vững, hồi máu tốt. Thiên Kiếp nhẹ hơn 20%.",
        0.85, 1.10, 1.05, 1.00,
        "§bChính §7Đạo - Con đường thuần khiết, hướng thiện"
    ),

    MA_DAO(
        "Ma Đạo", "§4", "☠",
        "Sát thương cao, hút máu, debuff mạnh. Thiên Kiếp nặng hơn 20% nhưng reward gấp đôi.",
        1.20, 0.90, 0.85, 1.35,
        "§4Ma §7Đạo - Con đường tà ác, mạo hiểm"
    ),

    // ─────────────────────────────────────────────
    //  SĨ VƯƠNG - Class Học Sĩ Hài Hước
    //  Triết lý: "Trí tuệ > Cơ bắp"
    //  Bonus: Tọa thiền LK x5, Đột phá dễ nhất game
    //  Nhược: ATK tệ nhất game, bị chê cười liên tục
    // ─────────────────────────────────────────────
    SI_VUONG(
        "Sĩ Vương", "§e", "📖",
        "§eThánh Nhân Học Giả §7— Không đánh mà thắng, không chạy mà tới.\n" +
        "§7• Tọa thiền hấp thụ LK nhanh §ax5§7 (cao nhất game)\n" +
        "§7• Thiên Kiếp dễ nhất §a-40%§7 (sách giáo khoa đỡ đòn)\n" +
        "§7• §cATK -40% §7(tay cầm bút không cầm kiếm được)\n" +
        "§7• §cMỗi khi di chuyển§7: LK regen giảm 50%\n" +
        "§7• Passive: §eMỗi 10 phút ngồi yên§7 → tự động đột phá thất bại\n" +
        "§8✦ \"Ta đọc sách ngàn năm, ngươi chạy không thoát.\"",
        0.60,   // breakDiff x0.6 = dễ nhất game
        1.00,   // def bình thường
        1.20,   // heal tốt (ngồi nhiều nên hồi phục tốt)
        0.60,   // ATK x0.6 = tệ nhất
        "§e§lSĨ VƯƠNG §7- Học Giả Thiên Hạ Đệ Nhất"
    ),

    // ─────────────────────────────────────────────
    //  LỌ VƯƠNG - Class Lọ Đại Pháp
    //  Triết lý: "Mọi thứ đều có thể cho vào lọ"
    //  Bonus: DEF cao nhất, knockback resistance x2
    //  Nhược: Chậm như sên, ATK cũng rất tệ
    // ─────────────────────────────────────────────
    LO_VUONG(
        "Lọ Vương", "§5", "🏺",
        "§5Thiên Hạ Đệ Nhất Lọ Pháp §7— Bọc mọi thứ trong lọ, kể cả bản thân.\n" +
        "§7• DEF §a+60%§7 (lọ dày không đạn nào xuyên)\n" +
        "§7• Knockback resistance §a+80%§7 (lọ quá nặng)\n" +
        "§7• §cTốc độ di chuyển -30%§7 (vác lọ trên lưng)\n" +
        "§7• §cATK -20%§7 (đánh bằng lọ nên không chuẩn)\n" +
        "§7• Passive: §5Khi HP < 20%§7 → chui vào lọ bất tử 3 giây (CD 5 phút)\n" +
        "§7• Passive: §5Nhận damage§7 → 10% chance bắn lọ trúng kẻ tấn công\n" +
        "§8✦ \"Lọ là đạo, đạo là lọ, ta là lọ, lọ là ta.\"",
        1.00,   // breakDiff bình thường
        1.60,   // DEF x1.6 = cao nhất game
        1.10,   // heal bình thường
        0.80,   // ATK x0.8
        "§5§lLỌ VƯƠNG §7- Thiên Hạ Đệ Nhất Lọ Pháp"
    );

    // ─────────────────────────────────────────────
    private final String displayName;
    private final String color;
    private final String icon;
    private final String description;
    private final double breakDifficultyMult;
    private final double defBonus;
    private final String loreDisplay;
    private final double healBonus;
    private final double atkBonus;

    DaoPath(String dn, String c, String icon, String desc,
            double breakMult, double def, double heal, double atk, String lore) {
        this.displayName = dn; this.color = c; this.icon = icon;
        this.description = desc; this.breakDifficultyMult = breakMult;
        this.defBonus = def; this.healBonus = heal; this.atkBonus = atk;
        this.loreDisplay = lore;
    }

    // ─── Getters ───────────────────────────────
    public String getColoredName()         { return color + displayName; }
    public String getDisplayName()         { return displayName; }
    public String getColor()               { return color; }
    public String getIcon()                { return icon; }
    public String getDescription()         { return description; }
    public double getBreakDifficultyMult() { return breakDifficultyMult; }
    public double getDefBonus()            { return defBonus; }
    public double getHealBonus()           { return healBonus; }
    public double getAtkBonus()            { return atkBonus; }
    public double getAtkMult()             { return atkBonus; }   // alias cho MythicLibHook
    public double getDefMult()             { return defBonus; }   // alias cho MythicLibHook
    public double getBreakDiffMult()       { return breakDifficultyMult; } // alias cho MythicLibHook
    public double getLkMult() {
        // Sĩ Vương: khi tọa thiền LK x5 (xử lý trong MeditationSystem)
        // ở đây trả hệ số tổng quát
        return switch (this) {
            case SI_VUONG -> 2.0;   // base LK x2 (bonus thêm khi ngồi xử lý riêng)
            case LO_VUONG -> 0.85;  // LK chậm hơn (vác lọ mệt)
            default       -> 1.0;
        };
    }
    public String getLoreDisplay()         { return loreDisplay; }

    /** Có phải class vui nhộn (special passive) không */
    public boolean isFunClass() { return this == SI_VUONG || this == LO_VUONG; }
}
