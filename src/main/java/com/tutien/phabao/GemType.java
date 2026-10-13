package com.tutien.phabao;

import org.bukkit.Material;

/**
 * Loai Ngoc dung de Kham Nam vao Phap Bao. Moi loai cong VINH VIEN vao 1
 * trong 3 chi so goc cua Phap Bao (ATK/DEF/HP) - cung 3 chi so ma Tier va
 * Cuong Hoa dang dung, de de tinh toan/hien thi dong nhat.
 */
public enum GemType {

    HOA ("Ngọc Hỏa", "§c", Material.FIRE_CHARGE,   "ATK"),
    BANG("Ngọc Băng", "§b", Material.PRISMARINE_CRYSTALS, "DEF"),
    LOI ("Ngọc Lôi",  "§e", Material.GLOWSTONE_DUST, "HP");

    private final String displayName;
    private final String color;
    private final Material material;
    private final String statCode; // "ATK" | "DEF" | "HP"

    GemType(String displayName, String color, Material material, String statCode) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.statCode = statCode;
    }

    public String getDisplayName() { return displayName; }
    public String getColor()       { return color; }
    public Material getMaterial()  { return material; }
    public String getStatCode()    { return statCode; }
}
