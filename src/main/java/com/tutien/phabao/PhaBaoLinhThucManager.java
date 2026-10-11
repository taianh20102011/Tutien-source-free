package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.zone.ZoneManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * LINH THUC / SKILL NGU CANH - lenh /linhthuc.
 *
 * Nguoi choi gan (imbue) 1 TRONG 3 "Linh Thuc" (ky nang tu dong kich hoat
 * theo tinh huong chien dau) len 1 Phap Bao dang cam tren tay:
 *
 *   1) HO THE CHI THUAN : Tu dong giam sat thuong nhan vao khi HP < 20%.
 *   2) NGHICH LAN PHAN KICH : Tu dong phan don sat thuong khi bi tan cong
 *      TU PHIA SAU (danh len).
 *   3) HOA CANH CHI UY : Tang % sat thuong gay ra khi dang o trong 1 vung
 *      (Zone, xem ZoneManager) co TEN chua chu "Hỏa" VA Phap Bao dang cam
 *      da duoc Kham Nam Ngoc Hoa (GemType.HOA).
 *
 * Moi Linh Thuc deu co cooldown rieng (dung chung co che addBuff/hasBuff
 * cua PlayerData, giong cach PhaBaoManager dang dung cho cooldown ky nang
 * chinh cua Phap Bao).
 */
public class PhaBaoLinhThucManager implements Listener {

    private final TuTienPlugin plugin;

    private static final String KEY_LINHTHUC = "pb_linhthuc"; // STRING: HO_THE | PHAN_KICH | HOA_CANH

    public enum LinhThuc {
        HO_THE   ("Hộ Thể Chi Thuẫn", "§b", "Tự động giảm sát thương khi HP < 20% (có hồi chiêu)."),
        PHAN_KICH("Nghịch Lân Phản Kích", "§c", "Tự động phản đòn khi bị tấn công từ phía sau."),
        HOA_CANH ("Hỏa Cảnh Chi Uy", "§6", "Tăng sát thương khi đứng trong vùng có tên chứa \"Hỏa\" (cần khảm Ngọc Hỏa).");

        public final String displayName, color, desc;
        LinhThuc(String displayName, String color, String desc) {
            this.displayName = displayName; this.color = color; this.desc = desc;
        }
        public String getColoredName() { return color + "✦ " + displayName; }
    }

    public PhaBaoLinhThucManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  DOC/GHI
    // ========================

