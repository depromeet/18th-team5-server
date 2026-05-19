# PeekTime 인증/인가 가이드

## 개요

PeekTime은 **디바이스 UUID 기반 인증**을 사용합니다.
- 별도 회원가입/로그인 없이 iOS 디바이스 UUID만으로 인증
- JWT (Access Token + Refresh Token) 방식
- Refresh Token은 Redis에 저장

---

## 인증 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              최초 앱 실행                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  1. POST /api/v1/auth/login                                                 │
│     Body: { "deviceUuid": "iOS-UUID-HERE" }                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
           ┌──────────────┐                    ┌──────────────┐
           │  신규 UUID   │                    │  기존 UUID   │
           │  → 자동 가입  │                    │  → 로그인    │
           └──────────────┘                    └──────────────┘
                    │                                   │
                    └─────────────────┬─────────────────┘
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Response:                                                                  │
│  {                                                                          │
│    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",                               │
│    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",                              │
│    "isNewUser": true/false                                                  │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  2. 토큰 저장 (iOS Keychain 권장)                                            │
│     - accessToken: API 요청 시 사용                                          │
│     - refreshToken: accessToken 만료 시 재발급용                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## API 요청 시 인증

### Header 형식

```
Authorization: Bearer {accessToken}
```

### 예시 (Swift)

```swift
var request = URLRequest(url: url)
request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
```

### 예시 (cURL)

```bash
curl -X GET "https://api.peektime.com/api/v1/missions" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 토큰 재발급 (Refresh)

Access Token이 만료되면 Refresh Token으로 재발급합니다.

### 요청

```
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 응답

```json
{
  "code": "TOKEN_REFRESHED",
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "새로운 Access Token",
    "refreshToken": "새로운 Refresh Token"
  }
}
```

### 토큰 재발급 흐름 (iOS)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  API 요청 → 401 Unauthorized 응답                                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  POST /api/v1/auth/refresh (refreshToken 전송)                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
           ┌──────────────┐                    ┌──────────────┐
           │   성공 200   │                    │   실패 401   │
           │ 새 토큰 저장  │                    │  재로그인 필요 │
           │ 원래 요청 재시도│                    │ (login 호출)  │
           └──────────────┘                    └──────────────┘
```

---

## 로그아웃

```
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

- Redis에서 Refresh Token 삭제
- 응답: 204 No Content

---

## 토큰 정보

### JWT Payload 구조

```json
{
  "userId": 123,
  "deviceUuid": "iOS-UUID-HERE",
  "iat": 1699000000,
  "exp": 1699003600
}
```

### 토큰 만료 시간

| 토큰 | 만료 시간 | 환경 변수 |
|------|----------|----------|
| Access Token | 1시간 (3600000ms) | `JWT_ACCESS_EXPIRATION` |
| Refresh Token | 30일 (2592000000ms) | `JWT_REFRESH_EXPIRATION` |

---

## 인증이 필요 없는 API

다음 경로는 인증 없이 접근 가능합니다:

| 경로 | 설명 |
|------|------|
| `/api/v1/auth/**` | 로그인, 토큰 재발급 |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI 문서 |
| `/h2-console/**` | H2 콘솔 (개발용) |

---

## 에러 응답

### 토큰 관련 에러

| 상황 | HTTP 상태 | 에러 코드 | 대응 방법 |
|------|----------|----------|----------|
| 토큰 없음 | 401 | - | 로그인 필요 |
| 토큰 형식 오류 | 401 | `INVALID_TOKEN` | 로그인 필요 |
| Access Token 만료 | 401 | `EXPIRED_TOKEN` | Refresh 요청 |
| Refresh Token 만료 | 401 | `EXPIRED_TOKEN` | 재로그인 필요 |
| Refresh Token 불일치 | 401 | `REFRESH_TOKEN_MISMATCH` | 재로그인 필요 |
| Refresh Token 없음 (Redis) | 401 | `REFRESH_TOKEN_NOT_FOUND` | 재로그인 필요 |

---

## iOS 구현 예시

### 토큰 저장 (Keychain)

```swift
class TokenManager {
    static let shared = TokenManager()

    private let accessTokenKey = "peektime_access_token"
    private let refreshTokenKey = "peektime_refresh_token"

    func saveTokens(access: String, refresh: String) {
        // Keychain에 저장
        KeychainHelper.save(key: accessTokenKey, value: access)
        KeychainHelper.save(key: refreshTokenKey, value: refresh)
    }

    func getAccessToken() -> String? {
        return KeychainHelper.load(key: accessTokenKey)
    }

    func getRefreshToken() -> String? {
        return KeychainHelper.load(key: refreshTokenKey)
    }

    func clearTokens() {
        KeychainHelper.delete(key: accessTokenKey)
        KeychainHelper.delete(key: refreshTokenKey)
    }
}
```

### API 클라이언트 (토큰 자동 재발급)

```swift
class APIClient {
    func request<T: Decodable>(_ endpoint: Endpoint) async throws -> T {
        var request = endpoint.urlRequest

        // Access Token 추가
        if let token = TokenManager.shared.getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await URLSession.shared.data(for: request)

        // 401이면 토큰 재발급 시도
        if let httpResponse = response as? HTTPURLResponse,
           httpResponse.statusCode == 401 {

            // Refresh Token으로 재발급
            if try await refreshToken() {
                // 재발급 성공 → 원래 요청 재시도
                return try await self.request(endpoint)
            } else {
                // 재발급 실패 → 재로그인 필요
                throw APIError.unauthorized
            }
        }

        return try JSONDecoder().decode(T.self, from: data)
    }

    private func refreshToken() async throws -> Bool {
        guard let refreshToken = TokenManager.shared.getRefreshToken() else {
            return false
        }

        // POST /api/v1/auth/refresh 호출
        let response: TokenRefreshResponse = try await authAPI.refresh(token: refreshToken)

        // 새 토큰 저장
        TokenManager.shared.saveTokens(
            access: response.accessToken,
            refresh: response.refreshToken
        )

        return true
    }
}
```

### 디바이스 UUID 가져오기

```swift
import UIKit

func getDeviceUUID() -> String {
    // identifierForVendor 사용 (앱 삭제 시 변경됨)
    return UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
}
```

> **주의**: `identifierForVendor`는 앱 삭제 후 재설치 시 변경될 수 있습니다.
> 영구적인 식별이 필요하면 Keychain에 UUID를 저장하세요.

---

## 요약

| 항목 | 값 |
|------|-----|
| 인증 방식 | JWT (Bearer Token) |
| 로그인 | `POST /api/v1/auth/login` + deviceUuid |
| 토큰 재발급 | `POST /api/v1/auth/refresh` |
| 로그아웃 | `POST /api/v1/auth/logout` |
| Header | `Authorization: Bearer {accessToken}` |
| Access Token 만료 | 1시간 |
| Refresh Token 만료 | 30일 |