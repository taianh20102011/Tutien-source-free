package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * TuTienPlaceholderExpansion — 30 placeholder cho PlaceholderAPI.
 * Prefix: %tutien_...%
 */
public class TuTienPlaceholderExpansion extends PlaceholderExpansion {

    private final TuTienPlugin plugin;

    public TuTienPlaceholderExpansion(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "tutien"; }
    @Override public @NotNull String getAuthor()     { return "TuTienDev"; }
    @Override public @NotNull String getVersion()    { return "4.0"; }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public String onRequest(OfflinePlayer op, @NotNull String params) {
        // Bang "nguoi choi da bi ban": KHONG phai stat rieng cua op (nguoi
        // dang xem hologram), ma la 1 danh sach chung toan server - nen
        // xu ly truoc, khong phu thuoc op co null hay khong.
        String lower = params.toLowerCase();
        if (lower.startsWith("ban_")) return resolveBanBoard(lower);

        if (op == null) return "";
        var player = op.getPlayer();
        PlayerData d = player != null ? plugin.getDataManager().get(player) : null;
        if (d != null) return resolve(d, params);

        // Nguoi choi OFFLINE (hoac chua tung duoc PlayerDataManager nap) - doc
        // thang tu file .yml de cac plugin bang xep hang (AJLeaderboards...)
        // van lay duoc so lieu cho ca nguoi khong dang online, khong chi
        // nguoi dang choi. Chi ho tro cac placeholder can cho leaderboard/
        // hien thi offline, khong load lai toan bo 30 placeholder.
        return resolveOffline(op.getUniqueId(), params);
    }

    private String resolve(PlayerData d, String params) {
        if (d == null) return "";

        return switch (params.toLowerCase()) {

            // ── CẢNH GIỚI ──────────────────────────────────────────────
            case "stage"        -> d.getStage().getColor() + d.getStage().getDisplayName();
            case "stage_plain"  -> d.getStage().getDisplayName();
            case "rank"         -> String.valueOf(d.getRank());
            case "rank_name"    -> rankName(d.getRank());
            case "full"         -> d.getStage().getDisplayName() + " Tầng " + d.getRank();
            case "full_color"   -> d.getStage().getFullDisplay(d.getRank());
            case "realm"        -> d.getStage().getRealm();
            // So nguyen duy nhat, tang dan dung theo tien do tu luyen thuc te (canh gioi
            // roi moi den tang) - dung placeholder NAY de cac plugin bang xep hang
            // (AJLeaderboards, Featherboard...) sap xep, vi cac placeholder khac o tren
            // la chuoi hien thi (co mau/chu), khong sap xep dung duoc.
            case "power_score"  -> String.valueOf(powerScore(d.getStage().ordinal(), d.getRank()));

            // ── LINH KHÍ ───────────────────────────────────────────────
            case "lk"           -> String.valueOf(d.getLinhKhi());
            case "lk_max"       -> String.valueOf(d.getMaxLinhKhi());
            case "lk_percent"   -> {
                long max = d.getMaxLinhKhi();
                yield max <= 0 ? "0" : String.valueOf((int)(d.getLinhKhi() * 100L / max));
            }
            case "lk_bar"       -> {
                long cur = d.getLinhKhi(), max = d.getMaxLinhKhi();
                if (max <= 0) yield "§8░░░░░░░░░░";
                int f = Math.min(10, (int)(cur * 10L / max));
                yield "§b" + "█".repeat(f) + "§8" + "░".repeat(10 - f);
            }
            case "lk_short"     -> shortNum(d.getLinhKhi());
            case "lk_short_max" -> shortNum(d.getMaxLinhKhi());
            case "lk_ready"     -> String.valueOf(d.getLinhKhi() >= d.getMaxLinhKhi());

            // ── LINH THẠCH ─────────────────────────────────────────────
            case "lt", "tien"        -> String.valueOf(d.getTien());
            case "lt_short", "tien_short" -> shortNum(d.getTien());

            // ── NHÂN VẬT ───────────────────────────────────────────────
            case "class"        -> d.hasClass()   ? d.getClassType().getDisplayName() : "Chưa chọn";
            case "dao"          -> d.hasDaoPath() ? d.getDaoPath().getDisplayName()   : "Chưa chọn";
            case "toc"          -> d.hasToc()     ? d.getToc().getDisplayName()       : "Chưa xác định";
            case "toc_color"    -> d.hasToc()     ? d.getToc().getColor() + d.getToc().getDisplayName() : "§7Chưa xác định";
            case "lingcan"      -> {
                if (!d.hasLinhCan()) yield "Chưa xác định";
                var sb = new StringBuilder();
                for (var lc : d.getLinhCan()) {
                    if (sb.length() > 0) sb.append("-");
                    sb.append(lc.getDisplayName());
                }
                yield sb.toString();
            }
            case "thienmeng"    -> {
                String tm = d.getExtraStr("thien_meng", "");
                if (tm.isEmpty()) yield "Chưa định";
                try {
                    var t = com.tutien.destiny.ThienMenhType.valueOf(tm);
                    yield t.color + t.icon + " " + t.name;
                } catch (Exception e) { yield "Chưa định"; }
            }

            // ── CHIẾN ĐẤU ──────────────────────────────────────────────
            case "kills"        -> String.valueOf(d.getKills());
            case "deaths"       -> String.valueOf(d.getDeaths());
            case "boss_kills"   -> String.valueOf(d.getBossKills());
            case "pvp_kills"    -> String.valueOf(d.getPvpKills());
            case "boss_toithuong_kills" -> String.valueOf(d.getBossToiThuongKills());
            case "bt_success"   -> String.valueOf(d.getBreakthroughSuccess());
            case "bt_fail"      -> String.valueOf(d.getBreakthroughFail());
            case "kd"           -> d.getDeaths() == 0 ? "∞"
                                   : String.format("%.2f", (double)d.getKills() / d.getDeaths());

            // ── KARMA ──────────────────────────────────────────────────
            case "karma_cong"   -> {
                var km = plugin.getKarmaManager();
                yield km != null ? shortNum(km.getCongDuc(d)) : "0";
            }
            case "karma_nghiep" -> {
                var km = plugin.getKarmaManager();
                yield km != null ? shortNum(km.getNghiepLuc(d)) : "0";
            }
            case "karma_grade"  -> {
                var km = plugin.getKarmaManager();
                yield km != null ? km.getKarmaDisplay(d) : "Trung Lap";
            }

            // ── TRẠNG THÁI ─────────────────────────────────────────────
            case "in_trial"     -> String.valueOf(d.isInBreakthroughTrial());
            case "pending_bt"   -> String.valueOf(d.isPendingBreakthrough());
            case "aura"         -> d.isAuraEnabled() ? "on" : "off";

            default -> null;
        };
    }

