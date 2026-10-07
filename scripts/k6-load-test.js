import http from 'k6/http';
import { check, sleep, fail } from 'k6';

export const options = {
    vus: 10,
    duration: '30s',

    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const EMAIL = __ENV.EMAIL;
const PASSWORD = __ENV.PASSWORD;
const ROOM_ID = Number(__ENV.ROOM_ID || 1);

export function setup() {
    if (!EMAIL || !PASSWORD) {
        fail('EMAIL and PASSWORD environment variables are required');
    }

    const loginPayload = JSON.stringify({
        email: EMAIL,
        password: PASSWORD,
    });

    const loginResponse = http.post(
        `${BASE_URL}/api/auth/login`,
        loginPayload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    const loginSuccess = check(loginResponse, {
        'login status is 200': (res) => res.status === 200,
        'login response contains token': (res) => {
            try {
                return Boolean(res.json('token'));
            } catch (e) {
                return false;
            }
        },
    });

    if (!loginSuccess) {
        fail(`Login failed: ${loginResponse.status} ${loginResponse.body}`);
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

    const bookingPayload = JSON.stringify({
        roomId: ROOM_ID,
        checkInDate: '2026-12-01',
        checkOutDate: '2026-12-03',
    });

    const createResponse = http.post(
        `${BASE_URL}/api/bookings`,
        bookingPayload,
        { headers }
    );

    check(createResponse, {
        'create booking status is 200': (res) => res.status === 200,
    });

    const historyResponse = http.get(
        `${BASE_URL}/api/bookings/me`,
        { headers }
    );

    check(historyResponse, {
        'booking history status is 200': (res) => res.status === 200,
    });

    sleep(1);
}