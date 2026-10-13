# Fix: TuTienPlugin disables when LuckPerms is missing

## Nguyên nhân
`LuckPermsHook` tham chiếu trực tiếp `net.luckperms.api.node.Node`, nhưng LuckPerms API có scope `provided` nên không được đóng gói trong JAR. `onEnable()` trước đây luôn khởi tạo hook, kể cả khi server không cài LuckPerms, dẫn đến `NoClassDefFoundError` và plugin bị tắt.

## Thay đổi
- Chỉ khởi tạo `LuckPermsHook` khi plugin LuckPerms đã được cài.
- Bắt `LinkageError`/`RuntimeException` cho tích hợp tùy chọn để lỗi API không làm hỏng toàn bộ TuTienPlugin.
- Thêm `LuckPerms` vào `softdepend` để Paper nạp LuckPerms trước và cho phép truy cập API khi có cài đặt.
- Các tính năng cốt lõi vẫn khởi động nếu LuckPerms không có; đồng bộ group/prefix/permission theo cảnh giới sẽ không hoạt động nếu thiếu LuckPerms.

## Cách dùng
Nếu cần đồng bộ quyền theo cảnh giới, cài LuckPerms tương thích với phiên bản server. Nếu không cần tính năng đó, bản đã sửa sẽ bỏ qua hook thay vì crash.

## Ghi chú khác trong log
`Vault is not installed` là cảnh báo riêng: tính năng kinh tế cần Vault cùng một plugin kinh tế tương thích. Nó không phải nguyên nhân của `NoClassDefFoundError` này.
