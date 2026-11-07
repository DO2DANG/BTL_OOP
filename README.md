# BTL OOP - Restaurant System

Dự án quản lý nhà hàng viết bằng Java. Mã nguồn chính nằm trong `RestaurantSystem.java`.

## Yêu cầu

- Đã cài Git.
- Đã cài JDK và có thể chạy lệnh `java` và `javac` trong Terminal.

## Tải dự án về máy

Mở Terminal tại thư mục muốn lưu dự án, sau đó chạy:

```powershell
git clone https://github.com/DO2DANG/BTL_OOP.git
cd BTL_OOP
```

Hoặc trên GitHub, chọn **Code > Download ZIP**, tải và giải nén dự án.

## Cập nhật code mới nhất

Nếu đã tải dự án bằng `git clone`, mở Terminal trong thư mục dự án và chạy trước khi bắt đầu làm việc:

```powershell
git pull
```

Lệnh này tải các thay đổi mới nhất từ GitHub. Nếu đã sửa code mà `git pull` báo xung đột, hãy xử lý các đoạn được Git đánh dấu trong file, lưu lại rồi tạo commit.

## Đẩy thay đổi của bạn lên GitHub

Sau khi chỉnh sửa và kiểm tra code, chạy trong thư mục dự án:

```powershell
git add .
git commit -m "Mô tả thay đổi"
git push
```

Thay `Mô tả thay đổi` bằng nội dung ngắn gọn, ví dụ `Cập nhật chức năng đặt món`.

Nếu có nhiều người cùng làm, hãy chạy `git pull` trước khi sửa và trước khi đẩy code để lấy thay đổi mới nhất. Không tải lại hoặc giải nén ZIP đè lên thư mục đang có các thay đổi chưa lưu.

## Biên dịch và chạy

Trong Terminal tại thư mục có `RestaurantSystem.java`, chạy:

```powershell
javac --add-modules jdk.httpserver RestaurantSystem.java
java --add-modules jdk.httpserver RestaurantSystem
```

Sau khi khởi động, ứng dụng dùng cổng `8080` cho khách hàng và cổng `8081` cho chủ nhà hàng. Mở `http://localhost:8080` hoặc `http://localhost:8081` trên trình duyệt.
