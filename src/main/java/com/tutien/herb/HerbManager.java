package com.tutien.herb;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Vườn Tiên Thảo: chỉ thu được thảo dược trong region được admin tạo.
 * Block trong vườn là "nút thu hoạch"; thu hoạch không phá block, node tự hồi sau cooldown.
 * Toàn bộ loại thảo dược + tỉ lệ + cảnh giới đều nằm trong config.yml.
 */
public class HerbManager implements Listener {
    private final TuTienPlugin plugin;
    private final Random random = new Random();
    private final List<HerbType> herbs = new ArrayList<>();
    private final Map<Material, HerbType> blockToHerb = new HashMap<>();
    private final Map<Material, HerbType> plantBlockToHerb = new HashMap<>();
    private final Map<String, Long> harvestCooldown = new HashMap<>();
    // Đánh dấu lần cuối đã báo cooldown cho từng người chơi + từng node.
    // Một node đang hồi chỉ báo "Nút này đang hồi Xs" đúng 1 lần cho mỗi người chơi.
    private final Map<String, Long> cooldownNoticeShown = new HashMap<>();
    // Chống việc giữ chuột phải làm event RIGHT_CLICK_BLOCK chạy quá dày.
    private final Map<String, Long> interactThrottle = new HashMap<>();
    private final Map<String, GardenRegion> gardens = new LinkedHashMap<>();

    public record HerbType(String id, String name, String color, Material baseMat,
                           int minStageOrd, double dropChance, long ltValue, double lkPct,
                           boolean plantable, Material seedBlock, double plantYieldMultiplier,
                           double premiumChance, PlantSurface plantSurface) {}
    private record GardenRegion(String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {}

    /** Bề mặt bắt buộc bên dưới để gieo được Linh Chủng — mỗi loại cây thật cần nền khác nhau. */
    public enum PlantSurface {
        FARMLAND("đất đã cày (farmland)"),
        SOUL_SAND("Cát Linh Hồn (Soul Sand)"),
        GRASS("đất/cỏ (Grass Block, Dirt, Coarse Dirt, Podzol)");

        private final String label;
        PlantSurface(String label) { this.label = label; }
        public String label() { return label; }

        public boolean matches(Material block) {
            return switch (this) {
                case FARMLAND -> block == Material.FARMLAND;
                case SOUL_SAND -> block == Material.SOUL_SAND;
                case GRASS -> block == Material.GRASS_BLOCK || block == Material.DIRT
                        || block == Material.COARSE_DIRT || block == Material.PODZOL;
            };
        }

        /** Block để lại tại vị trí cây sau khi thu hoạch, giữ đúng hành vi cũ (trả về farmland) cho từng loại nền. */
        public Material replantBlock() {
            return switch (this) {
                case FARMLAND -> Material.FARMLAND;
                case SOUL_SAND -> Material.SOUL_SAND;
                case GRASS -> Material.GRASS_BLOCK;
            };
        }

        /** Tự suy ra nền phù hợp dựa theo seed-block, để config cũ không cần khai báo thêm gì. */
        static PlantSurface inferFrom(Material seedBlock) {
            if (seedBlock == Material.NETHER_WART) return SOUL_SAND;
            if (seedBlock == Material.SWEET_BERRY_BUSH) return GRASS;
            return FARMLAND;
        }
    }

    // Cây đã trồng cần đất cày (farmland) bên dưới mới lớn được, giống cơ chế nông nghiệp thật.
    private static final Set<Material> FARMLAND_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.TORCHFLOWER_CROP, Material.PITCHER_CROP);
    // Các block Ageable khác đã được hệ thống hỗ trợ đầy đủ (nền riêng, xem PlantSurface).
    private static final Set<Material> KNOWN_SEED_BLOCKS;
    static {
        EnumSet<Material> known = EnumSet.copyOf(FARMLAND_CROPS);
        known.add(Material.NETHER_WART);
        known.add(Material.SWEET_BERRY_BUSH);
        KNOWN_SEED_BLOCKS = known;
    }
    // Cooldown "tưới linh khí" cho từng cây (theo toạ độ), tránh spam đẩy cây lớn liên tục.
    private final Map<String, Long> waterCooldown = new HashMap<>();
    // Chỉ những cây được gieo bằng Linh Chủng của plugin mới được xử lý như Thảo Dược.
    // Lưu location -> herb id để không nhầm cây Minecraft tự nhiên/trồng bình thường.
    private final Map<String, String> plantedHerbs = new HashMap<>();

