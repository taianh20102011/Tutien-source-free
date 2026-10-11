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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI Kham Nam - cung phong cach click-to-craft nhu PhaBaoEnhanceGUI (khong
 * drag-drop item vao GUI, Ngoc/Phap Bao van nam trong tui/tay nguoi choi).
 */
public class PhaBaoSocketGUI implements Listener {

    private final TuTienPlugin plugin;
    public static final String TITLE = "§b✦ Khảm Nạm Pháp Bảo";

    private static final int SLOT_ITEM  = 13;
    private static final int SLOT_HOA   = 20;
    private static final int SLOT_BANG  = 22;
    private static final int SLOT_LOI   = 24;
    private static final int SLOT_CLOSE = 31;

    public PhaBaoSocketGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getPhaBaoManager().isPhaBao(held)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm 1 Pháp Bảo ở tay chính để Khảm Nạm.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 36; i++) inv.setItem(i, bg);

        PhaBaoSocketManager sm = plugin.getPhaBaoSocketManager();
        List<GemType> gems = sm.getFilledGems(held);
        int max = sm.getMaxSockets(held);

        inv.setItem(SLOT_ITEM, held.clone());

        if (max == 0) {
            inv.setItem(20, btn(Material.BARRIER, "§cPháp Bảo này không có ô khảm.", ""));
        } else if (gems.size() >= max) {
            inv.setItem(20, btn(Material.BARRIER, "§eĐã khảm đủ §f" + max + "/" + max + " §eô!", ""));
        } else {
            for (GemType type : GemType.values()) {
                int slot = switch (type) { case HOA -> SLOT_HOA; case BANG -> SLOT_BANG; case LOI -> SLOT_LOI; };
                int have = sm.countGem(player, type);
                double perGem = plugin.getConfig().getDouble("phabao.socket.bonus-per-gem", 0.05);
                inv.setItem(slot, btn(have > 0 ? type.getMaterial() : Material.GRAY_DYE,
                        type.getColor() + "✦ " + type.getDisplayName() + " §7(" + have + " viên)",
                        "§7Cộng vĩnh viễn §a+" + Math.round(perGem * 100) + "% §7" + type.getStatCode(),
                        "§7Ô đã khảm: §f" + gems.size() + "/" + max,
                        have > 0 ? "§a▶ Bấm để khảm" : "§cKhông có Ngọc này trong túi"));
            }
        }

        inv.setItem(SLOT_CLOSE, btn(Material.BARRIER, "§c✘ Đóng", ""));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || !isOur(e.getView())) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot >= 36) return;

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        GemType type = switch (slot) {
            case SLOT_HOA -> GemType.HOA;
            case SLOT_BANG -> GemType.BANG;
            case SLOT_LOI -> GemType.LOI;
            default -> null;
        };
        if (type == null) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getPhaBaoManager().isPhaBao(held)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần vẫn đang cầm Pháp Bảo ở tay chính.");
            player.closeInventory();
            return;
        }

        PhaBaoSocketManager.Result result = plugin.getPhaBaoSocketManager().socketGem(player, held, type);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(plugin.prefix() + "§b✦ Đã khảm " + type.getColor() + type.getDisplayName()
                        + " §bvào Pháp Bảo! §7(vĩnh viễn)");
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
                player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                        player.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
            }
            case NO_SOCKET -> player.sendMessage(plugin.prefix() + "§cPháp Bảo đã hết ô khảm trống.");
            case NO_GEM -> player.sendMessage(plugin.prefix() + "§cBạn không có " + type.getDisplayName() + " nào.");
            case NOT_PHABAO -> player.sendMessage(plugin.prefix() + "§cĐây không phải là Pháp Bảo.");
        }
        open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && isOur(e.getView())) e.setCancelled(true);
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
