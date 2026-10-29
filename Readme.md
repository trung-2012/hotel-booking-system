# Hotel Booking System — Pha 1

**Mã nguồn:** https://github.com/trung-2012/hotel-booking-system
**Phiên bản kiểm thử (baseline):** `4d9e6b6` (PR #8)
**API:** [openapi.json](openapi.json) · Swagger UI khi chạy: `http://localhost:8081/swagger-ui/index.html`

## 1. Bài toán và nghiệp vụ

Xây dựng hệ thống đặt phòng khách sạn qua json. Khách có thể đăng ký, đăng nhập, xem và quản lý đơn đặt phòng của mình; Quản trị viên được tạo/xóa phòng.

Các tính năng chính bao gồm:
- **Xác thực:** đăng ký email và mật khẩu (tối thiểu 6 ký tự, lưu bằng BCrypt); đăng nhập nhận JWT Bearer Token.
- **Tạo phòng:** đảm báo chỉ có ADMIN được phép tạo/xóa phòng; ADMIN do bản thân hệ thống cấp quyền
- **Đặt phòng:** chọn `roomId`, `checkInDate`, `checkOutDate`; ngày nhận không ở quá khứ, ngày trả phải sau ngày nhận. `totalPrice = giá phòng × số đêm`.
- **Chống trùng lịch:** không cho hai đơn của cùng phòng giao nhau về thời gian. Được phép nhận phòng đúng ngày khách trước trả phòng.
- **Bảo vệ dữ liệu:** chỉ chủ đơn được xem/hủy đơn; người khác truy cập bằng ID nhận `404 Not Found`.
- **Khóa dữ liệu:** đảm bảo rằng không xảy ra việc 2 phòng đặt cùng 1 thời điểm và trùng thời gian gẫy lỗi database.

## 2. Kiến trúc và tổ chức mã nguồn

Hệ thống được xây dựng theo kiến trúc phần mềm Detabase -> Service -> Controller, cụ thể:

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

## 5. Kiểm thử và Kaggle CPU Baseline

**Chức năng đã kiểm thử thủ công:** đăng ký/đăng nhập; tạo/xem/hủy đơn; tính tổng tiền; ngăn đặt trùng toàn bộ/một phần; cho phép lịch tiếp nối; chặn xem/hủy đơn của tài khoản khác (`404`); chặn ngày thiếu, ngày quá khứ, ngày không hợp lệ (`400`). `mvn compile`: **BUILD SUCCESS**.

**Baseline Pha 1:** Kaggle Notebook **4 CPU cores**, Accelerator `None`; OpenJDK 21, PostgreSQL 16, k6 v2.2.0; backend và công cụ tạo tải trên **cùng máy**. Script: [`scripts/k6-load-test.js`](scripts/k6-load-test.js), **10 VUs / 30 giây**, luồng **tạo → xem → hủy**; kiểm thử trên commit `4d9e6b6`.

| Chỉ số | Kết quả |
|---|---:|
| HTTP requests / iterations | **850 / 283** |
| Throughput | **27.30 req/s** |
| Trung bình / p95 | **24.07 / 73.62 ms** |
| HTTP failure rate | **0/850 (0.00%)** |
| Checks | **1.134/1.134 (100%)** |
| Ngưỡng k6 | **PASS** (`p95 < 1000ms`, lỗi `< 5%`) |

**Bằng chứng:** Kaggle Notebook đã lưu Version #1 và gói `phase1_kaggle_baseline.zip` gồm log, JSON kết quả, mô tả môi trường, báo cáo tóm tắt (lưu riêng). Kết quả này **chỉ là baseline Pha 1**, không phải bằng chứng cải thiện so với Windows hay bảo đảm xử lý an toàn các yêu cầu đặt trùng đồng thời. Pha 2 cần đo trước/sau trên môi trường, cấu hình tải và dữ liệu so sánh tương đương.

## 7. Hạn chế hiện tại & Hướng phát triển (Pha 2)

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

4. **Chưa có Caching và Docker hóa toàn diện:**
    - *Hiện tại:* API tìm phòng trống (`/api/rooms/search`) liên tục truy vấn vào Database. Docker mới chỉ chạy PostgreSQL mà chưa đóng gói ứng dụng Spring Boot.
    - *Hướng Pha 2:*
        + Tích hợp **Redis Cache** cho các API ít thay đổi (như danh sách phòng) để giảm tải cho PostgreSQL.
        + Viết `Dockerfile` cho Spring Boot và cấu hình `docker-compose` chạy song song cả App lẫn Database, giúp deploy bằng 1 câu lệnh duy nhất.
*Đồ án học tập môn Kiến trúc Phần mềm — Pha 1.*
