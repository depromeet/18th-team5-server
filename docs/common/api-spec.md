# PeakTime API 명세서

## 개요

- **Base URL**: `http://localhost:8080` (로컬) / 배포 URL (운영)
- **Content-Type**: `application/json`
- **인증 방식**: `Authorization: Bearer {accessToken}`
- **Swagger UI**: `/swagger-ui/index.html`

---

## 공통 응답 형식

### 성공
```json
{
  "code": "AUTH_200",
  "message": "로그인 성공",
  "result": { ... }
}
```

### 실패
```json
{
  "code": "COMMON_400",
  "message": "잘못된 요청입니다"
}
```

---

## 공통 에러 코드

| code | HTTP | 설명 |
|------|------|------|
| `COMMON_400` | 400 | 잘못된 요청 (유효성 검사 실패) |
| `COMMON_401` | 401 | 인증 필요 (토큰 없음 또는 유효하지 않음) |
| `COMMON_500` | 500 | 서버 내부 오류 |

---

## 1. Auth — 인증

> 인증 불필요 경로: `/api/v1/auth/**`

### 1-1. 로그인 / 자동가입

iOS 디바이스 UUID로 로그인합니다. 신규 UUID면 자동 가입 후 토큰을 발급합니다.

- **POST** `/api/v1/auth/login`
- **인증 필요**: 없음

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `deviceUuid` | String | ✅ | iOS 디바이스 UUID (최대 36자) |

```json
{
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | String | JWT Access Token (유효기간 1시간) |
| `refreshToken` | String | JWT Refresh Token (유효기간 30일) |
| `isNewUser` | Boolean | 신규 가입 여부 |

```json
{
  "code": "AUTH_200",
  "message": "로그인 성공",
  "result": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
    "isNewUser": true
  }
}
```

**Error**

| 상황 | code |
|------|------|
| deviceUuid 누락 또는 빈 값 | `COMMON_400` |

---

### 1-2. Access Token 재발급

Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다. (Refresh Token Rotation)

- **POST** `/api/v1/auth/refresh`
- **인증 필요**: 없음

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `refreshToken` | String | ✅ | 기존 Refresh Token |

```json
{
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9..."
}
```

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | String | 새 JWT Access Token |
| `refreshToken` | String | 새 JWT Refresh Token |

```json
{
  "code": "AUTH_200",
  "message": "Access Token 재발급 성공",
  "result": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "eyJhbGciOiJIUzM4NCJ9..."
  }
}
```

**Error**

| 상황 | code |
|------|------|
| 유효하지 않은 토큰 | `AUTH_401` |
| 만료된 Refresh Token | `AUTH_401_EXPIRED` |
| Redis에 토큰 없음 (로그아웃 상태) | `AUTH_401_RT` |
| 토큰 값 불일치 | `AUTH_401_MISMATCH` |

---

### 1-3. 로그아웃

Redis에서 Refresh Token을 삭제합니다.

- **POST** `/api/v1/auth/logout`
- **인증 필요**: ✅

**Response**: HTTP 204 No Content

---

## 2. S3 — 파일 업로드

> 미션 기록 사진 업로드 플로우에서 사용합니다.
> S3에 직접 업로드 후 반환받은 `objectKey`를 미션 완료 API에 전달합니다.

### 2-1. Presigned URL 발급

S3 직접 업로드용 Presigned URL을 발급합니다. (유효시간 10분)

- **GET** `/api/v1/s3/presigned-url`
- **인증 필요**: ✅

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `fileName` | String | ✅ | 업로드할 파일명 (예: `photo.jpg`) |
| `contentType` | String | ✅ | MIME 타입 (`image/jpeg` \| `image/png` \| `image/heic`) |

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `presignedUrl` | String | S3 직접 업로드 URL (유효시간 10분) |
| `objectKey` | String | S3 object key (미션 완료 API에 전달) |

```json
{
  "code": "S3_200",
  "message": "Presigned URL 발급 성공",
  "result": {
    "presignedUrl": "https://s3.amazonaws.com/bucket/...?X-Amz-Signature=...",
    "objectKey": "missions/2026/05/07/uuid-filename.jpg"
  }
}
```

**업로드 방법**

발급받은 `presignedUrl`로 `PUT` 요청을 보냅니다.

```
PUT {presignedUrl}
Content-Type: image/jpeg

