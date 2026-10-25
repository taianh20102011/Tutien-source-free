package com.tutien.lang;

import com.tutien.TuTienPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Lightweight locale manager with full UTF-8 support.
 *
 * Messages are extracted from source into lang/vi.yml and lang/en.yml.
 * Runtime locale is controlled by language.locale in config.yml.
 */
public final class LangManager {

    private final TuTienPlugin plugin;
    private YamlConfiguration current;
    private YamlConfiguration fallback;

    public LangManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        ensureFiles();
        reload();
    }

    private static final String PACK_VERSION = "7.7.3-vietnamese-full";

    private void ensureFiles() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        File dir = new File(plugin.getDataFolder(), "lang");
        if (!dir.exists()) dir.mkdirs();
        updateBundledPack(new File(dir, "vi.yml"), "lang/vi.yml");
        updateBundledPack(new File(dir, "en.yml"), "lang/en.yml");
    }

    private void updateBundledPack(File target, String resource) {
        boolean update = !target.exists();
        if (!update) {
            try {
                YamlConfiguration old = loadFile(target);
                update = !PACK_VERSION.equals(old.getString("pack-version"));
            } catch (Exception ignored) { update = true; }
        }
        if (update) {
            if (target.exists()) target.delete();
            plugin.saveResource(resource, false);
        }
    }

    public void reload() {
        String locale = plugin.getConfig().getString("language.locale", "vi");
        String fallbackLocale = plugin.getConfig().getString("language.fallback", "vi");
        current = load(locale);
        fallback = load(fallbackLocale);
    }

    private YamlConfiguration load(String locale) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) file = new File(plugin.getDataFolder(), "lang/vi.yml");
        return loadFile(file);
    }

    private YamlConfiguration loadFile(File file) {
        YamlConfiguration config = new YamlConfiguration();
        if (file == null || !file.exists()) return config;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (Exception e) {
            plugin.getLogger().warning("[LangManager] Không thể tải " + file.getName() + ": " + e.getMessage());
        }
        return config;
    }

    public String msg(String key) {
        if (key == null) return "";
        String value = current != null ? current.getString("messages." + key) : null;
        if (value == null && fallback != null) value = fallback.getString("messages." + key);
        if (value == null) value = plugin.getConfig().getString("messages." + key);
        if (value == null) {
            if (current != null && current.contains(key)) value = current.getString(key);
            else if (fallback != null && fallback.contains(key)) value = fallback.getString(key);
        }
        if (value == null) return key;
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
