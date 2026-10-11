package com.tutien.phabao;

import org.bukkit.Material;

/**
 * 50 Pháp Bảo - mỗi loại có kỹ năng tích cực riêng (Shift + Chuột Phải).
 * Nhóm:
 *   VU_KHI    - tấn công trực tiếp
 *   PHONG_THU - giảm sát thương, hộ thân
 *   HO_TRO    - hồi máu, buff đồng đội
 *   DIEU_KHIEN- khống chế kẻ thù
 *   DE_PHAM+  - kỹ năng mạnh đặc biệt cấp cao
 */
public enum PhaBaoType {

    // ===== VU KHI (16) =====
    LINH_KIEM      ("Linh Kiếm",       "\u00a7c", Material.DIAMOND_SWORD,
                    "Kiếm linh khí: ATK cao nhất vũ khí phổ thông",
                    "kiemkhi",      "Phóng kiếm khí trước mặt, gây sát thương AoE"),

    THIEN_TRUONG   ("Thiên Trượng",    "\u00a79", Material.BLAZE_ROD,
                    "Trượng phép thuật: tăng phép ATK",
                    "phapvubao",    "Phóng 5 phép vũ tự động truy đuổi kẻ thù"),

    AM_THU         ("Ám Thủ",          "\u00a75", Material.BOW,
                    "Nỏ linh: xuyên giáp, tấn công từ xa",
                    "xuyenam",      "Tên linh xuyên qua mọi vật cản"),

    HONG_LONG_GIAN ("Hồng Long Giản",  "\u00a76", Material.IRON_AXE,
                    "Giản rồng lửa: AoE lửa phá lớn",
                    "hoalongtrang", "Phả độc lửa xuyên đất gây sát thương lớn"),

    QUY_DAN        ("Quỷ Đạn",         "\u00a75", Material.PHANTOM_MEMBRANE,
                    "Đạn ma: xuất hiện sau lưng kẻ thù",
                    "quydanphong",  "Dịch chuyển sau lưng kẻ thù gần nhất, gây sát thương x2"),

    PHONG_LEU_KIEM ("Phong Liễu Kiếm", "\u00a7b", Material.STONE_SWORD,
                    "Kiếm gió: gây sát thương kết hợp đẩy lùi",
                    "phongleukhi",  "Kiếm khí gió đẩy lùi kẻ thù 5m và gây sát thương"),

    HOA_LINH_CU    ("Hỏa Linh Cầu",    "\u00a76", Material.FIRE_CHARGE,
                    "Cầu lửa: phóng cầu lửa mạnh nhất",
                    "hoalinhcu",    "Ném cầu lửa lực mạnh, phát nổ AoE 4m khi trúng địch"),

    THIEN_LOI_CHAN ("Thiên Lôi Chấn",  "\u00a7e", Material.LIGHTNING_ROD,
                    "Chấn sấm: gọi sét từ trời xuống",
                    "thienloigiangthe", "Gọi 5 tia sét đánh ngẫu nhiên kẻ xung quanh 8m"),

    BANG_PHONG_BI  ("Băng Phong Bi",   "\u00a7b", Material.PACKED_ICE,
                    "Bi băng: đóng băng kẻ thù AoE",
                    "bangha",       "Đóng băng hoàn toàn tất cả kẻ xung quanh 5m trong 3 giây"),

    HON_PHACH_DANG ("Hồn Phách Đăng",  "\u00a75", Material.SOUL_LANTERN,
                    "Đăng hồn phách: hút linh lực kẻ chết",
                    "cuophon",      "Hút linh lực kẻ thù, hồi phục máu và nhận thêm LK"),

    THAM_THIEN_KHANH("Tham Thiền Khánh","\u00a7d",Material.BELL,
                    "Khánh linh: sóng âm làm choáng váng",
                    "ambacuong",    "Sóng âm làm choáng váng kẻ xung quanh 5m"),

    THACH_THAN_GIA ("Thạch Thần Gia",  "\u00a77", Material.IRON_PICKAXE,
                    "Gia thạch thần: vũ khí đất đá nặng, khống chế rất mạnh",
                    "thachthan",    "Ném đá lớn, nhốt cả kẻ thù 3 giây"),

