package com.tutien.quest;

public enum QuestType {
    // DAILY
    DAILY_KILL_MOB    (true,  false, "Giết {target} mob",           50,  200,  500),
    DAILY_BREAKTHROUGH(true,  false, "Đột phá thành công 1 lần",     1,  300, 1000),
    DAILY_ALCHEMY     (true,  false, "Luyện {target} Linh Đan",       3,  200,  600),
    DAILY_HERB        (true,  false, "Thu thập {target} Tiên Thảo",  10,  150,  400),
    DAILY_MEDITATE    (true,  false, "Thiền định {target} phút",     20,  250,  800),
    // WEEKLY
    WEEKLY_KILL_1000  (false, true,  "Giết 1000 mob trong tuần",   1000, 2000, 5000),
    WEEKLY_BREAKTHROUGH_5(false,true,"Đột phá 5 lần trong tuần",      5, 3000, 8000),
    WEEKLY_BOSS       (false, true,  "Hạ Boss Arena 3 lần",           3, 5000,15000),
    WEEKLY_ALCHEMY_20 (false, true,  "Luyện 20 Linh Đan trong tuần", 20, 2500, 7000),
    WEEKLY_PVP        (false, true,  "Tham gia 5 trận PvP",           5, 2000, 6000);

    public final boolean isDaily, isWeekly;
    public final String desc;
    public final int targetCount;
    public final long rewardLT;   // Linh Thạch thưởng
    public final long rewardLK;   // LK bonus thưởng

    QuestType(boolean d, boolean w, String desc, int target, long lt, long lk) {
        isDaily=d; isWeekly=w; this.desc=desc; targetCount=target; rewardLT=lt; rewardLK=lk;
    }
    public String displayDesc() { return desc.replace("{target}", String.valueOf(targetCount)); }
}
