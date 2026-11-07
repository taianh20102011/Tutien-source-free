# ⚔️ TuTienPlugin v7.7.4 — FREE (Free Perpetual)

> **Plugin RPG Tu Tiên Đỉnh Cao cho Minecraft Paper 1.21.1+ (Java 21+)**  
> Bản mã nguồn mở miễn phí vĩnh viễn — Tích hợp sẵn toàn bộ tính năng cốt lõi tu tiên chân thực.

---

## 📜 Giấy Phép Sử Dụng (License)

Dự án này được phát hành theo giấy phép **Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)**:

* ✅ **ĐƯỢC PHÉP:** Tải xuống mã nguồn, biên dịch, sử dụng, tự do chỉnh sửa (custom) tính năng theo ý muốn cho bản thân và bạn bè trải nghiệm trên máy chủ của bạn.
* ❌ **KHÔNG ĐƯỢC PHÉP (NON-COMMERCIAL):** Nghiêm cấm mọi hình thức thương mại hóa, mua bán lại mã nguồn/file JAR, hoặc phân phối lại nhằm mục đích thu lợi nhuận tài chính dưới bất kỳ hình thức nào.
* 📌 **Ghi nhận nguồn:** Giữ nguyên thông tin tác giả gốc (Hanita - Phạm Ngọc Tài Anh / TuTienPlugin).

Xem chi tiết điều khoản pháp lý tại tệp [LICENSE](LICENSE).

---

## 🌟 Điểm Mới Trong Phiên Bản v7.7.4

### 1. 🛡️ Bảo Vệ & Đồng Bộ Dữ Liệu Tu Sĩ Tuyệt Đối (Anti Data-Loss)
* **Khắc phục triệt để hiện tượng mất dữ liệu khi cài bản mới / reload:** Tự động đồng bộ toàn bộ người chơi đang trực tuyến ngay khi plugin nạp (`onEnable()`).
* **Tự động di trú dữ liệu cũ (Auto Migration):** Tự động phát hiện và chuyển đổi an toàn các tệp người chơi từ các thư mục cũ (`plugins/TuTien/players`, `plugins/TuTien_Plugin`) sang `plugins/TuTienPlugin/players`.
* **Cơ chế sao lưu dự phòng kép (.bak):** Tự động tạo bản sao lưu `.yml.bak` cho từng tu sĩ; nếu file chính bị mất điện đột ngột hoặc hỏng, hệ thống tự động khôi phục ngay lập tức.
* **Lưu trữ vĩnh viễn Túi Đồ Trữ Vật (`/tuitutien`, `/ttbag`):** 18 ô kho lưu trữ tự do nay đã được đồng bộ xuống file người chơi, giải quyết dứt điểm lỗi mất đồ khi khởi động lại server.
* **Bộ phân giải cảnh giới an toàn (Safe Stage Parsing):** Tương thích 100% với mọi định dạng tên cảnh giới (có dấu, không dấu, chữ thường, hệ cảnh giới cũ) mà không bao giờ bị tụt về Luyện Khí.

### 2. 🌌 Nâng Cấp Toàn Diện Hiệu Ứng Hình Ảnh & Âm Thanh (Visual & Audio Rework)
* **Sửa toàn bộ lỗi ngưỡng cảnh giới:** Đồng bộ chuẩn xác theo hệ 25 Đại Cảnh Giới, kích hoạt toàn bộ các hiệu ứng thần cấp trước đây bị khóa.
* **Aura Thiên Đạo Kim Quang (`/aura`):** Tu sĩ đạt Thiên Giới (Thiên Tôn, Thiên Đạo, Thiên Đế, Hỗn Nguyên) tự động mở khóa vòng hào quang kim quang xoay tròn đa tầng.
* **Cực Phẩm Vô Thượng Chí Tôn:** Hào quang tinh tú End Rod lượn sóng 3D kết hợp tia lửa pháo hoa rực rỡ quanh thân thể.
* **Thiên Kiếp Lôi Đình Đa Tầng:** Sóng xung kích lôi kiếp bùng nổ, pháo hoa 8 phương hướng bát quái, 3 lớp cầu hạt lộng lẫy và âm thanh chấn thiên động địa.

