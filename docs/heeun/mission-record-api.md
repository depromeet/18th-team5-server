# 미션 기록 API 명세서

## 개요

미션 기록 관련 API 명세서입니다.
**미션 기록 전 반드시 페이지 정보 조회 API를 먼저 호출**하여 미션 제목과 설명을 받아야 합니다.

---

## API 목록

| 기능 | HTTP | 경로 | 인증 | 비고 |
|-----|------|-----|-----|------|
| 기록 페이지 정보 조회 | GET | `/api/v1/missions/{missionId}/record` | X | **기록 전 필수 호출** |
| 오늘의 미션 기록 | POST | `/api/v1/missions/{missionId}/complete/daily` | O | 중복 완료 불가 |
| 추천 미션 기록 | POST | `/api/v1/missions/{missionId}/complete/recommended` | O | 하루 3회 제한 |
| 선택 미션 기록 | POST | `/api/v1/missions/{missionId}/complete/selected` | O | 하루 1회 제한 |
| 완료 기록 상세 조회 | GET | `/api/v1/missions/{missionId}/completions` | O | Presigned URL 포함 |

---

## 1. 미션 기록 페이지 정보 조회 (필수)

미션 기록 화면 진입 시 **가장 먼저 호출**해야 하는 API입니다.

### 요청

```
GET /api/v1/missions/{missionId}/record
```

| 파라미터 | 타입 | 필수 | 설명 |
|---------|-----|-----|------|
| missionId | Long | O | 미션 ID (Path Variable) |

### 응답

