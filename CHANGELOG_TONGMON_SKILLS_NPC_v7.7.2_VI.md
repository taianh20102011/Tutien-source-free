# TuTienPlugin v7.7.2 — Changelog (VI)

## Fix & Hardening

- **Land protection:** bổ sung listener thật cho break/place/interact, bucket, fluids, fire, entity block change, explosions và piston để provider nội bộ `TUTIEN` cũng bảo vệ được khu thuê.
- **Alliance entry:** kiểm tra thỏa thuận song phương trước khi người chơi bước vào lãnh địa đồng minh; khi bị thu hồi quyền giữa lúc đang trong vùng, người chơi có thể rời đi nhưng không thể sửa đổi khu đất.
- **Alliance PvP:** enforcement ở `EntityDamageByEntityEvent`, hỗ trợ đánh trực tiếp và projectile; PvP giữa hai Tông Môn đồng minh chỉ mở nếu cả hai cùng đồng ý.
- **WorldGuard sync:** ghi nhận thành viên đã được thêm vào region để cập nhật membership khi thay đổi Tông Môn.
- Giữ nguyên các tính năng công pháp custom, ba skill bổ sung, Citizens follower, RandomSkip, chỉ số NPC riêng, YAML `dao.yml`/`phabao.yml`/`hubs.yml`.

## Kiểm thử

Kiểm tra tĩnh pass cho cả FREE/PREMIUM; Maven compilation và runtime integration chưa được thực hiện do môi trường không có Maven/dependencies.