    public HerbManager(TuTienPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        herbs.clear(); blockToHerb.clear(); plantBlockToHerb.clear(); gardens.clear();
        harvestCooldown.clear(); cooldownNoticeShown.clear(); interactThrottle.clear(); waterCooldown.clear(); plantedHerbs.clear();
        loadPlantedHerbs();
        ConfigurationSection hs = plugin.getConfig().getConfigurationSection("herbs.types");
        if (hs != null) {
            for (String id : hs.getKeys(false)) {
                ConfigurationSection s = hs.getConfigurationSection(id);
                if (s == null) continue;
                try {
                    Material mat = Material.valueOf(s.getString("block", "SHORT_GRASS").toUpperCase());
                    boolean plantable = s.getBoolean("plantable", false);
                    Material seedBlock = null;
                    PlantSurface surface = PlantSurface.FARMLAND;
                    if (plantable) {
                        try {
                            seedBlock = Material.valueOf(s.getString("seed-block", "WHEAT").toUpperCase());
                            if (!(seedBlock.createBlockData() instanceof Ageable)) {
                                plugin.getLogger().warning("[Herb] seed-block " + seedBlock
                                        + " cho " + id + " không phải cây có thể lớn dần (Ageable) - tắt trồng cho loại này.");
                                plantable = false; seedBlock = null;
                            } else {
                                if (!KNOWN_SEED_BLOCKS.contains(seedBlock))
                                    plugin.getLogger().warning("[Herb] seed-block " + seedBlock
                                            + " cho " + id + " chưa được kiểm định kỹ — vẫn dùng được nhưng có thể không đúng ý.");
                                String surfaceCfg = s.getString("plant-surface", null);
                                surface = surfaceCfg != null
                                        ? PlantSurface.valueOf(surfaceCfg.toUpperCase())
                                        : PlantSurface.inferFrom(seedBlock);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("[Herb] seed-block sai cho " + id + " - tắt trồng cho loại này.");
                            plantable = false; seedBlock = null;
                        }
                    }
                    HerbType h = new HerbType(id,
                            s.getString("name", id),
                            s.getString("color", "§a"), mat,
                            s.getInt("min-stage", 0),
                            s.getDouble("drop-chance", .25),
                            s.getLong("lt-value", 1),
                            s.getDouble("lk-percent", 0),
                            plantable, seedBlock,
                            s.getDouble("grow-yield-multiplier", 2.0),
                            s.getDouble("premium-chance", 0.1),
                            surface);
                    herbs.add(h); blockToHerb.put(mat, h);
                    if (plantable && seedBlock != null) plantBlockToHerb.put(seedBlock, h);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Herb] Herb config sai: " + id + " - " + e.getMessage());
                }
            }
        }
        ConfigurationSection gs = plugin.getConfig().getConfigurationSection("herb-garden.regions");
        if (gs != null) for (String id : gs.getKeys(false)) {
            ConfigurationSection s = gs.getConfigurationSection(id);
            if (s == null) continue;
            gardens.put(id, new GardenRegion(
                    s.getString("world", "world"),
                    s.getInt("min-x"), s.getInt("max-x"),
                    s.getInt("min-y", -64), s.getInt("max-y", 320),
                    s.getInt("min-z"), s.getInt("max-z")));
        }
        plugin.getLogger().info("[Herb] Đã nạp " + herbs.size() + " loại thảo dược và " + gardens.size() + " vườn.");
        if (plugin.getConfig().getBoolean("herb-garden.require-garden", true) && gardens.isEmpty())
            plugin.getLogger().warning("[Herb] Chưa có vườn. Dùng /ttadmin vuon tao <tên> <bán_kính> để tạo vườn.");
    }

