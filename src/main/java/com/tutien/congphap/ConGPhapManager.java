package com.tutien.congphap;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** YAML-driven, server-customizable cultivation techniques. */
public final class ConGPhapManager implements CommandExecutor, TabCompleter {
    private static final String EXTRA_EQUIPPED = "congphap.equipped";
    private static final String EXTRA_LEARNED = "congphap.learned";
    private static final String EXTRA_CD_PREFIX = "congphap.cd.";
    private final TuTienPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private final Map<String, Technique> techniques = new LinkedHashMap<>();

    public ConGPhapManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "congphap.yml");
        reload();
    }

    public synchronized void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
        techniques.clear();
        ConfigurationSection section = yaml.getConfigurationSection("techniques");
        if (section == null) return;
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(rawId);
            if (s == null || !s.getBoolean("enabled", true)) continue;
            String id = normalizeId(rawId);
            if (id == null) {
                plugin.getLogger().warning("[Công Pháp] Bỏ qua ID không hợp lệ: " + rawId);
                continue;
            }
            String stageName = s.getString("minimum-stage", "LUYEN_KHI").toUpperCase(Locale.ROOT);
            CultivationStage stage;
            try { stage = CultivationStage.valueOf(stageName); }
            catch (IllegalArgumentException ex) { stage = CultivationStage.LUYEN_KHI; }
            String activeType = s.getString("active-type", "NONE").toUpperCase(Locale.ROOT);
            if (!Set.of("NONE", "DAMAGE", "AOE_DAMAGE", "MARK", "DETONATE_MARK", "HEAL", "BUFF").contains(activeType)) {
                plugin.getLogger().warning("[Công Pháp] active-type không hợp lệ ở " + rawId + ": " + activeType + " — chuyển thành NONE.");
                activeType = "NONE";
            }
            Technique t = new Technique(
                    id,
                    s.getString("display-name", id),
                    s.getString("description", "Công pháp tùy chỉnh."),
                    stage,
                    Math.max(1, Math.min(20, s.getInt("minimum-rank", 1))),
                    Math.max(0, s.getLong("learn-cost", 0)),
                    bounded(s.getDouble("attack-multiplier", 1.0), 0.1, 5.0, 1.0),
                    bounded(s.getDouble("defense-multiplier", 1.0), 0.1, 5.0, 1.0),
                    bounded(s.getDouble("linh-khi-multiplier", 1.0), 0.1, 10.0, 1.0),
                    Math.max(1, Math.min(3600, s.getInt("cooldown-seconds", 15))),
                    activeType,
                    bounded(s.getDouble("active-damage", 6.0), 0.0, 2048.0, 6.0),
                    bounded(s.getDouble("radius", 5.0), 1.0, 32.0, 5.0),
                    Math.max(1, Math.min(1200, s.getInt("duration-ticks", 100))),
                    s.getString("effect-id", "").trim());
            techniques.put(id, t);
        }
        plugin.getLogger().info("[Công Pháp] Đã nạp " + techniques.size() + " công pháp từ congphap.yml.");
    }

    private static String normalizeId(String raw) {
        if (raw == null) return null;
        String id = raw.toLowerCase(Locale.ROOT).trim();
        return id.matches("[a-z0-9][a-z0-9_-]{1,39}") ? id : null;
    }

    private static double bounded(double v, double min, double max, double fallback) {
        if (!Double.isFinite(v)) return fallback;
        return Math.max(min, Math.min(max, v));
    }

    public Collection<Technique> getTechniques() { return Collections.unmodifiableCollection(techniques.values()); }
    public Technique get(String id) { return id == null ? null : techniques.get(id.toLowerCase(Locale.ROOT)); }

    public double getAttackMultiplier(PlayerData data) {
        if (data != null && !data.isModuleEnabled("cong_phap")) return 1.0;
        Technique t = getEquipped(data);
        return t == null ? 1.0 : t.attackMultiplier;
    }
    public double getDefenseMultiplier(PlayerData data) {
        if (data != null && !data.isModuleEnabled("cong_phap")) return 1.0;
        Technique t = getEquipped(data);
        return t == null ? 1.0 : t.defenseMultiplier;
    }
    public double getLinhKhiMultiplier(PlayerData data) {
        if (data != null && !data.isModuleEnabled("cong_phap")) return 1.0;
        Technique t = getEquipped(data);
        return t == null ? 1.0 : t.linhKhiMultiplier;
    }
    public Technique getEquipped(PlayerData data) {
        if (data == null) return null;
        return get(data.getExtraStr(EXTRA_EQUIPPED, ""));
    }

    private List<String> learned(PlayerData data) {
        Object raw = data.getExtraData().get(EXTRA_LEARNED);
        List<String> out = new ArrayList<>();
        if (raw instanceof Collection<?> c) {
            for (Object value : c) if (value instanceof String s && get(s) != null && !out.contains(s)) out.add(s);
        } else if (raw instanceof String s && !s.isBlank()) {
            for (String token : s.split(",")) if (get(token) != null && !out.contains(token)) out.add(token);
        }
        return out;
    }

    private void saveLearned(PlayerData data, Collection<String> learned) {
        data.getExtraData().put(EXTRA_LEARNED, new ArrayList<>(learned));
        plugin.getDataManager().save(data);
    }

    private boolean meetsRequirement(Player player, PlayerData data, Technique t) {
        if (data.getStage().ordinal() < t.minimumStage.ordinal()
                || (data.getStage() == t.minimumStage && data.getRank() < t.minimumRank)) {
            player.sendMessage(plugin.prefix() + "§cCông pháp §e" + t.displayName + " §ccần "
                    + t.minimumStage.getDisplayName() + " rank " + t.minimumRank + " trở lên.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) { reload(); sender.sendMessage("Reloaded congphap.yml"); return true; }
            sender.sendMessage("Lệnh này cần người chơi hoặc /congphap reload từ console.");
            return true;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return true;
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) { showList(player, data); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§e/congphap info <id>"); return true; }
                Technique t = get(args[1]);
                if (t == null) { player.sendMessage(plugin.prefix() + "§cKhông tìm thấy công pháp."); return true; }
                showInfo(player, data, t); return true;
            }
            case "hoc" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§e/congphap hoc <id>"); return true; }
                Technique t = get(args[1]);
                if (t == null) { player.sendMessage(plugin.prefix() + "§cKhông tìm thấy công pháp."); return true; }
                if (!meetsRequirement(player, data, t)) return true;
                List<String> learned = learned(data);
                if (learned.contains(t.id)) { player.sendMessage(plugin.prefix() + "§eBạn đã học công pháp này rồi."); return true; }
                if (t.learnCost > 0 && !data.spendTien(t.learnCost)) {
                    player.sendMessage(plugin.prefix() + "§cKhông đủ " + plugin.getTienManager().tenTien() + " để học. Cần §e" + t.learnCost + "§c."); return true;
                }
                learned.add(t.id); saveLearned(data, learned);
                player.sendMessage(plugin.prefix() + "§aĐã học công pháp §e" + t.displayName + "§a.");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.9f, 1.2f);
                return true;
            }
            case "trangbi", "equip" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§e/congphap trangbi <id>"); return true; }
                Technique t = get(args[1]);
                if (t == null || !learned(data).contains(t.id)) { player.sendMessage(plugin.prefix() + "§cBạn chưa học công pháp này."); return true; }
                if (!meetsRequirement(player, data, t)) return true;
                data.setExtraStr(EXTRA_EQUIPPED, t.id); plugin.getDataManager().save(data);
                player.sendMessage(plugin.prefix() + "§aĐã vận công §e" + t.displayName + "§a. ATK ×" + t.attackMultiplier
                        + " · DEF ×" + t.defenseMultiplier + " · Linh Khí ×" + t.linhKhiMultiplier);
                return true;
            }
            case "bo", "unequip" -> {
                data.setExtraStr(EXTRA_EQUIPPED, ""); plugin.getDataManager().save(data);
                player.sendMessage(plugin.prefix() + "§7Bạn đã thu công pháp đang vận công."); return true;
            }
            case "thi-trien", "cast", "dung" -> {
                int comboStage = 1;
                if (args.length >= 2) {
                    try { comboStage = Integer.parseInt(args[1]); }
                    catch (NumberFormatException ignored) { comboStage = -1; }
                }
                if (comboStage < 1 || comboStage > 3) { player.sendMessage(plugin.prefix() + "§e/congphap thi-trien [1|2|3]"); return true; }
                cast(player, data, comboStage); return true;
            }
            case "reload" -> {
                if (!player.hasPermission("tutien.admin")) { player.sendMessage(plugin.prefix() + "§cBạn không có quyền."); return true; }
                reload(); player.sendMessage(plugin.prefix() + "§aĐã nạp lại congphap.yml."); return true;
            }
            case "tao" -> {
                if (!player.hasPermission("tutien.admin")) { player.sendMessage(plugin.prefix() + "§cChỉ Admin được tạo mẫu công pháp."); return true; }
                if (args.length < 3) { player.sendMessage(plugin.prefix() + "§e/congphap tao <id> <tên hiển thị>"); return true; }
                String id = normalizeId(args[1]);
                if (id == null || yaml.contains("techniques." + id)) { player.sendMessage(plugin.prefix() + "§cID sai hoặc đã tồn tại."); return true; }
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String path = "techniques." + id;
                yaml.set(path + ".enabled", true); yaml.set(path + ".display-name", name);
                yaml.set(path + ".description", "Công pháp mới của máy chủ."); yaml.set(path + ".minimum-stage", "LUYEN_KHI");
                yaml.set(path + ".minimum-rank", 1); yaml.set(path + ".learn-cost", 0L);
                yaml.set(path + ".attack-multiplier", 1.0); yaml.set(path + ".defense-multiplier", 1.0);
                yaml.set(path + ".linh-khi-multiplier", 1.0); yaml.set(path + ".active-type", "NONE");
                yaml.set(path + ".cooldown-seconds", 15); yaml.set(path + ".active-damage", 6.0); yaml.set(path + ".radius", 5.0);
                try { yaml.save(file); reload(); player.sendMessage(plugin.prefix() + "§aĐã tạo " + name + ". Hãy chỉnh thông số trong congphap.yml."); }
                catch (IOException ex) { player.sendMessage(plugin.prefix() + "§cKhông thể lưu congphap.yml."); }
                return true;
            }
            default -> {
                player.sendMessage("§6§lCÔNG PHÁP");
                player.sendMessage("§e/congphap list §7— danh sách công pháp");
                player.sendMessage("§e/congphap info <id> §7— xem thông tin");
                player.sendMessage("§e/congphap hoc <id> §7— học công pháp");
                player.sendMessage("§e/congphap trangbi <id> §7— vận công");
                player.sendMessage("§e/congphap thi-trien [1|2|3] §7— dùng thức chủ động; Huyết Ngục Ấn: 1 đóng ấn, 2/3 kích nổ");
                player.sendMessage("§e/congphap bo §7— thu công");
                if (player.hasPermission("tutien.admin")) player.sendMessage("§c/congphap tao|reload ... §7— quản lý YAML");
                return true;
            }
        }
    }

    private void showList(Player player, PlayerData data) {
        player.sendMessage("§6§l✦ CÔNG PHÁP §8— §7Đã học " + learned(data).size() + "/" + techniques.size());
        if (techniques.isEmpty()) { player.sendMessage("§7Chưa có công pháp. Admin hãy chỉnh plugins/TuTienPlugin/congphap.yml."); return; }
        for (Technique t : techniques.values()) {
            boolean has = learned(data).contains(t.id);
            boolean equipped = t.id.equals(data.getExtraStr(EXTRA_EQUIPPED, ""));
            player.sendMessage((equipped ? "§a▶ " : has ? "§e• " : "§8• ") + "§f" + t.displayName + " §8[§7" + t.id + "§8]"
                    + (has ? " §aĐã học" : " §7Giá: " + t.learnCost));
            player.sendMessage("  §7" + t.description);
        }
        Technique equipped = getEquipped(data);
        player.sendMessage("§7Đang vận công: " + (equipped == null ? "§8Không có" : "§a" + equipped.displayName));
    }

    private void showInfo(Player player, PlayerData data, Technique t) {
        player.sendMessage("§6§l✦ " + t.displayName + " §8(" + t.id + ")");
        player.sendMessage("§7" + t.description);
        player.sendMessage("§7Yêu cầu: §e" + t.minimumStage.getDisplayName() + " rank " + t.minimumRank);
        player.sendMessage("§7ATK ×" + t.attackMultiplier + " · DEF ×" + t.defenseMultiplier + " · Linh Khí ×" + t.linhKhiMultiplier);
        player.sendMessage("§7Thức chủ động: §e" + t.activeType + " §7| Hồi chiêu: §e" + t.cooldownSeconds + "s");
        player.sendMessage(learned(data).contains(t.id) ? "§aĐã học" : "§7Chưa học — /congphap hoc " + t.id);
    }

    private void cast(Player player, PlayerData data, int comboStage) {
        if (!data.isModuleEnabled("cong_phap")) { player.sendMessage(plugin.prefix() + "§cBạn đã tắt hệ thống Công Pháp trong /kinang."); return; }
        Technique t = getEquipped(data);
        if (t == null) { player.sendMessage(plugin.prefix() + "§cBạn chưa vận công pháp nào."); return; }
        if (!meetsRequirement(player, data, t)) return;
        if ("NONE".equals(t.activeType)) { player.sendMessage(plugin.prefix() + "§7Công pháp này không có thức chủ động. Hãy xem congphap.yml."); return; }
        long now = System.currentTimeMillis();
        boolean markCombo = "MARK".equals(t.activeType);
        if (!markCombo && comboStage != 1) { player.sendMessage(plugin.prefix() + "§cCông pháp này không có combo 1/2/3."); return; }
        String cooldownKey = EXTRA_CD_PREFIX + t.id + (markCombo ? "." + comboStage : "");
        long until = data.getExtraLong(cooldownKey, 0L);
        if (until > now) { player.sendMessage(plugin.prefix() + "§eThức còn hồi chiêu " + ((until - now + 999) / 1000) + "s."); return; }
        List<LivingEntity> targets = new ArrayList<>();
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(t.radius, t.radius, t.radius)) {
            if (e instanceof LivingEntity living && e != player && !(e instanceof org.bukkit.entity.ArmorStand)) targets.add(living);
        }
        targets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));
        String type = t.activeType;
        switch (type) {
            case "DAMAGE", "AOE_DAMAGE", "DETONATE_MARK" -> {
                if (targets.isEmpty()) { player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu trong phạm vi."); return; }
                int hit = 0;
                for (LivingEntity target : targets) {
                    if ("DAMAGE".equals(type) && hit > 0) break;
                    if ("DETONATE_MARK".equals(type) && !hasMarkFrom(target, player)) continue;
                    if ("DETONATE_MARK".equals(type)) removeMarksFrom(target, player);
                    double damage = t.activeDamage * data.calculateDamageMultiplier();
                    if ("DETONATE_MARK".equals(type)) damage *= 1.8;
                    if (target instanceof Player tp) {
                        PlayerData targetData = plugin.getDataManager().get(tp);
                        if (targetData != null && targetData.getDaoPath() != null) damage /= Math.max(0.1, targetData.calculateDefenseMultiplier());
                    }
                    target.damage(Math.max(0.0, damage), player);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 16, 0.4, 0.5, 0.4, 0.1);
                    hit++;
                }
                player.sendMessage(plugin.prefix() + "§d" + t.displayName + " §7đánh trúng §e" + hit + " §7mục tiêu.");
                if (hit == 0) { return; }
            }
            case "MARK" -> {
                if (comboStage == 1) {
                    if (targets.isEmpty()) { player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu để đóng huyết ấn trong phạm vi."); return; }
                    LivingEntity target = targets.get(0);
                    removeMarksFrom(target, player);
                    String tag = markTagPrefix(player) + System.currentTimeMillis();
                    target.addScoreboardTag(tag);
                    // A unique token prevents an older expiry timer from clearing a refreshed mark.
                    Bukkit.getScheduler().runTaskLater(plugin, () -> { if (target.isValid()) target.removeScoreboardTag(tag); }, t.durationTicks);
                    Location markAt = target.getLocation().add(0, 1, 0);
                    target.getWorld().spawnParticle(Particle.SOUL, markAt, 28, 0.45, 0.65, 0.45, 0.025);
                    target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, markAt, 18, 0.4, 0.5, 0.4, 0.015);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.7f);
                    player.sendMessage(plugin.prefix() + "§5Huyết Ngục Ấn I: đã đóng ấn lên §e" + (target instanceof Player tp ? tp.getName() : target.getType().name())
                            + "§5. Dùng §e/congphap thi-trien 2 §5hoặc §e3 §5để kích nổ toàn bộ mục tiêu bị đánh dấu trong phạm vi.");
                } else {
                    double comboMultiplier = comboStage == 3 ? 2.8 : 1.8;
                    int hit = 0;
                    for (LivingEntity target : targets) {
                        if (!hasMarkFrom(target, player)) continue;
                        removeMarksFrom(target, player);
                        double damage = Math.max(0.0, t.activeDamage * comboMultiplier * data.calculateDamageMultiplier());
                        if (target instanceof Player tp) {
                            PlayerData targetData = plugin.getDataManager().get(tp);
                            if (targetData != null) damage /= Math.max(0.1, targetData.calculateDefenseMultiplier());
                        }
                        target.damage(damage, player);
                        Location blast = target.getLocation().add(0, 1, 0);
                        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, blast, 32, 0.65, 0.75, 0.65, 0.045);
                        target.getWorld().spawnParticle(Particle.CRIT, blast, 28, 0.55, 0.55, 0.55, 0.2);
                        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, blast, 24, 0.6, 0.6, 0.6, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, comboStage == 3 ? 0.65f : 0.9f);
                        hit++;
                    }
                    if (hit == 0) { player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu mang Huyết Ấn của bạn trong phạm vi."); return; }
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 1.0, 0.7, 1.0, 0.035);
                    player.sendMessage(plugin.prefix() + "§cHuyết Ngục Ấn " + (comboStage == 3 ? "III" : "II") + "! §7Kích nổ §e" + hit + " §7dấu ấn với hệ số ×" + comboMultiplier + ".");
                }
            }
            case "HEAL" -> {
                double heal = Math.max(1.0, player.getMaxHealth() * Math.min(1.0, t.activeDamage / 100.0));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 22, 0.6, 0.7, 0.6, 0.1);
            }
            case "BUFF" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, t.durationTicks, Math.max(0, Math.min(4, (int) t.activeDamage - 1)), false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, t.durationTicks, 0, false, true, true));
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.7, 0.8, 0.7, 0.2);
            }
            default -> { player.sendMessage(plugin.prefix() + "§cactive-type không được hỗ trợ: " + type); return; }
        }
        int cooldownSeconds = markCombo && comboStage == 1 ? 1 : t.cooldownSeconds;
        data.setExtraLong(cooldownKey, now + cooldownSeconds * 1000L);
        plugin.getDataManager().save(data);
        if (!t.effectId.isBlank() && plugin.getItemsAdderEffectHook() != null) plugin.getItemsAdderEffectHook().tryPlay(t.effectId, player.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f);
    }

    private String markTagPrefix(Player player) {
        return "ttm_" + player.getUniqueId().toString().replace("-", "") + "_";
    }

    private boolean hasMarkFrom(LivingEntity target, Player caster) {
        String prefix = markTagPrefix(caster);
        return target.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith(prefix));
    }

    private void removeMarksFrom(LivingEntity target, Player caster) {
        String prefix = markTagPrefix(caster);
        for (String tag : new ArrayList<>(target.getScoreboardTags())) if (tag.startsWith(prefix)) target.removeScoreboardTag(tag);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("list", "info", "hoc", "trangbi", "thi-trien", "bo"));
            if (sender.hasPermission("tutien.admin")) base.addAll(List.of("tao", "reload"));
            return match(base, args[0]);
        }
        if (args.length == 2 && List.of("info", "hoc", "trangbi").contains(args[0].toLowerCase(Locale.ROOT))) return match(techniques.keySet(), args[1]);
        if (args.length == 2 && List.of("thi-trien", "cast", "dung").contains(args[0].toLowerCase(Locale.ROOT))) return match(List.of("1", "2", "3"), args[1]);
        return Collections.emptyList();
    }

    private List<String> match(Collection<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList();
    }

    public static final class Technique {
        public final String id, displayName, description, activeType, effectId;
        public final CultivationStage minimumStage;
        public final int minimumRank, cooldownSeconds, durationTicks;
        public final long learnCost;
        public final double attackMultiplier, defenseMultiplier, linhKhiMultiplier, activeDamage, radius;
        private Technique(String id, String displayName, String description, CultivationStage stage, int rank,
                          long cost, double atk, double def, double lk, int cooldown, String activeType,
                          double damage, double radius, int durationTicks, String effectId) {
            this.id = id; this.displayName = displayName; this.description = description; this.minimumStage = stage;
            this.minimumRank = rank; this.learnCost = cost; this.attackMultiplier = atk; this.defenseMultiplier = def;
            this.linhKhiMultiplier = lk; this.cooldownSeconds = cooldown; this.activeType = activeType;
            this.activeDamage = damage; this.radius = radius; this.durationTicks = durationTicks; this.effectId = effectId;
        }
    }
}
