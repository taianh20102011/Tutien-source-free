# Linh Thú config-driven — hướng dẫn áp dụng

Patch này bỏ toàn bộ `switch(stage.getRealm())` cứng trong `PetSummon.java` và
`SimplePetsHook.java`, chuyển thành đọc từ `config.yml`, và thêm khả năng
trieu hồi mob **MythicMobs** (có thể gắn model 3D riêng qua **ModelEngine**)
làm Linh Thú — không chỉ mob vanilla nữa.

## 1. File MỚI (copy nguyên, không đè gì cả)

- `src/main/java/com/tutien/data/PetTierConfig.java`
- `src/main/java/com/tutien/data/PetTierManager.java`
- `src/main/resources/mythicmobs/Mobs/TT_Pets.yml`

## 2. File SỬA (thay thế toàn bộ nội dung file cũ)

- `src/main/java/com/tutien/sword/PetSummon.java` — viết lại gần như hoàn
  toàn: bỏ `petTypeFor()`, `tierName()` hardcode; thêm `resolveMode()` +
  `spawnMythicPet()` + `spawnVanillaPet()`; buff giờ đọc `regen-amplifier`/
  `luck-amplifier` từ config (đặt `-1` để tắt hẳn 1 hiệu ứng).
- `src/main/java/com/tutien/hooks/SimplePetsHook.java` — `summon()` giờ nhận
  thẳng `String petType` (lấy từ config) thay vì tự switch theo `CultivationStage`.
- `src/main/java/com/tutien/hooks/MythicMobsHook.java` — **chỉ thêm 1 method
  mới** `spawnGenericMob(mobId, loc, level)`, mọi thứ khác giữ nguyên 100%
  (Boss Arena, Aura... không đổi).

## 3. File SỬA (chỉ vài dòng — nên diff trước khi ghi đè)

- `src/main/java/com/tutien/TuTienPlugin.java`:
  - Thêm field `petTierManager` + getter `getPetTierManager()`.
  - Thêm dòng khởi tạo `petTierManager = new com.tutien.data.PetTierManager(this);`
    ngay TRƯỚC dòng `petSummon = new PetSummon(this);`.
- `src/main/java/com/tutien/commands/AdminCommand.java`:
  - `case "reload"` giờ gọi thêm `plugin.reloadConfig(); plugin.getPetTierManager().reload();`
    → `/ttadmin reload` áp dụng thay đổi Linh Thú **ngay lập tức**, không cần
    restart server nữa (các hệ thống khác vẫn cần restart như cũ).
- `src/main/resources/config.yml`:
  - Thêm nguyên section `linh-thu:` (chèn ngay sau `flying-sword:`, trước `combat:`).
  - **QUAN TRỌNG**: nếu server của bạn đã chạy và đã có sẵn `config.yml` trong
    `plugins/TuTien/`, Bukkit sẽ **KHÔNG** tự thêm section mới vào file đang
    chạy (`saveDefaultConfig()` chỉ tạo file khi chưa tồn tại). Bạn cần tự
    copy đoạn `linh-thu:` bên dưới vào file config.yml thật trên server.

## 4. Đoạn config cần thêm (copy-paste vào config.yml đang chạy)

```yaml
linh-thu:
  enabled: true
  fallback-order: [MYTHICMOBS, SIMPLEPETS, VANILLA]

  default:
    mode: VANILLA
    entity-type: WOLF
    mythicmobs-id: ""
    mythicmobs-level: 1
    simplepets-type: wolf
    display-name: "Linh Thú"
    flies: false
    regen-amplifier: 0
    luck-amplifier: 0

  tiers:
    Linh Gioi:
      mode: VANILLA
      entity-type: WOLF
      simplepets-type: wolf
      display-name: "Linh Sói"
      flies: false
      regen-amplifier: 0
      luck-amplifier: 0

    De Gioi:
      mode: VANILLA
      entity-type: OCELOT
      simplepets-type: ocelot
      display-name: "Đế Miêu"
      flies: false
      regen-amplifier: 0
      luck-amplifier: 0

    Thanh Gioi:
      mode: MYTHICMOBS
      mythicmobs-id: "TT_Pet_ThanhHo"
      mythicmobs-level: 1
      entity-type: FOX
      simplepets-type: fox
      display-name: "Thánh Hồ"
      flies: false
      regen-amplifier: 1
      luck-amplifier: 0

    Tien Gioi:
      mode: MYTHICMOBS
      mythicmobs-id: "TT_Pet_TienLinh"
      mythicmobs-level: 1
      entity-type: ALLAY
      simplepets-type: allay
      display-name: "Tiên Linh"
      flies: true
      regen-amplifier: 1
      luck-amplifier: 0

    Thien Gioi:
      mode: MYTHICMOBS
      mythicmobs-id: "TT_Pet_ThienDieu"
      mythicmobs-level: 1
      entity-type: PARROT
      simplepets-type: parrot
      display-name: "Thiên Vũ Linh Điểu"
      flies: true
      regen-amplifier: 2
      luck-amplifier: 0
```

## 5. Cách hoạt động (đọc để tự mở rộng sau này)

`PetTierManager` đọc section `linh-thu` thành `Map<TênGiới, PetTierConfig>`
khi plugin khởi động, và mỗi khi gọi `/ttadmin reload`. `PetSummon` không
còn biết "Linh Gioi = Wolf" trong code nữa — nó chỉ hỏi
`plugin.getPetTierManager().getTier(stage.getRealm())` rồi làm theo những gì
đọc được.

Mỗi Giới chọn 1 `mode`:

| mode | Cần cài gì | Ghi chú |
|---|---|---|
| `VANILLA` | Không cần gì | Luôn hoạt động, dùng làm lưới an toàn cuối cùng |
| `MYTHICMOBS` | MythicMobs (miễn phí) | Mob định nghĩa trong `Mobs/TT_Pets.yml`; muốn model 3D riêng (kỳ lân, hồ ly 9 đuôi...) thì cài thêm ModelEngine (có bản free) + dựng model bằng BlockBench, rồi bật dòng `Model: <id>` trong file yml đó |
| `SIMPLEPETS` | Plugin SimplePets | Có GUI quản lý pet sẵn cho người chơi |

Nếu 1 Giới đặt `mode: MYTHICMOBS` nhưng server chưa cài MythicMobs,
`resolveMode()` trong `PetSummon` tự động rớt xuống mode kế tiếp trong
`fallback-order` — luôn rơi về `VANILLA` cuối cùng, không bao giờ bị lỗi
trắng tay.

### Muốn thêm 1 Giới mới hoàn toàn (không có trong `CultivationStage` hiện tại)?
Chỉ cần thêm 1 mục con trong `tiers:` với đúng tên Giới (`getRealm()` trả về
gì thì đặt tên y hệt) — không cần sửa 1 dòng Java nào.

### Muốn thêm buff khác ngoài Regeneration/Luck?
Đây là chỗ duy nhất còn "cứng" — sửa `applyBuff()` trong `PetSummon.java` để
đọc thêm field mới từ `PetTierConfig` (theo đúng khuôn mẫu `regenAmplifier`/
`luckAmplifier` đã có).

## 6. Lưu ý

Mình **không build/compile** được patch này trong môi trường hiện tại (không
có quyền truy cập Maven Central / repo.papermc.io để tải Paper API), nên bạn
nhớ chạy `mvn clean package` và test trên server dev trước khi đẩy lên
server thật. Mình đã kiểm tra kỹ theo đúng convention sẵn có trong project
(record, ConfigurationSection, cách các hook khác trong plugin xử lý lỗi...)
nhưng vẫn nên tự build-check lại.
