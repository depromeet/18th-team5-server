# Admin 페이지 개발 작업 계획

## 개요
- **목표**: peektime-admin 모듈에 Thymeleaf 기반 Admin 페이지 구현
- **참고**: `docs/wireframes/` 폴더의 HTML 와이어프레임

---

## Phase 1: Thymeleaf 기본 설정

### 1-1. 의존성 추가
**파일**: `peektime-admin/build.gradle`

```gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect'  // 레이아웃 지원
```

### 1-2. 설정 파일
**파일**: `application.yml`

```yaml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    cache: false  # 개발 시 캐시 비활성화
```

### 1-3. 폴더 구조 생성
```
peektime-admin/src/main/resources/
├── templates/
│   ├── layout/
│   │   └── default.html      # 공통 레이아웃
│   ├── fragments/
│   │   ├── header.html       # 헤더
│   │   ├── sidebar.html      # 사이드바
│   │   └── footer.html       # 푸터
│   ├── login.html
│   ├── dashboard.html
│   ├── mission/
│   │   ├── pool.html         # 미션풀 관리
│   │   ├── daily.html        # 오늘의 미션
│   │   ├── recommended.html  # 추천 미션
│   │   └── selected.html     # 선택 미션 (미리보기)
│   └── settings/
│       └── index.html        # 설정 (절기 관리)
└── static/
    ├── css/
    │   └── common.css
    └── js/
        └── common.js
```

---

## Phase 2: 공통 레이아웃 구성

### 2-1. 공통 레이아웃 (default.html)
- Thymeleaf Layout Dialect 사용
- 공통 헤더/사이드바/푸터 포함
- 페이지별 content 영역만 교체

### 2-2. 사이드바 메뉴 구조
```
📊 대시보드
📋 미션 관리
   ├── 미션풀 관리
   ├── 오늘의 미션
   ├── 추천 미션
   └── 선택 미션 (미리보기)
⚙️ 설정
   └── 절기 관리
```

### 2-3. 공통 CSS 이동
- `docs/wireframes/common.css` → `static/css/common.css`
- 필요시 Thymeleaf 문법에 맞게 수정

---

## Phase 3: 페이지별 개발 (우선순위 순)

### 3-1. 로그인 페이지
**파일**: `templates/login.html`
**참고**: `docs/wireframes/login.html`

- [ ] HTML 구조 작성
- [ ] Spring Security 연동 (추후)

### 3-2. 대시보드
**파일**: `templates/dashboard.html`
**참고**: `docs/wireframes/dashboard.html`

- [ ] 통계 카드 (전체 미션, 오늘의 미션, 추천 미션, 사용자 수)
- [ ] 빠른 링크 버튼
- [ ] Controller: `DashboardController`

### 3-3. 미션풀 관리
**파일**: `templates/mission/pool.html`
**참고**: `docs/wireframes/mission-pool.html`

- [ ] 미션 목록 테이블
- [ ] 필터링 (공간/강도/카테고리/동반/배정상태)
- [ ] 미션 추가/수정 모달
- [ ] AI 미션 생성 섹션
- [ ] Controller: `MissionPoolController`
- [ ] Service: `MissionService`

### 3-4. 오늘의 미션
**파일**: `templates/mission/daily.html`
**참고**: `docs/wireframes/daily-mission.html`

- [ ] 절기 선택 드롭다운
- [ ] 배정대기 패널 (드래그 소스)
- [ ] 날짜별 슬롯 (드래그 타겟)
- [ ] 드래그앤드롭 JS (SortableJS 사용)
- [ ] Controller: `DailyMissionController`
- [ ] Service: `DailyMissionService`

### 3-5. 추천 미션
**파일**: `templates/mission/recommended.html`
**참고**: `docs/wireframes/recommended-mission.html`

- [ ] 절기 선택
- [ ] 사용자 타입 탭 (4개)
- [ ] EnjoyType별 그룹 (자연야외/제철음식/감성콘텐츠)
- [ ] 미션 추가/제거
- [ ] Controller: `RecommendedMissionController`
- [ ] Service: `RecommendedMissionService`

### 3-6. 선택 미션 (미리보기)
**파일**: `templates/mission/selected.html`
**참고**: `docs/wireframes/selected-mission.html`

- [ ] 필터 UI (공간/강도/카테고리/동반)
- [ ] 실시간 필터링 결과 미리보기
- [ ] 오늘의미션/추천미션 제외 로직 표시
- [ ] Controller: `SelectedMissionController`

### 3-7. 설정 (절기 관리)
**파일**: `templates/settings/index.html`
**참고**: `docs/wireframes/settings.html`

- [ ] 절기 목록 테이블
- [ ] 절기 추가/수정
- [ ] Controller: `SettingsController`
- [ ] Service: `SolarTermService`

---

## Phase 4: Controller/Service 구현

### 4-1. Controller 목록
| Controller | 경로 | 설명 |
|------------|------|------|
| `DashboardController` | `/admin` | 대시보드 |
| `MissionPoolController` | `/admin/missions` | 미션풀 CRUD |
| `DailyMissionController` | `/admin/daily-missions` | 오늘의 미션 배정 |
| `RecommendedMissionController` | `/admin/recommended-missions` | 추천 미션 관리 |
| `SelectedMissionController` | `/admin/selected-missions` | 선택 미션 미리보기 |
| `SettingsController` | `/admin/settings` | 설정 |

### 4-2. API 엔드포인트 (AJAX용)
| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/missions` | 미션 목록 조회 |
| POST | `/api/admin/missions` | 미션 생성 |
| PUT | `/api/admin/missions/{id}` | 미션 수정 |
| DELETE | `/api/admin/missions/{id}` | 미션 삭제 (soft) |
| POST | `/api/admin/daily-missions/assign` | 오늘의 미션 배정 |
| DELETE | `/api/admin/daily-missions/{id}` | 배정 해제 |
| GET | `/api/admin/solar-terms` | 절기 목록 |

---

## 오늘 작업 체크리스트

### 필수 (MVP)
- [ ] Phase 1: Thymeleaf 설정 완료
- [ ] Phase 2: 공통 레이아웃 완료
- [ ] Phase 3-1: 로그인 페이지 (UI만)
- [ ] Phase 3-2: 대시보드 (UI만)
- [ ] Phase 3-3: 미션풀 관리 (UI + 목록 조회)

### 선택 (시간 여유시)
- [ ] Phase 3-4: 오늘의 미션 (드래그앤드롭)
- [ ] Phase 3-5: 추천 미션

---

## 기술 스택

| 항목 | 선택 |
|------|------|
| 템플릿 엔진 | Thymeleaf + Layout Dialect |
| CSS | 와이어프레임 CSS 재사용 |
| JS 라이브러리 | SortableJS (드래그앤드롭) |
| 아이콘 | Lucide Icons (와이어프레임 사용중) |

---

## 참고 문서
- 와이어프레임: `docs/wireframes/`
- ERD: `docs/common/ERD.md`
- 미션 아키텍처: `.claude/rules/mission-architecture.md`
- 서비스 기획: `.claude/rules/service-planning.md`