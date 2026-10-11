package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI Linh Thuc - cung phong cach "click-to-craft" nhu PhaBaoEnhanceGUI:
 * Phap Bao van nam tren tay chinh nguoi choi, KHONG can dat vao GUI.
 */
public class PhaBaoLinhThucGUI implements Listener {

    private final TuTienPlugin plugin;
    public static final String TITLE = "§9✦ Linh Thức";

    private static final int SLOT_ITEM     = 13;
    private static final int SLOT_HOTHE    = 20;
    private static final int SLOT_PHANKICH = 22;
    private static final int SLOT_HOACANH  = 24;
    private static final int SLOT_CLOSE    = 31;

    public PhaBaoLinhThucGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getPhaBaoManager().isPhaBao(held)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm 1 Pháp Bảo ở tay chính để gán Linh Thức.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 36; i++) inv.setItem(i, bg);

        inv.setItem(SLOT_ITEM, held.clone());

        PhaBaoLinhThucManager ltm = plugin.getPhaBaoLinhThucManager();
        long cost = ltm.getImbueCost();
        PhaBaoLinhThucManager.LinhThuc current = ltm.getLinhThuc(held);

        inv.setItem(SLOT_HOTHE, skillBtn(Material.SHIELD, PhaBaoLinhThucManager.LinhThuc.HO_THE, current, cost));
        inv.setItem(SLOT_PHANKICH, skillBtn(Material.IRON_SWORD, PhaBaoLinhThucManager.LinhThuc.PHAN_KICH, current, cost));
        inv.setItem(SLOT_HOACANH, skillBtn(Material.BLAZE_POWDER, PhaBaoLinhThucManager.LinhThuc.HOA_CANH, current, cost));

        inv.setItem(SLOT_CLOSE, btn(Material.BARRIER, "§c✘ Đóng", ""));
        player.openInventory(inv);
    }

    private ItemStack skillBtn(Material mat, PhaBaoLinhThucManager.LinhThuc lt,
                                PhaBaoLinhThucManager.LinhThuc current, long cost) {
        boolean active = lt == current;
        List<String> lore = new java.util.ArrayList<>();
        lore.add("§7" + lt.desc);
        lore.add("");
        if (active) {
            lore.add("§a✓ Đang được gán trên Pháp Bảo này");
        } else {
            lore.add("§7Phí gán: §6" + plugin.getTienManager().fmt(cost) + " " + plugin.getTienManager().tenTien());
            lore.add("§eBấm để gán (thay thế Linh Thức cũ nếu có)");
        }
        return btn(mat, (active ? "§a✔ " : lt.color + "✦ ") + lt.displayName, lore.toArray(new String[0]));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || !isOur(e.getView())) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();

        PhaBaoLinhThucManager.LinhThuc chosen = switch (slot) {
            case SLOT_HOTHE -> PhaBaoLinhThucManager.LinhThuc.HO_THE;
            case SLOT_PHANKICH -> PhaBaoLinhThucManager.LinhThuc.PHAN_KICH;
            case SLOT_HOACANH -> PhaBaoLinhThucManager.LinhThuc.HOA_CANH;
            default -> null;
        };

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }
        if (chosen == null) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getPhaBaoManager().isPhaBao(held)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm 1 Pháp Bảo ở tay chính.");
            player.closeInventory();
            return;
        }

        if (plugin.getPhaBaoLinhThucManager().getLinhThuc(held) == chosen) {
            player.sendMessage(plugin.prefix() + "§7Pháp Bảo đã có Linh Thức này rồi.");
            return;
        }

        if (plugin.getPhaBaoLinhThucManager().imbue(player, held, chosen)) {
            player.sendMessage(plugin.prefix() + "§9✦ Đã gán Linh Thức: " + chosen.getColoredName());
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
            open(player);
        } else {
            player.sendMessage(plugin.prefix() + "§cKhông đủ " + plugin.getTienManager().tenTien() + " để gán Linh Thức.");
        }
    }

    private boolean isOur(InventoryView v) {
        try {
            return LegacyComponentSerializer.legacySection().serialize(v.title()).equals(TITLE);
        } catch (Exception ex) {
            return false;
        }
    }

    private ItemStack btn(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
