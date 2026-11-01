package com.tutien.breakthrough.quest;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BreakthroughQuestManager implements Listener {

    private final TuTienPlugin plugin;

    public static final String KEY_MOB_KILLS      = "bt_kill_mobs";
    public static final String KEY_HERBS          = "bt_herbs";
    public static final String KEY_ALCHEMY        = "bt_alchemy";
    public static final String KEY_MEDITATE       = "bt_meditate_minutes";
    public static final String KEY_BOSS_KILLS     = "bt_boss_kills";
    public static final String KEY_NOTIFIED_DONE  = "bt_quest_notified";

    public static final String GUI_TITLE = "§6✦ NHIỆM VỤ ĐỘT PHÁ & TU LUYỆN ✦";

    public BreakthroughQuestManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Kiểm tra chế độ quest trong config.
     * BUILT_IN (mặc định): Tự động theo dõi 5 hoạt động tu tiên.
     * TOKEN: Cơ chế cũ, dùng Đột Phá Lệnh.
     */
    public boolean isBuiltinQuestMode() {
        String mode = plugin.getConfig().getString("cultivation.breakthrough.quest-mode", "BUILT_IN");
        return "BUILT_IN".equalsIgnoreCase(mode);
    }

    /**
     * Lấy cấu hình nhiệm vụ đột phá cho cảnh giới hiện tại của người chơi.
     */
    public BreakthroughQuest getQuestFor(PlayerData data) {
        CultivationStage stage = data.getStage();
        String stageKey = stage.name();

        String targetName;
        if (stage.hasNextRank(data.getRank())) {
            targetName = stage.getDisplayName() + " " + stage.getRankName(data.getRank() + 1);
        } else if (stage.nextStage() != null) {
            targetName = stage.nextStage().getDisplayName() + " Sơ Kỳ";
        } else {
            targetName = stage.getFullDisplay(data.getRank()) + " §e[Đỉnh Phong]";
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("cultivation.breakthrough.builtin-quests." + stageKey);
        if (sec != null) {
            return new BreakthroughQuest(
                    sec.getString("target-stage", targetName),
                    sec.getInt("kill-mobs", 100),
                    sec.getString("mob-desc", "Săn quái dã ngoại ban đêm hoặc Dungeon"),
                    sec.getInt("herbs", 20),
                    sec.getString("herb-name", "Huyết Linh Thảo (Red Tulip)"),
                    sec.getInt("alchemy", 2),
                    sec.getString("dan-name", "Huyết Khí Đan (dùng lò luyện /dan)"),
                    sec.getInt("meditate-minutes", 10),
                    sec.getString("meditate-desc", "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí"),
                    sec.getInt("boss-kills", 0),
                    sec.getString("boss-desc", "Boss Thế Giới hoặc Wither"),
                    sec.getString("dan-ho-menh", "Trúc Cơ Đan"),
                    sec.getString("dan-desc", "Chuẩn bị độ kiếp phá cảnh")
            );
        }

        // Mặc định chuẩn tu tiên theo từng phân khúc cảnh giới
        int ord = stage.ordinal();
        if (stage == CultivationStage.BAN_THANH) {
            return new BreakthroughQuest(
                    targetName,
                    450,
                    "Săn quái dã ngoại ban đêm hoặc Dungeon",
                    50,
                    "Huyết Linh Thảo (Red Tulip)",
                    6,
                    "Huyết Khí Đan (dùng lò luyện /dan)",
                    50,
                    "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    2,
                    "Boss Thế Giới hoặc Wither",
                    "Chuẩn Thánh Đan",
                    "Chuẩn bị thành thánh chân chính, ngưng tụ thánh uy"
            );
        }

        if (ord == 0) { // LUYEN_KHI
            return new BreakthroughQuest(
                    targetName,
                    25, "Săn quái dã ngoại ban đêm",
                    5, "Cỏ Linh Tuyền",
                    0, "Chưa yêu cầu luyện đan",
                    0, "Chưa yêu cầu thiền định",
                    0, "Chưa yêu cầu trảm boss",
                    "Tụ Linh Đan", "Dẫn khí nhập thể, đả thông kinh mạch"
            );
        } else if (ord == 1) { // TRUC_CO
            return new BreakthroughQuest(
                    targetName,
                    40, "Săn quái dã ngoại ban đêm hoặc Hang Động",
                    10, "Huyết Linh Thảo",
                    2, "Bổ Huyết Đan (dùng lò luyện /luyen)",
                    5, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    0, "Chưa yêu cầu trảm boss",
                    "Trúc Cơ Đan", "Đúc thành đạo cơ vững chắc, ngưng tụ linh dịch"
            );
        } else if (ord == 2) { // KET_DAN
            return new BreakthroughQuest(
                    targetName,
                    70, "Săn quái dã ngoại hoặc quái Nether",
                    20, "U Hồn Thảo",
                    4, "Tẩy Tủy Đan (dùng lò luyện /luyen)",
                    10, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    0, "Chưa yêu cầu trảm boss",
                    "Ngưng Kim Đan", "Ngưng tụ kim đan bất toái, phá toái hư không"
            );
        } else if (ord == 3) { // NGUYEN_ANH
            return new BreakthroughQuest(
                    targetName,
                    120, "Săn quái dã ngoại ban đêm hoặc Bí Cảnh",
                    30, "Huyền Băng Hoa",
                    5, "Nguyên Anh Đan (dùng lò luyện /luyen)",
                    20, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    1, "Boss Thế Giới, Wither hoặc Boss Bí Cảnh",
                    "Nguyên Anh Đan", "Hộ trì nguyên anh sơ sinh, chống thiên lôi bổ"
            );
        } else if (ord == 4) { // HOA_THAN
            return new BreakthroughQuest(
                    targetName,
                    180, "Săn quái dã ngoại hoặc Dungeon nguy hiểm",
                    35, "Cửu Diệp Chi",
                    5, "Hóa Thần Đan (dùng lò luyện /luyen)",
                    25, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    1, "Boss Thế Giới hoặc Wither",
                    "Hóa Thần Đan", "Thần thức phân hóa, câu thông thiên địa linh lực"
            );
        } else if (ord < 9) { // 5-8: Luyen Hu, Hop The, Dai Thua, Do Kiep
            int mobTarget = 200 + (ord - 4) * 60;
            int herbTarget = 35 + (ord - 4) * 5;
            int medTarget = 25 + (ord - 4) * 5;
            int bossTarget = ord >= 7 ? 2 : 1;
            return new BreakthroughQuest(
                    targetName,
                    mobTarget, "Săn quái dã ngoại ban đêm hoặc Dungeon",
                    herbTarget, "Ngũ Sắc Liên / Thiên Tinh Thảo",
                    6, "Cửu Chuyển Hoàn Hồn Đan (dùng /luyen)",
                    medTarget, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    bossTarget, "Boss Thế Giới hoặc Wither",
                    stage.getDisplayName() + " Đan", "Tẩy trần tục khí, chuyển hóa thánh thể"
            );
        } else { // >= 10: Thanh, Tien, Thien, Vo Thuong
            int mobTarget = Math.min(1500, 450 + (ord - 9) * 70);
            int herbTarget = Math.min(200, 50 + (ord - 9) * 10);
            int medTarget = Math.min(180, 50 + (ord - 9) * 8);
            int bossTarget = Math.min(10, 2 + (ord - 9) / 2);
            return new BreakthroughQuest(
                    targetName,
                    mobTarget, "Săn quái dã ngoại ban đêm hoặc Dungeon cấp cao",
                    herbTarget, "Tiên Dược Vạn Năm (Dược Viên /dongphu)",
                    8, "Thần Phẩm Đan Dược (dùng /luyen)",
                    medTarget, "Gõ /thiendinh ngồi yên tĩnh tọa nạp khí",
                    bossTarget, "Boss Thế Giới, Wither hoặc Boss Tối Thượng",
                    stage.getDisplayName() + " Thánh Đan", "Ngưng tụ chí tôn chân ý, độ kiếp đăng phong"
            );
        }
    }

    /**
     * Tạo chuỗi thanh tiến độ 10 ô đẹp mắt: [■■■■■□□□□□]
     */
    public String getProgressBar(int current, int max) {
        if (max <= 0) return "§a[■■■■■■■■■■]";
        int clamped = Math.max(0, Math.min(current, max));
        int filled = (int) Math.round(((double) clamped / max) * 10.0);
        filled = Math.max(0, Math.min(10, filled));
        int empty = 10 - filled;
        return "§a[" + "■".repeat(filled) + "§7" + "□".repeat(empty) + "§a]";
    }

    /**
     * Kiểm tra người chơi đã hoàn thành toàn bộ 5 mục tiêu hay chưa.
     */
    public boolean isQuestCompleted(Player player, PlayerData data) {
        if (!isBuiltinQuestMode()) return true;
        BreakthroughQuest q = getQuestFor(data);
        int mobs = data.getExtraInt(KEY_MOB_KILLS, 0);
        int herbs = data.getExtraInt(KEY_HERBS, 0);
        int alchemy = data.getExtraInt(KEY_ALCHEMY, 0);
        int meditate = data.getExtraInt(KEY_MEDITATE, 0);
        int boss = data.getExtraInt(KEY_BOSS_KILLS, 0);

        return mobs >= q.getKillMobs()
                && herbs >= q.getHerbs()
                && alchemy >= q.getAlchemy()
                && meditate >= q.getMeditateMinutes()
                && boss >= q.getBossKills();
    }

    /**
     * Tạo ItemStack hiển thị tại Slot 13 trong GUI Túi Đồ và Quest GUI.
     */
    public ItemStack buildQuestItem(PlayerData data) {
        BreakthroughQuest q = getQuestFor(data);
        int mobs = data.getExtraInt(KEY_MOB_KILLS, 0);
        int herbs = data.getExtraInt(KEY_HERBS, 0);
        int alchemy = data.getExtraInt(KEY_ALCHEMY, 0);
        int meditate = data.getExtraInt(KEY_MEDITATE, 0);
        int boss = data.getExtraInt(KEY_BOSS_KILLS, 0);

        boolean completed = (mobs >= q.getKillMobs()
                && herbs >= q.getHerbs()
                && alchemy >= q.getAlchemy()
                && meditate >= q.getMeditateMinutes()
                && boss >= q.getBossKills());

        ItemStack item = new ItemStack(completed ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6§l✦ NHIỆM VỤ ĐỘT PHÁ ✦");
        List<String> lore = new ArrayList<>();
        lore.add("§8══════════════════════════════════════════");
        lore.add("§eMục tiêu đột phá: §f" + q.getTargetStageName());
        lore.add("§c⚔ Tiêu diệt quái: " + getProgressBar(mobs, q.getKillMobs()) + " §e" + mobs + "/" + q.getKillMobs());
        lore.add("  §8↳ §7" + q.getMobDesc());
        lore.add("§a🌿 Thu hái Tiên Thảo: " + getProgressBar(herbs, q.getHerbs()) + " §e" + herbs + "/" + q.getHerbs());
        lore.add("  §8↳ §7Chỉ định loại: " + q.getHerbName());
        lore.add("§d🍶 Luyện chế Linh Đan: " + getProgressBar(alchemy, q.getAlchemy()) + " §e" + alchemy + "/" + q.getAlchemy());
        lore.add("  §8↳ §7Chỉ định loại: " + q.getDanName());
        lore.add("§b🧘 Thiền định: " + getProgressBar(meditate, q.getMeditateMinutes()) + " §e" + meditate + "/" + q.getMeditateMinutes() + " phút");
        lore.add("  §8↳ §7" + q.getMeditateDesc());
        lore.add("§4👑 Trảm sát Boss: " + getProgressBar(boss, q.getBossKills()) + " §e" + boss + "/" + q.getBossKills());
        lore.add("  §8↳ §7" + q.getBossDesc());
        lore.add("§8══════════════════════════════════════════");
        lore.add("§e💡 Đan Dược Hộ Mệnh: §6" + q.getDanHoMenh());
        lore.add("  §8↳ §7" + q.getDanDesc());

        if (completed) {
            lore.add("§8══════════════════════════════════════════");
            lore.add("§a✔ ĐÃ HOÀN THÀNH - SẴN SÀNG ĐỘT PHÁ!");
            lore.add("§eGõ §a/dotpha §eđể bắt đầu nghênh tiếp Thiên Kiếp.");
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        } else {
            lore.add("§8══════════════════════════════════════════");
            lore.add("§7Click để xem hướng dẫn chi tiết trong chat.");
        }

        meta.addItemFlags(ItemFlag.values());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gửi toàn bộ trạng thái tiến độ Nhiệm Vụ Đột Phá ra khung chat theo mẫu thiết kế.
     */
    public void sendQuestChatStatus(Player player, PlayerData data) {
        BreakthroughQuest q = getQuestFor(data);
        int mobs = data.getExtraInt(KEY_MOB_KILLS, 0);
        int herbs = data.getExtraInt(KEY_HERBS, 0);
        int alchemy = data.getExtraInt(KEY_ALCHEMY, 0);
        int meditate = data.getExtraInt(KEY_MEDITATE, 0);
        int boss = data.getExtraInt(KEY_BOSS_KILLS, 0);

        player.sendMessage("§6§l✦ NHIỆM VỤ ĐỘT PHÁ ✦");
        player.sendMessage("§8══════════════════════════════════════════");
        player.sendMessage("§eMục tiêu đột phá: §f" + q.getTargetStageName());
        player.sendMessage("§c⚔ Tiêu diệt quái: " + getProgressBar(mobs, q.getKillMobs()) + " §e" + mobs + "/" + q.getKillMobs());
        player.sendMessage("  §8↳ §7" + q.getMobDesc());
        player.sendMessage("§a🌿 Thu hái Tiên Thảo: " + getProgressBar(herbs, q.getHerbs()) + " §e" + herbs + "/" + q.getHerbs());
        player.sendMessage("  §8↳ §7Chỉ định loại: " + q.getHerbName());
        player.sendMessage("§d🍶 Luyện chế Linh Đan: " + getProgressBar(alchemy, q.getAlchemy()) + " §e" + alchemy + "/" + q.getAlchemy());
        player.sendMessage("  §8↳ §7Chỉ định loại: " + q.getDanName());
        player.sendMessage("§b🧘 Thiền định: " + getProgressBar(meditate, q.getMeditateMinutes()) + " §e" + meditate + "/" + q.getMeditateMinutes() + " phút");
        player.sendMessage("  §8↳ §7" + q.getMeditateDesc());
        player.sendMessage("§4👑 Trảm sát Boss: " + getProgressBar(boss, q.getBossKills()) + " §e" + boss + "/" + q.getBossKills());
        player.sendMessage("  §8↳ §7" + q.getBossDesc());
        player.sendMessage("§8══════════════════════════════════════════");
        player.sendMessage("§e💡 Đan Dược Hộ Mệnh: §6" + q.getDanHoMenh());
        player.sendMessage("  §8↳ §7" + q.getDanDesc());

        boolean completed = (mobs >= q.getKillMobs()
                && herbs >= q.getHerbs()
                && alchemy >= q.getAlchemy()
                && meditate >= q.getMeditateMinutes()
                && boss >= q.getBossKills());

        if (completed) {
            player.sendMessage("§a✦ §lCHÚC MỪNG: §aBạn đã hoàn tất toàn bộ 5 mục tiêu!");
            player.sendMessage("§eDùng lệnh §a/dotpha §eđể kích hoạt Thiên Kiếp vượt ải ngay bây giờ!");
        } else {
            player.sendMessage("§8Tiếp tục hoàn thành các mục tiêu trên để mở khóa Thiên Kiếp.");
        }
    }

    /**
     * Mở GUI trực quan tổng hợp nhiệm vụ Tu Tiên.
     */
    public void openQuestGUI(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(
                null, 27,
                LegacyComponentSerializer.legacySection().deserialize(GUI_TITLE));

        // Viền trang trí
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName("§8");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // Slot 13: Trọng tâm Nhiệm Vụ Đột Phá
        inv.setItem(13, buildQuestItem(data));

        // Slot 10: Thông tin Tu Vi
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e§lHỒ SƠ TU VI");
        im.setLore(List.of(
                "§7Đạo hữu: §f" + player.getName(),
                "§7Cảnh giới: " + data.getStage().getFullDisplay(data.getRank()),
                "§7Linh khí: §a" + data.getLinhKhi() + "§7/§e" + data.getMaxLinhKhi(),
                "",
                "§7Hoàn thành nhiệm vụ ở ô giữa để đột phá!"
        ));
        info.setItemMeta(im);
        inv.setItem(10, info);

        // Slot 16: Hướng dẫn & Đột Phá Lệnh
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = star.getItemMeta();
        sm.setDisplayName("§d§lTHIÊN KIẾP ĐỘT PHÁ");
        sm.setLore(List.of(
                "§7Chế độ: §a" + (isBuiltinQuestMode() ? "Tích Hợp Sẵn (Built-in)" : "Lệnh Bài (Token)"),
                "§7Không cần cài thêm plugin nhiệm vụ ngoài.",
                "§7Làm đủ 5 điều kiện sẽ được tự động mở khóa.",
                "",
                "§eNhấn để gõ lệnh: §a/dotpha"
        ));
        sm.addItemFlags(ItemFlag.values());
        star.setItemMeta(sm);
        inv.setItem(16, star);

        // Slot 22: Nút đóng
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("§c✘ Đóng Giao Diện");
        close.setItemMeta(cm);
        inv.setItem(22, close);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuestGUIClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().getTitle().contains("NHIỆM VỤ ĐỘT PHÁ & TU LUYỆN")) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == 22) {
                player.closeInventory();
            } else if (slot == 13) {
                player.closeInventory();
                PlayerData data = plugin.getDataManager().get(player);
                if (data != null) sendQuestChatStatus(player, data);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            } else if (slot == 16) {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "dotpha");
            }
        }
    }

    // ── PROGRESS RECORDING ──────────────────────────────────────────────

    public void onMobKill(Player player, Entity entity) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        int cur = data.getExtraInt(KEY_MOB_KILLS, 0);
        data.setExtraInt(KEY_MOB_KILLS, cur + 1);

        // Check if boss
        double minBossHp = plugin.getConfig().getDouble("cultivation.boss-min-health", 150.0);
        boolean isBoss = entity instanceof Wither
                || entity instanceof EnderDragon
                || entity instanceof Warden
                || (entity instanceof org.bukkit.entity.LivingEntity le && le.getMaxHealth() >= minBossHp && entity.getCustomName() != null);

        if (isBoss) {
            onBossKill(player);
        } else {
            checkCompletion(player, data);
        }
    }

    public void onBossKill(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        int cur = data.getExtraInt(KEY_BOSS_KILLS, 0);
        data.setExtraInt(KEY_BOSS_KILLS, cur + 1);
        checkCompletion(player, data);
    }

    public void onHerbHarvest(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        int cur = data.getExtraInt(KEY_HERBS, 0);
        data.setExtraInt(KEY_HERBS, cur + 1);
        checkCompletion(player, data);
    }

    public void onAlchemy(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        int cur = data.getExtraInt(KEY_ALCHEMY, 0);
        data.setExtraInt(KEY_ALCHEMY, cur + 1);
        checkCompletion(player, data);
    }

    public void onMeditate(Player player, int minutes) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) return;

        int cur = data.getExtraInt(KEY_MEDITATE, 0);
        data.setExtraInt(KEY_MEDITATE, cur + Math.max(1, minutes));
        checkCompletion(player, data);
    }

    private void checkCompletion(Player player, PlayerData data) {
        if (!isBuiltinQuestMode()) return;
        boolean done = isQuestCompleted(player, data);
        boolean notified = data.getExtraBool(KEY_NOTIFIED_DONE, false);

        if (done && !notified) {
            data.setExtraBool(KEY_NOTIFIED_DONE, true);
            player.sendTitle("§6✦ HOÀN THÀNH NHIỆM VỤ ĐỘT PHÁ ✦", "§aĐã mở khóa Thiên Kiếp! Dùng §e/dotpha", 10, 80, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage(plugin.prefix() + "§a§l✓ BẠN ĐÃ HOÀN THÀNH TOÀN BỘ 5 MỤC TIÊU TU LUYỆN!");
            player.sendMessage(plugin.prefix() + "§eThiên cơ đã thông suốt. Hãy chuẩn bị đan dược và gõ §a/dotpha §eđể vượt kiếp!");
        }
    }

    /**
     * Đặt lại tiến độ quest sau khi đột phá thành công sang cảnh giới mới.
     */
    public void resetProgress(PlayerData data) {
        data.setExtraInt(KEY_MOB_KILLS, 0);
        data.setExtraInt(KEY_HERBS, 0);
        data.setExtraInt(KEY_ALCHEMY, 0);
        data.setExtraInt(KEY_MEDITATE, 0);
        data.setExtraInt(KEY_BOSS_KILLS, 0);
        data.setExtraBool(KEY_NOTIFIED_DONE, false);
    }
}
