# Tuỳ biến cảnh giới — TuTien Free

File cấu hình được tự tạo sau khi bật plugin lần đầu:

`plugins/TuTienPlugin/custom-canh-gioi.yml`

## Có thể chỉnh gì?

- `name`: tên hiển thị của cảnh giới.
- `color`: màu/tô đậm tên, dùng mã Minecraft như `&b`, `&6&l`.
- `base-linh-khi`: lượng Linh Khí cơ bản cần cho một lần đột phá ở slot đó.
- `max-rank`: số tầng nhỏ trong cảnh giới, từ 1 đến 6.
- `realm`: tên nhóm cảnh giới hiển thị (ví dụ `Linh Giới`, `Tiên Giới`).
- `realm-color`: màu của tên nhóm cảnh giới.
- `breakthrough-difficulty`: tuỳ chọn, xác suất nền/độ khó trong khoảng lớn hơn 0 đến 1; nếu bỏ dòng sẽ dùng công thức hiện tại.
- `can-fly`, `can-use-all-skills`: tuỳ chọn bật/tắt bay và quyền dùng toàn bộ kỹ năng ở slot đó; nếu bỏ dòng sẽ dùng quy tắc mặc định.

Sửa file rồi chạy `/ttadmin reload` (hoặc khởi động lại server). Nếu thay `max-rank`, người chơi đang online sẽ được đưa về tầng hợp lệ; Linh Khí hiện có cũng được giới hạn theo ngưỡng mới.

## Giới hạn hiện tại

Bản Free hiện lưu cảnh giới bằng `CultivationStage` enum và nhiều hệ thống dùng ID/ordinal này để xác định đột phá, thiên kiếp, kỹ năng, bay, boss và placeholder. Vì vậy file này **tuỳ biến 25 slot có sẵn**, chưa thể thêm ID/cảnh giới mới hoàn toàn chỉ bằng YAML. Hãy giữ nguyên tên ID như `LUYEN_KHI`, `TRUC_CO`, `HON_NGUYEN`...; chỉ chỉnh giá trị bên trong từng slot. Đổi tên nhóm `realm` có thể làm các hiệu ứng/hook vốn dựa trên tên nhóm mặc định rơi về xử lý dự phòng.
