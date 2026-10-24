package com.tutien.career;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Config-driven secondary cultivation professions. Player progress is saved separately from config. */
public final class BachNgheManager implements Listener, CommandExecutor, TabCompleter {
    private final TuTienPlugin plugin;
    private final File configFile;
    private final File dataFile;
    private YamlConfiguration config;
    private YamlConfiguration data;
    private final Map<String, UUID> brewingOwners = new HashMap<>();

    public BachNgheManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "bach-nghe-tu-tien.yml");
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists() && !dataDir.mkdirs()) plugin.getLogger().warning("Không tạo được thư mục dữ liệu Bách Nghệ.");
        this.dataFile = new File(dataDir, "bach-nghe-data.yml");
        reload();
    }

    public void reload() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection professionSection = config.getConfigurationSection("professions");
            plugin.getLogger().info("Đã nạp Bách Nghệ Tu Tiên: " + (professionSection == null ? 0 : professionSection.getKeys(false).size()) + " nghề.");
        } catch (Exception ex) {
            plugin.getLogger().severe("Không nạp được Bách Nghệ: " + ex.getMessage());
            if (config == null) config = new YamlConfiguration();
            if (data == null) data = new YamlConfiguration();
        }
    }

    public void save() {
        if (data == null) return;
        try { data.save(dataFile); }
        catch (IOException ex) { plugin.getLogger().severe("Không lưu được dữ liệu Bách Nghệ: " + ex.getMessage()); }
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
    private String path(UUID uuid, String key) { return "players." + uuid + "." + key; }
    private String profession(Player player) { return data.getString(path(player.getUniqueId(), "profession"), ""); }
    private int level(Player player) { return Math.max(1, data.getInt(path(player.getUniqueId(), "level"), 1)); }
    private int xp(Player player) { return Math.max(0, data.getInt(path(player.getUniqueId(), "xp"), 0)); }
    private String profPath(String id, String suffix) { return "professions." + id + (suffix.isEmpty() ? "" : "." + suffix); }

    private List<String> professionIds(boolean enabledOnly) {
        ConfigurationSection section = config.getConfigurationSection("professions");
        if (section == null) return Collections.emptyList();
        List<String> ids = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            if (!enabledOnly || config.getBoolean(profPath(id, "enabled"), true)) ids.add(id);
        }
        return ids;
    }

    private String displayName(String id) { return config.getString(profPath(id, "name"), id); }
    private int maxLevel(String id) { return Math.max(1, config.getInt(profPath(id, "max-level"), config.getInt("settings.max-level", 50))); }
    private int xpRequired(String id, int currentLevel) {
        int base = Math.max(1, config.getInt("settings.xp-base", 100));
        int step = Math.max(0, config.getInt("settings.xp-per-level", 75));
        return Math.max(1, config.getInt(profPath(id, "xp-base"), base) + Math.max(0, currentLevel - 1) * step);
    }

    private void addXp(Player player, String action, String detail) {
        if (!config.getBoolean("settings.enabled", true) || !player.isOnline()) return;
        String id = profession(player);
        if (id.isBlank() || !config.getBoolean(profPath(id, "enabled"), true)) return;
        int amount = config.getInt(profPath(id, "xp." + action), 0);
        if (amount <= 0) return;
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0, config.getLong("settings.action-cooldown-seconds", 3)) * 1000L;
        long previous = data.getLong(path(player.getUniqueId(), "cooldowns." + action), 0L);
        if (now - previous < cooldown) return;
        data.set(path(player.getUniqueId(), "cooldowns." + action), now);

        int currentLevel = level(player);
        if (currentLevel >= maxLevel(id)) return;
        int currentXp = xp(player) + amount;
        int newLevel = currentLevel;
        while (newLevel < maxLevel(id) && currentXp >= xpRequired(id, newLevel)) {
            currentXp -= xpRequired(id, newLevel);
            newLevel++;
        }
        if (newLevel >= maxLevel(id)) currentXp = 0;
        data.set(path(player.getUniqueId(), "xp"), currentXp);
        data.set(path(player.getUniqueId(), "level"), newLevel);
        save();

        if (newLevel > currentLevel) {
            player.sendMessage(color(config.getString("messages.level-up",
                    "&6[Bách Nghệ] &aNghề &e%profession% &ađã lên cấp &e%level%&a!"))
                    .replace("%profession%", color(displayName(id))).replace("%level%", String.valueOf(newLevel)));
            int rewardEvery = Math.max(0, config.getInt("settings.reward-every-levels", 10));
            long rewardLk = Math.max(0L, config.getLong("settings.level-up-linh-khi", 250L));
            if (rewardEvery > 0 && rewardLk > 0 && newLevel % rewardEvery == 0) {
                PlayerData pd = plugin.getDataManager().getOrCreate(player);
                pd.addLinhKhi(rewardLk);
                plugin.getDataManager().save(pd);
                player.sendMessage(color("&dBách Nghệ thưởng thêm " + rewardLk + " Linh Khí."));
            }
        } else if (config.getBoolean("settings.show-xp-actionbar", false)) {
            player.sendMessage(color("&7+" + amount + " kinh nghiệm nghề " + displayName(id)));
        }
    }

    private boolean isMatureCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private ItemStack cropBonus(Material crop) {
        Material material = switch (crop) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
            default -> null;
        };
        return material == null ? null : new ItemStack(material, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseBrewingStand(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getAction().name().startsWith("LEFT")) return;
        if (event.getClickedBlock().getType() == Material.BREWING_STAND) {
            brewingOwners.put(locationKey(event.getClickedBlock().getLocation()), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        String key = locationKey(event.getBlock().getLocation());
        UUID ownerId = brewingOwners.remove(key);
        Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
        if (owner == null && event.getBlock().getWorld() != null) {
            Location center = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            double best = 64.0;
            for (Player candidate : event.getBlock().getWorld().getPlayers()) {
                double distance = candidate.getLocation().distanceSquared(center);
                if (distance <= best) { owner = candidate; best = distance; }
            }
        }
        if (owner == null || !profession(owner).equalsIgnoreCase("luyen_dan_su")) return;
        addXp(owner, "brew", "BrewEvent");
        int level = level(owner);
        double chancePerLevel = Math.max(0.0, config.getDouble(profPath("luyen_dan_su", "bonus-potion-chance-per-level"), 0.004));
        double chance = Math.min(0.50, chancePerLevel * Math.max(0, level - 1));
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            for (ItemStack item : event.getResults()) {
                if (item == null || item.getAmount() < 1) continue;
                Material type = item.getType();
                if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
                    ItemStack bonus = item.clone(); bonus.setAmount(1);
                    Map<Integer, ItemStack> leftovers = owner.getInventory().addItem(bonus);
                    for (ItemStack left : leftovers.values()) owner.getWorld().dropItemNaturally(owner.getLocation(), left);
                    owner.sendMessage(color("&d[Luyện Đan] May mắn nghề nghiệp: nhận thêm 1 bình thuốc!"));
                    break;
                }
            }
        }
    }

    private String locationKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        String id = profession(player);
        if (id.equalsIgnoreCase("luyen_khi_su")) {
            addXp(player, "enchant", "EnchantItemEvent");
            int discountEvery = Math.max(0, config.getInt(profPath(id, "discount-levels-per-level"), 10));
            if (discountEvery > 0) {
                int discount = level(player) / discountEvery;
                if (discount > 0) event.setExpLevelCost(Math.max(1, event.getExpLevelCost() - discount));
            }
        } else if (id.equalsIgnoreCase("tran_phap_su")) {
            addXp(player, "enchant", "EnchantItemEvent");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        if (!profession(player).equalsIgnoreCase("ngu_thu_su")) return;
        addXp(player, "tame", "EntityTameEvent");
        if (event.getEntity() instanceof LivingEntity living) {
            double heal = Math.max(0.0, config.getDouble(profPath("ngu_thu_su", "tame-heal"), 4.0));
            if (heal > 0) living.setHealth(Math.min(living.getMaxHealth(), living.getHealth() + heal));
            int duration = Math.max(0, config.getInt(profPath("ngu_thu_su", "tame-regeneration-seconds"), 8));
            if (duration > 0) living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration * 20, 0, true, true, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFormationBlockPlace(BlockPlaceEvent event) {
        if (!profession(event.getPlayer()).equalsIgnoreCase("tran_phap_su")) return;
        List<String> allowed = config.getStringList(profPath("tran_phap_su", "formation-materials"));
        if (allowed.isEmpty()) allowed = Arrays.asList("OBSIDIAN", "AMETHYST_BLOCK", "GLOWSTONE", "SEA_LANTERN");
        if (!allowed.contains(event.getBlockPlaced().getType().name())) return;
        addXp(event.getPlayer(), "formation-place", "BlockPlaceEvent");
        int level = level(event.getPlayer());
        double chance = Math.min(0.35, Math.max(0, level - 1) * config.getDouble(profPath("tran_phap_su", "barrier-chance-per-level"), 0.01));
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            int seconds = Math.max(1, config.getInt(profPath("tran_phap_su", "barrier-duration-seconds"), 3));
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 0, true, true, true));
            event.getPlayer().sendMessage(color("&b[Trận Pháp] Linh trận hộ thể kích hoạt!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!profession(event.getPlayer()).equalsIgnoreCase("linh_dien_su") || !isMatureCrop(block)) return;
        addXp(event.getPlayer(), "harvest", "BlockBreakEvent");
        ItemStack bonus = cropBonus(block.getType());
        if (bonus == null) return;
        double chancePerLevel = Math.max(0, config.getDouble(profPath("linh_dien_su", "bonus-harvest-chance-per-level"), 0.01));
        double chance = Math.min(0.50, Math.max(0, level(event.getPlayer()) - 1) * chancePerLevel);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            Map<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(bonus);
            leftovers.values().forEach(left -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), left));
            event.getPlayer().sendMessage(color("&a[Linh Điền] Thu hoạch linh điền được thêm " + bonus.getType().name() + "!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!profession(player).equalsIgnoreCase("phu_luc_su")) return;
        Material type = event.getRecipe().getResult().getType();
        if (type == Material.PAPER || type == Material.BOOK || type == Material.WRITABLE_BOOK || type == Material.ENCHANTED_BOOK) {
            addXp(player, "craft-scroll", "CraftItemEvent");
            double chance = Math.min(0.35, Math.max(0, level(player) - 1) * config.getDouble(profPath("phu_luc_su", "bonus-scroll-chance-per-level"), 0.005));
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                ItemStack extra = event.getRecipe().getResult().clone();
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(extra);
                leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                player.sendMessage(color("&d[Phù Lục] Linh cảm phù văn giúp tạo thêm một vật phẩm!"));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!config.getBoolean("settings.enabled", true)) {
            sender.sendMessage(color("&cBách Nghệ Tu Tiên đang được tắt trong cấu hình."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
                if (!sender.hasPermission("tutien.admin")) {
                    sender.sendMessage(color("&cBạn không có quyền."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(color("&cDùng: /bachnghe reset <tên-người-chơi>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&cNgười chơi phải đang online."));
                    return true;
                }
                resetProfession(target);
                sender.sendMessage(color("&aĐã reset nghề Bách Nghệ của " + target.getName() + "."));
                target.sendMessage(color("&eNghề Bách Nghệ của bạn đã được reset; dùng /bachnghe chon <id> để chọn lại."));
                return true;
            }
            sender.sendMessage("Lệnh này cần được dùng trong game.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("trangthai")) {
            sendStatus(player); return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list", "danh-sach", "danhsach" -> {
                player.sendMessage(color("&6&l══════ BÁCH NGHỆ TU TIÊN ══════"));
                for (String id : professionIds(true)) {
                    player.sendMessage(color("&e" + id + " &7- &f" + displayName(id) + " &8| &7" + config.getString(profPath(id, "description"), "Nghề tu tiên")));
                }
                player.sendMessage(color("&7Dùng &e/bachnghe info <id> &7để xem nghề."));
            }
            case "info", "thongtin" -> {
                if (args.length < 2) { player.sendMessage(color("&cDùng: /bachnghe info <id>")); return true; }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (!config.isConfigurationSection(profPath(id, "")) || !config.getBoolean(profPath(id, "enabled"), true)) {
                    player.sendMessage(color("&cKhông có nghề đó hoặc nghề đã bị tắt.")); return true;
                }
                player.sendMessage(color("&6&l" + displayName(id)));
                player.sendMessage(color("&7" + config.getString(profPath(id, "description"), "")));
                player.sendMessage(color("&7Cấp tối đa: &e" + maxLevel(id)));
                if (profession(player).equalsIgnoreCase(id)) player.sendMessage(color("&7Tiến độ: &eCấp " + level(player) + " &7(&f" + xp(player) + "/" + (level(player) >= maxLevel(id) ? "MAX" : xpRequired(id, level(player))) + " XP&7)"));
                player.sendMessage(color("&7Đặc quyền: &f" + config.getString(profPath(id, "perks"), "Tăng trưởng theo cấp nghề.")));
                if (profession(player).isBlank()) player.sendMessage(color("&aDùng &e/bachnghe chon " + id + " &ađể chọn nghề này."));
            }
            case "chon", "chọn", "choose", "select" -> {
                if (args.length < 2) { player.sendMessage(color("&cDùng: /bachnghe chon <id>")); return true; }
                if (!profession(player).isBlank()) { player.sendMessage(color("&cBạn đã chọn nghề &e" + displayName(profession(player)) + "&c. Hãy nhờ admin reset nếu muốn đổi nghề.")); return true; }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (!professionIds(true).contains(id)) { player.sendMessage(color("&cNghề không tồn tại. Dùng /bachnghe list.")); return true; }
                data.set(path(player.getUniqueId(), "profession"), id);
                data.set(path(player.getUniqueId(), "level"), 1);
                data.set(path(player.getUniqueId(), "xp"), 0);
                save();
                player.sendMessage(color("&aBạn đã trở thành &e" + displayName(id) + "&a!"));
                player.sendMessage(color("&7" + config.getString(profPath(id, "description"), "")));
            }
            case "reset" -> {
                if (!player.hasPermission("tutien.admin")) { player.sendMessage(color("&cBạn không có quyền.")); return true; }
                if (args.length < 2) { player.sendMessage(color("&cDùng: /bachnghe reset <tên-người-chơi>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(color("&cNgười chơi phải đang online.")); return true; }
                resetProfession(target);
                player.sendMessage(color("&aĐã reset nghề Bách Nghệ của " + target.getName() + "."));
                target.sendMessage(color("&eNghề Bách Nghệ của bạn đã được reset; dùng /bachnghe chon <id> để chọn lại."));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void resetProfession(Player target) {
        UUID uuid = target.getUniqueId();
        data.set(path(uuid, "profession"), null);
        data.set(path(uuid, "level"), null);
        data.set(path(uuid, "xp"), null);
        data.set(path(uuid, "cooldowns"), null);
        save();
    }

    private void sendStatus(Player player) {
        player.sendMessage(color("&6&l══════ BÁCH NGHỆ TU TIÊN ══════"));
        String id = profession(player);
        if (id.isBlank()) {
            player.sendMessage(color("&7Bạn chưa chọn nghề phụ. Dùng &e/bachnghe list&7 rồi &e/bachnghe chon <id>"));
            return;
        }
        int lvl = level(player);
        int max = maxLevel(id);
        player.sendMessage(color("&7Nghề: &e" + displayName(id)));
        player.sendMessage(color("&7Cấp: &e" + lvl + "/" + max));
        player.sendMessage(color("&7Kinh nghiệm: &b" + xp(player) + "/" + (lvl >= max ? "MAX" : xpRequired(id, lvl))));
        player.sendMessage(color("&7Đặc quyền: &f" + config.getString(profPath(id, "perks"), "Tăng trưởng theo cấp nghề.")));
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&6Bách Nghệ: &e/bachnghe list &7| &e/bachnghe info <id> &7| &e/bachnghe chon <id> &7| &e/bachnghe status"));
        if (player.hasPermission("tutien.admin")) player.sendMessage(color("&c/bachnghe reset <player> &7- reset nghề người chơi"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("list", "info", "chon", "status"));
            if (sender.hasPermission("tutien.admin")) list.add("reset");
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        if (args.length == 2 && Arrays.asList("info", "chon", "chọn", "choose", "select").contains(args[0].toLowerCase(Locale.ROOT))) {
            return professionIds(true).stream().filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("tutien.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
