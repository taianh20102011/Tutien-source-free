package com.tutien.data;

import org.bukkit.entity.EntityType;

/**
 * Cau hinh 1 Linh Thu cho 1 canh gioi.
 * Moi thu duoc khai bao trong config.yml, khong can sua Java khi custom.
 */
public record PetTierConfig(
        String realmKey,
        PetMode mode,
        EntityType entityType,
        String mythicMobsId,
        int mythicMobsLevel,
        String simplePetsType,
        String displayName,
        boolean flies,
        int regenAmplifier,
        int luckAmplifier,
        String attackSkill,
        String defendSkill,
        double attackDamage,
        double defendHealPercent,
        int defendResistanceAmplifier,
        int defendDurationSeconds,
        int attackCooldownSeconds,
        int defendCooldownSeconds
) {
    public enum PetMode { VANILLA, MCPETS, MYTHICMOBS, SIMPLEPETS }
}
