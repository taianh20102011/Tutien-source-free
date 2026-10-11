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
                    player.sendMessage(plugin.prefix() + "§cHạng vị không hợp lệ! Dùng: MON_DO, NGOAI_MON, GIAM_SO, TRUONG_LAO");
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

            case "danh_sach", "list", "danhsach" -> {
                tm.showList(player); yield true;
            }

            default -> { showHelp(player, tm); yield true; }
        };
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
            "thongbao","kho","motadoi","mausac","chuyengiao","danhsach");
        if (args.length == 2 && args[0].equalsIgnoreCase("kho"))
            return Arrays.asList("xem","nap");
        if (args.length == 3 && args[0].equalsIgnoreCase("cap"))
            return Arrays.asList("MON_DO","NGOAI_MON","GIAM_SO","TRUONG_LAO");
        return Collections.emptyList();
    }
}