    LOI_PHU        ("Lôi Phù",         "\u00a7e", Material.FIREWORK_ROCKET,
                    "Phù sét: đặt bẫy độc sét, kẻ đi qua bị sét đánh",
                    "loiphu",       "Đặt bẫy sét ở chân, kẻ bước vào bị sét đánh trong 8 giây"),

    AM_NGUYET_CUNG ("Ám Nguyệt Cung",  "\u00a75", Material.CROSSBOW,
                    "Cung nguyệt: tên ám tốc độ khủng, xuyên qua 3 kẻ",
                    "amnguyet",     "Phóng tên ám xuyên qua tối đa 3 kẻ thù"),

    CUONG_PHONG_PHIET("Cuồng Phong Phiệt","\u00a7b",Material.IRON_SWORD,
                    "Phiệt gió: chém dài xa, gây sát thương đối mọi kẻ trong đường thẳng",
                    "cuongphongphiet","Chém gió theo đường thẳng 10m, gây sát thương mọi kẻ trúng phải"),

    THUY_LONG_TRAM ("Thủy Long Trâm",  "\u00a79", Material.TRIDENT,
                    "Trâm nước: xuất hiện con rồng nước tấn công",
                    "thuylong",     "Xuất hiện rồng nước tấn công kẻ thù trong 6 giây"),

    // ===== PHONG THU (10) =====
    LINH_KHIEN     ("Linh Khiên",      "\u00a7b", Material.SHIELD,
                    "Khiên linh: giảm sát thương, hộ thân",
                    "hothecuong",   "Tỏa hào quang giảm 50% sát thương trong 4 giây"),

    NGOC_PHAI      ("Ngọc Bài",        "\u00a7e", Material.NETHER_STAR,
                    "Ngọc bài hộ mệnh: tăng sức mạnh, teleport",
                    "thoatxac",     "Teleport tức thì đến vị trí nhìn trong 8m"),

    BACH_NGOC_PHAP_THAN("Bạch Ngọc Pháp Thân","\u00a7f",Material.DIAMOND,
                    "Pháp thân bạch ngọc: khiên tuyệt đối 3 giây",
                    "bachngoc",     "Bất tử hoàn toàn trong 3 giây, không nhận bất kỳ sát thương"),

    THIET_BI_CUONG ("Thiết Bí Cường",  "\u00a77", Material.IRON_CHESTPLATE,
                    "Bí cường sắt: DEF tăng đột biến khi kích hoạt",
                    "thietbicuong", "Giảm 70% sát thương nhận vào trong 6 giây"),

    PHAN_HON_KINH  ("Phản Hồn Kính",   "\u00a7c", Material.TOTEM_OF_UNDYING,
                    "Kính phản hồn: tự động bất tử khi HP về 0 một lần",
                    "phantu",       "Kích hoạt trước: khi HP về 0 tự động hồi phục 60% HP"),

    VAN_LINH_GIAP  ("Vạn Linh Giáp",   "\u00a76", Material.GOLDEN_CHESTPLATE,
                    "Giáp vạn linh: phản đòn sát thương khi bị đánh",
                    "vanlinh",      "Trong 5 giây: tự động phản 30% sát thương nhận vào"),

    BAO_THAN_THANH ("Bảo Thân Thánh",  "\u00a7f", Material.AMETHYST_SHARD,
                    "Thánh bảo thân: tạo khiên thánh tinh bao bọc",
                    "baothanthanh", "Tạo khiên sáng tự tỏa cao trong 5 giây giảm 60% sát thương"),

    THAM_TAM_KINH  ("Thám Tâm Kính",   "\u00a7d", Material.WRITTEN_BOOK,
                    "Kính thám tâm: minh mẫn trí tuệ, tránh bị khống chế",
                    "thamtam",      "Miễn dịch mọi hiệu ứng xấu trong 6 giây"),

    TU_HAI_LINH_KHU("Tứ Hải Linh Khu", "\u00a79", Material.WATER_BUCKET,
                    "Linh khu nước: tạo lớp nước giảm hiệu ứng lửa và sát thương",
                    "linh_khu",     "Tạo linh khu nước xung quanh 3m: giảm sát thương 40% trong 5 giây"),

