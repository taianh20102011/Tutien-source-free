package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * KHAM NAM (Socketing) cho Phap Bao.
 *
 * - So o kham (socket) TOI DA phu thuoc TIER cua Phap Bao (cang cao tier
 *   cang nhieu o), doc tu config "phabao.socket.max-sockets.<TEN_TIER>".
 * - Moi o kham 1 vien Ngoc (Lua/Bang/Loi) -> cong THEM VINH VIEN % vao
 *   dung 1 trong 3 chi so ATK/DEF/HP (xem GemType#getStatCode). Cong don
 *   khi kham nhieu vien CUNG loai vao 1 Phap Bao.
 * - Kham la VINH VIEN, KHONG the go Ngoc ra lai (dung y "tang chi so vinh
 *   vien" trong yeu cau ban dau) - tranh phai lam them co che thao/farm
 *   ngoc lai gay phuc tap khong can thiet.
 * - Du lieu luu bang PersistentDataContainer (NBT) tren item, dang 1
 *   STRING danh sach ma GemType cach nhau boi dau phay (vd "HOA,HOA,BANG").
 */
public class PhaBaoSocketManager {

    private final TuTienPlugin plugin;

    private static final String KEY_SOCKETS = "pb_sockets"; // STRING: "HOA,BANG,LOI"
    private static final String KEY_GEM_TYPE = "pb_gem_type";
    private static final String SOCKET_LORE_MARKER = "§b✦ Khảm Nạm: ";

    public PhaBaoSocketManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  SO LUONG O KHAM THEO TIER
    // ========================

    public int getMaxSockets(ItemStack item) {
        PhaBaoTier tier = plugin.getPhaBaoManager().getTier(item);
        if (tier == null) return 0;
        int def = switch (tier) {
            case DIA_PHAM, HA_PHAM -> 1;
            case TRUNG_PHAM, THUONG_PHAM -> 2;
            case DE_PHAM, TIEN_PHAM -> 3;
        };
        return plugin.getConfig().getInt("phabao.socket.max-sockets." + tier.name(), def);
    }

    public List<GemType> getFilledGems(ItemStack item) {
        List<GemType> out = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return out;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_SOCKETS), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split(",")) {
            try { out.add(GemType.valueOf(part.trim())); } catch (Exception ignored) {}
        }
        return out;
    }

    public boolean hasEmptySocket(ItemStack item) {
        return getFilledGems(item).size() < getMaxSockets(item);
    }

    /** % cong them CHO 1 CHI SO cu the (vd "ATK") tu tat ca ngoc cung loai da kham. */
    public double getBonusMultiplier(ItemStack item, String statCode) {
        int count = 0;
        for (GemType g : getFilledGems(item)) if (g.getStatCode().equals(statCode)) count++;
        if (count == 0) return 1.0;
        double perGem = plugin.getConfig().getDouble("phabao.socket.bonus-per-gem", 0.05);
        return 1.0 + count * perGem;
    }

    // ========================
    //  KHAM NGOC
    // ========================

    public enum Result { SUCCESS, NO_SOCKET, NOT_PHABAO, NO_GEM }

    /**
     * Kham 1 vien Ngoc (gemType) vao "item" (Phap Bao dang cam) - TIEU 1
     * vien Ngoc tuong ung trong tui do nguoi choi (khong can dat vao GUI).
     */
    public Result socketGem(Player player, ItemStack item, GemType gemType) {
        if (!plugin.getPhaBaoManager().isPhaBao(item)) return Result.NOT_PHABAO;
        if (!hasEmptySocket(item)) return Result.NO_SOCKET;

        ItemStack gemInInv = findGem(player, gemType);
        if (gemInInv == null) return Result.NO_GEM;

        List<GemType> current = getFilledGems(item);
        current.add(gemType);

        ItemMeta meta = item.getItemMeta();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < current.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(current.get(i).name());
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_SOCKETS),
                PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
        refreshSocketLore(item);

        gemInInv.setAmount(gemInInv.getAmount() - 1);
        return Result.SUCCESS;
    }

    private ItemStack findGem(Player player, GemType type) {
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (getGemType(it) == type) return it;
        }
        return null;
    }

    public int countGem(Player player, GemType type) {
        int total = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (getGemType(it) == type) total += it.getAmount();
        }
        return total;
    }

    // ========================
    //  TAO/NHAN DIEN NGOC
    // ========================

    public ItemStack createGem(GemType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getColor() + "✦ " + type.getDisplayName());
        double perGem = plugin.getConfig().getDouble("phabao.socket.bonus-per-gem", 0.05);
        List<String> lore = new ArrayList<>();
        lore.add("§7Khảm §evĩnh viễn§7 vào Pháp Bảo còn ô trống");
        lore.add("§7(dùng lệnh §e/khamnam§7), cộng thêm");
        lore.add("§a+" + Math.round(perGem * 100) + "% §7" + type.getStatCode() + " §7vĩnh viễn.");
        lore.add("§8Không thể tháo ra sau khi đã khảm.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_GEM_TYPE),
                PersistentDataType.STRING, type.name());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public GemType getGemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String v = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, KEY_GEM_TYPE), PersistentDataType.STRING);
        if (v == null) return null;
        try { return GemType.valueOf(v); } catch (Exception e) { return null; }
    }

    // ========================
    //  LORE
    // ========================

    public void refreshSocketLore(ItemStack item) {
        if (!plugin.getPhaBaoManager().isPhaBao(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<GemType> gems = getFilledGems(item);
        int max = getMaxSockets(item);

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l != null && l.startsWith(SOCKET_LORE_MARKER));

        if (max > 0) {
            StringBuilder sb = new StringBuilder(SOCKET_LORE_MARKER);
            for (int i = 0; i < max; i++) {
                if (i < gems.size()) sb.append(gems.get(i).getColor()).append("● ");
                else sb.append("§8○ ");
            }
            lore.add(Math.min(1, lore.size()), sb.toString().trim());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
