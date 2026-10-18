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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI Dung Hop - KHAC voi PhaBaoEnhanceGUI/PhaBaoSocketGUI (khong dung style
 * "click-to-craft" thuan tuy) vi Dung Hop can THAT SU dat 2 item khac nhau
 * vao 2 o rieng cung luc. Dung 2 O THAT trong Inventory (giong o nguyen lieu
 * cua TradeManager) - an toan vi day la GUI CA NHAN (chi 1 nguoi choi thao
 * tac), khong can dong bo 2 phia nhu Trade.
 *
 * O dong (InventoryCloseEvent) LUON tra lai item con nam trong o "goc"/"nguyen
 * lieu" ve tui nguoi choi (hoac roi xuong dat neu tui day) - tranh mat item.
 */
public class PhaBaoFusionGUI implements Listener {

    private final TuTienPlugin plugin;
    public static final String TITLE = "§d✦ Dung Hợp Pháp Bảo";

    private static final int SLOT_BASE     = 20; // Phap Bao GIU LAI
    private static final int SLOT_MATERIAL = 24; // Phap Bao TIEU HAO
    private static final int SLOT_INFO     = 22;
    private static final int SLOT_CONFIRM  = 31;
    private static final int SLOT_CLOSE    = 40;
    private static final int SIZE          = 45;

    public PhaBaoFusionGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        inv.setItem(SLOT_BASE, null);
        inv.setItem(SLOT_MATERIAL, null);
        ensurePlaceholders(inv);
        refreshInfo(inv);

