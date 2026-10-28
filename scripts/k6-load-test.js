
import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const VUS = 10;

export const options = {
    vus: VUS,
    duration: '30s',

    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const EMAIL = __ENV.EMAIL;
const PASSWORD = __ENV.PASSWORD;
const ROOM_ID = Number(__ENV.ROOM_ID || 1);
const START_DATE = __ENV.START_DATE || '2028-01-01';

// Return a date after the specified number of days.
function getBookingDate(dayOffset) {
    const date = new Date(`${START_DATE}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + dayOffset);
    return date.toISOString().slice(0, 10);
}

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

    // Allocate a different two-night period to each iteration.
    const dayOffset = (__ITER * VUS + (__VU - 1)) * 2;

    const bookingPayload = JSON.stringify({
        roomId: ROOM_ID,
        checkInDate: getBookingDate(dayOffset),
        checkOutDate: getBookingDate(dayOffset + 2),
    });

    // 1. Create booking
    const createResponse = http.post(
        `${BASE_URL}/api/bookings`,
        bookingPayload,
        { headers }
    );

    let bookingId = null;

    if (createResponse.status === 200) {
        try {
            bookingId = createResponse.json('id');
        } catch (e) {
            bookingId = null;
        }
    }

    check(createResponse, {
        'create booking status is 200': (res) => res.status === 200,
        'booking ID returned': () => bookingId != null,
    });

    // 2. Retrieve booking history
    const historyResponse = http.get(
        `${BASE_URL}/api/bookings/me`,
        { headers }
    );

    check(historyResponse, {
        'booking history status is 200': (res) => res.status === 200,
    });

    // 3. Clean up the newly created booking
    if (bookingId != null) {
        const deleteResponse = http.del(
            `${BASE_URL}/api/bookings/${bookingId}`,
            null,
            { headers }
        );

        check(deleteResponse, {
            'cancel booking status is 204': (res) => res.status === 204,
        });
    }

    sleep(1);
}
