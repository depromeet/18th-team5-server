# 홈 메인 카드 API 명세서

## 개요
홈 화면의 메인 카드 영역에 표시되는 절기 정보와 오늘의 미션을 조회하는 API입니다.

---

## API 정보

| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/v1/home/card` |
| **인증** | **필요 (Bearer Token)** |
| **Content-Type** | `application/json` |

---

## 요청

### Headers
| 헤더 | 필수 | 설명 |
|------|------|------|
| `Authorization` | O | `Bearer {accessToken}` |
| `Accept` | - | `application/json` |

### 요청 예시
```bash
curl -X GET 'https://api.peektime.com/api/v1/home/card' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Accept: application/json'
```

---

## 인증 토큰 발급

홈 카드 API를 호출하기 전에 먼저 로그인하여 Access Token을 발급받아야 합니다.

### 로그인 API
```bash
curl -X POST 'https://api.peektime.com/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "deviceUuid": "iOS-Device-UUID-Here"
  }'
```

### 로그인 응답
```json
{
  "code": "AUTH_200",
  "message": "로그인 성공",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isNewUser": false
  }
}
```

> **Note**: `accessToken`을 `Authorization: Bearer {accessToken}` 형식으로 헤더에 포함하여 요청합니다.

---

## 응답

### 성공 응답 (200 OK)

```json
{
  "code": "HOME_200",
  "message": "홈 화면 조회 성공",
  "result": {
    "solarTerm": {
      "id": 9,
      "name": "입하",
      "description": "여름이 일어서는 시간, 입하예요",
      "startDate": "2026-05-05",
      "endDate": "2026-05-20"
    },
    "dailyMission": {
      "id": 10,
      "title": "봄의 첫 신호 유리잔 바꿔주기",
      "participantCount": 100,
      "missionType": "DAILY"
    }
  }
}
```

### 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `code` | String | O | 응답 코드 |
| `message` | String | O | 응답 메시지 |
| `result` | Object | O | 응답 데이터 |

#### result.solarTerm (절기 정보)

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | Long | - | 절기 ID |
| `name` | String | - | 절기 이름 (예: 입하, 소만) |
| `description` | String | - | 절기 설명 (화면에 표시되는 문구) |
| `startDate` | String | - | 절기 시작일 (yyyy-MM-dd) |
| `endDate` | String | - | 절기 종료일 (yyyy-MM-dd) |

> **Note**: 현재 날짜에 해당하는 절기가 없으면 `solarTerm: null`

#### result.dailyMission (오늘의 미션)

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | Long | - | 미션 ID |
| `title` | String | - | 미션 제목 |
| `participantCount` | Long | - | 해당 미션 참여자 수 |
| `missionType` | String | - | 미션 타입 (`DAILY`, `RECOMMENDED`, `SELECTED`) |

> **Note**: 오늘 날짜에 배정된 미션이 없으면 `dailyMission: null`

---

## 화면 매핑

```
┌─────────────────────────────────────┐
│  05.05 - 05.21         자세히 보기 > │  ← solarTerm.startDate ~ endDate
│                                     │
│  여름이 일어서는 시간,                │  ← solarTerm.description
│  입하예요                            │
│                                     │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │  봄의 첫 신호 유리잔 바꿔주기   │    │  ← dailyMission.title
│  │                             │    │
│  │     [ 미션 참여하기 ]         │    │
│  │                             │    │
│  │  해당 미션에 100명이 참여했어요 │    │  ← dailyMission.participantCount
│  │                             │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

---

## 미션 기록하기 버튼 동작

**"미션 참여하기" 버튼 클릭 시:**

`dailyMission` 데이터를 다음 화면(미션 기록 화면)으로 전달하여 재사용합니다.

### 전달할 데이터

```swift
// iOS 예시
struct MissionData {
    let missionId: Int       // dailyMission.id
    let title: String        // dailyMission.title
    let missionType: String  // dailyMission.missionType (서버에서 제공)
}

// 미션 기록 화면으로 전달
let missionData = MissionData(
    missionId: response.result.dailyMission.id,
    title: response.result.dailyMission.title,
    missionType: response.result.dailyMission.missionType  // 서버 응답값 그대로 사용
)
navigateToMissionRecord(with: missionData)
```

> **Note**: `missionType`은 서버에서 제공하므로 하드코딩하지 않고 응답값을 그대로 사용합니다.

---

## 에러 응답

### 인증 실패 (401 Unauthorized)
```json
{
  "code": "AUTH_401",
  "message": "인증이 필요합니다."
}
```

### 토큰 만료 (401 Unauthorized)
```json
{
  "code": "AUTH_401_EXPIRED",
  "message": "토큰이 만료되었습니다."
}
```

> 토큰 만료 시 Refresh Token으로 재발급 필요 (`POST /api/v1/auth/refresh`)

### 서버 에러 (500)
```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "서버 오류가 발생했습니다."
}
```

---

## 관련 API

| API | 설명 |
|-----|------|
| `POST /api/v1/auth/login` | 로그인 (토큰 발급) |
| `POST /api/v1/auth/refresh` | 토큰 재발급 |
| `POST /api/v1/missions/{missionId}/complete` | 미션 완료 기록 |

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2026-05-08 | v1.0 | 최초 작성 |
| 2026-05-08 | v1.1 | `dailyMission.missionType` 필드 추가 |