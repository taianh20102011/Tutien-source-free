package com.tutien.data;

/**
 * Chế độ tác chiến của Linh Thú
 */
public enum PetCombatMode {
    AGGRESSIVE("Cuồng Bạo", "§c⚔", "Chủ động hợp kích gây sát thương khi chủ nhân tấn công"),
    GUARDIAN("Hộ Thể", "§b🛡", "Hồi phục sinh lực & tạo kết giới kháng đòn khi chủ nhân nhận sát thương"),
    PASSIVE("Đồng Hành", "§e🕊", "Chỉ bay theo tỏa linh khí tiên gia, không tham gia chiến đấu");

    private final String displayName;
    private final String icon;
    private final String description;

    PetCombatMode(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public PetCombatMode next() {
        PetCombatMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
