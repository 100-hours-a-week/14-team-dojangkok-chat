import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 1. JSON 데이터 로드 (메모리 효율을 위해 SharedArray 사용)
const testData = new SharedArray('test data', function () {
    return JSON.parse(open('./test-data.json'));
});

// 타겟 서버 URL (환경에 맞게 수정하세요)
const BASE_URL = 'http://localhost:8081';

// 2. 테스트 옵션 설정
export const options = {
    // 초당 10건 정도의 요청을 발생시키기 위한 설정 (10명의 유저가 1초마다 요청)
    vus: 10,
    duration: '30m',

    // (선택) 결과를 보기 좋게 임계치(Thresholds) 설정
    thresholds: {
        http_req_failed: ['rate<0.01'], // 에러율 1% 미만이어야 통과
        http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내에 응답해야 통과
    },
};

// 랜덤 아이템을 뽑는 유틸 함수
function getRandomItem(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// 3. 메인 시나리오 함수 (각 VUser가 반복 실행)
export default function () {
    // JWT 토큰 세팅
    const params = {
        headers: {
            'Authorization': testData[0].token,
            'Content-Type': 'application/json',
        },
    };

    // 난수 생성 (0.0 ~ 1.0)
    const readWriteRand = Math.random();
    const hotColdRand = Math.random();

    // 타겟 방 ID 선택 (80/20 법칙)
    let targetRoomId;
    let isHotData = false;

    if (hotColdRand < 0.8) {
        // 80% 확률로 Hot Data (상위 200개 방) 찌르기
        targetRoomId = getRandomItem(testData[0].hotRoomIds);
        isHotData = true;
    } else {
        // 20% 확률로 Cold Data (하위 800개 방) 찌르기
        targetRoomId = getRandomItem(testData[0].coldRoomIds);
    }

    // 트래픽 분기 (Read 95% / Write 5%)
    if (readWriteRand < 0.95) {
        // ==========================================
        // [READ] 95%: 채팅방 상세 조회 (Cache Hit 테스트)
        // ==========================================
        const url = `${BASE_URL}/api/chat/v3/direct-chat/rooms/${targetRoomId}`;
        const res = http.get(url, params);

        check(res, {
            'is status 200 (GET)': (r) => r.status === 200,
        });

    } else {
        // ==========================================
        // [WRITE] 5%: 캐시 강제 무효화 이벤트 발생
        // ==========================================
        // 주의: 올려주신 ChatRoomController에는 매물 수정(이벤트 발생) API가 없습니다.
        // 실제 프로젝트의 "매물 수정 API"나, 테스트용 "캐시 삭제 API" 경로로 수정해야 합니다.

        const mockUpdateUrl = `${BASE_URL}/api/properties/test/evict-cache/${targetRoomId}`;

        // POST로 이벤트 발생 트리거 (바디가 필요하면 추가)
        const res = http.post(mockUpdateUrl, JSON.stringify({}), params);

        // Write 동작은 결과 확인용으로만 체크
        check(res, {
            'is status 200 (POST - Cache Evict)': (r) => r.status === 200 || r.status === 204,
        });
    }

    // VUser가 너무 미친 듯이 요청을 보내지 않도록 1초 대기 (초당 10건 유지)
    sleep(1);
}