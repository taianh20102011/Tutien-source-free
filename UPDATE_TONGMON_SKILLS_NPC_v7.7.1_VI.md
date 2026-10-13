# TuTienPlugin v7.7.1 — Tông Môn 2.0, Skill Custom và Đệ Tử NPC

Bản cập nhật source cho cả FREE và PREMIUM. Đây là mã nguồn Java/Paper, chưa phải JAR đã biên dịch.

## 1. Địa bàn Tông Môn — native claim/region

### Lệnh

- `/tongmon dat pos1` — đánh dấu góc thứ nhất tại vị trí người chơi.
- `/tongmon dat pos2` — đánh dấu góc đối diện.
- `/tongmon dat thue` — thuê vùng đã chọn; phí trừ từ kho Tông Môn.
- `/tongmon dat giahan` — gia hạn thời gian thuê.
- `/tongmon dat xem` — xem vùng và hạn thuê.

Chỉ thành viên có quyền quản lý Tông Môn mới có thể thuê/gia hạn. Hai góc phải cùng thế giới; kích thước được giới hạn bởi cấu hình.

### `plugins/TuTienPlugin/tongmon-config.yml`

```yaml
land:
  enabled: true
  rent-cost: 50000
  renew-cost: 25000
  duration-days: 30
  max-side: 64
  max-height: 128
  claim-provider: AUTO
  respect-worldguard: true
  respect-griefprevention: true
```

`claim-provider` nhận một trong các giá trị:

- `AUTO`: ưu tiên WorldGuard, sau đó GriefPrevention, nếu không có thì dùng lớp bảo vệ riêng TuTien.
- `WORLDGUARD`: tạo cuboid region native.
- `GRIEFPREVENTION`: tạo claim native do Môn Chủ sở hữu và trust thành viên.
- `TUTIEN`: chỉ dùng hệ thống bảo vệ nội bộ của TuTien.
- `NONE`: hiện vẫn dùng lớp bảo vệ nội bộ TuTien; không có nghĩa là vô hiệu hóa bảo vệ.

`respect-worldguard` và `respect-griefprevention` kiểm tra vùng/claim chồng lấn trước khi cho thuê. Việc kiểm tra dùng reflection để hỗ trợ các phiên bản API khác nhau; nếu không đọc được API của provider đã bật, hệ thống từ chối thuê/migrate để tránh chồng lấn.

Khi Tông Môn thêm/kick thành viên, danh sách quyền được đồng bộ. Khi giải thể hoặc hết hạn, plugin cố gắng gỡ native region/claim trước khi xóa dữ liệu. Nếu provider ngoài bị gỡ hoặc lỗi API, vùng cũ có thể cần khôi phục plugin provider để cleanup; kiểm tra console trước khi xóa file `tongmon-land.yml`.

## 2. Chiêu mộ, cấp bậc và liên minh

### Chiêu mộ

- `/mochieumo` — Môn Chủ mở/đóng chiêu mộ công khai.
- Thông báo chiêu mộ có nút gửi đơn `/tongmon ungTuyen <id>`.
- `/tongmon don` — xem đơn đang chờ.
- `/tongmon duyet <player>` — chấp nhận đơn.
- `/tongmon tuchoi <player>` — từ chối đơn.
- `/tongmon cap <player> HOP_PHAP_TRUONG_LAO` — bổ nhiệm Hộ Pháp Trưởng Lão (Môn Chủ/Phó Tông Chủ).

### Liên minh

- `/tongmon lienminh` hoặc `/tongmon lienminh gui` — mở GUI.
- `/tongmon lienminh moi <id|tag|ten>` — gửi lời mời.
- `/tongmon lienminh chap <id>` / `/tongmon lienminh tuchoi <id>` — chấp nhận/từ chối.
- `/tongmon lienminh pvp <id|tag|ten> <on|off>` — đồng ý/tắt PvP liên minh phía Tông Môn của mình.
- `/tongmon lienminh dat <id|tag|ten> <on|off>` — đồng ý/tắt quyền vào lãnh địa phía Tông Môn của mình.

PvP liên minh và quyền vào lãnh địa chỉ có hiệu lực khi **cả hai Tông Môn cùng bật đồng thuận**. Chỉ Môn Chủ/Phó Tông Chủ được thay đổi thỏa thuận. Khi một Tông Môn giải thể, liên minh và đơn chờ liên quan được dọn.

## 3. Skill mới và vật phẩm kỹ năng

Đã đăng ký trong `SkillManager`:

