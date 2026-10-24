# Bách Nghệ Tu Tiên & Cơ Duyên/Kim Thủ Chỉ — 7.7.3

## Bách Nghệ Tu Tiên (`bach-nghe-tu-tien.yml`)

- Nghề phụ: Luyện Đan Sư, Luyện Khí Sư, Trận Pháp Sư, Ngự Thú Sư, Linh Điền Sư và Phù Lục Sư.
- XP từ gameplay: hoàn thành mẻ thuốc, phù phép, đặt vật liệu trận pháp, thuần hóa thú, thu hoạch cây trưởng thành, chế tác sách/giấy.
- Cấp nghề tối đa, công thức XP, thời gian chống spam, thưởng Linh Khí và đặc quyền được cấu hình trong YAML.
- Dữ liệu nghề được lưu riêng trong `plugins/TuTienPlugin/data/bach-nghe-data.yml`.

## Cơ Duyên/Kim Thủ Chỉ (`co-duyen-kim-thu-chi.yml`)

- Nguồn cơ duyên: lần đầu tham gia, hạ quái, hạ boss, khai thác quặng hiếm và đột phá thành công.
- Bốn cơ duyên mẫu: Cổ Vật Bí Ẩn, Hệ Thống Tu Tiên, Đan Dược Kỳ Đặc, Lão Gia Gia Trong Nhẫn.
- Hệ thống có trọng số, xác suất theo trigger, cooldown toàn cục và giới hạn phát hiện mỗi người chơi.
- Vật phẩm phần thưởng có PersistentDataContainer tag riêng; không thể giả mạo chỉ bằng đổi tên vật phẩm.
- Dữ liệu phát hiện và điểm Hệ Thống được lưu riêng trong `plugins/TuTienPlugin/data/co-duyen-data.yml`.
- Người chơi dùng `/coduyen dung linhkhi <điểm>`, `/coduyen dung hoiphuc` hoặc `/coduyen dung cuonghoa` để sử dụng điểm Hệ Thống.

## Lệnh

- `/bachnghe list` — xem danh sách nghề.
- `/bachnghe info <id>` — xem mô tả và đặc quyền.
- `/bachnghe chon <id>` — chọn nghề; mặc định không thể tự đổi sau khi chọn.
- `/bachnghe status` — xem cấp và XP nghề.
- `/bachnghe reset <player>` — admin reset nghề (người chơi phải online).
- `/coduyen` — xem cơ duyên và điểm Hệ Thống.
- `/coduyen help` — xem cách sử dụng.
- `/ttadmin reload` — nạp lại hai file cấu hình.

## Cài đặt và lưu ý

1. Build từ source bằng Java 21 và Maven theo profile của edition (`mvn -Pfree clean package` hoặc `mvn -Ppremium clean package`).
2. Không dùng JAR cũ trong `target/`; các thay đổi Java cần được build thành JAR mới.
3. Mọi config được nạp lúc khởi động; có thể reload bằng `/ttadmin reload`.
4. Các tỷ lệ mặc định được cân bằng ở mức khởi đầu, nên kiểm thử trên server test trước khi dùng server chính thức.
