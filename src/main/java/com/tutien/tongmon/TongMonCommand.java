package com.tutien.tongmon;

import com.tutien.TuTienPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class TongMonCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public TongMonCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_3da9667838")); return true;
        }
        TongMonManager tm = plugin.getTongMonManager();

        if (cmd.getName().equalsIgnoreCase("mochieumo")) {
            tm.toggleRecruitment(player);
            return true;
        }
        if (args.length == 0) { showHelp(player, tm); return true; }

        return switch (args[0].toLowerCase()) {

            case "tao", "create" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon tao <tên> <TAG>"); yield true;
                }
                String name = args[1];
                String tag  = args[2];
                tm.create(player, name, tag);
                yield true;
            }

            case "roi", "leave" -> {
                tm.leave(player); yield true;
            }

            case "giaithe", "disband" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("xacnhan")) {
                    player.sendMessage(plugin.prefix() + "§c/tongmon giaithe xacnhan §cđể giải thể (không hoàn tiền!)");
                    yield true;
                }
                tm.disband(player); yield true;
            }

            case "moi", "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon moi <player>"); yield true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.prefix() + "§cKhông tìm thấy: §e" + args[1]); yield true;
                }
                tm.invite(player, target); yield true;
            }

            case "chap", "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon chap <id>"); yield true;
                }
                tm.join(player, args[1]); yield true;
            }

            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon kick <player>"); yield true;
                }
                tm.kick(player, args[1]); yield true;
            }

            case "cap", "setrank" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon cap <player> <hạng_vị>");
                    player.sendMessage(plugin.msg("auto_78f0c4d4a6"));
                    yield true;
                }
                TongMon tongMon = tm.getTongMon(player.getUniqueId());
                if (tongMon == null || !tongMon.canManage(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cKhông đủ quyền!"); yield true;
                }
                try {
                    TongMon.Rank newRank = TongMon.Rank.valueOf(args[2].toUpperCase());
                    if (newRank == TongMon.Rank.MON_CHU) {
                        player.sendMessage(plugin.prefix() + "§cKhông thể cấp Môn Chủ! Dùng /tongmon chuyengiao."); yield true;
                    }
                    if (newRank == TongMon.Rank.PHO_TONG_CHU && !tongMon.isOwner(player.getUniqueId())) {
                        player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ được bổ nhiệm Phó Tông Chủ."); yield true;
                    }
                    if (newRank == TongMon.Rank.HOP_PHAP_TRUONG_LAO
                            && !tongMon.isOwner(player.getUniqueId())
                            && tongMon.getRank(player.getUniqueId()) != TongMon.Rank.PHO_TONG_CHU) {
                        player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ được bổ nhiệm Hộ Pháp Trưởng Lão."); yield true;
                    }
                    // Tim uuid theo ten
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !tongMon.isMember(target.getUniqueId())) {
                        player.sendMessage(plugin.prefix() + "§cKhông tìm thấy thành viên: §e" + args[1]); yield true;
                    }
                    tongMon.setRank(target.getUniqueId(), newRank);
                    tm.save();
                    tm.notifyMembers(tongMon, plugin.prefix() + "§e" + target.getName()
                        + " §ađã lên hạng " + newRank.getColored() + "§a!");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.prefix() + "§cHạng vị: MON_DO, NGOAI_MON, GIAM_SO, TRUONG_LAO, HOP_PHAP_TRUONG_LAO, PHO_TONG_CHU (chỉ Môn Chủ)");
                }
                yield true;
            }

            case "chuyengiao", "transfer" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon chuyengiao <player>"); yield true;
                }
                TongMon tongMon = tm.getTongMon(player.getUniqueId());
                if (tongMon == null || !tongMon.isOwner(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ mới chuyển giao được!"); yield true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !tongMon.isMember(target.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cCần người chơi online và là thành viên!"); yield true;
                }
                tongMon.setOwner(target.getUniqueId());
                tongMon.setRank(target.getUniqueId(), TongMon.Rank.MON_CHU);
                tongMon.setRank(player.getUniqueId(), TongMon.Rank.TRUONG_LAO);
                tm.save();
                tm.notifyMembers(tongMon, plugin.prefix() + "§6§l✦ Môn Chủ đã chuyển giao cho §e" + target.getName() + "§6§l!");
                yield true;
            }

            case "thongbao", "announce" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon thongbao <nội dung>"); yield true;
                }
                TongMon tongMon = tm.getTongMon(player.getUniqueId());
                if (tongMon == null || !tongMon.canManage(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cKhông đủ quyền!"); yield true;
                }
                String announcement = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                tongMon.setAnnouncement(announcement);
                tm.save();
                tm.notifyMembers(tongMon, plugin.prefix() + "§6[" + tongMon.getTag() + "] §eThông Báo: §f" + announcement);
                yield true;
            }

            case "kho", "bank" -> {
                if (args.length < 2) { showBankHelp(player, tm); yield true; }
                if (args[1].equalsIgnoreCase("nap") && args.length >= 3) {
                    try {
                        long amount = Long.parseLong(args[2]);
                        tm.depositKho(player, amount);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.prefix() + "§cSố không hợp lệ!");
                    }
                } else if (args[1].equalsIgnoreCase("xem")) {
                    TongMon tongMon = tm.getTongMon(player.getUniqueId());
                    if (tongMon != null) player.sendMessage(plugin.prefix()
                        + "§6Kho Tông Môn: §e" + fmt(tongMon.getTienKho()) + " §6" + plugin.getTienManager().tenTien());
                }
                yield true;
            }

            case "motadoi", "setdesc" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§c/tongmon motadoi <nội dung>"); yield true;
                }
                TongMon tongMon = tm.getTongMon(player.getUniqueId());
                if (tongMon == null || !tongMon.isOwner(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ mới sửa mô tả!"); yield true;
                }
                String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                tongMon.setDescription(desc);
                tm.save();
                player.sendMessage(plugin.prefix() + "§aĐã cập nhật mô tả Tông Môn!");
                yield true;
            }

            case "mausac", "color" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§cDùng: /tongmon mausac <&a-&f>"); yield true;
                }
                TongMon tongMon = tm.getTongMon(player.getUniqueId());
                if (tongMon == null || !tongMon.isOwner(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ mới đổi màu!"); yield true;
                }
                String color = args[1].replace("&","§");
                tongMon.setColor(color);
                tm.save();
                player.sendMessage(plugin.prefix() + "§aĐã đổi màu Tông Môn thành: " + color + tongMon.getName());
                yield true;
            }

            case "info" -> {
                tm.showInfo(player); yield true;
            }

            case "ungtuyen", "apply" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§cDùng: /tongmon ungTuyen <id>"); yield true; }
                tm.applyForRecruitment(player, args[1]); yield true;
            }
            case "don", "applications" -> { tm.showApplications(player); yield true; }
            case "duyet", "approve" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§cDùng: /tongmon duyet <player>"); yield true; }
                tm.approveApplication(player, args[1]); yield true;
            }
            case "tuchoi", "reject" -> {
                if (args.length < 2) { player.sendMessage(plugin.prefix() + "§cDùng: /tongmon tuchoi <player>"); yield true; }
                tm.rejectApplication(player, args[1]); yield true;
            }
            case "lienminh", "alliance" -> { handleAlliance(player, args); yield true; }
            case "dat", "lanhtho", "land" -> { handleLand(player, args); yield true; }
            case "danh_sach", "list", "danhsach" -> {
                tm.showList(player); yield true;
            }

            default -> { showHelp(player, tm); yield true; }
        };
    }

    private void handleAlliance(Player player, String[] args) {
        var manager = plugin.getTongMonAllianceManager();
        if (manager == null) { player.sendMessage(plugin.prefix() + "§cHệ thống liên minh chưa sẵn sàng."); return; }
        if (args.length == 1 || args[1].equalsIgnoreCase("gui")) { manager.openGui(player); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "moi", "invite" -> {
                if (args.length < 3) { player.sendMessage("§e/tongmon lienminh moi <id|tag|ten>"); return; }
                manager.invite(player, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            case "chap", "accept" -> { if (args.length < 3) player.sendMessage("§e/tongmon lienminh chap <id> "); else manager.accept(player, args[2]); }
            case "tuchoi", "decline" -> { if (args.length < 3) player.sendMessage("§e/tongmon lienminh tuchoi <id>"); else manager.decline(player, args[2]); }
            case "pvp", "dat", "lanhtho" -> {
                if (args.length < 4) { player.sendMessage("§e/tongmon lienminh <pvp|dat> <id|tag|ten> <on|off>"); return; }
                String id = args[2];
                String toggle = args[3].toLowerCase(Locale.ROOT);
                if (!toggle.equals("on") && !toggle.equals("off") && !toggle.equals("bat") && !toggle.equals("tat")) { player.sendMessage("§cDùng on/off."); return; }
                boolean enabled = toggle.equals("on") || toggle.equals("bat");
                String type = args[1].equalsIgnoreCase("pvp") ? "pvp" : "dat";
                manager.setConsent(player, id, type, enabled);
            }
            case "list", "danhsach" -> manager.openGui(player);
            default -> player.sendMessage("§6Liên Minh: /tongmon lienminh <gui|moi|chap|tuchoi|pvp|dat> ...");
        }
    }

    private void handleLand(Player player, String[] args) {
        var land = plugin.getTongMonLandManager();
        if (land == null) { player.sendMessage(plugin.prefix() + "§cHệ thống địa bàn chưa sẵn sàng."); return; }
        if (args.length < 2) { land.showInfo(player); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "pos1" -> land.setPosition(player, 1);
            case "pos2" -> land.setPosition(player, 2);
            case "thue", "rent" -> land.rent(player);
            case "giahan", "renew" -> land.renew(player);
            case "xem", "info" -> land.showInfo(player);
            default -> player.sendMessage("§6Địa Bàn: /tongmon dat <pos1|pos2|thue|giahan|xem>");
        }
    }

    private void showHelp(Player player, TongMonManager tm) {
        boolean inTm = tm.hasTongMon(player.getUniqueId());
        player.sendMessage(plugin.msg("auto_63f2921e52"));
        if (!inTm) {
            player.sendMessage(plugin.msg("auto_fa4f5f9111"));
            player.sendMessage(plugin.msg("auto_bd08e95520"));
        } else {
            TongMon tongMon = tm.getTongMon(player.getUniqueId());
            player.sendMessage("  §7Tông Môn: " + tongMon.getColoredName() + " §e[Lv." + tongMon.getLevel() + "]");
            player.sendMessage("  §7Hạng vị: " + tongMon.getRank(player.getUniqueId()).getColored());
            player.sendMessage(plugin.msg("auto_dbbee6a20e"));
            player.sendMessage(plugin.msg("auto_e7588f2bf2"));
            player.sendMessage(plugin.msg("auto_c52c8d778f"));
            player.sendMessage(plugin.msg("auto_0e3aea85aa"));
            player.sendMessage(plugin.msg("auto_7a93e5d4a8"));
            if (tongMon.isOwner(player.getUniqueId())) {
                player.sendMessage(plugin.msg("auto_96a01d5964"));
                player.sendMessage(plugin.msg("auto_88951249f9"));
                player.sendMessage(plugin.msg("auto_8c687a6be2"));
                player.sendMessage(plugin.msg("auto_a0af14b6ea"));
                player.sendMessage(plugin.msg("auto_fa911af3e5"));
                player.sendMessage(plugin.msg("auto_e0e38f41ce"));
            }
            player.sendMessage(plugin.msg("auto_bd44730e9e"));
        }
        player.sendMessage("§e/mochieumo §7— Môn Chủ mở/đóng chiêu mộ công khai");
        player.sendMessage("§e/tongmon don §7— quản lý đơn chiêu mộ (quản lý)");
        player.sendMessage("§e/tongmon lienminh §7— GUI liên minh hai bên đồng thuận");
        player.sendMessage("§e/tongmon dat §7— thuê địa bàn Tông Môn");
        player.sendMessage("§e/congphap §7— công pháp tùy chỉnh server");
        player.sendMessage(plugin.msg("auto_0aea9ded58"));
        player.sendMessage(plugin.msg("auto_4697738232"));
    }

    private void showBankHelp(Player player, TongMonManager tm) {
        TongMon tongMon = tm.getTongMon(player.getUniqueId());
        if (tongMon == null) { player.sendMessage(plugin.prefix() + "§cChưa có Tông Môn!"); return; }
        player.sendMessage(plugin.msg("auto_1491f62c27"));
        player.sendMessage("  §7Số dư: §e" + fmt(tongMon.getTienKho()) + " §7" + plugin.getTienManager().tenTien());
        player.sendMessage("  §7Cấp Tông Môn: §e" + tongMon.getLevel());
        player.sendMessage(plugin.msg("auto_d0728fb087"));
        player.sendMessage(plugin.msg("auto_7d9e6e4cb4"));
    }

    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n/1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String a, String[] args) {
        if (args.length == 1) return Arrays.asList(
            "tao","info","moi","chap","kick","cap","roi","giaithe",
            "thongbao","kho","motadoi","mausac","chuyengiao","danhsach",
            "ungtuyen","don","duyet","tuchoi","lienminh","dat");
        if (args.length == 2 && args[0].equalsIgnoreCase("kho")) return Arrays.asList("xem","nap");
        if (args.length == 2 && args[0].equalsIgnoreCase("dat")) return Arrays.asList("pos1","pos2","thue","giahan","xem");
        if (args.length == 2 && args[0].equalsIgnoreCase("lienminh")) return Arrays.asList("gui","moi","chap","tuchoi","pvp","dat","list");
        if (args.length == 3 && args[0].equalsIgnoreCase("lienminh") && Arrays.asList("pvp","dat").contains(args[1].toLowerCase(Locale.ROOT))) return Arrays.asList("on","off");
        if (args.length == 3 && args[0].equalsIgnoreCase("cap"))
            return Arrays.asList("MON_DO","NGOAI_MON","GIAM_SO","TRUONG_LAO");
        return Collections.emptyList();
    }
}