```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "data": {
    "id": 1,
    "title": "나만의 여름 음료 개발",
    "description": "입하 제철 토마토로 상큼한 여름 음료를 만들어보세요"
  }
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|-----|-----|------|
| id | Long | 미션 ID |
| title | String | 미션 제목 |
| description | String | 미션 설명 |

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| MISSION_NOT_FOUND | 404 | 미션을 찾을 수 없음 |

---

## 2. 오늘의 미션 기록

### 요청

```
POST /api/v1/missions/{missionId}/complete/daily
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "맛있었다!"
}
```

| 필드 | 타입 | 필수 | 제한 | 설명 |
|-----|-----|-----|-----|------|
| objectKey | String | O | 최대 500자 | S3 오브젝트 키 |
| memo | String | X | 최대 200자 | 한줄 메모 |

### 응답

```json
{
  "code": "MISSION_200",
  "message": "미션 완료 성공",
  "data": {
    "completionId": 1234
  }
}
```

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| MISSION_ALREADY_COMPLETED | 409 | 이미 완료한 오늘의 미션 |
| DAILY_MISSION_NOT_FOUND | 404 | 오늘의 미션으로 등록되지 않음 |
| SOLAR_TERM_NOT_FOUND | 404 | 절기 정보 없음 |
| USER_NOT_FOUND | 404 | 사용자 없음 |

### 특이사항
- 동일한 오늘의 미션은 **중복 완료 불가**
- 완료 시 참여자 수가 자동으로 증가
- 이벤트 발행 (Outbox 패턴)

---

## 3. 추천 미션 기록

### 요청

```
POST /api/v1/missions/{missionId}/complete/recommended
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "추천받아서 해봤어요!"
}
```

| 필드 | 타입 | 필수 | 제한 | 설명 |
|-----|-----|-----|-----|------|
| objectKey | String | O | 최대 500자 | S3 오브젝트 키 |
| memo | String | X | 최대 200자 | 한줄 메모 |

### 응답

```json
{
  "code": "MISSION_200",
  "message": "미션 완료 성공",
  "data": {
    "completionId": 5678
  }
}
```

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| RECOMMENDED_MISSION_LIMIT_EXCEEDED | 409 | 하루 3회 초과 |
| MISSION_NOT_FOUND | 404 | 미션 없음 |
| SOLAR_TERM_NOT_FOUND | 404 | 절기 정보 없음 |
| USER_NOT_FOUND | 404 | 사용자 없음 |

### 특이사항
- **하루 최대 3회** 완료 가능
- 4번째 시도 시 409 에러

---

## 4. 선택 미션 기록

### 요청

```
POST /api/v1/missions/{missionId}/complete/selected
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "objectKey": "missions/2026/05/29/uuid.jpg",
  "memo": "직접 선택한 미션!"
}
```

| 필드 | 타입 | 필수 | 제한 | 설명 |
|-----|-----|-----|-----|------|
| objectKey | String | O | 최대 500자 | S3 오브젝트 키 |
| memo | String | X | 최대 200자 | 한줄 메모 |

### 응답

```json
{
  "code": "MISSION_200",
  "message": "미션 완료 성공",
  "data": {
    "completionId": 9012
  }
}
```

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| SELECTED_MISSION_LIMIT_EXCEEDED | 409 | 하루 1회 초과 |
| MISSION_NOT_FOUND | 404 | 미션 없음 |
| SOLAR_TERM_NOT_FOUND | 404 | 절기 정보 없음 |
| USER_NOT_FOUND | 404 | 사용자 없음 |

### 특이사항
- **하루 최대 1회** 완료 가능
- 2번째 시도 시 409 에러

---

## 5. 미션 완료 기록 상세 조회

사용자가 특정 미션에 대해 완료한 기록 목록을 조회합니다.

### 요청

```
GET /api/v1/missions/{missionId}/completions
Authorization: Bearer {accessToken}
```

### 응답

```json
{
  "code": "MISSION_200",
  "message": "미션 조회 성공",
  "data": [
    {
      "completionId": 1234,
      "missionId": 1,
      "missionType": "DAILY",
      "objectKey": "missions/2026/05/29/uuid.jpg",
      "presignedImageUrl": "https://s3.amazonaws.com/bucket/...?X-Amz-Signature=...",
      "memo": "맛있었다!",
      "completedAt": "2026-05-29T14:30:00+09:00"
    }
  ]
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|-----|-----|------|
| completionId | Long | 완료 기록 ID |
| missionId | Long | 미션 ID |
| missionType | String | DAILY / RECOMMENDED / SELECTED |
| objectKey | String | S3 오브젝트 키 |
| presignedImageUrl | String | 이미지 접근용 Presigned URL (만료 시간 있음) |
| memo | String | 한줄 메모 |
| completedAt | String | 완료 일시 (ISO 8601, KST) |

### 특이사항
- 같은 미션을 여러 번 완료했으면 모든 기록 반환
- Presigned URL은 일정 시간 후 만료됨

---

## 기록 플로우

```
┌─────────────────────────────────────────────────────────────┐
│                     미션 기록 플로우                          │
└─────────────────────────────────────────────────────────────┘

1. 미션 카드 클릭
       │
       ▼
┌─────────────────────────────────┐
│  GET /missions/{id}/record      │  ◀── 페이지 정보 조회 (필수)
│  → title, description 획득      │
└─────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  S3 Presigned URL로 이미지 업로드  │  ◀── 별도 S3 API
│  → objectKey 획득               │
└─────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│  POST /missions/{id}/complete/{type}                        │
│                                                             │
│  type = daily | recommended | selected                      │
│  body = { objectKey, memo }                                 │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
    기록 완료
```

---

## 일일 제한 요약

| 미션 타입 | 일일 제한 | 중복 완료 |
|----------|----------|----------|
| 오늘의 미션 (DAILY) | 없음 (단, 각 미션당 1회) | X |
| 추천 미션 (RECOMMENDED) | 3회 | O |
| 선택 미션 (SELECTED) | 1회 | O |

---

## 관련 파일

| 구분 | 파일 |
|-----|------|
| Controller | `UserMissionCompletionController.java` |
| Service | `UserMissionCompletionService.java` |
| Entity | `UserMissionCompletion.java` |
| Request DTO | `UserMissionCompletionRequest.java`, `MissionCompletionRequest.java` |
| Response DTO | `UserMissionCompletionResponse.java`, `MissionRecordPageResponse.java` |