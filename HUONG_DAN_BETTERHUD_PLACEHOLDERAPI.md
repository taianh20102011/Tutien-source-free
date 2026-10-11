# Liên kết TuTienPlugin ↔ PlaceholderAPI ↔ BetterHud (bản BetterHud 2.0.0 Bukkit)

## 1. PlaceholderAPI — đã có sẵn, không cần sửa gì thêm

File `src/main/java/com/tutien/hooks/TuTienPlaceholderExpansion.java` đã đăng ký expansion
`tutien` và tự `.register()` trong `TuTienPlugin.onEnable()` nếu server có PlaceholderAPI.
`plugin.yml` cũng đã khai `PlaceholderAPI` trong `softdepend`.

Bỏ jar PlaceholderAPI + jar TuTienPlugin vào `plugins/`, khởi động server, thử:

```
/papi parse me %tutien_stage%
/papi parse me %tutien_lk_bar%
```

Ra giá trị (không phải in nguyên `%tutien_stage%`) là đã chạy đúng. Danh sách đầy đủ placeholder
xem lại phần cuối file này.

## 2. BetterHud 2.0.0 (Bukkit) — cấu trúc thư mục mới, khác hẳn bản 1.x cũ

BetterHud 2.0.0 dùng kiến trúc **tách file theo loại**, không còn gộp hết vào 1 `config.yml`
như bản cũ (`hud-content:` phẳng). Sau khi cài, nó tự sinh:

```
plugins/BetterHud/
├── config.yml       # cấu hình chung (resource pack, refresh...)
├── huds/            # định nghĩa từng HUD (hud nào bật khi nào, dùng layout nào)
├── layouts/         # bố cục — sắp xếp các text/image/head trong 1 HUD
├── images/          # định nghĩa ảnh
├── texts/           # định nghĩa phần tử chữ (nơi bạn nhét %tutien_xxx%)
├── popups/          # popup
└── compasses/       # marker la bàn
```

BetterHud 2.0.0 **coi PlaceholderAPI là dependency tùy chọn** — chỉ cần cài PAPI song song là
placeholder của bạn dùng được ngay trong các file trên, không cần thêm dependency Java nào vào
`pom.xml` của TuTienPlugin (plugin của bạn không gọi API BetterHud, BetterHud tự gọi ngược qua PAPI).

### Cách làm nhanh nhất — sửa từ file mẫu có sẵn (khuyên dùng)
Vì cú pháp chi tiết của từng field trong `texts/`, `layouts/`, `huds/` bản 2.0.0 mình không
đảm bảo 100% chính xác từng key (tài liệu công khai cho bản 2.x còn khá sơ sài), cách an toàn
và nhanh nhất là:

1. Khởi động server với BetterHud 2.0.0 lần đầu (chưa cần TuTienPlugin cũng được) — nó tự sinh
   sẵn HUD mẫu mặc định (health/hunger bar...) trong `plugins/BetterHud/texts/` và `huds/`.
2. Copy một file `.yml` trong `texts/` (loại `TEXT`) ra làm file mới, ví dụ
   `texts/tutien_stage.yml`, rồi trong field hiển thị nội dung chữ của nó, thay chuỗi mẫu bằng:
   ```
   %tutien_full_color%
   ```
   (hoặc `%tutien_lk_bar% %tutien_lk_percent%%`, `%tutien_class% - %tutien_dao%`, v.v. — xem
   danh sách bên dưới).
3. Vào file trong `layouts/` đang được HUD mặc định dùng, thêm text mới đó vào layout (mỗi bản
   ghi trong layout thường có `name`, toạ độ `x`/`y`).
4. Chạy `/betterhud reload` (hoặc `/hud reload` tùy bản), vào server kiểm tra.

Làm theo cách "nhân bản từ mẫu có sẵn" đảm bảo đúng 100% cú pháp field của đúng version bạn
đang chạy, thay vì gõ tay một cấu trúc có thể lệch key so với 2.0.0.

### Tài liệu tham khảo chính thức cho bản 2.x
- https://toxicity188-betterhud.mintlify.app/ (docs mới, có mục Installation/Quickstart)
- https://github.com/toxicity188/BetterHud/wiki (wiki GitHub)
- Nếu cần chắc chắn tuyệt đối 1 field nào đó, hỏi trực tiếp trên Discord của BetterHud (link
  trong trang SpigotMC/Modrinth) — dev đổi cấu trúc file khá thường xuyên giữa các bản 2.x.

## 3. Danh sách placeholder hiện có (prefix `%tutien_...%`)
- Cảnh giới: `stage`, `stage_plain`, `rank`, `rank_name`, `full`, `full_color`, `realm`
- Linh khí: `lk`, `lk_max`, `lk_percent`, `lk_bar`, `lk_short`, `lk_short_max`, `lk_ready`
- Linh thạch/tiền: `lt` (`tien`), `lt_short` (`tien_short`)
- Nhân vật: `class`, `dao`, `toc`, `toc_color`, `lingcan`, `thienmeng`
- Chiến đấu: `kills`, `deaths`, `boss_kills`, `bt_success`, `bt_fail`, `kd`
- Karma: `karma_cong`, `karma_nghiep`, `karma_grade`
- Trạng thái: `in_trial`, `pending_bt`, `aura`

## 4. Lưu ý
- Nếu `%tutien_xxx%` hiện nguyên chữ thay vì giá trị: kiểm tra log server lúc `onEnable()` có
  dòng `[Hook] ✅ PlaceholderAPI — 30 placeholder đã đăng ký.` không, và PlaceholderAPI có load
  thành công không (`/papi list`).
- `plugin.yml` của TuTienPlugin đã có `BetterHud` trong `softdepend` để đảm bảo thứ tự load,
  dù thực ra không bắt buộc vì việc hiển thị đi qua PlaceholderAPI chứ không phải gọi thẳng nhau.
