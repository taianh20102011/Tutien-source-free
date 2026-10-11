package com.tutien.suphudedo;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * SuPhuDeDoManager — Hệ thống Sư Đồ (Master-Disciple), xây dựng hoàn toàn mới.
 * Trước đây package này rỗng, lệnh /suphu và /dedoan chưa từng tồn tại.
 *
 * Cơ chế:
 *  - Người chơi cảnh giới cao hơn tối thiểu 3 bậc mới có thể nhận đệ tử
 *  - Mỗi Sư Phụ nhận tối đa MAX_DE_TU (5) đệ tử
 *  - Mỗi lần đệ tử nhận Linh Khí (thiền định), Sư Phụ đang online nhận thêm
 *    SUPHU_LK_SHARE_PCT (10%) số LK đó — khích lệ dẫn dắt lẫn nhau
 *  - Dữ liệu lưu trong PlayerData.extraData (không cần sửa schema)
 *
 * Lệnh:
 *   /suphu <player>       — nhận player làm đệ tử
 *   /dedoan                — xem thông tin Sư Phụ / danh sách Đệ Tử
 *   /dedoan roi             — đệ tử tự rời khỏi Sư Phụ
 *   /dedoan kick <player>  — Sư Phụ đuổi 1 đệ tử
 */
public class SuPhuDeDoManager implements CommandExecutor {

    private final TuTienPlugin plugin;

    public static final int    MAX_DE_TU          = 5;
    public static final int    MIN_STAGE_GAP       = 3;
    public static final double SUPHU_LK_SHARE_PCT  = 0.10; // 10%

    private static final String KEY_SUPHU_UUID   = "suphu_uuid";
    private static final String KEY_DEDOAN_LIST  = "dedoan_list"; // comma-separated UUID

    public SuPhuDeDoManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════
    //  COMMAND DISPATCH
    // ══════════════════════════════════════════════
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        String name = cmd.getName().toLowerCase();

        if (name.equals("suphu")) {
            if (args.length < 1) {
                player.sendMessage(plugin.prefix() + "§cCú pháp: §e/suphu <player>");
                return true;
            }
            handleNhanDeTu(player, args[0]);
            return true;
        }

        if (name.equals("dedoan")) {
            if (args.length == 0) { showInfo(player); return true; }
            if (args[0].equalsIgnoreCase("roi")) { handleRoiSuPhu(player); return true; }
            if (args[0].equalsIgnoreCase("kick") && args.length >= 2) { handleKick(player, args[1]); return true; }
            showInfo(player);
            return true;
        }