    THAN_LINH_PHU  ("Thần Linh Phù",   "\u00a7a", Material.MAP,
                    "Phù thần linh: tăng sức đề kháng toàn bộ hiệu ứng xấu",
                    "thanlinh",     "Xóa tất cả hiệu ứng xấu và miễn dịch 8 giây"),

    // ===== HO TRO (12) =====
    HOI_SINH_DAN   ("Hồi Sinh Đan",    "\u00a7a", Material.GOLDEN_APPLE,
                    "Đan hồi sinh: hồi phục HP tức thì",
                    "hoiphuc",      "Hồi phục 30% max HP ngay lập tức"),

    TUYET_HOA_KINH ("Tuyệt Hoa Kinh",  "\u00a7f", Material.ENCHANTED_BOOK,
                    "Kinh tuyệt hoa: tăng tu luyện tốc độ",
                    "traudoi",      "Nhận ngay 10% LK tối đa"),

    VAN_PHU_DAN    ("Vạn Phù Đan",     "\u00a7e", Material.HONEYCOMB,
                    "Đan vạn phù: buff toàn đội đồng thời",
                    "vanphu",       "Buff STR + REGEN + SPEED cho toàn đội 6m trong 8 giây"),

    CAM_LO_BONG    ("Cam Lộ Bồng",     "\u00a7a", Material.HONEY_BOTTLE,
                    "Bồng cam lộ: hồi phục mặc dù đang chiến đấu",
                    "camlobong",    "Hồi phục 5% max HP mỗi 2 giây trong 10 giây (tổng 25%)"),

    PHUC_HOA_PHAP  ("Phục Hỏa Pháp",   "\u00a7a", Material.FERN,
                    "Pháp phục hỏa: hồi phục HP đồng đội xung quanh",
                    "phuchoa",      "Hồi phục 20% max HP cho toàn bộ đồng đội trong 8m"),

    HOP_THE_DAN    ("Hợp Thể Đan",     "\u00a7c", Material.MAGMA_CREAM,
                    "Đan hợp thể: tăng sức mạnh + tốc độ nhanh",
                    "hopthedan",    "Buff STR III + SPEED II + HASTE II trong 10 giây"),

    THIEN_DI_DAN   ("Thiên Di Đan",    "\u00a7b", Material.ENDER_PEARL,
                    "Đan thiên di: dịch chuyển tức thì đến vị trí nhìn",
                    "thiendi",      "Teleport chính xác đến vị trí đang nhìn (max 30m)"),

    ANH_THE_TAN    ("Ảnh Thế Tán",     "\u00a76", Material.SPIDER_EYE,
                    "Tán ảnh thế: mở rộng mắt đồng đội, tăng tầm nhìn",
                    "anhtetan",     "Cấp buff Night Vision + Saturation cho đồng đội 10m trong 60 giây"),

    HUE_NHAN_KHANH ("Huệ Nhãn Khánh",  "\u00a7d", Material.AMETHYST_BLOCK,
                    "Khánh huệ nhãn: khai sáng trí tuệ để tránh bẫy",
                    "huenhan",      "Hiển thị vị trí của tất cả kẻ thù trong 20m trong 10 giây"),

    HON_PHACH_HAP  ("Hồn Phách Hấp",   "\u00a7d", Material.SOUL_SAND,
                    "Hấp hồn phách: thu hút linh khí từ mọi nơi",
                    "honphachap",   "Thu hút LK từ bất kỳ mob nào trong 8m, nhận thêm 5000 LK"),

    LINH_TICH_TAN  ("Linh Tích Tán",   "\u00a77", Material.GLOWSTONE_DUST,
                    "Tán linh tích: gia tăng tốc độ tích lũy linh khí",
                    "linhtichtan",  "Trong 30 giây: mọi LK nhận được nhân x2"),

    PHAP_LUC_THUNG ("Pháp Lực Thùng",  "\u00a7e", Material.GOLD_INGOT,
                    "Thùng pháp lực: đổi Linh Thạch lấy LK tức thì",
                    "phaplucthung", "Đổi 1000 Linh Thạch lấy 50,000 LK ngay lập tức"),

