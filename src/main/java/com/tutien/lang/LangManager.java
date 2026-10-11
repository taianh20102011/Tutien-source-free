package com.tutien.lang;

import com.tutien.TuTienPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * Lightweight locale manager.
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

    private static final String PACK_VERSION = "7.2.8-accented-2";

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
                YamlConfiguration old = YamlConfiguration.loadConfiguration(target);
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
        return YamlConfiguration.loadConfiguration(file);
    }

    public String msg(String key) {
        String value = current.getString("messages." + key);
        if (value == null) value = fallback.getString("messages." + key);
        if (value == null) value = plugin.getConfig().getString("messages." + key);
        return value == null ? key : value;
    }
}