        inv.setItem(SLOT_CONFIRM, btn(Material.ANVIL, "§d✔ Dung Hợp Ngay",
                "§7Cả 2 Pháp Bảo phải §ecùng loại§7.",
                "§7Dòng ẩn: §5luôn được gộp",
                "§7Thăng phẩm: §etheo tỉ lệ ngẫu nhiên"));
        inv.setItem(SLOT_CLOSE, btn(Material.BARRIER, "§c✘ Đóng (trả lại item)", ""));
        player.openInventory(inv);
    }

    private ItemStack placeholderBase() {
        return placeholder("§a► Đặt Pháp Bảo GIỮ LẠI vào đây",
                "§7Pháp Bảo này sẽ được §agiữ lại§7,", "§7có thể được §athăng phẩm§7.");
    }

    private ItemStack placeholderMaterial() {
        return placeholder("§c► Đặt Pháp Bảo NGUYÊN LIỆU vào đây",
                "§7Pháp Bảo này sẽ bị §ctiêu hao§7 (mất),", "§7dòng ẩn của nó sẽ được §5gộp§7 vào bên trái.");
    }

    /** Dam bao 2 o insert luon co it nhat 1 icon huong dan khi dang trong (khong phai Phap Bao that). */
    private void ensurePlaceholders(Inventory inv) {
        ItemStack a = inv.getItem(SLOT_BASE);
        if (a == null || a.getType() == Material.AIR) inv.setItem(SLOT_BASE, placeholderBase());
        ItemStack b = inv.getItem(SLOT_MATERIAL);
        if (b == null || b.getType() == Material.AIR) inv.setItem(SLOT_MATERIAL, placeholderMaterial());
    }

    private boolean isPlaceholder(ItemStack item) {
        return item != null && item.getType() == Material.LIME_STAINED_GLASS_PANE;
    }

    private void refreshInfo(Inventory inv) {
        ItemStack base = inv.getItem(SLOT_BASE);
        ItemStack mat  = inv.getItem(SLOT_MATERIAL);
        PhaBaoManager pbm = plugin.getPhaBaoManager();

        if (base != null && pbm.isPhaBao(base)) {
            PhaBaoTier tier = pbm.getTier(base);
            long cost = plugin.getPhaBaoFusionManager().getCost(tier);
            double chance = plugin.getPhaBaoFusionManager().getUpgradeChance(tier);
            boolean maxTier = tier == PhaBaoTier.TIEN_PHAM;
            inv.setItem(SLOT_INFO, btn(Material.BOOK, "§e✦ Thông Tin Dung Hợp",
                    "§7Chi phí: §6" + plugin.getTienManager().fmt(cost) + " " + plugin.getTienManager().tenTien(),
                    maxTier ? "§bĐã Tiên Phẩm - chỉ gộp dòng ẩn"
                            : "§7Tỉ lệ thăng phẩm: §e" + Math.round(chance * 100) + "%",
                    "§7Dòng ẩn tối đa: §5" + plugin.getPhaBaoFusionManager().getMaxAffix()));
        } else {
            inv.setItem(SLOT_INFO, btn(Material.PAPER, "§7Đặt đủ 2 Pháp Bảo cùng loại", "§7để xem thông tin chi phí."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || !isOur(e.getView())) return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return; // tui do nguoi choi - cho tuong tac binh thuong

        if (slot == SLOT_BASE || slot == SLOT_MATERIAL) {
            handleInsertClick(player, e, slot);
            return;
        }

        e.setCancelled(true);

        if (slot == SLOT_CONFIRM) {
            doFusion(player, e.getInventory());
        } else if (slot == SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    /**
     * Xu ly click vao 1 trong 2 o insert. LUON tu code dieu khien viec dat/lay
     * (khong bao gio de Bukkit swap mac dinh) de tranh truong hop nguoi choi
     * "nhat nham" icon huong dan (LIME_STAINED_GLASS_PANE) vao tui cua ho.
     */
    private void handleInsertClick(Player player, InventoryClickEvent e, int slot) {
        e.setCancelled(true);
        Inventory inv = e.getInventory();
        ItemStack current = inv.getItem(slot);
        ItemStack cursor = e.getCursor();
        boolean currentIsPlaceholder = isPlaceholder(current);

        if (e.getClick().isShiftClick() || e.getClick().isKeyboardClick()) return; // chi ho tro click chuot thuong

        if (cursor == null || cursor.getType() == Material.AIR) {
            // Tay dang trong -> LAY item ra (chi khi la Phap Bao that, khong lay icon huong dan)
            if (!currentIsPlaceholder && current != null && plugin.getPhaBaoManager().isPhaBao(current)) {
                e.getView().setCursor(current.clone());
                inv.setItem(slot, null);
            }
        } else if (plugin.getPhaBaoManager().isPhaBao(cursor)) {
            // Tay dang cam 1 Phap Bao -> DAT vao o (thay the placeholder hoac Phap Bao cu neu co)
            ItemStack toPlace = cursor.clone();
            toPlace.setAmount(1);
            ItemStack oldCursor = cursor.clone();

            if (!currentIsPlaceholder && current != null && plugin.getPhaBaoManager().isPhaBao(current)) {
                // Doi cho: Phap Bao cu trong o quay ve tay
                e.getView().setCursor(current.clone());
            } else if (oldCursor.getAmount() <= 1) {
                e.getView().setCursor(new ItemStack(Material.AIR));
            } else {
                oldCursor.setAmount(oldCursor.getAmount() - 1);
                e.getView().setCursor(oldCursor);
            }
            inv.setItem(slot, toPlace);
        }
        // Neu tay cam vat pham KHONG PHAI Phap Bao: khong lam gi (khong cho dat vao o insert).

        ensurePlaceholders(inv);
        refreshInfo(inv);
    }

    private void doFusion(Player player, Inventory inv) {
        ItemStack base = inv.getItem(SLOT_BASE);
        ItemStack material = inv.getItem(SLOT_MATERIAL);
        PhaBaoManager pbm = plugin.getPhaBaoManager();

        if (base == null || material == null || !pbm.isPhaBao(base) || !pbm.isPhaBao(material)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần đặt đủ 2 Pháp Bảo (cùng loại) vào 2 ô.");
            return;
        }

        PhaBaoFusionManager.Result result = plugin.getPhaBaoFusionManager().attemptFusion(player, base, material);
        switch (result) {
            case SUCCESS_UPGRADE -> {
                inv.setItem(SLOT_BASE, base);
                inv.setItem(SLOT_MATERIAL, null);
                player.sendMessage(plugin.prefix() + "§d✦ Dung Hợp THÀNH CÔNG! §aĐã THĂNG PHẨM §7+ §5gộp dòng ẩn!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.6f);
                player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                        player.getLocation().add(0, 1, 0), 40, 0.5, 0.6, 0.5, 0.15);
            }
            case SUCCESS_NO_UPGRADE -> {
                inv.setItem(SLOT_BASE, base);
                inv.setItem(SLOT_MATERIAL, null);
                player.sendMessage(plugin.prefix() + "§5✦ Đã gộp dòng ẩn §7(chưa thăng phẩm lần này).");
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                player.getWorld().spawnParticle(org.bukkit.Particle.WITCH,
                        player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.05);
            }
            case MAX_TIER -> {
                inv.setItem(SLOT_BASE, base);
                inv.setItem(SLOT_MATERIAL, null);
                player.sendMessage(plugin.prefix() + "§bPháp Bảo đã Tiên Phẩm - đã gộp dòng ẩn (không thể thăng thêm).");
            }
            case DIFFERENT_TYPE -> player.sendMessage(plugin.prefix() + "§cHai Pháp Bảo phải CÙNG LOẠI mới dung hợp được.");
            case NOT_PHABAO -> player.sendMessage(plugin.prefix() + "§cCả 2 ô phải là Pháp Bảo hợp lệ.");
            case NOT_ENOUGH_MONEY -> player.sendMessage(plugin.prefix() + "§cBạn không đủ "
                    + plugin.getTienManager().tenTien() + " để dung hợp.");
        }
        refreshInfo(inv);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && isOur(e.getView())) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player) || !isOur(e.getView())) return;
        Inventory inv = e.getInventory();
        returnItem(player, inv, SLOT_BASE);
        returnItem(player, inv, SLOT_MATERIAL);
    }

    private void returnItem(Player player, Inventory inv, int slot) {
        ItemStack it = inv.getItem(slot);
        if (it == null || it.getType() == Material.AIR || isPlaceholder(it)) return;
        var leftover = player.getInventory().addItem(it);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
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

    private ItemStack placeholder(String name, String... lore) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
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