    // ===== DIEU KHIEN (8) =====
    TRUONG_PHONG   ("Trường Phong",    "\u00a7b", Material.FEATHER,
                    "Phong trường: gió mạnh đẩy lùi tất cả kẻ thù",
                    "truongphong",  "Đẩy lùi tất cả kẻ thù trong 6m lên cao và ra xa"),

    HOA_HOI_TRAN   ("Hỏa Hồi Trận",    "\u00a76", Material.BLAZE_POWDER,
                    "Trận hỏa hồi: đặt trận lửa AoE kéo dài",
                    "hoahoitran",   "Đặt trận lửa 5m, đốt cháy kẻ thù trong 8 giây"),

    BANG_PHONG_TRAN("Băng Phong Trận", "\u00a7b", Material.ICE,
                    "Trận băng phong: tạo vùng đóng băng kéo dài",
                    "bangphangtran","Tạo vùng băng 4m, đóng cứng kẻ thù trong 5 giây"),

    DOC_MAI_TRAN   ("Độc Mai Trận",    "\u00a72", Material.FERMENTED_SPIDER_EYE,
                    "Trận độc mai: đặt bẫy bước độc chuyên nghiệp",
                    "docmaitran",   "Đặt bẫy bước độc: kẻ đi qua bị Poison IV trong 6 giây"),

    DIEN_TU_CUONG  ("Điện Từ Cường",   "\u00a7e", Material.COPPER_INGOT,
                    "Cường điện từ: tạo trường điện từ làm chậm kẻ thù",
                    "diencuong",    "Tạo trường điện 5m, làm chậm mọi kẻ trong 4 giây"),

    THIEN_MO_TRAN  ("Thiên Mộ Trận",   "\u00a74", Material.OBSIDIAN,
                    "Trận thiên mộ: tạo vùng ma tối áp chế phép thuật",
                    "thienmotran",  "Trận 6m: kẻ thù mất 50% phép ATK trong 8 giây"),

    MAI_PHUC_TRUNG ("Mai Phục Trùng",  "\u00a75", Material.STRING,
                    "Trùng mai phục: đặt bẫy vô hình, kẻ dẫm vào bị kéo xuống",
                    "maiphuc",      "Đặt bẫy vô hình: kẻ bước vào bị kéo thẳng xuống và bay lên"),

    PHONG_TRAN_TINH("Phong Trần Tinh", "\u00a7b", Material.RABBIT_FOOT,
                    "Tinh phong trần: phong ba cuồng phong cuốn kẻ thù lên cao",
                    "phongtrantinh","Phong ba cuốn tất cả kẻ trong 5m lên cao 5m rồi thả rơi"),

    // ===== CAP CAO DE/TIEN PHAM (4) =====
    CHIEU_THIEN_KINH("Chiêu Thiên Kinh","\u00a7c\u00a7l",Material.ENCHANTED_BOOK,
                    "[ĐẾ PHẨM] Kinh chiêu thiên: hướng dẫn bất tử tạm thời",
                    "chieuthien",   "Bất tử hoàn toàn 5 giây + hồi phục toàn bộ HP sau khi hết"),

    VAN_HOA_BOT    ("Vạn Hóa Bột",     "\u00a7c\u00a7l",Material.NETHERITE_INGOT,
                    "[ĐẾ PHẨM] Bột vạn hóa: AoE tối thượng, phá vỡ tất cả khiên",
                    "vanhoa",       "AoE 8m sát thương cực lớn, xuyên qua mọi DEF và khiên"),

    THANH_LINH_PHONG("Thanh Linh Phong","\u00a7b\u00a7l",Material.NETHER_STAR,
                    "[TIÊN PHẨM] Phong thanh linh: gọi gió thanh tịnh tiếp lực",
                    "thanhlinh",    "Kêu gọi gió thanh tịnh: heal toàn đội + push kẻ thù 10m"),

    HONG_MONG_PHU  ("Hồng Mông Phù",   "\u00a76\u00a7l",Material.DRAGON_EGG,
                    "[TIÊN PHẨM] Phù hồng mông: hủy diệt tuyệt đối, nứt phép tắc",
                    "hongmong",     "[ULTIMATE] Bùng nổ AoE 15m, gây sát thương khủng, reset mọi CD"),