- `chienkhiquyet` — Chiến Khí Quyết (Thể Tu): vòng chấn động, sát thương diện gần, đẩy lùi và buff chiến đấu.
- `phatthuan` — Phật Thủ Ấn (Pháp Tu): nhảy lên rồi giáng chưởng, gây sát thương diện rộng; có giới hạn tick để tránh kẹt trên không.
- `thienmatrao` — Thiên Ma Trảo (Quỷ Tu): tìm mục tiêu phía trước trong 12 block, gây sát thương và kéo mục tiêu về phía người thi triển.

Lấy vật phẩm kỹ năng vật lý:

```text
/tutien layky chienkhiquyet
/tutien layky phatthuan
/tutien layky thienmatrao
```

Lệnh chỉ cấp skill thuộc class hiện tại hoặc khi người chơi đã đạt cảnh giới cho phép dùng mọi skill; yêu cầu rank được kiểm tra trước khi cấp. Vật phẩm dùng PDC `skill_id` và cần tương tác chuột phải theo luồng skill hiện có.

## 4. Huyết Ngục Ấn — công pháp custom

Mẫu nằm trong `plugins/TuTienPlugin/congphap.yml` (`techniques.huyet_nguc_an`):

- `/congphap hoc huyet_nguc_an` — học.
- `/congphap trangbi huyet_nguc_an` — trang bị làm công pháp hiện tại.
- `/congphap thi-trien 1` — đóng ấn lên mục tiêu gần nhất trong phạm vi.
- `/congphap thi-trien 2` — kích nổ tất cả mục tiêu trong tầm có ấn do chính người thi triển đặt, hệ số ×1.8.
- `/congphap thi-trien 3` — kích nổ tất cả mục tiêu mang ấn của người thi triển, hệ số ×2.8.

Tag chứa UUID người thi triển; ấn được tự gỡ sau `duration-ticks`. Combo 2/3 không gây sát thương lên mục tiêu không mang ấn của người thi triển.

Các loại `active-type` hỗ trợ: `NONE`, `DAMAGE`, `AOE_DAMAGE`, `MARK`, `DETONATE_MARK`, `HEAL`, `BUFF`. Trước khi sửa YAML, sao lưu file; giữ `id` chữ thường gồm `a-z`, `0-9`, `_` hoặc `-`.

## 5. Đệ Tử NPC follower (Citizens)

Cần cài Citizens tương thích với Paper 1.21.1. Dữ liệu được lưu tại `plugins/TuTienPlugin/dedoan-npc.yml`.

```text
/dedoan npc tao <tên>
/dedoan npc list
/dedoan npc stats <id>
/dedoan npc randomskip <id>
/dedoan npc thechat <id> <loại>
/dedoan npc linhcan <id> <loại>
/dedoan npc tuchat <id> <1-100>
/dedoan npc xoa <id>
```

Mỗi NPC có dữ liệu riêng: owner UUID, tên, cảnh giới/rank, Thể Chất, Linh Căn, Tư Chất 1–100, thời điểm tạo và cooldown RandomSkip. Người chơi chỉ sửa/xóa NPC của mình; Admin có thể quản lý tất cả. NPC đi theo chủ nhân cùng online, tự teleport khi khác world/quá xa và tên hiển thị cảnh giới/rank.

`RandomSkip` tiến một số rank ngẫu nhiên theo `settings.randomskip-min-ranks` / `randomskip-max-ranks`, có cooldown mỗi NPC. Một số chỉ số được áp dụng lên Bukkit entity attributes nếu NPC implementation hỗ trợ các attribute tương ứng; Citizens NPC vẫn là follower, chưa được bổ sung combat AI/skill AI riêng.

PlaceholderAPI (nếu đã cài):

```text
%tutien_npc_disciple_count%
%tutien_npc_disciple_1_name%
%tutien_npc_disciple_1_stage%
%tutien_npc_disciple_1_rank%
%tutien_npc_disciple_1_thechat%
%tutien_npc_disciple_1_linhcan%
%tutien_npc_disciple_1_tuchat%
```

## 6. Giới hạn kiểm thử

Source đã qua kiểm tra tĩnh: 103 file Java/edition được quét cân bằng delimiter và lexical state, 13 resource YAML/edition parse được, `pom.xml` hợp lệ, và file gameplay chung giữa FREE/PREMIUM đã được đối chiếu. Môi trường đóng gói không có Maven/dependency cache nên chưa chạy Maven compiler/build và chưa test trên Paper server thật. Trước khi dùng production, hãy thử WorldGuard/GriefPrevention theo đúng version server, Citizens follower, claim cleanup và skill trên một server staging.
