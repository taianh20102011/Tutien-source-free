package com.tutien.skills;

import com.tutien.TuTienPlugin;
import com.tutien.skills.ComboSkillRegistry;
import com.tutien.data.ClassType;
import com.tutien.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * He thong skill item:
 *  - Moi class co 1 "Thong Tin Linh Khi" (BOOK) o slot 8
 *  - Moi skill la 1 item co ten rieng dat o slot 0-3
 *  - Chuot phai vao skill item -> dung skill
 *  - Phim F (swap hand) -> dung skill o slot 0 (skill chinh)
 *  - Drop (Q) -> dung skill o slot dang cam
 */
public class SkillItemManager implements Listener {

    private final TuTienPlugin plugin;

    // Map: ten item (lowercase, bo mau) -> skillId
    private final Map<String, String> itemNameToSkill = new HashMap<>();

    public SkillItemManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        buildItemNameMap();
    }

    private void buildItemNameMap() {
        // Kiem Tu
        itemNameToSkill.put("kiem khi tram",     "kiemkhitram");
        itemNameToSkill.put("toc kiem",           "tockiem");
        itemNameToSkill.put("thien sat",          "thiensat");
        // Phap Tu
        itemNameToSkill.put("linh hoa cau",       "linhhoacau");
        itemNameToSkill.put("dong bang",          "dongbang");
        itemNameToSkill.put("phap tran",          "phaptran");
        // Y Tu
        itemNameToSkill.put("linh duoc",          "linhduoc");
        itemNameToSkill.put("phu ho tran",        "phuhotran");
        itemNameToSkill.put("giai doc",           "giaido");
        // Doc Tu
        itemNameToSkill.put("manh doc tan",       "manhdoctan");
        itemNameToSkill.put("am sat kiem",        "amsatkiem");
        itemNameToSkill.put("doc tran",           "doctran");
        // The Tu
        itemNameToSkill.put("thiet bich",         "thietbich");
        itemNameToSkill.put("ho phap tran",       "hophaptran");
        itemNameToSkill.put("phan don",           "phandon");
        // Quy Tu
        itemNameToSkill.put("quy anh tan",        "quyanhtan");
        itemNameToSkill.put("hu khong doat",      "hukhongdoat");
        itemNameToSkill.put("tu anh",             "tuanh");
        // Phong Tu
        itemNameToSkill.put("phong toc dot kich", "phongtocthiet");
        itemNameToSkill.put("toan phong ne",      "toanphongne");
        itemNameToSkill.put("phong nhan",         "phongnhan");
        itemNameToSkill.put("cuong phong tran",   "cuongphongtran");
        // Hoa Tu
        itemNameToSkill.put("nghiep hoa thieu",   "nghiephoathieu");
        itemNameToSkill.put("hoa long",           "hoalong");
        itemNameToSkill.put("phong hoa tran",     "phonghoa");
        itemNameToSkill.put("tuyet dia hoa hai",  "tuyetdiahoahai");
        // Special skills
        itemNameToSkill.put("kiem tien phap",        "kiemtienphap");
        itemNameToSkill.put("van kiem xuyen tam",    "vankiemxuyentam");
        itemNameToSkill.put("thai cuc huyen phap",   "thaicuchuanphap");
        itemNameToSkill.put("hoi duong cuu nghich",  "hoiduongcuunghich");
        itemNameToSkill.put("van doc chi the",       "vandocchithe");
        itemNameToSkill.put("kim cuong bat hoai",    "kimcuongbathoai");
        itemNameToSkill.put("quy vuong giang the",   "quyvuonggianthe");
        itemNameToSkill.put("pha khong thuan di",    "phakhongthuandi");
        itemNameToSkill.put("phuong hoang trong sinh","phuonghoangtrognsinh");
    }

    // ========================
    //  KIEM TRA VA DUNG SKILL
    // ========================

    @EventHandler(priority = org.bukkit.event.EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        // Chi xu ly tay chinh
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
         && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        // Neu shift+click va item la phap bao -> PhaBaoListener xu ly roi
        if (player.isSneaking() && plugin.getPhaBaoManager().isPhaBao(item)) return;

        // ITEM GHEP (moi): Chuot phai = Slot A | Sneak+Chuot phai = Slot B
        if (isMergedWeapon(item)) {
            event.setCancelled(true);
            int slot = player.isSneaking() ? 1 : 0;
            String skillId = getMergedSkillId(item, slot);
            if (skillId == null) {
                player.sendMessage(plugin.prefix() + "§7Slot " + (char)('A'+slot) + " chưa mở khóa!");
                return;
            }
            tryUseSkill(player, skillId);
            return;
        }

        // Item cu (combo-slot rieng le) - van ho tro nguoc
        String skillId = getSkillIdFromItem(item);
        if (skillId == null) return;

        event.setCancelled(true);
        tryUseSkill(player, skillId);
    }

    // ITEM GHEP: Sneak + Chuột trái (Nút Ngồi + Nút Chém trên Mobile / Shift + Left-click trên PC) = Slot C
    @EventHandler(priority = org.bukkit.event.EventPriority.NORMAL, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isMergedWeapon(item)) return;

        event.setCancelled(true);
        String skillId = getMergedSkillId(item, 2);
        if (skillId == null) {
            player.sendMessage(plugin.prefix() + "§7Slot C chưa mở khóa!");
            return;
        }
        tryUseSkill(player, skillId);
    }

    // ITEM GHEP: Sneak + Chém trúng Mob/Entity trên Mobile/PC = kích hoạt Slot C
    @EventHandler(priority = org.bukkit.event.EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isMergedWeapon(item)) return;

        String skillId = getMergedSkillId(item, 2);
        if (skillId == null) return;

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        // Nếu Skill C sẵn sàng (không cooldown) -> tự kích hoạt kèm đòn đánh
        if (!data.isSkillOnCooldown(skillId)) {
            tryUseSkill(player, skillId);
        }
    }

    // ITEM GHEP: Đổi tay (F trên PC / Swap Hand) = Slot C
    @EventHandler
    public void onSwapHand(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isMergedWeapon(item)) return;
        event.setCancelled(true);
        String skillId = getMergedSkillId(item, 2);
        if (skillId == null) {
            player.sendMessage(plugin.prefix() + "§7Slot C chưa mở khóa!");
            return;
        }
        tryUseSkill(player, skillId);
    }

    // NOTE: Phim F (SwapHand) KHONG DUOC HO TRO tren Bedrock/PE qua Geyser.
    // PE players su dung Chuot Phai (RIGHT_CLICK) hoac Q (DROP) thay the.
    // PC players van dung duoc F binh thuong qua PlayerInteractEvent.

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();

        // ITEM GHEP (moi): Q (Vut do / nut Drop Item) = Slot D - Tuyet Ky
        if (isMergedWeapon(dropped)) {
            event.setCancelled(true);
            String skillId = getMergedSkillId(dropped, 3);
            if (skillId == null) {
                player.sendMessage(plugin.prefix() + "§7Tuyệt Kỹ (Slot D) chưa mở khóa!");
                return;
            }
            tryUseSkill(player, skillId);
            return;
        }

        if (player.isSneaking()) {
            int comboSlot = getComboSlot(dropped);
            if (comboSlot >= 0) {
                // Shift+Q -> cycle sub-skill
                // KHONG cancel event - de slot bi trong hoac xau item entity
                // Thay vao do: xoa entity roi, put item moi vao slot
                event.setCancelled(true);
                // Item da bi xoa khoi inventory truoc khi event fire
                // setCancelled tra no lai vao inventory (slot ngau nhien)
                // -> phai xoa tat ca skill item cu trong hotbar truoc
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Scan TOAN BO 36 slot de xoa skill item vua bi tra ve (cancel drop)
                    // Dung getComboSlot (NBT) - an toan, khong xoa nham do cua player
                    for (int i = 0; i < 36; i++) {
                        ItemStack s = player.getInventory().getItem(i);
                        if (getComboSlot(s) == comboSlot) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                    // Dat item moi vao dung slot
                    cycleComboSlot(player, comboSlot);
                }, 1L);
                return;
            }
        }

        // Q -> use skill (cancel de khong thuc su drop) - item cu (legacy)
        String skillId = getSkillIdFromItem(dropped);
        if (skillId != null) {
            event.setCancelled(true);
            tryUseSkill(player, skillId);
            return;
        }

        // CHONG MAT PHAP BAO: neu drop PhaBao -> cancel, tra lai inventory
        if (plugin.getPhaBaoManager().isPhaBao(dropped)) {
            event.setCancelled(true);
            // Item se tu dong tra lai boi setCancelled
            return;
        }

        // CHONG MAT INFO ITEM: cancel drop neu la item cua plugin
        if (isInfoItem(dropped)) {
            event.setCancelled(true);
        }
    }

    /** /doiky <a|b|c|d> - doi sub-skill dang active trong 1 slot cua item ghep. */
    public void cycleMergedSlot(Player player, int slot) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;
        var unlocked = ComboSkillRegistry.getUnlockedSubSkills(data.getClassType(), slot, data.getStage());
        if (unlocked.size() <= 1) {
            player.sendMessage(plugin.prefix() + "§7Slot " + (char)('A'+slot) + ": chưa có sub-skill khác mở khóa.");
            return;
        }
        data.cycleSubSkill(slot, unlocked.size());
        refreshMergedWeapon(player, data);
        var entry = unlocked.get(data.getActiveSubSkill(slot));
        player.sendMessage(plugin.prefix() + "§b↺ §e" + (char)('A'+slot) + "§b: §f"
                + entry.displayName() + " §7(" + (data.getActiveSubSkill(slot)+1) + "/" + unlocked.size() + ")");
    }

    private void cycleComboSlot(Player player, int slot) {
        var data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;
        var unlocked = ComboSkillRegistry.getUnlockedSubSkills(
            data.getClassType(), slot, data.getStage());
        if (unlocked.size() <= 1) {
            player.sendMessage(plugin.prefix() + "§7Slot " + (char)('A'+slot) + ": chưa có kỹ năng phụ khác mở khóa.");
            return;
        }
        data.cycleSubSkill(slot, unlocked.size());
        int newIdx = data.getActiveSubSkill(slot);
        var entry = unlocked.get(newIdx);

        // UPDATE ITEM IN PLACE - chi thay the slot tuong ung, KHONG tao lai toan bo
        Skill skill = plugin.getSkillManager().getSkill(entry.id());
        if (skill == null) skill = buildPlaceholderSkill(entry, data.getClassType());
        ItemStack newItem = createComboSkillItem(skill, slot, data.getClassType(), unlocked, newIdx);
        player.getInventory().setItem(slot, newItem);  // Set truc tiep vao slot = khong tao them item

        player.sendMessage(plugin.prefix() + "§b↺ §e" + (char)('A'+slot) + "§b: §f"
            + entry.displayName() + " §7(" + (newIdx+1) + "/" + unlocked.size() + ")");
    }

    private int getComboSlot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        NamespacedKey key = new NamespacedKey(plugin, "combo_slot");
        var container = item.getItemMeta().getPersistentDataContainer();
        if (!container.has(key, PersistentDataType.INTEGER)) return -1;
        return container.get(key, PersistentDataType.INTEGER);
    }

    private void tryUseSkill(Player player, String skillId) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cBạn chưa chọn class tu tiên! Dùng §e/tutien chon");
            return;
        }
        if (!data.isModuleEnabled("ky_nang_them")) {
            player.sendMessage(plugin.prefix() + "§cBạn đã tắt hệ thống Kỹ Năng Thêm trong /kinang.");
            return;
        }
        String normalizedSkillId = skillId == null ? "" : skillId.toLowerCase(java.util.Locale.ROOT);
        if (isEvasionSkill(normalizedSkillId) && !data.isModuleEnabled("don_thuat")) {
            player.sendMessage(plugin.prefix() + "§cBạn đã tắt Độn Thuật trong /kinang.");
            return;
        }
        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill != null) {
            skill.tryUse(player, data);
        } else {
            // Combo skill chua trien khai day du: dung placeholder
            player.sendMessage(plugin.prefix() + "§e⚔ " + skillId + " §7(Đang phát triển!)");
            player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT,
                player.getLocation().add(0,1,0), 10, 0.4,0.4,0.4,0.1);
        }
    }


    private boolean isEvasionSkill(String id) {
        return id.contains("don_thuat") || id.contains("dich_chuyen") || id.contains("teleport")
                || id.contains("blink") || id.contains("dash") || id.contains("escape")
                || id.contains("thiendi") || id.contains("thoatxac") || id.contains("evasion");
    }

    private String getSkillIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        // Uu tien doc tu NBT (combo skill items)
        NamespacedKey skillKey = new NamespacedKey(plugin, "skill_id");
        String nbtId = meta.getPersistentDataContainer().get(skillKey,
            org.bukkit.persistence.PersistentDataType.STRING);
        if (nbtId != null && !nbtId.isEmpty()) return nbtId;
        // Fallback: parse name (skill items cu)
        if (!meta.hasDisplayName()) return null;
        String raw = ChatColor.stripColor(meta.getDisplayName()).toLowerCase().trim();
        return itemNameToSkill.get(raw);
    }

    // ========================
    //  TAO SKILL ITEMS
    // ========================

    /**
     * Cap phat bo skill items cho nguoi choi theo class cua ho.
     * Items dat vao hotbar slot 0-3, slot 8 la "Tui Linh Khi".
     */
    public void giveSkillItems(Player player, PlayerData data) {
        if (!data.hasClass()) return;
        ClassType ct = data.getClassType();
        // Xoa skill items chi o HOTBAR (0-8) - KHONG xoa backpack de tranh mat do
        // Dung NBT check de tranh xoa nham item cua player
        for (int i = 0; i < 9; i++) {
            ItemStack cur = player.getInventory().getItem(i);
            if (isTuTienItem(cur)) player.getInventory().setItem(i, null);
        }
        // 1 item GHEP duy nhat o slot 0 (thay cho 4 item rieng A/B/C/D truoc day)
        player.getInventory().setItem(0, createMergedWeaponItem(player, data));
        // Da bo "sach Linh Khi & Thong Tin" o slot 8 theo yeu cau - khong dat gi vao do nua
        // Silent - khong spam message moi lan cap phat
    }

    /**
     * MERGED WEAPON: gop 4 combo-slot item (A/B/C/D) lam MOT item duy nhat.
     * Cach dung (giong dung 4 nut co san tren Bedrock/Java, khong can doi item):
     *   Chuot phai (khong cui)      -> Slot A
     *   Chuot phai + Cui (Sneak)    -> Slot B
     *   Doi tay - F / nut Swap Hand -> Slot C
     *   Vut do - Q / nut Drop Item  -> Slot D (Tuyet Ky)
     * Doi sub-skill trong 1 slot: /doiky <a|b|c|d>
     */
    public ItemStack createMergedWeaponItem(Player player, PlayerData data) {
        ClassType ct = data.getClassType();
        var comboSlots = ComboSkillRegistry.getComboSlots(ct);
        String[] slotLetter = {"A","B","C","D"};
        String[] slotLabel  = {"Chuột phải", "Sneak+Chuột phải", "Sneak+Chém / F", "Vứt đồ (Q) ✦Tuyệt Kỹ"};

        Material mat = getSkillMaterial(comboSlots.isEmpty() || comboSlots.get(0).isEmpty()
                ? "default" : comboSlots.get(0).get(0).id());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ct.getColor() + "⚔ " + ct.getColoredName() + " §f- Linh Khí Kiếm");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE);

        List<String> lore = new ArrayList<>();
        lore.add("§8════════════════════");
        NamespacedKey mergedKey = new NamespacedKey(plugin, "tutien_merged_weapon");
        meta.getPersistentDataContainer().set(mergedKey, PersistentDataType.BYTE, (byte)1);

        for (int slot = 0; slot < 4 && slot < comboSlots.size(); slot++) {
            var unlocked = ComboSkillRegistry.getUnlockedSubSkills(ct, slot, data.getStage());
            NamespacedKey slotKey = new NamespacedKey(plugin, "skill_slot_" + slot);
            if (unlocked.isEmpty()) {
                lore.add("§8[" + slotLetter[slot] + "] §8Chưa mở khóa");
                continue;
            }
            int activeIdx = Math.min(data.getActiveSubSkill(slot), unlocked.size() - 1);
            var entry = unlocked.get(activeIdx);
            Skill skill = plugin.getSkillManager().getSkill(entry.id());
            if (skill == null) skill = buildPlaceholderSkill(entry, ct);
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, skill.getId());

            String tag = slot == 3 ? "§d✦[Tuyệt Kỹ]" : "§b[Slot " + slotLetter[slot] + "]";
            lore.add(tag + " §f" + skill.getDisplayName() + " §7(" + slotLabel[slot] + ")");
            lore.add("§8  └ §7" + skill.getDescription());
        }
        lore.add("§8────────────────────");
        lore.add("§7Đổi kỹ năng trong 1 slot: §f/doiky <a|b|c|d>");
        lore.add("§8════════════════════");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Lay skillId dang gan cho 1 slot (0-3) tren item ghep. */
    private String getMergedSkillId(ItemStack item, int slot) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(plugin, "skill_slot_" + slot);
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean isMergedWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "tutien_merged_weapon");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Cap nhat item ghep trong slot 0 (goi sau /doiky hoac len canh gioi). */
    public void refreshMergedWeapon(Player player, PlayerData data) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isMergedWeapon(s)) {
                player.getInventory().setItem(i, createMergedWeaponItem(player, data));
                return;
            }
        }
    }


    private ItemStack createComboSkillItem(Skill skill, int slot, ClassType ct,
                                              java.util.List<ComboSkillRegistry.SkillEntry> unlocked,
                                              int activeIdx) {
        Material mat = getSkillMaterial(skill.getId());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        // Name shows active skill + slot letter
        String slotLetter = new String[]{"A","B","C","D"}[slot];
        meta.setDisplayName(ct.getColor() + "[" + slotLetter + "] " + skill.getDisplayName());
        // Khong setUnbreakable de giam component count
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
            ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE);
        List<String> lore = new ArrayList<>();
        lore.add("§8════════════════════");
        lore.add("§7" + skill.getDescription());
        lore.add("§8────────────────────");
        lore.add("§eCooldown: §f" + (skill.getCooldownMs()/1000) + "s");
        lore.add("§8────────────────────");
        lore.add("§7Sub-skills §e(" + (activeIdx+1) + "/" + unlocked.size() + ")§7:");
        for (int i = 0; i < unlocked.size(); i++) {
            String mark = i == activeIdx ? "§a▶ " : "§8  ";
            lore.add(mark + "§f" + unlocked.get(i).displayName());
        }
        // Locked sub-skills
        var allSubs = ComboSkillRegistry.getComboSlots(ct).get(slot);
        long lockedCount = allSubs.stream()
            .filter(e -> unlocked.stream().noneMatch(u -> u.id().equals(e.id()))).count();
        if (lockedCount > 0)
            lore.add("§8[+" + lockedCount + " skill bị khóa - cần lên cấp]");
        lore.add("§8════════════════════");
        lore.add("§a► Chuột phải/F/Q §7= Dùng  §e|  §aShift+Q §7= Đổi kỹ năng");
        meta.setLore(lore);
        // Store slot index AND skill id in NBT
        NamespacedKey slotKey  = new NamespacedKey(plugin, "combo_slot");
        NamespacedKey skillKey = new NamespacedKey(plugin, "skill_id");
        meta.getPersistentDataContainer().set(slotKey,  org.bukkit.persistence.PersistentDataType.INTEGER, slot);
        meta.getPersistentDataContainer().set(skillKey, org.bukkit.persistence.PersistentDataType.STRING, skill.getId());
        item.setItemMeta(meta);
        return item;
    }

    private Skill buildPlaceholderSkill(ComboSkillRegistry.SkillEntry entry, ClassType ct) {
        // Placeholder for skills not yet in SkillManager
        return new Skill(plugin, entry.id(), entry.displayName(), "(Dang phat trien)", ct, 15000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, com.tutien.data.PlayerData data) {
                player.sendMessage(plugin.prefix() + "§eSkill §a" + entry.displayName() + " §eđang được triển khai!");
                return true;
            }
        };
    }

    /** Creates an independently usable physical item for a registered class skill (including extra skills outside A/B/C/D). */
    public ItemStack createStandaloneSkillItem(PlayerData data, String rawSkillId) {
        if (data == null || !data.hasClass() || rawSkillId == null) return null;
        String skillId = rawSkillId.trim().toLowerCase(java.util.Locale.ROOT);
        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return null;
        boolean topRealm = data.getStage() != null && data.getStage().canUseAllSkills();
        if (!topRealm && !data.getClassType().getSkillIds().contains(skillId)) return null;
        if (!topRealm && skill.getRequiredClass() != null && skill.getRequiredClass() != data.getClassType()) return null;
        if (!topRealm && data.getRank() < skill.getMinRank()) return null;
        ItemStack item = createSkillItem(skill, 0, data.getClassType());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (!lore.isEmpty()) lore.set(lore.size() - 1, "§a► Chuột phải §7để thi triển kỹ năng");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSkillItem(Skill skill, int slot, ClassType ct) {
        Material mat = getSkillMaterial(skill.getId());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        // Ten item = ten skill (dung de detect)
        meta.setDisplayName(ct.getColor() + toItemName(skill.getId()));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.setUnbreakable(true);
        // Luu skill ID vao NBT - PC va PE deu doc duoc
        NamespacedKey sk = new NamespacedKey(plugin, "skill_id");
        meta.getPersistentDataContainer().set(sk,
            org.bukkit.persistence.PersistentDataType.STRING, skill.getId());

        List<String> lore = new ArrayList<>();
        lore.add("§8════════════════════");
        lore.add("§7" + skill.getDescription());
        lore.add("§8────────────────────");
        lore.add("§eCooldown: §f" + (skill.getCooldownMs() / 1000) + "s");
        lore.add("§8════════════════════");
        lore.add("§a► Chuột phải §7/ §eF §7/ §eQ §7để sử dụng");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }


    // Da bo "sach Linh Khi & Thong Tin" theo yeu cau. Ham nay gio chi con tac dung
    // don sach nhung cuon sach CU con sot lai trong tui nguoi choi tu ban truoc,
    // giu nguyen chu ky ham (khong doi) vi co ~16 noi trong code dang goi no.
    public void updateInfoItem(Player player, PlayerData data) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s != null && s.getType() == Material.BOOK && isInfoItem(s)
                && !plugin.getPhaBaoManager().isPhaBao(s)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private boolean isInfoItem(ItemStack item) {
        // Dung NBT "tutien_info" - an toan, khong false-positive voi item khac
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "tutien_info");
        return item.getItemMeta().getPersistentDataContainer()
            .has(key, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    private boolean isSkillItem(ItemStack item) {
        // CHI check NBT - KHONG dung name fallback de tranh xoa nham item cua player
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey skillKey = new NamespacedKey(plugin, "skill_id");
        String nbtId = item.getItemMeta().getPersistentDataContainer()
            .get(skillKey, org.bukkit.persistence.PersistentDataType.STRING);
        return nbtId != null && !nbtId.isEmpty();
    }

    private boolean isTuTienItem(ItemStack item) {
        // KHONG xoa PhaBao du chung co material trung
        if (plugin.getPhaBaoManager().isPhaBao(item)) return false;
        // Check combo_slot OR skill_id OR info item OR merged weapon
        return isSkillItem(item)
            || isMergedWeapon(item)
            || getComboSlot(item) >= 0
            || (item != null && item.getType() == Material.BOOK && isInfoItem(item));
    }

    public Material getSkillMaterialPublic(String id) { return getSkillMaterial(id); }

    // Tra ve Material theo class - uu tien item don gian de giam component count
    private Material getSkillMaterial(String skillId) {
        if (skillId == null) return Material.STICK;
        String id = skillId.toLowerCase();
        // KIEM TU
        if (id.contains("kiem") || id.equals("thiensat") || id.equals("tockiem"))
            return id.contains("xuyentam") || id.contains("he_pho") ? Material.GOLDEN_SWORD
                : id.contains("thiensat") || id.contains("nhat_kiem") ? Material.DIAMOND_SWORD
                : Material.IRON_SWORD;
        // PHAP TU
        if (id.contains("phap") || id.contains("linhhoa") || id.contains("dongbang")
            || id.contains("thuy_long") || id.contains("ngu_hanh") || id.contains("cuong_loi")
            || id.contains("thien_mang") || id.contains("linh_vu") || id.contains("van_phap"))
            return id.contains("ngu_sac") || id.contains("thien_di") ? Material.BLAZE_ROD : Material.STICK;
        // Y TU
        if (id.contains("hoi_sinh") || id.contains("cam_lo") || id.contains("phuc_hoa")
            || id.contains("san_sinh") || id.contains("thien_y") || id.contains("linh_y")
            || id.contains("van_duoc") || id.contains("hua_tuo") || id.contains("y_dao")
            || id.contains("bao_shen") || id.equals("hoiduongcuunghich") || id.equals("linhduoc")
            || id.equals("phuhotran") || id.equals("giaido"))
            return id.contains("linh_y_chi") || id.contains("bao_shen") ? Material.TOTEM_OF_UNDYING
                : Material.GOLDEN_APPLE;
        // DOC TU
        if (id.contains("doc") || id.contains("manhdoc") || id.contains("amsatkiem")
            || id.contains("thanh_doc") || id.contains("am_sat_doc") || id.contains("shi_ling")
            || id.contains("du_jing") || id.contains("tian_du") || id.contains("wan_du"))
            return id.contains("du_zun") || id.contains("vandocchi") ? Material.FERMENTED_SPIDER_EYE
                : Material.SPIDER_EYE;
        // THE TU
        if (id.contains("kimcuong") || id.contains("thietbich") || id.contains("hophap")
            || id.contains("gang_") || id.contains("kim_zhong") || id.contains("tie_bi")
            || id.contains("bu_huai") || id.contains("bu_mie") || id.contains("tian_gang")
            || id.contains("jin_ti") || id.contains("phandon") || id.contains("wan_lu"))
            return id.contains("bathoai") || id.contains("jin_shen") || id.contains("wu_shang_jin")
                ? Material.DIAMOND : Material.SHIELD;
        // QUY TU
        if (id.contains("quy") || id.contains("tuanh") || id.contains("ling_hun")
            || id.contains("si_wang") || id.contains("yin_yue") || id.contains("gui_")
            || id.contains("ling_yu") || id.contains("wan_gui"))
            return id.contains("vuong") || id.contains("zu_wu") ? Material.WITHER_SKELETON_SKULL
                : id.contains("ling_yu") || id.contains("po_jun") ? Material.SOUL_SAND
                : Material.PHANTOM_MEMBRANE;
        // PHONG TU
        if (id.contains("phong") || id.contains("feng_") || id.contains("bao_feng")
            || id.contains("tian_feng") || id.contains("jue_feng") || id.contains("tinh_phong")
            || id.contains("kuang_feng") || id.contains("wu_ji_feng"))
            return id.contains("thuandi") || id.contains("wu_ji") ? Material.ENDER_EYE
                : id.contains("jue_feng") ? Material.ENDER_PEARL : Material.FEATHER;
        // HOA TU
        if (id.contains("hoa") || id.contains("nghiephoa") || id.contains("hoalong")
            || id.contains("huo_") || id.contains("yan_long") || id.contains("tian_huo")
            || id.contains("wan_huo") || id.contains("wu_shang_zhen") || id.contains("tian_yan"))
            return id.contains("phuonghoang") || id.contains("zhen_huo") ? Material.FIRE_CHARGE
                : id.contains("hoalong") || id.contains("yan_long") ? Material.DRAGON_BREATH
                : Material.MAGMA_CREAM;
        return Material.BLAZE_ROD;
    }

    // Chuyen skillId thanh ten item chuan (khong mau - phai khop itemNameToSkill)
    private String toItemName(String skillId) {
        for (Map.Entry<String, String> e : itemNameToSkill.entrySet()) {
            if (e.getValue().equals(skillId)) return capitalizeWords(e.getKey());
        }
        return skillId;
    }

    private String capitalizeWords(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatNum(long n) {
        if (n >= 1_000_000_000_000L) return String.format("%.1fT", n / 1_000_000_000_000.0);
        if (n >= 1_000_000_000)      return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000)          return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)              return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}
