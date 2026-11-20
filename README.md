# Gà Vàng Phố Cổ

Bài tập lớn OOP Java cho hệ thống đặt món tại bàn chuyên các món Gà & Cơm Gà. Cổng mặc định của trang khách là **8080** và của màn hình bếp là **8081**.

## Chạy dự án

Dùng JDK 17 trở lên. Với IntelliJ IDEA, mở thư mục chứa `pom.xml` rồi chạy `restaurant.RestaurantSystem`.

Nếu dùng Maven, chạy từ thư mục gốc của dự án:

```powershell
mvn clean package
java --add-modules jdk.httpserver -jar target/restaurant-system-1.0.0.jar
```

Có thể biên dịch và chạy trực tiếp bằng JDK ngay trong PowerShell:

```powershell
$sources = Get-ChildItem -LiteralPath src/main/java -Recurse -Filter *.java | ForEach-Object FullName
New-Item -ItemType Directory -Force -Path target/classes | Out-Null
javac --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d target/classes $sources
java --add-modules jdk.httpserver -cp 'target/classes;src/main/resources' restaurant.RestaurantSystem
```

Để bật chế độ lưu lịch sử đơn hàng bền vững ra tệp (`data/orders.txt`) thay vì chỉ lưu trên RAM, thêm tham số `--file`:

```powershell
java --add-modules jdk.httpserver -cp 'target/classes;src/main/resources' restaurant.RestaurantSystem --file
```

- [Trang khách](http://localhost:8080)
- [Màn hình bếp](http://localhost:8081)

Nếu cổng mặc định đang bận, chương trình tự chọn cổng còn trống và in địa chỉ tương ứng ra cửa sổ dòng lệnh.

## Kiểm thử tự động các tiêu chí OOP

Dự án đi kèm bộ kiểm thử tự động `RestaurantOopTest` (không phụ thuộc thư viện ngoài) kiểm chứng kế thừa, đa hình, đóng gói, ngoại lệ, lưu tệp và luồng đơn hàng:

```powershell
$mainSources = Get-ChildItem -LiteralPath src/main/java -Recurse -Filter *.java | ForEach-Object FullName
$testSources = Get-ChildItem -LiteralPath src/test/java -Recurse -Filter *.java | ForEach-Object FullName
New-Item -ItemType Directory -Force -Path target/test-classes | Out-Null
javac --release 17 --add-modules jdk.httpserver,java.net.http -encoding UTF-8 -d target/test-classes $mainSources $testSources
java --add-modules jdk.httpserver,java.net.http -cp 'target/test-classes;src/main/resources' restaurant.RestaurantOopTest
```

## Cấu trúc kiến trúc OOP & SOLID

Mã nguồn nằm trong `src/main/java/restaurant`:

| Thành phần | Nhiệm vụ & Tiêu chí OOP áp dụng |
|---|---|
| `RestaurantSystem.java` | Khởi tạo và tiêm phụ thuộc (Dependency Injection) cho `MenuRepository`, `OrderRepository`, `OrderService` và các `WebController`. |
| `model` | Đóng gói dữ liệu bất biến & kiểm tra ràng buộc (`Validation`). Kế thừa & Đa hình trên cây `User` (`Customer`, `RestaurantOwner`) và cây `MenuItem` (`ChickenDish`, `SideDish`, `Beverage`); phân loại bằng `enum MenuCategory`; máy trạng thái `Order` + `OrderStatus`; phiên giỏ hàng `CartSession`. |
| `exception` | Cây kế thừa ngoại lệ nghiệp vụ từ lớp trừu tượng `RestaurantException` (`InvalidPhoneNumberException`, `EmptyCartException`, `InvalidOrderStateException`, `ResourceNotFoundException`). |
| `repository` | Trừu tượng hóa lưu trữ qua `interface MenuRepository` (`InMemoryMenuRepository`) và `interface OrderRepository` (`InMemoryOrderRepository`, `FileOrderRepository` dùng Java NIO). |
| `service` | `interface OrderService` và lớp thực thi `RestaurantService` xử lý nghiệp vụ đặt món, chuyển trạng thái và tính doanh thu. |
| `controller` | `interface WebController` cùng `CustomerController`, `KitchenController` và `HttpSupport` quản lý cookie giỏ hàng (giới hạn LRU), kiểm tra quyền bếp và ánh xạ mã lỗi HTTP. |
| `view` & `resources/assets` | Tách biệt hoàn toàn hiển thị HTML/CSS/JS khỏi tầng `model` (chống XSS, định dạng tiền/thời gian, tự động sinh danh mục từ `MenuCategory`). |

## Phân công và nghiệm thu

| Thành viên | Phụ trách | Kịch bản nghiệm thu |
|---|---|---|
| 1 | `model` người dùng, thực đơn đa hình (`MenuItem` → `ChickenDish`, `SideDish`, `Beverage`), `MenuCategory` và cây `exception` | Giải thích kế thừa/đa hình trên `User` và `MenuItem`. Tạo đối tượng hợp lệ và kiểm tra ném ngoại lệ khi dữ liệu/SĐT sai. |
| 2 | `CartSession`, `repository` (`InMemory` & `FileOrderRepository`) và `OrderService` | Thêm, giảm, xóa món, `snapshot`/`drainItems`, kiểm tra giỏ trống và thử nghiệm chuyển đổi đa hình giữa lưu RAM và lưu tệp `--file`. |
| 3 | Trang khách (`CustomerView`, `customer.css`, `customer.js`) và `CustomerController` | Dùng cửa sổ thường và cửa sổ ẩn danh để kiểm tra hai giỏ độc lập, lọc danh mục động, giao diện di động và xử lý lỗi đặt đơn. |
| 4 | Trang bếp (`KitchenView`, `kitchen.css`, `kitchen.js`), `KitchenController` và `RestaurantOopTest` | Kiểm tra quyền `canManageKitchen()`, chuyển trạng thái `PENDING → COOKING → READY`, hủy đơn, tính doanh thu và chạy `RestaurantOopTest`. |
