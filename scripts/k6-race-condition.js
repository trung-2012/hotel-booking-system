import http from 'k6/http';
import { check, fail } from 'k6';

export const options = {
    vus: 10,
    iterations: 10,
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const EMAIL = __ENV.EMAIL;
const PASSWORD = __ENV.PASSWORD;
const ROOM_ID = Number(__ENV.ROOM_ID || 1);

export function setup() {
    if (!EMAIL || !PASSWORD) {
        fail('EMAIL and PASSWORD are required');
    }

    const loginPayload = JSON.stringify({
        email: EMAIL,
        password: PASSWORD,
    });

    const loginResponse = http.post(
        `${BASE_URL}/api/auth/login`,
        loginPayload,
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (loginResponse.status !== 200) {
        fail(`Login failed: HTTP ${loginResponse.status}`);
    }

    return {
        token: loginResponse.json('token'),
    };
}

export default function (data) {
    const headers = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.token}`,
    };

    // TẤT CẢ 10 USERS ĐỀU ĐẶT CÙNG 1 PHÒNG, CÙNG 1 NGÀY
    const payload = JSON.stringify({
        roomId: ROOM_ID,
        checkInDate: '2027-12-01', // Bạn có thể đổi ngày nếu cần
        checkOutDate: '2027-12-05',
    });

    const res = http.post(`${BASE_URL}/api/bookings`, payload, { headers });

    // In ra mã HTTP để xem request nào thành công, request nào bị chặn
    console.log(`Status: ${res.status} - Body: ${res.body}`);
}