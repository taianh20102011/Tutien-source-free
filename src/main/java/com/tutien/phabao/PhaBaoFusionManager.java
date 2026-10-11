package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * DUNG HOP PHAP BAO (Fusion) - lenh /dunghop.
 *
 * - Ghep 1 Phap Bao "goc" (giu lai) voi 1 Phap Bao "nguyen lieu" CUNG LOAI
 *   (tieu hao). Ket qua:
 *     1) GOP DONG AN: hop nhat (union) danh sach dong an cua 2 ben vao Phap
 *        Bao goc - LUON XAY RA neu thanh cong (khong random), toi da
 *        "phabao.fusion.max-affix" dong (mac dinh 2).
 *     2) THANG PHAM: co 1 ti le (random, giam dan theo tier - xem
 *        "phabao.fusion.upgrade-chance.<TIER>") de Phap Bao goc THANG 1
 *        BAC (vd Trung Pham -> Thuong Pham), giu nguyen Cuong Hoa/Kham Nam
 *        da co tren item (khong tao lai item moi tu dau).
 * - Ton phi Linh Thach, doc tu "phabao.fusion.cost-tien-base/per-tier".
 *
 * Ngoai ra class nay con quan ly he thong "DONG AN" (hidden affix) noi
 * chung: roll ngau nhien khi Phap Bao roi ra (xem
 * PhaBaoManager#randomPhaBao qua maybeRollAffix) va proc hieu ung khi nguoi
 * choi danh trung don thuong bang Phap Bao co dong an (xem onHit()).
 */
public class PhaBaoFusionManager implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    private static final String KEY_AFFIXES = "pb_affixes"; // STRING: "HUT_MAU,DOT_CHAY"
    private static final String AFFIX_LORE_MARKER = "§5✦ Dòng Ẩn: ";

    public PhaBaoFusionManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  DOC/GHI DONG AN
    // ========================

    public List<PhaBaoAffix> getAffixes(ItemStack item) {
        List<PhaBaoAffix> out = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return out;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_AFFIXES), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split(",")) {
            try { out.add(PhaBaoAffix.valueOf(part.trim())); } catch (Exception ignored) {}
        }
        return out;
    }

    private void setAffixes(ItemStack item, List<PhaBaoAffix> affixes) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < affixes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(affixes.get(i).name());
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_AFFIXES),
                PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
        refreshAffixLore(item);
    }

    public int getMaxAffix() {
        return Math.max(1, plugin.getConfig().getInt("phabao.fusion.max-affix", 2));
    }

    /**
     * Goi sau khi 1 Phap Bao MOI duoc tao ra (vd PhaBaoManager#randomPhaBao)
     * de co 1 co hoi nho duoc cong san 1 dong an ngau nhien. Chi roll neu
     * item CHUA co dong an nao (tranh cong don qua de dang ngoai Dung Hop).
     */
    public void maybeRollAffix(ItemStack item) {
        if (!plugin.getPhaBaoManager().isPhaBao(item)) return;
        if (!getAffixes(item).isEmpty()) return;
        double chance = plugin.getConfig().getDouble("phabao.fusion.roll-affix-chance", 0.15);
        if (random.nextDouble() >= chance) return;
        PhaBaoAffix[] all = PhaBaoAffix.values();
        PhaBaoAffix picked = all[random.nextInt(all.length)];
        List<PhaBaoAffix> list = new ArrayList<>();
        list.add(picked);
        setAffixes(item, list);
    }

    // ========================
    //  DUNG HOP
    // ========================

    public enum Result { SUCCESS_UPGRADE, SUCCESS_NO_UPGRADE, DIFFERENT_TYPE, NOT_PHABAO, NOT_ENOUGH_MONEY, MAX_TIER }

    public long getCost(PhaBaoTier tier) {
        long base = plugin.getConfig().getLong("phabao.fusion.cost-tien-base", 1000);
        long per = plugin.getConfig().getLong("phabao.fusion.cost-tien-per-tier", 500);
        return base + per * (long) tier.ordinal();
    }

    public double getUpgradeChance(PhaBaoTier tier) {
        double def = switch (tier) {
            case DIA_PHAM -> 0.80; case HA_PHAM -> 0.65; case TRUNG_PHAM -> 0.50;
            case THUONG_PHAM -> 0.35; case DE_PHAM -> 0.20; default -> 0.0;
        };
        return plugin.getConfig().getDouble("phabao.fusion.upgrade-chance." + tier.name(), def);
    }

    /**
     * base   = Phap Bao GIU LAI (se duoc tra ve tay nguoi choi, co the len tier).
     * material = Phap Bao TIEU HAO (mat vinh vien du ket qua the nao).
     * Ca 2 phai la Phap Bao CUNG PhaBaoType. Khong sua "material" - viec xoa
     * item nay khoi GUI/tui do do noi goi (PhaBaoFusionGUI) tu xu ly.
     */
    public Result attemptFusion(Player player, ItemStack base, ItemStack material) {
        PhaBaoManager pbm = plugin.getPhaBaoManager();
        if (!pbm.isPhaBao(base) || !pbm.isPhaBao(material)) return Result.NOT_PHABAO;

        PhaBaoType baseType = pbm.getType(base);
        PhaBaoType matType  = pbm.getType(material);
        if (baseType == null || matType == null || baseType != matType) return Result.DIFFERENT_TYPE;

        PhaBaoTier curTier = pbm.getTier(base);
        if (curTier == null) return Result.NOT_PHABAO;

        long cost = getCost(curTier);
        if (!plugin.getTienManager().spend(player, cost)) return Result.NOT_ENOUGH_MONEY;

        // 1) GOP DONG AN - luon xay ra, khong random
        List<PhaBaoAffix> merged = mergeAffixes(getAffixes(base), getAffixes(material), getMaxAffix());
        setAffixes(base, merged);

        // 2) THANG PHAM - random, chi khi chua o tier cao nhat
        if (curTier == PhaBaoTier.TIEN_PHAM) return Result.MAX_TIER;

        double chance = getUpgradeChance(curTier);
        if (random.nextDouble() < chance) {
            PhaBaoTier next = PhaBaoTier.values()[curTier.ordinal() + 1];
            upgradeTierInPlace(base, baseType, next);
            return Result.SUCCESS_UPGRADE;
        }
        return Result.SUCCESS_NO_UPGRADE;
    }

    private List<PhaBaoAffix> mergeAffixes(List<PhaBaoAffix> a, List<PhaBaoAffix> b, int max) {
        LinkedHashSet<PhaBaoAffix> set = new LinkedHashSet<>();
        set.addAll(a);
        set.addAll(b);
        List<PhaBaoAffix> out = new ArrayList<>(set);
        if (out.size() > max) {
            java.util.Collections.shuffle(out, random);
            out = new ArrayList<>(out.subList(0, max));
        }
        return out;
    }

    /**
     * Nang tier CUA CHINH ITEM DANG CO (khong tao ItemStack moi) de GIU
     * NGUYEN moi du lieu NBT khac da co tren item (Cuong Hoa, Kham Nam,
     * Dong An, ten tuy chinh...). Dung lai chinh khoa NBT "pb_tier" ma
     * PhaBaoManager dung (cung ten key + cung plugin => cung 1
     * NamespacedKey), nen khong can sua PhaBaoManager.java.
     */
    private void upgradeTierInPlace(ItemStack item, PhaBaoType type, PhaBaoTier newTier) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "pb_tier"),
                PersistentDataType.STRING, newTier.name());

        String name = newTier.getColor() + newTier.getIcon() + " ["
                + newTier.getDisplayName() + "] " + type.getColor() + type.getDisplayName();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§8════════════════════════");
        lore.add("§7" + type.getDescription());
        lore.add("§8────────────────────────");
        lore.add("§8════════════════════════");
        lore.add("  " + newTier.getColoredName());
        lore.add("§e⚔ ATK: §cx" + String.format("%.2f", newTier.getAtkMult()));
        lore.add("§e🛡 DEF: §bx" + String.format("%.2f", newTier.getDefMult()));
        lore.add("§e❤ HP:  §ax" + String.format("%.2f", newTier.getHpMult()));
        lore.add("§8────────────────────────");
        lore.add("§6✦ Kỹ Năng: §f" + type.getAbilityDesc());
        lore.add("§aCooldown: §f" + (newTier.getAbilityCooldown() / 1000) + "s");
        lore.add("§8════════════════════════");
        lore.add("§a► Shift + Chuột Phải §7để dùng kỹ năng");
        lore.add("§7Cầm trên tay chính hoặc off-hand để nhận buff.");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS);
        item.setItemMeta(meta);

        // Ve lai cac dong lore rieng cua tung he thong (doc lai NBT rieng
        // cua chung, khong bi anh huong boi viec ta vua reset lore o tren).
        plugin.getPhaBaoEnhanceManager().refreshEnhanceLore(item);
        plugin.getPhaBaoSocketManager().refreshSocketLore(item);
        refreshAffixLore(item);
    }

    // ========================
    //  LORE
    // ========================

    public void refreshAffixLore(ItemStack item) {
        if (!plugin.getPhaBaoManager().isPhaBao(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<PhaBaoAffix> affixes = getAffixes(item);
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l != null && l.startsWith(AFFIX_LORE_MARKER));

        if (!affixes.isEmpty()) {
            StringBuilder sb = new StringBuilder(AFFIX_LORE_MARKER);
            for (int i = 0; i < affixes.size(); i++) {
                if (i > 0) sb.append("§7, ");
                sb.append(affixes.get(i).getColoredName());
            }
            lore.add(Math.min(1, lore.size()), sb.toString());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ========================
    //  PROC KHI DANH TRUNG
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack pb = plugin.getPhaBaoManager().getEquippedPhaBao(attacker);
        if (pb == null) return;
        List<PhaBaoAffix> affixes = getAffixes(pb);
        if (affixes.isEmpty()) return;

        double finalDamage = event.getFinalDamage();

        for (PhaBaoAffix affix : affixes) {
            double chance = plugin.getConfig().getDouble("phabao.affix." + affix.name() + ".chance", affix.getDefaultChance());
            if (random.nextDouble() >= chance) continue;
            double value = plugin.getConfig().getDouble("phabao.affix." + affix.name() + ".value", affix.getDefaultValue());

            switch (affix) {
                case HUT_MAU -> {
                    double heal = finalDamage * value;
                    double newHp = Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal);
                    attacker.setHealth(newHp);
                    attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 4, 0.3, 0.3, 0.3, 0);
                }
                case DOT_CHAY -> {
                    victim.setFireTicks(Math.max(victim.getFireTicks(), (int) (value * 20)));
                }
                case BANG_GIA -> {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true));
                    victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                }
                case CHOANG_VANG -> {
                    int ticks = (int) (value * 20);
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, true, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 3, true, true));
                    victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 2, 0), 12, 0.3, 0.3, 0.3, 0.1);
                }
                case CHI_MANG -> {
                    // Dung damage(double) 1 tham so (khong truyen nguon) de
                    // KHONG kich hoat lai EntityDamageByEntityEvent -> tranh
                    // vong lap de quy proc lai chinh no.
                    victim.damage(finalDamage * value);
                    victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.2, 0), 15, 0.3, 0.3, 0.3, 0.2);
                    attacker.playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                }
                case THAN_TOC -> {
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (value * 20), 1, true, true));
                }
            }
        }
    }
}
