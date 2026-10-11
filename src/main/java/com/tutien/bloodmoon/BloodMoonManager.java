package com.tutien.bloodmoon;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.util.*;

public class BloodMoonManager implements Listener {

    private final TuTienPlugin plugin;

    // ── Trạng thái ──────────────────────────────────
    private boolean active = false;
    private double huyetTinh = 0;
    private static final double MAX_HUYETTINH = 1000.0;
    private boolean superBossSpawned = false;
    private LivingEntity activeBoss = null;
    private boolean bossRevived = false;

    // ── Khung giờ cố định Real-time (Cấu hình) ───────
    private static final int START_HOUR = 20;   // 20 giờ (8:00 PM)
    private static final int START_MINUTE = 0;  // 00 phút
    private static final int END_HOUR = 20;     // 20 giờ
    private static final int END_MINUTE = 30;   // 30 phút (Event kéo dài 30 phút)

    // ── BossBar (Adventure API) ──────────────────────
    private BossBar bossBar = null;
    private final Set<UUID> bossBarViewers = new HashSet<>();

    // ── NBT Keys ─────────────────────────────────────
    private static final String KEY_BINH      = "huyet_binh";
    private static final String KEY_BINH_FULL = "huyet_binh_full";
    private static final String KEY_BOSS      = "huyet_boss";
    private static final String KEY_PORTAL    = "huyet_portal";

    // ── Thống kê event này ────────────────────────────
    private final Map<UUID, Integer> killContrib = new HashMap<>();

