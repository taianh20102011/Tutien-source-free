# TuTien License Control

Web dashboard + API để phát hành/thu hồi license cho TuTienPlugin. Dùng Node.js 20+; API không cần npm dependency bên ngoài. UI Tailwind CSS hiện tải từ CDN để thử nghiệm; trước khi production nên bundle Tailwind CSS local và gỡ CDN vì dashboard xử lý admin token.

## Start

```bash
npm start
npm test
```

Default listen address: `127.0.0.1:8787`. Lần đầu tiên, một admin token ngẫu nhiên được in ra console và ghi vào `data/admin-token.txt`. Không chia sẻ token này. Chạy sau reverse proxy HTTPS khi host game cần kết nối từ Internet.

## Plan

- `FREE`: vĩnh viễn, không có ngày hết hạn; một license FREE duy nhất theo observed host IP trong vòng đời key; bind thêm installation UUID. Bản nâng cấp tự chuyển license FREE cũ có hạn 365 ngày sang vĩnh viễn, giữ nguyên IP/installation binding; key đã bị revoke vẫn bị khóa.
- `PREMIUM`: 30, 90, 180, 365, 730 ngày hoặc `0` cho lifetime; bind IP + installation UUID; không nhận FREE key.
- `Unbind`: dùng cho chuyển host hợp lệ; FREE vẫn vĩnh viễn, Premium giữ nguyên ngày hết hạn gốc.
- `Revoke`: thu hồi không thể activate/validate lại.

## Environment

Copy `.env.example` values into service manager/environment (Node does not auto-read `.env`):

- `HOST`, `PORT`
- `TUTIEN_ADMIN_TOKEN` (tối thiểu 24 ký tự; khuyến nghị token ngẫu nhiên mạnh)
- `TUTIEN_DATA_DIR`
- `TRUST_PROXY=true` only behind a trusted proxy that overwrites `X-Forwarded-For`.

Keep the database and token files private. Use TLS termination and firewall at deployment. Back up `data/licenses.json` and `data/admin-token.txt`. Admin operations are protected by bearer token; rate limiting applies by observed IP.

## Electron desktop shell (optional)

```bash
npm install --prefix desktop
npm run desktop
```

Electron starts the dashboard service bound to localhost and loads it in a desktop window. This shell is source-only; package/sign it on the target OS as a separate release step.

## Routes

- `GET /api/health`
- `POST /api/activate`, `POST /api/validate` (host IP observed by API)
- `GET /api/admin/licenses`
- `POST /api/admin/licenses`
- `POST /api/admin/licenses/:uuid/unbind`
- `POST /api/admin/licenses/:uuid/revoke`


Lưu ý: giới hạn theo IP là biện pháp chống lạm dụng thực tế, không phải DRM tuyệt đối. Nhà cung cấp Internet có thể dùng NAT chung; hãy cân nhắc yêu cầu xác minh thủ công cho người dùng bị ảnh hưởng. Hiện chưa có cổng thanh toán tự động; admin tạo và gửi Premium key sau khi xác nhận thanh toán.
