package com.tutien.breakthrough.quest;

/**
 * Dữ liệu nhiệm vụ đột phá cảnh giới (builtin-quests).
 */
public class BreakthroughQuest {

    private final String targetStageName;
    private final int killMobs;
    private final String mobDesc;
    private final int herbs;
    private final String herbName;
    private final int alchemy;
    private final String danName;
    private final int meditateMinutes;
    private final String meditateDesc;
    private final int bossKills;
    private final String bossDesc;
    private final String danHoMenh;
    private final String danDesc;

    public BreakthroughQuest(String targetStageName,
                            int killMobs, String mobDesc,
                            int herbs, String herbName,
                            int alchemy, String danName,
                            int meditateMinutes, String meditateDesc,
                            int bossKills, String bossDesc,
                            String danHoMenh, String danDesc) {
        this.targetStageName = targetStageName;
        this.killMobs = Math.max(0, killMobs);
        this.mobDesc = mobDesc != null ? mobDesc : "Săn quái dã ngoại hoặc Dungeon";
        this.herbs = Math.max(0, herbs);
        this.herbName = herbName != null ? herbName : "Linh Thảo Tiên Gia";
        this.alchemy = Math.max(0, alchemy);
        this.danName = danName != null ? danName : "Linh Đan Đột Phá";
        this.meditateMinutes = Math.max(0, meditateMinutes);
        this.meditateDesc = meditateDesc != null ? meditateDesc : "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí";
        this.bossKills = Math.max(0, bossKills);
        this.bossDesc = bossDesc != null ? bossDesc : "Boss Thế Giới hoặc Wither";
        this.danHoMenh = danHoMenh != null ? danHoMenh : "Hộ Mệnh Đan";
        this.danDesc = danDesc != null ? danDesc : "Chuẩn bị độ kiếp phá cảnh";
    }

    public String getTargetStageName() { return targetStageName; }
    public int getKillMobs() { return killMobs; }
    public String getMobDesc() { return mobDesc; }
    public int getHerbs() { return herbs; }
    public String getHerbName() { return herbName; }
    public int getAlchemy() { return alchemy; }
    public String getDanName() { return danName; }
    public int getMeditateMinutes() { return meditateMinutes; }
    public String getMeditateDesc() { return meditateDesc; }
    public int getBossKills() { return bossKills; }
    public String getBossDesc() { return bossDesc; }
    public String getDanHoMenh() { return danHoMenh; }
    public String getDanDesc() { return danDesc; }
}