    public BloodMoonManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startNightCheck();
    }

    // ========================
    //  KIỂM TRA GIỜ CỐ ĐỊNH (REAL-TIME)
    // ========================

    private void startNightCheck() {
        new BukkitRunnable() {
            @Override public void run() {
                LocalTime now = LocalTime.now();
                int hour = now.getHour();
                int minute = now.getMinute();

                // Kiểm tra xem hiện tại có nằm trong khoảng giờ Event hay không
                boolean isEventTime = isWithinEventTime(hour, minute);

                if (isEventTime && !active) {
                    // Đến giờ -> Bật Event
                    startBloodMoon();
                } else if (!isEventTime && active) {
                    // Hết giờ -> Tắt Event
                    stopBloodMoon();
                }
            }
        }.runTaskTimer(plugin, 120L, 120L); // Kiểm tra mỗi 6 giây (120 ticks)
    }

    private boolean isWithinEventTime(int hour, int minute) {
        int nowMinutes = hour * 60 + minute;
        int startMinutes = START_HOUR * 60 + START_MINUTE;
        int endMinutes = END_HOUR * 60 + END_MINUTE;

        if (startMinutes <= endMinutes) {
            return nowMinutes >= startMinutes && nowMinutes < endMinutes;
        } else {
            // Trường hợp event chạy xuyên đêm qua 00:00 (Ví dụ: 23:00 - 00:30)
            return nowMinutes >= startMinutes || nowMinutes < endMinutes;
        }
    }

    // ========================
    //  KÍCH HOẠT / KẾT THÚC
    // ========================

    public void startBloodMoon() {
        if (active) return;
        active = true;
        huyetTinh = 0;
        superBossSpawned = false;
        activeBoss = null;
        bossRevived = false;
        killContrib.clear();

        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                w.setTime(18000); // Ép thời gian Minecraft thành ban đêm
                w.setThundering(true);
                w.setStorm(true);
                w.setWeatherDuration(72000);
                w.setThunderDuration(72000);
            }
        }

        startLightningLoop();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("blindness")), 60, 0, false, false, false));
        }

        createBossBar();

        String announce = "§4§l☽ HUYẾT NGUYỆT ☽";
        String sub = "§cHuyết Tinh tích tụ - Dùng §eBình Huyết Nguyệt §ctrước khi SIÊU BOSS xuất hiện!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(announce, sub, 10, 80, 20);
            Location pl = p.getLocation();
            p.getWorld().strikeLightningEffect(pl.clone().add(10,0,0));
            p.getWorld().strikeLightningEffect(pl.clone().add(-10,0,0));
            p.getWorld().strikeLightningEffect(pl.clone().add(0,0,10));

            PlayerData _data = plugin.getDataManager().get(p);
            if (_data != null && _data.hasClass()
                    && _data.getClassType() == com.tutien.data.ClassType.QUY_TU) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), Integer.MAX_VALUE, 2, false, false, false));
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("night_vision")), Integer.MAX_VALUE, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), Integer.MAX_VALUE, 1, false, false, false));
                p.sendMessage(plugin.msg("auto_ad9ecbe261"));
            }
            p.sendMessage(plugin.msg("auto_da39a3ee5e"));
            p.sendMessage(plugin.msg("auto_ffddf5cff0"));
            p.sendMessage(plugin.msg("auto_8ee224552d"));
            p.sendMessage(plugin.msg("auto_1974ce5a0f"));
            p.sendMessage(plugin.msg("auto_7e7abbded2"));
            p.sendMessage(plugin.msg("auto_bed7f70d00"));
            p.sendMessage(plugin.msg("auto_fb0ddd33a0"));
            p.sendMessage(plugin.msg("auto_093ab16db3"));
            p.sendMessage(plugin.msg("auto_28a9e2c9b7"));
            p.sendMessage(plugin.msg("auto_4847a60cd7"));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.4f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
        }

        startQuyTuPassiveLoop();
        startAtmosphereLoop();
        startBloodAuraLoop();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!hasBinh(p)) {
                    p.getInventory().addItem(createBinh());
                    p.sendMessage(plugin.prefix() + "§c☽ Bạn nhận được §eBình Huyết Nguyệt§c! Cầm tay + Chuột Phải để thu thập.");
                }
            }
        }, 60L);
    }

    public void stopBloodMoon() {
        if (!active) return;
        active = false;
        huyetTinh = 0;

        removeBossBar();

        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                w.setStorm(false);
                w.setThundering(false);
            }
        }

        if (activeBoss != null && !activeBoss.isDead()) activeBoss.remove();
        activeBoss = null;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.prefix() + "§7☽ Huyết Nguyệt kết thúc.");
            PlayerData _sd = plugin.getDataManager().get(p);
            if (_sd != null && _sd.hasClass() && _sd.getClassType() == com.tutien.data.ClassType.QUY_TU) {
                p.removePotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")));
                p.removePotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("night_vision")));
                p.removePotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")));
            }
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.6f);
        }
    }

    public boolean isActive() { return active; }

    // ========================
    //  TÍCH LŨY HUYẾT TINH
    // ========================

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (!active) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity le)) return;
        if (entity instanceof Player) return;

        // Kiểm tra boss
        if (isOurBoss(entity)) return;

        Player killer = event.getEntity().getKiller();

        // Lượng HT tích lũy theo loại mob
        double ht = switch (entity.getType()) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER -> 1.0;
            case ENDERMAN, BLAZE, GHAST -> 3.0;
            case RAVAGER, ELDER_GUARDIAN -> 8.0;
            case ENDER_DRAGON, WITHER -> 50.0;
            case WARDEN -> 30.0;
            default -> 1.0;
        };

        // Đêm huyết nguyệt: mob có potionEffect đặc biệt → bonus
        if (!le.getActivePotionEffects().isEmpty()) ht *= 1.5;

        // Quỷ Tú bonus: thu thêm 2x HT khi Huyết Nguyệt
        if (killer != null) {
            PlayerData _kd = plugin.getDataManager().get(killer);
            if (_kd != null && _kd.hasClass()
                    && _kd.getClassType() == com.tutien.data.ClassType.QUY_TU) {
                ht *= 2.0; // Quỷ Tú thu gấp đôi Huyết Tinh
            }
        }
        addHuyetTinh(ht, killer);

        // Tracker kill
        if (killer != null) killContrib.merge(killer.getUniqueId(), 1, Integer::sum);

        // Nhỏ % LK bonus khi giết mob trong huyết nguyệt
        if (killer != null) {
            PlayerData data = plugin.getDataManager().get(killer);
            if (data != null) {
                data.addLinhKhi((long)(data.getMaxLinhKhi() * 0.0005));
            }
        }
    }

    private void addHuyetTinh(double amount, Player source) {
        huyetTinh = Math.min(MAX_HUYETTINH, huyetTinh + amount);
        updateBossBar();

        // Cảnh báo ngưỡng
        double pct = huyetTinh / MAX_HUYETTINH;
        if (pct >= 1.0 && !superBossSpawned) {
            spawnSuperBoss();
        } else if (pct >= 0.9 && (int)((huyetTinh - amount) / MAX_HUYETTINH * 10) < 9) {
            broadcastWarning("§4⚠ HUYẾT TINH 90%! §cSIÊU BOSS sắp xuất hiện! Dùng Bình ngay!");
        } else if (pct >= 0.75 && (int)((huyetTinh - amount) / MAX_HUYETTINH * 10) < 7) {
            broadcastWarning("§c⚠ Huyết Tinh §e" + (int)(pct*100) + "%§c! Cần tiêu thụ!");
        }
    }

    // ========================
    //  BÌNH HUYẾT NGUYỆT
    // ========================

    public ItemStack createBinh() {
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.msg("auto_e9b7ab8483"));
        meta.setLore(Arrays.asList(
                "§7Cầm tay + §eChuột Phải §7khi Huyết Nguyệt",
                "§7để thu thập §c5% §7năng lượng Huyết Tinh.",
                "§8───────────────────",
                "§7Bình đầy có thể dùng để:",
                "  §e→ §bMở Cổng Dịch Chuyển §8(5 phút)",
                "  §e→ §aTăng tốc farm quặng ×3 §8(60s)",
                "  §e→ §dHồi phục 80% HP + Regen V §8(30s)",
                "§8───────────────────",
                "§4§l☽ Chỉ hoạt động khi Huyết Nguyệt xuất hiện"
        ));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, KEY_BINH), PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBinhFull() {
        ItemStack item = new ItemStack(Material.DRAGON_BREATH); // Đỏ rực
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.msg("auto_645e3da9a1"));
        meta.setLore(Arrays.asList(
                "§cNăng lượng Huyết Nguyệt đã được thu thập!",
                "§8───────────────────",
                "§7Chuột Phải để sử dụng:",
                "  §b① Mở Cổng Dịch Chuyển §85 phút",
                "  §a② Tăng Tốc Farm ×3 §860s bán kính 20m",
                "  §d③ Hồi Phục Đại §880% HP + Regen V 30s",
                "§8───────────────────",
                "§7Shift+Chuột Phải để chọn công dụng"
        ));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, KEY_BINH_FULL), PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBinh(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, KEY_BINH), PersistentDataType.BYTE);
    }

    public boolean isBinhFull(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, KEY_BINH_FULL), PersistentDataType.BYTE);
    }

    private boolean hasBinh(Player player) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (isBinh(it) || isBinhFull(it)) return true;
        }
        return false;
    }

    // ========================
    //  SỬ DỤNG BÌNH
    // ========================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // ── Bình Trống: Thu Thập Huyết Tinh ──
        if (isBinh(item) && !isBinhFull(item)) {
            event.setCancelled(true);
            if (!active) { player.sendMessage(plugin.prefix() + "§7Bình này chỉ hoạt động khi Huyết Nguyệt!"); return; }
            double harvest = MAX_HUYETTINH * 0.05; // 5%
            if (huyetTinh < harvest) {
                player.sendMessage(plugin.prefix() + "§cChưa đủ Huyết Tinh trong không khí! §7(Cần ≥5%)"); return;
            }
            huyetTinh -= harvest;
            updateBossBar();
            // Đổi bình trống → bình đầy
            player.getInventory().setItemInMainHand(createBinhFull());
            player.sendMessage(plugin.prefix() + "§c☽ Bình đã hút §e5% §cHuyết Tinh! §7Dùng bình đầy để kích hoạt sức mạnh.");
            player.sendTitle(plugin.msg("auto_7493a6444c"), plugin.msg("auto_9b20995205"), 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1f, 0.7f);
            spawnCollectEffect(player.getLocation());
            return;
        }

        // ── Bình Đầy: Kích Hoạt Sức Mạnh ──
        if (isBinhFull(item)) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                // Mở menu chọn
                openBinhMenu(player);
            } else {
                // Mặc định: Hồi phục
                useBinhHeal(player, item);
            }
        }
    }

    private void openBinhMenu(Player player) {
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_47f4094612"));
        player.sendMessage(plugin.msg("auto_08eb085aca"));
        player.sendMessage(plugin.msg("auto_9307443037"));
        player.sendMessage(plugin.msg("auto_f62cdc8680"));
        player.sendMessage(plugin.msg("auto_de8d184295"));
        player.sendMessage(plugin.msg("auto_6c516582cf"));
        player.sendMessage(plugin.msg("auto_1e427f21f5"));
        player.sendMessage(plugin.msg("auto_7de6fc3773"));
    }

    private void useBinhHeal(Player player, ItemStack item) {
        double healAmt = player.getMaxHealth() * 0.80;
        player.setHealth(Math.min(player.getHealth() + healAmt, player.getMaxHealth()));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 600, 4, false, true, true));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("absorption")), 400, 3, false, true, true));
        player.getInventory().setItemInMainHand(createBinh()); // bình trống lại
        player.sendMessage(plugin.prefix() + "§d✦ Huyết Nguyệt Hồi Nguyên! §a+80% HP §7+ §dRegen V 30s");
        player.sendTitle(plugin.msg("auto_05b9302dae"), plugin.msg("auto_7fe87bc9ce"), 5, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        spawnUseEffect(player.getLocation(), Color.fromRGB(220,0,80));
    }

    /** Dùng bình để kích hoạt cổng */
    public void useBinhPortal(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBinhFull(item)) { player.sendMessage(plugin.prefix() + "§cCầm Bình Huyết Nguyệt đầy trên tay!"); return; }
        player.getInventory().setItemInMainHand(createBinh());
        Location loc = player.getLocation().clone().add(0, 0.5, 0);
        spawnPortal(player, loc);
        player.sendMessage(plugin.prefix() + "§b⚡ Cổng Huyết Nguyệt mở trong §e5 phút§b!");
        player.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.0f);
    }

    /** Dùng bình để tăng tốc farm */
    public void useBinhFarm(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBinhFull(item)) { player.sendMessage(plugin.prefix() + "§cCầm Bình Huyết Nguyệt đầy trên tay!"); return; }
        player.getInventory().setItemInMainHand(createBinh());
        // Buff Haste III cho 60s + thông báo
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("haste")), 1200, 2, false, true, true));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 1200, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("luck")), 1200, 2, false, true, true));
        player.sendMessage(plugin.prefix() + "§a⚡ Tăng Tốc Farm! §eHaste III + Strength II + Luck III §7trong 60s!");
        player.sendTitle(plugin.msg("auto_2a1646e80a"), plugin.msg("auto_6896469c1d"), 5, 50, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
        spawnUseEffect(player.getLocation(), Color.fromRGB(0, 220, 80));
    }

    // ========================
    //  CỔNG DỊCH CHUYỂN
    // ========================

    private void spawnPortal(Player summoner, Location loc) {
        // Tạo cổng End Portal giả bằng particles
        World w = loc.getWorld();
        w.spawnParticle(Particle.PORTAL, loc, 100, 1.5, 2, 1.5, 0.5);
        w.spawnParticle(Particle.ENCHANTED_HIT, loc, 50, 1, 2, 1, 0.3);
        w.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 0.8f);

        // Marker vị trí cổng với particles liên tục
        new BukkitRunnable() {
            int t = 0;
            final Location portalLoc = loc.clone();
            @Override public void run() {
                if (t++ >= 6000 || !active) { cancel(); return; } // 5 phút
                if (t % 5 == 0) {
                    // Hiệu ứng cổng
                    for (double a = 0; a < 360; a += 20) {
                        double rad = Math.toRadians(a + t*3);
                        Location p = portalLoc.clone().add(Math.cos(rad)*0.8, t*0.002%2, Math.sin(rad)*0.8);
                        w.spawnParticle(Particle.DUST, p, 1, 0,0,0,0,
                                new Particle.DustOptions(Color.fromRGB(180,0,60), 1.2f));
                    }
                    w.spawnParticle(Particle.PORTAL, portalLoc.clone().add(0,1,0), 10, 0.3,0.5,0.3,0.5);
                }
                // Teleport player nếu đứng gần trong 2 block
                for (Player p : w.getNearbyPlayers(portalLoc, 2, 3, 2)) {
                    if (p.equals(summoner)) continue;
                    // Teleport đến End hoặc Nether để thăm dò
                    World dest = Bukkit.getWorld("world_the_end");
                    if (dest == null) dest = Bukkit.getWorld("world_nether");
                    if (dest != null) {
                        p.teleport(dest.getSpawnLocation());
                        p.sendMessage(plugin.prefix() + "§b⚡ Cổng Huyết Nguyệt đưa bạn đến chiều không gian khác!");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ========================
    //  SIÊU BOSS
    // ========================

    private void spawnSuperBoss() {
        if (superBossSpawned) return;
        superBossSpawned = true;

        // Tìm vị trí có nhiều người chơi nhất
        Location spawnLoc = getMostPopulatedLocation();
        if (spawnLoc == null) return;

        broadcastWarning("§4§l☽ HUYẾT TINH ĐẦY! HUYẾT CỐT MA VƯƠNG XUẤT HIỆN! ☽");

        // Hiệu ứng báo trước 3 giây
        final Location finalLoc = spawnLoc;
        new BukkitRunnable() {
            int countdown = 3;
            @Override public void run() {
                if (countdown <= 0) {
                    cancel();
                    doSpawnBoss(finalLoc);
                    return;
                }
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.sendTitle("§4" + countdown + "...", plugin.msg("auto_bd61521ff6"), 0, 25, 5);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 1.5f, 0.3f);
                });
                // Sét tại vị trí spawn
                finalLoc.getWorld().strikeLightning(finalLoc);
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void doSpawnBoss(Location loc) {
        World w = loc.getWorld();

        // Spawn Boss: Wither Skeleton đặc biệt (customizable)
        Wither boss = (Wither) w.spawnEntity(loc, EntityType.WITHER);
        boss.setCustomName("§4§l☽ HUYẾT CỐT MA VƯƠNG ☽");
        boss.setCustomNameVisible(true);

        // Tăng HP
        setMaxHealth(boss, 500.0);
        boss.setHealth(500.0);

        // Potion buffs
        boss.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), Integer.MAX_VALUE, 1, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), Integer.MAX_VALUE, 3, false, false));
        boss.setInvulnerable(false);

        // Tag NBT
        boss.getPersistentDataContainer().set(
                new NamespacedKey(plugin, KEY_BOSS), PersistentDataType.BYTE, (byte)1);
        activeBoss = boss;

        // Thông báo
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(plugin.msg("auto_46feefa366"), plugin.msg("auto_622697322a"), 10, 100, 20);
            p.sendMessage(plugin.msg("auto_47f4094612"));
            p.sendMessage(plugin.msg("auto_2634228d9e"));
            p.sendMessage("§7  Vị trí: §e" + formatLoc(loc));
            p.sendMessage(plugin.msg("auto_320a818223"));
            p.sendMessage(plugin.msg("auto_06ba57fa40"));
            p.sendMessage(plugin.msg("auto_eea10964db"));
            p.sendMessage(plugin.msg("auto_8b8b035827"));
            p.sendMessage(plugin.msg("auto_7de6fc3773"));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.3f);
        }

        // Update BossBar cho boss này
        updateBossBarForBoss(boss);

        // AoE Attack loop
        startBossAoE(boss);
    }

    private void startBossAoE(Wither boss) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (boss.isDead() || !active) { cancel(); return; }
                t++;
                Location bl = boss.getLocation();

                // Mỗi 3s: AoE Wither + Slowness
                if (t % 60 == 0) {
                    for (Entity e : bl.getWorld().getNearbyEntities(bl, 12, 8, 12)) {
                        if (!(e instanceof Player p)) continue;
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.getByKey(NamespacedKey.minecraft("wither")), 100, 1, false, true, true));
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 60, 2, false, true, true));
                        p.damage(15.0, boss);
                    }
                    bl.getWorld().spawnParticle(Particle.DUST, bl, 80, 6,3,6, 0,
                            new Particle.DustOptions(Color.fromRGB(180,0,0), 2.0f));
                    bl.getWorld().playSound(bl, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
                }

                // Particles liên tục
                if (t % 4 == 0) {
                    bl.getWorld().spawnParticle(Particle.DUST, bl.clone().add(0,2,0), 8,
                            2,0.5,2, 0, new Particle.DustOptions(Color.fromRGB(200,0,50), 1.5f));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!isOurBoss(event.getEntity())) return;

        LivingEntity boss = event.getEntity();
        Player killer = boss.getKiller();

        event.getDrops().clear(); // Xóa drop mặc định

        // Revive lần 1
        if (!bossRevived) {
            bossRevived = true;
            Location bl = boss.getLocation();

            // Tái sinh với 250 HP
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Wither revived = (Wither) bl.getWorld().spawnEntity(bl, EntityType.WITHER);
                revived.setCustomName("§4§l☽ HUYẾT CỐT MA VƯƠNG [THỨC TỈNH] ☽");
                revived.setCustomNameVisible(true);
                setMaxHealth(revived, 250.0);
                revived.setHealth(250.0);
                revived.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), Integer.MAX_VALUE, 3, false, false));
                revived.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), Integer.MAX_VALUE, 4, false, false));
                revived.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, KEY_BOSS), PersistentDataType.BYTE, (byte)1);
                activeBoss = revived;
                startBossAoE(revived);
                updateBossBarForBoss(revived);

                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.sendTitle(plugin.msg("auto_d346478875"), plugin.msg("auto_2b60046854"), 5, 60, 15);
                    p.sendMessage(plugin.prefix() + "§4§l☽ Boss tái sinh! §cHình dạng Thức Tỉnh - tốc độ và sức mạnh tăng gấp đôi!");
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
                });
            }, 40L);

            // Particles phục sinh
            bl.getWorld().spawnParticle(Particle.EXPLOSION, bl, 5, 2,1,2,0);
            bl.getWorld().spawnParticle(Particle.DUST, bl, 100, 4,4,4,0,
                    new Particle.DustOptions(Color.fromRGB(220,0,0), 3.0f));
            return;
        }

        // Đã revive rồi → chết thật
        onFinalBossDeath(killer, boss.getLocation());
    }

    private void onFinalBossDeath(Player killer, Location loc) {
        activeBoss = null;

        // Phần thưởng
        if (killer != null) {
            PlayerData data = plugin.getDataManager().get(killer);
            if (data != null) {
                data.addTien(50_000);
            }
            // Pháp Bảo Đế Phẩm
            ItemStack reward = plugin.getPhaBaoManager().randomPhaBao(9); // Đế Phẩm = ordinal 9+
            killer.getInventory().addItem(reward);
            // Bình Huyết Nguyệt đầy ×3
            for (int i = 0; i < 3; i++) killer.getInventory().addItem(createBinhFull());

            killer.sendTitle(plugin.msg("auto_ec28f23c79"), plugin.msg("auto_e0cf39db8f"), 10, 100, 20);
        }

        // MVP - ai giết nhiều nhất
        UUID mvpUUID = killContrib.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);

        // Thông báo
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.msg("auto_c3893582a7"));
            p.sendMessage(plugin.msg("auto_777eb71409"));
            if (killer != null) p.sendMessage("  §7Người giết: §e§l" + killer.getName() + " §7(+50,000 LT)");
            if (mvpUUID != null) {
                Player mvp = Bukkit.getPlayer(mvpUUID);
                if (mvp != null)
                    p.sendMessage("  §7MVP Huyết Tinh: §6§l" + mvp.getName() + " §7(" + killContrib.get(mvpUUID) + " kills)");
            }
            p.sendMessage(plugin.msg("auto_2950416a88"));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
        }

        // Kết thúc sự kiện sau 10 giây
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stopBloodMoon(), 200L);
    }

    // ========================
    //  BOSS BAR
    // ========================

    private void createBossBar() {
        bossBar = BossBar.bossBar(
                Component.text("☽ HUYẾT NGUYỆT — Huyết Tinh: 0%")
                        .color(TextColor.color(200, 0, 50)),
                0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
            bossBarViewers.add(p.getUniqueId());
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        float pct = (float)(huyetTinh / MAX_HUYETTINH);
        bossBar.progress(Math.min(1f, pct));
        int pctInt = (int)(pct * 100);
        String status = pct >= 0.9 ? " §4⚠ NGUY HIỂM!" : pct >= 0.5 ? " §6⚡ Đang tích" : " §7Thu thập";
        bossBar.name(Component.text("☽ Huyết Nguyệt — Huyết Tinh: " + pctInt + "%" + status)
                .color(TextColor.color(200, 0, 50))
                .decoration(TextDecoration.BOLD, pct >= 0.9));
        BossBar.Color color = pct >= 0.9 ? BossBar.Color.RED : pct >= 0.5 ? BossBar.Color.PINK : BossBar.Color.PURPLE;
        bossBar.color(color);
    }

    private void updateBossBarForBoss(LivingEntity boss) {
        if (bossBar == null) return;
        bossBar.name(Component.text("☽ HUYẾT CỐT MA VƯƠNG — HP: " + (int)boss.getHealth() + "/" + (int)boss.getMaxHealth())
                .color(TextColor.color(220, 0, 0))
                .decoration(TextDecoration.BOLD, true));
        bossBar.progress(Math.min(1f, (float)(boss.getHealth()/boss.getMaxHealth())));
        bossBar.color(BossBar.Color.RED);
        bossBar.overlay(BossBar.Overlay.NOTCHED_20);
    }

    private void removeBossBar() {
        if (bossBar == null) return;
        for (UUID uid : bossBarViewers) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.hideBossBar(bossBar);
        }
        bossBarViewers.clear();
        bossBar = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (active && bossBar != null) {
            event.getPlayer().showBossBar(bossBar);
            bossBarViewers.add(event.getPlayer().getUniqueId());
            // Cho bình nếu chưa có
            if (!hasBinh(event.getPlayer()))
                event.getPlayer().getInventory().addItem(createBinh());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarViewers.remove(event.getPlayer().getUniqueId());
    }

    // ========================
    //  LỆNH BÌNH (BINHXL)
    // ========================

    public void handleBinhCommand(Player player, String[] args) {
        if (args.length == 0) { openBinhMenu(player); return; }
        switch (args[0].toLowerCase()) {
            case "cong", "portal" -> useBinhPortal(player);
            case "farm"           -> useBinhFarm(player);
            case "heal"           -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!isBinhFull(item)) { player.sendMessage(plugin.prefix() + "§cCầm Bình đầy trên tay!"); return; }
                useBinhHeal(player, item);
            }
            default -> openBinhMenu(player);
        }
    }

    // ========================
    //  EFFECTS
    // ========================

    private void spawnCollectEffect(Location loc) {
        World w = loc.getWorld();
        new BukkitRunnable() { int t=0; @Override public void run() {
            if (t++>=20){cancel();return;}
            for (int i=0;i<5;i++) {
                double a=Math.toRadians(t*18+i*72);
                double r=t*0.12+0.3;
                w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*r, t*0.1, Math.sin(a)*r),
                        1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(220,0,50),1.3f));
            }
        }}.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnUseEffect(Location loc, Color color) {
        World w = loc.getWorld();
        w.spawnParticle(Particle.EXPLOSION, loc.clone().add(0,1,0), 3, 0.5,0.5,0.5,0);
        for (double a=0;a<360;a+=15) {
            double rad=Math.toRadians(a);
            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(rad)*3,0.1,Math.sin(rad)*3),
                    2,0.1,0.1,0.1,0, new Particle.DustOptions(color,2.0f));
        }
    }

    // ========================
    //  HELPERS
    // ========================

    private boolean isOurBoss(Entity e) {
        return e.getPersistentDataContainer()
                .has(new NamespacedKey(plugin, KEY_BOSS), PersistentDataType.BYTE);
    }

    private Location getMostPopulatedLocation() {
        Map<String, Integer> chunkCount = new HashMap<>();
        Map<String, Location> chunkCenter = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            String key = p.getLocation().getChunk().getX() + ":" + p.getLocation().getChunk().getZ()
                    + ":" + p.getWorld().getName();
            chunkCount.merge(key, 1, Integer::sum);
            chunkCenter.putIfAbsent(key, p.getLocation().clone().add(0,0,0));
        }
        return chunkCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> chunkCenter.get(e.getKey()))
                .orElse(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    private void broadcastWarning(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(msg);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
        });
    }

    private String formatLoc(Location l) {
        return String.format("(%d, %d, %d)", l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public double getHuyetTinh()    { return huyetTinh; }
    public double getMaxHuyetTinh() { return MAX_HUYETTINH; }
    public int getHuyetTinhPct()    { return (int)(huyetTinh / MAX_HUYETTINH * 100); }

    @SuppressWarnings({"deprecation","unchecked"})
    private void setMaxHealth(LivingEntity entity, double hp) {
        // Use Registry to find max_health attribute - works on Paper 1.21+
        try {
            for (org.bukkit.attribute.Attribute a : org.bukkit.Registry.ATTRIBUTE) {
                String key = a.getKey().getKey();
                if (key.equals("max_health") || key.equals("generic.max_health")) {
                    org.bukkit.attribute.AttributeInstance inst = entity.getAttribute(a);
                    if (inst != null) { inst.setBaseValue(hp); entity.setHealth(hp); return; }
                }
            }
        } catch (Exception ignored) {}
        // Fallback: just set current health to max possible
        try { entity.setHealth(entity.getMaxHealth()); } catch (Exception ignored) {}
    }

    // Buff liên tục cho Quy Tu trong Blood Moon
    private void startQuyTuPassiveLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                if (!active) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData _d = plugin.getDataManager().get(p);
                    if (_d == null || !_d.hasClass()) continue;
                    if (_d.getClassType() != com.tutien.data.ClassType.QUY_TU) continue;
                    // Tái áp dụng buff nếu hết
                    if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")))) {
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 300, 2, false, false, false));
                    }
                    if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("night_vision")))) {
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.getByKey(NamespacedKey.minecraft("night_vision")), 300, 0, false, false, false));
                    }
                    if (!p.hasPotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")))) {
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 300, 1, false, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 200L); // mỗi 10 giây
    }

    // Sét liên tục trong Blood Moon (cứ 15-30s một cái ngẫu nhiên)
    private void startLightningLoop() {
        new BukkitRunnable() {
            int t = 0;
            final Random rng = new Random();
            @Override public void run() {
                if (!active) { cancel(); return; }
                t++;
                // Sét ngẫu nhiên mỗi 15-25s
                if (t % (15 + rng.nextInt(10)) == 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
                        // Sét trong bán kính 30m quanh player (không damage)
                        double angle = rng.nextDouble() * 2 * Math.PI;
                        double dist  = 10 + rng.nextDouble() * 20;
                        Location bolt = p.getLocation().clone().add(
                                Math.cos(angle)*dist, 0, Math.sin(angle)*dist);
                        p.getWorld().strikeLightningEffect(bolt);
                    }
                }
                // Particles đỏ mưa xuống mỗi 2s
                if (t % 2 == 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        World w = p.getWorld();
                        if (w.getEnvironment() != World.Environment.NORMAL) continue;
                        for (int i = 0; i < 5; i++) {
                            double rx = (rng.nextDouble()-0.5)*30, rz = (rng.nextDouble()-0.5)*30;
                            Location drop = p.getLocation().clone().add(rx, 8, rz);
                            w.spawnParticle(Particle.DUST, drop, 1, 0,0,0, 0,
                                    new Particle.DustOptions(Color.fromRGB(180,0,20), 1.5f));
                        }
                    }
                }
                // Ambient sound nghe như máu đổ mỗi 30s
                if (t % 30 == 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.3f);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Vòng hào quang đỏ liên tục quanh mỗi người chơi trong Blood Moon
    private void startBloodAuraLoop() {
        new BukkitRunnable() {
            double tick = 0;
            @Override public void run() {
                if (!active) { cancel(); return; }
                tick += 0.15;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
                    // Vòng tròn nhỏ đỏ ở chân
                    for (int i = 0; i < 6; i++) {
                        double a = tick + i * (Math.PI * 2 / 6);
                        Location pos = p.getLocation().clone().add(Math.cos(a)*0.8, 0.05, Math.sin(a)*0.8);
                        p.getWorld().spawnParticle(Particle.DUST, pos, 1, 0,0,0, 0,
                                new Particle.DustOptions(Color.fromRGB(180+new Random().nextInt(50), 0, 20), 0.9f));
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 1L);
    }

    // ========================
    //  HIỆU ỨNG KHÍ QUYỂN MÁU (liên tục trong Blood Moon)
    // ========================
    private void startAtmosphereLoop() {
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!active) { cancel(); return; }
                tick++;
                for (World w : Bukkit.getWorlds()) {
                    if (w.getEnvironment() != World.Environment.NORMAL) continue;
                    // Sét ngẫu nhiên (không gây sát thương)
                    if (tick % 60 == 0 && Math.random() < 0.4) {
                        for (Player p : w.getPlayers()) {
                            Location near = p.getLocation().clone()
                                    .add((Math.random()-0.5)*80, 0, (Math.random()-0.5)*80);
                            near.setY(w.getHighestBlockYAt(near.getBlockX(), near.getBlockZ()) + 1);
                            w.strikeLightningEffect(near); // Effect only - no damage
                        }
                    }
                    // Hạt máu rơi quanh người chơi
                    if (tick % 10 == 0) {
                        for (Player p : w.getPlayers()) {
                            Location pl = p.getLocation().clone().add(0, 3, 0);
                            // Giọt máu từ trên trời
                            for (int i=0;i<5;i++) {
                                double ox=(Math.random()-0.5)*20, oz=(Math.random()-0.5)*20;
                                Location drop = p.getLocation().clone().add(ox, 15+Math.random()*10, oz);
                                w.spawnParticle(Particle.DUST, drop, 1, 0.3,0.3,0.3, 0,
                                        new Particle.DustOptions(Color.fromRGB(180,0,0), 1.2f));
                            }
                            // Sương máu sát mặt đất
                            for (int i=0;i<8;i++) {
                                double ox=(Math.random()-0.5)*30, oz=(Math.random()-0.5)*30;
                                Location fog = p.getLocation().clone().add(ox, Math.random()*2, oz);
                                w.spawnParticle(Particle.DUST, fog, 1, 0.5,0.1,0.5, 0,
                                        new Particle.DustOptions(Color.fromRGB(150,0,20), 2.0f));
                            }
                            // Ember đỏ bay lên
                            if (tick % 20 == 0) {
                                Location ember = p.getLocation().clone().add(
                                        (Math.random()-0.5)*15, 0, (Math.random()-0.5)*15);
                                w.spawnParticle(Particle.SOUL_FIRE_FLAME, ember, 2, 0.2,0.5,0.2, 0.02);
                            }
                        }
                    }
                    // Tiếng sét + gió
                    if (tick % 100 == 0) {
                        for (Player p : w.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.5f);
                        }
                    }
                    // Tiếng quỷ
                    if (tick % 200 == 0) {
                        for (Player p : w.getPlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }
}