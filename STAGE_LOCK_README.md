# KHOA CANH GIOI THAT - LINH CHUNG

## Cơ chế
Nút Mua trong DeluxeMenus gọi:

  /linhchungshop buy <herbId> <amount>

Nhưng lệnh này kiểm tra ở SERVER trước khi trừ Linh Thạch:

  PlayerData.getStage().ordinal() >= HerbType.minStageOrd()

Nếu chưa đủ:
  - Không trừ tiền
  - Không tạo Linh Chủng
  - Không nhận item

Do đó không thể bypass bằng cách bấm GUI, chạy command console, hoặc gửi packet giả.

## Giá
Giá lấy trực tiếp từ:
  herbs.types.<herbId>.lt-value

Không hard-code giá trong command.

## Bán
Bán kiểm tra PDC:
  linh_chung_id

Không dùng material vanilla để nhận diện seed, nên không thể lấy WHEAT_SEEDS thường để bán giả.

## Cài
1. Build source này bằng Maven trên máy có JDK/Maven.
2. Thay JAR TuTienPlugin cũ bằng JAR mới.
3. Chép các file DeluxeMenus vào:
   plugins/DeluxeMenus/
4. Đăng ký menu theo CONFIG_FRAGMENT.yml nếu config.yml chưa có.
5. Restart server (khuyến nghị) hoặc reload plugin.

## Kiểm thử
Ví dụ một herb yêu cầu cảnh giới 10:

- Người chơi cảnh giới 9 -> Mua -> BỊ CHẶN.
- Người chơi cảnh giới 10 -> Mua -> được phép nếu đủ Linh Thạch.
- Chưa đủ tiền -> bị chặn.
- Túi đầy -> không mất tiền.
- Giá đổi trong config -> command dùng giá mới sau khi plugin reload.

## Lưu ý
Không nên chỉ dùng DeluxeMenus requirement để bảo vệ kinh tế. Requirement GUI chỉ là lớp hiển thị.
Lớp bảo vệ thật nằm trong LinhChungShopCommand.