    /**
     * Bang "Người chơi đã bị ban" - đọc từ cache của BanBoardManager (đã
     * tự đọc + sắp xếp banned-players.json định kỳ). Cú pháp placeholder:
     * %tutien_ban_<số thứ tự 1..top-count>_<name|reason|duration>%
     */
    private String resolveBanBoard(String params) {
        String[] parts = params.split("_");
        if (parts.length != 3) return "";

        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return "";
        }
        if (index < 1) return "";

        var banBoard = plugin.getBanBoardManager();
        if (banBoard == null) return "";

        var list = banBoard.getTop();
        if (index > list.size()) return ""; // chua du du lieu cho vi tri nay - hologram nen an dong nay

        var entry = list.get(index - 1);
        return switch (parts[2]) {
            case "name"     -> entry.name();
            case "reason"   -> entry.reason();
            case "duration" -> entry.durationDisplay();
            default -> "";
        };
    }

    /** So nguyen duy nhat dai dien "suc manh tu luyen" - dung de sap xep leaderboard. */
    private long powerScore(int stageOrdinal, int rank) {
        return stageOrdinal * 10L + rank; // rank toi da la 6 trong toan bo CultivationStage -> an toan
    }

    /**
     * Doc truc tiep file .yml cua 1 nguoi choi OFFLINE (khong qua cache RAM,
     * vi cache chi giu nguoi dang online). Chi ho tro tap placeholder can
     * cho leaderboard/hien thi offline - khong load lai toan bo PlayerData.
     */
    private String resolveOffline(java.util.UUID uuid, String params) {
        java.io.File file = new java.io.File(plugin.getDataManager().getPlayersFolder(), uuid + ".yml");
        if (!file.exists()) return "";
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

        com.tutien.data.CultivationStage stage;
        try { stage = com.tutien.data.CultivationStage.valueOf(cfg.getString("stage", "LUYEN_KHI")); }
        catch (IllegalArgumentException e) { stage = com.tutien.data.CultivationStage.LUYEN_KHI; }
        int rank = cfg.getInt("rank", 1);

        return switch (params.toLowerCase()) {
            case "stage"        -> stage.getColor() + stage.getDisplayName();
            case "stage_plain"  -> stage.getDisplayName();
            case "rank"         -> String.valueOf(rank);
            case "rank_name"    -> rankName(rank);
            case "full"         -> stage.getDisplayName() + " Tầng " + rank;
            case "full_color"   -> stage.getFullDisplay(rank);
            case "realm"        -> stage.getRealm();
            case "power_score"  -> String.valueOf(powerScore(stage.ordinal(), rank));
            case "lk"           -> String.valueOf(cfg.getLong("linhkhi", 0));
            case "lk_short"     -> shortNum(cfg.getLong("linhkhi", 0));
            case "lt", "tien", "lt_short", "tien_short" -> {
                long bal = plugin.getTienManager().getBalance(org.bukkit.Bukkit.getOfflinePlayer(uuid));
                yield params.equalsIgnoreCase("lt") || params.equalsIgnoreCase("tien")
                        ? String.valueOf(bal) : shortNum(bal);
            }
            case "kills"        -> String.valueOf(cfg.getInt("kills", 0));
            case "deaths"       -> String.valueOf(cfg.getInt("deaths", 0));
            case "boss_kills"   -> String.valueOf(cfg.getInt("bossKills", 0));
            case "pvp_kills"    -> String.valueOf(cfg.getInt("pvpKills", 0));
            case "boss_toithuong_kills" -> String.valueOf(cfg.getInt("bossToiThuongKills", 0));
            case "kd"           -> {
                int k = cfg.getInt("kills", 0), de = cfg.getInt("deaths", 0);
                yield de == 0 ? "∞" : String.format("%.2f", (double) k / de);
            }
            default -> ""; // Cac placeholder khac chi ho tro khi nguoi choi online
        };
    }

    private String shortNum(long n) {
        if (n >= 1_000_000_000_000L) return String.format("%.1fT", n / 1_000_000_000_000.0);
        if (n >= 1_000_000_000L)     return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)         return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000L)             return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private String rankName(int r) {
        return switch (r) {
            case 1->"Sơ"; case 2->"Nhị"; case 3->"Tam";
            case 4->"Tứ"; case 5->"Ngũ"; case 6->"Lục";
            case 7->"Thất"; case 8->"Bát"; case 9->"Cửu";
            default->String.valueOf(r);
        };
    }
}
