package com.tutien.title;

public enum TitleType {
    // Đột phá
    NEWBIE         ("Tân Thủ",       "§7", 0,  "Mới gia nhập"),
    BREAKTHROUGH_10("Dũng Cảm",      "§a", 1,  "Đột phá thành công 10 lần"),
    BREAKTHROUGH_50("Kiên Định",     "§b", 2,  "Đột phá thành công 50 lần"),
    BREAKTHROUGH_100("Bất Khuất",   "§6", 3,  "Đột phá thành công 100 lần"),
    // Giết mob
    HUNTER_100     ("Sát Thủ",       "§c", 1,  "Giết 100 mob"),
    HUNTER_1000    ("Đồ Tể",         "§4", 2,  "Giết 1.000 mob"),
    HUNTER_10000   ("Chiến Thần",    "§c§l",3, "Giết 10.000 mob"),
    // PvP
    PVP_10         ("Hiếu Chiến",    "§e", 1,  "Giết 10 người chơi"),
    PVP_50         ("Hung Thủ",      "§c", 2,  "Giết 50 người chơi"),
    PVP_200        ("Đại Ma Nhân",   "§4§l",3, "Giết 200 người chơi"),
    // Tông Môn
    SECT_ELDER     ("Trưởng Lão",    "§9", 2,  "Đạt vị trí Trưởng Lão Tông Môn"),
    SECT_MASTER    ("Tông Chủ",      "§6§l",3, "Trở thành Tông Chủ"),
    // Cảnh giới
    TIEN_GIOI      ("Chân Tiên",     "§b", 2,  "Đạt Tiên Giới"),
    THIEN_GIOI     ("Thiên Tôn",     "§6", 3,  "Đạt Thiên Giới"),
    SIEU_CAP       ("Vô Thượng",     "§d§l",4, "Đạt 18 tầng Siêu Cấp"),
    TOI_THUONG     ("Tuyệt Đỉnh",   "§c§l",5, "Đạt 30 tầng Tối Thượng"),
    // Đặc biệt
    ALCHEMIST      ("Luyện Đan Sư",  "§a", 2,  "Luyện 100 Linh Đan"),
    MASTER         ("Sư Phụ",        "§e", 2,  "Nhận đệ tử đầu tiên"),
    GRANDMASTER    ("Đại Sư",        "§6", 3,  "Có 5 đệ tử"),
    BOSS_SLAYER    ("Kiếp Nạn Sát",  "§5", 3,  "Hạ 20 Boss Arena"),
    KARMA_GOOD     ("Đại Thiện Nhân","§b", 2,  "Công Đức đạt 50.000"),
    KARMA_EVIL     ("Đại Ma Nhân",   "§4", 2,  "Nghiệp Lực đạt 50.000");

    public final String displayName, color, desc;
    public final int tier; // 0=default, 1=bronze, 2=silver, 3=gold, 4=diamond, 5=legendary

    TitleType(String dn, String c, int tier, String d) {
        displayName=dn; color=c; this.tier=tier; desc=d;
    }
    public String display() { return color + "[" + displayName + "]"; }
}
