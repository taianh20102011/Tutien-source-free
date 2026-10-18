# TuTien Plugin — bản sửa v7.7.0

## Thành phần được bổ sung/sửa

- Chuẩn hóa version Maven và `plugin.yml` về `${project.version}`.
- Thêm chế độ build `dev`, `free` và `premium`.
- Thêm preflight license cho bản phát hành, cache offline có thời hạn và kiểm tra license định kỳ.
- Thêm License Control web app dùng Tailwind CSS, API quản lý license, test tự động và desktop shell Electron tùy chọn.
- Mỗi IP public chỉ được bind một license FREE; do Free không hết hạn, cần admin chủ động unbind mới giải phóng IP. Binding còn khóa theo installation UUID. License Premium được khóa theo IP và UUID.
- Free dùng vĩnh viễn, không hết hạn; các key Free đã phát hành theo chính sách 365 ngày được chuyển sang vĩnh viễn khi nâng cấp License Control. Premium hỗ trợ 30/90/180/365/730 ngày hoặc lifetime.
- Bổ sung bộ chỉnh mapping skin skill ItemsAdder, kiểm tra định dạng ID trước khi xuất YAML.
- Sửa tương thích kiểu kết quả spawn MythicMobs, thêm xử lý fallback reflection, cập nhật kiểm tra hook theo config.
- Bổ sung kiểm tra bật/tắt skill và rank, command `/debug`, xử lý tham số vùng không hợp lệ.
- Chuyển autosave dữ liệu Bukkit/HashMap sang lịch chạy main-thread để tránh đọc/ghi đồng thời với luồng gameplay.

## Build plugin

Cần JDK 21 và Maven 3.9+; máy build cần truy cập được repository Maven bên ngoài.

```bash
# Bản dev để thử nghiệm, không bắt buộc license API
mvn clean package

# Bản phát hành Free (license bắt buộc)
mvn clean package -Pfree

# Bản phát hành Premium (license Premium bắt buộc)
mvn clean package -Ppremium

# Hoặc dùng script để build cả hai bản release (yêu cầu Maven):
# Linux/macOS: ./build-releases.sh
# Windows PowerShell: .\build-releases.ps1
```

Mỗi lệnh `mvn clean` xóa `target/`, nên nếu tự build thủ công hãy sao chép JAR Free ra chỗ khác trước khi chạy lệnh Premium. Script trên tự lưu cả hai JAR vào `dist/`:

- `TuTienPlugin-dev-7.7.0.jar` (chỉ khi build profile mặc định)
- `dist/TuTienPlugin-free-7.7.0.jar`
- `dist/TuTienPlugin-premium-7.7.0.jar`

Bản Free/Premium cần một license API đang hoạt động. Điền key và URL công khai của License Control trong `plugins/TuTienPlugin/config.yml`:

```yaml
licensing:
  api-url: "https://licenses.example.com"
  key: "TTF-..."
  offline-grace-hours: 72
```

Đổi `TTF-...` thành key thực được cấp. Không dùng URL localhost nếu Minecraft server chạy trên máy khác License Control.

## Chạy License Control (web)

Yêu cầu Node.js 20+; server API chỉ dùng thư viện tích hợp sẵn của Node.

```bash
cd license-platform
npm start
```

Mặc định dịch vụ chỉ bind `127.0.0.1:8787`; dashboard mở tại `http://127.0.0.1:8787`. Lần chạy đầu tạo admin token ngẫu nhiên và lưu tại `license-platform/data/admin-token.txt`; token cũng được in ra console. Bảo quản token như mật khẩu quản trị. Đừng commit thư mục `data/` hoặc chia sẻ token.

Để license API hoạt động với host game ở xa, triển khai dịch vụ trên máy chủ có thể truy cập từ host game và đặt HTTPS reverse proxy (ví dụ Nginx/Caddy). Giữ Node bind ở localhost phía sau reverse proxy, chỉ bật `TRUST_PROXY=true` khi proxy được cấu hình ghi đè `X-Forwarded-For`; không mở trực tiếp API HTTP ra Internet.

Các biến môi trường được mô tả trong `license-platform/.env.example`. Ví dụ chạy cho môi trường triển khai:

```bash
HOST=127.0.0.1 PORT=8787 node server.js
```

Dữ liệu license lưu trong `license-platform/data/licenses.json`; sao lưu và khóa quyền đọc file này. Server lưu hash SHA-256 và hint của key, không lưu key gốc; key gốc chỉ xuất hiện một lần ngay khi tạo.

## License & giới hạn IP

- FREE key do admin tạo, hiệu lực vĩnh viễn và không có ngày hết hạn.
- PREMIUM key có hạn theo gói hoặc lifetime. Chức năng tùy biến VFX/skill skin ItemsAdder được mở cho Premium có license còn hợp lệ.
- API tự quan sát IP kết nối tới nó, không tin giá trị IP do plugin gửi trong JSON; `instanceId` xác định installation.
- Nhiều server dùng chung NAT/egress IP sẽ cùng bị áp dụng giới hạn một license FREE duy nhất theo IP cho IP đó, trừ khi admin chủ động unbind. Chuyển host/IP cần admin dùng `Unbind` sau khi xác minh yêu cầu; Free vẫn vĩnh viễn và Premium giữ nguyên thời hạn còn lại.
- Khóa IP không phải DRM tuyệt đối: IP động, proxy và bản plugin đã bị chỉnh sửa có thể làm suy yếu kiểm soát. Giữ license API riêng tư, ký/gói artifact phát hành từ CI và không phát hành source của production build nếu mô hình kinh doanh yêu cầu.
- Hiện tại API phát hành license thủ công bởi admin; phần thanh toán tự động/gateway chưa được tích hợp.

## Chỉnh skin skill

Trong License Control mở **Skill skins**, nhập ID skill đúng với ID thực tế trong mã plugin (ví dụ `tockiem`, `kiemkhitram`) và Custom Entity ID dạng `namespace:entity_id`, sau đó xuất YAML. Merge phần `effects.itemsadder.map` vào `plugins/TuTienPlugin/config.yml`, ví dụ:

```yaml
effects:
  itemsadder:
    enabled: true
    map:
      skill-tockiem: "tutien:vfx_toc_kiem"
      skill-kiemkhitram: "tutien:vfx_kiem_khi_tram"
```

Các entity phải tồn tại và được ItemsAdder đăng ký trên server. Nếu mapping trống/sai, plugin quay về hiệu ứng particle mặc định. Sau khi cập nhật config dùng `/iareload` nếu cần và khởi động lại/reload plugin theo quy trình an toàn của server.

Dashboard hiện dùng Tailwind CDN để thuận tiện thử nghiệm, nên máy mở dashboard cần Internet. Trước khi chạy production, nên build/bundle Tailwind CSS thành file tĩnh local và gỡ CDN để tránh phụ thuộc mạng/CDN trong trang quản trị có token nhạy cảm.

## Kiểm thử và giới hạn xác minh

```bash
npm test --prefix license-platform
```

Bộ API có 7 integration test, bao gồm Free vĩnh viễn và migration key Free cũ. Java plugin cần build và integration test trên server Paper 1.21.1 với đúng các phiên bản plugin hook. Trong môi trường tạo bản vá này chưa có Maven và không thể truy cập repository Maven, nên chưa thể xác nhận hoàn tất một lệnh build JAR hoặc chạy test Minecraft thực tế. Không xem zip nguồn là JAR phát hành đã build.
