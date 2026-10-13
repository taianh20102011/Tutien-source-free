package com.tutien.phabao;

/**
 * Cap do Phap Bao - 6 cap:
 *   DIA_PHAM < HA_PHAM < TRUNG_PHAM < THUONG_PHAM < DE_PHAM < TIEN_PHAM
 */
public enum PhaBaoTier {

    DIA_PHAM   ("Địa Phẩm",    "\u00a78", "\u25cb",
                1.05, 1.02, 1.02, 60_000L, 1),

    HA_PHAM    ("Ha Pham",     "\u00a77", "\u25cb",
                1.10, 1.05, 1.05, 40_000L, 1),

    TRUNG_PHAM ("Trung Pham",  "\u00a7a", "\u25ce",
                1.25, 1.15, 1.15, 25_000L, 2),

    THUONG_PHAM("Thượng Phẩm", "\u00a76", "\u25c9",
                1.50, 1.30, 1.30, 15_000L, 3),

    DE_PHAM    ("Đế Phẩm",     "\u00a7c", "\u2606",
                1.80, 1.50, 1.50, 10_000L, 4),

    TIEN_PHAM  ("Tiên Phẩm",   "\u00a7b\u00a7l", "\u2605",
                2.20, 1.80, 1.80, 6_000L,  5);

    private final String displayName;
    private final String color;
    private final String icon;
    private final double atkMult;
    private final double defMult;
    private final double hpMult;
    private final long   abilityCooldown;
    private final int    glowRings; // so vong hieu ung

    PhaBaoTier(String dn, String c, String icon, double atk, double def, double hp, long cd, int rings) {
        this.displayName = dn; this.color = c; this.icon = icon;
        this.atkMult = atk; this.defMult = def; this.hpMult = hp;
        this.abilityCooldown = cd; this.glowRings = rings;
    }

    public String getColoredName()    { return color + icon + " " + displayName; }
    public String getDisplayName()    { return displayName; }
    public String getColor()          { return color; }
    public String getIcon()           { return icon; }
    public double getAtkMult()        { return atkMult; }
    public double getDefMult()        { return defMult; }
    public double getHpMult()         { return hpMult; }
    public long   getAbilityCooldown(){ return abilityCooldown; }
    public int    getGlowRings()      { return glowRings; }
}
