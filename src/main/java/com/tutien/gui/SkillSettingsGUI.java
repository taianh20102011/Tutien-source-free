package com.tutien.gui;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** Per-player toggle menu for optional TuTien combat/progression modules. */
public final class SkillSettingsGUI implements Listener {
    private static final String PREFIX = "tt.skill-module.";
    private final TuTienPlugin plugin;
    private final List<Module> modules = List.of(
            new Module("the_chat", "Thể Chất", Material.NETHERITE_CHESTPLATE, "Bật/tắt bonus thuộc class Thể Tu."),
            new Module("cong_phap", "Công Pháp", Material.ENCHANTED_BOOK, "Bật/tắt chỉ số và thức chủ động của công pháp."),
            new Module("noi_tinh", "Nội Tình / Bị Động", Material.EXPERIENCE_BOTTLE, "Bật/tắt các nội tại đã học."),
            new Module("ky_nang_them", "Kỹ Năng Thêm", Material.NETHER_STAR, "Bật/tắt sử dụng skill item."),
            new Module("toc", "Tộc", Material.TOTEM_OF_UNDYING, "Bật/tắt nội tại và hiệu ứng chủng tộc."),
            new Module("linh_khi_ho_the", "Linh Khí Hộ Thể", Material.SHIELD, "Bật/tắt Hộ Thể / Linh Thức phòng thủ."),
            new Module("don_thuat", "Độn Thuật", Material.FEATHER, "Bật/tắt chức năng dịch chuyển/độn thuật được hỗ trợ."),
            new Module("phan_don", "Phản Đòn", Material.IRON_CHESTPLATE, "Bật/tắt cơ chế phản đòn của nhân vật."));

    public SkillSettingsGUI(TuTienPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 27, "§0Tùy Chỉnh Hệ Thống Kỹ Năng");
        holder.inventory = inv;
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, "§0", List.of());
        for (int i = 0; i < 27; i++) if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, filler);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 17};
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            holder.slotToModule.put(slots[i], module.id);
            inv.setItem(slots[i], render(data, module));
        }
        inv.setItem(22, item(Material.PAPER, "§eChú thích", List.of("§7Nhấp vào biểu tượng để bật/tắt.", "§7Thiết lập được lưu riêng theo người chơi.", "§8Một số mục chỉ tác động lên hệ thống đã được nối.")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.2f);
    }

    private ItemStack render(PlayerData data, Module module) {
        boolean enabled = data.getExtraBool(PREFIX + module.id, true);
        return item(module.material, (enabled ? "§a● " : "§c● ") + module.name,
                List.of(module.description, "", "§7Trạng thái: " + (enabled ? "§aĐANG BẬT" : "§cĐANG TẮT"), "§eNhấp để đổi trạng thái"));
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.viewer.equals(player.getUniqueId())) return;
        String id = holder.slotToModule.get(event.getRawSlot());
        if (id == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        String key = PREFIX + id;
        data.setExtraBool(key, !data.getExtraBool(key, true));
        plugin.getDataManager().save(data);
        Module module = modules.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null);
        if (module != null) event.getView().getTopInventory().setItem(event.getRawSlot(), render(data, module));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        player.sendMessage(plugin.prefix() + "§7" + (module == null ? id : module.name) + ": " + (data.getExtraBool(key, true) ? "§aBẬT" : "§cTẮT"));
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private record Module(String id, String name, Material material, String description) { }

    private static final class MenuHolder implements InventoryHolder {
        private final UUID viewer;
        private final Map<Integer, String> slotToModule = new HashMap<>();
        private Inventory inventory;
        private MenuHolder(UUID viewer) { this.viewer = viewer; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
