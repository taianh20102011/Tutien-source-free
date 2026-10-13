package com.tutien.phabao;

/**
 * DONG AN (hidden affix) cua Phap Bao - khac voi ky nang chinh (abilityId co
 * dinh theo PhaBaoType), day la cac hieu ung PHU ngau nhien co the xuat hien
 * tren 1 Phap Bao khi roi ra (xem PhaBaoFusionManager#maybeRollAffix) va co
 * the GOP LAI voi nhau khi Dung Hop 2 Phap Bao cung loai (xem
 * PhaBaoFusionManager#attemptFusion).
 *
 * Moi dong an proc (kich hoat) ngau nhien khi nguoi choi CAM Phap Bao co dong
 * an do danh trung don thuong (xem PhaBaoFusionManager#onHit). Ti le proc va
 * do manh doc tu config "phabao.affix.<TEN>.chance" / ".value".
 */
public enum PhaBaoAffix {

    HUT_MAU   ("Hút Máu",    "§c", 0.20, 0.12), // proc 20% -> hoi 12% sat thuong gay ra
    DOT_CHAY  ("Đốt Cháy",   "§6", 0.25, 4.0),  // proc 25% -> dot 4 giay (80 ticks tinh o manager)
    BANG_GIA  ("Băng Giá",   "§b", 0.25, 0.35), // proc 25% -> Slowness (amplifier tinh tu value)
    CHOANG_VANG("Choáng Váng","§d", 0.15, 2.0), // proc 15% -> choang (Blindness+Slowness) 2 giay
    CHI_MANG  ("Chí Mạng",   "§4", 0.15, 0.50), // proc 15% -> +50% sat thuong don do la 1 don rieng
    THAN_TOC  ("Thần Tốc",   "§a", 0.20, 3.0);  // proc 20% -> Speed II ban than 3 giay

    private final String displayName;
    private final String color;
    private final double defaultChance;
    private final double defaultValue;

    PhaBaoAffix(String displayName, String color, double defaultChance, double defaultValue) {
        this.displayName = displayName;
        this.color = color;
        this.defaultChance = defaultChance;
        this.defaultValue = defaultValue;
    }

    public String getDisplayName()  { return displayName; }
    public String getColor()        { return color; }
    public double getDefaultChance(){ return defaultChance; }
    public double getDefaultValue() { return defaultValue; }
    public String getColoredName()  { return color + "✦ " + displayName; }
}