### 3. 📜 Hệ Thống Nhiệm Vụ Đột Phá Tích Hợp Sẵn (Built-in Breakthrough Quests)
* Không cần cài thêm bất kỳ plugin nhiệm vụ bên thứ 3 nào! Hệ thống tự động theo dõi 5 hoạt động tu tiên:
  1. ⚔️ **Săn Quái:** Tiêu diệt quái vật rèn luyện chiến lực.
  2. 🌿 **Thu Hái Linh Thảo:** Thu thập dược liệu dã ngoại.
  3. 🍶 **Luyện Chế Linh Đan:** Tự tay chế đan hộ mệnh phá cảnh.
  4. 🧘 **Thiền Định Nạp Khí:** Ngồi tĩnh tọa ngưng tụ linh khí đất trời.
  5. 👑 **Trảm Sát Boss:** Khiêu chiến Boss thế giới hoặc Wither.
* Xem trực quan tại **Ô 13 Túi Đồ Tu Tiên (`/tuitutien`)** hoặc gõ **`/quest`**.

### 4. ⚡ Kỹ Năng Phóng Thích Uy Áp Cảnh Giới (`/uyap`)
* Gõ `/uyap` để tức thời phóng thích uy áp trong bán kính 10m:
  * Âm thanh bộc phá cực mạnh (`Sound.ENTITY_WARDEN_SONIC_BOOM` & Sấm sét).
  * Luồng sóng xung kích siêu thanh SONIC_BOOM và bụi chấn 360 độ.
  * Hất tung toàn bộ kẻ địch lên không trung, áp các hiệu ứng Suy Yếu, Làm Chậm, Choáng Váng.

### 5. 🔥 Dị Hỏa & Thần Lôi Bản Mệnh (`/dihoa`, `/thanloi`)
* Thuần hóa các ngọn lửa thiên địa (Thanh Liên Địa Tâm Hỏa, Cốt Lãnh Du Hỏa...) hoặc Cửu Thiên Huyền Lôi.
* Kèm hiệu ứng hạt đổi màu đặc trưng và sát thương chuẩn thiêu đốt kinh mạch hoặc giật sét lan.

### 6. 🏡 Động Phủ Cá Nhân & Dược Viên Độc Quyền (`/dongphu`)
* Mở không gian Động Thiên Phúc Địa bỏ túi riêng biệt từ Kết Đan trở lên.
* Trồng Tiên Dược và tưới bằng Linh Tuyền rút ngắn thời gian thu hoạch.

---

## 🛠️ Hướng Dẫn Biên Dịch & Cài Đặt

### Yêu Cầu Môi Trường
* **Java Development Kit (JDK):** Phiên bản **Java 21** trở lên.
* **Apache Maven:** 3.8.0+.
* **Minecraft Server:** Paper / Purpur 1.21.1+.

### Các Bước Biên Dịch
```bash
# 1. Clone hoặc tải mã nguồn
git clone https://github.com/taianh20102011/Tutien-source-free.git
cd Tutien-source-free

# 2. Biên dịch gói JAR bằng Maven
mvn clean package -DskipTests
```

Sau khi build thành công, file JAR hoàn chỉnh sẽ nằm tại:
`target/TuTienPlugin-free-7.7.4.jar`

---

## 🎮 Danh Sách Lệnh Chính Cho Người Chơi

| Lệnh | Ý nghĩa |
| :--- | :--- |
| `/tutien` (`/tt`) | Menu chính Tu Tiên (chọn phái, đạo, bảng xếp hạng) |
| `/tuitutien` (`/ttbag`) | Mở Túi Đồ Trữ Vật 54 ô (có theo dõi Nhiệm Vụ Đột Phá tại ô 13) |
| `/quest` | Xem thanh tiến trình Nhiệm Vụ Đột Phá cảnh giới |
| `/uyap` | Phóng thích sóng xung kích Uy Áp cảnh giới |
| `/dihoa` / `/thanloi` | Quản lý Dị Hỏa Thiên Địa & Thần Lôi Bản Mệnh |
| `/dongphu` | Dịch chuyển vào Động Phủ & Dược Viên |
| `/thiendinh` | Ngồi tĩnh tọa thiền định hấp thu linh khí tự nhiên |
| `/vuottamma` | Khiêu chiến Tâm Ma trước khi độ kiếp (Bán Thánh+) |
| `/dotpha` | Bắt đầu Thiên Kiếp Lôi Đình để thăng cấp cảnh giới |
| `/luyen` | Mở lò luyện đan dược |
| `/cuonghoa` / `/renduc` | Nâng cấp và rèn đúc Pháp Bảo |
| `/tongmon` | Hệ thống Tông Môn / Bang hội tu tiên |
