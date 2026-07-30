import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 100 virtual users
  vus: 100,
  // Running continuously for 1 minute
  duration: '1m',
  thresholds: {
    // 95% of requests should be below 500ms
    http_req_duration: ['p(95)<500'], 
    // Error rate should be less than 1%
    http_req_failed: ['rate<0.01'],   
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:5000';

export default function () {
  // Hit the root endpoint (health check)
  const res = http.get(`${BASE_URL}/`);
  
  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  // Short sleep to simulate real user wait time
  sleep(1);
}
