# 공지사항 API 명세서

## 개요

공지사항 관련 API 명세서입니다.
- **조회 API**: 앱에서 사용
- **생성/삭제 API**: Internal API (Swagger에서 관리자가 직접 호출)

---

## API 목록

| 기능 | HTTP | 경로 | 용도 | 비고 |
|-----|------|-----|-----|------|
| 공지사항 전체 조회 | GET | `/api/announcements` | 앱 | 최신순 정렬 |
| 공지사항 상세 조회 | GET | `/api/announcements/{id}` | 앱 | |
| 공지사항 생성 | POST | `/api/announcements` | Internal | Swagger에서 호출 |
| 공지사항 삭제 | DELETE | `/api/announcements/{id}` | Internal | Swagger에서 호출 |

---

## 1. 공지사항 전체 조회

공지사항 목록을 최신순으로 조회합니다.

### 요청

```
GET /api/announcements
```

### 응답

```json
{
  "code": "OK",
  "message": "성공",
  "result": [
    {
      "id": 2,
      "title": "새로운 기능 안내",
      "createdAt": "2026-06-10T15:00:00"
    },
    {
      "id": 1,
      "title": "서비스 점검 안내",
      "createdAt": "2026-06-09T10:00:00"
    }
  ]
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|-----|-----|------|
| id | Long | 공지사항 ID |
| title | String | 공지사항 제목 |
| createdAt | LocalDateTime | 생성일시 |

### 특이사항
- 최신순(createdAt DESC) 정렬
- 공지사항이 없으면 빈 배열 반환

---

## 2. 공지사항 상세 조회

특정 공지사항의 상세 내용을 조회합니다.

### 요청

```
GET /api/announcements/{id}
```

| 파라미터 | 타입 | 필수 | 설명 |
|---------|-----|-----|------|
| id | Long | O | 공지사항 ID (Path Variable) |

### 응답

```json
{
  "code": "OK",
  "message": "성공",
  "result": {
    "id": 1,
    "title": "서비스 점검 안내",
    "content": "안녕하세요. PeekTime입니다.\n\n서비스 점검이 예정되어 있습니다.\n점검 시간: 2026년 6월 15일 02:00 ~ 04:00\n\n이용에 참고 부탁드립니다.",
    "createdAt": "2026-06-09T10:00:00"
  }
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|-----|-----|------|
| id | Long | 공지사항 ID |
| title | String | 공지사항 제목 |
| content | String | 공지사항 본문 |
| createdAt | LocalDateTime | 생성일시 |

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| ANNOUNCEMENT_NOT_FOUND | 404 | 공지사항을 찾을 수 없음 |

---

## 3. 공지사항 생성 (Internal)

> **Internal API**: Swagger에서 관리자가 직접 호출

### 요청

```
POST /api/announcements
Content-Type: application/json
```

```json
{
  "title": "서비스 점검 안내",
  "content": "안녕하세요. PeekTime입니다.\n\n서비스 점검이 예정되어 있습니다."
}
```

### 요청 필드

| 필드 | 타입 | 필수 | 제한 | 설명 |
|-----|-----|-----|-----|------|
| title | String | O | 최대 40자 | 공지사항 제목 |
| content | String | O | 제한 없음 | 공지사항 본문 |

### 응답

```json
{
  "code": "OK",
  "message": "성공",
  "result": {
    "id": 1,
    "title": "서비스 점검 안내",
    "content": "안녕하세요. PeekTime입니다.\n\n서비스 점검이 예정되어 있습니다.",
    "createdAt": "2026-06-10T12:00:00"
  }
}
```

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| BAD_REQUEST | 400 | 제목 또는 본문이 비어있음 |
| BAD_REQUEST | 400 | 제목이 40자 초과 |

### Validation 메시지

| 필드 | 검증 | 메시지 |
|-----|-----|------|
| title | @NotBlank | 제목은 필수입니다 |
| title | @Size(max=40) | 제목은 40자 이내로 입력해주세요 |
| content | @NotBlank | 본문은 필수입니다 |

---

## 4. 공지사항 삭제 (Internal)

> **Internal API**: Swagger에서 관리자가 직접 호출

### 요청

```
DELETE /api/announcements/{id}
```

| 파라미터 | 타입 | 필수 | 설명 |
|---------|-----|-----|------|
| id | Long | O | 공지사항 ID (Path Variable) |

### 응답

```json
{
  "code": "OK",
  "message": "성공",
  "result": null
}
```

### 에러

| 코드 | HTTP | 설명 |
|-----|------|------|
| ANNOUNCEMENT_NOT_FOUND | 404 | 공지사항을 찾을 수 없음 |

---

## 에러 코드 정리

| 코드 | HTTP | 메시지 |
|-----|------|------|
| ANNOUNCEMENT_NOT_FOUND | 404 | 공지사항을 찾을 수 없습니다 |

---

## 엔티티 스키마

### Announcement

| 컬럼 | 타입 | 제약조건 | 설명 |
|-----|-----|---------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 공지사항 ID |
| title | VARCHAR(40) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 본문 |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | | 수정일시 |

---

## 관련 파일

| 구분 | 파일 |
|-----|------|
| Controller | `AnnouncementController.java` |
| Service | `AnnouncementService.java` |
| Entity | `Announcement.java` |
| Repository | `AnnouncementRepository.java` |
| Request DTO | `AnnouncementRequest.java` |
| Response DTO | `AnnouncementResponse.java`, `AnnouncementListResponse.java` |