# Mission API 문서

## 변경 이력
- 2026-05-30: API 추가 및 수정

---

## 1. 홈 화면

### 홈 메인 카드 조회
- **URL**: `GET /api/v1/home/card`
- **인증**: 필요
- **설명**: 현재 절기 정보와 오늘의 미션 조회

**Response**
```json
{
  "code": "HOME_FOUND",
  "message": "홈 조회 성공",
  "data": {
    "solarTerm": {
      "id": 1,
      "name": "입하",
      "description": "여름의 시작",
      "startDate": "2026-05-05",
      "endDate": "2026-05-20"
    },
    "dailyMission": {
      "id": 1,
      "title": "나만의 여름 음료 개발",
      "participantCount": 42,
      "missionType": "DAILY",
      "isCompleted": false
    }
  }
}
```

---

## 2. 선택 미션

### 오늘 선택한 미션 조회
- **URL**: `GET /api/v1/missions/selected/today`
- **인증**: 필요
- **설명**: 오늘 선택한 미션이 있는지 확인하고, 있으면 미션 정보 반환

**Response**
```json
{
  "code": "MISSION_FOUND",
  "data": {
    "hasSelected": true,
    "mission": {
      "id": 1,
      "title": "봄나물 캐러 가기",
      "description": "가까운 산이나 들에서 봄나물을 캐보세요",
      "spaceType": "OUTDOOR",
      "companionType": "SOLO",
      "categoryType": "NATURE",
      "isCompleted": false
    }
  }
}
```

### 선택 미션 조회 (필터링)
- **URL**: `POST /api/v1/missions/selected`
- **인증**: 필요
- **설명**: 필터 조건에 맞는 선택 미션 1개 반환. 하루 1번만 선택 가능

**Request (Query Params)**
- `spaceType`: INDOOR / OUTDOOR (선택)
- `categoryType`: FOOD / NATURE / CONTENT / PLACE / MUSIC (선택)

**Response**
```json
{
  "code": "MISSION_SELECTED",
  "data": {
    "id": 1,
    "title": "봄나물 캐러 가기",
    "description": "가까운 산이나 들에서 봄나물을 캐보세요",
    "spaceType": "OUTDOOR",
    "companionType": "SOLO",
    "categoryType": "NATURE",
    "isCompleted": false
  }
}
```

---

## 3. 미션 기록하기

### 미션 기록 페이지 정보 조회
- **URL**: `GET /api/v1/missions/{missionId}/record`
- **인증**: 불필요
- **설명**: 미션 기록하기 페이지에 필요한 미션 정보(제목, 설명) 조회

**Response**
```json
{
  "code": "MISSION_FOUND",
  "data": {
    "id": 1,
    "title": "나만의 여름 음료 개발",
    "description": "입하 제철 토마토로 상큼한 여름 음료를 만들어보세요"
  }
}
```

---

## 4. 미션 완료 기록

### 오늘의 미션 완료 기록
- **URL**: `POST /api/v1/missions/{missionId}/complete/daily`
- **인증**: 필요
- **설명**: 오늘의 미션 완료 기록. 동일 미션 중복 완료 시 409 반환

**Request**
```json
{
  "missionType": "DAILY",
  "solarTermId": 1,
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "맛있었다!"
}
```

**Response**
```json
{
  "code": "MISSION_COMPLETED",
  "data": {
    "completionId": 1
  }
}
```

### 추천 미션 완료 기록
- **URL**: `POST /api/v1/missions/{missionId}/complete/recommended`
- **인증**: 필요
- **설명**: 추천 미션 완료 기록. **하루 3회까지** 가능

**Request**
```json
{
  "solarTermId": 1,
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "맛있었다!"
}
```

**Response (성공)**
```json
{
  "code": "MISSION_COMPLETED",
  "data": {
    "completionId": 1
  }
}
```

**Response (4회 이상 시도 시)**
```json
{
  "code": "MISSION_409_REC",
  "message": "추천 미션은 하루 3회까지만 기록할 수 있습니다"
}
```

### 선택 미션 완료 기록
- **URL**: `POST /api/v1/missions/{missionId}/complete/selected`
- **인증**: 필요
- **설명**: 선택 미션 완료 기록. **하루 1회만** 가능

**Request**
```json
{
  "solarTermId": 1,
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "맛있었다!"
}
```

**Response (성공)**
```json
{
  "code": "MISSION_COMPLETED",
  "data": {
    "completionId": 1
  }
}
```

**Response (2회 이상 시도 시)**
```json
{
  "code": "MISSION_409_SEL",
  "message": "선택 미션은 하루 1회만 기록할 수 있습니다"
}
```

---

## 5. 추천 미션

### 추천 미션 완료 횟수 조회
- **URL**: `GET /api/v1/missions/recommended/count`
- **인증**: 필요
- **설명**: 사용자가 완료한 추천 미션의 총 횟수 조회

**Response**
```json
{
  "code": "MISSION_FOUND",
  "data": {
    "completedCount": 5
  }
}
```

---

## Enum 값

### MissionType
| 값 | 설명 |
|---|---|
| DAILY | 오늘의 미션 |
| RECOMMENDED | 추천 미션 |
| SELECTED | 선택 미션 |

### SpaceType
| 값 | 설명 |
|---|---|
| INDOOR | 실내 |
| OUTDOOR | 실외 |

### CategoryType
| 값 | 설명 |
|---|---|
| FOOD | 음식 |
| NATURE | 자연 |
| CONTENT | 컨텐츠 |
| PLACE | 장소 |
| MUSIC | 음악 |

### CompanionType
| 값 | 설명 |
|---|---|
| SOLO | 혼자 |
| TOGETHER | 같이 |
