# 선택 미션 API 명세서

사용자가 필터 조건을 선택하여 원하는 미션을 조회하는 API입니다.

선택 미션은 오늘의 미션, 추천 미션에 포함되지 않은 미션 풀에서 필터링하여 제공됩니다.
**하루에 1번만 선택 가능**하며, 이미 선택한 경우 기존 미션을 반환합니다.

## 기본 정보

| 항목 | 내용 |
|------|------|
| Base URL | `/api/v1/missions` |
| 인증 필요 경로 | `/api/v1/missions/**` |

---

## 1. 오늘 선택한 미션 조회

오늘 이미 선택한 미션이 있는지 확인하고, 있으면 미션 정보를 반환합니다.

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Endpoint | `/api/v1/missions/selected/today` |
| 인증 | 필요 |
| 설명 | 오늘 선택한 미션이 있는지 확인합니다. 선택 미션 화면 진입 시 호출하여 이미 선택했는지 확인용으로 사용합니다. |

### Request Header

```
Authorization: Bearer {accessToken}
```

### Response

| 필드 | 타입 | 설명 |
|------|------|------|
| hasSelected | Boolean | 오늘 선택 미션을 조회한 적이 있는지 여부 |
| mission | Object \| null | 선택한 미션 정보 (선택한 적 없으면 null) |
| mission.id | Long | 미션 ID |
| mission.title | String | 미션 제목 |
| mission.description | String | 미션 설명 |
| mission.spaceType | String | 공간 타입 (INDOOR/OUTDOOR) |
| mission.companionType | String | 인원 타입 (SOLO/TOGETHER) |
| mission.categoryType | String | 카테고리 타입 |

### Response 예시

**선택한 미션이 있는 경우:**
```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "result": {
    "hasSelected": true,
    "mission": {
      "id": 42,
      "title": "새소리 들으며 산책하기",
      "description": "자연 속에서 힐링하기",
      "spaceType": "OUTDOOR",
      "companionType": "SOLO",
      "categoryType": "NATURE"
    }
  }
}
```

**선택한 미션이 없는 경우:**
```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "result": {
    "hasSelected": false,
    "mission": null
  }
}
```

---

## 2. 선택 미션 조회 (필터 적용)

필터 조건에 맞는 선택 미션 1개를 반환합니다.

| 항목 | 내용 |
|------|------|
| Method | `POST` |
| Endpoint | `/api/v1/missions/selected` |
| 인증 | 필요 |
| 설명 | 필터 조건에 맞는 선택 미션 1개를 반환합니다. **하루에 1번만 선택 가능**하며, 이미 선택한 경우 기존 미션을 반환합니다. |

### Request Header

```
Authorization: Bearer {accessToken}
```

### Request Parameters (Query String / Form Data)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| spaceType | String | N | 공간 필터 (INDOOR/OUTDOOR) |
| companionType | String | N | 인원 필터 (SOLO/TOGETHER) |
| categoryType | String | N | 카테고리 필터 (FOOD/NATURE/CONTENT/PLACE/MUSIC) |

> **Note:** 모든 필터는 선택사항입니다. 필터를 지정하지 않으면 전체 미션 풀에서 랜덤으로 선택됩니다.

### Request 예시

```
POST /api/v1/missions/selected?spaceType=OUTDOOR&companionType=SOLO&categoryType=NATURE
Authorization: Bearer {accessToken}
```

### Response

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 미션 ID |
| title | String | 미션 제목 (~하기 형식) |
| description | String | 미션 설명 (20자 이내) |
| spaceType | String | 공간 타입 |
| companionType | String | 인원 타입 |
| categoryType | String | 카테고리 타입 |

### Response 예시

```json
{
  "code": "MISSION_201",
  "message": "미션 선택 성공",
  "result": {
    "id": 42,
    "title": "새소리 들으며 산책하기",
    "description": "자연 속에서 힐링하기",
    "spaceType": "OUTDOOR",
    "companionType": "SOLO",
    "categoryType": "NATURE"
  }
}
```

---

## Error

| 상황 | HTTP | code |
|------|------|------|
| 토큰 없음 또는 유효하지 않음 | 401 | `COMMON_401` |
| 필터 조건에 맞는 미션 없음 | 404 | `MISSION_404` |

---

## Enum 정의

### SpaceType

| 값 | 한글 | 설명 |
|------|------|------|
| `INDOOR` | 실내 | 실내에서 수행하는 미션 |
| `OUTDOOR` | 실외 | 실외에서 수행하는 미션 |

### CompanionType

| 값 | 한글 | 설명 |
|------|------|------|
| `SOLO` | 혼자 | 혼자서 할 수 있는 미션 |
| `TOGETHER` | 같이 | 다른 사람과 함께하면 더 좋은 미션 |

### CategoryType

| 값 | 한글 | 설명 |
|------|------|------|
| `FOOD` | 음식 | 제철 음식, 요리 관련 미션 |
| `NATURE` | 자연 | 자연 감상, 야외 활동 미션 |
| `CONTENT` | 컨텐츠 | 사진, 일기 등 기록/콘텐츠 미션 |
| `PLACE` | 장소 | 특정 장소 방문 미션 |
| `MUSIC` | 음악 | 음악 관련 미션 |

---

## 선택 미션 규칙

1. **하루 1회 제한**: 선택 미션은 하루에 1번만 선택할 수 있습니다.
2. **중복 방지**: 이미 오늘 선택한 경우, 같은 미션을 반환합니다.
3. **미션 풀**: 오늘의 미션, 추천 미션에 포함되지 않은 미션에서 선택됩니다.
4. **랜덤 선택**: 필터 조건에 맞는 미션 중 랜덤으로 1개가 선택됩니다.

---

## 클라이언트 플로우

```
1. 선택 미션 화면 진입
   └── GET /api/v1/missions/selected/today 호출
       ├── hasSelected = true → 기존 미션 표시
       └── hasSelected = false → 필터 선택 UI 표시

2. 필터 선택 후 "미션 받기" 버튼 클릭
   └── POST /api/v1/missions/selected 호출
       └── 선택된 미션 표시
```