# Hotel Booking System — Pha 1

**Mã nguồn:** https://github.com/trung-2012/hotel-booking-system
**Phiên bản kiểm thử (baseline):** `4d9e6b6` (PR #8)
**API:** [openapi.json](openapi.json) · Swagger UI khi chạy: `http://localhost:8081/swagger-ui/index.html`

## 1. Bài toán và nghiệp vụ

Xây dựng REST API quản lý phòng khách sạn và đặt phòng qua JSON. Khách có thể đăng ký, đăng nhập, xem phòng và quản lý **đơn đặt phòng của chính mình**; quản trị viên (`ADMIN`) được tạo/xóa phòng.

- **Xác thực:** đăng ký email và mật khẩu (tối thiểu 6 ký tự, lưu bằng BCrypt); đăng nhập nhận JWT Bearer Token.
- **Đặt phòng:** chọn `roomId`, `checkInDate`, `checkOutDate`; ngày nhận không ở quá khứ, ngày trả phải sau ngày nhận. `totalPrice = giá phòng × số đêm`.
- **Chống trùng lịch:** không cho hai đơn của cùng phòng giao nhau về thời gian. Được phép nhận phòng đúng ngày khách trước trả phòng.
- **Bảo vệ dữ liệu:** chỉ chủ đơn được xem/hủy đơn; người khác truy cập bằng ID nhận `404 Not Found`.

**Vấn đề còn tồn tại:** kiểm tra lịch trùng và thao tác lưu chưa có khóa giao dịch bảo đảm an toàn khi hai yêu cầu đến đồng thời. Một số lỗi phòng không tồn tại đang bị trả về `500` thay vì `404`; khóa ký JWT còn nằm trong mã nguồn. Đây là các hướng cần xem xét ở Pha 2.

## 2. Kiến trúc và tổ chức mã nguồn

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

Cần **Java 17+**, **Maven 3.8+** và Docker Desktop/Compose. Từ thư mục dự án:

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

1. **Race Condition (Đặt trùng trong cùng 1 mili-giây):**
   Hiện tại, logic kiểm tra trùng lịch (Overbooking) hoạt động tốt ở mức độ tuần tự. Tuy nhiên, nếu 2 request tạo đơn gửi lên server cùng một thời điểm, có thể xảy ra tranh chấp dữ liệu (cả 2 đều đọc thấy phòng trống và cùng lưu thành công). Ở Pha 2, cần bổ sung `@Transactional` kết hợp `Pessimistic Locking` (Khóa bi quan) ở tầng Database để đảm bảo tính nguyên tử (Atomicity) khi đặt phòng đồng thời.

2. **Bảo mật JWT:**
   Khóa ký JWT (`secret`) hiện đang được hardcode trong file `JwtUtil.java`. Để chuẩn bị cho môi trường Production, cần đưa khóa này vào biến môi trường (Environment Variables) hoặc file cấu hình bảo mật, tránh lộ lộ mã nguồn.

*Đồ án học tập môn Kiến trúc Phần mềm — Pha 1.*
