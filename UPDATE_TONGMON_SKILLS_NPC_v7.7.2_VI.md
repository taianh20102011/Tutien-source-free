# TuTienPlugin v7.7.2 — Hoàn thiện Tông Môn, Skill & Đệ Tử NPC

Bản cập nhật source cho cả FREE và PREMIUM, dựa trên v7.7.1. Bản này tập trung đóng các khoảng trống trong lớp bảo vệ lãnh địa và thực thi thỏa thuận liên minh.

## 1. Lãnh địa Tông Môn — lớp bảo vệ sự kiện thực tế

- Thêm listener bảo vệ phá/đặt block trong vùng thuê.
- Chặn tương tác với container/redstone của người ngoài; thành viên đồng minh chỉ được đi qua cửa/cổng thông thường khi hai bên cùng bật quyền vào lãnh địa.
- Chặn người không có quyền bước vào vùng; khi thỏa thuận bị thu hồi lúc người chơi đang ở trong vùng, vẫn cho phép họ di chuyển ra ngoài nhưng khóa các thao tác xây dựng/tương tác.
- Bảo vệ bucket, dòng chất lỏng qua ranh giới, lửa, Enderman/entity đổi block, phá hoại từ nổ và piston tác động tới vùng bảo vệ.
- Native provider vẫn được hỗ trợ qua cấu hình `land.claim-provider: AUTO|WORLDGUARD|GRIEFPREVENTION|TUTIEN`.
- Cập nhật danh sách member theo Tông Môn cho WorldGuard region; GriefPrevention claim được sở hữu bởi Môn Chủ và trust thành viên.

## 2. Thực thi thỏa thuận PvP liên minh

- Khi hai Tông Môn là đồng minh, sát thương PvP giữa thành viên chỉ được cho phép khi cả hai bên cùng bật đồng thuận PvP.
- Kiểm tra cả đòn đánh trực tiếp và projectile do người chơi bắn.
- Khi một bên tắt PvP, sát thương giữa thành viên hai bên bị hủy; thông báo được giới hạn tần suất để tránh spam chat.

## 3. Các hệ thống giữ nguyên từ v7.7.1

- Công Pháp tùy chỉnh qua `congphap.yml`, gồm mẫu Huyết Ngục Ấn.
- Skill mới `chienkhiquyet`, `phatthuan`, `thienmatrao`; cấp vật phẩm skill bằng `/tutien layky <skill-id>` khi đủ điều kiện.
- Citizens NPC follower trong `dedoan-npc.yml`, dữ liệu Thể Chất/Linh Căn/Tư Chất riêng và RandomSkip riêng mỗi NPC.
- Các file riêng `dao.yml`, `phabao.yml`, `hubs.yml`, `tongmon-config.yml`.
- Liên minh 2 phía, quyền vào lãnh địa và PvP cần đồng thuận từ cả hai Tông Môn.

## 4. Lưu ý tích hợp

- WorldGuard/GriefPrevention/Citizens được gọi qua hook tùy chọn và một số lời gọi được dò bằng reflection để tránh hard dependency lúc khởi động.
- Native claim API phải được thử bằng đúng phiên bản plugin đang dùng. Nếu API không tương thích, việc thuê/gia hạn được từ chối thay vì âm thầm tạo claim không an toàn.
- Piston chạm vào vùng được bảo vệ bị chặn vì sự kiện piston không có actor rõ ràng đáng tin để phân biệt người kích hoạt; điều này có thể hạn chế piston tự động trong khu vực thuê.

## 5. Trạng thái kiểm thử

Đã kiểm tra tĩnh 103 file Java mỗi edition, 13 YAML mỗi edition, `pom.xml`, cấu hình, đăng ký lệnh, handler bảo vệ lãnh địa và đường dẫn enforcement PvP liên minh. Máy đóng gói không có Maven và không có dependency Paper/WorldGuard/GriefPrevention/Citizens trong cache; do đó chưa xác nhận Maven compile hoặc runtime server. Hãy test trên server staging trước production.
