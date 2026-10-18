# TuTienPlugin v7.7.1 — Feature Source Validation

## Kết quả

Đây là **kiểm tra tĩnh của source**, không phải kết quả Maven build hay runtime test. Maven không có trong môi trường đóng gói.

| Kiểm tra | FREE | PREMIUM |
|---|---:|---:|
| File Java đã quét lexical/delimiter | 103 | 103 |
| YAML resource cấp gốc | 7 | 7 |
| YAML parse đệ quy | 13 | 13 |
| pom.xml hợp lệ | PASS | PASS |
| Java lexical errors | 0 | 0 |
| YAML parse errors | 0 | 0 |

## Các điểm tính năng đã kiểm tra tĩnh

| Hạng mục | FREE | PREMIUM |
|---|---:|---:|
| `claim_check` | PASS | PASS |
| `worldguard_global_skip` | PASS | PASS |
| `gp_native_claim` | PASS | PASS |
| `skill_chienkhiquyet` | PASS | PASS |
| `skill_phatthuan` | PASS | PASS |
| `skill_thienmatrao` | PASS | PASS |
| `huyet_nguc_mark` | PASS | PASS |
| `npc_randomskip` | PASS | PASS |
| `npc_stats_applied` | PASS | PASS |
| `npc_tab_complete` | PASS | PASS |
| `griefprevention_softdepend` | PASS | PASS |
| `custom_dao_file` | PASS | PASS |
| `custom_phabao_file` | PASS | PASS |
| `custom_hubs_file` | PASS | PASS |

## Source chung giữa hai edition

Các file gameplay chung được so sánh byte-for-byte; kết quả:
- `src/main/java/com/tutien/tongmon/TongMonLandManager.java`: MATCH
- `src/main/java/com/tutien/tongmon/TongMonManager.java`: MATCH
- `src/main/java/com/tutien/tongmon/TongMonAllianceManager.java`: MATCH
- `src/main/java/com/tutien/data/ClassType.java`: MATCH
- `src/main/java/com/tutien/skills/SkillManager.java`: MATCH
- `src/main/java/com/tutien/skills/SkillItemManager.java`: MATCH
- `src/main/java/com/tutien/commands/TuTienCommand.java`: MATCH
- `src/main/java/com/tutien/congphap/ConGPhapManager.java`: MATCH
- `src/main/java/com/tutien/suphudedo/SuPhuDeDoManager.java`: MATCH
- `src/main/java/com/tutien/hooks/TuTienPlaceholderExpansion.java`: MATCH
- `src/main/resources/congphap.yml`: MATCH
- `src/main/resources/tongmon-config.yml`: MATCH

## Chưa xác minh

- Chưa chạy `mvn clean package` vì Maven và dependency cache không có trong môi trường đóng gói.
- Chưa chạy plugin trên Paper 1.21.1 thực tế.
- Chưa test tích hợp runtime với từng phiên bản WorldGuard, GriefPrevention và Citizens. Những API này dùng reflection để giảm ràng buộc compile-time; cần staging để xác minh provider cụ thể.
- NPC Đệ Tử hiện là Citizens follower có stats/RandomSkip riêng; chưa có AI chiến đấu dùng kỹ năng riêng.
- Hook native claim phải được kiểm tra trước khi bật production; nếu provider không thể kiểm tra hoặc tạo/xóa claim, thao tác fail-closed và cần xem console.

Không xem PASS tĩnh là bảo đảm không còn bug runtime.
