# TuTienPlugin v7.7.2 — FREE (Free Perpetual)

TuTienPlugin là plugin RPG/tu tiên cho Paper 1.21.1+ và Java 21+. Source này đã cập nhật Tông Môn 2.0, công pháp YAML tùy chỉnh, kỹ năng, địa bàn claim/region và NPC đệ tử dùng Citizens.

## Bắt đầu nhanh

1. Sao lưu server và thư mục `plugins/TuTienPlugin/` trước khi cập nhật.
2. Cài JDK 21 và Maven.
3. Chạy `mvn clean package -Pfree` tại thư mục chứa `pom.xml`.
4. Đưa `target/TuTienPlugin-free-7.7.2.jar` vào server Paper thử nghiệm trước khi dùng production.
5. Khởi động server, kiểm tra console và cấu hình các file YAML mới.

## Tính năng trong source v7.7.2

- Tông Môn: cấp Hộ Pháp Trưởng Lão, chiêu mộ và xét duyệt, liên minh hai phía, thỏa thuận PvP/quyền vào lãnh địa, được enforcement ở event thực tế.
- Thuê địa bàn: listener bảo vệ phá/đặt/tương tác, bucket, fluid, fire, nổ và piston; hook tạo WorldGuard region hoặc GriefPrevention claim theo `land.claim-provider`.
- Công pháp: cấu hình kỹ thuật ở `congphap.yml`, hỗ trợ các active type đã liệt kê trong hướng dẫn.
- Kỹ năng mới: `chienkhiquyet`, `phatthuan`, `thienmatrao`; có thể lấy skill item bằng `/tutien layky <skill-id>` khi đủ điều kiện.
- Đệ Tử NPC: Citizens follower, dữ liệu riêng, RandomSkip cooldown theo NPC và placeholder thống kê.
- Cấu hình riêng: `dao.yml`, `phabao.yml`, `hubs.yml`, `congphap.yml`, `tongmon-config.yml`.

## Hook và claim providers

- WorldGuard: tạo/xóa cuboid region và đồng bộ member.
- GriefPrevention: tạo/xóa claim sở hữu bởi Môn Chủ, trust thành viên.
- Citizens: NPC follower; cần cài phiên bản Citizens tương thích.
- Các hook là tùy chọn; chỉ dùng tính năng cần plugin phụ thuộc tương ứng.

Đặt `land.claim-provider: AUTO` để ưu tiên WorldGuard, sau đó GriefPrevention, rồi lớp bảo vệ riêng TuTien. Nếu API provider không thể kiểm tra an toàn, thao tác thuê sẽ bị từ chối để tránh ghi đè claim.

## Tài liệu

- `UPDATE_TONGMON_SKILLS_NPC_v7.7.2_VI.md` — lệnh, cấu hình và giới hạn.
- `CHANGELOG_TONGMON_SKILLS_NPC_v7.7.2_VI.md` — changelog.
- `TEST_RESULTS_TuTien_v7.7.2_FEATURES.md` — kết quả kiểm tra tĩnh.
- `HUONG_DAN_BETTERHUD_PLACEHOLDERAPI.md` — HUD và placeholder.

## Trạng thái kiểm thử

Đã chạy kiểm tra tĩnh: 103 file Java/edition được quét lexical/delimiter, 13 YAML/edition parse được, `pom.xml` hợp lệ và các điểm nối chức năng được kiểm tra. **Maven build và test runtime trên Paper chưa chạy trong môi trường đóng gói**, do thiếu Maven và dependency cache. Vì vậy đây là source đã kiểm tra tĩnh, chưa phải JAR đã biên dịch hoặc chứng nhận runtime.

## Edition

Bản FREE dùng vĩnh viễn. Các cơ chế liên kết host/license vẫn theo chính sách License Control.


## Bản vá v7.7.2

Xem `CHANGELOG_TONGMON_SKILLS_NPC_v7.7.2_VI.md` và `UPDATE_TONGMON_SKILLS_NPC_v7.7.2_VI.md`.
