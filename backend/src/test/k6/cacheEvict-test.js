import http from 'k6/http';
import { sleep, check } from 'k6';

// -------------------------------------------------------
// [SETUP] 테스트 시작 전 1회 실행 - 로그인해서 토큰 발급
// -------------------------------------------------------
// CacheEvict 테스트 (검색 API)
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

    // accessToken은 응답 헤더 Authorization에서 꺼냄
    // "Bearer eyJhbGci..." → "eyJhbGci..." 로 Bearer 제거
    const token = loginRes.headers['Authorization'].replace('Bearer ', '');

    console.log(`[SETUP] 로그인 성공, 토큰 발급 완료`);

    return { token };
}

// -------------------------------------------------------
// [OPTIONS] 테스트 시나리오 설정
// -------------------------------------------------------
export const options = {
    stages: [
        { duration: '20s', target: 100 }, // Ramp Up: 0 → 100명 (서버 워밍업)
        { duration: '1m',  target: 500 }, // 정합성 테스트 구간: 500명 유지
                                          // 이 구간에서 Evict로 인한 캐시 삭제 + 재생성 반복
        { duration: '20s', target: 0 },   // Ramp Down: 종료
    ],
    thresholds: {
        http_req_failed: ['rate < 0.01'], // 전체 실패율 1% 미만 유지
        'http_req_duration{type:search}': ['p(95)<100'],  // 검색 요청 p(95) 100ms 미만
        'http_req_duration{type:update}': ['p(95)<3000'], // 수정 요청 p(95) 3000ms 미만
                                                          // (Evict 후 Cache Miss → DB 재조회 감안)
    },
};

// -------------------------------------------------------
// [DEFAULT] VU마다 반복 실행되는 메인 함수
// -------------------------------------------------------
export default function (data) {
    const token = data.token;
    const keywords = ['Java', 'Spring']; // 키워드 2개로 좁혀서 Cache Hit율 높임
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    // --- [STEP 1: 조회 요청 (READ)] ---
    const searchRes = http.get(
        `http://localhost:8080/api/v2/search?keyword=${keyword}`,
        {
            tags: { type: 'search' }, // thresholds에서 search 요청만 따로 측정
        }
    );

    check(searchRes, {
        'search status is 200': (r) => r.status === 200,
        '캐시 갱신 확인 (Update Test 포함 여부)': (r) =>
            r.body.includes('Update Test'),
    });

    // --- [STEP 2: 수정 요청 (UPDATE) - 10번 중 1번만 실행] ---
    // 실제 서비스에서 조회(9) : 수정(1) 비율을 시뮬레이션
    if (__ITER % 10 === 0) {
        // client2 소유 프로젝트 ID만 사용 → 403 방지
        const client2ProjectIds = [3, 7, 8, 9, 10, 11, 13, 15, 18, 19];
        const projectId = client2ProjectIds[
            Math.floor(Math.random() * client2ProjectIds.length)
            ];
        const updateRes = http.put(
            `http://localhost:8080/api/v1/projects/${projectId}`,
            JSON.stringify({
                title: `Update Test ${Date.now()}`, // 매번 다른 제목 → 정합성 확인용
                description: '캐시 Evict 테스트 중입니다.',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                tags: { type: 'update' }, // thresholds에서 update 요청만 따로 측정
            }
        );

        check(updateRes, {
            'update status is 200': (r) =>
                r.status === 200 || r.status === 204,
        });

        console.log(
            `[UPDATE] projectId: ${projectId} 수정 발생! (keyword: ${keyword}, ITER: ${__ITER})`
        );
    }

    sleep(1);
}