[이미지 바이너리 데이터]
```

---

### 2-2. S3 이미지 삭제

- **DELETE** `/api/v1/s3/image`
- **인증 필요**: ✅

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `objectKey` | String | ✅ | 삭제할 S3 object key |

**Response**: HTTP 200

```json
{
  "code": "S3_200_DELETE",
  "message": "이미지 삭제 성공",
  "result": null
}
```

---

## 3. Mission — 미션 완료 기록

### 3-1. 미션 완료 기록

S3 업로드 완료 후 미션 완료를 기록합니다. 동일 미션 중복 완료 시 409 반환.

- **POST** `/api/v1/missions/{missionId}/complete`
- **인증 필요**: ✅

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `missionId` | Long | 완료할 미션 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `missionType` | String | ✅ | `DAILY` \| `RECOMMENDED` \| `SELECTED` |
| `objectKey` | String | - | S3 object key (사진 업로드 시 필수, 최대 500자) |
| `memo` | String | - | 한줄 메모 (최대 200자) |
| `completedAt` | String | - | 완료 시각 ISO 8601 형식, 미입력 시 현재 시각 |

```json
{
  "missionType": "DAILY",
  "objectKey": "missions/2026/05/07/uuid-filename.jpg",
  "memo": "오늘 청귤에이드 만들었다!",
  "completedAt": "2026-05-07T14:00:00+09:00"
}
```

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `completionId` | Long | 완료 기록 ID |
| `missionId` | Long | 미션 ID |
| `missionType` | String | 미션 타입 |
| `completedAt` | String | 완료 시각 |

```json
{
  "code": "MISSION_200",
  "message": "미션 완료 기록 성공",
  "result": {
    "completionId": 1,
    "missionId": 5,
    "missionType": "DAILY",
    "completedAt": "2026-05-07T14:00:00+09:00"
  }
}
```

**Error**

| 상황 | code |
|------|------|
| 동일 미션 중복 완료 | `MISSION_409` |

---

### 3-2. 미션 완료 기록 조회

특정 미션의 완료 기록을 조회합니다. 이미지는 Presigned URL로 제공됩니다.

- **GET** `/api/v1/missions/{missionId}/completions`
- **인증 필요**: ✅

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `missionId` | Long | 조회할 미션 ID |

**Response**

```json
{
  "code": "MISSION_200",
  "message": "미션 완료 기록 조회 성공",
  "result": [
    {
      "completionId": 1,
      "missionId": 5,
      "missionType": "DAILY",
      "objectKey": "missions/2026/05/07/uuid-filename.jpg",
      "presignedImageUrl": "https://s3.amazonaws.com/bucket/...?X-Amz-Signature=...",
      "memo": "오늘 청귤에이드 만들었다!",
      "completedAt": "2026-05-07T14:00:00+09:00"
    }
  ]
}
```

---

## 미션 기록 플로우 (S3 업로드 포함)

```
1. Presigned URL 발급
   GET /api/v1/s3/presigned-url?fileName=photo.jpg&contentType=image/jpeg
   → presignedUrl, objectKey 반환

2. S3 직접 업로드
   PUT {presignedUrl}
   Content-Type: image/jpeg
   Body: [이미지 바이너리]

3. 미션 완료 기록
   POST /api/v1/missions/{missionId}/complete
   Body: { missionType, objectKey, memo, completedAt }
```

---

## JWT Payload 구조

```json
{
  "userId": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1746345600,
  "exp": 1746349200
}
```

| 클레임 | 설명 |
|--------|------|
| `userId` | 서버 내부 User PK |
| `deviceUuid` | iOS 디바이스 UUID |
| `iat` | 발급 시각 (Unix timestamp) |
| `exp` | 만료 시각 (Unix timestamp) |

---

## 토큰 만료 시간

| 토큰 | 만료 |
|------|------|
| Access Token | 1시간 |
| Refresh Token | 30일 |
