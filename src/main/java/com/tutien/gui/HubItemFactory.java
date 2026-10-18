package com.tutien.gui;

import com.tutien.TuTienPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Loads GUI item appearance from hubs.yml with safe vanilla fallbacks. */
public final class HubItemFactory {
    private final TuTienPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public HubItemFactory(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hubs.yml");
        reload();
    }

    public void reload() { yaml = YamlConfiguration.loadConfiguration(file); }

    public ItemStack create(String key, Material fallback, String fallbackName, List<String> fallbackLore) {
        ConfigurationSection s = yaml.getConfigurationSection("hubs.items." + key);
        Material material = fallback;
        String name = fallbackName;
        List<String> lore = new ArrayList<>(fallbackLore == null ? List.of() : fallbackLore);
        String iaId = "";
        int modelData = 0;
        if (s != null) {
            Material configured = Material.matchMaterial(s.getString("material", fallback.name()));
            if (configured != null && configured.isItem()) material = configured;
            name = s.getString("display-name", name).replace('&', '§');
            lore = new ArrayList<>(s.getStringList("lore"));
            for (int i = 0; i < lore.size(); i++) lore.set(i, lore.get(i).replace('&', '§'));
            iaId = s.getString("itemsadder-id", "").trim();
            modelData = Math.max(0, s.getInt("custom-model-data", 0));
        }
        ItemStack stack = resolveItemsAdder(iaId);
        if (stack == null) stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isBlank()) meta.setDisplayName(name);
            if (!lore.isEmpty()) meta.setLore(lore);
            if (modelData > 0) meta.setCustomModelData(modelData);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack resolveItemsAdder(String id) {
        if (id == null || id.isBlank() || plugin.getServer().getPluginManager().getPlugin("ItemsAdder") == null) return null;
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStack.getMethod("getInstance", String.class);
            Object result = getInstance.invoke(null, id);
            if (result == null) return null;
            Object item = result.getClass().getMethod("getItemStack").invoke(result);
            return item instanceof ItemStack stack ? stack.clone() : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().fine("Không thể tải ItemsAdder GUI item '" + id + "': " + ex.getMessage());
            return null;
        }
    }
}