    // ══════════════════════════════════════════════════════
    //  30 PHÁP BẢO MỚI — HỆ THỐNG MỞ RỘNG
    // ══════════════════════════════════════════════════════

    // ───── VU KHI MO RONG (10) ─────
    THIEN_PHONG_KIEM("Thiên Phong Kiếm","\u00a7b",Material.IRON_SWORD,
                    "Kiếm thiên phong: cắt gió chớp, sát thương gió cực cao",
                    "thienphong",   "Phóng 3 luồng gió sắc bén chém 10m, kẻ trúng bị tung lên"),

    HAI_VUONG_TRAM ("Hải Vương Trâm",  "\u00a79",Material.PRISMARINE_SHARD,
                    "Trâm hải vương: vũ khí nước xuyên giáp vượt trội",
                    "haivuong",     "Phóng kim trâm nước xuyên giáp, gây sát thương x1.5 kẻ mặc giáp"),

    HOA_PHUNG_QUA  ("Hỏa Phụng Quạt", "\u00a7c",Material.FEATHER,
                    "Quạt phượng: AoE lửa phát nổ dạng quạt rồng",
                    "hoaphungqua",  "Mở quạt lửa 180 độ trước mặt, đốt cháy và đẩy lùi tất cả"),

    BACH_HO_QUYEN  ("Bạch Hổ Quyền",  "\u00a7f",Material.GOLDEN_AXE,
                    "Quyền bạch hổ: cuồng lực như hổ, sát thương tay không",
                    "bachho",       "Phát quyền AoE 3m trước mặt, gây sát thương x3 và giảm giáp"),

    TAM_TAI_KIEM   ("Tam Tài Kiếm",   "\u00a7d",Material.NETHERITE_SWORD,
                    "Kiếm tam tài: hòa hợp Kim-Mộc-Thủy, ba nguyên tố kết hợp",
                    "tamtai",       "Đánh 3 đòn liên tiếp: lóa lửa + băng + sét, mỗi đòn mang 1 hiệu ứng"),

    LONG_VUONG_CHAU("Long Vương Châu", "\u00a76",Material.HEART_OF_THE_SEA,
                    "Châu long vương: ngọc rồng nước, sức mạnh cường hóa",
                    "longvuongchau","Kêu gọi rồng nước tấn công tự động 8 giây, gây sát thương lớn"),

    PHUONG_HOANG_VU("Phượng Hoàng Vũ", "\u00a7c",Material.BLAZE_POWDER,
                    "Vũ phượng hoàng: lông vũ lửa, hồi sinh từ cái chết",
                    "phuonghoangvu","Khi HP về 0 trong 10 giây tiếp: hồi sinh với 50% HP và bọc lửa AoE"),

    TU_TUONG_AN    ("Tứ Tượng Ấn",    "\u00a7d",Material.CLOCK,
                    "Ấn tứ tượng: phong tỏa không-thời gian, làm slow kẻ thù",
                    "tutuongan",    "Phong tỏa vùng 5m: kẻ thù bị Slowness IV và mining fatigue 5 giây"),

    BAT_QUAT_TRAN  ("Bát Quái Trận",  "\u00a76",Material.COMPASS,
                    "Trận bát quái: bát quái xoay, tạo tướng mây chớp lửa",
                    "batquattran",  "Tạo vòng xoay bát quái: chớp lửa xuất hiện ngẫu nhiên trong 6 giây"),

    HON_DON_CAU    ("Hỗn Độn Cầu",    "\u00a75",Material.PURPLE_DYE,
                    "Cầu hỗn độn: nguyên khí nguyên thủy phá vỡ mọi phòng thủ",
                    "hondon",       "[ĐẾ PHẨM] Bùng nổ nguyên khí: xuyên qua mọi DEF, giảm 80% mọi khiên"),

    // ───── PHONG THU MO RONG (8) ─────
    HUYEN_VU_THUAN ("Huyền Vũ Thuẫn", "\u00a78",Material.TURTLE_HELMET,
                    "Thuẫn huyền vũ: giáp rùa thần, cứng như đá",
                    "huyenvu",      "Bất tử 4 giây + DEF +100% sau khi kết thúc trong 8 giây"),