    public List<HerbType> getHerbs() { return Collections.unmodifiableList(herbs); }

    public ItemStack createHerb(HerbType type, int amount) { return createHerb(type, amount, false); }

    public ItemStack createHerb(HerbType type, int amount, boolean premium) {
        ItemStack item = new ItemStack(Material.SUGAR, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((premium ? "§6★ " : "") + type.color() + "✿ " + type.name()
                + (premium ? " §6(Thượng Phẩm)" : ""));
        List<String> lore = new ArrayList<>(List.of(
                "§7Tiên Thảo dùng để luyện đan.",
                "§7Cấp mở: §e" + type.minStageOrd(),
                "§7Tỉ lệ thu: §e" + String.format(Locale.US, "%.1f", type.dropChance() * 100) + "%"
        ));
        if (premium) lore.add("§6Trồng từ Vườn Tiên Thảo — phẩm chất hơn hẳn thảo dại.");
        lore.add("§8ID: " + type.id());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "herb_id"),
                PersistentDataType.STRING, type.id());
        if (premium) meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "herb_premium"),
                PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHerb(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "herb_id"), PersistentDataType.STRING);
    }
    public String getHerbId(ItemStack item) {
        if (!isHerb(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "herb_id"), PersistentDataType.STRING);
    }
    public Optional<HerbType> getHerbType(String id) {
        return herbs.stream().filter(h -> h.id().equalsIgnoreCase(id)).findFirst();
    }

    // ========================
    //  LINH CHỦNG (hạt giống trồng vườn)
    // ========================

    public ItemStack createSeed(HerbType type, int amount) {
        if (type == null || !type.plantable() || type.seedBlock() == null || amount <= 0) return null;
        ItemStack item = new ItemStack(Material.WHEAT_SEEDS, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.color() + "☘ Linh Chủng: " + type.name());
        meta.setLore(List.of(
                "§7Gieo trên đất đã cày trong Vườn Tiên Thảo.",
                "§7Trồng sẽ cho thảo dược nhiều & tốt hơn hái ngoài tự nhiên.",
                "§7Cấp mở: §e" + type.minStageOrd(),
                "§8ID: " + type.id()
        ));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "linh_chung_id"),
                PersistentDataType.STRING, type.id());
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSeed(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "linh_chung_id"), PersistentDataType.STRING);
    }
    public String getSeedHerbId(ItemStack item) {
        if (!isSeed(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "linh_chung_id"), PersistentDataType.STRING);
    }

    private boolean inGarden(Location loc) {
        if (!plugin.getConfig().getBoolean("herb-garden.enabled", true)) return false;
        if (!plugin.getConfig().getBoolean("herb-garden.require-garden", true)) return true;
        for (GardenRegion r : gardens.values()) {
            if (!loc.getWorld().getName().equals(r.world())) continue;
            if (loc.getBlockX() >= Math.min(r.minX(), r.maxX()) && loc.getBlockX() <= Math.max(r.minX(), r.maxX())
                    && loc.getBlockY() >= Math.min(r.minY(), r.maxY()) && loc.getBlockY() <= Math.max(r.minY(), r.maxY())
                    && loc.getBlockZ() >= Math.min(r.minZ(), r.maxZ()) && loc.getBlockZ() <= Math.max(r.minZ(), r.maxZ())) return true;
        }
        return false;
    }

    private boolean harvest(Player player, Block block) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass() || !inGarden(block.getLocation())) return false;
        HerbType herb = blockToHerb.get(block.getType());
        if (herb == null || herb.minStageOrd() > data.getStage().ordinal()) return false;

        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        long now = System.currentTimeMillis();
        long until = harvestCooldown.getOrDefault(key, 0L);
        if (until > now) {
            long sec = Math.max(1, (until - now) / 1000);

            // Chỉ báo cooldown 1 lần cho mỗi người chơi trên mỗi node trong cùng 1 chu kỳ hồi.
            String noticeKey = player.getUniqueId() + "|" + key;
            Long shownForUntil = cooldownNoticeShown.get(noticeKey);
            if (shownForUntil == null || shownForUntil.longValue() != until) {
                cooldownNoticeShown.put(noticeKey, until);
                player.sendMessage(plugin.prefix() + "§7Nút này đang hồi §e" + sec + "s§7.");
            }
            return true;
        }

        // Cooldown cũ đã hết, dọn cờ thông báo của người chơi trên node này.
        String currentNoticeKey = player.getUniqueId() + "|" + key;
        cooldownNoticeShown.remove(currentNoticeKey);

        double chance = herb.dropChance() * plugin.getConfig().getDouble("herb-garden.harvest.drop-multiplier", 1.0);
        boolean success = random.nextDouble() <= Math.min(1.0, chance);
        long cd = plugin.getConfig().getLong("herb-garden.harvest.node-respawn-seconds", 30) * 1000L;
        harvestCooldown.put(key, now + (success ? cd : Math.max(3000L, cd / 5)));
        if (!success) {
            player.sendMessage(plugin.prefix() + "§8Bạn tìm thấy dấu vết linh lực nhưng chưa hái được gì.");
            return true;
        }

        int min = plugin.getConfig().getInt("herb-garden.harvest.min-amount", 1);
        int max = Math.max(min, plugin.getConfig().getInt("herb-garden.harvest.max-amount", 2));
        int amount = min + random.nextInt(max - min + 1);
        ItemStack item = createHerb(herb, amount);
        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        left.values().forEach(it -> block.getWorld().dropItemNaturally(block.getLocation(), it));

        if (plugin.getConfig().getBoolean("herb-garden.harvest.give-linh-khi", false)) {
            long lk = (long) (data.getMaxLinhKhi() * herb.lkPct()) * amount;
            if (lk > 0) data.addLinhKhi(lk);
        }
        player.sendMessage(plugin.prefix() + herb.color() + "✿ Hái được §l" + herb.name()
                + " §r" + herb.color() + "×" + amount + " §7| node hồi §e" + (cd / 1000) + "s");

        // Hái ngoài tự nhiên đôi khi nhặt được luôn linh chủng — khuyến khích người
        // chơi đem về trồng trong vườn để có thảo dược nhiều & tốt hơn.
        if (herb.plantable()) {
            double seedChance = plugin.getConfig().getDouble("herb-garden.plant.wild-seed-chance", 0.08);
            if (random.nextDouble() <= seedChance) {
                ItemStack seed = createSeed(herb, 1);
                if (seed != null) {
                    Map<Integer, ItemStack> leftSeed = player.getInventory().addItem(seed);
                    leftSeed.values().forEach(it -> block.getWorld().dropItemNaturally(block.getLocation(), it));
                    player.sendMessage(plugin.prefix() + "§b☘ Bạn còn nhặt được §aLinh Chủng " + herb.name() + "§b!");
                }
            }
        }
        block.getWorld().spawnParticle(Particle.ENCHANTED_HIT, block.getLocation().add(.5,1,.5),
                10,.3,.4,.3,.08);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, .5f, 1.4f);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String key = locationKey(block);
        String plantedId = plantedHerbs.get(key);

        if (plantedId != null) {
            Optional<HerbType> opt = getHerbType(plantedId);
            HerbType plantHerb = opt.orElse(null);
            if (plantHerb != null && block.getType() == plantHerb.seedBlock()) {
                harvestPlant(event, plantHerb);
                return;
            }
            // Marker cũ nhưng block đã bị thay đổi/phá bởi cơ chế khác: loại bỏ để không tái nhận nhầm.
            plantedHerbs.remove(key);
            savePlantedHerbs();
        }

        // Crop tự nhiên/trồng bình thường không phải Thảo Dược của plugin.
        if (harvest(event.getPlayer(), block)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        // Gieo linh chủng: chuột phải bằng hạt giống lên đất đã cày trong vườn.
        if (isSeed(hand)) {
            if (plantSeed(player, clicked, hand)) event.setCancelled(true);
            return;
        }
        // Tưới linh khí cho cây đang trồng: lén (sneak) + tay không + chuột phải vào cây.
        if (player.isSneaking() && (hand == null || hand.getType() == Material.AIR)
                && plantedHerbs.containsKey(locationKey(clicked))
                && isTrackedPlant(clicked)) {
            if (waterPlant(player, clicked)) event.setCancelled(true);
            return;
        }
        if (player.isSneaking()) return;

        // Giữ chuột phải sẽ tạo rất nhiều RIGHT_CLICK_BLOCK event; giới hạn xử lý xuống ~1 lần/350ms.
        String interactionKey = player.getUniqueId() + "|"
                + clicked.getWorld().getName() + ":"
                + clicked.getX() + ":"
                + clicked.getY() + ":"
                + clicked.getZ();
        long now = System.currentTimeMillis();
        long lastInteraction = interactThrottle.getOrDefault(interactionKey, 0L);
        if (now - lastInteraction < 350L) {
            event.setCancelled(true);
            return;
        }
        interactThrottle.put(interactionKey, now);

        if (harvest(player, clicked)) event.setCancelled(true);
    }

    /** Gieo hạt giống lên đúng nền yêu cầu (farmland/soul sand/đất cỏ...) trong vườn -> mọc linh chủng non (age 0). */
    private boolean plantSeed(Player player, Block surfaceBlock, ItemStack seedItem) {
        String herbId = getSeedHerbId(seedItem);
        Optional<HerbType> opt = getHerbType(herbId);
        if (opt.isEmpty()) return false;
        HerbType herb = opt.get();
        if (!plugin.getConfig().getBoolean("herb-garden.plant.enabled", true)) return false;
        if (!inGarden(surfaceBlock.getLocation())) return false;

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return false;
        if (herb.minStageOrd() > data.getStage().ordinal()) {
            player.sendMessage(plugin.prefix() + "§cCảnh giới chưa đủ để trồng " + herb.color() + herb.name() + "§c.");
            return true;
        }
        if (!herb.plantSurface().matches(surfaceBlock.getType())) {
            player.sendMessage(plugin.prefix() + "§7Linh chủng " + herb.color() + herb.name()
                    + " §7cần được gieo trên " + herb.plantSurface().label() + ".");
            return true;
        }
        Block target = surfaceBlock.getRelative(BlockFace.UP);
        if (target.getType() != Material.AIR) {
            player.sendMessage(plugin.prefix() + "§7Chỗ này đã có cây rồi.");
            return true;
        }

        target.setType(herb.seedBlock());
        if (target.getBlockData() instanceof Ageable age) {
            age.setAge(0);
            target.setBlockData(age);
        } else {
            player.sendMessage(plugin.prefix() + "§cLoại cây này không hỗ trợ trồng Thảo Dược.");
            return true;
        }
        String plantedKey = locationKey(target);
        plantedHerbs.put(plantedKey, herb.id());
        savePlantedHerbs();
        seedItem.setAmount(seedItem.getAmount() - 1);
        player.sendMessage(plugin.prefix() + herb.color() + "☘ Đã gieo linh chủng " + herb.name()
                + " §7— chờ nó hấp thụ linh khí trời đất, hoặc lén tưới linh khí để cây lớn nhanh hơn.");
        surfaceBlock.getWorld().playSound(target.getLocation(), Sound.ITEM_CROP_PLANT, .6f, 1.1f);
        return true;
    }

    /** Lén (sneak) + tay không + chuột phải vào cây đang trồng -> dùng linh khí của bản thân đẩy nhanh sinh trưởng. */
    private boolean waterPlant(Player player, Block plant) {
        if (!isTrackedPlant(plant)) return false;
        if (!(plant.getBlockData() instanceof Ageable age)) return false;
        if (!inGarden(plant.getLocation())) return false;
        if (age.getAge() >= age.getMaximumAge()) {
            player.sendMessage(plugin.prefix() + "§7Cây đã trưởng thành, có thể thu hoạch rồi.");
            return true;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return false;

        String key = plant.getWorld().getName() + ":" + plant.getX() + ":" + plant.getY() + ":" + plant.getZ();
        long now = System.currentTimeMillis();
        long until = waterCooldown.getOrDefault(key, 0L);
        if (until > now) {
            long sec = Math.max(1, (until - now) / 1000);
            player.sendMessage(plugin.prefix() + "§7Linh khí vừa tưới chưa kịp thấm, còn §e" + sec + "s§7.");
            return true;
        }

        double costPct = plugin.getConfig().getDouble("herb-garden.plant.water-linh-khi-cost-percent", 5.0) / 100.0;
        long cost = (long) (data.getMaxLinhKhi() * costPct);
        if (cost > 0 && data.getLinhKhi() < cost) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ linh khí để tưới cho cây (" + "cần §e" + cost + "§c).");
            return true;
        }
        if (cost > 0) data.setLinhKhi(Math.max(0, data.getLinhKhi() - cost));

        int boost = Math.max(1, plugin.getConfig().getInt("herb-garden.plant.water-age-boost", 1));
        age.setAge(Math.min(age.getMaximumAge(), age.getAge() + boost));
        plant.setBlockData(age);

        long cdSec = plugin.getConfig().getLong("herb-garden.plant.water-cooldown-seconds", 120);
        waterCooldown.put(key, now + cdSec * 1000L);

        boolean matured = age.getAge() >= age.getMaximumAge();
        player.sendMessage(plugin.prefix() + "§b✦ Tưới linh khí thành công" + (matured ? " — §a cây đã trưởng thành!" : "§7, cây lớn thêm."));
        plant.getWorld().spawnParticle(Particle.WITCH, plant.getLocation().add(.5, .6, .5), 12, .25, .3, .25, 0.02);
        plant.getWorld().playSound(plant.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, .6f, 1.6f);
        return true;
    }

    /** Phá cây đang trồng: chỉ cho thu hoạch khi đã lớn hết cỡ, năng suất & phẩm chất cao hơn hái node. */
    private void harvestPlant(BlockBreakEvent event, HerbType herb) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable age)) return;
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass() || !inGarden(block.getLocation())) return;

        if (age.getAge() < age.getMaximumAge()) {
            event.setCancelled(true);
            player.sendMessage(plugin.prefix() + "§7Linh chủng chưa hấp thụ đủ linh khí, chưa thể thu hoạch. "
                    + "§8(lén + chuột phải để tưới linh khí đẩy nhanh)");
            return;
        }

        event.setCancelled(true);
        event.setDropItems(false);
        int min = plugin.getConfig().getInt("herb-garden.plant.min-amount", 2);
        int max = Math.max(min, plugin.getConfig().getInt("herb-garden.plant.max-amount", 4));
        int base = min + random.nextInt(max - min + 1);
        int amount = Math.max(1, (int) Math.round(base * herb.plantYieldMultiplier()));
        boolean premium = random.nextDouble() <= Math.max(0, Math.min(1, herb.premiumChance()));

        ItemStack item = createHerb(herb, amount, premium);
        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        left.values().forEach(it -> block.getWorld().dropItemNaturally(block.getLocation(), it));

        if (plugin.getConfig().getBoolean("herb-garden.harvest.give-linh-khi", false)) {
            long lk = (long) (data.getMaxLinhKhi() * herb.lkPct() * (premium ? 1.5 : 1.0)) * amount;
            if (lk > 0) data.addLinhKhi(lk);
        }
        player.sendMessage(plugin.prefix() + herb.color() + "✿ Thu hoạch " + (premium ? "§6★ Thượng Phẩm §r" : "")
                + herb.name() + " §r" + herb.color() + "×" + amount + " §7từ vườn trồng.");
        block.getWorld().spawnParticle(Particle.ENCHANTED_HIT, block.getLocation().add(.5, .6, .5),
                16, .35, .4, .35, .1);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, .7f, 1.6f);
        // Sau khi thu hoạch cây trồng, xóa marker trước rồi trả ô về đúng nền của loại thảo dược đó.
        plantedHerbs.remove(locationKey(block));
        savePlantedHerbs();
        block.setType(herb.plantSurface().replantBlock());
    }

    private String locationKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private boolean isTrackedPlant(Block block) {
        String id = plantedHerbs.get(locationKey(block));
        if (id == null) return false;
        HerbType herb = getHerbType(id).orElse(null);
        if (herb == null || herb.seedBlock() == null || block.getType() != herb.seedBlock()) {
            plantedHerbs.remove(locationKey(block));
            savePlantedHerbs();
            return false;
        }
        return true;
    }

    private void loadPlantedHerbs() {
        List<String> entries = plugin.getConfig().getStringList("herb-garden.planted");
        for (String entry : entries) {
            int split = entry.lastIndexOf('=');
            if (split <= 0 || split >= entry.length() - 1) continue;
            String key = entry.substring(0, split);
            String id = entry.substring(split + 1);
            if (!id.isBlank()) plantedHerbs.put(key, id);
        }
    }

    private void savePlantedHerbs() {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : plantedHerbs.entrySet()) {
            entries.add(entry.getKey() + "=" + entry.getValue());
        }
        plugin.getConfig().set("herb-garden.planted", entries);
        plugin.saveConfig();
    }

    public int countHerb(Player player, String herbId) {
        int total = 0;
        for (ItemStack it : player.getInventory().getContents())
            if (isHerb(it) && herbId.equalsIgnoreCase(getHerbId(it))) total += it.getAmount();
        return total;
    }

    public boolean removeHerb(Player player, String herbId, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (!isHerb(it) || !herbId.equalsIgnoreCase(getHerbId(it))) continue;
            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
            remaining -= take;
            if (remaining <= 0) return true;
        }
        return false;
    }

    // Admin helpers for /ttadmin vuon ...
    public void createGarden(String name, Player admin, int radius) {
        int r = Math.max(2, Math.min(radius, 500));
        String path = "herb-garden.regions." + name;
        Location l = admin.getLocation();
        plugin.getConfig().set(path + ".world", l.getWorld().getName());
        plugin.getConfig().set(path + ".min-x", l.getBlockX() - r);
        plugin.getConfig().set(path + ".max-x", l.getBlockX() + r);
        plugin.getConfig().set(path + ".min-y", l.getWorld().getMinHeight());
        plugin.getConfig().set(path + ".max-y", l.getWorld().getMaxHeight());
        plugin.getConfig().set(path + ".min-z", l.getBlockZ() - r);
        plugin.getConfig().set(path + ".max-z", l.getBlockZ() + r);
        plugin.saveConfig();
        reload();
    }
    public boolean removeGarden(String name) {
        if (!gardens.containsKey(name)) return false;
        plugin.getConfig().set("herb-garden.regions." + name, null);
        plugin.saveConfig();
        reload();
        return true;
    }
    public Set<String> getGardenNames() { return Collections.unmodifiableSet(gardens.keySet()); }

    public void showHerbInfo(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        player.sendMessage("§6§l═══ VƯỜN TIÊN THẢO ═══");
        player.sendMessage("§7Vào đúng khu vườn → phá/chuột phải cây → nhận thảo dược.");
        player.sendMessage("§7Có Linh Chủng (☘) → chuột phải lên đất đã cày để gieo trồng.");
        player.sendMessage("§7Lén (sneak) + tay không + chuột phải vào cây đang trồng để tưới linh khí, giúp lớn nhanh hơn.");
        player.sendMessage("§7Vườn hiện có: §e" + gardens.size() + " §7khu.");
        for (HerbType h : herbs) {
            boolean unlock = data.getStage().ordinal() >= h.minStageOrd();
            player.sendMessage((unlock ? "§a✓ " : "§8✗ ") + h.color() + h.name()
                    + (h.plantable() ? " §b☘" : "")
                    + " §8| §7" + String.format(Locale.US, "%.1f", h.dropChance()*100) + "%");
        }
    }
}
