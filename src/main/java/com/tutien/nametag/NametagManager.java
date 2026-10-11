package com.tutien.nametag;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * NametagManager v4 - Sử dụng Scoreboard Team riêng để hiển thị prefix.
 *
 * - Tạo 1 Scoreboard riêng (KHÔNG dùng main scoreboard của server)
 * - Mỗi cảnh giới = 1 Team với prefix tương ứng
 * - Dùng DISPLAY_NAME (tab list) kèm theo
 * - Action bar cho thông tin nhanh
 *
 * Giải quyết conflict với LuckPerms:
 * Plugin tạo scoreboard riêng, LuckPerms không can thiệp scoreboard này.
 * Người chơi được add vào team của plugin (ghi đè prefix LuckPerms).
 */
public class NametagManager {

    private final TuTienPlugin plugin;
    private Scoreboard sb;
    private final Map<String, Team> teams = new HashMap<>(); // teamId -> Team

    public NametagManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        initScoreboard();
        startLoops();
    }

    // ========================
    //  INIT SCOREBOARD
    // ========================

    private void initScoreboard() {
        sb = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    private Team getOrCreateTeam(String id, String prefix, String suffix) {
        if (teams.containsKey(id)) {
            Team t = teams.get(id);
            t.prefix(LegacyComponentSerializer.legacySection().deserialize(prefix));
            t.suffix(LegacyComponentSerializer.legacySection().deserialize(suffix));
            return t;
        }
        Team t = sb.registerNewTeam(id);
        t.prefix(LegacyComponentSerializer.legacySection().deserialize(prefix));
        t.suffix(LegacyComponentSerializer.legacySection().deserialize(suffix));
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teams.put(id, t);
        return t;
    }

    // ========================
    //  UPDATE
    // ========================

    public void updateNametag(Player player, PlayerData data) {
        updateTag(player, data);
        updateTabName(player, data);
    }

    private void updateTag(Player player, PlayerData data) {
        if (!data.hasClass()) return;
        String rc     = data.getStage().getRealmColor();
        String sc     = data.getStage().getColor();
        String abbrev = abbrev(data.getStage().getDisplayName());
        String icon   = data.getClassType().getColor() + data.getClassType().getIcon();

        // Prefix trên đầu: [DeTu] TênNguoiChoi
        String prefix  = rc + "[" + sc + abbrev + rc + "] ";
        String suffix  = " " + icon;

        // Team ID theo cảnh giới
        String teamId = "tt_" + data.getStage().ordinal() + "_" + data.getClassType().ordinal();
        Team team = getOrCreateTeam(teamId, prefix, suffix);

        // Remove player khỏi team cũ
        try {
            Team old = sb.getEntryTeam(player.getName());
            if (old != null && !old.getName().equals(team.getName())) old.removeEntry(player.getName());
        } catch (Exception ignored) {}

        // Add vào team mới
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());

        // Đặt scoreboard cho player
        try { player.setScoreboard(sb); } catch (Exception ignored) {}
    }

    private void updateTabName(Player player, PlayerData data) {
        if (!data.hasClass() || data.getStage() == null) return;
        String rc     = data.getStage().getRealmColor();
        String sc     = data.getStage().getColor();
        String abbrev = abbrev(data.getStage().getDisplayName());
        String icon   = data.getClassType().getColor() + data.getClassType().getIcon();
        String listName = rc + "[" + sc + abbrev + rc + "] §f" + player.getName() + " " + icon;
        try {
            player.playerListName(LegacyComponentSerializer.legacySection().deserialize(listName));
        } catch (Exception e) {
            try { player.setPlayerListName(listName); } catch (Exception ignored) {}
        }
    }

    // ========================
    //  LOOPS
    // ========================

    private void startLoops() {
        // Action bar mỗi 2 giây
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null) continue;
                    sendActionBar(p, data);
                    // Re-apply tag mỗi 10 giây
                    if (System.currentTimeMillis() % 10000 < 2200) updateTag(p, data);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);

        // Tab list mỗi 10 giây
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data != null && data.hasClass()) updateTabName(p, data);
                }
            }
        }.runTaskTimer(plugin, 60L, 200L);
    }

    private void sendActionBar(Player player, PlayerData data) {
        if (!data.hasClass()) {
            player.sendActionBar(Component.text("§7/tutien chon §8— chọn class để bắt đầu!"));
            return;
        }
        String stage = data.getStage().getFullDisplay(data.getRank());
        String classIcon = data.getClassType().getColor() + data.getClassType().getIcon();
        String daoIcon = data.hasDaoPath() ? (data.getDaoPath().name().equals("MA_DAO") ? " §4☠" : " §b☯") : "";
        // Thêm thiền định indicator
        String medit = data.isMeditating() ? " §9⛩" : "";
        String bar = stage + " §8| " + classIcon + daoIcon + medit
            + (data.isPendingBreakthrough() ? " §e⚡/dotpha" : "");
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(bar));
    }

    // ========================
    //  HELPERS
    // ========================

    public void removePlayer(Player player) {
        try {
            Team t = sb.getEntryTeam(player.getName());
            if (t != null) t.removeEntry(player.getName());
            player.playerListName(null);
        } catch (Exception ignored) {}
    }

    private String abbrev(String name) {
        return switch (name) {
            case "Luyện Khí"  -> "LuyKhi";  case "Trúc Cơ"    -> "TrucCo";
            case "Kết Đan"    -> "KetDan";  case "Nguyên Anh" -> "NgAnh";
            case "Hóa Thần"   -> "HoaThan";
            case "Đế Tu"      -> "DeTu";    case "Đế Hậu"     -> "DeHau";
            case "Đế Vương"   -> "DeVuong"; case "Đế Tôn"     -> "DeTon";
            case "Đế Chủ"     -> "DeChu";
            case "Bán Thánh"  -> "BanTh";   case "Chuẩn Thánh"-> "ChuanTh";
            case "Đại Thánh"  -> "DaiTh";   case "Thánh Tôn"  -> "ThanhTon";
            case "Thánh Đế"   -> "ThanhDe";
            case "Chân Tiên"  -> "ChanTien";case "Kim Tiên"   -> "KimTien";
            case "Thái Ất"    -> "ThaiAt";  case "Đại La"     -> "DaiLa";
            case "Tiên Đế"    -> "TienDe";
            case "Thiên Tôn"  -> "ThienTon";case "Thiên Đạo"  -> "ThienDao";
            case "Thiên Đế"   -> "ThienDe"; case "Hỗn Nguyên" -> "HonNg";
            case "Vô Thượng Chí Tôn" -> "VTCTon";
            default -> name.length() > 7 ? name.substring(0,7) : name;
        };
    }
}
