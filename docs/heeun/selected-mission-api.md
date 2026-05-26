# 선택 미션 API 명세서

## 개요

선택 미션은 **오늘의 미션**, **추천 미션**에 포함되지 않은 미션 풀에서 사용자가 직접 필터링하여 선택하는 미션이다.

- 하루 1회만 선택 가능
- 선택 후 재호출 시 기존 미션 반환 (멱등성 보장)

---

## API 목록

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/missions/selected/today` | 오늘 선택한 미션 조회 |
| POST | `/api/v1/missions/selected` | 선택 미션 조회 (신규 선택) |

---

## 전체 플로우

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            앱 진입 (선택 미션 화면)                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              1️⃣ GET /api/v1/missions/selected/today                         │
│                        (오늘 선택한 미션 있는지 확인)                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
                          ┌────────────────────────┐
                          │   hasSelected 값 확인   │
                          └────────────────────────┘
                                       │
               ┌───────────────────────┴───────────────────────┐
               │                                               │
               ▼                                               ▼
   ┌───────────────────────┐                     ┌───────────────────────┐
   │   hasSelected: true   │                     │  hasSelected: false   │
   │   (오늘 이미 선택함)    │                     │   (오늘 첫 선택)       │
   └───────────────────────┘                     └───────────────────────┘
               │                                               │
               ▼                                               ▼
   ┌───────────────────────┐                     ┌───────────────────────┐
   │  mission 데이터 사용   │                     │   필터 선택 UI 노출    │
   │  (API 추가 호출 X)     │                     │ (공간/인원/카테고리)   │
   └───────────────────────┘                     └───────────────────────┘
               │                                               │
               │                                               ▼
               │                     ┌─────────────────────────────────────┐
               │                     │  2️⃣ POST /api/v1/missions/selected  │
               │                     │         (필터와 함께 요청)            │
               │                     └─────────────────────────────────────┘
               │                                               │
               │                                               ▼
               │                                  ┌────────────────────────┐
               │                                  │  서버: 랜덤 1개 선택    │
               │                                  │  + DB 저장             │
               │                                  └────────────────────────┘
               │                                               │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
                         ┌───────────────────────────┐
                         │      미션 상세 화면 표시    │
                         └───────────────────────────┘
```

---

## 1. 오늘 선택한 미션 조회

### 기본 정보

| 항목 | 내용 |
|-----|------|
| **Method** | `GET` |
| **URL** | `/api/v1/missions/selected/today` |
| **인증** | Bearer Token 필수 |
| **설명** | 오늘 선택한 미션이 있는지 확인하고, 있으면 미션 정보 반환 |

### Request

**Header**
```
Authorization: Bearer {accessToken}
```

**Body**: 없음

### Response

#### 성공 (200 OK)

**오늘 선택한 미션이 있는 경우**
```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "data": {
    "hasSelected": true,
    "mission": {
      "id": 1,
      "title": "가까운 카페에서 제철 음료 마시기",
      "description": "계절의 맛을 느껴보세요",
      "spaceType": "INDOOR",
      "companionType": "ALONE",
      "categoryType": "FOOD"
    }
  }
}
```

**오늘 선택한 미션이 없는 경우**
```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "data": {
    "hasSelected": false,
    "mission": null
  }
}
```

#### 실패

| HTTP Status | Code | Message | 상황 |
|-------------|------|---------|------|
| 401 | `AUTH_401` | 유효하지 않은 토큰입니다 | 토큰 누락/위조 |
| 401 | `AUTH_401_EXPIRED` | 만료된 토큰입니다 | 토큰 만료 |

### Response 분기 정리

| hasSelected | mission | 의미 | 클라이언트 액션 |
|-------------|---------|------|----------------|
| `true` | `{...}` | 오늘 이미 선택함 | 바로 미션 표시 |
| `false` | `null` | 오늘 첫 방문 | 필터 UI 노출 → POST 호출 |

---

## 2. 선택 미션 조회 (신규 선택)

### 기본 정보

| 항목 | 내용 |
|-----|------|
| **Method** | `POST` |
| **URL** | `/api/v1/missions/selected` |
| **인증** | Bearer Token 필수 |
| **Content-Type** | `application/x-www-form-urlencoded` |
| **설명** | 필터 조건에 맞는 선택 미션 1개 반환. **하루 1번만 선택 가능** |

### Request

**Header**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**

| Parameter | Type | Required | Description | Values |
|-----------|------|----------|-------------|--------|
| spaceType | String | N | 공간 필터 | `INDOOR`, `OUTDOOR` |
| companionType | String | N | 인원 필터 | `ALONE`, `TOGETHER` |
| categoryType | String | N | 카테고리 필터 | `FOOD`, `NATURE`, `RECORD`, `PLACE`, `SENSE` |

**예시**
```
POST /api/v1/missions/selected?spaceType=INDOOR&companionType=ALONE
POST /api/v1/missions/selected?categoryType=FOOD
POST /api/v1/missions/selected  (필터 없이 전체에서 선택)
```

