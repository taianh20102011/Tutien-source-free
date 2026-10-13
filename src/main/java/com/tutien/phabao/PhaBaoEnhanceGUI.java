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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI Cuong Hoa Phap Bao - theo dung phong cach "click-to-craft" da dung o
 * AlchemyGUI (KHONG can drag item vao slot GUI - nguyen lieu/Phap Bao van
 * nam trong tui/tay nguoi choi, GUI chi hien thi thong tin + nut bam). Cach
 * nay tranh moi rui ro dup/mat item khi keo tha giua 2 inventory.
 *
 * Nguoi choi phai dang CAM Phap Bao o tay chinh khi mo GUI nay.
 */
public class PhaBaoEnhanceGUI implements Listener {

    private final TuTienPlugin plugin;
    public static final String TITLE = "§6⚒ Cường Hóa Pháp Bảo";

    private static final int SLOT_ITEM   = 13;
    private static final int SLOT_STONE  = 20;
    private static final int SLOT_INFO   = 22;
    private static final int SLOT_ENHANCE = 24;
    private static final int SLOT_CLOSE  = 31;

    public PhaBaoEnhanceGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!plugin.getPhaBaoManager().isPhaBao(held)) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm 1 Pháp Bảo ở tay chính để Cường Hóa.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 36; i++) inv.setItem(i, bg);

        PhaBaoEnhanceManager em = plugin.getPhaBaoEnhanceManager();
        int lvl = em.getEnhanceLevel(held);
        int max = em.getMaxLevel();

        inv.setItem(SLOT_ITEM, held.clone());

        int stones = em.countProtectionStones(player);
        inv.setItem(SLOT_STONE, btn(stones > 0 ? Material.AMETHYST_SHARD : Material.GRAY_DYE,
                "§d✦ Đá Bảo Hộ: §f" + stones,
                stones > 0 ? "§a✓ Sẽ tự dùng 1 viên nếu thất bại" : "§7Không có - thất bại có thể rớt cấp/vỡ đồ",
                "§7(Không cần đặt vào GUI - tự lấy từ túi đồ)"));

        if (lvl >= max) {
            inv.setItem(SLOT_INFO, btn(Material.BARRIER, "§c✦ Đã đạt cấp tối đa +" + max, ""));
            inv.setItem(SLOT_ENHANCE, glass(Material.GRAY_STAINED_GLASS_PANE, "§8— Không thể cường hóa thêm —"));
        } else {
            int target = lvl + 1;
            long cost = em.getCost(target);
            double rate = em.getSuccessRate(target);
            inv.setItem(SLOT_INFO, btn(Material.BOOK,
                    "§e✦ +" + lvl + " §7→ §a+" + target,
                    "§7Tỉ lệ thành công: §e" + Math.round(rate * 100) + "%",
                    "§7Phí: §e" + plugin.getTienManager().fmt(cost) + " " + plugin.getTienManager().tenTien(),
                    "§7Mỗi cấp: §a+" + Math.round(em.getBonusPerLevel() * 100) + "% §7ATK/DEF/HP + sát thương kỹ năng",
                    "§cThất bại (không Đá Bảo Hộ): §7có thể rớt cấp hoặc vỡ đồ"));
            inv.setItem(SLOT_ENHANCE, btn(Material.ANVIL, "§a✔ Cường Hóa Ngay",
                    "§7Bấm để thử cường hóa lên §a+" + target));
        }

        inv.setItem(SLOT_CLOSE, btn(Material.BARRIER, "§c✘ Đóng", ""));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || !isOur(e.getView())) return;
        e.setCancelled(true); // GUI nay CHI xem/bam nut - khong cho lay/dat item gi ca

        int slot = e.getRawSlot();
        if (slot >= 36) return;

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_ENHANCE) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!plugin.getPhaBaoManager().isPhaBao(held)) {
                player.sendMessage(plugin.prefix() + "§cBạn cần vẫn đang cầm Pháp Bảo ở tay chính.");
                player.closeInventory();
                return;
            }
            PhaBaoEnhanceManager.Result result = plugin.getPhaBaoEnhanceManager().attemptEnhance(player, held);
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(plugin.prefix() + "§a✦ Cường Hóa THÀNH CÔNG! §7Pháp Bảo hiện tại: §d+"
                            + plugin.getPhaBaoEnhanceManager().getEnhanceLevel(held));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
                    player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT,
                            player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.2);
                }
                case FAIL_SAFE -> {
                    player.sendMessage(plugin.prefix() + "§eCường hóa thất bại §7- §dĐá Bảo Hộ§7 đã bảo vệ Pháp Bảo, không mất gì.");
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
                }
                case FAIL_DOWNGRADE -> {
                    player.sendMessage(plugin.prefix() + "§cCường hóa thất bại! §7Pháp Bảo bị §crớt 1 cấp§7: hiện tại §c+"
                            + plugin.getPhaBaoEnhanceManager().getEnhanceLevel(held));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.7f);
                }
                case FAIL_BROKEN -> {
                    player.sendMessage(plugin.prefix() + "§4✦ Cường hóa thất bại! §cPháp Bảo đã VỠ TAN thành tro bụi!");
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                    player.closeInventory();
                    return;
                }
                case MAX_LEVEL -> player.sendMessage(plugin.prefix() + "§cPháp Bảo đã đạt cấp cường hóa tối đa.");
                case NOT_ENOUGH_MONEY -> player.sendMessage(plugin.prefix() + "§cBạn không đủ "
                        + plugin.getTienManager().tenTien() + " để cường hóa.");
            }
            open(player); // refresh GUI voi trang thai moi
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && isOur(e.getView())) e.setCancelled(true);
    }

    private boolean isOur(org.bukkit.inventory.InventoryView v) {
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
