package com.tutien.alchemy;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** GUI Luyện Đan 32 công thức, đọc hoàn toàn từ config. */
public class AlchemyGUI implements Listener {
    private final TuTienPlugin plugin;
    private static final String TITLE = "§6⚗ Lò Luyện Đan 7.2.8";
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, String> selected = new HashMap<>();

    public AlchemyGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        open(p, pages.getOrDefault(p.getUniqueId(), 0));
    }

    /** Mở GUI và chọn sẵn một công thức theo ID. */
    public void open(Player p, String recipeId) {
        if (recipeId != null && plugin.getAlchemyManager().getAllRecipes().containsKey(recipeId)) {
            selected.put(p.getUniqueId(), recipeId);
        }
        open(p, pages.getOrDefault(p.getUniqueId(), 0));
    }

    public void open(Player p, int page) {
        PlayerData data = plugin.getDataManager().get(p);
        if (data == null) return;

        List<AlchemyManager.DanDuoc> list = new ArrayList<>(plugin.getAlchemyManager().getAllRecipes().values());
        if (list.isEmpty()) {
            p.sendMessage(plugin.prefix() + "§cKhông có đan dược nào được nạp từ config.yml!");
            p.sendMessage(plugin.prefix() + "§7Admin kiểm tra mục §ealchemy.recipes§7, cần đủ 32 công thức.");
            return;
        }
        int maxPage = Math.max(0, (list.size() - 1) / 18);
        String selectedId = selected.get(p.getUniqueId());
        if (selectedId != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id.equals(selectedId)) { page = i / 18; break; }
            }
        }
        page = Math.max(0, Math.min(page, maxPage));
        pages.put(p.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        for (int i = 18; i < 27; i++) {
            inv.setItem(i, glass(Material.BLACK_STAINED_GLASS_PANE, "§8— Công thức — Chi tiết —"));
        }

        int start = page * 18;
        for (int i = 0; i < 18 && start + i < list.size(); i++) {
            inv.setItem(i, icon(list.get(start + i), data));
        }

        AlchemyManager.DanDuoc r = selected.containsKey(p.getUniqueId())
                ? plugin.getAlchemyManager().getAllRecipes().get(selected.get(p.getUniqueId()))
                : null;

        if (r == null) {
            inv.setItem(31, btn(Material.CAULDRON, "§7← Chọn một đan dược", "§7Danh sách ở hàng trên."));
        } else {
            detail(inv, p, data, r);
        }

        inv.setItem(48, btn(Material.BOOK, "§e/luyen danh_sach", "§7Xem toàn bộ công thức bằng chat."));
        inv.setItem(49, btn(Material.BARRIER, "§c✘ Đóng", ""));
        if (page > 0) inv.setItem(51, btn(Material.ARROW, "§7◀ Trang " + page, ""));
        inv.setItem(52, glass(Material.LIME_STAINED_GLASS_PANE, "§7Trang §e" + (page + 1) + "§7/§e" + (maxPage + 1)));
        if (page < maxPage) inv.setItem(53, btn(Material.ARROW, "§7Trang " + (page + 2) + " ▶", ""));

        p.openInventory(inv);
    }

    private ItemStack icon(AlchemyManager.DanDuoc r, PlayerData d) {
        boolean ok = d.getStage().ordinal() >= r.minStageOrdinal;
        ItemStack it = new ItemStack(ok ? Material.BLAZE_POWDER : Material.GRAY_DYE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName((ok ? r.color : "§8") + (ok ? "" : "🔒 ") + r.displayName);
        m.setLore(List.of(
                "§7" + r.description,
                "§7Độ khó: §e" + r.difficulty,
                "§7Thành công: §e" + pct(r.successRate) + "%",
                "§7Thời gian: §e" + r.craftTimeSeconds + "s",
                ok ? "§a✓ Đã mở khóa" : "§c✗ Chưa đủ cảnh giới"
        ));
        m.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_recipe_id"), PersistentDataType.STRING, r.id);
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m);
        return it;
    }

    private void detail(Inventory inv, Player p, PlayerData d, AlchemyManager.DanDuoc r) {
        // Slot 27: Icon tổng quan công thức
        inv.setItem(27, btn(Material.NETHER_STAR, r.color + "§l⚗ " + r.displayName,
                "§7" + r.description,
                "§7Độ khó: §e" + r.difficulty,
                "§7Tỉ lệ: §e" + pct(r.successRate) + "%",
                "§7Thời gian: §e" + r.craftTimeSeconds + "s",
                "§7Phí: §e" + r.ltCost + " " + plugin.getTienManager().tenTien()));

        // Slot 28->31: Nguyên liệu Minecraft cơ bản (Tối đa 4 item)
        for (int i = 0; i < Math.min(4, r.materials.length); i++) {
            ItemStack mat = r.materials[i].clone();
            int need = mat.getAmount();
            int have = countMat(p, mat.getType());
            ItemMeta m = mat.getItemMeta();
            m.setDisplayName("§f" + pretty(mat.getType().name()) + " §8×" + need);
            m.setLore(List.of((have >= need ? "§a✓ " : "§c✗ ") + have + "/" + need));
            mat.setItemMeta(m);
            inv.setItem(28 + i, mat);
        }

        // Slot 33->35: Tiên Thảo (Tối đa 3 loại thảo dược để không bị đè sang slot 40)
        inv.setItem(32, glass(Material.FLOWERING_AZALEA, "§a§l✿ TIÊN THẢO"));
        for (int i = 0; i < Math.min(3, r.herbIds.length); i++) {
            String id = r.herbIds[i];
            int need = r.herbAmounts[i];
            int have = plugin.getHerbManager().countHerb(p, id);
            String name = plugin.getHerbManager().getHerbType(id).map(h -> h.name()).orElse(id);
            inv.setItem(33 + i, btn(have >= need ? Material.FLOWERING_AZALEA : Material.DEAD_BUSH,
                    (have >= need ? "§a" : "§c") + "✿ " + name + " ×" + need,
                    (have >= need ? "§a✓ " : "§c✗ ") + have + "/" + need));
        }

        // Slot 40: Nút bấm Luyện Đan chính
        inv.setItem(40, btn(Material.BLAST_FURNACE, "§a§l⚗ LUYỆN ĐAN",
                "§7Click để luyện",
                "§7Thất bại: §cMất toàn bộ nguyên liệu",
                "§7Thành công: §e" + pct(r.successRate) + "%"));

        // Slot 44: Danh sách Hiệu Ứng (Tách thành các dòng Lore chuẩn)
        List<String> effectLore = new ArrayList<>();
        if (r.effectLines.length == 0) {
            effectLore.add("§7Không có hiệu ứng đặc biệt");
        } else {
            for (String line : r.effectLines) {
                effectLore.add("§a▸ " + line);
            }
        }
        inv.setItem(44, btnList(Material.PAPER, "§bHiệu ứng khi uống", effectLore));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isOur(e.getView())) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot >= 54) return;

        PlayerData d = plugin.getDataManager().get(p);
        if (d == null) return;

        if (slot < 18) {
            ItemStack it = e.getCurrentItem();
            if (it == null || !it.hasItemMeta()) return;
            String id = it.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "gui_recipe_id"), PersistentDataType.STRING);
            if (id != null) {
                selected.put(p.getUniqueId(), id);
                open(p, pages.getOrDefault(p.getUniqueId(), 0));
            }
            return;
        }

        if (slot == 40) {
            String id = selected.get(p.getUniqueId());
            if (id == null) {
                p.sendMessage(plugin.prefix() + "§cChọn công thức trước!");
                return;
            }
            AlchemyManager.DanDuoc r = plugin.getAlchemyManager().getAllRecipes().get(id);
            if (r == null) return;
            if (plugin.getAlchemyManager().canCraft(p, r, true)) {
                p.closeInventory();
                plugin.getAlchemyManager().luyenDan(p, id);
            }
            return;
        }

        if (slot == 49) {
            p.closeInventory();
            return;
        }

        int page = pages.getOrDefault(p.getUniqueId(), 0);
        if (slot == 51 && page > 0) open(p, page - 1);
        if (slot == 53) open(p, page + 1);
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && isOur(e.getView())) e.setCancelled(true);
    }

    private boolean isOur(InventoryView v) {
        try {
            return LegacyComponentSerializer.legacySection().serialize(v.title()).equals(TITLE);
        } catch (Exception e) {
            return false;
        }
    }

    private int countMat(Player p, Material m) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == m) n += it.getAmount();
        }
        return n;
    }

    private ItemStack glass(Material m, String name) {
        return btn(m, name, "");
    }

    private ItemStack btn(Material m, String name, String... lore) {
        return btnList(m, name, Arrays.asList(lore));
    }

    private ItemStack btnList(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    private String pct(double d) {
        return String.format(Locale.US, "%.1f", d * 100);
    }

    private String pretty(String s) {
        return s.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public void cleanup(Player p) {
        pages.remove(p.getUniqueId());
        selected.remove(p.getUniqueId());
    }
}