### Response

#### 성공 (200 OK)

```json
{
  "code": "MISSION_201",
  "message": "미션 선택 성공",
  "data": {
    "id": 15,
    "title": "창문 열고 바깥 소리 듣기",
    "description": "5분간 눈을 감고 계절의 소리에 집중해보세요",
    "spaceType": "INDOOR",
    "companionType": "ALONE",
    "categoryType": "SENSE"
  }
}
```

#### 실패

| HTTP Status | Code | Message | 상황 |
|-------------|------|---------|------|
| 401 | `AUTH_401` | 유효하지 않은 토큰입니다 | 토큰 누락/위조 |
| 401 | `AUTH_401_EXPIRED` | 만료된 토큰입니다 | 토큰 만료 |
| 404 | `USER_404` | 사용자를 찾을 수 없습니다 | 탈퇴한 사용자 |
| 404 | `USER_404_ONBOARD` | 온보딩 정보를 찾을 수 없습니다 | 온보딩 미완료 |
| 404 | `SOLAR_TERM_404` | 절기를 찾을 수 없습니다 | 현재 날짜에 절기 데이터 없음 |
| 404 | `MISSION_404` | 미션을 찾을 수 없습니다 | 필터 조건에 맞는 미션 없음 |

### 동작 상세

| 상황 | 동작 | 비고 |
|-----|------|------|
| 오늘 첫 선택 | 랜덤 선택 + DB 저장 + 반환 | 정상 케이스 |
| 오늘 이미 선택함 | 기존 미션 반환 (새로 선택 X) | 멱등성 보장 |
| 필터 조건에 맞는 미션 없음 | `MISSION_404` 에러 | 필터 완화 필요 |

---

## 예외 케이스 플로우

```
┌──────────────────────────────────────────────────────────────┐
│                POST /api/v1/missions/selected                │
└──────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   ┌─────────┐          ┌──────────┐          ┌──────────┐
   │ 401     │          │ 404      │          │ 200 OK   │
   │ 인증실패 │          │ 리소스없음 │          │ 성공     │
   └─────────┘          └──────────┘          └──────────┘
        │                     │                     │
        ▼                     ▼                     ▼
  ┌───────────┐    ┌──────────────────┐      ┌──────────┐
  │ 로그인     │    │ USER_404         │      │ 미션 표시 │
  │ 페이지     │    │ USER_404_ONBOARD │      └──────────┘
  └───────────┘    │ SOLAR_TERM_404   │
                   │ MISSION_404      │
                   └──────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                 ▼
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │온보딩 미완료│    │절기 데이터 │    │필터 조건   │
   │→ 온보딩    │    │누락        │    │완화 안내   │
   └────────────┘    │→ 관리자    │    └────────────┘
                     └────────────┘
```

---

## 비즈니스 로직

### 선택 미션 선정 기준

```
전체 미션 풀
  - 오늘의 미션 (현재 절기에 배정된 DailyMission) 제외
  - 추천 미션 (현재 절기 + 사용자 타입에 배정된 RecommendedMissionPool) 제외
  - 이미 선택한 미션 (UserSelectedMission) 제외
  + 필터 조건 적용 (spaceType, companionType, categoryType)
  → 남은 미션 중 랜덤 1개 선택
```

### 하루 1회 제한

- 오늘 이미 선택한 미션이 있으면 → **새로 선택하지 않고 기존 미션 반환**
- 새 미션 선택 시 `UserSelectedMission` 테이블에 저장
- 날짜 기준: `LocalDate.now()` (서버 시간 기준)

### 필터 미전달 시

- 모든 필터는 optional
- 필터 없이 호출하면 전체 선택 미션 풀에서 랜덤 선택

---

## Response DTO

### SelectedMissionStatusResponse (GET 응답)

| Field | Type | Description |
|-------|------|-------------|
| hasSelected | boolean | 오늘 선택 미션 조회 여부 |
| mission | SelectedMissionResponse | 선택한 미션 정보 (없으면 null) |

### SelectedMissionResponse (POST 응답 / mission 필드)

| Field | Type | Description |
|-------|------|-------------|
| id | Long | 미션 ID |
| title | String | 미션 제목 |
| description | String | 미션 설명 |
| spaceType | String | 공간 타입 (INDOOR/OUTDOOR) |
| companionType | String | 인원 타입 (ALONE/TOGETHER) |
| categoryType | String | 카테고리 타입 (FOOD/NATURE/RECORD/PLACE/SENSE) |

---

## Enum 값 정리

### SpaceType
| Value | Label |
|-------|-------|
| INDOOR | 실내 |
| OUTDOOR | 실외 |

### CompanionType
| Value | Label |
|-------|-------|
| ALONE | 혼자 |
| TOGETHER | 같이 |

### CategoryType
| Value | Label |
|-------|-------|
| FOOD | 음식 |
| NATURE | 자연 |
| RECORD | 기록 |
| PLACE | 장소 |
| SENSE | 감각 |
