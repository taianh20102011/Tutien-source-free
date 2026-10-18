# ĐỘT PHÁ QUA NPC (KHOÁ LỆNH THEO WORLD)

## Cơ chế
- `/vuottamma` và `/dotpha` bị **CHẶN** khi người chơi đứng trong bất kỳ
  world nào nằm trong danh sách `config.yml -> breakthrough-npc.worlds`
  (khai báo được **NHIỀU world cùng lúc**, không chỉ 1).
- Trong các world đó, người chơi phải **click chuột phải** vào 1 NPC
  Citizens (ID khai báo trong `breakthrough-npc.npc-ids`) thì mới bắt
  đầu được Thiền Định (Bước 1) / Thiên Kiếp (Bước 2).
- NPC "thông minh": tự biết người chơi đang ở bước nào (chưa đủ Linh Khí,
  cần Thiền Định trước, hay đã sẵn sàng Thiên Kiếp) — chỉ cần click lặp
  lại, không cần nhớ lệnh nào.
- Bạn có thể đặt **nhiều NPC ở nhiều world khác nhau** — chỉ cần ID của
  từng NPC nằm trong `npc-ids`, click NPC nào cũng đột phá được, không
  bắt buộc phải đúng world của NPC đó.
- World nào KHÔNG có trong danh sách `worlds` thì `/vuottamma` và
  `/dotpha` **vẫn hoạt động bình thường** như cũ.

## File đã sửa / thêm mới
- `pom.xml` — thêm repo + dependency `net.citizensnpcs:citizensapi` (scope `provided`, chỉ để biên dịch).
- `plugin.yml` — thêm `Citizens` vào `softdepend`.
- `config.yml` — thêm mục `breakthrough-npc` (cuối file).
- `BreakthroughManager.java` — thêm `isNpcOnlyWorld(player)` và `getNpcBlockMessage()`.
- `TuTienCommand.java` — `handleVuotTamMa`/`handleDotPha` chặn sớm nếu `isNpcOnlyWorld()`.
- `BreakthroughNpcListener.java` **(mới)** — lắng nghe `NPCRightClickEvent` của Citizens, gọi thẳng
  `BreakthroughManager.startThienDinh()` / `startBreakthroughTrial()`, KHÔNG đi qua lệnh nên không bị chặn.
- `TuTienPlugin.java` — chỉ đăng ký `BreakthroughNpcListener` khi plugin Citizens THẬT SỰ có trên server
  (tránh `NoClassDefFoundError` nếu server chưa cài Citizens).

## Cài đặt
1. Build lại bằng Maven (`mvn clean package`) trên máy có JDK 21 + Maven, đã cài Citizens
   trên server test (repo Citizens cần internet để tải `citizensapi`).
2. Thay JAR `TuTienPlugin` cũ bằng JAR mới trong `target/`.
3. Đảm bảo plugin **Citizens** đã cài và bật trên server.
4. Tạo NPC bằng Citizens: `/npc create <TênNPC>` tại vị trí mong muốn trong world bị khoá.
5. Lấy ID NPC: đứng cạnh NPC, `/npc select` rồi xem `/npc id` (hoặc `/npc list` để xem toàn bộ).
6. Sửa `config.yml` — khai báo TẤT CẢ world muốn khoá và TẤT CẢ NPC muốn dùng:
   ```yaml
   breakthrough-npc:
     enabled: true
     worlds:
       - "the_gioi_tu_luyen"
       - "the_gioi_tu_luyen_nether"
       - "dao_hoang"
     npc-ids: [5, 12, 20]   # 1 ID cho mỗi NPC bạn đặt ở các world trên
     block-message: "&cKhu vực này bạn phải đến gặp NPC để tiến hành Đột Phá!"
   ```
7. `/ttadmin reload` (hoặc restart server) để áp dụng.

## Kiểm thử
- Đứng trong 1 trong các world đã khai báo, gõ `/dotpha` hoặc `/vuottamma` → nhận tin nhắn chặn, KHÔNG đột phá được.
- Click chuột phải vào đúng NPC đã khai báo ID (ở bất kỳ world nào trong danh sách) → bắt đầu Thiền Định / Thiên Kiếp bình thường.
- Sang world khác (không có trong `worlds`) → `/dotpha`, `/vuottamma` vẫn chạy bình thường như cũ.
- Tắt Citizens (hoặc không cài) mà vẫn để `enabled: true` → server log sẽ cảnh báo (WARNING) vì
  world sẽ bị khoá lệnh nhưng không có NPC nào hoạt động để thay thế — nhớ kiểm tra log khi mới cấu hình.

## Mở rộng (tuỳ chọn)
Nếu sau này muốn khoá theo **vùng WorldGuard** thay vì cả world (server đã có sẵn WorldGuard
làm dependency), chỉ cần đổi điều kiện trong `BreakthroughManager.isNpcOnlyWorld()` từ so sánh
tên world sang kiểm tra region bằng WorldGuard `RegionQuery#getApplicableRegions(location)`.
