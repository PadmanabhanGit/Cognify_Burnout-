import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration for the load test
export const options = {
    // Stage 1: Ramp up to 100 virtual users over 10 seconds
    // Stage 2: Hold at 100 virtual users for 1 minute (as per baseline testing requirements)
    // Stage 3: Ramp down to 0 users over 10 seconds
    stages: [
        { duration: '10s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        // Assertions for response times
        http_req_duration: ['p(95)<500', 'p(99)<1500'], // 95% of requests must complete below 500ms, 99% under 1.5s
        http_req_failed: ['rate<0.01'], // Less than 1% of requests should fail
    },
};

// Target BASE_URL from environment variable or fallback to localhost
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';

export default function () {
    // 1. Test the main health/status endpoint (or dashboard)
    const res = http.get(`${BASE_URL}/api/health`);
    
    // Verify that the endpoint returns 200 OK
    check(res, {
        'is status 200': (r) => r.status === 200,
        'response time is acceptable': (r) => r.timings.duration < 1500,
    });

    // 2. Simulate user reading their burnout data
    const dataRes = http.get(`${BASE_URL}/api/burnout/stats`);
    check(dataRes, {
        'stats returned 200': (r) => r.status === 200,
    });

    // Pause briefly to simulate real user think time before the next request cycle
    sleep(1);
}
