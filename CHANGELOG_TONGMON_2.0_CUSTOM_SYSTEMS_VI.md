# TuTienPlugin v7.7.1 — Changelog

## Tông Môn 2.0
- Bổ sung Hộ Pháp Trưởng Lão trong hệ thống cấp bậc.
- Bổ sung thông báo chiêu mộ có thao tác gửi đơn nhanh và luồng duyệt/từ chối đơn.
- Bổ sung GUI liên minh và đồng thuận hai phía đối với PvP/liên thông lãnh địa.
- Dọn liên minh và đơn đang chờ khi Tông Môn giải thể.
- Bổ sung thuê/gia hạn địa bàn cuboid theo phí và thời hạn cấu hình.
- Hook native region WorldGuard và native claim GriefPrevention; lựa chọn provider bằng `land.claim-provider`.
- Kiểm tra chồng lấn với vùng ngoài; bỏ qua `__global__` của WorldGuard; từ chối thao tác khi provider không thể kiểm tra an toàn.
- Đồng bộ quyền thành viên vào region/claim; cleanup claim trước khi giải thể/hết hạn.

## Skill & Công Pháp
- Thêm skill Chiến Khí Quyết, Phật Thủ Ấn và Thiên Ma Trảo.
- Thêm `/tutien layky <skill-id>` để cấp vật phẩm skill riêng theo class/cảnh giới/rank.
- Cải thiện Phật Thủ Ấn với pha lao xuống và timeout để tránh trạng thái kẹt trên không.
- Hoàn thiện combo Huyết Ngục Ấn: đặt dấu, kích nổ ×1.8 hoặc ×2.8 trên mục tiêu do người thi triển đánh dấu.

## Đệ Tử NPC
- Thêm Citizens NPC follower theo chủ nhân.
- Lưu riêng cảnh giới/rank, Thể Chất, Linh Căn, Tư Chất, owner và cooldown RandomSkip mỗi NPC.
- Thêm lệnh chỉnh chỉ số riêng, xóa NPC theo ownership và tab completion.
- Áp dụng thuộc tính Bukkit cho NPC khi Citizens entity hỗ trợ, cập nhật tên hiển thị theo cảnh giới.
- Bổ sung placeholder cho số lượng và thuộc tính từng NPC.

## Tương thích & cấu hình
- Thêm GriefPrevention vào danh sách soft-dependency.
- Bổ sung/hợp nhất config `congphap.yml`, `tongmon-config.yml`, `dao.yml`, `phabao.yml`, `hubs.yml`.
- Cùng một bộ gameplay source được đồng bộ giữa FREE và PREMIUM; edition/license profiles vẫn riêng biệt.
