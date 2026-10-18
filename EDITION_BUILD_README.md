# TuTienPlugin v7.7.1 — FREE — dùng vĩnh viễn

## Build

Yêu cầu JDK 21 và Maven. Từ thư mục gốc chứa `pom.xml`:

- Windows PowerShell: `./build-releases.ps1`
- Linux/macOS shell: `./build-releases.sh`
- Build trực tiếp: `mvn clean package -Pfree`

JAR đầu ra dự kiến sau khi build thành công: `target/TuTienPlugin-free-7.7.1.jar`. Script build-release sẽ copy JAR vào `dist/`.

## Nội dung chính

- Tông Môn 2.0: chiêu mộ, xét duyệt, cấp bậc, liên minh hai phía, PvP/liên thông lãnh địa.
- Thuê địa bàn và provider WorldGuard/GriefPrevention/TuTien.
- Công pháp tùy chỉnh `congphap.yml` và Huyết Ngục Ấn mẫu.
- Kỹ năng `chienkhiquyet`, `phatthuan`, `thienmatrao` và lệnh `/tutien layky <skill-id>`.
- Đệ Tử NPC follower bằng Citizens, RandomSkip và chỉ số riêng.
- `dao.yml`, `phabao.yml`, `hubs.yml`, `tongmon-config.yml`.

## Trước khi production

- Sao lưu dữ liệu.
- Thử trên Paper 1.21.1 + Java 21 staging.
- Nếu dùng native claim, thử với đúng phiên bản WorldGuard/GriefPrevention đang cài. Không cài đồng thời hai provider cho cùng claim; `AUTO` sẽ chọn một provider.
- NPC follower cần Citizens; các attribute có thể tùy entity/provider.
- Đọc `UPDATE_TONGMON_SKILLS_NPC_v7.7.1_VI.md` và `TEST_RESULTS_TuTien_v7.7.1_FEATURES.md`.

## Giới hạn xác minh

Kiểm tra tĩnh đã đạt, nhưng môi trường đóng gói không có Maven/dependency cache; Maven compile và runtime test trên Paper chưa được xác nhận. Gói là mã nguồn, không chứa JAR phát hành đã build.
