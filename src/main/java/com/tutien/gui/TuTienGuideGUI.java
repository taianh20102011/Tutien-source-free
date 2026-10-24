package com.tutien.gui;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * TuTienGuideGUI - Giao diện Cẩm Nang Tu Tiên & Tra Cứu Toàn Bộ Lệnh
 * Hỗ trợ tra cứu trực quan, phân loại khoa học và tương tác nhấp lệnh tiện lợi.
 */
public final class TuTienGuideGUI implements Listener {

    private final TuTienPlugin plugin;

    public TuTienGuideGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class GuideHolder implements InventoryHolder {
        private final UUID viewer;
        private final String viewType;
        private final int page;
        private final Map<Integer, Runnable> actions = new HashMap<>();
        private Inventory inventory;

        public GuideHolder(UUID viewer, String viewType, int page) {
            this.viewer = viewer;
            this.viewType = viewType;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // ============================================================
    // 🌟 MỞ GIAO DIỆN CHÍNH (MAIN MENU)
    // ============================================================

    public void open(Player player) {
        openMain(player);
    }

    public void openMain(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "MAIN", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ CẨM NANG TU TIÊN GIỚI ✦");
        holder.inventory = inv;

        fillBorder(inv);

        // Slot 4: Thông tin đạo hữu
        inv.setItem(4, createPlayerProfile(player, data));

        // Slot 19: Nhập Môn & Tu Luyện
        inv.setItem(19, makeItem(Material.BOOK, "§a§l📜 NHẬP MÔN & TU LUYỆN",
                List.of(
                        "§7Tìm hiểu về con đường tu tiên vấn đạo:",
                        "§8• §fHấp thu Linh Khí & Tĩnh tọa thiền định",
                        "§8• §fVượt Tâm Ma & Độ Kiếp phi thăng",
                        "§8• §fHệ thống 25 Cảnh Giới Tu Chân",
                        "§8• §fHào quang Tu Vi & Uy Áp cảnh giới",
                        "",
                        "§e▶ Nhấp chuột để xem chi tiết bí kíp"
                )));
        holder.actions.put(19, () -> openTuLuyen(player));

        // Slot 21: Kiếm Đạo & Kỹ Năng
        inv.setItem(21, makeItem(Material.NETHERITE_SWORD, "§b§l⚔ KIẾM ĐẠO & KỸ NĂNG",
                List.of(
                        "§7Vạn pháp quy tông, kiếm đạo vô song:",
                        "§8• §fNgự Kiếm Phi Hành (Đạp kiếm lướt gió)",
                        "§8• §fTriệu Kiếm (Vạn Kiếm Quy Tông hộ thể)",
                        "§8• §fLinh Thú đồng hành cùng tác chiến",
                        "§8• §fHọc Bí Kíp Bị Động & Đổi nhánh kiếm",
                        "§8• §fCông Pháp tu chân tùy chỉnh",
                        "",
                        "§e▶ Nhấp chuột để xem chi tiết bí kíp"
                )));
        holder.actions.put(21, () -> openKiemDao(player));

        // Slot 23: Luyện Chế & Trang Bị
        inv.setItem(23, makeItem(Material.BREWING_STAND, "§d§l🔮 LUYỆN CHẾ & PHÁP BẢO",
                List.of(
                        "§7Luyện đan chế khí, bảo vật hộ thân:",
                        "§8• §f32 Công thức Luyện Đan huyền diệu",
                        "§8• §fRèn Đúc nâng phẩm giai Pháp Bảo",
                        "§8• §fCường Hóa +1 đến +10 gia tăng chỉ số",
                        "§8• §fKhảm Nạm ngọc thuộc tính & Dung Hợp",
                        "§8• §fKhai mở Khí Linh & Khắc ấn Linh Thức",
                        "§8• §fTúi Trữ Vật Tu Tiên (54 ô bảo lưu)",
                        "",
                        "§e▶ Nhấp chuột để xem chi tiết bí kíp"
                )));
        holder.actions.put(23, () -> openLuyenChe(player));

        // Slot 25: Bách Nghệ & Cơ Duyên
        inv.setItem(25, makeItem(Material.ENCHANTED_GOLDEN_APPLE, "§6§l🌾 BÁCH NGHỆ & CƠ DUYÊN",
                List.of(
                        "§7Cơ duyên nghịch thiên, bách nghệ tinh thông:",
                        "§8• §f6 Nghề Phụ: Đan, Khí, Trận, Phù, Thú, Nông",
                        "§8• §fKim Thủ Chỉ (Hệ Thống phúc lợi tu tiên)",
                        "§8• §fThu thập Tiên Thảo & Mua bán Linh Chủng",
                        "§8• §fTẩy luyện Linh Căn & Dị Biến Chủng Tộc",
                        "",
                        "§e▶ Nhấp chuột để xem chi tiết bí kíp"
                )));
        holder.actions.put(25, () -> openBachNghe(player));

        // Slot 29: Tông Môn & Sư Đồ
        inv.setItem(29, makeItem(Material.BEACON, "§e§l🏯 TÔNG MÔN & SƯ ĐỒ",
                List.of(
                        "§7Kết giao đạo lữ, khai tông lập phái:",
                        "§8• §fKhai mở Tông Môn, chiêu mộ đệ tử",
                        "§8• §fBái Sư - Thu Nhận Đồ Đệ chân truyền",
                        "§8• §fĐệ Tử NPC phò tá chiến đấu",
                        "§8• §fTổ Đội (Party) cùng săn Boss & Tu luyện",
                        "§8• §fGiao Dịch an toàn & Tặng phẩm vật",
                        "§8• §fSự kiện Huyết Nguyệt & Boss Thế Giới",
                        "",
                        "§e▶ Nhấp chuột để xem chi tiết bí kíp"
                )));
        holder.actions.put(29, () -> openTongMon(player));

        // Slot 31: Toàn Bộ Lệnh Người Chơi
        inv.setItem(31, makeItem(Material.WRITABLE_BOOK, "§a§l📖 BẢNG TRA CỨU LỆNH NGƯỜI CHƠI",
                List.of(
                        "§7Danh sách đầy đủ tất cả các lệnh của plugin.",
                        "§7Phân loại theo từng tính năng, có cú pháp & mô tả.",
                        "",
                        "§e▶ Nhấp để mở bảng tra cứu lệnh"
                )));
        holder.actions.put(31, () -> openPlayerCommands(player, 0));

        // Slot 33: Toàn Bộ Lệnh Admin
        boolean isAdmin = player.hasPermission("tutien.admin") || player.isOp();
        if (isAdmin) {
            inv.setItem(33, makeItem(Material.COMMAND_BLOCK, "§c§l👑 BẢNG TRA CỨU LỆNH ADMIN",
                    List.of(
                            "§7Dành riêng cho Quản Trị Viên & OP.",
                            "§7Tra cứu toàn bộ lệnh cấu hình, phát đồ, quản lý.",
                            "",
                            "§e▶ Nhấp để mở bảng lệnh Admin"
                    )));
            holder.actions.put(33, () -> openAdminCommands(player, 0));
        } else {
            inv.setItem(33, makeItem(Material.BARRIER, "§8§l👑 LỆNH QUẢN TRỊ VIÊN",
                    List.of(
                            "§7Khu vực dành riêng cho Quản Trị Viên.",
                            "§cBạn không có quyền truy cập mục này."
                    )));
        }

        // Slot 49: Đóng
        inv.setItem(49, makeItem(Material.BARRIER, "§c§lĐÓNG CẨM NANG", List.of("§7Nhấp để đóng giao diện")));
        holder.actions.put(49, player::closeInventory);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
    }

    // ============================================================
    // 1. NHẬP MÔN & TU LUYỆN
    // ============================================================

    public void openTuLuyen(Player player) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "TU_LUYEN", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ NHẬP MÔN & TU LUYỆN ✦");
        holder.inventory = inv;
        fillBorder(inv);

        inv.setItem(10, makeItem(Material.EXPERIENCE_BOTTLE, "§a1. Hấp Thu Linh Khí & Tu Vi",
                List.of(
                        "§7Linh Khí (LK) là căn cơ của mọi tu sĩ.",
                        "§7Cách tích lũy Linh Khí:",
                        "§8• §fĐánh quái, đào khoáng, chặt cây, trồng trọt.",
                        "§8• §fSử dụng Đan Dược tăng tu vi.",
                        "§8• §fĐứng trong khu vực §eLinh Mạch§f.",
                        "§8• §fDùng lệnh §e/thiendinh§f để ngồi thiền lấy LK tự động.",
                        "",
                        "§eLệnh: §f/linhkhi §7hoặc §f/lk"
                )));
        holder.actions.put(10, () -> suggestCommand(player, "/linhkhi"));

        inv.setItem(12, makeItem(Material.CAMPFIRE, "§b2. Thiền Định Tĩnh Tọa",
                List.of(
                        "§7Khi kích hoạt Thiền Định, tu sĩ sẽ ngồi tĩnh tọa,",
                        "§7tâm thần thanh tịnh, liên tục hấp thu linh khí",
                        "§7thiên địa mà không cần phải săn quái.",
                        "",
                        "§eLệnh: §f/thiendinh [bat|tat]",
                        "§7Click để bật/tắt Thiền Định ngay!"
                )));
        holder.actions.put(12, () -> runCommand(player, "thiendinh"));

        inv.setItem(14, makeItem(Material.WITHER_SKELETON_SKULL, "§c3. Vượt Tâm Ma",
                List.of(
                        "§7Từ §fThánh Giới (Bán Thánh)§7 trở lên, trước khi",
                        "§7đột phá BẮT BUỘC phải chiến thắng Tâm Ma của chính mình.",
                        "§7Tâm Ma sở hữu sức mạnh và ngoại hình giống hệt bạn.",
                        "§7Chiến thắng mới có đủ tư cách nghênh đón Lôi Kiếp.",
                        "",
                        "§eLệnh: §f/vuottamma",
                        "§7Click để bắt đầu thử thách Tâm Ma"
                )));
        holder.actions.put(14, () -> runCommand(player, "vuottamma"));

        inv.setItem(16, makeItem(Material.LIGHTNING_ROD, "§e4. Thiên Kiếp & Đột Phá",
                List.of(
                        "§7Khi Linh Khí đạt tối đa (100%):",
                        "§8• §fHệ thống sẽ thông báo sẵn sàng đột phá.",
                        "§8• §fGõ §e/dotpha§f để giáng lôi kiếp thử thách.",
                        "§8• §fHãy chuẩn bị máu và Đan Dược phòng thủ!",
                        "§8• §fNếu chưa sẵn sàng, gõ §e/boqua§f để hoãn lại.",
                        "",
                        "§eLệnh: §f/dotpha §7hoặc §f/boqua"
                )));
        holder.actions.put(16, () -> suggestCommand(player, "/dotpha"));

        inv.setItem(28, makeItem(Material.NETHER_STAR, "§65. Hệ Thống 25 Cảnh Giới",
                List.of(
                        "§7Chia làm 5 Đại Giới lớn:",
                        "§81. §fLinh Giới: §aLuyện Khí ➔ Trúc Cơ ➔ Kết Đan ➔ Nguyên Anh ➔ Hóa Thần",
                        "§82. §cĐế Giới: §cĐế Tu ➔ Đế Hậu ➔ Đế Vương ➔ Đế Tôn ➔ Đế Chủ",
                        "§83. §fThánh Giới: §fBán Thánh ➔ Chuẩn Thánh ➔ Đại Thánh ➔ Thánh Tôn ➔ Thánh Đế §b[BAY ĐƯỢC]",
                        "§84. §bTiên Giới: §9Chân Tiên ➔ Kim Tiên ➔ Thái Ất ➔ Đại La ➔ Tiên Đế",
                        "§85. §eThiên Giới: §eThiên Tôn ➔ Thiên Đạo ➔ Thiên Đế ➔ Hỗn Nguyên ➔ Vô Thượng Chí Tôn"
                )));

        inv.setItem(30, makeItem(Material.BEACON, "§d6. Hào Quang Tu Vi (Aura)",
                List.of(
                        "§7Vòng sáng linh lực luân chuyển quanh người",
                        "§7tùy theo Cảnh Giới và phẩm vị của bạn.",
                        "§7Có thể chủ động bật hoặc tắt bất cứ lúc nào.",
                        "",
                        "§eLệnh: §f/aura [on|off]",
                        "§7Click để bật/tắt hào quang ngay"
                )));
        holder.actions.put(30, () -> runCommand(player, "aura"));

        inv.setItem(32, makeItem(Material.DRAGON_HEAD, "§47. Uy Áp Cảnh Giới",
                List.of(
                        "§7Đại năng cảnh giới cao tỏa ra khí tức đè bẹp",
                        "§7kẻ có tu vi thấp hơn trong bán kính xung quanh,",
                        "§7khiến đối phương bị làm chậm, suy yếu và giảm sát thương.",
                        "",
                        "§eLệnh: §f/uyap [on|off|status]"
                )));
        holder.actions.put(32, () -> runCommand(player, "uyap"));

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    // ============================================================
    // 2. KIẾM ĐẠO & KỸ NĂNG
    // ============================================================

    public void openKiemDao(Player player) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "KIEM_DAO", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ KIẾM ĐẠO & KỸ NĂNG ✦");
        holder.inventory = inv;
        fillBorder(inv);

        inv.setItem(11, makeItem(Material.DIAMOND_SWORD, "§b1. Ngự Kiếm Phi Hành",
                List.of(
                        "§7Tuyệt kỹ độc quyền của Kiếm Tu (Thánh Giới+).",
                        "§7Triệu hồi phi kiếm dưới chân, tự do bay lượn trên trời.",
                        "§8• §fMũi kiếm xoay 3D chuẩn xác theo hướng nhìn.",
                        "§8• §fHiệu ứng vệt khí động học 3D uốn lượn tuyệt đẹp.",
                        "§8• §fSau khi bật, bấm §eSpace x2§f để cất cánh phi hành.",
                        "",
                        "§eLệnh: §f/ngukiem §7hoặc §f/phikiem",
                        "§7Click để bật/tắt Ngự Kiếm!"
                )));
        holder.actions.put(11, () -> runCommand(player, "ngukiem"));

        inv.setItem(13, makeItem(Material.IRON_SWORD, "§e2. Triệu Kiếm (Vạn Kiếm Quy Tông)",
                List.of(
                        "§7Triệu hồi tối đa 5 phi kiếm bay hộ thể sau lưng.",
                        "§7Tự động xuất chiêu trảm sát mục tiêu khi vào giao tranh.",
                        "§8• §fChế độ AUTO: Tự động đánh quái lại gần.",
                        "§8• §fChế độ SEMI: Chỉ đánh khi bạn đánh hoặc bị đánh.",
                        "§8• §fChế độ OFF: Chỉ bay làm cảnh sau lưng.",
                        "",
                        "§eLệnh: §f/trieukiem [mode] §7(viết tắt: §f/tk§7)",
                        "§7Click để chuyển đổi chế độ triệu kiếm"
                )));
        holder.actions.put(13, () -> runCommand(player, "trieukiem"));

        inv.setItem(15, makeItem(Material.WOLF_SPAWN_EGG, "§a3. Triệu Hồi Linh Thú",
                List.of(
                        "§7Triệu hồi Linh Thú đồng hành cùng tác chiến.",
                        "§7Dành cho tất cả môn phái khi đạt tu vi yêu cầu.",
                        "§7Linh Thú có chỉ số và kỹ năng hỗ trợ riêng biệt.",
                        "",
                        "§eLệnh: §f/trieuthu §7hoặc §f/linhthu",
                        "§e▶ Click để mở Giao Diện Quản Lý Linh Thú"
                )));
        holder.actions.put(15, () -> plugin.getPetGUI().open(player));

        inv.setItem(29, makeItem(Material.ENCHANTED_BOOK, "§d4. Bí Kíp Bị Động & Nội Tại",
                List.of(
                        "§7Tu sĩ có thể lĩnh ngộ tối đa 3 bí kíp bị động.",
                        "§8• §fCầm bí kíp trên tay và dùng lệnh §e/hocbidong <hệ>§f.",
                        "§8• §fCác hệ: §catk§f, §ahp§f, §bdef§f, §fspeed§f, §eluck§f, §dregen§f.",
                        "§8• §fXem danh sách bí kíp đã học bằng §e/bidong§f.",
                        "",
                        "§eLệnh: §f/bidong §7và §f/hocbidong"
                )));
        holder.actions.put(29, () -> runCommand(player, "bidong"));

        inv.setItem(31, makeItem(Material.BOOKSHELF, "§65. Hệ Thống Công Pháp",
                List.of(
                        "§7Học và trang bị các bộ Công Pháp tu tiên độc môn:",
                        "§8• §fTăng vĩnh viễn các chỉ số thuộc tính cơ bản.",
                        "§8• §fMở khóa các thức chiêu thức chủ động đặc biệt.",
                        "§8• §e/congphap list§f: Xem danh sách công pháp.",
                        "§8• §e/congphap hoc <id>§f: Học công pháp.",
                        "§8• §e/congphap trangbi <id>§f: Kích hoạt công pháp.",
                        "",
                        "§eLệnh: §f/congphap"
                )));
        holder.actions.put(31, () -> runCommand(player, "congphap"));

        inv.setItem(33, makeItem(Material.NETHER_STAR, "§c6. Tùy Chỉnh Kỹ Năng (GUI)",
                List.of(
                        "§7Mở bảng cài đặt bật/tắt các module kỹ năng:",
                        "§8• §fThể Chất, Công Pháp, Bị Động, Độn Thuật, Phản Đòn...",
                        "",
                        "§eLệnh: §f/kinang",
                        "§7Click để mở menu cài đặt kỹ năng"
                )));
        holder.actions.put(33, () -> runCommand(player, "kinang"));

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    // ============================================================
    // 3. LUYỆN CHẾ & PHÁP BẢO
    // ============================================================

    public void openLuyenChe(Player player) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "LUYEN_CHE", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ LUYỆN CHẾ & PHÁP BẢO ✦");
        holder.inventory = inv;
        fillBorder(inv);

        inv.setItem(10, makeItem(Material.POTION, "§a1. Luyện Đan Dược (32 Loại)",
                List.of(
                        "§7Dùng lò luyện đan để chế tạo tiên đan diệu dược:",
                        "§8• §fĐan tăng Linh Khí, đan bảo vệ khi độ kiếp.",
                        "§8• §fĐan tăng tốc độ di chuyển, tăng sát thương.",
                        "§8• §e/luyen danh_sach§f: Xem tất cả công thức đan.",
                        "§8• §e/luyen <tên_đan>§f: Tiến hành luyện chế.",
                        "",
                        "§eLệnh: §f/luyen [tên_đan|danh_sach]"
                )));
        holder.actions.put(10, () -> runCommand(player, "luyen danh_sach"));

        inv.setItem(12, makeItem(Material.ANVIL, "§e2. Rèn Đúc Nâng Phẩm Pháp Bảo",
                List.of(
                        "§7Pháp Bảo chia làm 6 phẩm giai:",
                        "§8• §7Hạ Phẩm ➔ §aTrung Phẩm ➔ §bThượng Phẩm ➔ §dCực Phẩm ➔ §6Đế Phẩm ➔ §cTiên Phẩm",
                        "§7Dùng lệnh Rèn Đúc để thăng giai trang bị.",
                        "",
                        "§eLệnh: §f/renduc §7hoặc §f/rd"
                )));
        holder.actions.put(12, () -> runCommand(player, "renduc"));

        inv.setItem(14, makeItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§b3. Cường Hóa Pháp Bảo (+1 ➔ +10)",
                List.of(
                        "§7Nâng cấp Pháp Bảo từ +1 đến +10:",
                        "§8• §fGiữ nguyên phẩm giai, tăng % tất cả chỉ số.",
                        "§8• §fCần chuẩn bị Linh Thạch và đá cường hóa.",
                        "",
                        "§eLệnh: §f/cuonghoa §7hoặc §f/chpb"
                )));
        holder.actions.put(14, () -> runCommand(player, "cuonghoa"));

        inv.setItem(16, makeItem(Material.AMETHYST_SHARD, "§d4. Khảm Nạm Ngọc Thuộc Tính",
                List.of(
                        "§7Khảm các loại ngọc nguyên tố vào lỗ khảm Pháp Bảo:",
                        "§8• §cNgọc Hỏa: §7Tăng thiêu đốt và sát thương công kích.",
                        "§8• §bNgọc Băng: §7Làm chậm và đóng băng mục tiêu.",
                        "§8• §eNgọc Lôi: §7Gây choáng và sét đánh lan.",
                        "",
                        "§eLệnh: §f/khamnam §7hoặc §f/kn"
                )));
        holder.actions.put(16, () -> runCommand(player, "khamnam"));

        inv.setItem(28, makeItem(Material.BLAST_FURNACE, "§65. Dung Hợp Pháp Bảo",
                List.of(
                        "§7Dung hợp 2 pháp bảo cùng loại:",
                        "§8• §fGhép dòng ấn bí truyền của cả hai món.",
                        "§8• §fCó cơ hội đột biến thăng phẩm giai trang bị.",
                        "",
                        "§eLệnh: §f/dunghop §7hoặc §f/dh"
                )));
        holder.actions.put(28, () -> runCommand(player, "dunghop"));

        inv.setItem(30, makeItem(Material.ENDER_EYE, "§56. Khí Linh & Linh Thức",
                List.of(
                        "§8• §eKhí Linh (/khilinh)§f: Cho Pháp Bảo ăn trang bị",
                        "§7  thừa để tích lũy linh tính, triệu hồi Khí Linh phò trợ.",
                        "§8• §eLinh Thức (/linhthuc)§f: Gán kỹ năng tự kích hoạt",
                        "§7  khi gặp nguy hiểm (Hộ thể, phản đòn, hồi sinh).",
                        "",
                        "§eLệnh: §f/khilinh §7và §f/linhthuc"
                )));
        holder.actions.put(30, () -> runCommand(player, "khilinh"));

        inv.setItem(32, makeItem(Material.CHEST, "§e7. Túi Trữ Vật Tu Tiên",
                List.of(
                        "§7Túi đồ thần bí 54 ô độc lập dành riêng cho tu sĩ,",
                        "§7không bị mất khi chết (tùy thiết lập server).",
                        "",
                        "§eLệnh: §f/tuitutien §7hoặc §f/ttbag"
                )));
        holder.actions.put(32, () -> runCommand(player, "tuitutien"));

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    // ============================================================
    // 4. BÁCH NGHỆ & CƠ DUYÊN
    // ============================================================

    public void openBachNghe(Player player) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "BACH_NGHE", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ BÁCH NGHỆ & CƠ DUYÊN ✦");
        holder.inventory = inv;
        fillBorder(inv);

        inv.setItem(11, makeItem(Material.GOLDEN_PICKAXE, "§61. Bách Nghệ Tu Tiên (6 Nghề Phụ)",
                List.of(
                        "§7Mỗi tu sĩ có thể chọn theo đuổi nghề nghiệp:",
                        "§81. §dLuyện Đan Sư: §7Chuyên chế xuất đan dược cao cấp.",
                        "§82. §cLuyện Khí Sư: §7Rèn đúc, cường hóa thần binh bảo giáp.",
                        "§83. §bTrận Pháp Sư: §7Bố trí kết giới, linh trận hộ thân.",
                        "§84. §ePhù Lục Sư: §7Chế tạo bùa chú linh ứng công phòng.",
                        "§85. §aNgự Thú Sư: §7Thuần hóa và tăng cường chiến lực Linh Thú.",
                        "§86. §2Linh Nông Sư: §7Trồng trọt, thu hoạch tiên thảo linh chủng.",
                        "",
                        "§eLệnh: §f/bachnghe <list|info|chon|status>"
                )));
        holder.actions.put(11, () -> runCommand(player, "bachnghe status"));

        inv.setItem(13, makeItem(Material.NETHER_STAR, "§b2. Cơ Duyên & Kim Thủ Chỉ",
                List.of(
                        "§7Hệ thống ban tặng cho đạo hữu kỳ ngộ đặc biệt:",
                        "§8• §fTích lũy Điểm Hệ Thống khi tu luyện, làm nhiệm vụ.",
                        "§8• §fDùng điểm để đổi quà, mở khóa may mắn, bảo hiểm đột phá.",
                        "",
                        "§eLệnh: §f/coduyen status §7hoặc §f/coduyen dung"
                )));
        holder.actions.put(13, () -> runCommand(player, "coduyen status"));

        inv.setItem(15, makeItem(Material.WHEAT, "§a3. Tiên Thảo & Linh Chủng",
                List.of(
                        "§7Thu hái tiên thảo trong thiên nhiên hoặc gieo trồng:",
                        "§8• §fXem thông tin các loài thảo dược bằng §e/thao§f.",
                        "§8• §fMua bán hạt giống linh chủng qua §e/linhchungshop§f.",
                        "",
                        "§eLệnh: §f/thao §7và §f/linhchungshop"
                )));
        holder.actions.put(15, () -> runCommand(player, "thao"));

        inv.setItem(31, makeItem(Material.TOTEM_OF_UNDYING, "§d4. Linh Căn & Chủng Tộc",
                List.of(
                        "§8• §fXem Tộc của bạn: §e/toc§f.",
                        "§8• §fQuay lại Linh Căn: §e/tutien reroll§f.",
                        "§8• §fQuay lại Chủng Tộc: §e/tutien reroll_toc§f.",
                        "§7Linh căn càng thuần khiết (Thiên Linh Căn),",
                        "§7tốc độ hấp thu linh khí càng tăng vọt!"
                )));
        holder.actions.put(31, () -> runCommand(player, "toc"));

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    // ============================================================
    // 5. TÔNG MÔN & SƯ ĐỒ
    // ============================================================

    public void openTongMon(Player player) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "TONG_MON", 0);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ TÔNG MÔN & SƯ ĐỒ ✦");
        holder.inventory = inv;
        fillBorder(inv);

