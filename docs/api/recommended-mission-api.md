# 추천 미션 API 명세서

사용자의 온보딩 정보(UserType, EnjoyType 우선순위)를 기반으로 현재 절기의 추천 미션 목록을 반환하는 API입니다.

카테고리 필터링은 클라이언트에서 처리하며, 서버는 EnjoyType 우선순위 순서로 정렬된 전체 목록을 내려줍니다.

## 기본 정보

| 항목 | 내용 |
|------|------|
| Base URL | `/api/v1/missions` |
| 인증 필요 경로 | `/api/v1/missions/**` |

---

## 추천 미션 조회

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Endpoint | `/api/v1/missions/recommended` |
| 인증 | 필요 |
| 설명 | 온보딩에서 저장된 UserType과 EnjoyType 우선순위를 기반으로 현재 절기의 추천 미션을 조회합니다. |

### Request Header

```
Authorization: Bearer {accessToken}
```

### Response

| 필드 | 타입 | 설명 |
|------|------|------|
| userType | String | 사용자 타입 (Enum) |
| solarTerm | String | 현재 절기 이름 (예: "소만") |
| missions | Array | 추천 미션 목록 (최대 15개, EnjoyType 우선순위 순 정렬) |
| missions[].id | Long | 미션 ID |
| missions[].title | String | 미션 제목 (~하기 형식) |
| missions[].categoryType | String | 카테고리 타입 (Enum, 클라이언트 필터링용) |
| missions[].enjoyType | String | EnjoyType (Enum) |
| missions[].order | Int | 우선순위 기반 표시 순서 (1부터 시작) |
| missions[].isCompleted | Boolean | 미션 완료 여부 |

### Response 예시

```json
{
  "code": "MISSION_200_REC",
  "message": "추천 미션 조회 성공",
  "result": {
    "userType": "EXPLORER",
    "solarTerm": "소만",
    "missions": [
      {
        "id": 12,
        "title": "나만의 여름 음료 만들기",
        "categoryType": "FOOD",
        "enjoyType": "SEASONAL_FOOD",
        "order": 1,
        "isCompleted": false
      },
      {
        "id": 15,
        "title": "여름 플레이리스트 만들기",
        "categoryType": "MUSIC",
        "enjoyType": "CULTURE_CONTENT",
        "order": 2,
        "isCompleted": true
      },
      {
        "id": 18,
        "title": "초록빛 풍경 사진 찍기",
        "categoryType": "CONTENT",
        "enjoyType": "NATURE_OUTDOOR",
        "order": 3,
        "isCompleted": false
      }
    ]
  }
}
```

> **Note:** 절기가 설정되지 않은 경우 `missions`는 빈 배열(`[]`)로 반환됩니다. 에러가 아닙니다.

---

## Error

| 상황 | HTTP | code |
|------|------|------|
| 온보딩 정보 없음 | 404 | `USER_404_ONBOARD` |
| 토큰 없음 또는 유효하지 않음 | 401 | `COMMON_401` |

---

## Enum 정의

### CategoryType

| 값 | 한글 | 설명 |
|------|------|------|
| `FOOD` | 음식 | 제철 음식, 요리 관련 미션 |
| `NATURE` | 자연 | 자연 감상, 야외 활동 미션 |
| `CONTENT` | 컨텐츠 | 사진, 일기 등 기록/콘텐츠 미션 |
| `PLACE` | 장소 | 특정 장소 방문 미션 |
| `MUSIC` | 음악 | 음악 관련 미션 |

### EnjoyType

| 값 | 한글 | 설명 |
|------|------|------|
| `NATURE_OUTDOOR` | 자연/야외 | 자연이나 야외에서 즐기는 활동 |
| `SEASONAL_FOOD` | 제철음식 | 제철 음식을 맛보거나 요리하는 활동 |
| `CULTURE_CONTENT` | 감성콘텐츠 | 감성적인 콘텐츠나 문화 활동 |

---

## 정렬 방식

미션 목록은 온보딩 3번 질문에서 사용자가 설정한 **EnjoyType 우선순위 순서**로 그룹 정렬됩니다.

같은 EnjoyType 그룹 내에서는 Admin에서 동기화 시 설정된 `displayOrder` 기준으로 정렬됩니다.

### 예시

사용자 설정:
- `enjoyTypeFirst` = `SEASONAL_FOOD`
- `enjoyTypeSecond` = `NATURE_OUTDOOR`
- `enjoyTypeThird` = `CULTURE_CONTENT`

결과:
| 순서 | EnjoyType | order |
|------|-----------|-------|
| 1~5 | SEASONAL_FOOD | 1~5 |
| 6~10 | NATURE_OUTDOOR | 6~10 |
| 11~15 | CULTURE_CONTENT | 11~15 |