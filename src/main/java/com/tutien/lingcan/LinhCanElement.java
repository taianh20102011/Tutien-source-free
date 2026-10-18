package com.tutien.lingcan;

public enum LinhCanElement {

    // ===== NGU HANH =====
    KIM  ("Kim",  "§e", "⚔", "Luyện võ, sát thương cao",
          0.35,  // ty le xuat hien
          1.20,  // ATK bonus
          1.00,  // DEF bonus
          1.00,  // LK speed
          1.00), // dot pha bonus (giam kho)

    MOC  ("Mộc",  "§a", "🌿", "Luyện đan, trị liệu, sức sống bền bỉ",
          0.30, 1.00, 1.10, 1.10, 0.95),

    THUY ("Thủy", "§9", "💧", "Khống chế, nhu hòa, linh hoạt",
          0.25, 1.05, 1.05, 1.05, 0.95),

    HOA  ("Hỏa",  "§c", "🔥", "Luyện khí, luyện đan, sát thương bạo liệt",
          0.30, 1.25, 0.95, 1.00, 1.00),

    THO  ("Thổ",  "§6", "🏔", "Phòng ngự, vững chãi",
          0.25, 0.95, 1.30, 1.00, 0.90),

    // ===== BIEN DI (Rare) =====
    LOI  ("Lôi",  "§e", "⚡", "[BIẾN DỊ] Tốc độ nhanh, sát thương sấm sét",
          0.04, 1.40, 1.05, 1.20, 0.85),

    BANG ("Băng", "§b", "❄", "[BIẾN DỊ] Đóng băng, thuần khiết, quý hiếm",
          0.03, 1.20, 1.20, 1.15, 0.80),

    PHONG("Phong","§f", "🌬", "[BIẾN DỊ] Tốc độ, linh hoạt cao nhất",
          0.03, 1.15, 1.10, 1.25, 0.82),

    AM   ("Ám",   "§5", "🌑", "[BIẾN DỊ] Bóng tối, sát thương ẩn hiểm",
          0.02, 1.50, 0.90, 1.10, 0.85),

    QUANG("Quang","§f", "☀", "[BIẾN DỊ] Ánh sáng, thuần khiết nhất",
          0.02, 1.30, 1.25, 1.20, 0.75),

    // ===== SIEU CAP BIEN DI (Cuc Hiem) =====
    HON  ("Hỗn",  "§d", "☯", "[SIÊU CẤP] Hỗn độn nguyên khí - kết hợp cả ngũ hành",
          0.005, 1.60, 1.40, 1.35, 0.60),

    HUYET("Huyết","§c", "🩸", "[SIÊU CẤP] Huyết mạch linh - sát thương máu, hút linh",
          0.004, 1.80, 0.85, 1.15, 0.70),

    CO_HO("Cổ Hồ", "§e","⭐", "[SIÊU CẤP] Cổ Hồ linh căn - vượt qua ngũ hành",
          0.003, 1.70, 1.50, 1.45, 0.55),

    THIEN_NHAT("Thiên Nhật","§6","🌟","[THIÊN LINH+] Thiên nhật linh căn - hòa hợp trời đất",
          0.002, 2.00, 1.80, 1.60, 0.45),

    NGUYEN_SHI("Nguyên Thủy","§b","💎","[NGUYÊN THỦY] Linh căn nguyên thủy vũ trụ - tối hiếm",
          0.001, 2.20, 2.00, 1.80, 0.35),

    VO_THUONG_CAN("Vô Thượng Căn","§6§l","♾","[HUYỀN THIÊN] Vô thượng linh căn - tuyệt đối hiếm",
          0.0001, 3.00, 2.50, 2.00, 0.20);

    private final String displayName;
    private final String color;
    private final String icon;
    private final String description;
    private final double spawnWeight;
    private final double atkBonus;
    private final double defBonus;
    private final double lkSpeedBonus;
    private final double breakthroughEase; // < 1 = de dot pha hon

    LinhCanElement(String dn, String c, String icon, String desc,
                   double weight, double atk, double def, double lk, double bt) {
        this.displayName = dn; this.color = c; this.icon = icon;
        this.description = desc; this.spawnWeight = weight;
        this.atkBonus = atk; this.defBonus = def;
        this.lkSpeedBonus = lk; this.breakthroughEase = bt;
    }

    public boolean isBienDi() {
        return this == LOI || this == BANG || this == PHONG || this == AM || this == QUANG
            || this == HON || this == HUYET || this == CO_HO;
    }

    public boolean isSieuCap() {
        return this == HON || this == HUYET || this == CO_HO
            || this == THIEN_NHAT || this == NGUYEN_SHI || this == VO_THUONG_CAN;
    }

    public String getColoredName()  { return color + displayName; }
    public String getDisplayName()  { return displayName; }
    public String getColor()        { return color; }
    public String getIcon()         { return icon; }
    public String getDescription()  { return description; }
    public double getSpawnWeight()  { return spawnWeight; }
    public double getAtkBonus()     { return atkBonus; }
    public double getDefBonus()     { return defBonus; }
    public double getLkSpeedBonus() { return lkSpeedBonus; }
    public double getBreakthroughEase() { return breakthroughEase; }
}
