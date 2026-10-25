package com.tutien.breakthrough;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * TribulationBossArena - Vong Dau Solo Boss khi dot pha tang lon moi (18 tang sieu cap).
 *
 * Khi nguoi choi dot pha CHUYEN sang stage enum moi trong 18 tang lon (ord >= 53):
 *   1. Mot vong tron hang rao BARRIER duoc tao ra xung quanh nguoi choi (ban kinh 9 block)
 *   2. Mot Boss manh xuat hien ben trong vong tron
 *   3. Nguoi choi PHAI solo ha boss moi vuot qua ai
 *   4. Neu chet / het gio -> Dot pha that bai
 *   5. Neu ha boss -> Vong tron bien mat, dot pha thanh cong
 *
 * Do kho Boss tang dan theo tang lon (1-18):
 *   Tang  1-3  : Wither Skeleton Giant (HP 500-800, atk 15-25)
 *   Tang  4-6  : Wither (HP 900-1300,  atk cao hon)
 *   Tang  7-9  : Warden (HP 1500-2500, atk cuc manh)
 *   Tang 10-12 : Song Warden (spawn 2 con)
 *   Tang 13-15 : Warden HP 4000-7000 + toc do nhanh
 *   Tang 16-18 : Boss Cuoi - Warden Thuong De HP 8000-15000
 */
public class TribulationBossArena implements Listener {

    private final TuTienPlugin plugin;

    // Active arena data per player
    private final Map<UUID, ArenaInfo> activeArenas = new HashMap<>();
    // Callbacks
    private final Map<UUID, Runnable> successCallbacks = new HashMap<>();
    private final Map<UUID, Runnable> failCallbacks    = new HashMap<>();

    // ===================================================================
    static class ArenaInfo {
        final UUID playerUUID;
        final Set<UUID> bossUUIDs = new HashSet<>();
        final List<Location> barrierBlocks = new ArrayList<>();
        final Location center;
        final int bossLevel; // 1..18
        boolean complete = false;

        ArenaInfo(UUID p, Location c, int level) {
            this.playerUUID = p; this.center = c.clone(); this.bossLevel = level;
        }
    }
    // ===================================================================

