package com.tutien.tongmon;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityInteractEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Rental-based sect territory with native protection and overlap checks for WorldGuard/GriefPrevention. */
public final class TongMonLandManager implements Listener {
    private final TuTienPlugin plugin;
    private final TongMonManager tongMonManager;
    private final File file;
    private final File settingsFile;
    private YamlConfiguration settings;
    private final Map<String, Land> lands = new LinkedHashMap<>();
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Set<UUID> warnedMove = new HashSet<>();
    private final Map<UUID, Long> lastProtectionMessage = new HashMap<>();

    public TongMonLandManager(TuTienPlugin plugin, TongMonManager tongMonManager) {
        this.plugin = plugin; this.tongMonManager = tongMonManager;
        this.file = new File(plugin.getDataFolder(), "tongmon-land.yml");
        this.settingsFile = new File(plugin.getDataFolder(), "tongmon-config.yml");
        this.settings = YamlConfiguration.loadConfiguration(settingsFile);
        load();
        // Native claims are reconciled after all soft-dependencies have completed startup.
        Bukkit.getScheduler().runTaskLater(plugin, this::ensureNativeProtectionForLoadedLands, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::purgeExpiredLands, 20L, 1200L);
    }

    public void setPosition(Player player, int slot) {
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection());
        Point point = new Point(player.getWorld().getName(), player.getLocation().getBlockX(),
                player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        if (slot == 1) selection.first = point; else selection.second = point;
        player.sendMessage(plugin.prefix() + "§aĐã đặt vị trí " + (slot == 1 ? "§e1" : "§e2")
                + "§a tại §f" + point.world + " " + point.x + " " + point.y + " " + point.z + "§a.");
    }

    public boolean rent(Player player) {
        if (!settings.getBoolean("land.enabled", true)) {
            player.sendMessage(plugin.prefix() + "§cTính năng thuê địa bàn đang bị tắt trong tongmon-config.yml."); return false;
        }
        TongMon tm = tongMonManager.getTongMon(player.getUniqueId());
        if (tm == null || !tm.canManage(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ, Phó Tông Chủ hoặc cấp quản lý được thuê địa bàn."); return false;
        }
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.first == null || selection.second == null) {
            player.sendMessage(plugin.prefix() + "§cĐặt hai góc bằng §e/tongmon dat pos1 §cvà §e/tongmon dat pos2§c trước."); return false;
        }
        Point p1 = selection.first, p2 = selection.second;
        if (!p1.world.equals(p2.world)) { player.sendMessage(plugin.prefix() + "§cHai góc phải ở cùng một thế giới."); return false; }
        int minX = Math.min(p1.x, p2.x), maxX = Math.max(p1.x, p2.x);
        int minY = Math.min(p1.y, p2.y), maxY = Math.max(p1.y, p2.y);
        int minZ = Math.min(p1.z, p2.z), maxZ = Math.max(p1.z, p2.z);
        // Hard caps protect against accidental huge config values causing expensive claim scans.
        int maxSide = Math.max(8, Math.min(128, settings.getInt("land.max-side", 64)));
        int maxHeight = Math.max(8, Math.min(384, settings.getInt("land.max-height", 128)));
        if ((long) maxX - minX + 1 > maxSide || (long) maxZ - minZ + 1 > maxSide) {
            player.sendMessage(plugin.prefix() + "§cĐịa bàn vượt giới hạn " + maxSide + " block mỗi chiều ngang (land.max-side)."); return false;
        }
        if ((long) maxY - minY + 1 > maxHeight) {
            player.sendMessage(plugin.prefix() + "§cĐịa bàn vượt giới hạn cao " + maxHeight + " block (land.max-height)."); return false;
        }
        World world = Bukkit.getWorld(p1.world);
        if (world == null) { player.sendMessage(plugin.prefix() + "§cThế giới không đang tải."); return false; }
        long now = System.currentTimeMillis();
        Land existing = lands.get(tm.getId());
        if (existing != null && existing.expiresAt > now) {
            player.sendMessage(plugin.prefix() + "§eTông Môn đã có địa bàn. Dùng §f/tongmon dat giahan §eđể gia hạn."); return false;
        }
        if (existing != null) {
            if (!removeNativeProtection(existing)) {
                player.sendMessage(plugin.prefix() + "§cĐịa bàn cũ hết hạn nhưng chưa gỡ được claim native. Hệ thống sẽ thử lại; chưa thể thuê mới để tránh claim mồ côi.");
                return false;
            }
            lands.remove(tm.getId());
            save();
        }
        Land candidate = new Land(tm.getId(), p1.world, minX, minY, minZ, maxX, maxY, maxZ, 0L);
        for (Land land : lands.values()) {
            if (land.expiresAt <= now || land.tongMonId.equals(tm.getId())) continue;
            if (candidate.intersects(land)) { player.sendMessage(plugin.prefix() + "§cĐịa bàn chồng lấn với Tông Môn khác."); return false; }
        }
        if (hasExternalClaim(candidate, world)) {
            player.sendMessage(plugin.prefix() + "§cĐịa bàn này đang thuộc vùng bảo vệ của WorldGuard/GriefPrevention. Hãy chọn nơi khác."); return false;
        }
        long cost = Math.max(0L, settings.getLong("land.rent-cost", 50000L));
        int days = Math.max(1, Math.min(3650, settings.getInt("land.duration-days", 30)));
        if (tm.getTienKho() < cost) {
            player.sendMessage(plugin.prefix() + "§cKho Tông Môn cần §e" + cost + " §cLinh Thạch để thuê."); return false;
        }
        candidate.expiresAt = now + days * 86_400_000L;
        if (!createNativeProtection(candidate, tm, player)) {
            player.sendMessage(plugin.prefix() + "§cKhông thể tạo claim/region native. Chưa trừ phí thuê; xem console để biết nguyên nhân.");
            return false;
        }
        // A configured zero rent is valid; withdrawKho intentionally rejects non-positive amounts.
        if (cost > 0 && !tm.withdrawKho(cost)) {
            removeNativeProtection(candidate);
            player.sendMessage(plugin.prefix() + "§cKhông thể trừ phí từ kho Tông Môn; đã hoàn tác claim.");
            return false;
        }
        lands.put(tm.getId(), candidate);
        tongMonManager.save(); save();
        player.sendMessage(plugin.prefix() + "§aĐã thuê địa bàn Tông Môn trong §e" + days + " ngày§a. Phí: §e" + cost + "§a.");
        player.sendMessage(plugin.prefix() + "§7Người ngoài không thể phá/đặt block; quyền đi vào vùng được điều khiển bởi thỏa thuận liên minh.");
        return true;
    }

