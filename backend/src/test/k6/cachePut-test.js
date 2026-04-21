import http from 'k6/http';
import { sleep, check } from 'k6';

// -------------------------------------------------------
// [SETUP] 테스트 시작 전 1회 실행 - 로그인해서 토큰 발급
// -------------------------------------------------------
export function setup() {
    const loginRes = http.post(
        'http://localhost:8080/api/v1/auth/login',
        JSON.stringify({
            email: 'client2@test.com',
            password: '12345678',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(loginRes, {
        '로그인 성공': (r) => r.status === 200,
    });

    const token = loginRes.headers['Authorization'].replace('Bearer ', '');
    console.log(`[SETUP] 로그인 성공, 토큰 발급 완료`);

    return { token };
}

// -------------------------------------------------------
// [OPTIONS] 테스트 시나리오 설정
// CacheEvict 테스트와 동일한 조건으로 공정한 비교
// -------------------------------------------------------
export const options = {
    stages: [
        { duration: '20s', target: 100 }, // Ramp Up: 0 → 100명 (서버 워밍업)
        { duration: '1m',  target: 500 }, // 정합성 테스트 구간: 500명 유지
        { duration: '20s', target: 0 },   // Ramp Down: 종료
    ],
    thresholds: {
        http_req_failed: ['rate < 0.01'],
        'http_req_duration{type:get}':    ['p(95)<100'],   // 단건 조회 p(95) 100ms 미만
        'http_req_duration{type:update}': ['p(95)<3000'],  // 수정 요청 p(95) 3000ms 미만
    },
};

// -------------------------------------------------------
// [DEFAULT] VU마다 반복 실행되는 메인 함수
// -------------------------------------------------------
export default function (data) {
    const token = data.token;

    // client2 소유 프로젝트 ID 목록
    const client2ProjectIds = [3, 7, 8, 9, 10, 11, 13, 15, 18, 19];
    const projectId = client2ProjectIds[
        Math.floor(Math.random() * client2ProjectIds.length)
        ];

    // --- [STEP 1: 단건 조회 (READ)] ---
    const getRes = http.get(
        `http://localhost:8080/api/v1/projects/${projectId}`,
        {
            tags: { type: 'get' },
        }
    );

    check(getRes, {
        'get status is 200': (r) => r.status === 200,
        // 수정이 발생했다면 응답에 "CachePut Test" 문구가 즉시 반영되어야 함
        // @CachePut은 수정과 동시에 캐시를 갱신하므로
        // Cache Miss 공백 없이 바로 최신 데이터 반환
        '캐시 즉시 갱신 확인 (CachePut Test 포함 여부)': (r) =>
            r.body.includes('CachePut Test'),
    });

    // --- [STEP 2: 수정 요청 (UPDATE) - 10번 중 1번만 실행] ---
    // CacheEvict 테스트와 동일한 비율 (조회 9 : 수정 1)
    if (__ITER % 10 === 0) {
        const updateRes = http.put(
            `http://localhost:8080/api/v1/projects/${projectId}`,
            JSON.stringify({
                title: `CachePut Test ${Date.now()}`, // CacheEvict의 "Update Test"와 구분
                description: '캐시 CachePut 테스트 중입니다.',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                tags: { type: 'update' },
            }
        );

        check(updateRes, {
            'update status is 200': (r) =>
                r.status === 200 || r.status === 204,
        });

        // 수정 실패 시 로그
        if (updateRes.status !== 200 && updateRes.status !== 204) {
            console.log(
                `[UPDATE FAIL] status: ${updateRes.status}, body: ${updateRes.body}`
            );
        }

        console.log(
            `[UPDATE] projectId: ${projectId} 수정 발생! (ITER: ${__ITER})`
        );

        // --- [STEP 3: 수정 직후 즉시 단건 조회] ---
        // @CachePut의 핵심 검증 포인트
        // 수정과 동시에 캐시가 갱신됐으므로
        // 수정 직후 조회에서 즉시 최신 데이터가 반환되어야 함
        const immediateGetRes = http.get(
            `http://localhost:8080/api/v1/projects/${projectId}`,
            {
                tags: { type: 'get' },
            }
        );

        check(immediateGetRes, {
            '수정 직후 즉시 반영 확인': (r) =>
                r.body.includes('CachePut Test'),
        });

        if (immediateGetRes.body.includes('CachePut Test')) {
            console.log(`[CACHE HIT] projectId: ${projectId} 수정 직후 즉시 반영 확인! ✅`);
        } else {
            console.log(`[CACHE MISS] projectId: ${projectId} 수정 직후 반영 안됨 ❌`);
        }
    }

    sleep(1);
}