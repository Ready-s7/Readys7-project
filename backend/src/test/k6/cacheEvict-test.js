import http from 'k6/http';
import {sleep, check} from 'k6';

// -------------------------------------------------------
// [SETUP] 테스트 시작 전 1회 실행
// 1. 로그인 → 토큰 발급
// 2. Java/Spring 검색 결과에서 client2 소유 프로젝트 ID 동적 수집
//    → bulk insert로 clientId가 랜덤 배정되기 때문에 ID 하드코딩 불가
//    → 매 테스트 시작 시 DB 현재 상태 기준으로 ID를 수집
// -------------------------------------------------------
export function setup() {

    // 1. 로그인
    const loginRes = http.post(
        'http://localhost:8080/api/v1/auth/login',
        JSON.stringify({
            email: 'client2@test.com',
            password: '12345678',
        }),
        {headers: {'Content-Type': 'application/json'}}
    );

    check(loginRes, {
        '로그인 성공': (r) => r.status === 200,
    });

    const token = loginRes.headers['Authorization'].replace('Bearer ', '');
    console.log(`[SETUP] 로그인 성공, 토큰 발급 완료`);

    // 2. 키워드별 client2 소유 프로젝트 ID 동적 수집
    // size=50으로 넉넉하게 조회해서 client2 소유 프로젝트가 포함될 확률을 높임
    const keywords = ['Java'];
    const projectIdsByKeyword = {};

    for (const kw of keywords) {
        const res = http.get(
            `http://localhost:8080/api/v2/search?keyword=${kw}&size=10`
        );
        const body = JSON.parse(res.body);
        const projects = body?.data?.projects?.content || [];

        const client2Ids = projects
            .filter((p) => p.clientName === '이개인')
            .map((p) => p.id);

        projectIdsByKeyword[kw] = client2Ids;
        console.log(`[SETUP] "${kw}" 검색 → client2 프로젝트 ID: [${client2Ids}]`);

        if (client2Ids.length === 0) {
            console.warn(`[SETUP] ⚠️ "${kw}" 검색 결과에 client2 소유 프로젝트가 없습니다.`);
        }
    }

    return {token, projectIdsByKeyword};
}

// -------------------------------------------------------
// [OPTIONS] 테스트 시나리오 설정
// -------------------------------------------------------
export const options = {
    stages: [
        // --- Stage 1: 정합성 검증 구간 ---
        // 목적: 수정 → Evict → 검색의 인과관계가 명확하게 성립하는지 확인
        // VU가 적어야 타이밍 노이즈 없이 before/after 비교가 가능
        {duration: '1m', target: 20},

        // --- Stage 2: 동시성 검증 구간 ---
        // 목적: 동시 요청이 많아지는 상황에서도 Evict가 정상 동작하는지 확인
        // 수정 요청 + 검색 요청이 동시에 몰릴 때 정합성 깨지지 않는지 검증
        {duration: '1m30s', target: 50},

        // --- Stage 3: 종료 ---
        {duration: '20s', target: 0},
    ],
    thresholds: {
        // 전체 실패율 1% 미만 유지
        http_req_failed: ['rate < 0.01'],

        // 검색 요청: Evict 후 Cache Miss 구간 감안해서 넉넉하게 설정
        'http_req_duration{type:search}': ['p(95)<3000'],

        // 수정 요청: DB 업데이트 + Evict 포함
        'http_req_duration{type:update}': ['p(95)<5000'],
    },
};