        inv.setItem(11, makeItem(Material.SHIELD, "§b1. Tông Môn (Bang Hội)",
                List.of(
                        "§7Thành lập hoặc gia nhập tông môn tu tiên:",
                        "§8• §e/tongmon tao <tên>§f: Lập tông môn mới.",
                        "§8• §e/tongmon info§f: Xem thông tin tông môn.",
                        "§8• §e/tongmon moi <player>§f: Mời người vào môn phái.",
                        "§8• §e/tongmon kho§f: Mở rương chứa đồ của tông môn.",
                        "§8• §e/mochieumo§f: Mở tuyển đệ tử công khai.",
                        "",
                        "§eLệnh: §f/tongmon §7hoặc §f/tm"
                )));
        holder.actions.put(11, () -> runCommand(player, "tongmon info"));

        inv.setItem(13, makeItem(Material.PLAYER_HEAD, "§e2. Sư Đồ & Đệ Tử NPC",
                List.of(
                        "§7Bái sư học đạo hoặc nhận đệ tử chân truyền:",
                        "§8• §e/suphu <player>§f: Nhận người chơi làm đệ tử.",
                        "§8• §e/dedoan§f: Quản lý đệ tử môn hạ.",
                        "§8• §e/dedoan npc tao <tên>§f: Tạo đệ tử NPC theo sau phò tá.",
                        "§8• §e/dedoan npc list§f: Danh sách đệ tử NPC.",
                        "",
                        "§eLệnh: §f/suphu §7và §f/dedoan"
                )));
        holder.actions.put(13, () -> runCommand(player, "dedoan"));

