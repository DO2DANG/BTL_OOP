# Gà Vàng Phố Cổ

Bài tập lớn OOP Java cho hệ thống đặt món tại bàn. Cổng mặc định của trang khách là **8080** và của màn hình bếp là **8081**. Cả hai màn hình truy cập cùng một `RestaurantService`.

## Chạy dự án

Dùng JDK 17 trở lên. Với IntelliJ IDEA, mở thư mục chứa `pom.xml` rồi chạy `restaurant.RestaurantSystem`.

Nếu dùng Maven, chạy từ thư mục gốc của dự án.

```powershell
mvn clean package
java --add-modules jdk.httpserver -jar target/restaurant-system-1.0.0.jar
```

Có thể biên dịch bằng JDK ngay trong PowerShell.

```powershell
$sources = Get-ChildItem -LiteralPath src/main/java -Recurse -Filter *.java | ForEach-Object FullName
New-Item -ItemType Directory -Force -Path target/classes | Out-Null
javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d target/classes $sources
java --add-modules jdk.httpserver -cp 'target/classes;src/main/resources' restaurant.RestaurantSystem
```

- [Trang khách](http://localhost:8080)
- [Màn hình bếp](http://localhost:8081)

Nếu cổng mặc định đang bận, chương trình tự chọn cổng còn trống. Mở hai địa chỉ được in trong cửa sổ Run.

## Cấu trúc và luồng đặt món

Mã Java nằm trong `src/main/java/restaurant`.

| Thành phần | Nhiệm vụ |
|---|---|
| `RestaurantSystem.java` | Khởi động hai máy chủ HTTP và truyền cùng một service cho hai controller. |
| `model` | `User`, `Customer`, `RestaurantOwner`, `MenuItem`, `OrderItem`, `CartSession`, `Order` và `OrderStatus`. |
| `exception` | `InvalidPhoneNumberException` biểu diễn số điện thoại sai định dạng. |
| `service` | Giữ thực đơn, tạo đơn, cập nhật đơn và tính tổng tiền của các đơn đã hoàn tất. |
| `controller` | Đọc yêu cầu HTTP, gọi model hoặc service và trả kết quả cho trình duyệt. |
| `view` và `src/main/resources/assets` | Dựng HTML theo dữ liệu Java, phục vụ CSS và JavaScript cho hai màn hình. |

`CustomerController` lấy giỏ theo cookie, thêm món hoặc đổi số lượng trong `CartSession`, rồi gọi `CustomerView` để trả phần HTML của giỏ. Khi khách xác nhận, `Customer` kiểm tra thông tin và service tạo `Order` ở trạng thái `PENDING`. `KitchenController` đọc danh sách đơn và gửi phiếu bếp qua `KitchenView`. `Order` kiểm tra các bước chuyển `PENDING → COOKING → READY` và cho phép hủy từ `PENDING`.

Trong đồ án, số điện thoại là trường tùy chọn. Nếu nhập, dùng số bắt đầu bằng `0` với 10–11 chữ số hoặc dạng `+84` tương ứng. Có thể dùng dấu cách, dấu chấm và dấu gạch ngang giữa các nhóm chữ số.

## Phân công và nghiệm thu

| Thành viên | Phụ trách | Kịch bản nghiệm thu |
|---|---|---|
| 1 | Model người dùng, món, đơn và exception | Giải thích `User` và hai lớp con. Tạo khách với số hợp lệ, số bỏ trống và số sai. |
| 2 | `CartSession` và `RestaurantService` | Thêm, giảm, xóa món, tính tiền, kiểm tra giỏ trống và dữ liệu đơn sau khi xóa giỏ. |
| 3 | Trang khách và `CustomerController` | Dùng cửa sổ thường và cửa sổ ẩn danh để kiểm tra hai giỏ. Nhập số sai, sửa lại rồi gửi đơn thành công. |
| 4 | Trang bếp, `KitchenController` và tích hợp | Nhận chế biến, hoàn tất, hủy đơn, kiểm tra tổng tiền và chạy demo từ lúc chọn món. |

Mỗi người làm trên một nhánh riêng và gửi pull request để một thành viên khác kiểm tra. Nhóm chạy chung một kịch bản từ trang khách đến trang bếp trước buổi bảo vệ.

Dữ liệu giỏ và đơn hàng nằm trong RAM và được khởi tạo lại khi chạy lại chương trình.