    public LinhThuc getLinhThuc(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_LINHTHUC), PersistentDataType.STRING);
        if (raw == null) return null;
        try { return LinhThuc.valueOf(raw); } catch (Exception ex) { return null; }
    }

    public long getImbueCost() {
        return plugin.getConfig().getLong("phabao.linhthuc.cost-tien", 2000);
    }

    /** Gan (hoac thay doi) Linh Thuc len 1 Phap Bao dang cam. Tra ve true neu thanh cong. */
    public boolean imbue(Player player, ItemStack item, LinhThuc linhThuc) {
        PhaBaoManager pbm = plugin.getPhaBaoManager();
        if (!pbm.isPhaBao(item)) return false;
        long cost = getImbueCost();
        if (!plugin.getTienManager().spend(player, cost)) return false;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_LINHTHUC),
                PersistentDataType.STRING, linhThuc.name());
        item.setItemMeta(meta);
        refreshLinhThucLore(item);
        return true;
    }

    private static final String LORE_MARKER = "§9✦ Linh Thức: ";

    public void refreshLinhThucLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        java.util.List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        lore.removeIf(l -> l != null && l.startsWith(LORE_MARKER));
        LinhThuc lt = getLinhThuc(item);
        if (lt != null) {
            lore.add(Math.min(1, lore.size()), LORE_MARKER + lt.getColoredName());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ========================
    //  KICH HOAT NGU CANH
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // ---- Phia PHONG THU (Ho The / Phan Kich) ----
        if (event.getEntity() instanceof Player defender) {
            ItemStack pb = plugin.getPhaBaoManager().getEquippedPhaBao(defender);
            LinhThuc lt = getLinhThuc(pb);
            if (lt == LinhThuc.HO_THE) {
                handleHoThe(defender, event);
            } else if (lt == LinhThuc.PHAN_KICH && event.getDamager() instanceof LivingEntity attacker) {
                handlePhanKich(defender, attacker, event);
            }
        }

        // ---- Phia TAN CONG (Hoa Canh) ----
        if (event.getDamager() instanceof Player attacker) {
            ItemStack pb = plugin.getPhaBaoManager().getEquippedPhaBao(attacker);
            if (getLinhThuc(pb) == LinhThuc.HOA_CANH) {
                handleHoaCanh(attacker, pb, event);
            }
        }
    }

    private void handleHoThe(Player defender, EntityDamageByEntityEvent event) {
        PlayerData data = plugin.getDataManager().get(defender);
        if (data == null) return;
        String cdKey = "linhthuc_hothe_cd";
        if (data.hasBuff(cdKey)) return;

        double threshold = plugin.getConfig().getDouble("phabao.linhthuc.hothe.hp-percent", 0.20);
        double afterHp = defender.getHealth() - event.getFinalDamage();
        if (afterHp > defender.getMaxHealth() * threshold) return;
        if (afterHp <= 0) return; // dang chet that su thi khong "cuu"

        double reduce = plugin.getConfig().getDouble("phabao.linhthuc.hothe.reduce-percent", 0.5);
        long cooldownMs = plugin.getConfig().getLong("phabao.linhthuc.hothe.cooldown-ms", 30000);

        event.setDamage(event.getDamage() * (1 - reduce));
        data.addBuff(cdKey, cooldownMs);

        defender.sendMessage(plugin.prefix() + "§b✦ Hộ Thể Chi Thuẫn kích hoạt! §7Giảm " + Math.round(reduce * 100) + "% sát thương.");
        defender.playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.4f);
        defender.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, defender.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.3);
    }

    private void handlePhanKich(Player defender, LivingEntity attacker, EntityDamageByEntityEvent event) {
        PlayerData data = plugin.getDataManager().get(defender);
        if (data == null) return;
        String cdKey = "linhthuc_phankich_cd";
        if (data.hasBuff(cdKey)) return;

        if (!isBackstab(defender, attacker)) return;

        long cooldownMs = plugin.getConfig().getLong("phabao.linhthuc.phankich.cooldown-ms", 20000);
        double reflectPercent = plugin.getConfig().getDouble("phabao.linhthuc.phankich.reflect-percent", 0.6);
        data.addBuff(cdKey, cooldownMs);

        double reflect = event.getFinalDamage() * reflectPercent;
        if (reflect > 0) {
            // damage(double) 1 tham so - khong kich hoat lai EntityDamageByEntityEvent (tranh vong lap)
            attacker.damage(reflect);
        }
        defender.sendMessage(plugin.prefix() + "§c✦ Nghịch Lân Phản Kích! §7Phản lại " + Math.round(reflectPercent * 100) + "% sát thương.");
        defender.playSound(defender.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
        defender.getWorld().spawnParticle(Particle.SWEEP_ATTACK, attacker.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
    }

    private boolean isBackstab(Player defender, LivingEntity attacker) {
        Vector facing = defender.getLocation().getDirection().setY(0).normalize();
        Vector toAttacker = attacker.getLocation().toVector().subtract(defender.getLocation().toVector()).setY(0);
        if (toAttacker.lengthSquared() < 1.0E-4) return false;
        toAttacker.normalize();
        double dot = facing.dot(toAttacker);
        double maxDot = plugin.getConfig().getDouble("phabao.linhthuc.phankich.max-dot", -0.3);
        return dot < maxDot; // dot < 0 nghia la o phia sau (goc > 90 do)
    }

    private void handleHoaCanh(Player attacker, ItemStack pb, EntityDamageByEntityEvent event) {
        List<GemType> gems = plugin.getPhaBaoSocketManager().getFilledGems(pb);
        if (!gems.contains(GemType.HOA)) return;

        ZoneManager.Zone zone = plugin.getZoneManager().getZoneAt(attacker.getLocation());
        if (zone == null || zone.name == null) return;
        if (!zone.name.toLowerCase().contains("hỏa") && !zone.name.toLowerCase().contains("hoa")) return;

        double buff = plugin.getConfig().getDouble("phabao.linhthuc.hoacanh.buff-percent", 0.30);
        event.setDamage(event.getDamage() * (1 + buff));
        attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.01);
    }
}
