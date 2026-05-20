# PeakTime 코드 현황 분석 (2026-05-04 기준)

## 프로젝트 구조

멀티모듈 Gradle 프로젝트 (`peektime-server`)

```
peektime-server/
├── peektime-admin/   # Admin 웹 (Thymeleaf + Spring Security)
└── peektime-api/     # 모바일 API (REST, Swagger)
```

현재 브랜치: `feature/PICK-59-UUID-Auth`  
(아직 커밋 없음 — 이전 main 커밋과 동일)

---

## peektime-admin

### 엔티티

| 엔티티 | 테이블 | 주요 필드 |
|--------|--------|-----------|
| `Mission` | `mission` | title, description, spaceType, intensityType, categoryType, companionType, enjoyType, userType, deleted, deletedAt |
| `DailyMission` | `daily_mission` | mission, solarTerm, missionDate (NULL=배정대기, 값=배정완료) |
| `RecommendedMissionPool` | `recommended_mission_pool` | mission, solarTerm, userType, displayOrder |
| `SolarTerm` | `solar_term` | name, description, startDate, endDate, solar_year |
| `MissionCompletionStats` | - | missionType (통계용) |

**Mission 특이사항**
- `MissionStatus` 필드 없음 (엔티티 구조에서 제거됨)
- Soft Delete 적용 (`deleted`, `deletedAt`)
- 정적 팩토리 메서드 `Mission.create(MissionRequest)` 있음
- `update()` 메서드로 모든 필드 수정 가능

**DailyMission 특이사항**
- `displayOrder` 필드 제거됨 (f0e3f3e)
- `missionDate=NULL` → 배정대기, `missionDate=값` → 배정완료
- unique constraint: `(mission_id, solar_term_id)`

**RecommendedMissionPool 특이사항**
- unique constraint: `(mission_id, solar_term_id, user_type)`

### API 컨트롤러

| 경로 | 클래스 | 기능 |
|------|--------|------|
| `POST /api/admin/missions/generate` | `MissionGenerationController` | Gemini AI 미션 생성 |
| `POST /api/admin/daily-missions/{id}/assign` | `DailyMissionApiController` | 날짜 배정 |
| `POST /api/admin/daily-missions/{id}/unassign` | `DailyMissionApiController` | 배정 해제 |
| `DELETE /api/admin/daily-missions/{id}` | `DailyMissionApiController` | 미션풀로 복귀 |
| `POST /api/admin/missions/bulk-*` | `MissionBulkController` | 일괄 작업 |

### 웹 페이지 컨트롤러 (Thymeleaf)

| URL | 컨트롤러 | 페이지 |
|-----|----------|--------|
| `/admin` | `AdminPageController` | 대시보드 |
| `/admin/missions/pool` | `MissionPageController` | 미션풀 관리 |
| `/admin/daily-missions` | `DailyMissionPageController` | 오늘의 미션 (드래그앤드롭) |
| `/admin/recommended-missions` | `RecommendedMissionPageController` | 추천 미션 |
| `/admin/selected-missions` | `SelectedMissionPageController` | 선택 미션 |
| `/admin/settings` | `SettingsPageController` | 설정 |
| `/login` | `LoginController` | 로그인 |

### 인프라

- **Gemini LLM**: `GeminiClient` + `GeminiConfig` (gemini-2.5-flash 모델)
- **인증**: Spring Security FormLogin, InMemoryUserDetailsManager (admin/admin123)
- **DB**: H2 (개발), application-local.yml로 설정 (gitignore)

---

## peektime-api

### 엔티티

| 엔티티 | 테이블 | 주요 필드 |
|--------|--------|-----------|
| `User` | `users` | deviceUuid (UUID, unique), nickname, pushToken |
| `UserOnboarding` | `user_onboarding` | user, spaceType, intensityType, enjoyTypeFirst/Second/Third, userType |
| `DeviceAuth` | `device_auth` | user, deviceUuid, refreshToken, expiresAt, revoked |
| `UserMissionCompletion` | `user_mission_completion` | user, missionId, missionType, objectKey, memo, completedAt |
| `UserRecord` | `user_record` | (기록용) |