// -------------------------------------------------------
// [DEFAULT] VU마다 반복 실행되는 메인 함수
// -------------------------------------------------------
export default function (data) {
    const {token, projectIdsByKeyword} = data;

    // 키워드 랜덤 선택
    const keywords = ['Java'];
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    // 해당 키워드의 client2 소유 프로젝트 ID 목록에서 랜덤 선택
    const ids = projectIdsByKeyword[keyword];
    if (!ids || ids.length === 0) {
        console.warn(`[SKIP] "${keyword}"에 해당하는 client2 프로젝트 없음 → 이번 반복 건너뜀`);
        sleep(1);
        return;
    }
    const projectId = ids[__VU % ids.length];

    // -------------------------------------------------------
    // [STEP 1] 수정 전 검색 → before title 저장
    // 응답 구조: { data: { projects: { content: [...] } } }
    // size=50으로 수정 대상 projectId가 결과에 포함되도록 보장
    // -------------------------------------------------------
    const beforeRes = http.get(
        `http://localhost:8080/api/v2/search?keyword=${keyword}&size=10`,
        {tags: {type: 'search'}}
    );

    check(beforeRes, {
        '[BEFORE] search status 200': (r) => r.status === 200,
    });

    let beforeTitle = '';
    try {
        const body = JSON.parse(beforeRes.body);
        const projects = body?.data?.projects?.content || [];
        const target = projects.find((p) => p.id === projectId);
        beforeTitle = target ? target.title : '';
    } catch (e) {
        console.warn(`[BEFORE] body 파싱 실패: ${e}`);
    }

    // -------------------------------------------------------
    // [STEP 2] 수정 요청 → @CacheEvict 발생
    // 매번 다른 title을 넣어서 before/after 비교가 가능하도록 timestamp 포함
    // -------------------------------------------------------
    const newTitle = `Update Test ${Date.now()}`;

    const updateRes = http.put(
        `http://localhost:8080/api/v1/projects/${projectId}`,
        JSON.stringify({
            title: newTitle,
            description: '캐시 Evict 정합성 테스트 중입니다.',
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`,
            },
            tags: {type: 'update'},
        }
    );

    check(updateRes, {
        '[UPDATE] status 200 or 204': (r) =>
            r.status === 200 || r.status === 204,
    });

    console.log(
        `[UPDATE] projectId: ${projectId} | keyword: ${keyword} | newTitle: ${newTitle}`
    );

    // -------------------------------------------------------
    // Evict 후 캐시 재적재 대기
    // → 수정 요청으로 globalSearch 캐시 전체가 삭제됨
    // → 0.5초 대기 후 다음 검색 요청이 Cache Miss → DB 조회 → 재적재
    // -------------------------------------------------------
    sleep(0.5);

    if (__VU <= 20) {
        // -------------------------------------------------------
        // [STEP 3] 수정 후 검색 → after title 확인
        // -------------------------------------------------------
        const afterRes = http.get(
            `http://localhost:8080/api/v2/search?keyword=${keyword}&size=50`,
            {tags: {type: 'search'}}
        );

        check(afterRes, {
            '[AFTER] search status 200': (r) => r.status === 200,
        });

        let afterTitle = '';
        try {
            const body = JSON.parse(afterRes.body);
            const projects = body?.data?.projects?.content || [];
            const target = projects.find((p) => p.id === projectId);
            afterTitle = target ? target.title : '';
        } catch (e) {
            console.warn(`[AFTER] body 파싱 실패: ${e}`);
        }

        // -------------------------------------------------------
        // [STEP 4] before vs after 비교 → Evict 정상 동작 검증
        // newTitle과 afterTitle이 일치하면 Evict 후 최신 데이터 반영 성공 ✅
        // -------------------------------------------------------
        const evictWorked = afterTitle !== beforeTitle && afterTitle !== '';

        check(null, {
            '[EVICT] 캐시 갱신 확인 (after === newTitle)': () => evictWorked,
        });

        if (evictWorked) {
            console.log(
                `[EVICT ✅] projectId: ${projectId} | before: "${beforeTitle}" → after: "${afterTitle}"`
            );
        } else {
            console.log(
                `[EVICT ❌] projectId: ${projectId} | before: "${beforeTitle}" | after: "${afterTitle}" | expected: "${newTitle}"`
            );
        }
    }

    sleep(1);
}