    public boolean renew(Player player) {
        if (!settings.getBoolean("land.enabled", true)) {
            player.sendMessage(plugin.prefix() + "§cTính năng thuê địa bàn đang bị tắt trong tongmon-config.yml."); return false;
        }
        TongMon tm = tongMonManager.getTongMon(player.getUniqueId());
        if (tm == null || !tm.canManage(player.getUniqueId())) { player.sendMessage(plugin.prefix() + "§cKhông đủ quyền gia hạn."); return false; }
        Land land = lands.get(tm.getId());
        if (land == null) { player.sendMessage(plugin.prefix() + "§cTông Môn chưa có địa bàn để gia hạn."); return false; }
        if (land.expiresAt <= System.currentTimeMillis()) {
            if (!removeNativeProtection(land)) { player.sendMessage(plugin.prefix() + "§cChưa gỡ được claim native đã hết hạn; xem console và thử lại sau."); return false; }
            lands.remove(tm.getId()); save();
            player.sendMessage(plugin.prefix() + "§cĐịa bàn đã hết hạn. Hãy thuê lại bằng /tongmon dat thue.");
            return false;
        }
        long cost = Math.max(0L, settings.getLong("land.renew-cost", settings.getLong("land.rent-cost", 50000L)));
        int days = Math.max(1, Math.min(3650, settings.getInt("land.duration-days", 30)));
        if (tm.getTienKho() < cost || (cost > 0 && !tm.withdrawKho(cost))) { player.sendMessage(plugin.prefix() + "§cKho Tông Môn không đủ Linh Thạch."); return false; }
        land.expiresAt = Math.max(System.currentTimeMillis(), land.expiresAt) + days * 86_400_000L;
        tongMonManager.save(); save();
        player.sendMessage(plugin.prefix() + "§aĐã gia hạn địa bàn thêm §e" + days + " ngày§a."); return true;
    }

    public void showInfo(Player player) {
        TongMon tm = tongMonManager.getTongMon(player.getUniqueId());
        if (tm == null) { player.sendMessage(plugin.prefix() + "§cBạn chưa có Tông Môn."); return; }
        Land land = lands.get(tm.getId());
        if (land == null) { player.sendMessage(plugin.prefix() + "§7Tông Môn chưa thuê địa bàn. Dùng /tongmon dat pos1, pos2 rồi /tongmon dat thue."); return; }
        long days = Math.max(0, (land.expiresAt - System.currentTimeMillis() + 86_399_999L) / 86_400_000L);
        player.sendMessage("§6§l✦ ĐỊA BÀN TÔNG MÔN");
        player.sendMessage("§7Thế giới: §e" + land.world + " §7| Còn: §e" + days + " ngày");
        player.sendMessage("§7Góc: §f(" + land.minX + "," + land.minY + "," + land.minZ + ") → (" + land.maxX + "," + land.maxY + "," + land.maxZ + ")");
        player.sendMessage("§7Kích thước: §e" + land.width() + " × " + land.depth() + "§7 blocks");
    }

