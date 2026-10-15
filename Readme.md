# Room Booking System API — Pha 1

Dịch vụ backend quản lý và đặt phòng khách sạn. Mỗi tài khoản người dùng chỉ xem, quản lý và hủy các đơn đặt phòng do chính mình tạo ra. API giao tiếp hoàn toàn bằng JSON; giao diện tài liệu Swagger UI tự động tại `/swagger-ui/index.html`, đặc tả OpenAPI JSON tại `/v3/api-docs` và đặc tả tĩnh trong [`openapi.json`](openapi.json).

Bài toán đặt ra là xây dựng một dịch vụ backend cho phép người dùng xem danh sách phòng trống, tạo đơn đặt phòng theo khoảng thời gian, xem lịch sử đặt phòng cá nhân và hủy đơn khi không còn nhu cầu.

Yêu cầu quan trọng nhất là tách biệt dữ liệu giữa các tài khoản và xử lý xung đột lịch đặt phòng (Overbooking). Người dùng chỉ được thao tác trên đơn đặt phòng của mình. Ví dụ, tài khoản A tạo một đơn đặt phòng thì tài khoản B không thể xem hay xóa đơn đó, kể cả khi biết ID (hệ thống sẽ trả về lỗi `404 Not Found`).

---

## Phạm vi nghiệp vụ

- Đăng ký bằng email và mật khẩu; email không phân biệt hoa thường, không trùng lặp.
- Đăng nhập nhận Bearer Token (JWT).
- Xem danh sách phòng, xem chi tiết thông tin từng phòng.
- Tạo đơn đặt phòng (`POST`), xem chi tiết đơn (`GET`), xem danh sách đơn đã đặt (`GET`) và hủy đơn đặt phòng (`DELETE`).
- Kiểm tra trùng lịch: Không cho phép đặt trùng phòng trong cùng khoảng thời gian (`check_in_date` đến `check_out_date`). (chx thực hiện)
- Sắp xếp danh sách đơn đặt phòng theo ID giảm dần (mới nhất xếp trước).
- Truy cập đơn đặt phòng của người khác trả về lỗi `404 Not Found` (tránh lộ sự tồn tại của dữ liệu).
- Pha 1 chưa hỗ trợ thanh toán trực tuyến, đổi lịch, mã giảm giá hay tự động làm mới token (Refresh Token).

---

## Kiến trúc phân tầng (Layered Architecture)

```text
HTTP / JSON (Presentation: Controller, DTO, JWT Filter)
       ↓
Nghiệp vụ (Domain: BookingService, RoomService, Pure Java Models, Interfaces)
       ↓ (Thông qua các Repository Interfaces)
Truy cập dữ liệu (Infrastructure: Spring Data JPA, Entity, Database)
       ↓
Spring Data JPA (Hibernate) → PostgreSQL / H2 Database
```
---
## Chạy cục bộ (Local Development)

Yêu cầu Java 17+ và Maven 3.8+. Từ thư mục gốc dự án:

```bash
# 1. Clone dự án và truy cập thư mục
git clone https://github.com/trung-2012/hotel-booking-system
cd hotel-booking-system

# 2. Thiết lập biến môi trường
export JWT_SECRET="chuoi-bi-mat-ngau-nhien-it-nhat-32-ky-tu-cho-jwt"

# 3. Build dự án
mvn clean package -DskipTests

# 4. Khởi chạy ứng dụng
java -jar target/booking-system-1.0.0.jar
```

*Trên Windows (PowerShell):*
```powershell
$env:JWT_SECRET="chuoi-bi-mat-ngau-nhien-it-nhat-32-ky-tu-cho-jwt"
mvn clean package -DskipTests
java -jar target/booking-system-1.0.0.jar
```

Mặc định ứng dụng sử dụng CSDL H2 In-Memory chạy trên port `8080`. Bạn có thể cấu hình chuyển sang PostgreSQL bằng cách đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, và `SPRING_DATASOURCE_PASSWORD`. Biến `JWT_SECRET` bắt buộc phải có độ dài tối thiểu 32 ký tự.

---

## Đóng gói & Chạy bằng Docker