    CUU_LONG_GIAP  ("Cửu Long Giáp",  "\u00a76",Material.DIAMOND_CHESTPLATE,
                    "Giáp cửu long: chịu được mọi nguyên tố, giáp bảo vệ tuyệt vời",
                    "cuulong",      "Miễn dịch hiệu ứng xấu 8 giây + DEF +50% toàn cầu"),

    NGOC_HU_BINH   ("Ngọc Hư Bình",  "\u00a7b",Material.GLASS_BOTTLE,
                    "Bình ngọc hư: phong tỏa kẻ thù trong bình, ngăn di chuyển",
                    "ngochubinh",   "Phong tỏa kẻ thù gần nhất trong bình ảo 4 giây: không thể di chuyển"),

    THAI_AT_PHU    ("Thái Ất Phù",    "\u00a76",Material.PAPER,
                    "Phù thái ất: phù thần thiêng bảo vệ chủ nhân",
                    "thaiatphu",    "Kích hoạt: +5 max HP vĩnh viễn cho bản thân (tối đa 10 lần)"),

    AM_DUONG_KINH  ("Âm Dương Kính", "\u00a7d",Material.ENDER_EYE,
                    "Kính âm dương: gương phản lại sát thương phép thuật",
                    "amduongkinhphan","Phản lại 50% sát thương phép thuật nhận vào trong 6 giây"),

    CAN_KHON_DAI   ("Càn Khôn Đai",  "\u00a7e",Material.GOLDEN_BOOTS,
                    "Đai càn khôn: đai vũ trụ, di chuyển siêu nhanh và teleport",
                    "cankhon",      "Teleport tức thì + Speed IV 5 giây + không nhận damage di chuyển"),

    KIM_THIEN_GIA  ("Kim Thiên Giáp", "\u00a7e",Material.GOLDEN_CHESTPLATE,
                    "Giáp kim thiên: giáp vàng thiên thông, phản thù mọi damage",
                    "kimthien",     "Giảm 60% toàn bộ damage + phản lại 20% cho kẻ tấn công trong 5 giây"),

    PHU_TEN_DU     ("Phù Tên Du",    "\u00a77",Material.PHANTOM_MEMBRANE,
                    "Du phong thanh: bao phong thanh, teleport thoát nguy hiểm",
                    "phuteng",      "Di chuyển xuyên tường 3m + phòng thủ tuyệt đối 2 giây"),

    // ───── DIEU KHIEN MO RONG (6) ─────
    VAN_HOA_TAP    ("Vạn Hoa Tập",   "\u00a7a",Material.BAMBOO,
                    "Tập vạn hoa: trói buộc kẻ thù bằng dây linh khí",
                    "vanhoatap",    "Trói buộc kẻ thù gần nhất: bị treo lên cao 3m trong 4 giây"),

    THIET_HOA_LIEN ("Thiết Hoa Liên","\u00a7c",Material.CHAIN,
                    "Liên thiết hoa: xích sắt liên kết kẻ thù, ảnh hưởng đồng loạt",
                    "thiethoa",     "Kết nối 3 kẻ thù gần nhất: mọi damage 1 kẻ truyền 50% cho 2 kẻ kia"),

    HOA_GIAM       ("Hoa Giam",      "\u00a7a",Material.VINE,
                    "Giam hoa: bắt kẻ thù bằng cây leo ma thuật",
                    "hoagiam",      "Cây leo ma thuật trói buộc kẻ trong 4m: Slowness V + Jump penalty"),

    LINH_HON_SUY   ("Linh Hồn Suy",  "\u00a75",Material.SOUL_SAND,
                    "Suy linh hồn: hút linh lực kẻ thù giảm sát thương",
                    "linhohonsuy",  "Hút 20% sát thương + speed của kẻ thù trong 5 giây"),

    THIEN_MA_CHUNG ("Thiên Ma Chung","\u00a74",Material.BELL,
                    "Chuông thiên ma: âm thanh ma đạo làm kẻ thù hoảng loạn",
                    "thienmachung", "Kẻ trong 8m: bị Confusion + ngẫu nhiên tấn công nhau trong 4 giây"),

