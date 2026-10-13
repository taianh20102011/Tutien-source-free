package com.tutien.zone;

import com.tutien.TuTienPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ZoneManager implements Listener {

    private final TuTienPlugin plugin;
    private File dataFile;
    private static final String FILE_NAME = "zones.yml";

    public enum ZoneType {
        BINH_THUONG("Bình Thường", 1.5, Color.fromRGB(100,200,255), "§9"),
        LINH_MACH   ("Linh Mạch",  2.0, Color.fromRGB( 50,255,200), "§b"),
        THANH_DIA   ("Thánh Địa",  2.5, Color.fromRGB(255,220,  0), "§e"),
        LINH_SAN    ("Linh Sơn",   3.0, Color.fromRGB(200, 80,255), "§5"),
        THIEN_DIA   ("Thiên Địa",  4.0, Color.fromRGB(255,100, 30), "§6");
        public final String displayName; public final double mult;
        public final Color color;        public final String chat;
        ZoneType(String d, double m, Color c, String cc) { displayName=d; mult=m; color=c; chat=cc; }
    }

    public static class Zone {
        public final int id; public final Location center;
        public double radius; public ZoneType type; public String name;
        public Zone(int i, Location c, double r, ZoneType t, String n) {
            id=i; center=c.clone(); radius=r; type=t; name=n;
        }
    }

    private final List<Zone> zones = new ArrayList<>();
    private int nextId = 1;
    private final Map<UUID,Integer> inZone = new HashMap<>();

    public ZoneManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        loadZones();
        startZoneCheck();
        startZoneEffects();
    }

    // ========================
    //  PERSISTENCE
    // ========================

    private void loadZones() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        nextId = cfg.getInt("nextId", 1);
        if (!cfg.contains("zones")) return;
        for (String key : cfg.getConfigurationSection("zones").getKeys(false)) {
            try {
                int id      = cfg.getInt("zones." + key + ".id");
                String wn   = cfg.getString("zones." + key + ".world", "world");
                double x    = cfg.getDouble("zones." + key + ".x");
                double y    = cfg.getDouble("zones." + key + ".y");
                double z    = cfg.getDouble("zones." + key + ".z");
                double r    = cfg.getDouble("zones." + key + ".radius");
                String tn   = cfg.getString("zones." + key + ".type", "LINH_MACH");
                String n    = cfg.getString("zones." + key + ".name", "Zone");
                World w = Bukkit.getWorld(wn);
                if (w == null) w = Bukkit.getWorlds().get(0);
                ZoneType t;
                try { t = ZoneType.valueOf(tn); } catch (Exception e) { t = ZoneType.LINH_MACH; }
                zones.add(new Zone(id, new Location(w, x, y, z), r, t, n));
            } catch (Exception e) {
                plugin.getLogger().warning("[ZoneManager] Failed to load zone: " + key);
            }
        }
        plugin.getLogger().info("[ZoneManager] Loaded " + zones.size() + " zones.");
    }

    public void saveZones() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nextId", nextId);
        for (Zone z : zones) {
            String key = "zones.z" + z.id;
            cfg.set(key + ".id",     z.id);
            cfg.set(key + ".world",  z.center.getWorld() != null ? z.center.getWorld().getName() : "world");
            cfg.set(key + ".x",      z.center.getX());
            cfg.set(key + ".y",      z.center.getY());
            cfg.set(key + ".z",      z.center.getZ());
            cfg.set(key + ".radius", z.radius);
            cfg.set(key + ".type",   z.type.name());
            cfg.set(key + ".name",   z.name);
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("[ZoneManager] Cannot save zones: " + e.getMessage()); }
    }

    // ========================
    //  ZONE MANAGEMENT
    // ========================

    public Zone createZone(Location center, double radius, ZoneType type, String name) {
        Zone z = new Zone(nextId++, center, radius, type, name);
        zones.add(z);
        saveZones();
        return z;
    }
    public Zone createZone(Location center, double radius, String typeName, String name) {
        ZoneType t; try { t = ZoneType.valueOf(typeName.toUpperCase()); } catch (Exception e) { t = ZoneType.BINH_THUONG; }
        return createZone(center, radius, t, name);
    }
    public boolean removeZone(int id) {
        boolean removed = zones.removeIf(z -> z.id == id);
        if (removed) saveZones();
        return removed;
    }
    public List<Zone> getZones() { return zones; }

    public Zone getZoneAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Zone z : zones) {
            if (!z.center.getWorld().equals(loc.getWorld())) continue;
            if (z.center.distance(loc) <= z.radius) return z;
        }
        return null;
    }
    public boolean isInZone(Location loc)       { return getZoneAt(loc) != null; }
    public double getLKMultiplier(Location loc) { Zone z = getZoneAt(loc); return z != null ? z.type.mult : 1.0; }
    public long applyZoneBonus(Player p, long base) {
        Zone z = getZoneAt(p.getLocation()); return z != null ? (long)(base*z.type.mult) : base;
    }
    public void addZone(Location center, double radius, String name) {
        createZone(center, radius, ZoneType.LINH_MACH, name);
    }

    // ========================
    //  ENTER / LEAVE
    // ========================

    private void startZoneCheck() {
        new BukkitRunnable() { @Override public void run() {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Zone cur = getZoneAt(p.getLocation());
                int curId = cur!=null?cur.id:-1, lastId = inZone.getOrDefault(p.getUniqueId(),-1);
                if (curId != lastId) {
                    if (cur != null) { inZone.put(p.getUniqueId(), curId); onEnter(p, cur); }
                    else { inZone.remove(p.getUniqueId()); onLeave(p); }
                }
            }
        }}.runTaskTimer(plugin, 20L, 40L);
    }

    private void onEnter(Player p, Zone z) {
        String c = z.type.chat;
        p.sendMessage(plugin.msg("auto_da39a3ee5e")); p.sendMessage(plugin.prefix() + c + "✦ Vào §l" + z.name + c + " [" + z.type.displayName + "] — LK §l×" + z.type.mult);
        p.sendTitle(c + "✦ " + z.name, "§7Linh Khí ×" + z.type.mult, 5, 50, 15);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.6f);
        World w = p.getWorld(); Location l = p.getLocation().clone();
        new BukkitRunnable() { int t=0; @Override public void run() {
            if (t++>=20){cancel();return;}
            for (int i=0;i<3;i++) { double a=Math.toRadians(t*18+i*120), r=t*0.18;
                w.spawnParticle(Particle.DUST, l.clone().add(Math.cos(a)*r,0.05,Math.sin(a)*r),2,0.1,0.1,0.1,0,new Particle.DustOptions(z.type.color,1.5f));}
            w.spawnParticle(Particle.ENCHANTED_HIT, l.clone().add(0,1,0),2,0.4,0.4,0.4,0.1);
        }}.runTaskTimer(plugin, 0L, 1L);
    }
    private void onLeave(Player p) {
        p.sendMessage(plugin.prefix() + "§7✦ Đã rời khỏi khu vực Linh Mạch.");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f);
    }

    // ========================
    //  AMBIENT EFFECTS
    // ========================

    private void startZoneEffects() {
        new BukkitRunnable() { double tick=0; @Override public void run() {
            tick += 0.3;
            for (Zone z : zones) {
                if (z.center.getWorld()==null) continue;
                World w = z.center.getWorld();
                int pts = Math.max(12,(int)(z.radius*3));
                for (int i=0;i<pts;i++) {
                    double a=Math.toRadians(360.0/pts*i+tick*20), yy=0.1+Math.sin(tick+i*0.3)*0.4;
                    w.spawnParticle(Particle.DUST,z.center.clone().add(Math.cos(a)*z.radius,yy,Math.sin(a)*z.radius),1,0,0,0,0,new Particle.DustOptions(z.type.color,1.2f));
                }
                for (int i=0;i<3;i++) {
                    double rx=(Math.random()-0.5)*z.radius*1.5, rz=(Math.random()-0.5)*z.radius*1.5;
                    if (Math.sqrt(rx*rx+rz*rz)>z.radius) continue;
                    w.spawnParticle(Particle.DUST,z.center.clone().add(rx,0.1,rz),1,0,0.1,0,0.02,new Particle.DustOptions(z.type.color,0.8f));
                }
                if (z.type.ordinal()>=ZoneType.THANH_DIA.ordinal())
                    w.spawnParticle(Particle.ENCHANTED_HIT,z.center.clone().add(0,1.5+Math.sin(tick)*0.5,0),2,0.3,0.3,0.3,0.2);
            }
        }}.runTaskTimer(plugin, 20L, 10L);
    }

    public void showZoneList(org.bukkit.command.CommandSender s) {
        s.sendMessage("§6§l=== Linh Mạch Zones (" + zones.size() + ") ===");
        if (zones.isEmpty()) { s.sendMessage(plugin.msg("auto_a8ffe1f132")); s.sendMessage(plugin.msg("auto_5dbab3309c")); return; }
        for (Zone z : zones) {
            Location c2 = z.center;
            s.sendMessage(String.format("  §e#%d §f%s §8[%s] §7R=%.0fm §aLK×%.1f §8(%d,%d,%d %s)",
                z.id,z.name,z.type.displayName,z.radius,z.type.mult,c2.getBlockX(),c2.getBlockY(),c2.getBlockZ(),
                c2.getWorld()!=null?c2.getWorld().getName():"?"));
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { inZone.remove(e.getPlayer().getUniqueId()); }
}