    public TribulationBossArena(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ===================================================================
    //  PUBLIC API - Called by BreakthroughManager after waves complete
    // ===================================================================

    /**
     * Bat dau Boss Arena cho nguoi choi.
     * @param player       Nguoi choi dot pha
     * @param stage        CultivationStage dang dot pha sang
     * @param onSuccess    Callback khi ha boss thanh cong
     * @param onFail       Callback khi that bai (chet / het gio)
     */
    public void startBossChallenge(Player player, CultivationStage stage,
                                   Runnable onSuccess, Runnable onFail) {
        UUID uuid = player.getUniqueId();
        if (activeArenas.containsKey(uuid)) {
            player.sendMessage(plugin.prefix() + "§cBạn đang trong Boss Arena rồi!");
            return;
        }

        // Calculate boss level (1 - 18) based on stage ordinal
        int bossLevel = stage.ordinal() - CultivationStage.VO_THUONG_CHI_TON.ordinal(); // 1..48
        bossLevel = Math.max(1, Math.min(48, bossLevel));

        successCallbacks.put(uuid, onSuccess);
        failCallbacks.put(uuid, onFail);

        // Announce
        broadcastArenaStart(player, stage, bossLevel);

        // Small delay before arena builds (dramatic effect)
        final int finalLevel = bossLevel;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { cleanupAndFail(uuid); return; }
            buildArena(player, stage, finalLevel);
        }, 40L); // 2 sec delay
    }

    public boolean isInBossArena(UUID uuid) {
        return activeArenas.containsKey(uuid);
    }

    /** Called when player dies outside of normal flow (e.g. logout) */
    public void handlePlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeArenas.containsKey(uuid)) {
            cleanupAndFail(uuid);
        }
    }

    // ===================================================================
    //  ARENA BUILDING
    // ===================================================================

    private void buildArena(Player player, CultivationStage stage, int bossLevel) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int radius = 9;

        ArenaInfo info = new ArenaInfo(player.getUniqueId(), center, bossLevel);

        // --- Build circular BARRIER wall (radius=9, height=5 blocks) ---
        for (double angle = 0; angle < 360; angle += 4.0) {
            double rad = Math.toRadians(angle);
            int bx = cx + (int) Math.round(radius * Math.cos(rad));
            int bz = cz + (int) Math.round(radius * Math.sin(rad));
            for (int y = -1; y <= 5; y++) {
                Location loc = new Location(world, bx, cy + y, bz);
                Material current = loc.getBlock().getType();
                if (current == Material.AIR || current == Material.CAVE_AIR || current == Material.VOID_AIR) {
                    loc.getBlock().setType(Material.BARRIER, false);
                    info.barrierBlocks.add(loc);
                }
            }
        }

        // --- Ceiling (barrier roof at y+6) ---
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius + 4) {
                    Location roof = new Location(world, cx + dx, cy + 6, cz + dz);
                    if (roof.getBlock().getType() == Material.AIR) {
                        roof.getBlock().setType(Material.BARRIER, false);
                        info.barrierBlocks.add(roof);
                    }
                }
            }
        }

        activeArenas.put(player.getUniqueId(), info);

        // Spawn boss(es) after arena is built
        spawnBosses(player, info, stage, bossLevel);

        // Announce arena ready
        player.sendTitle(plugin.msg("auto_0359a8e00e"), plugin.msg("auto_63d799db15"), 5, 50, 15);
        player.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1f, 0.4f);

        // Timeout: 8 minutes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ArenaInfo a = activeArenas.get(player.getUniqueId());
            if (a != null && !a.complete) {
                player.sendMessage(plugin.prefix() + "§c⌛ Hết giờ! Boss Arena timeout - Đột phá thất bại!");
                cleanupAndFail(player.getUniqueId());
            }
        }, 9600L); // 8 min
    }

    // ===================================================================
    //  BOSS SPAWNING  (scale by bossLevel 1-18)
    // ===================================================================

    private void spawnBosses(Player player, ArenaInfo info, CultivationStage stage, int bossLevel) {
        Location center = info.center.clone().add(0, 0.5, 3);
        World world = center.getWorld();
        String stageName = stage.getDisplayName();

        // ── Ưu tiên MythicMobs nếu đã hook ──
        if (plugin.hasMythicMobs()) {
            boolean spawnedMM = spawnMythicBosses(info, stage, bossLevel, center, stageName);
            if (spawnedMM) return; // Đã spawn xong qua MythicMobs
        }

        // ── Fallback: vanilla bosses ──
        spawnVanillaBosses(player, info, stage, bossLevel, center, world, stageName);
    }

    /** Spawn boss qua MythicMobs API */
    private boolean spawnMythicBosses(ArenaInfo info, CultivationStage stage,
                                       int bossLevel, Location center, String stageName) {
        com.tutien.hooks.MythicMobsHook mm = plugin.getMythicMobsHook();
        try {
            if (bossLevel <= 18) {
                UUID uuid = mm.spawnBoss(center, bossLevel, stageName);
                if (uuid != null) { info.bossUUIDs.add(uuid); return true; }
            } else if (bossLevel <= 24) {
                // Dual: spawn 2 bosses
                Location p2 = info.center.clone().add(0, 0.5, -4);
                UUID u1 = mm.spawnBoss(center, bossLevel, stageName);
                UUID u2 = mm.spawnBoss(p2, bossLevel, stageName + " II");
                if (u1 != null) info.bossUUIDs.add(u1);
                if (u2 != null) info.bossUUIDs.add(u2);
                return u1 != null || u2 != null;
            } else if (bossLevel <= 30) {
                UUID u1 = mm.spawnBoss(center, bossLevel, stageName);
                if (u1 != null) info.bossUUIDs.add(u1); return u1 != null;
            } else if (bossLevel <= 36) {
                Location p2 = info.center.clone().add(0, 0.5, -5);
                UUID u1 = mm.spawnBoss(center, bossLevel, stageName);
                UUID u2 = mm.spawnBoss(p2, bossLevel, stageName + " II");
                if (u1 != null) info.bossUUIDs.add(u1);
                if (u2 != null) info.bossUUIDs.add(u2);
                return u1 != null;
            } else if (bossLevel <= 42) {
                Location p2 = info.center.clone().add(4, 0.5, -2);
                Location p3 = info.center.clone().add(-4, 0.5, -2);
                UUID u1 = mm.spawnBoss(center, bossLevel, stageName);
                UUID u2 = mm.spawnBoss(p2, bossLevel, stageName + " II");
                UUID u3 = mm.spawnBoss(p3, bossLevel, stageName + " III");
                if (u1 != null) info.bossUUIDs.add(u1);
                if (u2 != null) info.bossUUIDs.add(u2);
                if (u3 != null) info.bossUUIDs.add(u3);
                return u1 != null;
            } else {
                Location p2 = info.center.clone().add(4, 0.5, 2);
                Location p3 = info.center.clone().add(-4, 0.5, 2);
                Location p4 = info.center.clone().add(0, 0.5, -5);
                UUID u1 = mm.spawnBoss(center, bossLevel, stageName);
                UUID u2 = mm.spawnBoss(p2, bossLevel, stageName + " II");
                UUID u3 = mm.spawnBoss(p3, bossLevel, stageName + " III");
                UUID u4 = mm.spawnBoss(p4, bossLevel, stageName + " IV");
                if (u1 != null) info.bossUUIDs.add(u1);
                if (u2 != null) info.bossUUIDs.add(u2);
                if (u3 != null) info.bossUUIDs.add(u3);
                if (u4 != null) info.bossUUIDs.add(u4);
                return u1 != null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BossArena] MythicMobs spawn thất bại, fallback vanilla: " + e.getMessage());
        }
        return false;
    }

    /** Vanilla boss spawner (giữ nguyên logic cũ) */
    private void spawnVanillaBosses(Player player, ArenaInfo info, CultivationStage stage,
                                     int bossLevel, Location center, World world, String stageName) {

        if (bossLevel <= 3) {
            spawnWitherSkeleton(world, center, info, stageName, bossLevel);
        } else if (bossLevel <= 6) {
            spawnWither(world, center, info, stageName, bossLevel, 1);
        } else if (bossLevel <= 9) {
            spawnWarden(world, center, info, stageName, bossLevel, 1);
        } else if (bossLevel <= 12) {
            Location pos2 = info.center.clone().add(0, 0.5, -3);
            spawnWarden(world, center, info, stageName, bossLevel, 1);
            spawnWarden(world, pos2,   info, stageName + " II", bossLevel, 2);
        } else if (bossLevel <= 15) {
            spawnUltraWarden(world, center, info, stageName, bossLevel);
        } else if (bossLevel <= 18) {
            spawnFinalBoss(world, center, info, stageName, bossLevel);

        // ===== 30 TANG TOI THUONG (level 19-48) - CAC CAP DO BOSS TOT HON =====
        } else if (bossLevel <= 21) {
            // Tang 19-21: Final Boss x2 (song tuyet dinh)
            Location pos2 = info.center.clone().add(0, 0.5, -4);
            spawnFinalBoss(world, center, info, stageName,        bossLevel);
            spawnFinalBoss(world, pos2,   info, stageName + " II", bossLevel);
        } else if (bossLevel <= 24) {
            // Tang 22-24: Triple Final Boss
            Location p2 = info.center.clone().add(3.5, 0.5, -2);
            Location p3 = info.center.clone().add(-3.5, 0.5, -2);
            spawnFinalBoss(world, center, info, stageName,          bossLevel);
            spawnFinalBoss(world, p2,     info, stageName + " II",  bossLevel);
            spawnFinalBoss(world, p3,     info, stageName + " III", bossLevel);
        } else if (bossLevel <= 30) {
            // Tang 25-30: Omega Boss (Final Boss + maxed stats)
            spawnOmegaBoss(world, center, info, stageName, bossLevel);
        } else if (bossLevel <= 36) {
            // Tang 31-36: Omega Boss x2
            Location pos2 = info.center.clone().add(0, 0.5, -5);
            spawnOmegaBoss(world, center, info, stageName,        bossLevel);
            spawnOmegaBoss(world, pos2,   info, stageName + " II", bossLevel);
        } else if (bossLevel <= 42) {
            // Tang 37-42: Tri Omega Boss
            Location p2 = info.center.clone().add(4, 0.5, -2);
            Location p3 = info.center.clone().add(-4, 0.5, -2);
            spawnOmegaBoss(world, center, info, stageName,          bossLevel);
            spawnOmegaBoss(world, p2,     info, stageName + " II",  bossLevel);
            spawnOmegaBoss(world, p3,     info, stageName + " III", bossLevel);
        } else {
            // Tang 43-48: TUYET DINH - Omega Boss quad
            Location p2 = info.center.clone().add(4, 0.5, 2);
            Location p3 = info.center.clone().add(-4, 0.5, 2);
            Location p4 = info.center.clone().add(0, 0.5, -5);
            spawnOmegaBoss(world, center, info, stageName,           bossLevel);
            spawnOmegaBoss(world, p2,     info, stageName + " II",   bossLevel);
            spawnOmegaBoss(world, p3,     info, stageName + " III",  bossLevel);
            spawnOmegaBoss(world, p4,     info, stageName + " IV",   bossLevel);
        }
    }

    private void spawnWitherSkeleton(World world, Location loc, ArenaInfo info, String stageName, int level) {
        try {
            WitherSkeleton ws = (WitherSkeleton) world.spawnEntity(loc, EntityType.WITHER_SKELETON);
            double hp = 400.0 + level * 120.0;
            ws.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            ws.setHealth(hp);
            ws.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12.0 + level * 6.0);
            ws.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.38);
            ws.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(5.0 + level * 2.0);
            ws.setCustomName("§c§lKIẾP NẠN VỆ BINH §e[" + stageName + "] §8[" + (int)hp + "❤]");
            ws.setCustomNameVisible(true);
            ws.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(ws.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("[TribulationBoss] Could not spawn WitherSkeleton: " + e.getMessage());
        }
    }

    private void spawnWither(World world, Location loc, ArenaInfo info, String stageName, int level, int idx) {
        try {
            Wither w = (Wither) world.spawnEntity(loc, EntityType.WITHER);
            double hp = 800.0 + level * 200.0;
            w.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            w.setHealth(hp);
            w.setCustomName("§4§lTHIÊN KIẾP CHI LINH §e[" + stageName + "] §8[" + (int)hp + "❤]");
            w.setCustomNameVisible(true);
            w.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(w.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("[TribulationBoss] Could not spawn Wither: " + e.getMessage());
            // fallback to wither skeleton
            spawnWitherSkeleton(world, loc, info, stageName, level);
        }
    }

    private void spawnWarden(World world, Location loc, ArenaInfo info, String stageName, int level, int idx) {
        try {
            LivingEntity warden = trySpawnWarden(world, loc);
            if (warden == null) {
                // Fallback: use Wither with doubled HP
                spawnWither(world, loc, info, stageName + (idx > 1 ? " " + toRoman(idx) : ""), level, idx);
                return;
            }
            double hp = 1200.0 + level * 350.0;
            if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null)
                warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            warden.setHealth(Math.min(hp, warden.getMaxHealth()));
            if (warden.getAttribute(Attribute.GENERIC_ARMOR) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(8.0 + level * 2.0);
            String title = idx > 1 ? "§4§lVỆ THẦN " + toRoman(idx) : "§4§lVỆ THẦN";
            warden.setCustomName(title + " §e[" + stageName + "] §8[" + (int)hp + "❤]");
            warden.setCustomNameVisible(true);
            warden.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(warden.getUniqueId());
        } catch (Exception e) {
            spawnWither(world, loc, info, stageName, level, idx);
        }
    }

    private void spawnUltraWarden(World world, Location loc, ArenaInfo info, String stageName, int level) {
        try {
            LivingEntity warden = trySpawnWarden(world, loc);
            if (warden == null) { spawnWither(world, loc, info, stageName, level, 1); return; }
            double hp = 3500.0 + (level - 12) * 800.0;
            if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null)
                warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            warden.setHealth(Math.min(hp, warden.getMaxHealth()));
            if (warden.getAttribute(Attribute.GENERIC_ARMOR) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(15.0 + (level - 12) * 3.0);
            if (warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null)
                warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.32 + (level - 12) * 0.02);
            warden.setCustomName("§4§l§nKIẾP VỆ THẦN ✦ §e[" + stageName + "] §8[" + (int)hp + "❤]");
            warden.setCustomNameVisible(true);
            warden.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(warden.getUniqueId());
        } catch (Exception e) {
            spawnWither(world, loc, info, stageName, level, 1);
        }
    }

    private void spawnFinalBoss(World world, Location loc, ArenaInfo info, String stageName, int level) {
        // Final boss: extremely powerful Warden with added effects
        try {
            LivingEntity warden = trySpawnWarden(world, loc);
            if (warden == null) { spawnWither(world, loc, info, stageName, level, 1); return; }
            double hp = 8000.0 + (level - 15) * 3000.0; // 8000 / 11000 / 14000
            if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null)
                warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            warden.setHealth(Math.min(hp, warden.getMaxHealth()));
            if (warden.getAttribute(Attribute.GENERIC_ARMOR) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(20.0);
            if (warden.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(8.0);
            if (warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null)
                warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.40);
            String title = level == 18 ? "§c§l§n☠ TUYỆT ĐỈNH VÔ THƯỢNG CHI THẦN ☠"
                         : level == 17 ? "§4§l☠ HÓA THẦN TÔN VỆ THẦN"
                                       : "§4§l☠ VÔ BIÊN KIẾP THẦN";
            warden.setCustomName(title + " §8[" + (int)hp + "❤]");
            warden.setCustomNameVisible(true);
            warden.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(warden.getUniqueId());

            // Visual: lightning strikes around the boss on spawn
            world.strikeLightningEffect(loc);
            world.strikeLightningEffect(loc.clone().add(2, 0, 0));
            world.strikeLightningEffect(loc.clone().add(-2, 0, 0));
        } catch (Exception e) {
            spawnWither(world, loc, info, stageName, level, 1);
        }
    }

    /** Try spawning a Warden (1.19+); returns null if EntityType not available */
    private LivingEntity trySpawnWarden(World world, Location loc) {
        try {
            EntityType wardenType = EntityType.valueOf("WARDEN");
            return (LivingEntity) world.spawnEntity(loc, wardenType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** OMEGA BOSS: Boss cuc manh danh cho 30 tang toi thuong (level 19-48) */
    private void spawnOmegaBoss(World world, Location loc, ArenaInfo info, String stageName, int level) {
        try {
            LivingEntity warden = trySpawnWarden(world, loc);
            if (warden == null) { spawnFinalBoss(world, loc, info, stageName, level); return; }

            // HP scale: level 19=20000, tang 1500/cap, max level 48=63500
            double hp = 20000.0 + (level - 19) * 1500.0;
            if (warden.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null)
                warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            warden.setHealth(Math.min(hp, warden.getMaxHealth()));
            if (warden.getAttribute(Attribute.GENERIC_ARMOR) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(25.0);
            if (warden.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS) != null)
                warden.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(12.0);
            if (warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null)
                warden.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45 + (level - 19) * 0.002);
            if (warden.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null)
                warden.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(30.0 + (level - 19) * 1.5);
            if (warden.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null)
                warden.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);

            String prefix = level >= 43 ? "§c§l§n⚔ TỨ TUYỆT ĐỈnh " : level >= 37 ? "§4§l⚔ TAM OMEGA "
                          : level >= 31 ? "§5§l⚔ SONG OMEGA " : "§4§l⚔ OMEGA";
            warden.setCustomName(prefix + " §e[" + stageName + "] §8[" + fmt((long)hp) + "❤]");
            warden.setCustomNameVisible(true);
            warden.setRemoveWhenFarAway(false);
            info.bossUUIDs.add(warden.getUniqueId());

            // Lightning show on omega spawn
            world.strikeLightningEffect(loc);
            for (int i = 0; i < 3; i++) {
                double a = i * 120 * (Math.PI / 180.0);
                world.strikeLightningEffect(loc.clone().add(Math.cos(a)*2.5, 0, Math.sin(a)*2.5));
            }
        } catch (Exception e) {
            spawnFinalBoss(world, loc, info, stageName, level);
        }
    }

    private String fmt(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }

    // ===================================================================
    //  EVENTS
    // ===================================================================

    /** When a boss inside an arena is killed */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        UUID deadId = dead.getUniqueId();

        for (Map.Entry<UUID, ArenaInfo> entry : new ArrayList<>(activeArenas.entrySet())) {
            ArenaInfo info = entry.getValue();
            if (!info.bossUUIDs.contains(deadId)) continue;

            info.bossUUIDs.remove(deadId);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // If all bosses are dead -> success!
            if (info.bossUUIDs.isEmpty() && !info.complete) {
                info.complete = true;
                UUID playerUUID = entry.getKey();
                Player player = Bukkit.getPlayer(playerUUID);

                // Remove arena after 1 sec so player can see the boss fall
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    removeArenaBlocks(info);
                    // Kill any remaining entities in arena
                    killRemainingBosses(info);
                }, 20L);

                if (player != null && player.isOnline()) {
                    player.sendTitle(plugin.msg("auto_9ab0121d2d"), plugin.msg("auto_deb1eee14b"), 10, 70, 20);
                    player.sendMessage(plugin.msg("auto_da39a3ee5e"));
                    player.sendMessage(plugin.msg("auto_83304f8b0f"));
                    player.sendMessage(plugin.msg("auto_e9d8e30a29"));
                    player.sendMessage(plugin.msg("auto_da39a3ee5e"));
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
                }

                Runnable success = successCallbacks.remove(playerUUID);
                failCallbacks.remove(playerUUID);
                activeArenas.remove(playerUUID);

                if (success != null) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, success, 30L);
                }
            }
            return;
        }
    }

    /** Prevent player from moving outside arena while in boss fight */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ArenaInfo info = activeArenas.get(player.getUniqueId());
        if (info == null || info.complete) return;

        Location to = event.getTo();
        if (to == null) return;
        double dx = to.getX() - info.center.getX();
        double dz = to.getZ() - info.center.getZ();
        // If player somehow exits arena radius, push back
        if (dx * dx + dz * dz > 10 * 10) {
            player.teleport(info.center.clone().add(0, 0.5, 0));
            player.sendMessage(plugin.prefix() + "§cBạn không thể thoát khỏi Boss Arena!");
        }
    }

    /** If player takes lethal damage in arena - fail */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ArenaInfo info = activeArenas.get(player.getUniqueId());
        if (info == null || info.complete) return;
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            info.complete = true;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                cleanupAndFail(player.getUniqueId());
            }, 2L);
        }
    }

    // ===================================================================
    //  CLEANUP
    // ===================================================================

    private void cleanupAndFail(UUID playerUUID) {
        ArenaInfo info = activeArenas.remove(playerUUID);
        if (info != null) {
            removeArenaBlocks(info);
            killRemainingBosses(info);
        }
        successCallbacks.remove(playerUUID);
        Runnable fail = failCallbacks.remove(playerUUID);
        Player player = Bukkit.getPlayer(playerUUID);
        if (fail != null && player != null && player.isOnline()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, fail, 5L);
        }
    }

    private void removeArenaBlocks(ArenaInfo info) {
        new BukkitRunnable() {
            @Override public void run() {
                for (Location loc : info.barrierBlocks) {
                    try {
                        if (loc.getBlock().getType() == Material.BARRIER) {
                            loc.getBlock().setType(Material.AIR, false);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTask(plugin);
    }

    private void killRemainingBosses(ArenaInfo info) {
        for (UUID id : new HashSet<>(info.bossUUIDs)) {
            try {
                Bukkit.getEntity(id);
                for (World w : Bukkit.getWorlds()) {
                    w.getEntities().stream()
                        .filter(e -> e.getUniqueId().equals(id))
                        .findFirst()
                        .ifPresent(Entity::remove);
                }
            } catch (Exception ignored) {}
        }
        info.bossUUIDs.clear();
    }

    // ===================================================================
    //  ANNOUNCEMENTS
    // ===================================================================

    private void broadcastArenaStart(Player player, CultivationStage stage, int bossLevel) {
        String bossDesc = getBossDescription(bossLevel);
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_d48765abfa"));
        player.sendMessage(plugin.msg("auto_817bca3519"));
        player.sendMessage(plugin.msg("auto_1974ce5a0f"));
        player.sendMessage("  §7Cảnh giới: §e" + stage.getColor() + stage.getDisplayName());
        player.sendMessage("  §7Boss:      §c" + bossDesc);
        player.sendMessage("  §7Cấp độ:   §4" + bossLevel + "/18");
        player.sendMessage(plugin.msg("auto_e51b7b9987"));
        player.sendMessage(plugin.msg("auto_c217ccdb3c"));
        player.sendMessage(plugin.msg("auto_4847a60cd7"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));

        // Broadcast nearby (radius 200)
        String msg = "§4§l☠ §c" + player.getName() + " §7đang đối mặt với §4" + bossDesc
            + " §7để đột phá §e" + stage.getDisplayName() + "§7!";
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld().equals(player.getWorld()) &&
                online.getLocation().distanceSquared(player.getLocation()) < 200 * 200) {
                online.sendMessage(msg);
            }
        }
    }

    private String getBossDescription(int level) {
        return switch (level) {
            case 1  -> "Kiếp Nạn Vệ Binh [Wither Skeleton]";
            case 2  -> "Cường Kiếp Vệ Binh [Wither Skeleton+]";
            case 3  -> "Thượng Kiếp Vệ Binh [Wither Skeleton++]";
            case 4  -> "Thiên Kiếp Chi Linh [Wither Lv.1]";
            case 5  -> "Thiên Kiếp Chi Linh [Wither Lv.2]";
            case 6  -> "Thượng Thiên Kiếp Linh [Wither Lv.3]";
            case 7  -> "Vệ Thần [Warden Lv.1]";
            case 8  -> "Cường Vệ Thần [Warden Lv.2]";
            case 9  -> "Thượng Vệ Thần [Warden Lv.3]";
            case 10 -> "Song Vệ Thần [2x Warden]";
            case 11 -> "Song Vệ Thần Cường [2x Warden+]";
            case 12 -> "Song Vệ Thần Thượng [2x Warden++]";
            case 13 -> "Kiếp Vệ Thần ✦ [Ultra Warden Lv.1]";
            case 14 -> "Kiếp Vệ Thần ✦✦ [Ultra Warden Lv.2]";
            case 15 -> "Kiếp Vệ Thần ✦✦✦ [Ultra Warden Lv.3]";
            case 16 -> "Vô Biên Kiếp Thần ☠ [Final Boss Lv.1]";
            case 17 -> "Hóa Thần Tôn Vệ Thần ☠ [Final Boss Lv.2]";
            case 18 -> "Tuyệt Đỉnh Vô Thượng Chi Thần ☠ [Final Boss Lv.3]";
            case 19 -> "Song Tuyệt Đỉnh Chi Thần ⚔ [Dual Final Lv.1]";
            case 20 -> "Song Tuyệt Đỉnh Cường ⚔ [Dual Final Lv.2]";
            case 21 -> "Song Tuyệt Đỉnh Thượng ⚔ [Dual Final Lv.3]";
            case 22 -> "Tam Tuyệt Đỉnh Chi Thần ⚔⚔ [Triple Final Lv.1]";
            case 23 -> "Tam Tuyệt Đỉnh Cường ⚔⚔ [Triple Final Lv.2]";
            case 24 -> "Tam Tuyệt Đỉnh Thượng ⚔⚔ [Triple Final Lv.3]";
            case 25 -> "Omega Thần ☠⚔ [Omega Boss Lv.1 — 20.000❤]";
            case 26 -> "Omega Thần Cường ☠⚔ [Omega Boss Lv.2 — 21.500❤]";
            case 27 -> "Omega Thần Thượng ☠⚔ [Omega Boss Lv.3 — 23.000❤]";
            case 28 -> "Omega Tuyệt Đỉnh I ☠ [Omega Boss Lv.4 — 24.500❤]";
            case 29 -> "Omega Tuyệt Đỉnh II ☠ [Omega Boss Lv.5 — 26.000❤]";
            case 30 -> "Omega Tuyệt Đỉnh III ☠ [Omega Boss Lv.6 — 27.500❤]";
            case 31, 32, 33 -> "Song Omega Thần ☠☠ [2x Omega — Lv." + (level-30) + "]";
            case 34, 35, 36 -> "Song Omega Cường ☠☠ [2x Omega+ — Lv." + (level-33) + "]";
            case 37, 38, 39 -> "Tam Omega Thần ☠☠☠ [3x Omega — Lv." + (level-36) + "]";
            case 40, 41, 42 -> "Tam Omega Cường ☠☠☠ [3x Omega+ — Lv." + (level-39) + "]";
            case 43, 44, 45 -> "Tứ Tuyệt Đỉnh Omega ⚔⚔⚔⚔ [4x Omega — Lv." + (level-42) + "]";
            case 46, 47, 48 -> "THỦY TINH VĨNH HẰNG KIẾP THẦN ⚔⚔⚔⚔ [ĐỈNH CAO — Lv." + (level-45) + "]";
            default -> "Kiếp Nạn Boss [Lv." + level + "]";
        };
    }

    // ===================================================================
    //  UTILS
    // ===================================================================

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; default -> String.valueOf(n);
        };
    }
}
