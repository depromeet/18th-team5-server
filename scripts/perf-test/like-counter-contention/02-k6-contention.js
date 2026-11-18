// =============================================================
// 좋아요 카운터 행 락 경합 부하 — k6
//
// 고정 completion(HOT) 하나에 "서로 다른" userId로 좋아요를 1000 TPS로 퍼붓는다.
// 매 반복 유일 userId(iterationInTest)라 전부 신규 멤버십 INSERT + 핫-행 카운터 UPDATE를
// 타므로 그 행의 행 락에서 직렬화가 최대로 드러난다. (같은 유저면 유니크 제약에 걸려
// 카운터 UPDATE가 안 일어나 경합이 사라진다 — 00-README 참고)
//
// 실행:
//   k6 run 02-k6-contention.js
//   k6 run -e RPS=1500 -e DURATION=90s -e HOT_ID=8000001 02-k6-contention.js
// =============================================================

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const HOT = __ENV.HOT_ID || '8000001';
const USER_BASE = Number(__ENV.USER_BASE || 1); // userId 시작 오프셋

export const options = {
    scenarios: {
        like_storm: {
            executor: 'constant-arrival-rate', // 서버 속도와 무관하게 목표 도착률 고정
            rate: Number(__ENV.RPS || 1000),   // 초당 요청 수 (목표 TPS)
            timeUnit: '1s',
            duration: __ENV.DURATION || '60s',
            preAllocatedVUs: Number(__ENV.VUS || 200),
            maxVUs: Number(__ENV.MAX_VUS || 500), // 경합으로 지연 상승 시 동원할 VU 상한
        },
    },
    // 경합을 보려는 실험이라 임계값으로 실패시키지 않고 수치만 관측한다.
    // dropped_iterations가 0보다 크면 = 목표 TPS를 못 맞춤 = 경합 천장에 닿았다는 신호.
};

export default function () {
    // 전역 반복 인덱스 → 매 요청 유일 userId → 모두 신규 좋아요(카운터 UPDATE 발생)
    const userId = USER_BASE + exec.scenario.iterationInTest;
    const res = http.post(`${BASE}/loadtest/feed/${HOT}/like?userId=${userId}`);
    check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
}