package com.tutien.skills;

import com.tutien.data.ClassType;
import com.tutien.data.CultivationStage;

import java.util.*;

/**
 * ComboSkillRegistry - 16 skills per class, grouped into 4 combo slots.
 *
 * Each combo slot holds 4 sub-skills. Player cycles through sub-skills with Shift+Q.
 * Sub-skills unlock when player reaches the required cultivation stage.
 *
 * Unlock tiers:
 *   Slot A (sub 1-4): Luyen Khi, Truc Co, Ket Dan, Nguyen Anh
 *   Slot B (sub 5-8): Hoa Than, Luyen Hu, Hop The, Dai Thua
 *   Slot C (sub 9-12): Chan Tien, Kim Tien, Thai At, Dai La
 *   Slot D (sub 13-16): Dao To, De Tu, De Vuong, Thanh Gioi
 */
public class ComboSkillRegistry {

    public record SkillEntry(String id, String displayName, int minStageOrdinal) {}

    // Map classType -> List of 16 skill entries (4 per slot)
    private static final Map<ClassType, List<SkillEntry>> REGISTRY = new EnumMap<>(ClassType.class);

    static {
        // ==================== KIEM TU ====================
        REGISTRY.put(ClassType.KIEM_TU, List.of(
            // Slot A (0-3)
            new SkillEntry("kiemkhitram",     "Kiếm Khí Trảm",        0),  // Luyen Khi
            new SkillEntry("tockiem",          "Tốc Kiếm",             1),  // Truc Co
            new SkillEntry("thiensat",         "Thiên Sát",            2),  // Ket Dan
            new SkillEntry("kiemkhiphan",      "Kiếm Khí Phản",        3),  // Nguyen Anh
            // Slot B (4-7)
            new SkillEntry("bat_quan_kiem",    "Bát Quái Kiếm",        4),  // Hoa Than
            new SkillEntry("kiem_cuong_ba",    "Kiếm Cuồng Ba",        5),  // Luyen Hu
            new SkillEntry("thien_kiem_trui",  "Thiên Kiếm Trụy",      6),  // Hop The
            new SkillEntry("van_kiem_tran",    "Vạn Kiếm Trận",        7),  // Dai Thua
            // Slot C (8-11)
            new SkillEntry("nhat_kiem_sat",    "Nhất Kiếm Sát",        8),  // Chan Tien
            new SkillEntry("kiem_tien_phap",   "Kiếm Tiền Pháp",       9),  // Kim Tien
            new SkillEntry("thien_long_kiem",  "Thiên Long Kiếm",      10), // Thai At
            new SkillEntry("kiem_dao_vo_thuong","Kiếm Đạo Vô Thượng",  11), // Dai La
            // Slot D (12-15)
            new SkillEntry("vankiemxuyentam",  "Vạn Kiếm Xuyên Tâm",  12), // Dao To [UNIQUE]
            new SkillEntry("kiem_thoat_the",   "Kiếm Thoát Thể",       13), // De Tu
            new SkillEntry("nguyen_shi_kiem",  "Nguyên Thủy Kiếm",     14), // De Vuong
            new SkillEntry("kiem_he_pho_tien", "Kiếm Hệ Phổ Thiên",    15)  // Thanh Gioi
        ));

        // ==================== PHAP TU ====================
        REGISTRY.put(ClassType.PHAP_TU, List.of(
            new SkillEntry("linhhoacau",       "Linh Hỏa Cầu",         0),
            new SkillEntry("dongbang",         "Đóng Băng",            1),
            new SkillEntry("phaptran",         "Pháp Trận",            2),
            new SkillEntry("loi_hoa_phap",     "Lôi Hỏa Pháp",        3),
            new SkillEntry("thuy_long_phap",   "Thủy Long Pháp",       4),
            new SkillEntry("ngu_hanh_tran",    "Ngũ Hành Trận",        5),
            new SkillEntry("thien_di_phap",    "Thiên Địa Pháp",       6),
            new SkillEntry("linh_vu_bao_phong","Linh Vũ Bão Phong",    7),
            new SkillEntry("thaicuchuanphap",  "Thái Cực Huyền Pháp",  8),
            new SkillEntry("cuong_loi_phap",   "Cuồng Lôi Pháp",       9),
            new SkillEntry("phap_tac_vu",      "Pháp Tắc Vũ",          10),
            new SkillEntry("thien_mang_phap",  "Thiên Mang Pháp",      11),
            new SkillEntry("linh_vuc_khai_mo", "Linh Vực Khai Mở",     12),
            new SkillEntry("phap_tu_thoai_thu","Pháp Tổ Thoái Thủ",    13),
            new SkillEntry("ngu_sac_shen_lei", "Ngũ Sắc Thần Lôi",    14),
            new SkillEntry("van_phap_qui_tong","Vạn Pháp Quy Tông",    15)
        ));

        // ==================== Y TU ====================
        REGISTRY.put(ClassType.Y_TU, List.of(
            new SkillEntry("linhduoc",         "Linh Dược",            0),
            new SkillEntry("phuhotran",        "Phù Hộ Trận",          1),
            new SkillEntry("giaido",           "Giải Độc",             2),
            new SkillEntry("hoi_sinh_phap",    "Hồi Sinh Pháp",        3),
            new SkillEntry("cam_lo_vu",        "Cam Lộ Vũ",            4),
            new SkillEntry("linh_dan_bao_tri", "Linh Đan Bảo Trị",     5),
            new SkillEntry("phuc_hoa_lan",     "Phục Hoa Lan",         6),
            new SkillEntry("san_sinh_chi_thu", "San Sinh Chi Thuật",   7),
            new SkillEntry("hoiduongcuunghich","Hồi Dương Cứu Nghịch", 8),
            new SkillEntry("thien_y_shen_shou","Thiên Y Thần Thủ",     9),
            new SkillEntry("linh_y_bao_than",  "Linh Y Bảo Thân",      10),
            new SkillEntry("van_duoc_gui_yuan","Vạn Dược Quy Nguyên",  11),
            new SkillEntry("y_dao_shen_thu",   "Y Đạo Thần Thuật",     12),
            new SkillEntry("hua_tuo_zhi_shu",  "Hoa Đà Chi Thuật",     13),
            new SkillEntry("linh_y_zhi_jing",  "Linh Y Chi Cảnh",      14),
            new SkillEntry("bao_shen_shen_yi", "Bảo Thần Thần Y",      15)
        ));

        // ==================== DOC TU ====================
        REGISTRY.put(ClassType.DOC_TU, List.of(
            new SkillEntry("manhdoctan",       "Mãnh Độc Tán",         0),
            new SkillEntry("amsatkiem",        "Ám Sát Kiếm",          1),
            new SkillEntry("doctran",          "Độc Trận",             2),
            new SkillEntry("doc_vu_phong",     "Độc Vũ Phong",         3),
            new SkillEntry("thanh_doc_bao",    "Thanh Độc Bạo",        4),
            new SkillEntry("van_doc_chi_the",  "Vạn Độc Chi Thể",      5),
            new SkillEntry("am_sat_doc_tran",  "Ám Sát Độc Trận",      6),
            new SkillEntry("doc_lung_bao_ba",  "Độc Long Bạo Phát",    7),
            new SkillEntry("vandocchithe",     "Vạn Độc Chi Thể+",     8),
            new SkillEntry("doc_shen_fa",      "Độc Thần Pháp",        9),
            new SkillEntry("shi_ling_du_gong", "Thế Linh Độc Công",    10),
            new SkillEntry("doc_zhu_shen_zhen","Độc Chú Thần Trận",    11),
            new SkillEntry("du_jing_wan_xiang","Độc Kinh Vạn Tượng",   12),
            new SkillEntry("tian_du_zhi_ti",   "Thiên Độc Chi Thể",    13),
            new SkillEntry("wan_du_gui_yi",    "Vạn Độc Quy Yi",       14),
            new SkillEntry("du_zun_wu_shang",  "Độc Tôn Vô Thượng",    15)
        ));

        // ==================== THE TU ====================
        REGISTRY.put(ClassType.THE_TU, List.of(
            new SkillEntry("thietbich",        "Thiết Bích",           0),
            new SkillEntry("hophaptran",       "Hộ Pháp Trận",         1),
            new SkillEntry("phandon",          "Phản Đòn",             2),
            new SkillEntry("gang_ti_shen_ti",  "Cương Thể Thần Thể",  3),
            new SkillEntry("kim_zhong_zhao",   "Kim Chung Triệu",      4),
            new SkillEntry("kimcuongbathoai",  "Kim Cương Bất Hoại",   5),
            new SkillEntry("gang_shan_ya",     "Cương Sơn Áp",         6),
            new SkillEntry("tie_bi_jin_zhong", "Thiết Bị Kim Chung",   7),
            new SkillEntry("bu_huai_jin_shen", "Bất Hoại Kim Thân",    8),
            new SkillEntry("tian_gang_ti",     "Thiên Cương Thể",      9),
            new SkillEntry("bu_mie_jin_gang",  "Bất Diệt Kim Cương",   10),
            new SkillEntry("wan_lu_gui_zong",  "Vạn Lực Quy Tông",    11),
            new SkillEntry("tian_ti_jing",     "Thiên Thể Cảnh",       12),
            new SkillEntry("jin_ti_wu_shang",  "Kim Thể Vô Thượng",    13),
            new SkillEntry("bu_huai_wang_shen","Bất Hoại Vương Thần",  14),
            new SkillEntry("wu_shang_jin_ti",  "Vô Thượng Kim Thể",    15)
        ));

        // ==================== QUY TU ====================
        REGISTRY.put(ClassType.QUY_TU, List.of(
            new SkillEntry("quyanhtan",        "Quỷ Ảnh Tàng",         0),
            new SkillEntry("hukhongdoat",      "Hư Không Đoạt",        1),
            new SkillEntry("tuanh",            "Tử Ảnh",               2),
            new SkillEntry("quy_hun_fen_shen", "Quỷ Hồn Phân Thân",   3),
            new SkillEntry("si_wang_zhu_zhou", "Tử Vong Chú Nguyền",   4),
            new SkillEntry("yin_yue_sha_qi",   "Âm Nguyệt Sát Khí",   5),
            new SkillEntry("ling_hun_huo",     "Linh Hồn Hỏa",         6),
            new SkillEntry("gui_wang_xia_ling","Quỷ Vương Hạ Lệnh",    7),
            new SkillEntry("quyvuonggianthe",  "Quỷ Vương Giáng Thế",  8),
            new SkillEntry("gui_di_zhi_yan",   "Quỷ Địa Chi Nhãn",     9),
            new SkillEntry("si_wang_zhi_yuan", "Tử Vong Chi Nguyên",   10),
            new SkillEntry("ling_yu_mo_zhen",  "Linh Vực Ma Trận",     11),
            new SkillEntry("gui_zun_zhi_fa",   "Quỷ Tôn Chi Pháp",     12),
            new SkillEntry("wan_gui_chao_zong","Vạn Quỷ Triều Tông",   13),
            new SkillEntry("gui_di_po_jun",    "Quỷ Đế Phá Quân",      14),
            new SkillEntry("gui_zu_wu_shang",  "Quỷ Tổ Vô Thượng",     15)
        ));

        // ==================== PHONG TU ====================
        REGISTRY.put(ClassType.PHONG_TU, List.of(
            new SkillEntry("phongtocthiet",    "Phong Tốc Đột Kích",   0),
            new SkillEntry("toanphongne",      "Toàn Phong Né",        1),
            new SkillEntry("phongnhan",        "Phong Nhẫn",           2),
            new SkillEntry("phong_liu_hua",    "Phong Lưu Hoa",        3),
            new SkillEntry("cuongphongtran",   "Cuồng Phong Trận",     4),
            new SkillEntry("phakhongthuandi",  "Phá Không Thuấn Di",   5),
            new SkillEntry("bao_feng_shan",    "Bão Phong Trảm",       6),
            new SkillEntry("feng_shen_bu",     "Phong Thần Bộ",        7),
            new SkillEntry("tinh_phong_jie",   "Tinh Phong Kiếm",      8),
            new SkillEntry("tian_feng_zhan",   "Thiên Phong Trảm",     9),
            new SkillEntry("feng_shen_biao",   "Phong Thần Tiêu",      10),
            new SkillEntry("kuang_feng_jie",   "Cuồng Phong Kiếm",     11),
            new SkillEntry("feng_zu_zhi_bu",   "Phong Tổ Chi Bộ",      12),
            new SkillEntry("tian_feng_yi",     "Thiên Phong Dực",      13),
            new SkillEntry("jue_feng_shen_su", "Tuyệt Phong Thần Tốc", 14),
            new SkillEntry("wu_ji_feng_shen",  "Vô Cực Phong Thần",    15)
        ));

        // ==================== HOA TU ====================
        REGISTRY.put(ClassType.HOA_TU, List.of(
            new SkillEntry("nghiephoathieu",   "Nghiệp Hỏa Thiêu",     0),
            new SkillEntry("hoalong",          "Hỏa Long",             1),
            new SkillEntry("phonghoa",         "Phong Hỏa Trận",       2),
            new SkillEntry("hoa_phen_ba",      "Hỏa Phẫn Bạo",        3),
            new SkillEntry("phuonghoangtrognsinh","Phượng Hoàng Trọng Sinh",4),
            new SkillEntry("huo_shen_jiang",   "Hỏa Thần Giáng",       5),
            new SkillEntry("yan_long_jue",     "Viêm Long Quyết",      6),
            new SkillEntry("di_huo_fen_bao",   "Địa Hỏa Phẫn Bạo",    7),
            new SkillEntry("tian_huo_jing",    "Thiên Hỏa Cảnh",       8),
            new SkillEntry("huo_zu_zhi_yan",   "Hỏa Tổ Chi Viêm",      9),
            new SkillEntry("wan_huo_gui_zong", "Vạn Hỏa Quy Tông",    10),
            new SkillEntry("huo_di_li",        "Hỏa Đế Lệ",           11),
            new SkillEntry("tuyetdiahoahai",   "Tuyệt Địa Hỏa Hải",   12),
            new SkillEntry("huo_huang_lin",    "Hỏa Hoàng Lâm",        13),
            new SkillEntry("tian_yan_jue",     "Thiên Viêm Quyết",     14),
            new SkillEntry("wu_shang_zhen_huo","Vô Thượng Chân Hỏa",   15)
        ));
    }