        return false;
    }

    // ══════════════════════════════════════════════
    //  NHẬN ĐỆ TỬ
    // ══════════════════════════════════════════════
    private void handleNhanDeTu(Player suPhu, String targetName) {
        Player deTu = Bukkit.getPlayer(targetName);
        if (deTu == null || !deTu.isOnline()) {
            suPhu.sendMessage(plugin.prefix() + "§cKhông tìm thấy người chơi §e" + targetName + " §c(đang offline)!");
            return;
        }
        if (deTu.getUniqueId().equals(suPhu.getUniqueId())) {
            suPhu.sendMessage(plugin.prefix() + "§cBạn không thể nhận chính mình làm đệ tử!");
            return;
        }

        PlayerData spData = plugin.getDataManager().get(suPhu);
        PlayerData dtData = plugin.getDataManager().get(deTu);
        if (spData == null || dtData == null || !spData.hasClass() || !dtData.hasClass()) {
            suPhu.sendMessage(plugin.prefix() + "§cCả hai phải chọn class trước!");
            return;
        }

        // Đã có sư phụ chưa?
        String existingSuPhu = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (!existingSuPhu.isEmpty()) {
            suPhu.sendMessage(plugin.prefix() + "§c" + deTu.getName() + " §cđã có Sư Phụ rồi!");
            return;
        }

        // Cảnh giới phải cao hơn tối thiểu MIN_STAGE_GAP bậc
        int gap = spData.getStage().ordinal() - dtData.getStage().ordinal();
        if (gap < MIN_STAGE_GAP) {
            suPhu.sendMessage(plugin.prefix() + "§cCảnh giới của bạn phải cao hơn §e"
                    + deTu.getName() + " §ctối thiểu §e" + MIN_STAGE_GAP + " bậc§c! (Hiện tại chênh " + gap + " bậc)");
            return;
        }

        // Kiểm tra số lượng đệ tử hiện tại
        List<UUID> deTuList = getDeTuList(spData);
        if (deTuList.size() >= MAX_DE_TU) {
            suPhu.sendMessage(plugin.prefix() + "§cBạn đã đạt tối đa §e" + MAX_DE_TU + " §cđệ tử!");
            return;
        }

        // Thành công -> lưu 2 chiều
        dtData.setExtraStr(KEY_SUPHU_UUID, suPhu.getUniqueId().toString());
        deTuList.add(deTu.getUniqueId());
        saveDeTuList(spData, deTuList);

        // Thông báo + hiệu ứng
        suPhu.sendMessage(plugin.msg("auto_da39a3ee5e"));
        suPhu.sendMessage(plugin.msg("auto_7fcdcd636f"));
        suPhu.sendMessage(plugin.msg("auto_d4de3df054"));
        suPhu.sendMessage("  §7Đệ tử: §e" + deTu.getName());
        suPhu.sendMessage("  §7Số đệ tử: §e" + deTuList.size() + "§7/§e" + MAX_DE_TU);
        suPhu.sendMessage(plugin.msg("auto_045f213664"));
        suPhu.sendMessage(plugin.msg("auto_da39a3ee5e"));

        deTu.sendMessage(plugin.msg("auto_da39a3ee5e"));
        deTu.sendMessage(plugin.prefix() + "§6§l✦ Bạn đã trở thành đệ tử của §e" + suPhu.getName() + "§6!");
        deTu.sendMessage(plugin.prefix() + "§7Sư Phụ sẽ nhận §e10%§7 Linh Khí mỗi khi bạn thiền định.");
        deTu.sendMessage(plugin.msg("auto_da39a3ee5e"));

        for (Player p : new Player[]{suPhu, deTu}) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0),
                    20, 0.5, 0.5, 0.5, 0);
        }
    }

    // ══════════════════════════════════════════════
    //  RỜI SƯ PHỤ / ĐUỔI ĐỆ TỬ
    // ══════════════════════════════════════════════
    private void handleRoiSuPhu(Player deTu) {
        PlayerData dtData = plugin.getDataManager().get(deTu);
        String suPhuUuidStr = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (suPhuUuidStr.isEmpty()) {
            deTu.sendMessage(plugin.prefix() + "§cBạn chưa có Sư Phụ!");
            return;
        }
        dtData.setExtraStr(KEY_SUPHU_UUID, "");

        // Xóa khỏi danh sách của sư phụ (nếu sư phụ đang online, cập nhật cache trực tiếp)
        UUID suPhuUuid = UUID.fromString(suPhuUuidStr);
        Player suPhu = Bukkit.getPlayer(suPhuUuid);
        if (suPhu != null) {
            PlayerData spData = plugin.getDataManager().get(suPhu);
            if (spData != null) {
                List<UUID> list = getDeTuList(spData);
                list.remove(deTu.getUniqueId());
                saveDeTuList(spData, list);
                suPhu.sendMessage(plugin.prefix() + "§7Đệ tử §e" + deTu.getName() + " §7đã rời khỏi môn hạ của bạn.");
            }
        }
        deTu.sendMessage(plugin.prefix() + "§7Bạn đã rời khỏi Sư Phụ.");
    }

    private void handleKick(Player suPhu, String targetName) {
        Player deTu = Bukkit.getPlayer(targetName);
        PlayerData spData = plugin.getDataManager().get(suPhu);
        List<UUID> list = getDeTuList(spData);

        UUID targetUuid = null;
        if (deTu != null) targetUuid = deTu.getUniqueId();
        else {
            suPhu.sendMessage(plugin.prefix() + "§cĐệ tử phải đang online để đuổi!");
            return;
        }

        if (!list.contains(targetUuid)) {
            suPhu.sendMessage(plugin.prefix() + "§c" + targetName + " §ckhông phải đệ tử của bạn!");
            return;
        }

        list.remove(targetUuid);
        saveDeTuList(spData, list);

        PlayerData dtData = plugin.getDataManager().get(deTu);
        if (dtData != null) dtData.setExtraStr(KEY_SUPHU_UUID, "");

        suPhu.sendMessage(plugin.prefix() + "§7Đã đuổi §e" + deTu.getName() + " §7khỏi môn hạ.");
        deTu.sendMessage(plugin.prefix() + "§cBạn đã bị Sư Phụ §e" + suPhu.getName() + " §cđuổi khỏi môn hạ!");
    }

    // ══════════════════════════════════════════════
    //  XEM THÔNG TIN
    // ══════════════════════════════════════════════
    private void showInfo(Player player) {
        PlayerData data = plugin.getDataManager().get(player);

        String suPhuUuidStr = data.getExtraStr(KEY_SUPHU_UUID, "");
        List<UUID> deTuList = getDeTuList(data);

        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_81030847ba"));

        if (!suPhuUuidStr.isEmpty()) {
            OfflinePlayerName spName = resolveName(UUID.fromString(suPhuUuidStr));
            player.sendMessage("  §7Sư Phụ của bạn: " + (spName.online ? "§a● " : "§7○ ") + "§e" + spName.name);
        } else {
            player.sendMessage(plugin.msg("auto_cd6e66db25"));
        }

        if (!deTuList.isEmpty()) {
            player.sendMessage("  §7Đệ tử (§e" + deTuList.size() + "§7/§e" + MAX_DE_TU + "§7):");
            for (UUID uid : deTuList) {
                OfflinePlayerName dt = resolveName(uid);
                Player online = Bukkit.getPlayer(uid);
                String stageInfo = "";
                if (online != null) {
                    PlayerData d = plugin.getDataManager().get(online);
                    if (d != null) stageInfo = " §8- " + d.getStage().getFullDisplay(d.getRank());
                }
                player.sendMessage("    " + (dt.online ? "§a● " : "§7○ ") + "§f" + dt.name + stageInfo);
            }
        } else {
            player.sendMessage(plugin.msg("auto_63fe46fe5f"));
        }

        player.sendMessage("  §8/suphu <player> §7— nhận đệ tử (cần cao hơn " + MIN_STAGE_GAP + " bậc)");
        player.sendMessage(plugin.msg("auto_e2288cc371"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
    }

    // ══════════════════════════════════════════════
    //  HOOK: Chia sẻ LK khi đệ tử tu luyện
    //  (gọi từ MeditationSystem mỗi lần đệ tử nhận LK)
    // ══════════════════════════════════════════════
    public void onDeTuGainLK(Player deTu, PlayerData dtData, long gainedAmount) {
        String suPhuUuidStr = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (suPhuUuidStr.isEmpty()) return;

        Player suPhu = Bukkit.getPlayer(UUID.fromString(suPhuUuidStr));
        if (suPhu == null || !suPhu.isOnline()) return; // Sư Phụ offline -> không nhận được

        PlayerData spData = plugin.getDataManager().get(suPhu);
        if (spData == null) return;

        long share = (long) (gainedAmount * SUPHU_LK_SHARE_PCT);
        if (share <= 0) return;

        spData.addLinhKhi(share);
        if (spData.getLinhKhi() >= spData.getMaxLinhKhi()) {
            plugin.getBreakthroughManager().notifyBreakthroughReady(suPhu, spData);
        }
    }

    // ══════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════
    private List<UUID> getDeTuList(PlayerData data) {
        String raw = data.getExtraStr(KEY_DEDOAN_LIST, "");
        List<UUID> list = new ArrayList<>();
        if (raw.isEmpty()) return list;
        for (String s : raw.split(",")) {
            if (s.isBlank()) continue;
            try { list.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    private void saveDeTuList(PlayerData data, List<UUID> list) {
        StringBuilder sb = new StringBuilder();
        for (UUID u : list) {
            if (sb.length() > 0) sb.append(",");
            sb.append(u.toString());
        }
        data.setExtraStr(KEY_DEDOAN_LIST, sb.toString());
    }

    private OfflinePlayerName resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return new OfflinePlayerName(online.getName(), true);
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return new OfflinePlayerName(name != null ? name : "???", false);
    }

    private record OfflinePlayerName(String name, boolean online) {}
}
