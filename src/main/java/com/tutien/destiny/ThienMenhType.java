package com.tutien.destiny;

public enum ThienMenhType {
    MENH_KY  ("Mệnh Kỳ",   "§b", "⚡",
              "Căn cơ kỳ diệu — đột phá dễ hơn +5%",
              0.05, 0, 0, 0, 1.0),
    MENH_TAI ("Mệnh Tài",  "§6", "💰",
              "Thiên phú tài lộc — Linh Thạch drop +15%",
              0, 0.15, 0, 0, 1.0),
    MENH_CHIEN("Mệnh Chiến","§c", "⚔",
              "Sinh ra để chiến đấu — PvP damage +10%",
              0, 0, 0.10, 0, 1.0),
    MENH_HOA ("Mệnh Họa",  "§4", "☠",
              "Số phận nghiệt ngã — debuff ngẫu nhiên nhưng LK nhân đôi",
              -0.05, 0, 0, 2.0, 1.0),
    MENH_TUE ("Mệnh Tuệ",  "§a", "📖",
              "Trí tuệ hơn người — thời gian luyện đan -20%",
              0, 0, 0, 0, 1.0),
    MENH_LONG("Mệnh Long",  "§d", "🐉",
              "Long Mệnh thiên kiều — cực hiếm, mọi chỉ số +5%",
              0.05, 0.05, 0.05, 1.2, 1.0);

    public final String name, color, icon, desc;
    public final double btBonus;   // breakthrough rate +
    public final double ltBonus;   // LT drop multiplier +
    public final double pvpBonus;  // PvP damage +
    public final double lkMult;    // LK multiplier
    public final double weight;    // spawn weight (for random)

    ThienMenhType(String n, String c, String i, String d,
                  double bt, double lt, double pvp, double lk, double w) {
        name=n; color=c; icon=i; desc=d; btBonus=bt; ltBonus=lt; pvpBonus=pvp; lkMult=lk; weight=w;
    }
    public String display() { return color + icon + " " + name; }
}
