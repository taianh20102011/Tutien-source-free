package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * KHI LINH (Phap Bao nuoi nhu Pet) - lenh /khilinh.
 *
 * Cho Phap Bao "an" do rac (Material trong danh sach config
 * "phabao.khilinh.junk-items") de tich diem "Linh Tinh" (luu tren NBT cua
 * chinh item, KHONG phai tren PlayerData - Linh Tinh la cua rieng Phap Bao
 * do). Cang nhieu Linh Tinh cang mo khoa nhieu buoc:
 *
 *   Bac 1 (>= tier1): hao quang hat (particle aura) quanh nguoi cam.
 *   Bac 2 (>= tier2): tu dong hut item roi gan do vao tui (auto pickup).
 *   Bac 3 (>= tier3): tu dong tro giup tan cong quai vat gan nhat (tick nho).
 *
 * Vi hieu ung la lien tuc (particle/aura/auto-pickup/auto-attack), class
 * nay chay 1 BukkitRunnable rieng (giong cach PassiveSkillManager dang lam)
 * thay vi dua vao 1 event don le.
 */
public class PhaBaoKhiLinhManager {

    private final TuTienPlugin plugin;

    private static final String KEY_LINHTINH = "pb_linhtinh"; // INTEGER
    private static final String LORE_MARKER  = "§2✦ Khí Linh: ";

    public PhaBaoKhiLinhManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startPassiveLoop();
    }

    // ========================
    //  DIEM LINH TINH
    // ========================

    public int getSpirit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer v = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_LINHTINH), PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    private void setSpirit(ItemStack item, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_LINHTINH), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        refreshKhiLinhLore(item);
    }

    public int getTier1() { return plugin.getConfig().getInt("phabao.khilinh.tier1-points", 100); }
    public int getTier2() { return plugin.getConfig().getInt("phabao.khilinh.tier2-points", 300); }
    public int getTier3() { return plugin.getConfig().getInt("phabao.khilinh.tier3-points", 600); }

    public int getTierUnlocked(ItemStack item) {
        int sp = getSpirit(item);
        if (sp >= getTier3()) return 3;
        if (sp >= getTier2()) return 2;
        if (sp >= getTier1()) return 1;
        return 0;
    }

    public Set<Material> getJunkMaterials() {
        Set<Material> set = new HashSet<>();
        List<String> names = plugin.getConfig().getStringList("phabao.khilinh.junk-items");
        if (names.isEmpty()) {
            set.addAll(EnumSet.of(Material.ROTTEN_FLESH, Material.STRING, Material.BONE,
                    Material.GUNPOWDER, Material.SPIDER_EYE, Material.RABBIT_HIDE,
                    Material.FEATHER, Material.INK_SAC));
        } else {
            for (String n : names) {
                try { set.add(Material.valueOf(n.trim().toUpperCase())); } catch (Exception ignored) {}
            }
        }
        return set;
    }

    public int getPointsPerItem() {
        return plugin.getConfig().getInt("phabao.khilinh.points-per-item", 2);
    }

    /** Tru bot diem Linh Tinh cua 1 Phap Bao (dung boi PhaBaoDoatBaoListener khi chet trong vung PVP). */
    public void reduceSpirit(ItemStack item, int amount) {
        int updated = Math.max(0, getSpirit(item) - amount);
        setSpirit(item, updated);
    }

    public enum FeedResult { OK, NOT_PHABAO, NOT_JUNK, EMPTY }

    /** Cho item (item.getAmount() lan) an vao phapBao, tra ve so diem da them (0 neu that bai). */
    public FeedResult feed(ItemStack phapBao, ItemStack junk) {
        if (!plugin.getPhaBaoManager().isPhaBao(phapBao)) return FeedResult.NOT_PHABAO;
        if (junk == null || junk.getType() == Material.AIR || junk.getAmount() <= 0) return FeedResult.EMPTY;
        if (!getJunkMaterials().contains(junk.getType())) return FeedResult.NOT_JUNK;

        int gained = junk.getAmount() * getPointsPerItem();
        setSpirit(phapBao, getSpirit(phapBao) + gained);
        return FeedResult.OK;
    }

    // ========================
    //  LORE
    // ========================

    public void refreshKhiLinhLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l != null && l.startsWith(LORE_MARKER));

        int sp = getSpirit(item);
        if (sp > 0) {
            int tier = getTierUnlocked(item);
            lore.add(Math.min(1, lore.size()), LORE_MARKER + "§a" + sp + " §7điểm §7(Bậc §2" + tier + "§7/3)");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ========================
    //  VONG LAP HIEU UNG THU DONG
    // ========================

    private void startPassiveLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack pb = plugin.getPhaBaoManager().getEquippedPhaBao(player);
                    if (pb == null) continue;
                    int tier = getTierUnlocked(pb);
                    if (tier <= 0) continue;

                    Location loc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, loc, 3, 0.4, 0.6, 0.4, 0.01);

                    if (tier >= 2) autoPickup(player);
                    if (tier >= 3) autoAssist(player);
                }
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }

    private void autoPickup(Player player) {
        double radius = plugin.getConfig().getDouble("phabao.khilinh.pickup-radius", 4.0);
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Item itemEntity) || !itemEntity.isValid()) continue;
            ItemStack stack = itemEntity.getItemStack();
            var leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty()) {
                itemEntity.remove();
            } else if (leftover.values().iterator().next().getAmount() < stack.getAmount()) {
                itemEntity.remove();
                for (ItemStack extra : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
        }
    }

    private void autoAssist(Player player) {
        double radius = plugin.getConfig().getDouble("phabao.khilinh.assist-radius", 6.0);
        double dmg = plugin.getConfig().getDouble("phabao.khilinh.assist-damage", 1.0);
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Monster monster) || !monster.isValid() || monster.isDead()) continue;
            double d = monster.getLocation().distanceSquared(player.getLocation());
            if (d < best) { best = d; target = monster; }
        }
        if (target != null) {
            target.damage(dmg, player);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.05);
        }
    }
}
