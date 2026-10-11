package com.tutien.tienthao;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * TienThao - Tiên thảo xuất hiện tự nhiên trên map.
 *
 * Các loại tiên thảo (10 loại, mỗi loại dùng cho luyện đan khác nhau):
 *  Linh Thảo    - Xanh lá   - phổ thông nhất
 *  Hoa Thảo     - Đỏ        - ra hoa dạng
 *  Tuyết Thảo   - Trắng     - mọc ở tuyết
 *  Âm Thảo      - Tím       - mọc ở hang
 *  Dương Thảo   - Vàng      - mọc ở sa mạc
 *  Mộc Linh     - Xanh đậm  - rừng sâm
 *  Hỏa Linh     - Cam       - núi lửa
 *  Thủy Linh    - Xanh nước - biển/sông
 *  Kim Linh     - Vàng nhạt - cực hiếm
 *  Long Tử Thảo - Đỏ thẫm   - cực hiếm, để luyện đan hiếm
 *
 * Spawn: admin dùng /ttadmin spawntienthao [loại] để spawn thủ công
 * Auto: mỗi 5 phút spawn 1-3 cây ngẫu nhiên cho mỗi người chơi online
 */
public class TienThao implements Listener {

    private final TuTienPlugin plugin;
    private final Random rng = new Random();

    public enum ThaoType {
        LINH_THAO  ("Linh Thảo",    "§a",  Material.FERN,           0.40, Color.fromRGB(50,220,80)),
        HOA_THAO   ("Hoa Thảo",     "§c",  Material.POPPY,          0.30, Color.fromRGB(255,80,80)),
        TUYET_THAO ("Tuyết Thảo",   "§f",  Material.LILY_OF_THE_VALLEY, 0.15, Color.fromRGB(240,240,255)),
        AM_THAO    ("Âm Thảo",      "§5",  Material.DEAD_BUSH,      0.07, Color.fromRGB(140,50,200)),
        DUONG_THAO ("Dương Thảo",   "§e",  Material.SUNFLOWER,      0.04, Color.fromRGB(255,200,30)),
        MOC_LINH   ("Mộc Linh",     "§2",  Material.BIG_DRIPLEAF,   0.02, Color.fromRGB(20,160,20)),
        HOA_LINH   ("Hỏa Linh",     "§6",  Material.FIRE_CORAL,     0.01, Color.fromRGB(255,140,10)),
        THUY_LINH  ("Thủy Linh",    "§9",  Material.BLUE_ORCHID,    0.007,Color.fromRGB(60,160,255)),
        KIM_LINH   ("Kim Linh",     "§e§l",Material.GOLDEN_CARROT,  0.003,Color.fromRGB(255,215,0)),
        LONG_TU    ("Long Tử Thảo", "§4§l",Material.NETHER_WART,    0.0001,Color.fromRGB(200,10,10));

        public final String displayName, color;
        public final Material material;
        public final double spawnWeight;
        public final Color dustColor;

        ThaoType(String d, String c, Material m, double w, Color col) {
            displayName=d; color=c; material=m; spawnWeight=w; dustColor=col;
        }

        public static ThaoType random(Random rng) {
            double r = rng.nextDouble();
            double cum = 0;
            for (ThaoType t : values()) {
                cum += t.spawnWeight;
                if (r < cum) return t;
            }
            return LINH_THAO;
        }
    }

    public TienThao(TuTienPlugin plugin) {
        this.plugin = plugin;
        // 7.2.8: không còn auto-spawn ngẫu nhiên trên map.
        // Nguồn thảo dược chính thức là Vườn Tiên Thảo do HerbManager quản lý.
        startGlowLoop();
    }

    // ========================
    //  CREATE ITEM
    // ========================

    public ItemStack createThao(ThaoType type, int amount) {
        ItemStack item = new ItemStack(type.material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.color + "🌿 " + type.displayName);
        meta.setLore(Arrays.asList(
                "§7Tiên thảo quý giá, dùng để luyện đan.",
                "§8Loại: §e" + type.name(),
                "",
                "§7Dùng lệnh: §e/luyen <cong_thuc>",
                "§7Thu thập: §e/thao xem §7để xem kho"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin,"tien_thao_type"),
                PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isThao(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin,"tien_thao_type"), PersistentDataType.STRING);
    }