**User 특이사항**
- UUID 기반 인증 (`deviceUuid` 컬럼)
- `UserOnboarding`과 1:1 관계 (OneToOne)
- `DeviceAuth`와 1:N 관계

**UserOnboarding 특이사항**
- `userType`은 `determineUserType(spaceType, intensityType)` 로직으로 **자동 계산**
  - OUTDOOR + ACTIVE → `EXPLORER`
  - OUTDOOR + LIGHT → `WALKER`
  - INDOOR + ACTIVE → `LIFE_CREATOR`
  - INDOOR + LIGHT (기본) → `AESTHETE`

**DeviceAuth 특이사항**
- `rotate()`: Refresh Token 갱신
- `revoke()`: 토큰 무효화
- `isExpired()`: 만료 여부 확인
- `feature/PICK-59-UUID-Auth` 브랜치에서 작업 중 (현재 비어있음)

**UserMissionCompletion 특이사항**
- `imageUrl` → `objectKey`로 변경됨 (920d0a4)
- `objectKey`: S3 Object Key 직접 저장
- 중복 완료 시 409 반환

### API 컨트롤러

| 경로 | 클래스 | 기능 |
|------|--------|------|
| `POST /api/v1/missions/{missionId}/complete` | `UserMissionCompletionController` | 미션 완료 기록 |
| `GET /api/v1/missions/{missionId}/completions` | `UserMissionCompletionController` | 완료 기록 조회 (S3 Presigned URL 포함) |
| S3 관련 | `S3Controller` | Presigned URL 발급 |
| 기록 관련 | `UserRecordController` | 사용자 기록 CRUD |

### 인프라

- **S3**: `S3Config` + `S3Service` — 업로드용 Presigned URL, 조회용 Presigned URL 발급
- **Security**: 현재 전체 허용 (`anyRequest().permitAll()`) — 개발 단계
- **Swagger**: `http://localhost:8080/swagger-ui.html`

---

## 공통 Enum

두 모듈 모두 동일한 Enum 보유 (각자 패키지에 중복 존재)

| Enum | 값 |
|------|-----|
| `SpaceType` | INDOOR, OUTDOOR |
| `IntensityType` | LIGHT, MODERATE, ACTIVE |
| `CompanionType` | SOLO, TOGETHER |
| `CategoryType` | FOOD, NATURE, RECORD, PLACE, SENSORY |
| `EnjoyType` | NATURE_OUTDOOR, SEASONAL_FOOD, CULTURE_CONTENT |
| `UserType` | EXPLORER, WALKER, LIFE_CREATOR, AESTHETE |
| `MissionType` | DAILY, RECOMMENDED, SELECTED (통계/응답용) |

---

## 현재 브랜치 작업: PICK-59 UUID 인증

`DeviceAuth` 엔티티는 추가되어 있으나, 실제 인증 로직 (JWT 발급, 필터, 컨트롤러)은 아직 구현 안 됨.

**구현 필요 항목 추정:**
- Device UUID로 회원가입/로그인 엔드포인트
- JWT Access Token + Refresh Token (DeviceAuth 테이블 활용)
- Spring Security Filter (JWT 검증)
- `UserRepository`에 `findByDeviceUuid()` 쿼리 추가 필요

---

## 미구현 / TODO

- `UserRepository`: `findByDeviceUuid()` 메서드 없음 (현재 `findById()`만 있음)
- 미션 완료 API에서 `userId`를 RequestParam으로 받음 → JWT 인증 후 SecurityContext에서 추출하도록 변경 예정
- 선택 미션 API (필터링 쿼리) 미구현
- 추천 미션 조회 API 미구현 (admin에서 pool 관리만 됨)
- 오늘의 미션 조회 API 미구현