```bash
# Build Docker image
docker build -t hotel-booking-api .

# Chạy container độc lập
docker run --rm -p 8080:8080 \
  -e JWT_SECRET="chuoi-bi-mat-ngau-nhien-it-nhat-32-ky-tu-cho-jwt" \
  hotel-booking-api
```

Hoặc sử dụng **Docker Compose** (kèm PostgreSQL):

```bash
docker compose up -d --build
```

Mở trình duyệt truy cập Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

---

## Đặc tả Endpoint (API Specification)

Tất cả Request Body đều sử dụng `Content-Type: application/json`. Các endpoint yêu cầu bảo mật phải kèm Header `Authorization: Bearer <access_token>`.

## 🔌 Đặc tả Endpoint (API Specification)

### Authentication & User
| Method | Đường dẫn | Auth | Nội dung |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/register` | Public | Đăng ký tài khoản mới |
| **POST** | `/api/auth/login` | Public | Đăng nhập, trả về JWT Token |
| **GET** | `/api/auth/me` | Bearer Token | Lấy thông tin user hiện tại |

### Room Management
| Method | Đường dẫn | Auth | Nội dung |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/rooms` | Public | Lấy danh sách tất cả các phòng |
| **GET** | `/api/rooms/{id}` | Public | Lấy chi tiết 1 phòng |
| **POST** | `/api/rooms` | Bearer Token (Admin) | Thêm phòng mới |
| **DELETE** | `/api/rooms/{id}` | Bearer Token (Admin) | Xóa phòng theo id |

### Booking Management
| Method | Đường dẫn | Auth | Nội dung |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/bookings` | Bearer Token | Đặt phòng mới |
| **GET** | `/api/bookings/me` | Bearer Token | Xem lịch sử đặt phòng của bản thân |
| **GET** | `/api/bookings/{id}` | Bearer Token | Xem chi tiết một đơn đặt phòng |
| **DELETE** | `/api/bookings/{id}` | Bearer Token | Hủy đơn đặt phòng |
### Cấu trúc một đối tượng Booking JSON
```json
{
  "id": 10,
  "user_id": 1,
  "room_id": 101,
  "room_number": "P.302",
  "check_in_date": "2026-11-01",
  "check_out_date": "2026-11-04",
  "total_price": 1500000.0,
  "status": "CONFIRMED",
  "created_at": "2026-10-15T07:00:00Z"
}
```

Thời gian `created_at` là UTC. Lỗi nghiệp vụ trả về dạng `{"detail":"..."}`.

---

## Kiểm thử & Benchmark trên Kaggle CPU

### 1. Kiểm thử Unit/Integration Test
```bash
mvn test
```

### 2. Kịch bản Benchmark trên Kaggle CPU
1. Tạo một Kaggle Notebook mới với phần cứng **CPU (Accelerator: None)**.
2. Upload file JAR đã build hoặc kéo mã nguồn từ GitHub về Notebook.
3. Khởi động ứng dụng backend dưới dạng tiến trình ngầm:
   ```python
   import subprocess, os
   env = {**os.environ, "JWT_SECRET": "secret-key-super-secure-32-characters-min"}
   subprocess.Popen(["java", "-jar", "app.jar"], env=env)
   ```
4. Cài đặt công cụ k6 hoặc Locust trên Python Notebook:
   ```bash
   pip install locust
   ```
5. Chạy file script test tải `benchmark.py` với các mức luồng đồng thời tăng dần:
   - `--concurrency 1` (Đơn luồng)
   - `--concurrency 4` (Tải nhẹ)
   - `--concurrency 8` (Tải cao trên Kaggle CPU)
6. Script sẽ tự động đăng ký tài khoản test, sau đó lặp lại luồng: `POST (Đặt phòng)` $\rightarrow$ `GET (Xem danh sách đơn)` $\rightarrow$ `DELETE (Hủy đơn)`.
7. Ghi lại các thông số: Throughput (RPS), độ trễ p50/p95 và tỷ lệ lỗi HTTP để phục vụ báo cáo So sánh cho Pha 2.

---

## Kế hoạch Cải tiến ở Pha 2
...
---
*Dự án phục vụ mục đích học tập môn Kiến trúc Phần mềm.*