    HON_PHACH_TINH ("Hồn Phách Tinh","\u00a7d",Material.ECHO_SHARD,
                    "Tinh hồn phách: tinh thể linh hồn hút tinh lực kẻ chết",
                    "honphach",     "Mọi kẻ thù chết trong 8m: hồi phục 15% HP và +50 LK"),

    // ───── PHAP BAO BAN MENH (6) — Tier cao nhất ─────
    BAN_MENH_KIEM  ("Bản Mệnh Kiếm", "\u00a7c\u00a7l",Material.DIAMOND_SWORD,
                    "[BẢN MỆNH] Kiếm bản mệnh: không thể mất, tăng mạnh theo cảnh giới",
                    "banmenhkiem",  "[BẢN MỆNH] Mỗi đột phá: ATK +5% vĩnh viễn. Cảnh giới càng cao càng mạnh"),

    BAN_MENH_GIAP  ("Bản Mệnh Giáp", "\u00a77\u00a7l",Material.NETHERITE_CHESTPLATE,
                    "[BẢN MỆNH] Giáp bản mệnh: không thể mất, DEF tăng vĩnh viễn",
                    "banmenhgiap",  "[BẢN MỆNH] Mỗi đột phá: DEF +3% vĩnh viễn. Tự động sau khi bị sát"),

    BAN_MENH_PHONG ("Bản Mệnh Phong","\u00a7b\u00a7l",Material.ELYTRA,
                    "[BẢN MỆNH] Phong bản mệnh: cánh linh khí, bay tự do không cần rocket",
                    "banmenhphong", "[BẢN MỆNH] Bay tự do + giảm fall damage + Speed III thụ động"),

    BAN_MENH_NHAC  ("Bản Mệnh Nhạc", "\u00a7e\u00a7l",Material.NOTE_BLOCK,
                    "[BẢN MỆNH] Nhạc bản mệnh: nhạc khí hồi sinh mỗi lần chết",
                    "banmenhhoisnh","[BẢN MỆNH] Hồi sinh 1 lần/30 phút khi chết. HP full + immunity 3 giây"),

    BAN_MENH_AN    ("Bản Mệnh Ấn",   "\u00a7d\u00a7l",Material.AMETHYST_CLUSTER,
                    "[BẢN MỆNH] Ấn bản mệnh: ấn pháp bảo vượt trội, buff toàn diện",
                    "banmenthan",   "[BẢN MỆNH] Mỗi 5 phút: tự động hồi phục 10% HP + xóa 1 debuff"),

    BAN_MENH_CHAU  ("Bản Mệnh Châu", "\u00a76\u00a7l",Material.HEART_OF_THE_SEA,
                    "[BẢN MỆNH] Châu bản mệnh: ngọc linh khí, tăng LK tích lũy vĩnh viễn",
                    "banmenhchau",  "[BẢN MỆNH] LK regen +50% vĩnh viễn. Cảnh giới càng cao bonus càng lớn"),

    // ══════════════════════════════════════════════════════
    //  TRẤN THIÊN GIÁP — Passive giữ trong túi giảm 50% damage
    // ══════════════════════════════════════════════════════
    TRAN_THIEN_GIAP("Trấn Thiên Giáp","\u00a76\u00a7l",Material.NETHERITE_CHESTPLATE,
                    "[ĐẶC BIỆT] Giáp trấn thiên: giảm 50% sát thương khi giữ trong túi đồ",
                    "tranthiengiap","[PASSIVE] Chỉ cần trong túi -> giảm 50% mọi damage. Không cần mặc!");

    private final String displayName;
    private final String color;
    private final Material material;
    private final String description;
    private final String abilityId;
    private final String abilityDesc;

    PhaBaoType(String dn, String c, Material mat, String desc, String abilId, String abilDesc) {
        this.displayName = dn; this.color = c; this.material = mat;
        this.description = desc; this.abilityId = abilId; this.abilityDesc = abilDesc;
    }

    public String getDisplayName()  { return displayName; }
    public String getColor()        { return color; }
    public Material getMaterial()   { return material; }
    public String getDescription()  { return description; }
    public String getAbilityId()    { return abilityId; }
    public String getAbilityDesc()  { return abilityDesc; }
    public String getColoredName()  { return color + displayName; }
}
