package com.tutien.coduyen;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Random cultivation opportunities, persistent discoveries and tagged reward items. */
public final class CoDuyenManager implements Listener, CommandExecutor, TabCompleter {
    private final TuTienPlugin plugin;
    private final File configFile;
    private final File dataFile;
    private final NamespacedKey itemKey;
    private YamlConfiguration config;
    private YamlConfiguration data;

    public CoDuyenManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "co-duyen-kim-thu-chi.yml");
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists() && !dataDir.mkdirs()) plugin.getLogger().warning("Không tạo được thư mục dữ liệu Cơ Duyên.");
        this.dataFile = new File(dataDir, "co-duyen-data.yml");
        this.itemKey = new NamespacedKey(plugin, "co_duyen_item");
        reload();
    }

    public void reload() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            data = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection pool = config.getConfigurationSection("opportunities");
            plugin.getLogger().info("Đã nạp Cơ Duyên/Kim Thủ Chỉ: " + (pool == null ? 0 : pool.getKeys(false).size()) + " cơ duyên.");
        } catch (Exception ex) {
            plugin.getLogger().severe("Không nạp được cấu hình Cơ Duyên: " + ex.getMessage());
            if (config == null) config = new YamlConfiguration();
            if (data == null) data = new YamlConfiguration();
        }
    }

    public void save() {
        if (data == null) return;
        try { data.save(dataFile); }
        catch (IOException ex) { plugin.getLogger().severe("Không lưu được dữ liệu Cơ Duyên: " + ex.getMessage()); }
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
    private String base(UUID uuid) { return "players." + uuid; }
    private String discoveredPath(UUID uuid, String id) { return base(uuid) + ".discovered." + id; }
    private int points(Player player) { return Math.max(0, data.getInt(base(player.getUniqueId()) + ".system-points", 0)); }
    private int discoveries(Player player) { return Math.max(0, data.getInt(base(player.getUniqueId()) + ".discovery-count", 0)); }
    private boolean hasDiscovered(Player player, String id) { return data.getBoolean(discoveredPath(player.getUniqueId(), id), false); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!config.getBoolean("settings.enabled", true)) return;
        if (!player.hasPlayedBefore() && config.getBoolean("triggers.first-join.enabled", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) tryDiscover(player, "first-join");
            }, Math.max(1L, config.getLong("settings.first-join-delay-ticks", 40L)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRareBlockBreak(BlockBreakEvent event) {
        if (!config.getBoolean("triggers.rare-block-break.enabled", true)) return;
        List<String> allowed = config.getStringList("triggers.rare-block-break.materials");
        if (allowed.isEmpty()) allowed = Arrays.asList("DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "EMERALD_ORE", "ANCIENT_DEBRIS");
        if (allowed.contains(event.getBlock().getType().name())) tryDiscover(event.getPlayer(), "rare-block-break");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        EntityType type = event.getEntity().getType();
        boolean boss = type == EntityType.WITHER || type == EntityType.ENDER_DRAGON || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN;
        String trigger = boss ? "boss-kill" : "mob-kill";
        if (config.getBoolean("triggers." + trigger + ".enabled", true)) tryDiscover(killer, trigger);
    }

    /** Called by the breakthrough manager after a successful cultivation breakthrough. */
    public void rollForBreakthrough(Player player) {
        if (config.getBoolean("triggers.breakthrough.enabled", true)) tryDiscover(player, "breakthrough");
    }

    private void tryDiscover(Player player, String trigger) {
        if (!config.getBoolean("settings.enabled", true) || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        String basePath = base(uuid);
        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0L, config.getLong("settings.global-roll-cooldown-seconds", 600L)) * 1000L;
        long last = data.getLong(basePath + ".last-roll", 0L);
        if (now - last < cooldownMs) return;
        if (discoveries(player) >= Math.max(1, config.getInt("settings.max-discoveries-per-player", 4))) return;

        double chance = config.getDouble("triggers." + trigger + ".chance-percent", 0.0);
        chance = Math.max(0.0, Math.min(100.0, chance));
        data.set(basePath + ".last-roll", now);
        save();
        if (ThreadLocalRandom.current().nextDouble(100.0) >= chance) return;

        String selected = selectOpportunity(player);
        if (selected == null) return;
        data.set(discoveredPath(uuid, selected), true);
        data.set(basePath + ".discovery-count", discoveries(player) + 1);
        applyOpportunity(player, selected);
        save();
    }

    private String selectOpportunity(Player player) {
        ConfigurationSection section = config.getConfigurationSection("opportunities");
        if (section == null) return null;
        List<String> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (String id : section.getKeys(false)) {
            String path = "opportunities." + id;
            if (!config.getBoolean(path + ".enabled", true)) continue;
            if (config.getBoolean(path + ".one-time-per-player", true) && hasDiscovered(player, id)) continue;
            int weight = Math.max(0, config.getInt(path + ".weight", 1));
            if (weight <= 0) continue;
            totalWeight += weight;
            eligible.add(id);
        }
        if (eligible.isEmpty() || totalWeight <= 0) return null;
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (String id : eligible) {
            roll -= Math.max(0, config.getInt("opportunities." + id + ".weight", 1));
            if (roll < 0) return id;
        }
        return eligible.get(eligible.size() - 1);
    }

    private void applyOpportunity(Player player, String id) {
        String path = "opportunities." + id;
        String title = config.getString(path + ".name", id);
        String message = config.getString(path + ".message", "&dBạn đã gặp một cơ duyên: &f%name%&d!").replace("%name%", title);
        player.sendTitle(color(config.getString("messages.title", "&d&lCƠ DUYÊN XUẤT THẾ")), color(title), 10, 60, 20);
        player.sendMessage(color(message));
        String type = config.getString(path + ".reward.type", id).toLowerCase(Locale.ROOT);
        if (type.equals("system") || type.equals("he-thong")) {
            int amount = Math.max(0, config.getInt(path + ".reward.points", 3));
            String key = base(player.getUniqueId()) + ".system-points";
            data.set(key, Math.min(1_000_000, data.getInt(key, 0) + amount));
            player.sendMessage(color("&b[Hệ Thống] &fBạn nhận được &e" + amount + " &bđiểm hệ thống. Dùng /coduyen dung linhkhi <điểm>."));
            return;
        }
        ItemStack item = createOpportunityItem(id, type);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.sendMessage(color("&7Vật phẩm cơ duyên đã được đưa vào túi đồ."));
    }

    private ItemStack createOpportunityItem(String id, String type) {
        String path = "opportunities." + id + ".item";
        Material material = Material.matchMaterial(config.getString(path + ".material", "ECHO_SHARD"));
        if (material == null || material.isAir()) material = Material.ECHO_SHARD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(config.getString(path + ".name", config.getString("opportunities." + id + ".name", "Cơ Duyên"))));
            List<String> lore = config.getStringList(path + ".lore").stream().map(this::color).collect(Collectors.toList());
            if (lore.isEmpty()) lore = List.of(color("&7Vật phẩm được đánh dấu bởi Tu Tiên."), color("&eChuột phải để kích hoạt."));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUseOpportunityItem(PlayerInteractEvent event) {
        if (!config.getBoolean("settings.enabled", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta()) return;
        ItemMeta meta = held.getItemMeta();
        String opportunityId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (opportunityId == null || opportunityId.isBlank()) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        // Consume exactly one tagged item, without letting the base material's vanilla action run.
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else { held.setAmount(held.getAmount() - 1); player.getInventory().setItemInMainHand(held); }
        String type = config.getString("opportunities." + opportunityId + ".reward.type", opportunityId).toLowerCase(Locale.ROOT);
        String rewardPath = config.isConfigurationSection("opportunities." + opportunityId + ".consume-reward")
                ? "opportunities." + opportunityId + ".consume-reward" : "rewards.items." + type;
        long linhKhi = Math.max(0L, config.getLong(rewardPath + ".linh-khi", config.getLong("rewards.items." + type + ".linh-khi", 0L)));
        if (linhKhi > 0) {
            PlayerData pd = plugin.getDataManager().getOrCreate(player);
            pd.addLinhKhi(linhKhi);
            plugin.getDataManager().save(pd);
        }
        double heal = Math.max(0.0, config.getDouble(rewardPath + ".heal", 0.0));
        if (heal > 0) {
            double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null ? 20.0 : player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(max, player.getHealth() + heal));
        }
        int buffSeconds = Math.max(0, config.getInt(rewardPath + ".buff-seconds", 0));
        if (buffSeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, buffSeconds * 20, Math.max(0, config.getInt(rewardPath + ".strength-amplifier", 0)), true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, buffSeconds * 20, 0, true, true, true));
        }
        int points = Math.max(0, config.getInt(rewardPath + ".system-points", 0));
        if (points > 0) {
            String pointsPath = base(player.getUniqueId()) + ".system-points";
            data.set(pointsPath, Math.min(1_000_000, data.getInt(pointsPath, 0) + points));
            save();
        }
        player.sendMessage(color(config.getString(rewardPath + ".success-message", "&dCơ duyên đã dung nhập vào cơ thể!")));
    }


    private void addLinhKhi(Player player, long amount) {
        PlayerData pd = plugin.getDataManager().getOrCreate(player);
        pd.addLinhKhi(Math.max(0L, amount));
        plugin.getDataManager().save(pd);
    }

    private void showStatus(Player player) {
        player.sendMessage(color("&d&l══════ CƠ DUYÊN & KIM THỦ CHỈ ══════"));
        player.sendMessage(color("&7Điểm Hệ Thống: &b" + points(player)));
        player.sendMessage(color("&7Cơ duyên đã khám phá: &e" + discoveries(player) + "/" + Math.max(1, config.getInt("settings.max-discoveries-per-player", 4))));
        ConfigurationSection section = config.getConfigurationSection("opportunities");
        if (section != null) {
            List<String> found = new ArrayList<>();
            for (String id : section.getKeys(false)) if (hasDiscovered(player, id)) found.add(config.getString("opportunities." + id + ".name", id));
            player.sendMessage(color("&7Đã gặp: &f" + (found.isEmpty() ? "Chưa có" : String.join("&7, &f", found))));
        }
        player.sendMessage(color("&7Dùng &e/coduyen dung <linhkhi|hoiphuc|cuonghoa> [số điểm]&7."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Lệnh này cần được dùng trong game."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("trangthai") || args[0].equalsIgnoreCase("info")) {
            showStatus(player); return true;
        }
        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("giup")) {
            player.sendMessage(color("&dCơ Duyên: &e/coduyen &7| &e/coduyen dung linhkhi <điểm> &7| &e/coduyen dung hoiphuc &7| &e/coduyen dung cuonghoa"));
            return true;
        }
        if (args[0].equalsIgnoreCase("dung") || args[0].equalsIgnoreCase("use")) {
            if (args.length < 2) { player.sendMessage(color("&cDùng: /coduyen dung <linhkhi|hoiphuc|cuonghoa> [số điểm]")); return true; }
            String option = args[1].toLowerCase(Locale.ROOT);
            int cost = option.equals("linhkhi") ? (args.length >= 3 ? parsePositive(args[2]) : 1) : 1;
            if (cost <= 0 || cost > 64) { player.sendMessage(color("&cSố điểm phải từ 1 đến 64.")); return true; }
            if (points(player) < cost) { player.sendMessage(color("&cBạn không đủ điểm hệ thống. Hiện có: &e" + points(player))); return true; }
            if (!Arrays.asList("linhkhi", "hoiphuc", "cuonghoa").contains(option)) { player.sendMessage(color("&cLựa chọn không hợp lệ.")); return true; }
            data.set(base(player.getUniqueId()) + ".system-points", points(player) - cost);
            save();
            switch (option) {
                case "linhkhi" -> {
                    long perPoint = Math.max(1L, config.getLong("rewards.system.linh-khi-per-point", 1000L));
                    addLinhKhi(player, perPoint * cost);
                    player.sendMessage(color("&bHệ Thống: &aĐổi " + cost + " điểm thành " + (perPoint * cost) + " Linh Khí."));
                }
                case "hoiphuc" -> {
                    double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null ? 20.0 : player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(max);
                    player.setFoodLevel(20); player.setSaturation(10.0f);
                    for (PotionEffectType bad : Arrays.asList(PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER)) player.removePotionEffect(bad);
                    player.sendMessage(color("&bHệ Thống: &aĐã hồi phục sinh lực và giải trừ một số trạng thái bất lợi."));
                }
                case "cuonghoa" -> {
                    int seconds = Math.max(1, config.getInt("rewards.system.buff-seconds", 120));
                    int strength = Math.max(0, config.getInt("rewards.system.strength-amplifier", 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, seconds * 20, strength, true, true, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 0, true, true, true));
                    player.sendMessage(color("&bHệ Thống: &aCường hóa trong " + seconds + " giây."));
                }
            }
            return true;
        }
        player.sendMessage(color("&dDùng: /coduyen [status|help|dung ...]"));
        return true;
    }

    private int parsePositive(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("status", "help", "dung").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        if (args.length == 2 && (args[0].equalsIgnoreCase("dung") || args[0].equalsIgnoreCase("use"))) return Arrays.asList("linhkhi", "hoiphuc", "cuonghoa").stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("dung") && args[1].equalsIgnoreCase("linhkhi")) return Arrays.asList("1", "2", "5", "10").stream().filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
