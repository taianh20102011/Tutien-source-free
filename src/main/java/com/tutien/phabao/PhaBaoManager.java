package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import com.tutien.effects.EffectEngine;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PhaBaoManager {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    // NBT keys de nhan biet phap bao ma khong can parse ten
    private static final String KEY_TIER = "pb_tier";
    private static final String KEY_TYPE = "pb_type";

    // Cooldown key prefix trong PlayerData buff map
    private static final String CD_PREFIX = "phabao_cd_";
    public  static final String PB_TAG    = "phabao:";   // NBT-like: ten item bat dau bang tag nay

    public PhaBaoManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  TAO ITEM PHAP BAO
    // ========================

    public ItemStack createPhaBao(PhaBaoType type, PhaBaoTier tier) {
        ItemStack item = resolveItemsAdderItem(type.getItemsAdderId());
        if (item == null) item = new ItemStack(type.getMaterial());
        else item = item.clone();
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        // Ten item - co TAG de nhan biet
        String name = tier.getColor() + tier.getIcon() + " ["
            + tier.getDisplayName() + "] " + type.getColor() + type.getDisplayName();
        meta.setDisplayName(name);
        meta.setUnbreakable(true);
        if (type.getCustomModelData() > 0) meta.setCustomModelData(type.getCustomModelData());
        // Luu tier va type vao PersistentData de detect chinh xac
        NamespacedKey tierKey = new NamespacedKey(plugin, KEY_TIER);
        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_TYPE);
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.name());
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS);

        List<String> lore = new ArrayList<>();
        lore.add("§8════════════════════════");
        lore.add("§7" + type.getDescription());
        lore.add("§8────────────────────────");
        lore.add("§8════════════════════════");
        lore.add("  " + tier.getColoredName());
        lore.add("§e⚔ ATK: §cx" + String.format("%.2f", tier.getAtkMult()));
        lore.add("§e🛡 DEF: §bx" + String.format("%.2f", tier.getDefMult()));
        lore.add("§e❤ HP:  §ax" + String.format("%.2f", tier.getHpMult()));
        lore.add("§8────────────────────────");
        lore.add("§6✦ Kỹ Năng: §f" + type.getAbilityDesc());
        lore.add("§aCooldown: §f" + (tier.getAbilityCooldown() / 1000) + "s");
        lore.add("§8════════════════════════");
        lore.add("§a► Shift + Chuột Phải §7để dùng kỹ năng");
        lore.add("§7Cầm trên tay chính hoặc off-hand để nhận buff.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack resolveItemsAdderItem(String id) {
        if (id == null || id.isBlank() || plugin.getServer().getPluginManager().getPlugin("ItemsAdder") == null) return null;
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object result = customStack.getMethod("getInstance", String.class).invoke(null, id);
            if (result == null) return null;
            Object stack = result.getClass().getMethod("getItemStack").invoke(result);
            return stack instanceof ItemStack item ? item.clone() : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().fine("ItemsAdder skin không khả dụng cho Pháp Bảo " + id + ": " + ex.getMessage());
            return null;
        }
    }

    /**
     * Random phap bao theo canh gioi:
     *   0-4  (Nhan Gioi) : Dia/Ha Pham chu yeu
     *   5-7  (Linh Gioi) : Ha/Trung Pham
     *   8-12 (Tien Gioi) : Trung/Thuong Pham
     *   13-15 (De Gioi)  : Thuong/De Pham
     *   16-19 (Thanh Gioi): De/Tien Pham
     */
    public ItemStack randomPhaBao(int stageOrdinal) {
        PhaBaoTier tier;
        double r = random.nextDouble();
        if (stageOrdinal >= 20) {         // Thien De
            tier = PhaBaoTier.TIEN_PHAM;
        } else if (stageOrdinal >= 15) { // Tien Gioi
            tier = r < 0.45 ? PhaBaoTier.DE_PHAM
                 : r < 0.85 ? PhaBaoTier.TIEN_PHAM
                 : PhaBaoTier.THUONG_PHAM;
        } else if (stageOrdinal >= 11) { // Thanh Gioi
            tier = r < 0.40 ? PhaBaoTier.THUONG_PHAM
                 : r < 0.80 ? PhaBaoTier.DE_PHAM
                 : PhaBaoTier.TIEN_PHAM;
        } else if (stageOrdinal >= 8) {  // De Gioi
            tier = r < 0.55 ? PhaBaoTier.TRUNG_PHAM
                 : r < 0.90 ? PhaBaoTier.THUONG_PHAM
                 : PhaBaoTier.DE_PHAM;
        } else if (stageOrdinal >= 5) {  // Linh Gioi
            tier = r < 0.55 ? PhaBaoTier.HA_PHAM
                 : r < 0.90 ? PhaBaoTier.TRUNG_PHAM
                 : PhaBaoTier.THUONG_PHAM;
        } else {                          // Nhan Gioi
            tier = r < 0.45 ? PhaBaoTier.DIA_PHAM
                 : r < 0.85 ? PhaBaoTier.HA_PHAM
                 : PhaBaoTier.TRUNG_PHAM;
        }
        PhaBaoType type = pickWeightedType();
        if (type == null) return null; // All random drops disabled by the server owner.
        ItemStack item = createPhaBao(type, tier);
        // Co hoi nho duoc cong san 1 "dong an" (xem PhaBaoFusionManager) - de
        // he thong Dung Hop (/dunghop) co gi de gop lai voi nhau.
        plugin.getPhaBaoFusionManager().maybeRollAffix(item);
        return item;
    }

    /**
     * Chon 1 PhaBaoType theo TRONG SO (khong con uniform random tuyet doi).
     * Doc trong so tung loai tu config "phabao.type-weight.<TEN_ENUM>" (mac
     * dinh 1.0 = nhu nhau cho moi loai neu khong khai bao). So cang NHO =
     * cang HIEM; 0 = loai han khoi vong random (khong bao gio roi).
     *
     * Dung de giam manh ti le roi cua TRAN_THIEN_GIAP (qua manh: +50% giam
     * dame toan than chi can nam trong tui) xuong gan nhu bang khong ma
     * khong xoa han item, va de dang chinh lai/them loai khac sau nay chi
     * bang cach sua config.yml, khong can sua code.
     */
    private PhaBaoType pickWeightedType() {
        List<PhaBaoType> all = Arrays.stream(PhaBaoType.values())
                .filter(PhaBaoType::isDropEnabled).toList();
        if (all.isEmpty()) {
            plugin.getLogger().warning("[Pháp Bảo] Tất cả items trong phabao.yml đang tắt; bỏ qua lượt random drop.");
            return null;
        }
        double[] weights = new double[all.size()];
        double total = 0.0;
        for (int i = 0; i < all.size(); i++) {
            PhaBaoType type = all.get(i);
            double weight = type.hasConfigDropWeight()
                    ? type.getDropWeight()
                    : plugin.getConfig().getDouble("phabao.type-weight." + type.name(), 1.0);
            weights[i] = Double.isFinite(weight) ? Math.max(0.0, Math.min(100000.0, weight)) : 0.0;
            total += weights[i];
        }
        if (total <= 0.0) {
            plugin.getLogger().warning("[Pháp Bảo] Tổng drop-weight bằng 0; chọn ngẫu nhiên trong danh sách đã bật.");
            return all.get(random.nextInt(all.size()));
        }
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < all.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) return all.get(i);
        }
        return all.get(all.size() - 1);
    }

    // ========================
    //  NHAN BIET PHAP BAO
    // ========================

    public boolean isPhaBao(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey tierKey = new NamespacedKey(plugin, KEY_TIER);
        return item.getItemMeta().getPersistentDataContainer()
            .has(tierKey, PersistentDataType.STRING);
    }

    public PhaBaoTier getTier(ItemStack item) {
        if (!isPhaBao(item)) return null;
        NamespacedKey tierKey = new NamespacedKey(plugin, KEY_TIER);
        String val = item.getItemMeta().getPersistentDataContainer()
            .get(tierKey, PersistentDataType.STRING);
        if (val == null) return PhaBaoTier.HA_PHAM;
        try { return PhaBaoTier.valueOf(val); } catch (Exception e) { return PhaBaoTier.HA_PHAM; }
    }

    public PhaBaoType getType(ItemStack item) {
        if (!isPhaBao(item)) return null;
        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_TYPE);
        String val = item.getItemMeta().getPersistentDataContainer()
            .get(typeKey, PersistentDataType.STRING);
        if (val == null) return null;
        try { return PhaBaoType.valueOf(val); } catch (Exception e) { return null; }
    }

    // ========================
    //  SU DUNG KY NANG
    // ========================

    /**
     * Goi khi nguoi choi Shift + Chuot Phai voi phap bao tren tay.
     */
    public boolean useAbility(Player player, PlayerData data, ItemStack item) {
        PhaBaoType type = getType(item);
        PhaBaoTier tier = getTier(item);
        if (type == null || tier == null) return false;

        String cdKey = CD_PREFIX + type.getAbilityId();
        if (data.hasBuff(cdKey)) {
            long rem = (data.getBuffExpire(cdKey) - System.currentTimeMillis()) / 1000;
            player.sendMessage(plugin.prefix() + "§ePháp bảo §c" + type.getDisplayName()
                + " §ecooldown: §c" + (rem + 1) + "s");
            return false;
        }

        boolean success = executeAbility(player, data, type, tier, item);
        if (success) {
            data.addBuff(cdKey, tier.getAbilityCooldown());
            // Hieu ung chi khi thanh cong
            plugin.getEffectEngine().phaBaoCast(player, type.getAbilityId(), tier.getDisplayName());
        }
        return success;
    }

    private boolean executeAbility(Player player, PlayerData data, PhaBaoType type, PhaBaoTier tier, ItemStack item) {
        double dmgMult = data.calculateDamageMultiplier() * tier.getAtkMult()
                * plugin.getPhaBaoEnhanceManager().getBonusMultiplier(item)
                * plugin.getPhaBaoSocketManager().getBonusMultiplier(item, "ATK");
        return switch (type.getAbilityId()) {

            // ==== LINH KIEM: Kiem Khi Phat ====
            case "kiemkhi" -> {
                double dmg = 18.0 * dmgMult;
                Location origin = player.getEyeLocation();
                Vector dir = origin.getDirection().normalize();
                player.sendMessage(plugin.prefix() + "§c⚔ Kiếm Khí Phát! §7Kiếm khí bùng nổ!");
                new BukkitRunnable() {
                    int step = 0;
                    @Override public void run() {
                        if (step++ >= 15) { cancel(); return; }
                        Location pos = origin.clone().add(dir.clone().multiply(step * 0.6));
                        player.getWorld().spawnParticle(Particle.CRIT, pos, 6, 0.2, 0.2, 0.2, 0.1);
                        player.getWorld().spawnParticle(Particle.CRIT, pos, 1, 0, 0, 0, 0);
                        for (Entity e : player.getWorld().getNearbyEntities(pos, 1.2, 1.2, 1.2)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.damage(dmg, player);
                                le.setNoDamageTicks(0);
                                cancel();
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                yield true;
            }

            // ==== THIEN TRUONG: Phap Vu Bao Phong ====
            case "phapvubao" -> {
                double dmg = 10.0 * dmgMult;
                List<Entity> enemies = new ArrayList<>(player.getNearbyEntities(12, 8, 12));
                enemies.removeIf(e -> e == player || !(e instanceof LivingEntity));
                int targets = Math.min(5, enemies.size());
                for (int i = 0; i < targets; i++) {
                    final LivingEntity le = (LivingEntity) enemies.get(i);
                    final int delay = i * 4;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!le.isValid()) return;
                        le.damage(dmg, player);
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                            le.getLocation().add(0,1,0), 10, 0.3, 0.3, 0.3, 0.1);
                        player.getWorld().playSound(le.getLocation(),
                            Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.5f);
                    }, delay);
                }
                player.sendMessage(plugin.prefix() + "§9🔮 Pháp Vũ Bão Phong! §7Tấn công " + targets + " mục tiêu!");
                yield true;
            }

            // ==== AM THU: Xuyen Am ====
            case "xuyenam" -> {
                double dmg = 22.0 * dmgMult;
                Location origin = player.getEyeLocation();
                Vector dir = origin.getDirection().normalize();
                player.sendMessage(plugin.prefix() + "§5🏹 Xuyên Ám! §7Tên linh xuyên qua tất cả!");
                new BukkitRunnable() {
                    int step = 0;
                    final Set<UUID> hitSet = new HashSet<>();
                    @Override public void run() {
                        if (step++ >= 30) { cancel(); return; }
                        Location pos = origin.clone().add(dir.clone().multiply(step * 0.5));
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 3, 0.1, 0.1, 0.1, 0.02);
                        for (Entity e : player.getWorld().getNearbyEntities(pos, 0.6, 0.6, 0.6)) {
                            if (e instanceof LivingEntity le && e != player && !hitSet.contains(e.getUniqueId())) {
                                le.damage(dmg, player);
                                hitSet.add(e.getUniqueId());
                                player.getWorld().spawnParticle(Particle.CRIT,
                                    le.getLocation().add(0,1,0), 8, 0.3, 0.3, 0.3, 0.2);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                yield true;
            }

            // ==== HONG LONG GIAN: Hoa Long Trang ====
            case "hoalongtrang" -> {
                double dmg = 25.0 * dmgMult;
                Location center = player.getLocation();
                player.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
                player.sendMessage(plugin.prefix() + "§6🐉 Hỏa Long Trảng! §7Lửa long xuyên đất!");
                // Song lua xuyen theo 3 huong
                for (int dir = -1; dir <= 1; dir++) {
                    final int d = dir;
                    Vector baseDir = player.getLocation().getDirection().normalize();
                    Vector rotated = baseDir.rotateAroundY(Math.toRadians(d * 20));
                    new BukkitRunnable() {
                        int s = 0;
                        @Override public void run() {
                            if (s++ >= 12) { cancel(); return; }
                            Location p = center.clone().add(rotated.clone().multiply(s * 0.8));
                            player.getWorld().spawnParticle(Particle.FLAME, p, 8, 0.3, 0.3, 0.3, 0.08);
                            player.getWorld().spawnParticle(Particle.LAVA, p, 2, 0.2, 0.2, 0.2, 0);
                            for (Entity e : player.getWorld().getNearbyEntities(p, 1.2, 1.5, 1.2)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(dmg, player);
                                    le.setFireTicks(100);
                                }
                            }
                        }
                    }.runTaskTimer(plugin, d == -1 ? 0 : d * 3L, 2);
                }
                yield true;
            }

            // ==== LINH KHIEN: Ho The Quang Mang ====
            case "hothecuong" -> {
                data.addBuff("phabao_shield", 4000);
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 80, 3, false, true, true));
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    player.getLocation().add(0,1,0), 40, 0.7, 0.7, 0.7, 0.1);
                player.sendMessage(plugin.prefix() + "§b🛡 Hộ Thể Quang Mang! §7Khiên quang mang bảo vệ 4 giây!");
                player.sendTitle(plugin.msg("auto_12b653beee"), plugin.msg("auto_0c9a1da7c7"), 5, 50, 10);
                yield true;
            }

            // ==== NGOC PHAI: Thoat Xac Kim Than ====
            case "thoatxac" -> {
                Location target = player.getTargetBlock(null, 8).getLocation().add(0.5, 1, 0.5);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 20, 0.5, 0.5, 0.5, 0.2);
                player.teleport(target);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.add(0,1,0), 20, 0.5, 0.5, 0.5, 0.2);
                player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§e✦ Thoát Xác Kim Thân! §7Dịch chuyển!");
                yield true;
            }

            // ==== HOI SINH DAN: Hoi Phuc ====
            case "hoiphuc" -> {
                double heal = player.getMaxHealth() * 0.30;
                player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 100, 1, false, true, true));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,1.5,0), 20, 0.5, 0.5, 0.5, 0);
                player.sendMessage(plugin.prefix() + "§a💊 Hồi Sinh Đan! §7Hồi phục §e" + (int)heal + " §7máu!");
                yield true;
            }

            // ==== THAM THIEN KHANH: Am Ba Cuong ====
            case "ambacuong" -> {
                double dmg = 14.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2f, 0.5f);
                player.sendMessage(plugin.prefix() + "§d🔔 Âm Ba Cuồng! §7Sóng âm làm choáng váng!");
                for (Entity e : player.getNearbyEntities(5, 4, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 60, 3, false, true, true));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 60, 2, false, true, true));
                        le.setVelocity(le.getVelocity().add(new Vector(0, 0.3, 0)));
                    }
                }
                spawnRingParticles(player.getLocation(), 5, Particle.ENCHANTED_HIT);
                yield true;
            }

            // ==== THIEN LOI CHAN: Thien Loi Giang The ====
            case "thienloigiangthe" -> {
                double dmg = 20.0 * dmgMult;
                List<Entity> near = new ArrayList<>(player.getNearbyEntities(8, 5, 8));
                near.removeIf(e -> !(e instanceof LivingEntity) || e == player);
                Collections.shuffle(near, random);
                int strikes = Math.min(5, near.size() + 2);
                for (int i = 0; i < strikes; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        Location strikeLoc;
                        if (idx < near.size()) {
                            strikeLoc = near.get(idx).getLocation();
                            if (near.get(idx) instanceof LivingEntity le) le.damage(dmg, player);
                        } else {
                            double ang = Math.random() * 360;
                            double r = Math.random() * 6 + 2;
                            strikeLoc = player.getLocation().add(
                                Math.cos(Math.toRadians(ang)) * r, 0,
                                Math.sin(Math.toRadians(ang)) * r);
                        }
                        player.getWorld().strikeLightning(strikeLoc);
                    }, idx * 8L);
                }
                player.sendMessage(plugin.prefix() + "§e⚡ Thiên Lôi Giáng Thế! §7" + strikes + " tia set giáng xuong!");
                yield true;
            }

            // ==== BANG PHONG BI: Bang Ha Phong Kham ====
            case "bangha" -> {
                int count = 0;
                for (Entity e : player.getNearbyEntities(5, 4, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 60, 10, false, true, true));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 60, 3, false, true, true));
                        player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            le.getLocation().add(0,1,0), 30, 0.5, 0.5, 0.5, 0.05);
                        count++;
                    }
                }
                player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    player.getLocation(), 80, 3, 2, 3, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 2f, 0.4f);
                player.sendMessage(plugin.prefix() + "§b❄ Băng Hà Phong Khám! §7Đóng băng " + count + " kẻ!");
                yield true;
            }

            // ==== TUYET HOA KINH: Trau Doi Linh Khi ====
            case "traudoi" -> {
                long bonus = (long)(data.getMaxLinhKhi() * 0.10);
                data.addLinhKhi(bonus);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    player.getLocation().add(0,1.5,0), 40, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage(plugin.prefix() + "§f📖 Trau Dồi Linh Khí! §7+§e" + fmt(bonus) + " §7Linh Khí!");
                if (player.isOnline()) plugin.getSkillItemManager().updateInfoItem(player, data);
                if (data.getLinhKhi() >= data.getMaxLinhKhi() && !data.isPendingBreakthrough()) {
                    plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
                }
                yield true;
            }

            // ==== HON PHACH DANG: Cuop Linh Hop ====
            case "cuophon" -> {
                double dmg = 12.0 * dmgMult;
                long lkBonus = 0;
                double healTotal = 0;
                for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        healTotal += dmg * 0.4;
                        lkBonus += data.getStage().ordinal() * 200L + 500L;
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            le.getLocation().add(0,1,0), 10, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                if (healTotal > 0)
                    player.setHealth(Math.min(player.getHealth() + healTotal, player.getMaxHealth()));
                if (lkBonus > 0) {
                    data.addLinhKhi(lkBonus);
                    if (player.isOnline()) plugin.getSkillItemManager().updateInfoItem(player, data);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§5🕯 Cướp Linh Hồn! §7Hút "
                    + (int)healTotal + " máu + §e" + fmt(lkBonus) + " §7LK!");
                yield true;
            }

            // ==== QUY DAN: Dich chuyen sau lung x2 ====
            case "quydanphong" -> {
                LivingEntity tgt = getNearestEnemy(player, 15.0);
                if (tgt == null) { player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu!"); yield false; }
                Location behind = tgt.getLocation().clone().subtract(tgt.getLocation().getDirection().normalize().multiply(1.5));
                player.teleport(behind);
                double dmg = 22.0 * dmgMult;
                tgt.damage(dmg * 2.0, player);
                tgt.setNoDamageTicks(0);
                player.getWorld().spawnParticle(Particle.PORTAL, behind.clone().add(0,1,0), 30, 0.3,0.5,0.3,0.4);
                player.sendMessage(plugin.prefix() + "§5👻 Quỷ Đan! §dGây §e" + (int)(dmg*2) + " §dsát thương (x2)!");
                yield true;
            }

            // ==== PHONG LIEU KIEM: Day lui ====
            case "phongleukhi" -> {
                double dmg = 14.0 * dmgMult;
                for (Entity e : player.getNearbyEntities(5, 3, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        Vector away = le.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(1.2).add(new Vector(0,0.4,0));
                        le.setVelocity(away);
                    }
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0,1,0), 25, 1,0.5,1,0.15);
                player.sendMessage(plugin.prefix() + "§b💨 Phong Liễu Kiếm! §7Đẩy lùi tất cả kẻ thù!");
                yield true;
            }

            // ==== HOA LINH CU: Cau lua AoE ====
            case "hoalinhcu" -> {
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize().multiply(2.0);
                org.bukkit.entity.Fireball fb = (org.bukkit.entity.Fireball)
                    player.getWorld().spawnEntity(eye, EntityType.FIREBALL);
                fb.setShooter(player); fb.setDirection(dir); fb.setYield(4.5f); fb.setIsIncendiary(true);
                player.sendMessage(plugin.prefix() + "§6🔥 Hỏa Linh Cầu! §7Cầu lửa phóng ra!");
                yield true;
            }

            // ==== THACH THAN: Nhot con ====
            case "thachthan" -> {
                double dmg = 16.0 * dmgMult;
                LivingEntity t2 = getNearestEnemy(player, 10.0);
                if (t2 == null) { player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu!"); yield false; }
                t2.damage(dmg, player);
                t2.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 60, 10, false, true, true));
                t2.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("mining_fatigue")), 60, 3, false, true, true));
                t2.setVelocity(new Vector(0,0,0));
                player.sendMessage(plugin.prefix() + "§7🪨 Thạch Thần Gia! §7Nhốt cả kẻ thù 3 giây!");
                yield true;
            }

            // ==== LOI PHU: Bep set (so lieu chinh o config.yml "phabao-abilities.loiphu") ====
            case "loiphu" -> {
                Location trapLoc = player.getLocation().clone();
                double loiPhuDmg = plugin.getConfig().getDouble("phabao-abilities.loiphu.damage-per-strike", 5.0);
                int loiPhuInterval = plugin.getConfig().getInt("phabao-abilities.loiphu.strike-interval-ticks", 10);
                int loiPhuDuration = plugin.getConfig().getInt("phabao-abilities.loiphu.duration-ticks", 160);
                if (loiPhuInterval < 1) loiPhuInterval = 1; // tranh chia/lap vo han neu chinh config sai
                final int finalInterval = loiPhuInterval;
                final int finalDuration = loiPhuDuration;
                final double finalDmg = loiPhuDmg;
                player.sendMessage(plugin.prefix() + "§e⚡ Lôi Phù! §7Đặt bẫy sét tại vị trí!");
                // LUU Y: ban goc dung "period=10" nhung dem t theo SO LAN CHAY (khong
                // phai theo tick that), nen "t>=160" thuc te la 160*10=1600 tick = 80
                // giay chu KHONG PHAI 8 giay nhu comment cu ghi -> bay ton tai lau gap
                // 10 lan du dinh, day la 1 phan ly do Loi Phu qua manh. Sua lai o day:
                // elapsedTicks cong don dung so tick that moi lan chay (== finalInterval),
                // nen duration-ticks trong config gio moi PHAN ANH DUNG so tick that.
                new BukkitRunnable() {
                    int elapsedTicks = 0;
                    @Override public void run() {
                        if (elapsedTicks >= finalDuration) { cancel(); return; }
                        for (Entity e : trapLoc.getWorld().getNearbyEntities(trapLoc, 1.5, 1, 1.5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                trapLoc.getWorld().strikeLightningEffect(e.getLocation());
                                le.damage(finalDmg * dmgMult, player);
                            }
                        }
                        trapLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, trapLoc, 3, 0.3,0.1,0.3,0.1);
                        elapsedTicks += finalInterval;
                    }
                }.runTaskTimer(plugin, 0, finalInterval);
                yield true;
            }

            // ==== AM NGUYET: Ten xuyen ====
            case "amnguyet" -> {
                Location origin = player.getEyeLocation();
                Vector dir = origin.getDirection().normalize();
                double dmg = 18.0 * dmgMult;
                int[] hits = {0};
                new BukkitRunnable() {
                    int step = 0;
                    @Override public void run() {
                        if (step++ >= 30 || hits[0] >= 3) { cancel(); return; }
                        Location pos = origin.clone().add(dir.clone().multiply(step * 0.7));
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 2, 0.1,0.1,0.1,0.02);
                        for (Entity e : player.getWorld().getNearbyEntities(pos, 0.6, 0.6, 0.6)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.damage(dmg, player); hits[0]++;
                                player.getWorld().spawnParticle(Particle.CRIT, pos, 5, 0.2,0.2,0.2,0.1);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                player.sendMessage(plugin.prefix() + "§5🏹 Ám Nguyệt Cung! §7Tên ám xuyên 3 kẻ!");
                yield true;
            }

            // ==== CUONG PHONG PHIET: Cheo gio ====
            case "cuongphongphiet" -> {
                double dmg = 15.0 * dmgMult;
                Vector dir = player.getLocation().getDirection().normalize();
                java.util.Set<java.util.UUID> hitSet = new java.util.HashSet<>();
                new BukkitRunnable() {
                    int step = 0;
                    @Override public void run() {
                        if (step++ >= 20) { cancel(); return; }
                        Location pos = player.getLocation().add(dir.clone().multiply(step * 0.5));
                        player.getWorld().spawnParticle(Particle.CLOUD, pos, 3, 0.3,0.3,0.3,0.08);
                        for (Entity e : player.getWorld().getNearbyEntities(pos, 1.5, 2, 1.5)) {
                            if (e instanceof LivingEntity le && e != player && !hitSet.contains(e.getUniqueId())) {
                                le.damage(dmg, player); hitSet.add(e.getUniqueId());
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                player.sendMessage(plugin.prefix() + "§b🌪 Cuồng Phong Phiệt! §7Chém gió 10m!");
                yield true;
            }

            // ==== THUY LONG: Rong nuoc ====
            case "thuylong" -> {
                double dmg = 10.0 * dmgMult;
                player.sendMessage(plugin.prefix() + "§9🌊 Thủy Long Trâm! §7Rồng nước xuất hiện!");
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 120) { cancel(); return; }
                        Location center = player.getLocation();
                        if (t % 20 == 0) {
                            for (Entity e : center.getWorld().getNearbyEntities(center, 6, 3, 6)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(dmg, player);
                                    le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 40, 1, false, true, true));
                                }
                            }
                        }
                        double ang = Math.toRadians(t * 12);
                        Location p = center.clone().add(Math.cos(ang)*3, 0.5 + Math.sin(Math.toRadians(t*6))*0.5, Math.sin(ang)*3);
                        center.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 3, 0.2,0.2,0.2,0.03);
                    }
                }.runTaskTimer(plugin, 0, 1);
                yield true;
            }

            // ==== BACH NGOC: Bat tu hoan toan ====
            case "bachngoc" -> {
                data.addBuff("bachngoc_invincible", 3000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 60, 255, false, false, false));
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 40, 0.8,0.8,0.8,0.2);
                player.sendMessage(plugin.prefix() + "§f💎 Bạch Ngọc Pháp Thân! §fBất tử 3 giây!");
                player.sendTitle(plugin.msg("auto_4da9c4d370"), plugin.msg("auto_2e22fddd06"), 5, 50, 5);
                yield true;
            }

            // ==== THIET BI CUONG: Giam 70% sát thương ====
            case "thietbicuong" -> {
                data.addBuff("thietbi_shield", 6000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 120, 3, false, true, true));
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0,1,0), 30, 0.6,0.6,0.6,0.2);
                player.sendMessage(plugin.prefix() + "§7⚙ Thiết Bí Cường! §7Giảm 70% sát thương 6 giây!");
                yield true;
            }

            // ==== PHAN TU: Bat tu 1 lan ====
            case "phantu" -> {
                data.addBuff("phabao_rebirth", 60000);
                player.sendMessage(plugin.prefix() + "§c✦ Phản Hồn Kính! §7Bất tử 1 lần kích hoạt (60 giây).");
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,2,0), 15, 0.5,0.5,0.5,0);
                yield true;
            }

            // ==== VAN LINH: Phan don 30% ====
            case "vanlinh" -> {
                data.addBuff("vanlinh_reflect", 5000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("absorption")), 100, 3, false, true, true));
                player.sendMessage(plugin.prefix() + "§6✦ Vạn Linh Giáp! §7Phản 30% sát thương nhận vào 5 giây!");
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 20, 0.5,0.5,0.5,0.1);
                yield true;
            }

            // ==== BAO THAN THANH: Khien 60% ====
            case "baothanthanh" -> {
                data.addBuff("baothanh_shield", 5000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 100, 2, false, true, true));
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 35, 0.7,0.7,0.7,0.1);
                player.sendMessage(plugin.prefix() + "§f✨ Bảo Thân Thánh! §fGiảm 60% sát thương 5 giây!");
                yield true;
            }

            // ==== THAM TAM: Mien dich hieu ung xau ====
            case "thamtam" -> {
                for (var eff : player.getActivePotionEffects()) {
                    boolean bad = java.util.Arrays.asList("poison","wither","slowness","weakness","blindness","hunger","mining_fatigue","nausea")
                        .contains(eff.getType().getKey().getKey());
                    if (bad) player.removePotionEffect(eff.getType());
                }
                data.addBuff("thamtam_immune", 6000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("absorption")), 120, 2, false, true, true));
                player.sendMessage(plugin.prefix() + "§d📖 Thám Tâm Kính! §7Miễn dịch hiệu ứng xấu 6 giây!");
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0,1,0), 20, 0.8,0.8,0.8,0.5);
                yield true;
            }

            // ==== LINH KHU: Giam sát thương 40% ====
            case "linh_khu" -> {
                data.addBuff("linh_khu_shield", 5000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 100, 1, false, true, true));
                player.sendMessage(plugin.prefix() + "§9💧 Tứ Hải Linh Khu! §7Giảm 40% sát thương 5 giây!");
                for (double a = 0; a < 360; a += 12) {
                    double rad = Math.toRadians(a);
                    Location p = player.getLocation().clone().add(Math.cos(rad)*2, 0.5, Math.sin(rad)*2);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 3, 0.2,0.3,0.2,0.03);
                }
                yield true;
            }

            // ==== THAN LINH PHU: Xoa hieu ung xau ====
            case "thanlinh" -> {
                for (var eff : player.getActivePotionEffects()) player.removePotionEffect(eff.getType());
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("absorption")), 160, 3, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 80, 1, false, true, true));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,1.5,0), 20, 0.5,0.5,0.5,0);
                player.sendMessage(plugin.prefix() + "§a🍃 Thần Linh Phù! §7Xóa tất cả hiệu ứng xấu + miễn dịch!");
                yield true;
            }

            // ==== VAN PHU DAN: Buff toan doi ====
            case "vanphu" -> {
                int cnt = 0;
                for (Entity e : player.getNearbyEntities(6,6,6)) {
                    if (e instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 160, 1, false, true, true));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 160, 1, false, true, true));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 160, 1, false, true, true));
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0,2,0), 8, 0.4,0.3,0.4,0);
                        cnt++;
                    }
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 160, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 160, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 160, 1, false, true, true));
                player.sendMessage(plugin.prefix() + "§e🎁 Vạn Phù Đan! §7Buff " + (cnt+1) + " người trong đội 8 giây!");
                yield true;
            }

            // ==== CAM LO BONG: Hoi phuc theo thoi gian ====
            case "camlobong" -> {
                player.sendMessage(plugin.prefix() + "§a🍯 Cam Lộ Bồng! §7Hồi phục 5%/2 giây trong 10 giây!");
                new BukkitRunnable() {
                    int waves = 0;
                    @Override public void run() {
                        if (waves++ >= 5 || !player.isOnline()) { cancel(); return; }
                        double heal = player.getMaxHealth() * 0.05;
                        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,1.5,0), 3, 0.3,0.3,0.3,0);
                    }
                }.runTaskTimer(plugin, 0, 40);
                yield true;
            }

            // ==== PHUC HOA PHAP: Hoi dong doi ====
            case "phuchoa" -> {
                double heal = 0;
                for (Entity e : player.getNearbyEntities(8,8,8)) {
                    if (e instanceof Player ally) {
                        double h = ally.getMaxHealth() * 0.20;
                        ally.setHealth(Math.min(ally.getHealth() + h, ally.getMaxHealth()));
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0,2,0), 10, 0.4,0.4,0.4,0);
                        ally.sendMessage(plugin.prefix() + "§a💊 Phục Hồi Pháp hồi phục " + (int)h + " máu!");
                        heal += h;
                    }
                }
                player.setHealth(Math.min(player.getHealth() + player.getMaxHealth()*0.20, player.getMaxHealth()));
                player.sendMessage(plugin.prefix() + "§a🌿 Phục Hỏa Pháp! §7Hồi tổng " + (int)heal + " máu cho đồng đội!");
                yield true;
            }

            // ==== HOP THE DAN: Triple buff ====
            case "hopthedan" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 200, 2, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 200, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("haste")), 200, 1, false, true, true));
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0,1,0), 30, 0.5,0.5,0.5,0.2);
                player.sendMessage(plugin.prefix() + "§c⚡ Hợp Thể Đan! §7STR III + SPEED II + HASTE II 10 giây!");
                yield true;
            }

            // ==== THIEN DI DAN: Teleport den vi tri nhin ====
            case "thiendi" -> {
                Location target = player.getTargetBlock(null, 30).getLocation().add(0.5, 1, 0.5);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0,1,0), 20, 0.3,0.5,0.3,0.3);
                player.teleport(target);
                player.getWorld().spawnParticle(Particle.PORTAL, target.add(0,1,0), 20, 0.3,0.5,0.3,0.3);
                player.sendMessage(plugin.prefix() + "§b✈ Thiên Di Đan! §7Dịch chuyển thành công!");
                yield true;
            }

            // ==== ANH THE TAN: Night Vision dong doi ====
            case "anhtetan" -> {
                int cnt2 = 0;
                for (Entity e : player.getNearbyEntities(10,10,10)) {
                    if (e instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("night_vision")), 1200, 0, false, true, true));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("saturation")), 100, 1, false, false, false));
                        cnt2++;
                    }
                }
                player.sendMessage(plugin.prefix() + "§6👁 Ảnh Thế Tán! §7Night Vision cho " + (cnt2+1) + " người 60 giây!");
                yield true;
            }

            // ==== HUE NHAN KHANH: Reveal ke thu ====
            case "huenhan" -> {
                int found = 0;
                for (Entity e : player.getNearbyEntities(20,10,20)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("glowing")), 200, 0, false, false, true));
                        found++;
                    }
                }
                player.sendMessage(plugin.prefix() + "§d👁 Huệ Nhãn Khánh! §7Phát hiện §e" + found + " §dkẻ thù trong 20m 10 giây!");
                yield true;
            }

            // ==== HON PHACH HAP: Thu LK tu mob ====
            case "honphachap" -> {
                long lkGain = 5000L + data.getStage().ordinal() * 1000L;
                int mobs = 0;
                for (Entity e : player.getNearbyEntities(8,4,8)) {
                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                        le.damage(8.0 * dmgMult, player);
                        lkGain += 2000L;
                        mobs++;
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, le.getLocation().add(0,1,0), 5, 0.3,0.5,0.3,0.04);
                    }
                }
                data.addLinhKhi(lkGain);
                plugin.getSkillItemManager().updateInfoItem(player, data);
                player.sendMessage(plugin.prefix() + "§d✦ Hồn Phách Hấp! §7Thu §e" + fmt(lkGain) + " §dLK từ §e" + mobs + " §dmob!");
                if (data.getLinhKhi() >= data.getMaxLinhKhi() && !data.isPendingBreakthrough())
                    plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
                yield true;
            }

            // ==== LINH TICH TAN: x2 LK 30 giay ====
            case "linhtichtan" -> {
                data.addBuff("lktichtan_double", 30000);
                player.sendMessage(plugin.prefix() + "§7✨ Linh Tích Tán! §7Linh Khí x2 trong 30 giây!");
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0,1,0), 30, 0.8,0.8,0.8,0.5);
                yield true;
            }

            // ==== PHAP LUC THUNG: Doi Tien lay LK ====
            case "phaplucthung" -> {
                long cost = 1000L;
                if (data.getTien() < cost) {
                    player.sendMessage(plugin.prefix() + "§cCan §e1000 §c" + plugin.getTienManager().tenTien() + "!"); yield false;
                }
                data.spendTien(cost);
                data.addLinhKhi(50000L);
                plugin.getSkillItemManager().updateInfoItem(player, data);
                player.sendMessage(plugin.prefix() + "§6✦ Pháp Lực Thùng! §7-1000 " + plugin.getTienManager().tenTien() + " → §e+50,000 §7LK!");
                if (data.getLinhKhi() >= data.getMaxLinhKhi() && !data.isPendingBreakthrough())
                    plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
                yield true;
            }

            // ==== TRUONG PHONG: Day lui AoE ====
            case "truongphong" -> {
                for (Entity e : player.getNearbyEntities(6,3,6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        Vector away = le.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(1.8).add(new Vector(0,0.6,0));
                        le.setVelocity(away);
                        le.damage(6.0 * dmgMult, player);
                    }
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0,1,0), 40, 2,0.5,2,0.18);
                player.sendMessage(plugin.prefix() + "§b🌪 Trường Phong! §7Đẩy lùi tất cả kẻ xung quanh!");
                yield true;
            }

            // ==== HOA HOI TRAN: Tran lua keo dai ====
            case "hoahoitran" -> {
                Location center = player.getLocation().clone();
                double dmg3 = 4.0 * dmgMult;
                player.sendMessage(plugin.prefix() + "§6🔥 Hỏa Hồi Trận! §7Đặt trận lửa 8 giây!");
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 160) { cancel(); return; }
                        if (t % 20 == 0) {
                            for (Entity e : center.getWorld().getNearbyEntities(center, 5, 2, 5)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(dmg3, player); le.setFireTicks(60);
                                }
                            }
                        }
                        for (double a = 0; a < 360; a += 20) {
                            double rad = Math.toRadians(a + t*3);
                            Location p = center.clone().add(Math.cos(rad)*4, 0.1, Math.sin(rad)*4);
                            center.getWorld().spawnParticle(Particle.FLAME, p, 1, 0.2,0.1,0.2,0.03);
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                yield true;
            }

            // ==== BANG PHANG TRAN: Vung dong bang ====
            case "bangphangtran" -> {
                Location center = player.getLocation().clone();
                player.sendMessage(plugin.prefix() + "§b❄ Băng Phong Trận! §7Vùng đóng băng 5 giây!");
                for (Entity e : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 100, 10, false, true, true));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 100, 2, false, true, true));
                        le.setVelocity(new Vector(0,0,0));
                        center.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0,1,0), 20, 0.4,0.5,0.4,0.05);
                    }
                }
                center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 60, 2,1,2,0.1);
                yield true;
            }

            // ==== DOC MAI TRAN: Bep doc ====
            case "docmaitran" -> {
                Location trapLoc = player.getLocation().clone();
                player.sendMessage(plugin.prefix() + "§2☠ Độc Mai Trận! §7Đặt bẫy độc vô hình!");
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 200) { cancel(); return; }
                        for (Entity e : trapLoc.getWorld().getNearbyEntities(trapLoc, 1.5, 1, 1.5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("poison")), 120, 3, false, true, true));
                                trapLoc.getWorld().spawnParticle(Particle.WITCH, le.getLocation().add(0,1,0), 8, 0.3,0.3,0.3,0.05);
                            }
                        }
                        if (t % 15 == 0) trapLoc.getWorld().spawnParticle(Particle.WITCH, trapLoc, 2, 0.2,0.05,0.2,0);
                    }
                }.runTaskTimer(plugin, 0, 10);
                yield true;
            }

            // ==== DIEN CUONG: Truong dien cham ====
            case "diencuong" -> {
                int cnt3 = 0;
                for (Entity e : player.getNearbyEntities(5,3,5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 80, 3, false, true, true));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 80, 1, false, true, true));
                        le.damage(5.0 * dmgMult, player);
                        cnt3++;
                    }
                }
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 25, 1.5,0.5,1.5,0.2);
                player.sendMessage(plugin.prefix() + "§e⚡ Điện Từ Cường! §7Làm chậm §e" + cnt3 + " §ekẻ thù 4 giây!");
                yield true;
            }

            // ==== THIEN MO TRAN: Giam phep ATK ====
            case "thienmotran" -> {
                Location center = player.getLocation().clone();
                player.sendMessage(plugin.prefix() + "§4🌑 Thiên Mộ Trận! §7Giảm 50% phép ATK kẻ thù 8 giây!");
                for (Entity e : center.getWorld().getNearbyEntities(center, 6, 3, 6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 160, 2, false, true, true));
                        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, le.getLocation().add(0,1,0), 5, 0.3,0.5,0.3,0.02);
                    }
                }
                center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 30, 2.5,1,2.5,0.04);
                yield true;
            }

            // ==== MAI PHUC: Bep keo ====
            case "maiphuc" -> {
                Location trapLoc = player.getLocation().clone();
                player.sendMessage(plugin.prefix() + "§5🕸 Mai Phục Trùng! §7Đặt bẫy vô hình kéo lên cao!");
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 200) { cancel(); return; }
                        for (Entity e : trapLoc.getWorld().getNearbyEntities(trapLoc, 1.5, 0.5, 1.5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.setVelocity(new Vector(0, 1.5, 0));
                                cancel();
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 5);
                yield true;
            }

            // ==== PHONG TRAN TINH: Cuon len ====
            case "phongtrantinh" -> {
                for (Entity e : player.getNearbyEntities(5,3,5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.setVelocity(new Vector(
                            (le.getLocation().getX() - player.getLocation().getX()) * 0.3,
                            2.0,
                            (le.getLocation().getZ() - player.getLocation().getZ()) * 0.3));
                        le.damage(8.0 * dmgMult, player);
                    }
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0,1,0), 40, 2,1,2,0.15);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
                player.sendMessage(plugin.prefix() + "§b🌪 Phong Trần Tinh! §7Cuốn tất cả kẻ thù lên cao!");
                yield true;
            }

            // ==== CHIEU THIEN KINH: Bat tu De Pham 5 giay ====
            case "chieuthien" -> {
                data.addBuff("chieuthien_invincible", 5000);
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 100, 255, false, false, false));
                player.sendMessage(plugin.prefix() + "§c§l✦ Chiêu Thiên Kinh! §fBất tử hoàn toàn 5 giây!");
                player.sendTitle(plugin.msg("auto_2b70f24a92"), plugin.msg("auto_b66ce35736"), 5, 60, 10);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,2,0), 2, 0.3,0,0.3,0);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 50, 1,1,1,0.3);
                // Hoi phuc toan bo sau khi het
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.setHealth(player.getMaxHealth());
                        player.sendMessage(plugin.prefix() + "§aChiêu Thiên hết hiệu lực - Hồi phục toàn bộ HP!");
                    }
                }, 100L);
                yield true;
            }

            // ==== VAN HOA BOT: AoE xuyen DEF De Pham ====
            case "vanhoa" -> {
                double bigDmg = 60.0 * dmgMult;
                int cnt4 = 0;
                for (Entity e : player.getNearbyEntities(8, 5, 8)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(bigDmg, player);
                        le.setNoDamageTicks(0);
                        le.setFireTicks(100);
                        cnt4++;
                        player.getWorld().spawnParticle(Particle.EXPLOSION, le.getLocation().add(0,1,0), 1, 0,0,0,0);
                    }
                }
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 3, 0.5,0,0.5,0);
                player.sendMessage(plugin.prefix() + "§c§l☠ Vạn Hỏa Bốt! §7Gây §c" + (int)bigDmg + " §7sát thương cho §e" + cnt4 + " §7kẻ (xuyên DEF)!");
                player.sendTitle(plugin.msg("auto_55f6e373ca"), "§e" + cnt4 + " kẻ trúng đòn!", 5, 30, 10);
                yield true;
            }

            // ==== THANH LINH PHONG: Heal toan doi + push Tien Pham ====
            case "thanhlinh" -> {
                // Heal dong doi
                int healed = 0;
                for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                    if (e instanceof Player ally) {
                        ally.setHealth(ally.getMaxHealth());
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")), 200, 2, false, true, true));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 200, 2, false, true, true));
                        ally.sendMessage(plugin.prefix() + "§b§l✦ Thanh Linh Phong cứu sống! HP hồi phục toàn bộ!");
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0,2,0), 20, 0.5,0.5,0.5,0);
                        healed++;
                    }
                }
                // Push ke thu
                for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                        Vector away = le.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(2.5).add(new Vector(0, 0.8, 0));
                        le.setVelocity(away);
                        le.damage(20.0 * dmgMult, player);
                    }
                }
                player.setHealth(player.getMaxHealth());
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,2,0), 3, 0.5,0,0.5,0);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 60, 2,1,2,0.3);
                player.sendMessage(plugin.prefix() + "§b§l✦ Thanh Linh Phong! §7Hồi phục " + (healed+1) + " đồng đội + Đẩy lùi tất cả kẻ thù!");
                yield true;
            }

            // ==== HONG MONG PHU: ULTIMATE Tien Pham ====
            case "hongmong" -> {
                double ultDmg = 100.0 * dmgMult;
                int cnt5 = 0;
                // Broadcast
                String bc = plugin.prefix() + "§6§l⚡ " + player.getName() + " §ethi triển §6§lHỒNG MÔNG PHÙ! §7Thế giới run chuyển!";
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(bc));
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 0.3f));
                // Phase: giao huong 2 giay
                player.sendTitle(plugin.msg("auto_52af814c43"), plugin.msg("auto_a627dfa968"), 5, 50, 10);
                final int[] totalHits = {0};
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Vu no 5 song lan
                    for (int wave = 1; wave <= 5; wave++) {
                        final int w = wave;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            double waveR = w * 3.0;
                            for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), waveR+1, 5, waveR+1)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(ultDmg * (1.0 - w*0.1), player);
                                    le.setNoDamageTicks(0);
                                    le.setFireTicks(200);
                                    totalHits[0]++;
                                }
                            }
                            for (double a = 0; a < 360; a += 6) {
                                double rad = Math.toRadians(a);
                                Location p = player.getLocation().clone().add(Math.cos(rad)*waveR, 0.5, Math.sin(rad)*waveR);
                                player.getWorld().spawnParticle(Particle.EXPLOSION, p, 1, 0,0,0,0);
                                player.getWorld().spawnParticle(Particle.FLAME, p, 3, 0.2,0.3,0.2,0.08);
                            }
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f - w*0.05f);
                        }, wave * 8L);
                    }
                    // Reset moi CD sau 3 giay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (data.hasClass()) {
                            for (String sid : data.getClassType().getSkillIds()) data.setSkillCooldown(sid, 0);
                        }
                        player.sendMessage(plugin.prefix() + "§6✦ Hồng Mông Phù kết thúc! Reset mọi CD! Hit: §e" + totalHits[0]);
                        plugin.getTienManager().give(player, totalHits[0] * 500 + 10000);
                    }, 50L);
                }, 40L);
                yield true;
            }

            // ════════════════════════════════════════════════════════
            //  26 PHAP BAO MOI - TRUOC DAY CHUA CO CODE (chi hien
            //  "Kỹ năng chưa được triển khai" khi su dung)
            // ════════════════════════════════════════════════════════

            case "thienphong" -> { // Thien Phong Kiem: 3 luoi gio chem, hat tung
                double dmg = 16.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.5f, 1.2f);
                player.sendMessage(plugin.prefix() + "§b💨 Thiên Phong Kiếm! §7Ba lưỡi gió bay ra!");
                Vector dir0 = player.getEyeLocation().getDirection().normalize();
                for (int i = 0; i < 3; i++) {
                    final int idx = i;
                    new BukkitRunnable() {
                        int step = 0;
                        @Override public void run() {
                            if (step++ >= 10) { cancel(); return; }
                            double spread = (idx - 1) * 0.8;
                            Location pos = player.getEyeLocation().add(dir0.clone().multiply(step * 1.0))
                                .add(dir0.clone().rotateAroundY(Math.PI / 2).multiply(spread));
                            pos.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pos, 1, 0, 0, 0, 0);
                            for (Entity e : pos.getWorld().getNearbyEntities(pos, 1.2, 1.2, 1.2)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(dmg, player);
                                    le.setVelocity(le.getVelocity().add(new Vector(0, 0.5, 0)));
                                }
                            }
                        }
                    }.runTaskTimer(plugin, idx * 2L, 1L);
                }
                yield true;
            }

            case "haivuong" -> { // Hai Vuong Tram: kim tram xuyen giap
                double dmg = 17.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 1f);
                player.sendMessage(plugin.prefix() + "§9🔱 Hải Vương Trâm! §7Kim trâm nước xuyên giáp!");
                Location origin = player.getEyeLocation();
                Vector dir1 = origin.getDirection().normalize();
                new BukkitRunnable() {
                    int step = 0;
                    @Override public void run() {
                        if (step++ >= 20) { cancel(); return; }
                        Location pos = origin.clone().add(dir1.clone().multiply(step * 0.8));
                        pos.getWorld().spawnParticle(Particle.DRIPPING_WATER, pos, 3, 0.1, 0.1, 0.1, 0);
                        for (Entity e : pos.getWorld().getNearbyEntities(pos, 1, 1, 1)) {
                            if (e instanceof LivingEntity le && e != player) {
                                boolean armored = le instanceof Player p2 && p2.getInventory().getChestplate() != null;
                                le.damage(armored ? dmg * 1.5 : dmg, player);
                                cancel();
                                return;
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                yield true;
            }

            case "hoaphungqua" -> { // Hoa Phung Qua: quat lua 180 do
                double dmg = 15.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.8f);
                player.sendMessage(plugin.prefix() + "§c🪭 Hỏa Phụng Quạt! §7Quạt lửa bung mở!");
                Location eye = player.getEyeLocation();
                Vector fwd = eye.getDirection().normalize();
                for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                    if (!(e instanceof LivingEntity le) || e == player) continue;
                    Vector toEnt = le.getLocation().toVector().subtract(eye.toVector()).normalize();
                    if (fwd.dot(toEnt) > 0.3) {
                        le.damage(dmg, player);
                        le.setFireTicks(100);
                        le.setVelocity(le.getVelocity().add(toEnt.clone().multiply(1.2).setY(0.3)));
                    }
                }
                spawnRingParticles(eye, 4, Particle.FLAME);
                yield true;
            }

            case "bachho" -> { // Bach Ho Quyen: quyen AoE giam giap
                double dmg = 22.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.7f);
                player.sendMessage(plugin.prefix() + "§f🐯 Bạch Hổ Quyền! §7Cuồng lực như hổ!");
                for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 100, 2, false, true, true));
                        le.setVelocity(le.getVelocity().add(new Vector(0, 0.4, 0)));
                    }
                }
                spawnRingParticles(player.getLocation(), 3, Particle.CRIT);
                yield true;
            }

            case "tamtai" -> { // Tam Tai Kiem: 3 don lua-bang-set
                double dmg = 14.0 * dmgMult;
                player.sendMessage(plugin.prefix() + "§d⚔ Tam Tài Kiếm! §7Kim - Mộc - Thủy hòa hợp!");
                String[] effs = {"fire", "ice", "thunder"};
                for (int i = 0; i < 3; i++) {
                    final int idx = i;
                    new BukkitRunnable() {
                        @Override public void run() {
                            for (Entity e : player.getNearbyEntities(4, 3, 4)) {
                                if (!(e instanceof LivingEntity le) || e == player) continue;
                                le.damage(dmg, player);
                                switch (effs[idx]) {
                                    case "fire" -> le.setFireTicks(60);
                                    case "ice" -> le.addPotionEffect(new PotionEffect(
                                        PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 60, 3, false, true, true));
                                    case "thunder" -> le.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                        le.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                                }
                            }
                            player.getWorld().playSound(player.getLocation(),
                                idx == 0 ? Sound.ITEM_FIRECHARGE_USE : idx == 1 ? Sound.BLOCK_GLASS_BREAK : Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                                1f, 1f);
                        }
                    }.runTaskLater(plugin, idx * 6L);
                }
                yield true;
            }

            case "tutuongan" -> { // Tu Tuong An: phong toa vung 5m
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.6f);
                player.sendMessage(plugin.prefix() + "§d⏰ Tứ Tượng Ấn! §7Phong tỏa không-thời gian!");
                for (Entity e : player.getNearbyEntities(5, 4, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 100, 3, false, true, true));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("mining_fatigue")), 100, 2, false, true, true));
                    }
                }
                spawnRingParticles(player.getLocation(), 5, Particle.PORTAL);
                yield true;
            }

            case "batquattran" -> { // Bat Quat Tran: vong xoay chop lua 6 giay
                double dmg = 12.0 * dmgMult;
                player.sendMessage(plugin.prefix() + "§6🧭 Bát Quái Trận! §7May chop lua xoay quanh!");
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1f);
                new BukkitRunnable() {
                    int tick = 0;
                    @Override public void run() {
                        if (tick++ >= 30) { cancel(); return; }
                        double rad = Math.toRadians(tick * 24);
                        Location pos = player.getLocation().add(Math.cos(rad) * 3, 1, Math.sin(rad) * 3);
                        pos.getWorld().spawnParticle(Particle.FLAME, pos, 5, 0.2, 0.2, 0.2, 0.02);
                        for (Entity e : pos.getWorld().getNearbyEntities(pos, 1, 1, 1)) {
                            if (e instanceof LivingEntity le && e != player) le.damage(dmg, player);
                        }
                    }
                }.runTaskTimer(plugin, 0L, 4L);
                yield true;
            }

            case "hondon" -> { // Hon Don Cau: xuyen DEF, giam khien
                double dmg = 30.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.5f);
                player.sendMessage(plugin.prefix() + "§5☯ Hỗn Độn Cầu! §7Nguyên khí nguyên thủy bùng nổ!");
                for (Entity e : player.getNearbyEntities(6, 5, 6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        le.removePotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 80, 3, false, true, true));
                    }
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3, 1, 1, 1, 0);
                spawnRingParticles(player.getLocation(), 6, Particle.PORTAL);
                yield true;
            }

            case "huyenvu" -> { // Huyen Vu Thuan: bat tu 4s + DEF +100% 8s
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 0.8f);
                player.sendMessage(plugin.prefix() + "§8🐢 Huyền Vũ Thuẫn! §7Vững như thần quy bất tử!");
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 80, 255, false, true, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("fire_resistance")), 80, 0, false, true, true));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 160, 2, false, true, true));
                        player.sendMessage(plugin.prefix() + "§8Huyền Vũ: DEF +100% trong 8 giây!");
                    }
                }, 80L);
                spawnRingParticles(player.getLocation(), 2, Particle.CLOUD);
                yield true;
            }

            case "cuulong" -> { // Cuu Long Giap: mien dich debuff + DEF +50%
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);
                player.sendMessage(plugin.prefix() + "§6🐉 Cửu Long Giáp! §7Chín rồng bảo vệ!");
                for (String key : new String[]{"poison", "wither", "slowness", "weakness"}) {
                    player.removePotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft(key)));
                }
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 160, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("fire_resistance")), 160, 0, false, true, true));
                spawnRingParticles(player.getLocation(), 3, Particle.END_ROD);
                yield true;
            }

            case "ngochubinh" -> { // Ngoc Hu Binh: nhot ke thu gan nhat
                LivingEntity target = null;
                double best = 8;
                for (Entity e : player.getNearbyEntities(8, 5, 8)) {
                    if (e instanceof LivingEntity le && e != player) {
                        double d = le.getLocation().distance(player.getLocation());
                        if (d < best) { best = d; target = le; }
                    }
                }
                if (target == null) { player.sendMessage(plugin.prefix() + "§cKhông có kẻ thù trong tầm!"); yield false; }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§b🍶 Ngọc Hư Bình! §7Nhốt kẻ thù vào bình ảo!");
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 80, 255, false, true, true));
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("jump_boost")), 80, 128, false, true, true));
                spawnRingParticles(target.getLocation(), 1.5, Particle.PORTAL);
                yield true;
            }

            case "thaiatphu" -> { // Thai At Phu: +5 max HP vinh vien, toi da 10 lan
                int used = data.getExtraInt("thaiatphu_used", 0);
                if (used >= 10) {
                    player.sendMessage(plugin.prefix() + "§6Thái Ất Phù đã đạt giới hạn +5 HP x10 lần!");
                    yield false;
                }
                data.setExtraInt("thaiatphu_used", used + 1);
                var attrHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (attrHp != null) attrHp.setBaseValue(attrHp.getBaseValue() + 5.0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§6📜 Thái Ất Phù! §a+5 Max HP vĩnh viễn! §7(" + (used + 1) + "/10)");
                spawnRingParticles(player.getLocation(), 1.5, Particle.HAPPY_VILLAGER);
                yield true;
            }

            case "cankhon" -> { // Can Khon Dai: teleport + speed
                Location dest = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(8));
                if (dest.getBlock().getType().isSolid()) dest = player.getLocation();
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                spawnRingParticles(player.getLocation(), 1, Particle.PORTAL);
                player.teleport(dest);
                spawnRingParticles(dest, 1, Particle.PORTAL);
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 100, 3, false, true, true));
                player.setFallDistance(0f);
                player.sendMessage(plugin.prefix() + "§e🌀 Càn Khôn Đại! §7Dịch chuyển tức thời!");
                yield true;
            }

            case "kimthien" -> { // Kim Thien Gia: giam 60% damage 5 giay
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 0.8f);
                player.sendMessage(plugin.prefix() + "§e🛡 Kim Thiên Giáp! §7Giáp vàng thiên thông kích hoạt!");
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 100, 2, false, true, true));
                spawnRingParticles(player.getLocation(), 1.5, Particle.HAPPY_VILLAGER);
                yield true;
            }

            case "phuteng" -> { // Phu Ten Du: xuyen tuong 3m
                Location dest = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(3));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.2f);
                player.teleport(dest);
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("resistance")), 40, 255, false, true, true));
                player.sendMessage(plugin.prefix() + "§7👻 Phù Tên Du! §7Xuyên tường thoát hiểm!");
                spawnRingParticles(player.getLocation(), 1, Particle.CLOUD);
                yield true;
            }

            case "vanhoatap" -> { // Van Hoa Tap: troi buoc ke thu gan nhat
                LivingEntity target = null;
                double best = 6;
                for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        double d = le.getLocation().distance(player.getLocation());
                        if (d < best) { best = d; target = le; }
                    }
                }
                if (target == null) { player.sendMessage(plugin.prefix() + "§cKhông có kẻ thù trong tầm!"); yield false; }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BAMBOO_BREAK, 1f, 0.8f);
                player.sendMessage(plugin.prefix() + "§a🎋 Vạn Hoa Tập! §7Dây linh khí trói buộc!");
                target.setVelocity(new Vector(0, 1.6, 0));
                target.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("slow_falling")), 80, 250, false, true, true));
                spawnRingParticles(target.getLocation(), 1, Particle.HAPPY_VILLAGER);
                yield true;
            }

            case "thiethoa" -> { // Thiet Hoa Lien: lien ket 3 ke thu, chia se damage
                double dmg = 13.0 * dmgMult;
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof LivingEntity le && e != player) targets.add(le);
                    if (targets.size() >= 3) break;
                }
                if (targets.isEmpty()) { player.sendMessage(plugin.prefix() + "§cKhông có kẻ thù trong tầm!"); yield false; }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 0.7f);
                player.sendMessage(plugin.prefix() + "§7⛓ Thiết Hoa Liên! §7Xích sắt liên kết " + targets.size() + " kẻ thù!");
                targets.get(0).damage(dmg, player);
                spawnRingParticles(targets.get(0).getLocation(), 1, Particle.CRIT);
                for (int i = 1; i < targets.size(); i++) {
                    targets.get(i).damage(dmg * 0.5, player);
                    spawnRingParticles(targets.get(i).getLocation(), 1, Particle.CRIT);
                }
                yield true;
            }

            case "hoagiam" -> { // Hoa Giam: cay leo troi buoc
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_VINE_BREAK, 1f, 0.8f);
                player.sendMessage(plugin.prefix() + "§a🌿 Hoa Giam! §7Cây leo ma thuật trói buộc!");
                for (Entity e : player.getNearbyEntities(4, 3, 4)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 100, 4, false, true, true));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("jump_boost")), 100, 128, false, true, true));
                    }
                }
                spawnRingParticles(player.getLocation(), 4, Particle.HAPPY_VILLAGER);
                yield true;
            }

            case "linhohonsuy" -> { // Linh Hon Suy: hut damage + speed
                double dmg = 10.0 * dmgMult;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1f, 0.7f);
                player.sendMessage(plugin.prefix() + "§5💀 Linh Hồn Suy! §7Hút linh lực kẻ thù!");
                for (Entity e : player.getNearbyEntities(5, 4, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg, player);
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("slowness")), 100, 1, false, true, true));
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("weakness")), 100, 1, false, true, true));
                    }
                }
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 100, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("strength")), 100, 0, false, true, true));
                spawnRingParticles(player.getLocation(), 5, Particle.SOUL);
                yield true;
            }

            case "thienmachung" -> { // Thien Ma Chung: nausea + danh nhau
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.5f, 0.5f);
                player.sendMessage(plugin.prefix() + "§4🔔 Thiên Ma Chung! §7Âm thanh ma đạo vang lên!");
                List<LivingEntity> affected = new ArrayList<>();
                for (Entity e : player.getNearbyEntities(8, 5, 8)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByKey(NamespacedKey.minecraft("nausea")), 80, 3, false, true, true));
                        affected.add(le);
                    }
                }
                if (affected.size() >= 2) {
                    new BukkitRunnable() {
                        int t = 0;
                        @Override public void run() {
                            if (t++ >= 4 || affected.size() < 2) { cancel(); return; }
                            LivingEntity a = affected.get(random.nextInt(affected.size()));
                            LivingEntity b = affected.get(random.nextInt(affected.size()));
                            if (a != b && a.isValid() && b.isValid()) a.damage(4.0, b);
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                }
                spawnRingParticles(player.getLocation(), 8, Particle.SOUL);
                yield true;
            }

            case "honphach" -> { // Hon Phach Tinh: hut tinh linh hon, hoi HP+LK ngay
                double healAmt = player.getMaxHealth() * 0.15;
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healAmt));
                data.setLinhKhi(data.getLinhKhi() + 50);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1f);
                player.sendMessage(plugin.prefix() + "§d💠 Hồn Phách Tinh! §aHoi 15% HP §7+ §b+50 LK!");
                spawnRingParticles(player.getLocation(), 1.5, Particle.SOUL);
                yield true;
            }

            // ── PHAP BAO BAN MENH (5 loai) — tang chi so vinh vien, gioi han so lan ──

            case "banmenhkiem" -> { // ATK +5%/lan, toi da 20 lan (+100%)
                int lvl = data.getExtraInt("banmenh_kiem_lvl", 0);
                if (lvl >= 20) { player.sendMessage(plugin.prefix() + "§cBản Mệnh Kiếm đã đạt giới hạn (+100% ATK)!"); yield false; }
                data.setExtraInt("banmenh_kiem_lvl", lvl + 1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.3f, 1.3f);
                player.sendMessage(plugin.prefix() + "§c⚔ Bản Mệnh Kiếm! §aATK +5% vĩnh viễn! §7(" + (lvl + 1) + "/20 - Tổng: +" + ((lvl + 1) * 5) + "%)");
                spawnRingParticles(player.getLocation(), 1.5, Particle.CRIT);
                yield true;
            }

            case "banmenhgiap" -> { // DEF +3%/lan, toi da 20 lan (+60%)
                int lvl = data.getExtraInt("banmenh_giap_lvl", 0);
                if (lvl >= 20) { player.sendMessage(plugin.prefix() + "§cBản Mệnh Giáp đã đạt giới hạn (+60% DEF)!"); yield false; }
                data.setExtraInt("banmenh_giap_lvl", lvl + 1);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.2f, 1f);
                player.sendMessage(plugin.prefix() + "§7🛡 Bản Mệnh Giáp! §aDEF +3% vĩnh viễn! §7(" + (lvl + 1) + "/20 - Tổng: +" + ((lvl + 1) * 3) + "%)");
                spawnRingParticles(player.getLocation(), 1.5, Particle.END_ROD);
                yield true;
            }

            case "banmenhphong" -> { // Bay tu do 10 giay
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByKey(NamespacedKey.minecraft("speed")), 200, 2, false, true, true));
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1f, 1.2f);
                player.sendMessage(plugin.prefix() + "§b🪽 Bản Mệnh Phong! §7Cánh linh khí mở ra - bay tự do 10 giây!");
                spawnRingParticles(player.getLocation(), 1.5, Particle.CLOUD);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.getGameMode() != GameMode.CREATIVE
                            && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                    }
                }, 200L);
                yield true;
            }

            case "banmenthan" -> { // Ban Menh An: hoi HP + xoa 1 debuff ngay
                double heal = player.getMaxHealth() * 0.10;
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
                    String k = eff.getType().getKey().getKey();
                    if (k.equals("poison") || k.equals("wither") || k.equals("slowness")
                        || k.equals("weakness") || k.equals("nausea") || k.equals("blindness") || k.equals("hunger")) {
                        player.removePotionEffect(eff.getType());
                        break;
                    }
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.3f);
                player.sendMessage(plugin.prefix() + "§d💎 Bản Mệnh Ấn! §aHồi 10% HP §7+ §exóa 1 hiệu ứng xấu!");
                spawnRingParticles(player.getLocation(), 1.5, Particle.HAPPY_VILLAGER);
                yield true;
            }

            case "banmenhchau" -> { // LK regen +50%/lan, toi da 10 lan (+500%)
                int lvl = data.getExtraInt("banmenh_chau_lvl", 0);
                if (lvl >= 10) { player.sendMessage(plugin.prefix() + "§cBản Mệnh Châu đã đạt giới hạn (+500% LK regen)!"); yield false; }
                data.setExtraInt("banmenh_chau_lvl", lvl + 1);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1.2f);
                player.sendMessage(plugin.prefix() + "§6💧 Bản Mệnh Châu! §aLK regen +50% vĩnh viễn! §7(" + (lvl + 1) + "/10 - Tổng: +" + ((lvl + 1) * 50) + "%)");
                spawnRingParticles(player.getLocation(), 1.5, Particle.DRIPPING_WATER);
                yield true;
            }

            default -> {
                player.sendMessage(plugin.prefix() + "§cKỹ năng chưa được triển khai!");
                yield false;
            }
        };
    }

    // ========================
    //  PASSIVE BUFF CHECK
    // ========================

    /**
     * Lay pháp bảo dang cam tren tay chinh hoac off-hand.
     */
    public ItemStack getEquippedPhaBao(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isPhaBao(main)) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isPhaBao(off)) return off;
        return null;
    }

    /**
     * Lay ATK multiplier tu phap bao dang cam (passive) - da GOM ca he so
     * Cuong Hoa (+1~+10, xem PhaBaoEnhanceManager).
     */
    public double getPassiveAtkMult(Player player) {
        ItemStack pb = getEquippedPhaBao(player);
        if (pb == null) return 1.0;
        PhaBaoTier t = getTier(pb);
        double base = t != null ? t.getAtkMult() : 1.0;
        return base * plugin.getPhaBaoEnhanceManager().getBonusMultiplier(pb)
                * plugin.getPhaBaoSocketManager().getBonusMultiplier(pb, "ATK");
    }

    public double getPassiveDefMult(Player player) {
        ItemStack pb = getEquippedPhaBao(player);
        if (pb == null) return 1.0;
        PhaBaoTier t = getTier(pb);
        double base = t != null ? t.getDefMult() : 1.0;
        return base * plugin.getPhaBaoEnhanceManager().getBonusMultiplier(pb)
                * plugin.getPhaBaoSocketManager().getBonusMultiplier(pb, "DEF");
    }

    public double getPassiveHpMult(Player player) {
        ItemStack pb = getEquippedPhaBao(player);
        if (pb == null) return 1.0;
        PhaBaoTier t = getTier(pb);
        double base = t != null ? t.getHpMult() : 1.0;
        return base * plugin.getPhaBaoEnhanceManager().getBonusMultiplier(pb)
                * plugin.getPhaBaoSocketManager().getBonusMultiplier(pb, "HP");
    }

    // ========================
    //  HELPERS
    // ========================

    private LivingEntity getNearestEnemy(Player player, double range) {
        LivingEntity nearest = null;
        double nearestDist = range * range;
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity le && e != player) {
                double d = player.getLocation().distanceSquared(le.getLocation());
                if (d < nearestDist) { nearest = le; nearestDist = d; }
            }
        }
        return nearest;
    }

    private void spawnRingParticles(Location center, double r, Particle particle) {
        for (double a = 0; a < 360; a += 15) {
            double rad = Math.toRadians(a);
            Location p = center.clone().add(Math.cos(rad)*r, 0.05, Math.sin(rad)*r);
            center.getWorld().spawnParticle(particle, p, 3, 0.1, 0.1, 0.1, 0);
        }
    }

    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.2fM", n/1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }
}
