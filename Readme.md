# Hotel Booking System — Pha 1

**Mã nguồn:** https://github.com/trung-2012/hotel-booking-system
**Phiên bản kiểm thử (baseline):** `4d9e6b6` (PR #8)
**API:** [openapi.json](openapi.json) · Swagger UI khi chạy: `http://localhost:8081/swagger-ui/index.html`

## 1. Bài toán và nghiệp vụ

Hệ thống REST API quản lý đặt phòng khách sạn. Dự án tập trung xử lý các bài toán khó ở tầng Backend: Phân quyền bảo mật, chống trùng lịch (Overbooking) và xử lý tranh chấp dữ liệu đồng thời (Race Condition).

Các tính năng chính bao gồm:
- **Xác thực:** đăng ký email và mật khẩu (tối thiểu 6 ký tự, lưu bằng BCrypt); đăng nhập nhận JWT Bearer Token.
- **Tạo phòng:** đảm báo chỉ có ADMIN được phép tạo/xóa phòng; ADMIN do bản thân hệ thống cấp quyền
- **Đặt phòng:** chọn `roomId`, `checkInDate`, `checkOutDate`; ngày nhận không ở quá khứ, ngày trả phải sau ngày nhận. `totalPrice = giá phòng × số đêm`.
- **Chống trùng lịch:** không cho hai đơn của cùng phòng giao nhau về thời gian. Được phép nhận phòng đúng ngày khách trước trả phòng.
- **Bảo vệ dữ liệu:** chỉ chủ đơn được xem/hủy đơn; người khác truy cập bằng ID nhận `404 Not Found`.
- **Khóa dữ liệu:** Đảm bảo rằng không xảy ra việc 2 khách hàng cùng đặt 1 phòng trong cùng một thời điểm (tránh gây lỗi Database).

## 2. Kiến trúc và tổ chức mã nguồn

Hệ thống được xây dựng theo kiến trúc phần mềm Controller -> Service -> Repository -> Database, cụ thể:

```text
HTTP/JSON → Controller (+ DTO, @Valid, JWT Filter)
          → Service (quy tắc đặt phòng, tính tiền, kiểm tra quyền)
          → Repository (Spring Data JPA) → PostgreSQL
```

```text
src/main/java/com/group/booking/
├── controller/       # AuthController, RoomController, BookingController
├── dto/request/      # DTO đầu vào + Bean Validation
├── dto/response/     # DTO đầu ra
├── service/          # BookingService, RoomService
├── repository/       # BookingRepository, RoomRepository, UserRepository
├── model/            # User, Room, Booking, Role
├── security/         # JwtFilter, JwtUtil
├── config/           # SecurityConfig
└── exception/        # Xử lý lỗi API
```

**Công nghệ:** Java 17 (mục tiêu build), Spring Boot 3.3.4, Spring MVC, Spring Security/JWT, BCrypt, Spring Data JPA/Hibernate, PostgreSQL, Maven, Docker, Springdoc OpenAPI, k6. Cấu trúc hiện tại là **Layered Architecture**; Service vẫn phụ thuộc trực tiếp vào Spring Data repository.

## 3. Xây dựng, chạy và triển khai

Yêu cầu người dùng: có **Java 17+**, **Maven 3.8+** và Docker Desktop/Compose. 
Thao tác cụ thể (từ terminal thư mục dự án):

```bash
git clone https://github.com/trung-2012/hotel-booking-system.git
cd hotel-booking-system
docker compose up -d postgres-db
mvn spring-boot:run
```

API chạy tại `http://localhost:8081`; PostgreSQL mặc định `localhost:5432/booking_db` (`myuser` / `mypassword` — **chỉ dùng môi trường học tập**). Nếu cổng `5432` của máy Windows đã bị chiếm, đổi cổng host trong Compose thành `5433:5432`, rồi thiết lập `$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/booking_db"` ở PowerShell trước khi chạy Maven.

**Đóng gói:** `mvn clean package -DskipTests` tạo `target/booking-0.0.1-SNAPSHOT.jar`; chạy bằng `java -jar target/booking-0.0.1-SNAPSHOT.jar`. Có thể xây dựng image với `docker build -t hotel-booking-api .`, nhưng `docker-compose.yml` hiện **chỉ chạy PostgreSQL**: muốn chạy cả backend trong container phải cung cấp kết nối DB qua mạng Docker và ánh xạ cổng `8081`. `Dockerfile` hiện khai báo `EXPOSE 8080` chưa đồng bộ với cổng ứng dụng `8081`.

## 4. Đặc tả REST API

**Đặc tả đầy đủ có thể preview:** [**openapi.json**](openapi.json) (OpenAPI 3); JSON do ứng dụng sinh tại `http://localhost:8081/v3/api-docs`.

| Method | Endpoint | Quyền | Nghiệp vụ |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký |
| POST | `/api/auth/login` | Public | Đăng nhập, trả JWT |
| GET | `/api/rooms` | Public | Danh sách phòng |
| GET | `/api/rooms/{id}` | Public | Chi tiết phòng |
| POST | `/api/rooms` | ADMIN | Tạo phòng |
| DELETE | `/api/rooms/{id}` | ADMIN | Xóa phòng |
| POST | `/api/bookings` | JWT | Tạo đơn |
| GET | `/api/bookings/me` | JWT | Danh sách đơn của tôi |
| GET | `/api/bookings/{id}` | JWT, chủ đơn | Chi tiết đơn |
| DELETE | `/api/bookings/{id}` | JWT, chủ đơn | Hủy đơn (`204`) |

Ví dụ **POST `/api/bookings`**: `{"roomId":1,"checkInDate":"2027-01-10","checkOutDate":"2027-01-12"}`. Phản hồi gồm `id`, `roomId`, `roomNumber`, `checkInDate`, `checkOutDate`, `totalPrice` (camelCase). Lỗi kiểm tra dữ liệu / đặt trùng trả `400` với dạng `{"error":"..."}`. Các endpoint được bảo vệ sử dụng header `Authorization: Bearer <token>`.

## 5. Kiểm thử và Đánh giá hiệu năng (Local Baseline)

**Chức năng đã kiểm thử thủ công:** Đăng ký/đăng nhập; tạo/xem/hủy đơn; tính tổng tiền; ngăn đặt trùng toàn bộ/một phần; cho phép lịch tiếp nối; chặn xem/hủy đơn của tài khoản khác (`404`); chặn ngày thiếu, ngày quá khứ, ngày không hợp lệ (`400`). `mvn compile`: **BUILD SUCCESS**.

**Baseline Pha 1 (Local Windows):**
- **Môi trường:** Windows Local, OpenJDK 21, PostgreSQL 15 (Docker), k6.
- **Kịch bản:** Script [`scripts/k6-load-test.js`](scripts/k6-load-test.js) giả lập **10 VUs (Virtual Users)** chạy liên tục trong **30 giây**. Mỗi user thực hiện luồng: Đăng nhập → Tạo đơn → Xem lịch sử → Hủy đơn.

| Chỉ số | Kết quả thực tế |
|---|---:|
| HTTP requests / iterations | **847 / 282** |
| Throughput | **27.22 req/s** |
| Trung bình / p95 | **23.55 / 43.54 ms** |
| HTTP failure rate | **0/847 (0.00%)** |
| Checks | **1130/1130 (100%)** |
| Ngưỡng k6 (Thresholds) | **PASS** (`p95 < 1000ms`, lỗi `< 5%`) |

**Kiểm thử Race Condition (Tranh chấp đồng thời):**
- **Kịch bản:** Dùng k6 bắn 10 request đồng thời (cùng 1 tích tắc) đặt cùng 1 phòng, cùng 1 khoảng thời gian.
- **Kết quả:** Hệ thống ghi nhận **1 request thành công (HTTP 200)**, **9 request bị chặn (HTTP 400 - Phòng đã được đặt)**.
- **Phân tích (Blocking):** Thời gian phản hồi trung bình tăng từ `23.55ms` lên `237ms` do các request sau phải xếp hàng chờ (Blocking) request đầu tiên nhả khóa (Unlock).
- **Kết luận:** Cơ chế Khóa bi quan (Pessimistic Lock) hoạt động chính xác, ngăn chặn triệt để lỗi Overbooking do tranh chấp dữ liệu. Tuy nhiên, nó cũng bộc lộ nhược điểm về hiệu năng (gây Blocking) như đã nêu ở Mục 6.

## 6. Hạn chế hiện tại & Hướng phát triển (Pha 2)

Mặc dù hệ thống đã chạy ổn định, đáp ứng đầy đủ nghiệp vụ cốt lõi và vượt qua bài kiểm thử tải, một số điểm kỹ thuật cần tiếp tục tối ưu trong Pha 2:

1. **Chuyển đổi từ Pessimistic Lock sang Optimistic Lock:**
    - *Hiện tại:* Để chống Race Condition, hệ thống dùng `PESSIMISTIC_WRITE` (Khóa bi quan) ở tầng Database. Cơ chế này đảm bảo an toàn tuyệt đối nhưng sẽ khiến các request khác phải **xếp hàng chờ (Blocking)** nếu có quá nhiều người cùng đặt một phòng.
    - *Hướng Pha 2:* Chuyển sang `OPTIMISTIC` (Khóa lạc quan) sử dụng annotation `@Version`. Khi có tranh chấp, hệ thống sẽ ném ra `OptimisticLockException` và Frontend/Client có thể tự động Retry (thử lại) mà không làm nghẽn toàn bộ hệ thống.

2. **Vấn đề N+1 Query (Hiệu năng Database):**
    - *Hiện tại:* Khi gọi API `GET /api/bookings/me`, Hibernate đang thực hiện 1 câu query lấy danh sách Booking, sau đó thực hiện thêm N câu query để lấy thông tin Room cho từng Booking (Lỗi kinh điển N+1 của JPA).
    - *Hướng Pha 2:* Sử dụng `@EntityGraph` hoặc câu lệnh `JOIN FETCH` trong JPQL để gom lại thành 1 câu query duy nhất, giảm tải cho Database.

3. **Thiếu Phân trang (Pagination) cho API danh sách:**
    - *Hiện tại:* API `GET /api/rooms` đang dùng `findAll()`. Nếu khách sạn có 10.000 phòng, server sẽ trả về toàn bộ 10.000 bản ghi trong 1 request, gây tràn RAM và chậm mạng.
    - *Hướng Pha 2:* Áp dụng `Pageable` của Spring Data JPA để phân trang (VD: `GET /api/rooms?page=0&size=10`).

4. **Thiếu API Tìm kiếm phòng trống theo ngày:**
    - *Hiện tại:* Khách hàng mới chỉ có thể xem toàn bộ danh sách phòng (API `/api/rooms`). Họ chưa biết được phòng nào còn trống trong khoảng thời gian mong muốn đặt.
    - *Hướng Pha 2:* Xây dựng API `GET /api/rooms/search?checkIn=...&checkOut=...` kết hợp với câu query `isRoomBooked` hiện có để lọc ra danh sách các phòng thực sự còn trống, nâng cao trải nghiệm người dùng.

*Đồ án học tập môn Kiến trúc Phần mềm — Pha 1.*
