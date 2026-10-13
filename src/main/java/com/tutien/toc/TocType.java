package com.tutien.toc;

/**
 * Hệ thống Tộc (Race):
 *   NGUOI (Human)      - 89.9% | Cân bằng, không có đặc quyền
 *   THU_HOA (Beast)    -  8.0% | Nhanh, bản năng chiến đấu
 *   YEU_TOC (Demon)    -  1.5% | Ma lực, hút sinh lực
 *   THAN_TOC (Divine)  -  0.5% | Phòng thủ thiên phú, hồi máu
 *   LINH_TOC (Spirit)  -  0.09%| Tu luyện thần tốc
 *   LONG_TOC (Dragon)  - 0.0001%| Cực hiếm, mọi stat đều khủng
 */
public enum TocType {

    NGUOI(
            "Người",        "§f", "👤",
            "Tộc người bình thường. Cân bằng, không có đặc quyền nhưng thích nghi tốt.",
            89.9000,
            1.00, 1.00, 1.00, 0,
            "Không có đặc quyền đặc biệt nhưng có thể học mọi công pháp và sử dụng mọi pháp bảo.",
            false
    ),
    THU_HOA(
            "Thú Hóa",      "§6", "🐾",
            "Hóa thân từ thú vật linh, tốc độ và bản năng chiến đấu vượt trội.",
            8.0000,
            1.20, 0.95, 1.05, 2,    // +2 speed bonus ticks
            "Tốc độ +20% ATK, di chuyển nhanh hơn. Khi HP < 50%: bản năng thú hóa, ATK +30% thêm.",
            false
    ),
    YEU_TOC(
            "Yêu Tộc",      "§5", "👹",
            "Hậu duệ quỷ yêu, ma lực mạnh mẽ, hút sinh lực kẻ thù.",
            1.5000,
            1.15, 1.10, 0.95, 0,
            "Mỗi đòn đánh hút 5% sát thương thành máu. Ma lực tạo thêm 10% sát thương pháp thuật.",
            false
    ),
    THAN_TOC(
            "Thần Tộc",     "§b", "✨",
            "Hậu duệ thần linh, cơ thể phòng thủ bẩm sinh, hồi phục khi nguy nan.",
            0.5000,
            0.95, 1.35, 1.10, 0,
            "DEF +35%. Khi HP < 20%: tự động kích hoạt khiên bảo vệ, giảm 70% sát thương nhận vào 5 giây.",
            false
    ),
    LINH_TOC(
            "Linh Tộc",     "§d", "💫",
            "Sinh vật linh thiêng cực hiếm, tu luyện tốc độ vượt xa người thường.",
            0.0900,
            1.05, 1.05, 1.50, 0,   // LK speed x1.5
            "Linh Khí tích lũy nhanh x1.5. Thiên Kiếp giảm 20% độ khó. Đột phá tự động đủ LK.",
            false
    ),
    LONG_TOC(
            "Long Tộc",     "§6§l", "🐉",
            "TỘC RỒNG CỰC HIẾM - Hậu duệ rồng linh từ thượng cổ, sát thương và phòng thủ đều kinh hoàng.",
            0.0001,
            1.75, 1.60, 1.40, 3,
            "ATK +75%, DEF +60%, LK nhanh x1.4, bay nhanh x2. Thụ bắt lửa, tự động hồi phục 2% HP/5s. " +
                    "Khi đột phá: phát hiệu ứng Long Uy khiến kẻ thù xung quanh hoảng sợ.",
            true   // is ultra rare
    ),

    // ===== TỘC SỬA THÊM - SIÊU HIẾM =====
    PHUONG_HOANG_TOC(
            "Phượng Hoàng Tộc", "§c§l", "🦅",
            "Hậu duệ phượng hoàng bất tử - hồi sinh từ cái chết, lửa linh cực mạnh.",
            0.00008,
            1.85, 1.40, 1.50, 4,
            "Khi HP về 0: tự động hồi sinh với 30% HP (1 lần/3 phút). ATK lửa +100%.",
            true
    ),
    THUONG_THAN_TOC(
            "Thượng Thần Tộc", "§b§l", "⚡",
            "Hậu duệ của Thượng Thần từ thượng cổ - sát thương chớp giật kinh hoàng.",
            0.00006,
            2.00, 1.50, 1.60, 5,
            "ATK chớp giật +100%. Mỗi giậm bắn thêm sát thương sét. DEF +50%, LK x1.6.",
            true
    ),
    HON_HIEP_TOC(
            "Hỗn Hiệp Tộc", "§5§l", "🌀",
            "Sinh vật hỗn hiệp, nửa quỷ nửa tiên - hút linh lực và thao túng bóng tối.",
            0.00004,
            1.90, 1.30, 1.70, 3,
            "Hút 8% sát thương thành HP. Ám phú lực tăng +80%. Di chuyển trong bóng tối nhanh x2.",
            true
    ),
    KIM_CUONG_TOC(
            "Kim Cương Tộc", "§e§l", "💎",
            "Thân thể kim cương - bất khả hủy diệt, chịu mọi vật lí cực mạnh.",
            0.00003,
            1.60, 2.50, 1.30, 2,
            "DEF +150% - mọi vật lí giảm 50%. Khi HP < 10%: kích hoạt Kim Cương Bất Hoại (bất tử 5 giây).",
            true
    ),
    DAO_TOC(
            "Đạo Tộc", "§d§l", "☯",
            "Hậu duệ của Đạo Thần - cá nhân tu hành hồn đôn, lãnh đạo thiên hạ.",
            0.00001,
            2.10, 2.00, 2.00, 4,
            "Tất cả chỉ số +100%. LK x2. Mỗi đột phá: tự động thành công (không cần thiên kiếp).",
            true
    ),
    THUY_TINH_TOC(
            "Thủy Tinh Tộc", "§6§l§k", "✨",
            "Cảnh giới cuối cùng của sinh linh - tuyệt đối hiếm, tuyệt đối mạnh.",
            0.000001,
            3.00, 3.00, 3.00, 6,
            "Tất cả chỉ số x3. Bay nhanh x3. Đột phá tự động, lôi kiếp giảm 90%. TUYỆT ĐỐI HIẾM.",
            true
    );

    private final String displayName;
    private final String color;
    private final String icon;
    private final String description;
    private final double spawnWeight;   // % xác suất
    private final double atkMult;
    private final double defMult;
    private final double lkMult;
    private final int    speedBonus;    // bonus ticks of Speed potion
    private final String passiveDesc;
    private final boolean ultraRare;

    TocType(String dn, String c, String icon, String desc,
            double weight, double atk, double def, double lk, int spd,
            String passive, boolean rare) {
        this.displayName = dn; this.color = c; this.icon = icon;
        this.description = desc; this.spawnWeight = weight;
        this.atkMult = atk; this.defMult = def; this.lkMult = lk;
        this.speedBonus = spd; this.passiveDesc = passive; this.ultraRare = rare;
    }

    public String getDisplayName()  { return displayName; }
    public String getColor()        { return color; }
    public String getIcon()         { return icon; }
    public String getDescription()  { return description; }
    public double getSpawnWeight()  { return spawnWeight; }
    public double getAtkMult()      { return atkMult; }
    public double getDefMult()      { return defMult; }
    public double getLkMult()       { return lkMult; }
    public int    getSpeedBonus()   { return speedBonus; }
    public String getPassiveDesc()  { return passiveDesc; }
    public boolean isUltraRare()    { return ultraRare; }
    public String getColoredName()  { return color + icon + " " + displayName; }
}