        inv.setItem(15, makeItem(Material.GOLD_INGOT, "§a3. Tổ Đội & Giao Dịch",
                List.of(
                        "§8• §eTổ Đội (/party)§f: Lập nhóm cùng săn Boss, chia sẻ điểm.",
                        "§8• §eGiao Dịch (/trade <player>)§f: Trao đổi an toàn 2 bên.",
                        "§8• §eTặng Quà (/tang <player>)§f: Tặng item trên tay cho bạn bè.",
                        "",
                        "§eLệnh: §f/party §7hoặc §f/trade"
                )));
        holder.actions.put(15, () -> runCommand(player, "party info"));

        inv.setItem(29, makeItem(Material.REDSTONE_BLOCK, "§c4. Sự Kiện Huyết Nguyệt",
                List.of(
                        "§7Hiện tượng dị tượng trời đêm đẫm máu:",
                        "§8• §fQuái vật cuồng bạo x2 sát thương nhưng rớt x3 phần thưởng.",
                        "§8• §fSử dụng §e/binhxl <cong|farm|heal>§f để kích hoạt bình huyết.",
                        "",
                        "§eLệnh: §f/binhxl"
                )));
        holder.actions.put(29, () -> runCommand(player, "binhxl"));

        inv.setItem(31, makeItem(Material.WITHER_ROSE, "§55. Công Đức & Nhiệm Vụ",
                List.of(
                        "§8• §e/karma§f: Kiểm tra điểm Công Đức (thiện) và Nghiệp Lực (ác).",
                        "§8• §e/quest§f: Nhận và kiểm tra nhiệm vụ hàng ngày/tuần.",
                        "",
                        "§eLệnh: §f/karma §7và §f/quest"
                )));
        holder.actions.put(31, () -> runCommand(player, "quest"));

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
    }

    // ============================================================
    // 6. TOÀN BỘ LỆNH NGƯỜI CHƠI (PLAYER COMMANDS)
    // ============================================================

    public void openPlayerCommands(Player player, int page) {
        GuideHolder holder = new GuideHolder(player.getUniqueId(), "PLAYER_CMDS", page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ BẢNG LỆNH NGƯỜI CHƠI (" + (page + 1) + "/2) ✦");
        holder.inventory = inv;
        fillBorder(inv);

        List<CommandEntry> list = getPlayerCommands();
        int pageSize = 28;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, list.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex; i < endIndex; i++) {
            CommandEntry cmd = list.get(i);
            int slot = slots[i - startIndex];
            inv.setItem(slot, makeItem(cmd.icon, "§e" + cmd.syntax,
                    List.of(
                            "§aTên lệnh: §f" + cmd.name,
                            "§7Công dụng: " + cmd.desc,
                            (cmd.aliases.isEmpty() ? "" : "§8Viết tắt: " + cmd.aliases),
                            "",
                            "§b▶ Nhấp để gợi ý lệnh vào thanh chat"
                    )));
            holder.actions.put(slot, () -> suggestCommand(player, cmd.syntax.split(" ")[0]));
        }

        // Chuyển trang
        if (page > 0) {
            inv.setItem(48, makeItem(Material.PAPER, "§e◀ Trang Trước (" + page + ")", List.of()));
            holder.actions.put(48, () -> openPlayerCommands(player, page - 1));
        }
        if (endIndex < list.size()) {
            inv.setItem(50, makeItem(Material.PAPER, "§eTrang Tiếp ▶ (" + (page + 2) + ")", List.of()));
            holder.actions.put(50, () -> openPlayerCommands(player, page + 1));
        }

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
    }

    // ============================================================
    // 7. TOÀN BỘ LỆNH QUẢN TRỊ (ADMIN COMMANDS)
    // ============================================================

    public void openAdminCommands(Player player, int page) {
        if (!player.hasPermission("tutien.admin") && !player.isOp()) {
            player.sendMessage(plugin.prefix() + "§cBạn không có quyền quản trị viên!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        GuideHolder holder = new GuideHolder(player.getUniqueId(), "ADMIN_CMDS", page);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ BẢNG LỆNH ADMIN (" + (page + 1) + "/2) ✦");
        holder.inventory = inv;
        fillBorder(inv);

        List<CommandEntry> list = getAdminCommands();
        int pageSize = 28;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, list.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex; i < endIndex; i++) {
            CommandEntry cmd = list.get(i);
            int slot = slots[i - startIndex];
            inv.setItem(slot, makeItem(cmd.icon, "§c" + cmd.syntax,
                    List.of(
                            "§6Lệnh: §f" + cmd.name,
                            "§7Công dụng: " + cmd.desc,
                            (cmd.aliases.isEmpty() ? "" : "§8Viết tắt: " + cmd.aliases),
                            "",
                            "§e▶ Nhấp để gợi ý lệnh vào thanh chat"
                    )));
            holder.actions.put(slot, () -> suggestCommand(player, cmd.syntax.split(" ")[0]));
        }

        // Chuyển trang
        if (page > 0) {
            inv.setItem(48, makeItem(Material.PAPER, "§e◀ Trang Trước (" + page + ")", List.of()));
            holder.actions.put(48, () -> openAdminCommands(player, page - 1));
        }
        if (endIndex < list.size()) {
            inv.setItem(50, makeItem(Material.PAPER, "§eTrang Tiếp ▶ (" + (page + 2) + ")", List.of()));
            holder.actions.put(50, () -> openAdminCommands(player, page + 1));
        }

        addBackButton(inv, holder, () -> openMain(player));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
    }

    // ============================================================
    // DANH SÁCH DỮ LIỆU LỆNH
    // ============================================================

    public record CommandEntry(String name, String syntax, String aliases, String desc, Material icon) {}

    public static List<CommandEntry> getPlayerCommands() {
        return List.of(
                new CommandEntry("tutien chon", "/tutien chon [class]", "/tt chon", "Chọn môn phái tu tiên (Kiếm Tu, Thể Tu, Pháp Tu...)", Material.NETHER_STAR),
                new CommandEntry("tutien dao", "/tutien dao [dao]", "/tt dao", "Chọn con đường Đạo (Chính Đạo, Ma Đạo, Kiếm Đạo...)", Material.BOOK),
                new CommandEntry("tutien info", "/tutien info [player]", "/tt info", "Xem thông tin tu vi, cảnh giới, chỉ số", Material.PLAYER_HEAD),
                new CommandEntry("tutien top", "/tutien top [stage|tien]", "/tt top", "Xem bảng xếp hạng cao thủ tu tiên", Material.GOLD_BLOCK),
                new CommandEntry("linhkhi", "/linhkhi", "/lk", "Kiểm tra tiến trình Linh Khí và tốc độ hấp thu", Material.EXPERIENCE_BOTTLE),
                new CommandEntry("thiendinh", "/thiendinh [bat|tat]", "/td", "Ngồi thiền tĩnh tọa tăng tốc tích lũy linh khí", Material.CAMPFIRE),
                new CommandEntry("vuottamma", "/vuottamma", "/vtm", "Vượt Tâm Ma (bắt buộc từ Bán Thánh trở lên để đột phá)", Material.WITHER_SKELETON_SKULL),
                new CommandEntry("dotpha", "/dotpha", "/dp", "Kích hoạt lôi kiếp để đột phá cảnh giới mới", Material.LIGHTNING_ROD),
                new CommandEntry("boqua", "/boqua", "/bq", "Bỏ qua thông báo sẵn sàng đột phá", Material.CLOCK),
                new CommandEntry("ngukiem", "/ngukiem", "/nk, /phikiem", "Bật/tắt Ngự Kiếm Phi Hành lướt gió (Kiếm Tu Bán Thánh+)", Material.DIAMOND_SWORD),
                new CommandEntry("trieukiem", "/trieukiem [mode]", "/tk, /kiemdao", "Triệu hồi kiếm bay hộ thể sau lưng (AUTO/SEMI/OFF)", Material.IRON_SWORD),
                new CommandEntry("trieuthu", "/trieuthu", "/ls, /linhthu", "Triệu hồi hoặc thu hồi Linh Thú đồng hành", Material.BONE),
                new CommandEntry("aura", "/aura [on|off]", "/tuvi", "Bật hoặc tắt hào quang vòng Tu Vi phát sáng", Material.BEACON),
                new CommandEntry("uyap", "/uyap [on|off|status]", "", "Bật hoặc tắt hiệu ứng Uy Áp cảnh giới", Material.DRAGON_HEAD),
                new CommandEntry("luyen", "/luyen [tên_đan|danh_sach]", "/ld", "Luyện đan dược theo 32 công thức huyền bí", Material.BREWING_STAND),
                new CommandEntry("renduc", "/renduc", "/rd", "Rèn đúc nâng phẩm giai Pháp Bảo", Material.ANVIL),
                new CommandEntry("cuonghoa", "/cuonghoa", "/chpb", "Cường hóa chỉ số Pháp Bảo từ +1 đến +10", Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new CommandEntry("khamnam", "/khamnam", "/kn", "Khảm nạm ngọc nguyên tố vào lỗ khảm Pháp Bảo", Material.AMETHYST_SHARD),
                new CommandEntry("dunghop", "/dunghop", "/dh", "Dung hợp 2 pháp bảo cùng loại để ghép dòng ấn", Material.BLAST_FURNACE),
                new CommandEntry("linhthuc", "/linhthuc", "/lt", "Gán kỹ năng tự kích hoạt (Linh Thức) lên Pháp Bảo", Material.ENDER_EYE),
                new CommandEntry("khilinh", "/khilinh", "/kl", "Cho Pháp Bảo ăn item thừa để tích lũy nuôi Khí Linh", Material.HEART_OF_THE_SEA),
                new CommandEntry("tuitutien", "/tuitutien", "/ttbag", "Mở túi trữ vật 54 ô không rớt đồ khi chết", Material.CHEST),
                new CommandEntry("thao", "/thao", "/herb", "Xem danh sách và dược tính các loài Tiên Thảo", Material.WHEAT),
                new CommandEntry("linhchungshop", "/linhchungshop <buy|sell> <id> [số]", "/lcshop", "Mua/bán hạt giống linh chủng có khóa cảnh giới", Material.EMERALD),
                new CommandEntry("bachnghe", "/bachnghe <list|chon|status>", "/nghephu", "Chọn và quản lý 6 Nghề Phụ tu tiên", Material.GOLDEN_PICKAXE),
                new CommandEntry("coduyen", "/coduyen <status|dung>", "/kimthuchi", "Khám phá Cơ Duyên và dùng điểm Kim Thủ Chỉ", Material.NETHER_STAR),
                new CommandEntry("tongmon", "/tongmon <tao|info|moi|kho>", "/tm, /sect", "Hệ thống Tông Môn / Bang hội tu tiên", Material.SHIELD),
                new CommandEntry("mochieumo", "/mochieumo", "/mcm", "Bật/tắt trạng thái chiêu mộ đệ tử công khai", Material.WRITABLE_BOOK),
                new CommandEntry("suphu", "/suphu <player>", "", "Bái sư hoặc nhận đệ tử chân truyền", Material.PLAYER_HEAD),
                new CommandEntry("dedoan", "/dedoan [npc <tao|list>]", "", "Quản lý đồ đoàn sư đồ và đệ tử NPC theo sau", Material.ARMOR_STAND),
                new CommandEntry("party", "/party <create|invite|kick>", "/nhom", "Tạo nhóm/tổ đội cùng tu luyện săn Boss", Material.GOLD_INGOT),
                new CommandEntry("trade", "/trade <player>", "/gd", "Giao dịch vật phẩm an toàn với đạo hữu", Material.EMERALD),
                new CommandEntry("tang", "/tang <player>", "/gift", "Tặng vật phẩm đang cầm trên tay cho người khác", Material.CHEST_MINECART),
                new CommandEntry("karma", "/karma", "/congduc", "Xem điểm Công Đức (thiện) và Nghiệp Lực (ác)", Material.WITHER_ROSE),
                new CommandEntry("quest", "/quest", "/nv", "Xem và nhận thưởng nhiệm vụ Tu Tiên", Material.MAP),
                new CommandEntry("binhxl", "/binhxl <cong|farm|heal>", "/binh", "Sử dụng Bình Huyết Nguyệt tăng chỉ số", Material.REDSTONE),
                new CommandEntry("kinang", "/kinang", "", "Mở GUI bật/tắt các module kỹ năng tu tiên", Material.NETHER_STAR),
                new CommandEntry("congphap", "/congphap <list|hoc|trangbi>", "", "Học và trang bị các bộ Công Pháp", Material.BOOKSHELF),
                new CommandEntry("hocbidong", "/hocbidong <atk|hp|def...>", "/hoc", "Lĩnh ngộ bí kíp bị động từ sách trên tay", Material.ENCHANTED_BOOK),
                new CommandEntry("bidong", "/bidong", "/passives", "Xem danh sách bí kíp bị động đã học", Material.BOOK),
                new CommandEntry("doiky", "/doiky <a|b|c|d>", "/doikinang", "Đổi nhánh kỹ năng trong slot Linh Khí Kiếm", Material.FEATHER),
                new CommandEntry("toc", "/toc", "", "Xem thông tin chủng tộc và chỉ số tộc", Material.TOTEM_OF_UNDYING),
                new CommandEntry("tutien reroll", "/tutien reroll", "", "Quay lại Linh Căn bằng Linh Thạch", Material.BLAZE_POWDER),
                new CommandEntry("tutien reroll_toc", "/tutien reroll_toc", "", "Quay lại Chủng Tộc bằng Linh Thạch", Material.GHAST_TEAR)
        );
    }

    public static List<CommandEntry> getAdminCommands() {
        return List.of(
                new CommandEntry("ttadmin info", "/ttadmin info <player>", "/tta info", "Xem toàn bộ hồ sơ dữ liệu tu tiên của người chơi", Material.PLAYER_HEAD),
                new CommandEntry("ttadmin set stage", "/ttadmin set stage <player> <cấp>", "", "Set cảnh giới (LUYEN_KHI, BAN_THANH, VO_THUONG...)", Material.NETHER_STAR),
                new CommandEntry("ttadmin set rank", "/ttadmin set rank <player> <1-6>", "", "Set kỳ nhỏ trong cảnh giới (Sơ kỳ -> Cực hạn)", Material.GOLD_NUGGET),
                new CommandEntry("ttadmin set class", "/ttadmin set class <player> <class>", "", "Set môn phái (KIEM_TU, THE_TU, PHAP_TU...)", Material.IRON_SWORD),
                new CommandEntry("ttadmin set dao", "/ttadmin set dao <player> <dao>", "", "Set hướng đạo (CHINH_DAO, MA_DAO, KIEM_DAO...)", Material.BOOK),
                new CommandEntry("ttadmin set toc", "/ttadmin set toc <player> <toc>", "", "Set chủng tộc cho người chơi", Material.TOTEM_OF_UNDYING),
                new CommandEntry("ttadmin set linhcan", "/ttadmin set linhcan <player> <1-5>", "", "Set phẩm chất linh căn (1 = Thiên linh căn)", Material.AMETHYST_CLUSTER),
                new CommandEntry("ttadmin set lk", "/ttadmin set lk <player> <số_lượng>", "", "Set số điểm Linh Khí hiện tại", Material.EXPERIENCE_BOTTLE),
                new CommandEntry("ttadmin set tien", "/ttadmin set tien <player> <số_lượng>", "", "Set số Linh Thạch / Tiền của người chơi", Material.EMERALD),
                new CommandEntry("ttadmin give phabao", "/ttadmin give phabao <player> <tier> [type]", "", "Phát Pháp Bảo theo phẩm giai hoặc ID", Material.DIAMOND_CHESTPLATE),
                new CommandEntry("ttadmin give dan", "/ttadmin give dan <player> <tên_đan> [số]", "", "Phát đan dược tu tiên cho người chơi", Material.POTION),
                new CommandEntry("ttadmin give bidong_sach", "/ttadmin give bidong_sach <player> <hệ>", "", "Phát sách bí kíp bị động (atk, hp, def...)", Material.ENCHANTED_BOOK),
                new CommandEntry("ttadmin give dotpha_token", "/ttadmin give dotpha_token <player> <tier> [số]", "", "Phát lệnh bài / token bảo hộ đột phá", Material.NAME_TAG),
                new CommandEntry("ttadmin give thao", "/ttadmin give thao <player> <id> [số]", "", "Phát tiên thảo thu hái cho người chơi", Material.WHEAT),
                new CommandEntry("ttadmin give seed", "/ttadmin give seed <player> <id> [số]", "", "Phát hạt giống linh chủng cho người chơi", Material.WHEAT_SEEDS),
                new CommandEntry("ttadmin give lk", "/ttadmin give lk <player> <số_lượng>", "", "Cộng thêm Linh Khí cho người chơi", Material.EXPERIENCE_BOTTLE),
                new CommandEntry("ttadmin give tien", "/ttadmin give tien <player> <số_lượng>", "", "Cộng thêm Linh Thạch cho người chơi", Material.GOLD_INGOT),
                new CommandEntry("ttadmin maxlk", "/ttadmin maxlk [player]", "", "Làm đầy 100% Linh Khí ngay lập tức", Material.BEACON),
                new CommandEntry("ttadmin maxlk_all", "/ttadmin maxlk_all", "", "Làm đầy 100% Linh Khí cho toàn server online", Material.BEACON),
                new CommandEntry("ttadmin heal", "/ttadmin heal <player>", "", "Hồi đầy máu và đầy Linh Khí cho người chơi", Material.GOLDEN_APPLE),
                new CommandEntry("ttadmin dotpha", "/ttadmin dotpha <player>", "", "Ép buộc người chơi đột phá thành công ngay", Material.LIGHTNING_ROD),
                new CommandEntry("ttadmin reset", "/ttadmin reset <player> confirm", "", "Reset sạch toàn bộ dữ liệu nhân vật về Luyện Khí", Material.LAVA_BUCKET),
                new CommandEntry("ttadmin zone", "/ttadmin zone <create|list|remove>", "", "Tạo hoặc xóa vùng Linh Mạch tăng tốc tu vi", Material.BEACON),
                new CommandEntry("ttadmin arena", "/ttadmin arena <create|list|remove>", "", "Quản lý đấu trường độ kiếp thiên kiếp", Material.IRON_BARS),
                new CommandEntry("ttadmin vuon", "/ttadmin vuon <tao|list|xoa>", "", "Quản lý khu vườn tiên thảo server", Material.FLOWER_POT),
                new CommandEntry("ttadmin bloodmoon", "/ttadmin bloodmoon <start|stop|status>", "/tta bm", "Bật, tắt hoặc xem trạng thái Huyết Nguyệt", Material.REDSTONE_BLOCK),
                new CommandEntry("ttadmin worldboss", "/ttadmin worldboss <spawn|despawn|status>", "/tta wb", "Spawn hoặc despawn Boss Thế Giới", Material.DRAGON_EGG),
                new CommandEntry("ttadmin season", "/ttadmin season <info|set|clear>", "", "Quản lý mùa giải và sự kiện tu tiên", Material.CLOCK),
                new CommandEntry("ttadmin broadcast", "/ttadmin broadcast <tin_nhắn>", "/tta bc", "Thông báo thiên hạ toàn server với hiệu ứng", Material.BELL),
                new CommandEntry("ttadmin reload", "/ttadmin reload", "", "Reload toàn bộ config và dữ liệu plugin", Material.REDSTONE_TORCH),
                new CommandEntry("debug", "/debug [on|off]", "/ttdebug", "Bật hoặc tắt chế độ debug ghi log chi tiết", Material.LEVER)
        );
    }

    // ============================================================
    // EVENT HANDLERS
    // ============================================================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuideHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || !holder.viewer.equals(player.getUniqueId())) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        Runnable action = holder.actions.get(rawSlot);
        if (action != null) {
            action.run();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuideHolder) {
            event.setCancelled(true);
        }
    }

    // ============================================================
    // TIỆN ÍCH
    // ============================================================

    private void runCommand(Player player, String cmd) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        player.performCommand(cmd);
    }

    private void suggestCommand(Player player, String cmd) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        player.sendMessage(plugin.prefix() + "§7Lệnh gợi ý: §e" + cmd);
        player.sendMessage("§8(Hãy mở thanh chat 'T' để chỉnh sửa hoặc gõ lệnh)");
    }

    private ItemStack createPlayerProfile(Player player, PlayerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("§8─────────────────────────");
        if (data != null && data.hasClass()) {
            lore.add("§7Cảnh Giới: " + data.getStage().getFullDisplay(data.getRank()));
            lore.add("§7Môn Phái: " + data.getClassType().getColor() + data.getClassType().getDisplayName());
            lore.add("§7Đạo Hướng: " + (data.hasDaoPath() ? data.getDaoPath().getDisplayName() : "§8Chưa chọn"));
            lore.add("§7Chủng Tộc: " + (data.hasToc() ? data.getToc().getColoredName() : "§8Chưa mở"));
            lore.add("§7Linh Khí: §e" + formatNum(data.getLinhKhi()) + " §7/ §e" + formatNum(data.getMaxLinhKhi()));
            lore.add("§7Linh Thạch: §e" + formatNum(data.getTien()));
        } else {
            lore.add("§cChưa nhập đạo! Gõ §e/tutien chon §cđể bắt đầu.");
        }
        lore.add("§8─────────────────────────");
        lore.add("§eNhấp vào các mục bên dưới để đọc cẩm nang!");

        return makeItem(Material.PLAYER_HEAD, "§e§lĐẠO HỮU: §f§l" + player.getName(), lore);
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§0", List.of());
        ItemStack corner = makeItem(Material.CYAN_STAINED_GLASS_PANE, "§0", List.of());

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(45, corner);
        inv.setItem(53, corner);
    }

    private void addBackButton(Inventory inv, GuideHolder holder, Runnable onBack) {
        inv.setItem(45, makeItem(Material.ARROW, "§a§l◀ QUAY LẠI MENU CHÍNH", List.of("§7Nhấp để trở về trang chủ")));
        holder.actions.put(45, () -> {
            Player p = Bukkit.getPlayer(holder.viewer);
            if (p != null) onBack.run();
        });

        inv.setItem(49, makeItem(Material.BARRIER, "§c§lĐÓNG CẨM NANG", List.of("§7Nhấp để đóng")));
        holder.actions.put(49, () -> {
            Player p = Bukkit.getPlayer(holder.viewer);
            if (p != null) p.closeInventory();
        });
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String formatNum(long num) {
        if (num >= 1_000_000_000_000L) return String.format("%.2fT", num / 1_000_000_000_000.0);
        if (num >= 1_000_000_000L) return String.format("%.2fB", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format("%.2fM", num / 1_000_000.0);
        if (num >= 1_000L) return String.format("%.2fK", num / 1_000.0);
        return String.valueOf(num);
    }
}
