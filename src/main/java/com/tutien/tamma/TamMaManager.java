package com.tutien.tamma;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.util.*;

/**
 * TamMaManager — Hệ thống Tâm Ma khi đột phá.
 *
 * Khi đột phá tầng cao (≥ Thánh Giới), có 30% cơ hội kích hoạt Tâm Ma.
 * Người chơi phải chọn đúng câu trả lời trong GUI 9-slot.
 * Sai → bị "Tẩu Hỏa Nhập Ma":
 *   - Tụt 1 rank thêm
 *   - Nhận hiệu ứng Confusion + Weakness 30 giây
 * Đúng → breakthrough dễ hơn 20% đợt này
 */
public class TamMaManager implements Listener {

    private final TuTienPlugin plugin;
    // player UUID → challenge state
    private final Map<UUID, TamMaChallenge> active = new HashMap<>();

    private static final List<QuestionEntry> QUESTIONS = List.of(
        new QuestionEntry("§e\"Đạo có thể gọi tên được không?\"",
            "§cKhông thể gọi tên", "§7Có thể gọi tên", "§8Không quan trọng",
            0, "Đạo khả đạo, phi thường Đạo"),
        new QuestionEntry("§e\"Điều gì quan trọng nhất trong tu luyện?\"",
            "§aTâm thái bình tĩnh", "§7Pháp bảo tốt", "§6Linh Thạch nhiều",
            0, "Tâm bình thì vạn sự bình"),
        new QuestionEntry("§e\"Khi đối mặt cái chết, người tu tiên làm gì?\"",
            "§cChạy trốn", "§aĐối mặt bình tĩnh", "§7Dùng bảo bối",
            1, "Bình tâm đối mặt"),
        new QuestionEntry("§e\"Nghịch cảnh là gì đối với người tu tiên?\"",
            "§cĐiều xấu cần tránh", "§aThử thách để trưởng thành", "§7Điều không thể tránh",
            1, "Nghịch cảnh là thuốc quý"),
        new QuestionEntry("§e\"Linh lực và thể xác quan hệ ra sao?\"",
            "§aHỗ trợ nhau", "§7Độc lập nhau", "§cLinh lực quan trọng hơn",
            0, "Âm dương cân bằng"),
        new QuestionEntry("§e\"Con đường dài nhất trong tu tiên là gì?\"",
            "§7Tìm pháp bảo", "§6Kiếm Linh Thạch", "§aTự hiểu bản thân",
            2, "Tự tri là con đường dài nhất"),
        new QuestionEntry("§e\"Khi đệ tử phạm sai lầm, sư phụ nên...\"",
            "§aChỉ bảo và tha thứ", "§cTrừng phạt nặng", "§7Đuổi khỏi tông môn",
            0, "Nhân từ là nền tảng của đạo"),
        new QuestionEntry("§e\"Lực lượng nào mạnh nhất trong vũ trụ?\"",
            "§6Linh lực", "§7Thể lực", "§aÝ chí và tâm thức",
            2, "Tâm thức vượt qua vạn vật")
    );

    public TamMaManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Trả về true nếu cần kích hoạt Tâm Ma (và đã mở GUI) */
    public boolean tryTrigger(Player player, PlayerData data, Runnable onPass, Runnable onFail) {
        if (data.getStage().ordinal() < 11) return false; // Chỉ từ Thánh Giới
        if (Math.random() > 0.30) return false;           // 30% cơ hội

        QuestionEntry q = QUESTIONS.get((int)(Math.random() * QUESTIONS.size()));
        TamMaChallenge ch = new TamMaChallenge(player.getUniqueId(), q, onPass, onFail);
        active.put(player.getUniqueId(), ch);

        openGUI(player, q);
        return true;
    }

    private void openGUI(Player player, QuestionEntry q) {
        Inventory inv = Bukkit.createInventory(null, 27,
            LegacyComponentSerializer.legacySection().deserialize("§4☯ Tâm Ma Thử Thách"));

        // Tiêu đề ở slot 4
        inv.setItem(4, makeItem(Material.SOUL_LANTERN, "§e§l" + q.question,
            List.of("§7Chọn câu trả lời đúng!", "§cSai → Tẩu Hỏa Nhập Ma!")));

        // 3 đáp án ở slot 11, 13, 15
        inv.setItem(11, makeItem(Material.LIME_CONCRETE,   q.answers()[0], List.of("§7Đáp án A")));
        inv.setItem(13, makeItem(Material.YELLOW_CONCRETE, q.answers()[1], List.of("§7Đáp án B")));
        inv.setItem(15, makeItem(Material.RED_CONCRETE,    q.answers()[2], List.of("§7Đáp án C")));

        // Viền
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) if (inv.getItem(i) == null) inv.setItem(i, border);

        player.openInventory(inv);
        player.sendTitle("§4☯ TÂM MA GIÁNG!", "§cHãy vượt qua thử thách!", 10, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TamMaChallenge ch = active.get(player.getUniqueId());
        if (ch == null) return;
        String title = LegacyComponentSerializer.legacySection()
            .serialize(event.getView().title());
        if (!title.contains("Tâm Ma")) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        if (slot != 11 && slot != 13 && slot != 15) return;

        int chosen = slot == 11 ? 0 : slot == 13 ? 1 : 2;
        active.remove(player.getUniqueId());
        player.closeInventory();

        if (chosen == ch.question.correctIdx) {
            player.sendTitle("§a§l✓ ĐÚNG!", "§eTâm Ma bị đẩy lùi! Breakthrough dễ hơn 20%!", 10, 60, 20);
            player.sendMessage(plugin.prefix() + "§a§l✓ Vượt Tâm Ma! §7" + ch.question.hint);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            plugin.getDataManager().get(player).addBuff("tamma_pass", 60_000L);
            plugin.getServer().getScheduler().runTaskLater(plugin, ch.onPass, 5L);
        } else {
            player.sendTitle("§c§l✗ SAI!", "§4Tẩu Hỏa Nhập Ma!", 10, 80, 20);
            player.sendMessage(plugin.prefix() + "§c✗ Sai! Đáp án đúng: §e" + ch.question.hint);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NAUSEA, 600, 1));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.WEAKNESS, 600, 2));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.5f);
            plugin.getServer().getScheduler().runTaskLater(plugin, ch.onFail, 5L);
        }
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        List<net.kyori.adventure.text.Component> loreCmp = new ArrayList<>();
        for (String l : lore) loreCmp.add(LegacyComponentSerializer.legacySection().deserialize(l));
        meta.lore(loreCmp);
        item.setItemMeta(meta);
        return item;
    }

    record QuestionEntry(String question, String a, String b, String c, int correctIdx, String hint) {
        // Lấy đáp án dưới dạng array qua method
        String[] answers() { return new String[]{a, b, c}; }
    }

    record TamMaChallenge(UUID uuid, QuestionEntry question, Runnable onPass, Runnable onFail) {}
}
