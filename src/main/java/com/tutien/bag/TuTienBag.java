package com.tutien.bag;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.skills.ComboSkillRegistry;
import com.tutien.skills.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * TuTienBag - Giao dien tui do Tu Tien (54 slot).
 *
 *  Row 0  (0–8)  : Thong tin nhan vat + 4 skill slot + 3 phap bao slot cuoi
 *  Row 1  (9–17) : Linh Khi bar + Stats + Dan duoc (6 slot)
 *  Row 2  (18–26): Separator + tieu de kho
 *  Row 3–4 (27–44): 18 slot luu tru tu do
 *  Row 5 (45–53) : Cac nut hanh dong
 *
 *  Dung /tuitutien hoac /ttbag de mo.
 */
public class TuTienBag implements Listener {

    private final TuTienPlugin plugin;

    // Phai khop voi title khi tao inventory
    public static final String TITLE = "§6⚗ Túi Đồ Tu Tiên";

    // Slots trong bag
    private static final int SLOT_INFO    = 0;
    private static final int SLOT_SKILL_A = 1;
    private static final int SLOT_SKILL_B = 2;
    private static final int SLOT_SKILL_C = 3;
    private static final int SLOT_SKILL_D = 4;
    // 5 = separator
    private static final int SLOT_PB_1   = 6;
    private static final int SLOT_PB_2   = 7;
    private static final int SLOT_PB_3   = 8;

    private static final int SLOT_LK     = 9;
    private static final int SLOT_STATS  = 10;
    // 11 = separator
    private static final int SLOT_DAN_START = 12; // 12-17 (6 slot)

    // 18-26 = separator row
    // 27-44 = free storage (18 slot)
    // 45-53 = action buttons
    private static final int SLOT_BTN_CLOSE    = 45;
    private static final int SLOT_BTN_SKILLS   = 46;
    private static final int SLOT_BTN_LT       = 47;
    private static final int SLOT_BTN_PB       = 48;
    private static final int SLOT_BTN_LUYEN    = 49;
    private static final int SLOT_BTN_GUIDE    = 53;

    private static final int STORE_START = 27;
    private static final int STORE_COUNT = 18;

    public TuTienBag(TuTienPlugin plugin) { this.plugin = plugin; }

    // ====================================================
    //  MO INVENTORY
    // ====================================================

