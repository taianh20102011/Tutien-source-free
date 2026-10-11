TU TIEN 7.6.0 - /guide -> DeluxeMenus HOOK

Đã sửa:
1. /guide không còn mở menu chữ của TuTienPlugin khi DeluxeMenus tồn tại.
2. /guide gọi "dm open tutien_guide <player>" từ console để member không cần quyền deluxemenus.open.others.
3. Nếu DeluxeMenus không có, /guide tự fallback về menu chữ cũ.
4. Bỏ alias /hd và /huongdan để người chơi chỉ dùng /guide.

Cài:
- Dùng source này để build lại TuTienPlugin.
- Trong DeluxeMenus/config.yml phải có:
    tutien_guide:
      file: tutien_guide.yml
- Đặt tutien_guide.yml vào plugins/DeluxeMenus/gui_menus/
- /dm reload
- restart server sau khi thay jar.

Sau build:
  /guide

Không cần:
  /guide_thao
  /guide_tuluyen
  /guide_luyendan
...
Menu con chỉ mở bằng click trong /guide.
