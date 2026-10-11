package com.tutien.data;

import java.util.Arrays;
import java.util.List;

public enum ClassType {

    KIEM_TU("Kiếm Tu", "⚔", "§c",
        "Cận chiến, ATK +50%",
        Arrays.asList("kiemkhitram","tockiem","thiensat","kiemtienphap","vankiemxuyentam"),
        50, 0, 0, 0),

    PHAP_TU("Pháp Tu", "🔮", "§9",
        "AoE phép, tu luyện +30%",
        Arrays.asList("linhhoacau","dongbang","phaptran","thaicuchuanphap"),
        0, 30, 0, 0),

    Y_TU("Ý Tu", "🍎", "§a",
        "Hồi máu, buff đồng đội",
        Arrays.asList("linhduoc","phuhotran","giaido","hoiduongcuunghich"),
        0, 0, 0, 0),

    DOC_TU("Độc Tu", "🕷", "§2",
        "DoT, debuff, bẫy",
        Arrays.asList("manhdoctan","amsatkiem","doctran","vandocchithe"),
        0, 0, 0, 0),

    THE_TU("Thể Tu", "🛡", "§7",
        "Tank, DEF +50%, HP x10",
        Arrays.asList("thietbich","hophaptran","phandon","kimcuongbathoai"),
        0, 0, 50, 10),

    QUY_TU("Quỷ Tu", "👻", "§5",
        "Tàng hình, burst",
        Arrays.asList("quyanhtan","hukhongdoat","tuanh","quyvuonggianthe"),
        0, 0, 0, 0),

    PHONG_TU("Phong Tự", "💨", "§b",
        "Tốc độ +40%, né đòn 50%+",
        Arrays.asList("phongtocthiet","toanphongne","phongnhan","cuongphongtran","phakhongthuandi"),
        0, 0, 0, 0),

    HOA_TU("Hỏa Tu", "🔥", "§6",
        "DoT lửa, boss killer",
        Arrays.asList("nghiephoathieu","hoalong","phonghoa","tuyetdiahoahai","phuonghoangtrognsinh"),
        0, 0, 0, 0);

    private final String displayName;
    private final String icon;
    private final String color;
    private final String description;
    private final List<String> skillIds;
    private final int bonusAtk, bonusCultivation, bonusDef, bonusHpMultiplier;

    ClassType(String dn, String icon, String color, String desc, List<String> skills,
              int atk, int cult, int def, int hp) {
        this.displayName = dn; this.icon = icon; this.color = color;
        this.description = desc; this.skillIds = skills;
        this.bonusAtk = atk; this.bonusCultivation = cult;
        this.bonusDef = def; this.bonusHpMultiplier = hp;
    }

    public String getDisplayName()      { return displayName; }
    public String getIcon()             { return icon; }
    public String getColor()            { return color; }
    public String getDescription()      { return description; }
    public List<String> getSkillIds()   { return skillIds; }
    public int getBonusAtk()            { return bonusAtk; }
    public int getBonusCultivation()    { return bonusCultivation; }
    public int getBonusDef()            { return bonusDef; }
    public int getBonusHpMultiplier()   { return bonusHpMultiplier; }
    public String getColoredName()      { return color + displayName; }
}