    public static List<SkillEntry> getSkillsForClass(ClassType ct) {
        return REGISTRY.getOrDefault(ct, Collections.emptyList());
    }

    /** 4 combo slots, each with 4 sub-skills */
    public static List<List<SkillEntry>> getComboSlots(ClassType ct) {
        List<SkillEntry> all = getSkillsForClass(ct);
        List<List<SkillEntry>> slots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int from = i * 4, to = Math.min(from + 4, all.size());
            slots.add(all.subList(from, to));
        }
        return slots;
    }

    /** Get unlocked sub-skills for a combo slot given current stage */
    public static List<SkillEntry> getUnlockedSubSkills(ClassType ct, int slotIndex, CultivationStage stage) {
        List<List<SkillEntry>> slots = getComboSlots(ct);
        if (slotIndex >= slots.size()) return Collections.emptyList();
        int stageOrd = stage.ordinal();
        List<SkillEntry> unlocked = new ArrayList<>();
        for (SkillEntry e : slots.get(slotIndex)) {
            if (stageOrd >= e.minStageOrdinal()) unlocked.add(e);
        }
        return unlocked;
    }

    /** Min stage display string for a skill entry */
    public static String getUnlockStageDisplay(SkillEntry e) {
        CultivationStage[] stages = CultivationStage.values();
        if (e.minStageOrdinal() < stages.length)
            return stages[e.minStageOrdinal()].getDisplayName();
        return "Cảnh Giới Cấp " + e.minStageOrdinal();
    }
}