    public ThaoType getThaoType(ItemStack item) {
        if (!isThao(item)) return null;
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin,"tien_thao_type"), PersistentDataType.STRING);
        try { return ThaoType.valueOf(name); } catch (Exception e) { return ThaoType.LINH_THAO; }
    }

    // ========================
    //  SPAWN
    // ========================

    public void spawnThaoForPlayer(Player player) {
        ThaoType type = ThaoType.random(rng);
        int amount = rng.nextInt(3) + 1;
        ItemStack item = createThao(type, amount);

        // Spawn near player (5-15m away)
        double angle = rng.nextDouble() * Math.PI * 2;
        double dist  = 5 + rng.nextDouble() * 10;
        Location base = player.getLocation().clone().add(
                Math.cos(angle)*dist, 0, Math.sin(angle)*dist);
        // Điều chỉnh Y xuống đất
        base.setY(base.getWorld().getHighestBlockYAt(base));
        base.add(0, 1, 0);

        Item entity = player.getWorld().dropItem(base, item);
        entity.setPickupDelay(0);
        entity.setGlowing(true);
        entity.setCustomName(type.color + "🌿 " + type.displayName);
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(
                new NamespacedKey(plugin,"tienthao_spawn"),
                PersistentDataType.BYTE, (byte)1);

        // Particle spawn
        player.getWorld().spawnParticle(Particle.DUST, base, 8, 0.3,0.3,0.3,0,
                new Particle.DustOptions(type.dustColor, 1.5f));
        player.getWorld().playSound(base, Sound.BLOCK_GRASS_PLACE, 0.5f, 1.5f);

        player.sendMessage(plugin.prefix() + type.color + "🌿 Tiên thảo [" + type.displayName
                + "] §7xuất hiện gần bạn! (x§e" + amount + "§7)");
    }

    public void spawnThaoAt(Location loc, ThaoType type, int amount) {
        ItemStack item = createThao(type, amount);
        Item entity = loc.getWorld().dropItem(loc, item);
        entity.setPickupDelay(0);
        entity.setGlowing(true);
        entity.setCustomName(type.color + "🌿 " + type.displayName);
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(
                new NamespacedKey(plugin,"tienthao_spawn"), PersistentDataType.BYTE, (byte)1);
        loc.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.5,0.5,0.5,0,
                new Particle.DustOptions(type.dustColor, 2.0f));
        loc.getWorld().playSound(loc, Sound.BLOCK_FLOWERING_AZALEA_PLACE, 0.8f, 1.2f);
    }

    // ========================
    //  AUTO SPAWN
    // ========================

    private void startAutoSpawn() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null || !data.hasClass()) continue;
                    // 30% chance mỗi 5 phút
                    if (rng.nextInt(10) < 3) spawnThaoForPlayer(p);
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // mỗi 5 phút
    }

    // ========================
    //  GLOW LOOP
    // ========================

    private void startGlowLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity e : world.getEntities()) {
                        if (!(e instanceof Item item)) continue;
                        if (!item.getPersistentDataContainer()
                                .has(new NamespacedKey(plugin,"tienthao_spawn"), PersistentDataType.BYTE)) continue;
                        // Particle đẹp cho tiên thảo trên đất
                        world.spawnParticle(Particle.ENCHANTED_HIT,
                                item.getLocation().add(0,0.3,0), 1, 0.1,0.1,0.1,0.05);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ========================
    //  EVENTS
    // ========================

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item dropped = event.getItem();
        if (!isThao(dropped.getItemStack())) return;

        ThaoType type = getThaoType(dropped.getItemStack());
        if (type == null) return;
        int amount = dropped.getItemStack().getAmount();

        // Thông báo
        player.sendMessage(plugin.prefix() + type.color + "✦ Thu §e" + amount
                + " §e" + type.displayName + "§7! (Dùng /thao xem để xem kho)");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.6f, 1.5f);

        // Particle
        player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(0,1.5,0), 8, 0.3,0.3,0.3,0,
                new Particle.DustOptions(type.dustColor, 1.2f));
    }
}
