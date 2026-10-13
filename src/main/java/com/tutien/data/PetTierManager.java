package com.tutien.data;

import com.tutien.TuTienPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Doc pet theo tung CANH GIOI tu config.yml.
 * Uu tien stages.<stage-name>, sau do fallback ve tiers.<realm>.
 */
public class PetTierManager {
    private final TuTienPlugin plugin;
    private final Map<String, PetTierConfig> stageTiers = new LinkedHashMap<>();
    private final Map<String, PetTierConfig> realmTiers = new LinkedHashMap<>();
    private PetTierConfig defaultTier;
    private boolean enabled = true;
    private List<PetTierConfig.PetMode> fallbackOrder = List.of(
            PetTierConfig.PetMode.MYTHICMOBS,
            PetTierConfig.PetMode.SIMPLEPETS,
            PetTierConfig.PetMode.VANILLA
    );

    public PetTierManager(TuTienPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        stageTiers.clear();
        realmTiers.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("linh-thu");
        PetTierConfig hardDefault = new PetTierConfig(
                "default", PetTierConfig.PetMode.VANILLA, EntityType.WOLF,
                "", 1, "wolf", "Linh Thú", false, 0, 0,
                "", "", 2.0, 0.10, 0, 3, 8, 12);
        if (root == null) {
            enabled = true;
            defaultTier = hardDefault;
            plugin.getLogger().warning("[LinhThu] Không tìm thấy section linh-thu.");
            return;
        }
        enabled = root.getBoolean("enabled", true);
        List<String> order = root.getStringList("fallback-order");
        if (!order.isEmpty()) fallbackOrder = order.stream()
                .map(s -> parseMode(s, PetTierConfig.PetMode.VANILLA)).toList();
        defaultTier = parseTier("default", root.getConfigurationSection("default"), hardDefault);

        loadSection(root.getConfigurationSection("tiers"), realmTiers);
        loadSection(root.getConfigurationSection("stages"), stageTiers);
        plugin.getLogger().info("[LinhThu] Đã nạp " + stageTiers.size() + " pet theo cảnh giới + "
                + realmTiers.size() + " pet fallback theo giới.");
    }

    private void loadSection(ConfigurationSection sec, Map<String, PetTierConfig> target) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            PetTierConfig cfg = parseTier(key, sec.getConfigurationSection(key), defaultTier);
            if (cfg != null) target.put(key, cfg);
        }
    }

    private PetTierConfig parseTier(String key, ConfigurationSection sec, PetTierConfig fallback) {
        if (sec == null) return fallback;
        EntityType type = fallback.entityType();
        try { type = EntityType.valueOf(sec.getString("entity-type", type.name()).toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[LinhThu] entity-type sai ở '" + key + "', dùng " + type);
        }
        return new PetTierConfig(
                key,
                parseMode(sec.getString("mode", fallback.mode().name()), fallback.mode()),
                type,
                sec.getString("mythicmobs-id", fallback.mythicMobsId()),
                sec.getInt("mythicmobs-level", fallback.mythicMobsLevel()),
                sec.getString("simplepets-type", fallback.simplePetsType()),
                sec.getString("display-name", fallback.displayName()),
                sec.getBoolean("flies", fallback.flies()),
                sec.getInt("regen-amplifier", fallback.regenAmplifier()),
                sec.getInt("luck-amplifier", fallback.luckAmplifier()),
                sec.getString("attack-skill", fallback.attackSkill()),
                sec.getString("defend-skill", fallback.defendSkill()),
                sec.getDouble("attack-damage", fallback.attackDamage()),
                sec.getDouble("defend-heal-percent", fallback.defendHealPercent()),
                sec.getInt("defend-resistance-amplifier", fallback.defendResistanceAmplifier()),
                sec.getInt("defend-duration-seconds", fallback.defendDurationSeconds()),
                sec.getInt("attack-cooldown-seconds", fallback.attackCooldownSeconds()),
                sec.getInt("defend-cooldown-seconds", fallback.defendCooldownSeconds())
        );
    }

    private PetTierConfig.PetMode parseMode(String raw, PetTierConfig.PetMode fallback) {
        if (raw == null) return fallback;
        try { return PetTierConfig.PetMode.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    public PetTierConfig getTier(CultivationStage stage) {
        if (stage == null) return defaultTier;
        PetTierConfig exact = stageTiers.get(stage.name());
        if (exact != null) return exact;
        return realmTiers.getOrDefault(stage.getRealm(), defaultTier);
    }

    /** Backward-compatible API. */
    public PetTierConfig getTier(String realm) { return realmTiers.getOrDefault(realm, defaultTier); }
    public boolean isEnabled() { return enabled; }
    public List<PetTierConfig.PetMode> getFallbackOrder() { return fallbackOrder; }
}
