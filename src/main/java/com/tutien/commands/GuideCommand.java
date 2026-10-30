package com.tutien.commands;

import com.tutien.TuTienPlugin;
import com.tutien.gui.TuTienGuideGUI;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /guide - Cẩm Nang Toàn Tập & Hướng Dẫn Tu Tiên Plugin
 * Hỗ trợ giao diện rương GUI trực quan và tra cứu lệnh văn bản tiện lợi.
 */
public class GuideCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public GuideCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        TuTienGuideGUI gui = plugin.getGuideGUI();

        // Nếu là Player và không có tham số: Mở thẳng giao diện GUI
        if (sender instanceof Player player) {
            if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
                if (gui != null) {
                    gui.openMain(player);
                } else {
                    showTextMainMenu(player);
                }
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "batdau", "start", "tuyen" -> {
                    if (gui != null) gui.openTuLuyen(player);
                    else showTextTuLuyen(player);
                    return true;
                }
                case "kiemdao", "skill", "kiem" -> {
                    if (gui != null) gui.openKiemDao(player);
                    else showTextKiemDao(player);
                    return true;
                }
                case "luyenche", "phabao", "dan", "phap_bao" -> {
                    if (gui != null) gui.openLuyenChe(player);
                    else showTextLuyenChe(player);
                    return true;
                }
                case "bachnghe", "coduyen", "nghe" -> {
                    if (gui != null) gui.openBachNghe(player);
                    else showTextBachNghe(player);
                    return true;
                }
                case "tongmon", "sudo", "party" -> {
                    if (gui != null) gui.openTongMon(player);
                    else showTextTongMon(player);
                    return true;
                }
                case "lenh", "commands", "player" -> {
                    if (gui != null) gui.openPlayerCommands(player, 0);
                    else showTextPlayerCommands(player);
                    return true;
                }
                case "admin", "quantri" -> {
                    if (gui != null) gui.openAdminCommands(player, 0);
                    else showTextAdminCommands(player);
                    return true;
                }
                case "text", "chat" -> {
                    showTextMainMenu(player);
                    return true;
                }
            }
        }

        // Dành cho Console hoặc fallback Text
        if (args.length == 0) {
            showTextMainMenu(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "batdau", "start", "tuyen" -> showTextTuLuyen(sender);
            case "kiemdao", "skill", "kiem"  -> showTextKiemDao(sender);
            case "luyenche", "phabao", "dan" -> showTextLuyenChe(sender);
            case "bachnghe", "coduyen"       -> showTextBachNghe(sender);
            case "tongmon", "sudo"           -> showTextTongMon(sender);
            case "lenh", "commands", "player"-> showTextPlayerCommands(sender);
            case "admin", "quantri"          -> showTextAdminCommands(sender);
            default                          -> showTextMainMenu(sender);
        }

        return true;
    }

    // ============================================================
    // VĂN BẢN HƯỚNG DẪN TRONG CHAT
    // ============================================================

    private void showTextMainMenu(CommandSender sender) {
        sender.sendMessage("§6§l╔══════════════════════════════════════════════╗");
        sender.sendMessage("§6§l║      §e§l✦ CẨM NANG TU TIÊN GIỚI TOÀN TẬP ✦      §6§l║");
        sender.sendMessage("§6§l╠══════════════════════════════════════════════╣");
        sender.sendMessage("§7  Gõ lệnh để tra cứu từng phân mục chi tiết:");
        sender.sendMessage("  §a/guide batdau     §8- §fNhập môn, Linh khí, Cảnh giới, Đột phá");
        sender.sendMessage("  §b/guide kiemdao    §8- §fNgự kiếm phi hành, Kỹ năng, Công pháp");
        sender.sendMessage("  §d/guide luyenche   §8- §fLuyện đan, Rèn đúc, Cường hóa Pháp bảo");
        sender.sendMessage("  §6/guide bachnghe   §8- §f6 Nghề phụ, Kim thủ chỉ, Tiên thảo");
        sender.sendMessage("  §e/guide tongmon    §8- §fTông môn, Sư đồ, Đệ tử NPC, Tổ đội");
        sender.sendMessage("  §f/guide lenh       §8- §fBảng tra cứu toàn bộ lệnh người chơi");
        sender.sendMessage("  §c/guide admin      §8- §fBảng tra cứu toàn bộ lệnh quản trị viên");
        sender.sendMessage("§6§l╚══════════════════════════════════════════════╝");
    }

    private void showTextTuLuyen(CommandSender sender) {
        sender.sendMessage("§a§l[1. NHẬP MÔN & TU LUYỆN]");
        sender.sendMessage("§7• §e/tutien chon [class] §8- §fChọn môn phái (Kiếm Tu, Thể Tu, Pháp Tu, Đan Tu, Phù Tu).");
        sender.sendMessage("§7• §e/tutien dao [dao]     §8- §fChọn đạo tâm (Chính Đạo, Ma Đạo, Kiếm Đạo, Tiêu Dao...).");
        sender.sendMessage("§7• §e/linhkhi             §8- §fXem thanh tiến trình linh khí tích lũy.");
        sender.sendMessage("§7• §e/thiendinh           §8- §fNgồi thiền tĩnh tọa tăng tốc hấp thu linh khí tự động.");
        sender.sendMessage("§7• §e/vuottamma           §8- §fChiến đấu vượt tâm ma (bắt buộc từ Bán Thánh trở lên).");
        sender.sendMessage("§7• §e/dotpha              §8- §fKích hoạt lôi kiếp để đột phá cảnh giới khi đạt 100% LK.");
        sender.sendMessage("§7• §e/boqua               §8- §fBỏ qua thông báo sẵn sàng đột phá.");
        sender.sendMessage("§7• §e/aura [on|off]       §8- §fBật/tắt vòng hào quang tu vi phát sáng.");
        sender.sendMessage("§7• §e/uyap [toggle|on|off] §8- §fPhóng thích Uy Áp bộc phát (10m) / Quản lý áp chế cảnh giới.");
    }

    private void showTextKiemDao(CommandSender sender) {
        sender.sendMessage("§b§l[2. KIẾM ĐẠO & KỸ NĂNG]");
        sender.sendMessage("§7• §e/ngukiem             §8- §fNgự Kiếm Phi Hành (Đạp phi kiếm bay lượn, mũi kiếm xoay 3D chuẩn xác).");
        sender.sendMessage("§7• §e/trieukiem [mode]    §8- §fTriệu hồi kiếm bay hộ thể sau lưng (AUTO/SEMI/OFF).");
        sender.sendMessage("§7• §e/trieuthu            §8- §fTriệu hồi hoặc thu hồi Linh Thú đồng hành.");
        sender.sendMessage("§7• §e/hocbidong <hệ>      §8- §fHọc bí kíp bị động từ sách trên tay (atk, hp, def, speed, luck, regen).");
        sender.sendMessage("§7• §e/bidong              §8- §fXem danh sách bí kíp bị động đã lĩnh ngộ.");
        sender.sendMessage("§7• §e/doiky <a|b|c|d>     §8- §fHoán đổi nhánh kỹ năng trong slot Linh Khí Kiếm.");
        sender.sendMessage("§7• §e/kinang              §8- §fMở bảng GUI bật/tắt các module kỹ năng.");
        sender.sendMessage("§7• §e/congphap            §8- §fHệ thống công pháp tu tiên tùy chỉnh (list, hoc, trangbi).");
    }

    private void showTextLuyenChe(CommandSender sender) {
        sender.sendMessage("§d§l[3. LUYỆN CHẾ & PHÁP BẢO]");
        sender.sendMessage("§7• §e/luyen danh_sach     §8- §fXem danh sách 32 công thức đan dược tu tiên.");
        sender.sendMessage("§7• §e/luyen <tên_đan>     §8- §fBắt đầu luyện chế đan dược theo công thức.");
        sender.sendMessage("§7• §e/renduc              §8- §fRèn đúc nâng phẩm giai Pháp Bảo (Hạ ➔ Trung ➔ Thượng ➔ Cực ➔ Đế ➔ Tiên).");
        sender.sendMessage("§7• §e/cuonghoa            §8- §fCường hóa Pháp Bảo từ +1 đến +10 gia tăng % chỉ số.");
        sender.sendMessage("§7• §e/khamnam             §8- §fKhảm ngọc nguyên tố (Hỏa, Băng, Lôi) vào lỗ khảm.");
        sender.sendMessage("§7• §e/dunghop             §8- §fDung hợp 2 pháp bảo cùng loại để ghép dòng ẩn & thăng phẩm.");
        sender.sendMessage("§7• §e/linhthuc            §8- §fGán kỹ năng tự động kích hoạt (hộ thể, phản kích) lên Pháp Bảo.");
        sender.sendMessage("§7• §e/khilinh             §8- §fCho Pháp Bảo ăn item thừa nuôi dưỡng Khí Linh phò trợ.");
        sender.sendMessage("§7• §e/tuitutien           §8- §fMở Túi Trữ Vật Tu Tiên (54 ô bảo lưu vĩnh viễn).");
    }

    private void showTextBachNghe(CommandSender sender) {
        sender.sendMessage("§6§l[4. BÁCH NGHỆ & CƠ DUYÊN]");
        sender.sendMessage("§7• §e/bachnghe list       §8- §fXem 6 nghề phụ (Đan, Khí, Trận, Phù, Thú, Nông).");
        sender.sendMessage("§7• §e/bachnghe chon <id>  §8- §fChọn nghề nghiệp tu tiên.");
        sender.sendMessage("§7• §e/bachnghe status     §8- §fXem cấp độ và đặc quyền nghề hiện tại.");
        sender.sendMessage("§7• §e/coduyen status      §8- §fKiểm tra điểm Hệ Thống Kim Thủ Chỉ.");
        sender.sendMessage("§7• §e/coduyen dung        §8- §fSử dụng điểm Cơ Duyên đổi phúc lợi.");
        sender.sendMessage("§7• §e/thao                §8- §fXem thông tin và dược tính các loài Tiên Thảo.");
        sender.sendMessage("§7• §e/linhchungshop       §8- §fMua bán hạt giống Linh Chủng theo cảnh giới.");
        sender.sendMessage("§7• §e/tutien reroll       §8- §fQuay lại Linh Căn bằng Linh Thạch.");
        sender.sendMessage("§7• §e/tutien reroll_toc   §8- §fQuay lại Chủng Tộc bằng Linh Thạch.");
    }

    private void showTextTongMon(CommandSender sender) {
        sender.sendMessage("§e§l[5. TÔNG MÔN & SƯ ĐỒ]");
        sender.sendMessage("§7• §e/tongmon tao <tên>   §8- §fKhai tông lập phái, thành lập môn phái riêng.");
        sender.sendMessage("§7• §e/tongmon info        §8- §fXem thông tin môn phái và thành viên.");
        sender.sendMessage("§7• §e/tongmon moi <p>     §8- §fMời người chơi gia nhập tông môn.");
        sender.sendMessage("§7• §e/tongmon kho         §8- §fMở rương kho tàng bảo vật tông môn.");
        sender.sendMessage("§7• §e/mochieumo           §8- §fMở hoặc đóng trạng thái chiêu mộ đệ tử công khai.");
        sender.sendMessage("§7• §e/suphu <player>      §8- §fBái sư hoặc nhận đệ tử chân truyền.");
        sender.sendMessage("§7• §e/dedoan              §8- §fQuản lý đồ đoàn và đệ tử môn hạ.");
        sender.sendMessage("§7• §e/dedoan npc tao <tên>§8- §fTạo đệ tử NPC theo sau phò tá chiến đấu.");
        sender.sendMessage("§7• §e/party <create|moi>  §8- §fTổ đội cùng săn quái và tu luyện.");
        sender.sendMessage("§7• §e/trade <player>      §8- §fGiao dịch vật phẩm an toàn 2 bên.");
        sender.sendMessage("§7• §e/tang <player>       §8- §fTặng vật phẩm trên tay cho đạo hữu.");
    }

    private void showTextPlayerCommands(CommandSender sender) {
        sender.sendMessage("§a§l════════════ BẢNG LỆNH NGƯỜI CHƠI ════════════");
        for (TuTienGuideGUI.CommandEntry cmd : TuTienGuideGUI.getPlayerCommands()) {
            sender.sendMessage(" §e" + cmd.syntax() + " §8- §7" + cmd.desc());
        }
    }

    private void showTextAdminCommands(CommandSender sender) {
        if (!sender.hasPermission("tutien.admin") && !sender.isOp()) {
            sender.sendMessage("§cBạn không có quyền quản trị viên!");
            return;
        }
        sender.sendMessage("§c§l════════════ BẢNG LỆNH ADMIN (QUẢN TRỊ) ════════════");
        for (TuTienGuideGUI.CommandEntry cmd : TuTienGuideGUI.getAdminCommands()) {
            sender.sendMessage(" §c" + cmd.syntax() + " §8- §7" + cmd.desc());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("gui", "batdau", "kiemdao", "luyenche", "bachnghe", "tongmon", "lenh"));
            if (sender.hasPermission("tutien.admin") || sender.isOp()) {
                list.add("admin");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
