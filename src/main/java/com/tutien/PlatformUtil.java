package com.tutien;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * PlatformUtil - Tuong thich PC (Java) va PE (Bedrock qua Geyser/Floodgate).
 *
 * Bedrock players qua Geyser:
 *  - UUID bat dau bang 00000000-0000-0000-
 *  - Khong co swap-hand event (phim F)
 *  - Off-hand hoat dong han che
 *  - Mot so Particle/Sound khong map duoc → fallback
 */
public class PlatformUtil {

    private static boolean geyserPresent = false;

    public static void init() {
        geyserPresent = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null
            || Bukkit.getPluginManager().getPlugin("Geyser-Paper") != null
            || Bukkit.getPluginManager().getPlugin("floodgate") != null;
        if (geyserPresent) {
            Bukkit.getLogger().info("[TuTien] Phát hiện Geyser/Floodgate - bật chế độ tương thích PE.");
        }
    }

    /** Kiem tra nguoi choi co phai Bedrock (qua Geyser) khong */
    public static boolean isBedrock(Player player) {
        if (!geyserPresent) return false;
        // Geyser Bedrock UUIDs bat dau bang 00000000-0000-0000
        UUID uuid = player.getUniqueId();
        return uuid.getMostSignificantBits() == 0L;
    }

    /** Play sound an toan - fallback neu sound khong ton tai */
    public static void playSound(Player player, Sound sound, float vol, float pitch) {
        try {
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (Exception ignored) {
            // Fallback: pling
            try { player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, vol, pitch); }
            catch (Exception ignored2) {}
        }
    }

    public static void playWorldSound(Location loc, Sound sound, float vol, float pitch) {
        try {
            loc.getWorld().playSound(loc, sound, vol, pitch);
        } catch (Exception ignored) {
            try { loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, vol, pitch); }
            catch (Exception ignored2) {}
        }
    }

    /** Spawn particle an toan */
    public static void spawnParticle(World w, Location loc, Particle p, int count,
                                      double sx, double sy, double sz, double speed) {
        try { w.spawnParticle(p, loc, count, sx, sy, sz, speed); }
        catch (Exception ignored) {
            // Fallback to CRIT
            try { w.spawnParticle(Particle.CRIT, loc, count, sx, sy, sz, speed); }
            catch (Exception ignored2) {}
        }
    }

    /** Material an toan - fallback neu Material khong ton tai (PE compat) */
    public static Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    /**
     * Tao item Tui Linh Thach an toan.
     * PE: dung ENDER_CHEST (Bundle khong ho tro Bedrock)
     * PC: dung SHULKER_BOX mau tim (Bundle co the gay loi tren some versions)
     */
    public static Material getTuiMaterial(Player player) {
        if (isBedrock(player)) return Material.ENDER_CHEST;
        return Material.ENDER_CHEST; // Dung ENDER_CHEST cho ca hai (on dinh nhat)
    }

    /** Kiem tra item co phai la "Tui" theo NBT (khong phu thuoc material) */
    public static boolean hasTuiNbt(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey("tutien", "tui_vo_han");
        return meta.getPersistentDataContainer().has(key,
            org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public static void setTuiNbt(ItemMeta meta) {
        NamespacedKey key = new NamespacedKey("tutien", "tui_vo_han");
        meta.getPersistentDataContainer().set(key,
            org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }

    /** Geyser co san khong */
    public static boolean isGeyserPresent() { return geyserPresent; }
}
