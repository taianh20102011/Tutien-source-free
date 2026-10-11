# TuTienPlugin FREE — Free vĩnh viễn

Đây là gói mã nguồn dành riêng cho edition **FREE**. Java source và các hook được giữ trong project.

## Build

Yêu cầu JDK 21 và Maven đã cài đặt. Từ thư mục gốc project, chạy:

- Windows PowerShell: `./build-releases.ps1`
- Linux: `./build-releases.sh`
- Hoặc: `mvn clean package -Pfree`

JAR đầu ra: `dist/TuTienPlugin-free-7.7.0.jar` (sau khi build thành công).

## License

- FREE: không có ngày hết hạn. Việc sử dụng vẫn cần server License Control và key FREE hợp lệ; binding IP/installation là chính sách chống lạm dụng, IP không phải định danh tuyệt đối.
- PREMIUM: cần key PREMIUM hợp lệ; license có thể được cấp theo thời hạn hoặc vĩnh viễn từ dashboard.

## License Control

Project có thư mục `license-platform/` chứa API/dashboard quản lý license và `desktop/` là shell Electron dạng mã nguồn. Hướng dẫn triển khai nằm ở `license-platform/README.md`.

## Lưu ý kiểm thử

Đây là source bundle, không phải JAR đã biên dịch. Môi trường đóng gói hiện tại không có Maven/Paper API dependency đầy đủ nên chưa xác nhận build trên Paper thực tế. Sau khi build, hãy thử trên staging server và kiểm tra hook phụ thuộc đang cài.