    public Land getLandAt(Location location) {
        if (!settings.getBoolean("land.enabled", true) || location == null || location.getWorld() == null) return null;
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, Land>> it = lands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Land> e = it.next(); Land land = e.getValue();
            if (land.expiresAt <= now) {
                if (removeNativeProtection(land)) { it.remove(); changed = true; }
                continue;
            }
            if (land.contains(location)) {
                if (changed) save();
                return land;
            }
        }
        if (changed) save();
        return null;
    }

    /** Called after sect membership changes so native region/claim permissions remain in sync. */
    public void syncExternalMembers(TongMon tm) {
        if (tm == null) return;
        Land land = lands.get(tm.getId());
        if (land == null || land.expiresAt <= System.currentTimeMillis()) return;
        if ("WORLDGUARD".equals(land.protectionProvider)) syncWorldGuardMembers(land, tm);
        else if ("GRIEFPREVENTION".equals(land.protectionProvider)) syncGriefPreventionMembers(land, tm);
        save();
    }

    /** Releases TuTien metadata and the corresponding native region/claim (used on disband/expiry). */
    public boolean releaseLand(String tongMonId) {
        if (tongMonId == null) return true;
        Land land = lands.get(tongMonId);
        if (land == null) return true;
        if (!removeNativeProtection(land)) return false;
        lands.remove(tongMonId);
        save();
        return true;
    }

    /**
     * Refuses a rental if any enabled external protection already covers the requested cuboid.
     * The checks are deliberately fail-closed: when a configured provider cannot be inspected,
     * we do not risk creating an overlapping claim/region.
     */
    private boolean hasExternalClaim(Land candidate, World world) {
        if (settings.getBoolean("land.respect-worldguard", true)
                && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                Object manager = worldGuardRegionManager(candidate);
                if (manager == null) {
                    plugin.getLogger().warning("Không đọc được WorldGuard RegionManager; từ chối thuê để tránh chồng lấn.");
                    return true;
                }
                Object rawRegions = manager.getClass().getMethod("getRegions").invoke(manager);
                if (!(rawRegions instanceof Map<?, ?> regions)) {
                    plugin.getLogger().warning("WorldGuard trả về danh sách region không hợp lệ; từ chối thuê để an toàn.");
                    return true;
                }
                for (Map.Entry<?, ?> entry : regions.entrySet()) {
                    // WorldGuard always exposes __global__, which covers the entire world and is
                    // not a player-created claim. Treating it as a conflict would block all rentals.
                    String regionId = String.valueOf(entry.getKey());
                    // Skip the global region and our own previous region (legacy/migration recovery).
                    if ("__global__".equalsIgnoreCase(regionId)
                            || regionId.equalsIgnoreCase(worldGuardId(candidate))
                            || (!candidate.worldGuardRegionId.isBlank() && regionId.equalsIgnoreCase(candidate.worldGuardRegionId))) continue;
                    Object region = entry.getValue();
                    if (region == null) continue;
                    Object min = region.getClass().getMethod("getMinimumPoint").invoke(region);
                    Object max = region.getClass().getMethod("getMaximumPoint").invoke(region);
                    int rMinX = ((Number) min.getClass().getMethod("getBlockX").invoke(min)).intValue();
                    int rMinY = ((Number) min.getClass().getMethod("getBlockY").invoke(min)).intValue();
                    int rMinZ = ((Number) min.getClass().getMethod("getBlockZ").invoke(min)).intValue();
                    int rMaxX = ((Number) max.getClass().getMethod("getBlockX").invoke(max)).intValue();
                    int rMaxY = ((Number) max.getClass().getMethod("getBlockY").invoke(max)).intValue();
                    int rMaxZ = ((Number) max.getClass().getMethod("getBlockZ").invoke(max)).intValue();
                    boolean intersects = candidate.minX <= rMaxX && candidate.maxX >= rMinX
                            && candidate.minY <= rMaxY && candidate.maxY >= rMinY
                            && candidate.minZ <= rMaxZ && candidate.maxZ >= rMinZ;
                    if (intersects) return true;
                }
            } catch (Throwable ex) {
                plugin.getLogger().warning("Không kiểm tra được chồng lấn WorldGuard: " + ex.getClass().getSimpleName()
                        + "; từ chối thuê để an toàn.");
                return true;
            }
        }

        if (settings.getBoolean("land.respect-griefprevention", true)
                && Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            try {
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object gp = gpClass.getField("instance").get(null);
                Object dataStore = gpClass.getField("dataStore").get(gp);
                Method getClaimAt = Arrays.stream(dataStore.getClass().getMethods())
                        .filter(m -> m.getName().equals("getClaimAt") && m.getParameterCount() == 3
                                && m.getParameterTypes()[0].isAssignableFrom(Location.class)
                                && m.getParameterTypes()[1] == boolean.class)
                        .findFirst().orElse(null);
                if (getClaimAt == null) {
                    plugin.getLogger().warning("Không tìm thấy GriefPrevention DataStore.getClaimAt(Location, boolean, Claim); từ chối thuê để an toàn.");
                    return true;
                }
                // max-side is hard-capped to 128 above: at most 16,384 lookups, with ignoreHeight=true
                // so claims at any Y level are detected, including claims smaller than one chunk.
                int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, candidate.minY));
                for (int x = candidate.minX; x <= candidate.maxX; x++) {
                    for (int z = candidate.minZ; z <= candidate.maxZ; z++) {
                        Location probe = new Location(world, x + 0.5, y, z + 0.5);
                        Object claim = getClaimAt.invoke(dataStore, probe, true, null);
                        if (claim != null) return true;
                    }
                }
            } catch (Throwable ex) {
                plugin.getLogger().warning("Không kiểm tra được chồng lấn GriefPrevention: " + ex.getClass().getSimpleName()
                        + "; từ chối thuê để an toàn.");
                return true;
            }
        }
        return false;
    }

    private void purgeExpiredLands() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, Land>> it = lands.entrySet().iterator();
        while (it.hasNext()) {
            Land land = it.next().getValue();
            if (land.expiresAt <= now && removeNativeProtection(land)) { it.remove(); changed = true; }
        }
        if (changed) save();
    }

    private void ensureNativeProtectionForLoadedLands() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Land>> iterator = lands.entrySet().iterator();
        while (iterator.hasNext()) {
            Land land = iterator.next().getValue();
            if (land.expiresAt <= now) continue;
            TongMon tm = tongMonManager.getTongMonById(land.tongMonId);
            if (tm == null) {
                // A sect was removed while its native claim survived. Release it or retain its metadata for retry.
                if (removeNativeProtection(land)) { iterator.remove(); changed = true; }
                continue;
            }
            // Older saves used only the TuTien listener. Migrate them once to the configured native provider.
            if (land.protectionProvider == null || land.protectionProvider.isBlank()) {
                World landWorld = Bukkit.getWorld(land.world);
                if (landWorld == null || hasExternalClaim(land, landWorld)) {
                    plugin.getLogger().warning("Không migrate native claim cho Tông Môn " + tm.getName()
                            + ": khu đất đang chồng lấn claim/region khác hoặc provider chưa sẵn sàng. TuTien protection vẫn hoạt động.");
                    continue;
                }
                if (createNativeProtection(land, tm, null)) changed = true;
                else plugin.getLogger().warning("Không thể migrate native claim cho Tông Môn " + tm.getName() + "; TuTien protection vẫn hoạt động.");
            } else {
                if ("WORLDGUARD".equals(land.protectionProvider)) syncWorldGuardMembers(land, tm);
                if ("GRIEFPREVENTION".equals(land.protectionProvider)) syncGriefPreventionMembers(land, tm);
            }
        }
        if (changed) save();
    }

    private boolean createNativeProtection(Land land, TongMon tm, Player actor) {
        String configured = settings.getString("land.claim-provider", "AUTO").trim().toUpperCase(Locale.ROOT);
        boolean hasWg = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        boolean hasGp = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        String provider = configured.equals("AUTO") ? (hasWg ? "WORLDGUARD" : hasGp ? "GRIEFPREVENTION" : "TUTIEN") : configured;
        if (provider.equals("TUTIEN") || provider.equals("NONE")) { land.protectionProvider = "TUTIEN"; return true; }
        if (provider.equals("WORLDGUARD")) {
            if (!hasWg) { plugin.getLogger().warning("land.claim-provider=WorldGuard nhưng plugin WorldGuard chưa được nạp."); return false; }
            if (!createWorldGuardRegion(land, tm)) return false;
            land.protectionProvider = "WORLDGUARD";
            syncWorldGuardMembers(land, tm);
            return true;
        }
        if (provider.equals("GRIEFPREVENTION") || provider.equals("GRIEFPrevention".toUpperCase(Locale.ROOT))) {
            if (!hasGp) { plugin.getLogger().warning("land.claim-provider=GriefPrevention nhưng plugin GriefPrevention chưa được nạp."); return false; }
            if (!createGriefPreventionClaim(land, tm, actor)) return false;
            land.protectionProvider = "GRIEFPREVENTION";
            if (!syncGriefPreventionMembers(land, tm)) {
                if (!removeNativeProtection(land)) {
                    // Keep an expired cleanup record so the scheduled reaper can retry instead of orphaning a native claim.
                    land.expiresAt = System.currentTimeMillis();
                    lands.put(land.tongMonId, land); save();
                }
                return false;
            }
            return true;
        }
        plugin.getLogger().warning("land.claim-provider không hợp lệ: " + configured + ". Dùng AUTO, WORLDGUARD, GRIEFPREVENTION hoặc TUTIEN.");
        return false;
    }

    private String worldGuardId(Land land) { return "tutien_tm_" + land.tongMonId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_+-]", "_"); }

    private boolean createWorldGuardRegion(Land land, TongMon tm) {
        try {
            Object manager = worldGuardRegionManager(land);
            if (manager == null) return false;
            land.worldGuardRegionId = worldGuardId(land);
            Object existing = manager.getClass().getMethod("getRegion", String.class).invoke(manager, land.worldGuardRegionId);
            if (existing == null) {
                Class<?> vec = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Method at = vec.getMethod("at", int.class, int.class, int.class);
                Object min = at.invoke(null, land.minX, land.minY, land.minZ);
                Object max = at.invoke(null, land.maxX, land.maxY, land.maxZ);
                Class<?> regionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion");
                Object region = regionClass.getConstructor(String.class, vec, vec).newInstance(land.worldGuardRegionId, min, max);
                // Add members before publishing the region; a new region without members could lock the whole sect out.
                if (!addWorldGuardMembers(region, tm)) return false;
                manager.getClass().getMethod("addRegion", Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion")).invoke(manager, region);
            } else if (!addWorldGuardMembers(existing, tm)) return false;
            land.externalMemberUuids.clear();
            land.externalMemberUuids.addAll(tm.getMembers().keySet());
            saveWorldGuardManager(manager);
            return true;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Tạo WorldGuard region thất bại: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return false;
        }
    }

    private Object worldGuardRegionManager(Land land) throws Exception {
        Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Object wg = wgClass.getMethod("getInstance").invoke(null);
        Object platform = wgClass.getMethod("getPlatform").invoke(wg);
        Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
        Class<?> adapter = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
        World world = Bukkit.getWorld(land.world);
        if (world == null) return null;
        Object adapted = adapter.getMethod("adapt", World.class).invoke(null, world);
        Class<?> wgWorld = Class.forName("com.sk89q.worldedit.world.World");
        return container.getClass().getMethod("get", wgWorld).invoke(container, adapted);
    }

    private boolean addWorldGuardMembers(Object region, TongMon tm) {
        try {
            Object domain = region.getClass().getMethod("getMembers").invoke(region);
            Method addPlayer = Arrays.stream(domain.getClass().getMethods()).filter(m -> m.getName().equals("addPlayer") && m.getParameterCount() == 1).findFirst().orElse(null);
            if (addPlayer == null) return false;
            Class<?> argType = addPlayer.getParameterTypes()[0];
            for (UUID uuid : tm.getMembers().keySet()) {
                Object arg = argType == UUID.class ? uuid : uuid.toString();
                addPlayer.invoke(domain, arg);
            }
            return true;
        } catch (Throwable ex) { plugin.getLogger().warning("Không thể thêm thành viên vào WorldGuard region: " + ex.getClass().getSimpleName()); return false; }
    }

    private void syncWorldGuardMembers(Land land, TongMon tm) {
        if (land.worldGuardRegionId == null || land.worldGuardRegionId.isBlank()) return;
        try {
            Object manager = worldGuardRegionManager(land);
            if (manager == null) return;
            Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, land.worldGuardRegionId);
            if (region == null) { createWorldGuardRegion(land, tm); return; }
            Object domain = region.getClass().getMethod("getMembers").invoke(region);
            Method removePlayer = Arrays.stream(domain.getClass().getMethods()).filter(m -> m.getName().equals("removePlayer") && m.getParameterCount() == 1).findFirst().orElse(null);
            Method addPlayer = Arrays.stream(domain.getClass().getMethods()).filter(m -> m.getName().equals("addPlayer") && m.getParameterCount() == 1).findFirst().orElse(null);
            if (removePlayer != null) for (UUID uuid : land.externalMemberUuids) removePlayer.invoke(domain, removePlayer.getParameterTypes()[0] == UUID.class ? uuid : uuid.toString());
            if (addPlayer == null) return;
            Class<?> argType = addPlayer.getParameterTypes()[0];
            for (UUID uuid : tm.getMembers().keySet()) addPlayer.invoke(domain, argType == UUID.class ? uuid : uuid.toString());
            land.externalMemberUuids.clear(); land.externalMemberUuids.addAll(tm.getMembers().keySet());
            saveWorldGuardManager(manager);
        } catch (Throwable ex) { plugin.getLogger().warning("Đồng bộ thành viên WorldGuard thất bại: " + ex.getClass().getSimpleName()); }
    }

    private void saveWorldGuardManager(Object manager) {
        try { manager.getClass().getMethod("save").invoke(manager); }
        catch (Throwable ex) { plugin.getLogger().warning("Không lưu được WorldGuard region: " + ex.getClass().getSimpleName()); }
    }

    private boolean createGriefPreventionClaim(Land land, TongMon tm, Player actor) {
        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gp = gpClass.getField("instance").get(null);
            Object dataStore = gpClass.getField("dataStore").get(gp);
            World world = Bukkit.getWorld(land.world); if (world == null) return false;
            Method create = Arrays.stream(dataStore.getClass().getMethods()).filter(m -> m.getName().equals("createClaim") && m.getParameterCount() == 11
                    && m.getParameterTypes()[0].isAssignableFrom(World.class)).findFirst().orElse(null);
            if (create == null) { plugin.getLogger().warning("Không tìm thấy API DataStore.createClaim 11 tham số trong GriefPrevention phiên bản đang cài."); return false; }
            // A null owner creates a GriefPrevention admin claim; members may not be able to build
            // even when trusted. Bind the claim to the sect leader and separately trust all members.
            Object result = create.invoke(dataStore, world, land.minX, land.maxX, land.minY, land.maxY,
                    land.minZ, land.maxZ, tm.getOwner(), null, null, actor);
            Object succeeded = readMember(result, "succeeded", "isSucceeded");
            Object claim = readMember(result, "claim", "getClaim");
            if (!(succeeded instanceof Boolean ok) || !ok || claim == null) {
                plugin.getLogger().warning("GriefPrevention từ chối tạo claim cho địa bàn Tông Môn."); return false;
            }
            Object id = claim.getClass().getMethod("getID").invoke(claim);
            land.griefPreventionClaimId = ((Number) id).longValue();
            return true;
        } catch (Throwable ex) { plugin.getLogger().warning("Tạo GriefPrevention claim thất bại: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); return false; }
    }

    private Object readMember(Object object, String fieldName, String getter) {
        if (object == null) return null;
        try { return object.getClass().getField(fieldName).get(object); }
        catch (Throwable ignored) { try { return object.getClass().getMethod(getter).invoke(object); } catch (Throwable ignoredAgain) { return null; } }
    }

    private Object griefPreventionClaim(Land land) throws Exception {
        if (land.griefPreventionClaimId < 0) return null;
        Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
        Object gp = gpClass.getField("instance").get(null);
        Object dataStore = gpClass.getField("dataStore").get(gp);
        Method getClaim = Arrays.stream(dataStore.getClass().getMethods()).filter(m -> m.getName().equals("getClaim") && m.getParameterCount() == 1 && (m.getParameterTypes()[0] == long.class || m.getParameterTypes()[0] == Long.class)).findFirst().orElse(null);
        return getClaim == null ? null : getClaim.invoke(dataStore, land.griefPreventionClaimId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean syncGriefPreventionMembers(Land land, TongMon tm) {
        try {
            Object claim = griefPreventionClaim(land); if (claim == null) return false;
            Method setPermission = Arrays.stream(claim.getClass().getMethods()).filter(m -> m.getName().equals("setPermission") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1].isEnum()).findFirst().orElse(null);
            if (setPermission == null) { plugin.getLogger().warning("Không tìm thấy Claim.setPermission trong GriefPrevention API."); return false; }
            Class<? extends Enum> permissionType = (Class<? extends Enum>) setPermission.getParameterTypes()[1];
            // Sect members need ordinary build trust, not Manage (which controls claim permissions).
            // GriefPrevention's Build permission grants the expected /Trust-equivalent access.
            Enum build = Arrays.stream(permissionType.getEnumConstants()).filter(e -> ((Enum)e).name().equalsIgnoreCase("Build")).map(e -> (Enum)e).findFirst().orElse(null);
            if (build == null) { plugin.getLogger().warning("GriefPrevention không có ClaimPermission.Build."); return false; }
            for (String oldName : land.externalMemberNames) setPermission.invoke(claim, oldName, null);
            List<String> names = new ArrayList<>();
            for (UUID uuid : tm.getMembers().keySet()) {
                String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                if (playerName == null || playerName.isBlank()) continue;
                setPermission.invoke(claim, playerName, build); names.add(playerName);
            }
            if (names.isEmpty()) { plugin.getLogger().warning("Không tìm được tên member để trust trong GriefPrevention."); return false; }
            land.externalMemberNames.clear(); land.externalMemberNames.addAll(names);
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gp = gpClass.getField("instance").get(null); Object ds = gpClass.getField("dataStore").get(gp);
            Method saveClaim = Arrays.stream(ds.getClass().getMethods()).filter(m -> m.getName().equals("saveClaim") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(claim.getClass())).findFirst().orElse(null);
            if (saveClaim == null) { plugin.getLogger().warning("Không tìm thấy DataStore.saveClaim trong GriefPrevention."); return false; }
            saveClaim.invoke(ds, claim);
            return true;
        } catch (Throwable ex) { plugin.getLogger().warning("Đồng bộ thành viên GriefPrevention thất bại: " + ex.getClass().getSimpleName()); return false; }
    }

    private boolean removeNativeProtection(Land land) {
        if (land == null) return true;
        String provider = land.protectionProvider == null ? "" : land.protectionProvider;
        try {
            if ("WORLDGUARD".equals(provider) && land.worldGuardRegionId != null && !land.worldGuardRegionId.isBlank()) {
                if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return false;
                Object manager = worldGuardRegionManager(land);
                if (manager == null) return false;
                Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, land.worldGuardRegionId);
                if (region != null) {
                    Method remove = Arrays.stream(manager.getClass().getMethods()).filter(m -> m.getName().equals("removeRegion") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class).findFirst().orElse(null);
                    if (remove != null) remove.invoke(manager, land.worldGuardRegionId);
                    else {
                        Method removeWithStrategy = Arrays.stream(manager.getClass().getMethods()).filter(m -> m.getName().equals("removeRegion") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1].isEnum()).findFirst().orElse(null);
                        if (removeWithStrategy == null) return false;
                        Object[] strategyValues = (Object[]) removeWithStrategy.getParameterTypes()[1].getEnumConstants();
                        Object strategy = Arrays.stream(strategyValues).filter(v -> ((Enum<?>) v).name().equals("UNSET_PARENT_IN_CHILDREN")).findFirst().orElse(strategyValues[0]);
                        removeWithStrategy.invoke(manager, land.worldGuardRegionId, strategy);
                    }
                    saveWorldGuardManager(manager);
                }
            } else if ("GRIEFPREVENTION".equals(provider) && land.griefPreventionClaimId >= 0) {
                if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) return false;
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object gp = gpClass.getField("instance").get(null); Object ds = gpClass.getField("dataStore").get(gp);
                Object claim = griefPreventionClaim(land);
                if (claim != null) {
                    Method delete = Arrays.stream(ds.getClass().getMethods()).filter(m -> m.getName().equals("deleteClaim") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(claim.getClass())).findFirst().orElse(null);
                    if (delete == null) return false;
                    delete.invoke(ds, claim);
                }
            }
            land.protectionProvider = ""; land.worldGuardRegionId = ""; land.griefPreventionClaimId = -1L;
            land.externalMemberUuids.clear(); land.externalMemberNames.clear();
            return true;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Không giải phóng native protection địa bàn " + land.tongMonId + ": " + ex.getClass().getSimpleName());
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Protection listeners. These are intentionally kept even when a native
    // WorldGuard/GriefPrevention claim exists: they enforce alliance entry and
    // prevent non-sect members from using storage or modifying rented land.
    // ---------------------------------------------------------------------
    private boolean isSectMember(Player player, Land land) {
        if (player == null || land == null) return false;
        TongMon memberOf = tongMonManager.getTongMon(player.getUniqueId());
        return memberOf != null && land.tongMonId.equals(memberOf.getId());
    }

    private boolean canEnterLand(Player player, Land land) {
        if (isSectMember(player, land)) return true;
        if (player == null || land == null) return false;
        TongMon visitorSect = tongMonManager.getTongMon(player.getUniqueId());
        if (visitorSect == null || visitorSect.getId().equals(land.tongMonId)) return false;
        TongMonAllianceManager diplomacy = plugin.getTongMonAllianceManager();
        return diplomacy != null && diplomacy.hasMutualLandAccess(land.tongMonId, visitorSect.getId());
    }

    private void tellProtectionDenied(Player player, String action) {
        long now = System.currentTimeMillis();
        Long previous = lastProtectionMessage.get(player.getUniqueId());
        if (previous != null && now - previous < 900L) return;
        lastProtectionMessage.put(player.getUniqueId(), now);
        player.sendMessage(plugin.prefix() + "§cĐịa bàn Tông Môn được bảo vệ. Bạn không có quyền " + action + " tại đây.");
    }

    private boolean isProtected(Block block) {
        return block != null && getLandAt(block.getLocation()) != null;
    }

    private boolean pistonTouchesProtected(Block piston, List<Block> moved, org.bukkit.block.BlockFace direction) {
        if (isProtected(piston)) return true;
        for (Block movedBlock : moved) {
            if (isProtected(movedBlock) || isProtected(movedBlock.getRelative(direction))) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandBlockBreak(BlockBreakEvent event) {
        Land land = getLandAt(event.getBlock().getLocation());
        if (land == null || isSectMember(event.getPlayer(), land)) return;
        event.setCancelled(true);
        tellProtectionDenied(event.getPlayer(), "phá block");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandBlockPlace(BlockPlaceEvent event) {
        Land land = getLandAt(event.getBlockPlaced().getLocation());
        if (land == null || isSectMember(event.getPlayer(), land)) return;
        event.setCancelled(true);
        tellProtectionDenied(event.getPlayer(), "đặt block");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Land land = getLandAt(clicked.getLocation());
        if (land == null || isSectMember(event.getPlayer(), land)) return;

        // Allied visitors may pass through ordinary doors/gates only when both
        // sects consent to territory access. Storage and redstone remain private.
        boolean alliedVisitor = canEnterLand(event.getPlayer(), land);
        if (clicked.getState() instanceof Container || !alliedVisitor || !(clicked.getBlockData() instanceof Openable)) {
            event.setCancelled(true);
            tellProtectionDenied(event.getPlayer(), "tương tác với vật thể");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (from.getWorld().equals(to.getWorld()) && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Land destination = getLandAt(to);
        if (destination == null) {
            warnedMove.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (canEnterLand(event.getPlayer(), destination)) {
            warnedMove.remove(event.getPlayer().getUniqueId());
            return;
        }

        Land origin = getLandAt(from);
        // Deny entering new protected land. If an alliance was revoked while a
        // visitor is inside, let them move only toward the nearest boundary so
        // they can leave rather than being trapped permanently.
        if (origin == null || !origin.tongMonId.equals(destination.tongMonId)) {
            event.setCancelled(true);
            if (warnedMove.add(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(plugin.prefix() + "§cBạn không có quyền vào địa bàn Tông Môn này.");
            }
        } else {
            // A visitor whose alliance access was revoked while already inside
            // can still walk out. Block/interact listeners deny any protected
            // actions during exit; do not trap the player inside the region.
            if (warnedMove.add(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(plugin.prefix() + "§eQuyền vào địa bàn đã thay đổi. Hãy rời khỏi khu vực; thao tác xây dựng vẫn bị khóa.");
            }
        }
    }

    private double edgeDistance(Location location, Land land) {
        return Math.min(Math.min(location.getBlockX() - land.minX, land.maxX - location.getBlockX()),
                Math.min(Math.min(location.getBlockZ() - land.minZ, land.maxZ - location.getBlockZ()),
                        Math.min(location.getBlockY() - land.minY, land.maxY - location.getBlockY())));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandBucketEmpty(PlayerBucketEmptyEvent event) {
        Land land = getLandAt(event.getBlock().getLocation());
        if (land == null || isSectMember(event.getPlayer(), land)) return;
        event.setCancelled(true);
        tellProtectionDenied(event.getPlayer(), "đổ chất lỏng");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandBucketFill(PlayerBucketFillEvent event) {
        Block clicked = event.getBlockClicked();
        Land land = clicked == null ? null : getLandAt(clicked.getLocation());
        if (land == null || isSectMember(event.getPlayer(), land)) return;
        event.setCancelled(true);
        tellProtectionDenied(event.getPlayer(), "lấy chất lỏng");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandTrample(EntityInteractEvent event) {
        if (event.getBlock() == null || event.getBlock().getType() != Material.FARMLAND) return;
        if (getLandAt(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandFluidFlow(BlockFromToEvent event) {
        Land source = getLandAt(event.getBlock().getLocation());
        Land destination = getLandAt(event.getToBlock().getLocation());
        if (source == null && destination == null) return;
        if (source != null && destination != null && source.tongMonId.equals(destination.tongMonId)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandFire(BlockBurnEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandIgnite(BlockIgniteEvent event) {
        Land land = getLandAt(event.getBlock().getLocation());
        if (land == null) return;
        Player igniter = event.getPlayer();
        if (igniter == null || !isSectMember(igniter, land)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandPistonExtend(BlockPistonExtendEvent event) {
        if (pistonTouchesProtected(event.getBlock(), event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLandPistonRetract(BlockPistonRetractEvent event) {
        if (pistonTouchesProtected(event.getBlock(), event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("lands"); if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id); if (s == null) continue;
            String world = s.getString("world", "");
            if (world.isBlank()) continue;
            Land land = new Land(id, world, s.getInt("min-x"), s.getInt("min-y"), s.getInt("min-z"),
                    s.getInt("max-x"), s.getInt("max-y"), s.getInt("max-z"), s.getLong("expires-at", 0L));
            land.protectionProvider = s.getString("protection-provider", "");
            land.worldGuardRegionId = s.getString("worldguard-region-id", "");
            land.griefPreventionClaimId = s.getLong("griefprevention-claim-id", -1L);
            for (String uuid : s.getStringList("external-member-uuids")) try { land.externalMemberUuids.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) { }
            land.externalMemberNames.addAll(s.getStringList("external-member-names"));
            lands.put(id, land);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Land> entry : lands.entrySet()) {
            Land l = entry.getValue(); String p = "lands." + entry.getKey();
            cfg.set(p + ".world", l.world); cfg.set(p + ".min-x", l.minX); cfg.set(p + ".min-y", l.minY); cfg.set(p + ".min-z", l.minZ);
            cfg.set(p + ".max-x", l.maxX); cfg.set(p + ".max-y", l.maxY); cfg.set(p + ".max-z", l.maxZ); cfg.set(p + ".expires-at", l.expiresAt);
            cfg.set(p + ".protection-provider", l.protectionProvider);
            cfg.set(p + ".worldguard-region-id", l.worldGuardRegionId);
            cfg.set(p + ".griefprevention-claim-id", l.griefPreventionClaimId);
            cfg.set(p + ".external-member-uuids", l.externalMemberUuids.stream().map(UUID::toString).toList());
            cfg.set(p + ".external-member-names", new ArrayList<>(l.externalMemberNames));
        }
        try { cfg.save(file); } catch (IOException e) { plugin.getLogger().warning("Không thể lưu địa bàn: " + e.getMessage()); }
    }

    private static final class Selection { private Point first, second; }
    private record Point(String world, int x, int y, int z) { }

    public static final class Land {
        public final String tongMonId, world;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public long expiresAt;
        private String protectionProvider = "";
        private String worldGuardRegionId = "";
        private long griefPreventionClaimId = -1L;
        private final Set<UUID> externalMemberUuids = new LinkedHashSet<>();
        private final Set<String> externalMemberNames = new LinkedHashSet<>();
        private Land(String id, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long expiresAt) {
            this.tongMonId = id; this.world = world; this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ; this.expiresAt = expiresAt;
        }
        public boolean contains(Location l) { return l != null && l.getWorld() != null && world.equals(l.getWorld().getName())
                && l.getBlockX() >= minX && l.getBlockX() <= maxX && l.getBlockY() >= minY && l.getBlockY() <= maxY
                && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ; }
        private boolean intersects(Land l) { return world.equals(l.world) && minX <= l.maxX && maxX >= l.minX && minY <= l.maxY && maxY >= l.minY && minZ <= l.maxZ && maxZ >= l.minZ; }
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
    }
}