    public void openBag(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(
            null, 54,
            LegacyComponentSerializer.legacySection().deserialize(TITLE));

        // nen
        fillBorders(inv);

        // Row 0
        inv.setItem(SLOT_INFO,    buildInfoItem(data));
        buildSkillItems(inv, player, data);
        inv.setItem(5, sep());
        buildPhaBaoSlots(inv, data);

        // Row 1
        inv.setItem(SLOT_LK,    buildLKItem(data));
        inv.setItem(SLOT_STATS, buildStatsItem(data));
        inv.setItem(11, sep());
        buildDanSlots(inv, data);

        // Row 2: separator voi label
        for (int i = 18; i < 27; i++) inv.setItem(i, i == 18
            ? label("§7§l📦 Lưu Trữ Tu Tiên §8(kéo item vào đây)") : sep());

        // Row 3-4: free storage
        ItemStack[] store = data.getBagStorage();
        for (int i = 0; i < STORE_COUNT; i++) {
            ItemStack stored = store[i];
            inv.setItem(STORE_START + i,
                (stored != null && stored.getType() != Material.AIR)
                    ? stored.clone()
                    : emptySlot("§8▫ Ô trống " + (i+1)));
        }

        // Row 5: action buttons
        for (int i = 45; i <= 53; i++) inv.setItem(i, sep());
        inv.setItem(SLOT_BTN_CLOSE,  btn("§c✘ Đóng",         "§7Đóng túi đồ",                     Material.BARRIER));
        inv.setItem(SLOT_BTN_SKILLS, btn("§a⚔ Lấy Skill",   "§7Đặt lại 4 skill + Sách LK vào hotbar", Material.NETHER_STAR));
        inv.setItem(SLOT_BTN_LT,     btn("§6✦ Tiền",  "§7Xem số dư\n§7Shift+click: nạp tất cả", Material.GOLD_NUGGET));
        inv.setItem(SLOT_BTN_PB,     btn("§d◉ Pháp Bảo",    "§7Nhận pháp bảo miễn phí (lần đầu)",  Material.DIAMOND));
        inv.setItem(SLOT_BTN_LUYEN,  btn("§e⚗ Luyện Đan",   "§7Xem 29 công thức luyện đan",        Material.BLAZE_ROD));
        inv.setItem(SLOT_BTN_GUIDE,  btn("§b? Hướng Dẫn",   "§7/guide — Hướng dẫn toàn bộ",        Material.BOOK));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.3f);
    }

    // ====================================================
    //  BUILD ITEM HELPERS
    // ====================================================

    private ItemStack buildInfoItem(PlayerData data) {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(plugin.msg("auto_1dc964bddd"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Cảnh Giới: " + (data.hasClass()
            ? data.getStage().getFullDisplay(data.getRank()) : "§8Chưa chọn class"));
        lore.add("§7Class:    " + (data.hasClass()
            ? data.getClassType().getColor() + data.getClassType().getDisplayName() : "§8/tutien chon"));
        lore.add("§7Đạo:      " + (data.hasDaoPath()
            ? data.getDaoPath().getDisplayName() : "§8/tutien dao"));
        lore.add("§7Linh Căn: §e" + (data.hasLinhCan()
            ? data.getLinhCan().size() + " nguyên tố" : "§8Chưa biết"));
        lore.add("§7Tộc:      " + (data.hasToc()
            ? data.getToc().getColoredName() : "§8Chưa biết"));
        m.setLore(lore);
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m);
        return it;
    }

    private void buildSkillItems(Inventory inv, Player player, PlayerData data) {
        if (!data.hasClass()) {
            for (int s = 0; s < 4; s++)
                inv.setItem(SLOT_SKILL_A + s, label("§8[" + (char)('A'+s) + "] Chưa chọn class"));
            return;
        }
        var ct = data.getClassType();
        var comboSlots = ComboSkillRegistry.getComboSlots(ct);
        for (int slot = 0; slot < 4 && slot < comboSlots.size(); slot++) {
            var unlocked = ComboSkillRegistry.getUnlockedSubSkills(ct, slot, data.getStage());
            if (unlocked.isEmpty()) {
                inv.setItem(SLOT_SKILL_A + slot,
                    label("§8[" + (char)('A'+slot) + "] Bị khóa"));
                continue;
            }
            int ai = Math.min(data.getActiveSubSkill(slot), unlocked.size()-1);
            var entry  = unlocked.get(ai);
            Skill skill = plugin.getSkillManager().getSkill(entry.id());

            Material mat = plugin.getSkillItemManager().getSkillMaterialPublic(entry.id());
            ItemStack it = new ItemStack(mat);
            ItemMeta  meta = it.getItemMeta();
            meta.setDisplayName(ct.getColor() + "§l[" + (char)('A'+slot) + "] " + entry.displayName());
            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────");
            lore.add(skill != null ? "§7" + skill.getDescription() : "§8(Đang phát triển)");
            lore.add("§7CD: §e" + (skill != null ? skill.getCooldownMs()/1000 + "s" : "?"));
            lore.add("§7Sub: §e(" + (ai+1) + "/" + unlocked.size() + ")");
            for (int i = 0; i < unlocked.size(); i++)
                lore.add((i==ai ? "§a▶ " : "§8  ") + unlocked.get(i).displayName());
            lore.add("§8────────────────");
            lore.add("§7Hotbar: §eShift+Q §7đổi  |  §eChuột phải §7dùng");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            it.setItemMeta(meta);
            inv.setItem(SLOT_SKILL_A + slot, it);
        }
    }

    private void buildPhaBaoSlots(Inventory inv, PlayerData data) {
        for (int i = 0; i < 3; i++) {
            ItemStack it = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
            ItemMeta  m  = it.getItemMeta();
            m.setDisplayName("§b◉ Pháp Bảo " + (i+1));
            m.setLore(List.of(
                "§8Kéo Pháp Bảo vào đây để lưu",
                "§8Shift+Chuột phải để dùng từ đây"));
            m.addItemFlags(ItemFlag.values());
            m.getPersistentDataContainer().set(
                new NamespacedKey(plugin,"bag_pb_slot"), PersistentDataType.INTEGER, i);
            it.setItemMeta(m);
            inv.setItem(SLOT_PB_1 + i, it);
        }
    }

    private void buildDanSlots(Inventory inv, PlayerData data) {
        for (int i = 0; i < 6; i++)
            inv.setItem(SLOT_DAN_START + i,
                emptySlot("§7⚗ Linh Đan " + (i+1)));
    }

    private ItemStack buildLKItem(PlayerData data) {
        long cur = data.getLinhKhi(), max = data.getMaxLinhKhi();
        int pct = max > 0 ? (int)(cur*100/max) : 0;
        int bars = pct / 10;
        String bar = "§a" + "█".repeat(Math.min(bars,10)) + "§8" + "█".repeat(Math.max(0,10-bars));
        ItemStack it = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(plugin.msg("auto_75428e6b4c"));
        m.setLore(Arrays.asList(
            "  " + bar + " §e" + pct + "%",
            "§7Hiện tại: §e" + fmt(cur),
            "§7Tối đa:   §e" + fmt(max),
            "",
            pct >= 100 ? "§a✦ SẴN SÀNG ĐỘT PHÁ! §7/dotpha" : "§7Tiếp tục tu luyện..."
        ));
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m);
        return it;
    }

    private ItemStack buildStatsItem(PlayerData data) {
        ItemStack it = new ItemStack(Material.IRON_SWORD);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(plugin.msg("auto_288edf6763"));
        m.setLore(Arrays.asList(
            "§7ATK:  §e×" + String.format("%.2f", data.calculateDamageMultiplier()),
            "§7DEF:  §e×" + String.format("%.2f", data.calculateDefenseMultiplier()),
            "§7Tiền: §6" + fmt(data.getTien()),
            "",
            "§7Tiền server §8— quản lý qua Vault"
        ));
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m);
        return it;
    }

    // ====================================================
    //  EVENTS
    // ====================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!isBag(e.getView())) return;
        e.setCancelled(true);                       // default: khong cho lay do

        int raw  = e.getRawSlot();
        PlayerData data = plugin.getDataManager().get(p);
        if (data == null) return;

        // ── Bottom inv (player's own inventory): tu do ──
        if (raw >= 54) { e.setCancelled(false); return; }

        // ── Buttons ──
        if (raw == SLOT_BTN_CLOSE)  { p.closeInventory(); return; }
        if (raw == SLOT_BTN_SKILLS) {
            p.closeInventory();
            plugin.getSkillItemManager().giveSkillItems(p, data);
            p.sendMessage(plugin.prefix() + "§aĐã đặt lại skill items vào hotbar!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            return;
        }
        if (raw == SLOT_BTN_LT) {
            p.closeInventory();
            p.sendMessage(plugin.prefix() + "§6Số dư: §e" + plugin.getTienManager().fmt(plugin.getTienManager().getBalance(p)) + " §7" + plugin.getTienManager().tenTien());
            return;
        }
        if (raw == SLOT_BTN_PB) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "tutien phabao");
            return;
        }
        if (raw == SLOT_BTN_LUYEN) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "luyen danh_sach");
            return;
        }
        if (raw == SLOT_BTN_GUIDE) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "guide");
            return;
        }

        // ── Free storage slots (27-44): cho phep dat/lay bat ky item ──
        if (raw >= STORE_START && raw < STORE_START + STORE_COUNT) {
            handleStorage(e, p, data, raw - STORE_START);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!isBag(e.getView())) return;
        // Chi cho keo vao storage slots (27-44)
        for (int slot : e.getRawSlots()) {
            if (slot < STORE_START || slot >= STORE_START + STORE_COUNT) {
                if (slot < 54) { e.setCancelled(true); return; }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!isBag(e.getView())) return;
        // Luu tru khi dong
        PlayerData data = plugin.getDataManager().get(p);
        if (data == null) return;
        ItemStack[] store = data.getBagStorage();
        Inventory inv = e.getInventory();
        for (int i = 0; i < STORE_COUNT; i++) {
            ItemStack s = inv.getItem(STORE_START + i);
            store[i] = (s != null && s.getType() != Material.AIR && !isEmpty(s)) ? s.clone() : null;
        }
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.6f, 1.0f);
    }

    private void handleStorage(InventoryClickEvent e, Player p, PlayerData data, int idx) {
        ItemStack cursor  = e.getCursor();
        ItemStack current = e.getCurrentItem();

        boolean hasCursor  = cursor  != null && cursor.getType()  != Material.AIR;
        boolean hasCurrent = current != null && current.getType() != Material.AIR && !isEmpty(current);

        if (hasCursor && hasCurrent) {
            // Swap
            e.getView().setCursor(current.clone());
            e.getInventory().setItem(STORE_START + idx, cursor.clone());
        } else if (hasCursor) {
            e.getInventory().setItem(STORE_START + idx, cursor.clone());
            e.getView().setCursor(new ItemStack(Material.AIR));
        } else if (hasCurrent) {
            e.getView().setCursor(current.clone());
            e.getInventory().setItem(STORE_START + idx, emptySlot("§8▫ Ô trống " + (idx+1)));
        }
    }

    // ====================================================
    //  UTILITY
    // ====================================================

    private boolean isBag(InventoryView view) {
        try {
            String t = LegacyComponentSerializer.legacySection().serialize(view.title());
            return TITLE.equals(t);
        } catch (Exception e) { return false; }
    }
    private boolean isEmpty(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin,"empty_slot"), PersistentDataType.BYTE);
    }

    private void fillBorders(Inventory inv) {
        ItemStack g = sep();
        for (int i = 0; i < 54; i++) inv.setItem(i, g);
    }

    private ItemStack sep() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta(); m.setDisplayName(plugin.msg("auto_374dd318e3"));
        m.addItemFlags(ItemFlag.values()); it.setItemMeta(m); return it;
    }
    private ItemStack label(String name) {
        ItemStack it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta(); m.setDisplayName(name);
        m.addItemFlags(ItemFlag.values()); it.setItemMeta(m); return it;
    }
    private ItemStack emptySlot(String name) {
        ItemStack it = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(List.of("§8Kéo vật phẩm vào đây để lưu"));
        m.addItemFlags(ItemFlag.values());
        m.getPersistentDataContainer().set(
            new NamespacedKey(plugin,"empty_slot"), PersistentDataType.BYTE, (byte)1);
        it.setItemMeta(m); return it;
    }
    private ItemStack btn(String name, String lore, Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore.split("\n")));
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m); return it;
    }
    private String fmt(long n) {
        if (n >= 1_000_000L) return String.format("%.1fM", n/1_000_000.0);
        if (n >= 1_000L)     return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }
}
