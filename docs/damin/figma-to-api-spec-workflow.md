# Figma → 코드베이스 분석 → Confluence API 명세 초안 자동화 워크플로우

## 개요

디자인팀의 Figma 와이어프레임과 정보구조도(IA)를 분석하고, 현재 코드베이스·ERD·아키텍처와 대조하여 Confluence에 API 명세 초안을 자동으로 작성한 작업 흐름을 정리한 문서다.

---

## 배경

API 명세 초안을 작성할 때 보통 다음 두 가지 문제가 생긴다.

1. **디자인 화면 기반으로 작성하면** 실제 DB 구조나 Enum 값과 맞지 않는 명세가 나온다.
2. **코드베이스만 보고 작성하면** 화면에서 실제로 필요한 데이터를 빠뜨린다.

이번 작업은 Figma 와이어프레임 + 코드베이스 + ERD를 동시에 분석해서 이 두 문제를 한 번에 잡는 방식으로 진행했다.

---

## 사용한 도구

| 도구 | 용도 |
|------|------|
| **Figma CLI** (`figma` npm package) | Figma 파일 구조 및 텍스트 노드 추출 |
| **Figma REST API** | 특정 노드 상세 조회 (정보구조도 등) |
| **Claude Code** | 코드베이스·ERD 분석 및 명세 작성 |
| **Confluence REST API** | Confluence 페이지 생성·업데이트 |

> **왜 Figma MCP 대신 Figma CLI를 사용했나?**
>
> Figma MCP 서버를 연결하면 디자인 파일 전체를 컨텍스트로 불러오는 과정에서 **컨텍스트 초과 오류**나 **응답 잘림** 현상이 발생할 수 있다. Figma CLI + REST API를 직접 호출하면 필요한 노드만 선택적으로 가져올 수 있어 안정적이다.
>
> **Confluence MCP** 는 해당 Atlassian 인스턴스에 앱이 설치되어 있지 않아 사용 불가였다. 대신 Confluence REST API + Atlassian API Token으로 동일하게 처리했다.

---

## 전체 플로우

```
1. Figma 와이어프레임 분석
        ↓
2. 코드베이스 + ERD 분석
        ↓
3. 불일치 항목 도출
        ↓
4. API 명세 초안 작성
        ↓
5. Confluence 업데이트
        ↓
6. 코드 수정 + Git 커밋
```

---

## Step 1. Figma 와이어프레임 분석

### Figma CLI로 파일 구조 파악

```bash
# Figma Personal Access Token 설정
figma --token {FIGMA_TOKEN}

# Figma REST API로 파일 전체 구조 조회 (depth=2)
curl -H "X-Figma-Token: {TOKEN}" \
  "https://api.figma.com/v1/files/{FILE_ID}?depth=2"
```

파일 ID는 Figma URL에서 추출한다.
```
https://www.figma.com/design/{FILE_ID}/...
```

### 화면 목록 및 텍스트 노드 추출

depth를 높여 각 프레임 내 텍스트 노드까지 재귀 탐색했다.

```bash
# depth=6으로 텍스트 내용까지 추출
curl -H "X-Figma-Token: {TOKEN}" \
  "https://api.figma.com/v1/files/{FILE_ID}?depth=6"
```

Python으로 파싱해서 화면 트리와 텍스트 내용을 정리했다.

**추출된 화면 목록 (peektime 와이어프레임 기준)**

| 화면 | 주요 구성 요소 |
|------|-------------|
| 절기 소개 | header, top, card list, footer |
| 홈 | header, card list, footer (GNB) |
| 미션 추천 | top, card list, fab, footer |
| 캘린더 | calendar, fab, footer |
| 캘린더 (상세) | top, card 1, card 2, footer |

### 정보구조도(IA) 조회

특정 노드 ID로 사용 흐름 페이지를 별도 조회했다.

```bash
# node-id는 Figma URL의 ?node-id= 파라미터
curl -H "X-Figma-Token: {TOKEN}" \
  "https://api.figma.com/v1/files/{FILE_ID}/nodes?ids=4-179&depth=5"
```

이를 통해 와이어프레임에 없는 화면들을 추가로 파악했다.

**정보구조도에서 파악한 전체 앱 구조**

```
시작 (온보딩)
└── 메인 탭
    ├── 홈
    │   ├── 절기 소개
    │   ├── 오늘의 미션
    │   ├── 제철 미션 추천
    │   └── 미션 기록하기
    ├── 캘린더
    ├── 절기 리포트 (절기 결산)
    ├── 제철 기록 (자유 기록)
    └── 마이페이지
        ├── 온보딩 (취향 설정)
        └── 설정 (알림, 1:1 문의, 약관)
```

---

## Step 2. 코드베이스 + ERD 분석

### 분석 대상

```
peektime-admin/
  domain/mission/entity/     # Mission, DailyMission, RecommendedMissionPool
  domain/solarterm/entity/   # SolarTerm
  domain/stats/entity/       # MissionCompletionStats
  global/common/enums/       # 전체 Enum 정의

peektime-api/
  domain/user/entity/        # User, UserOnboarding
  domain/mission/entity/     # UserMissionCompletion
  domain/record/entity/      # UserRecord
  domain/stats/entity/       # MissionCompletionStats
  global/response/           # SuccessCode, ErrorCode

docs/common/ERD.md           # Mermaid ERD
```

---

## Step 3. 불일치 항목 도출

코드베이스와 ERD를 대조했을 때 발견된 문제들이다.

### 🔴 즉시 수정 필요

| 항목 | 초안에서 작성한 값 | 실제 코드 값 |
|------|-----------------|------------|
| CompanionType | `ALONE` | **`SOLO`** |
| EnjoyType | `NATURE / FOOD / CONTENT` | **`NATURE_OUTDOOR / SEASONAL_FOOD / CULTURE_CONTENT`** |
| UserType | `FOOD_ENTHUSIAST` | **`LIFE_CREATOR`** |
| ERD.md EnjoyType | `CULTURE_CONTENT` | **`CULTURE_CONTENT`** (코드 기준이 맞음) |

### 🟡 설계 합의 필요

- **SolarTerm에 `season` 필드 없음**: 와이어프레임에 봄/여름/가을/겨울 칩이 있으나 컬럼 없음
- **UserRecord vs UserMissionCompletion 역할 중복처럼 보임**: 정보구조도 확인 후 역할 명확화 (UserRecord = 제철 기록 탭 자유 기록, UserMissionCompletion = 미션 완료 기록)
- **인증 구조 없음**: device UUID 기반으로 결정 → DeviceAuth 엔티티 신규 추가
- **MissionCompletionStats 모듈 중복**: admin/api 양쪽에 존재

---

## Step 4. API 명세 초안 작성

코드베이스의 실제 값을 기반으로 명세를 작성했다.

**최종 명세 구성**

| 섹션 | 주요 API |
|------|---------|
| 1. 인증 | `POST /auth/device`, `POST /auth/refresh`, `POST /auth/logout` |
| 2. 온보딩 | `POST/PUT/GET /onboarding` |
| 3. 절기 | `GET /solar-terms/current`, `GET /solar-terms` |
| 4. 홈 | `GET /home` |
| 5. 미션 | 오늘의 미션 / 추천 미션 / 선택 미션 조회, 미션 완료 기록 |
| 6. 캘린더 | 월별 기록 조회, 날짜별 상세 조회 |
| 7. 제철 기록 | `POST/GET/DELETE /records` |
| 8. 마이페이지 | 사용자 정보 조회·수정, 푸시토큰 갱신 |

**인증 방식 확정 내용**

소셜 로그인 없이 iOS 기기 고유 UUID(`identifierForVendor`)를 서버에 전달하면 서버가 User를 조회·생성 후 JWT를 발급하는 방식으로 결정했다. Refresh Token은 DeviceAuth 테이블에 저장하고 Token Rotation 방식을 사용한다.

---

## Step 5. Confluence 업데이트

Atlassian API Token을 발급해 REST API로 직접 업데이트했다.

```bash
# 페이지 버전 확인
curl -u "{EMAIL}:{API_TOKEN}" \
  "https://{DOMAIN}.atlassian.net/wiki/rest/api/content/{PAGE_ID}?expand=version"

# 페이지 업데이트
curl -X PUT \
  -u "{EMAIL}:{API_TOKEN}" \
  -H "Content-Type: application/json" \
  "https://{DOMAIN}.atlassian.net/wiki/rest/api/content/{PAGE_ID}" \
  -d '{
    "version": { "number": {현재버전 + 1} },
    "title": "API 명세서 초안",
    "type": "page",
    "body": {
      "storage": {
        "value": "{HTML_CONTENT}",
        "representation": "storage"
      }
    }
  }'
```

Confluence Storage Format(HTML 기반)으로 콘텐츠를 작성했다. 코드 블록, 테이블, 패널(info/warning/note) 등을 `data-type` 속성으로 표현한다.

```html
<!-- 패널 -->
<div data-type="panel-note"><p>내용</p></div>

<!-- 코드 블록 -->
<pre><code class="language-json">{ "key": "value" }</code></pre>
```

---

## Step 6. 코드 수정 + Git 커밋

분석 과정에서 발견한 불일치를 실제 코드에도 반영했다.

```
feat: iOS device UUID 기반 인증 구조 추가
- User 엔티티: email 제거, device_uuid(UK) 추가
- DeviceAuth 엔티티 신규 추가 (refresh token, revoke, rotation 지원)

docs: ERD 인증 구조 반영 및 테이블 설명 정비
- User: email 제거, device_uuid UK 추가
- DeviceAuth 테이블 추가
- UserRecord 설명 명확화
```

---

## 핵심 인사이트

### 1. Figma CLI + REST API가 MCP보다 안정적인 경우가 있다

Figma MCP 서버는 디자인 파일 전체를 한 번에 로드할 때 컨텍스트를 많이 소비하거나 오류가 날 수 있다. CLI와 REST API를 직접 사용하면 **필요한 노드만 선택적으로 조회**할 수 있어 더 효율적이다.

depth 파라미터로 조회 깊이를 조절하고, 특정 노드 ID를 타겟하면 불필요한 데이터를 줄일 수 있다.

### 2. 와이어프레임만 보면 코드와 불일치가 생긴다

Figma에서 `ALONE`으로 라벨링된 것이 코드에서는 `SOLO`인 경우처럼, 디자인과 코드 사이의 용어 불일치는 흔하다. API 명세를 작성할 때는 **항상 실제 Enum 코드를 기준**으로 해야 한다.

### 3. 정보구조도(IA)는 와이어프레임보다 더 많은 정보를 담고 있다

와이어프레임에 없는 화면(절기 리포트, 위젯, 다이나믹 아일랜드 등)과 TBD 메모, 설계 의도까지 정보구조도에서 파악할 수 있었다. Figma에서 IA 페이지를 별도로 만들어두면 분석 시 유용하다.

### 4. API 명세와 함께 ERD 불일치도 잡을 수 있다

명세를 작성하면서 ERD와 코드 사이의 불일치(CULTURE_CONTENT vs CULTURE_CONTENT 등)를 자연스럽게 발견했다. 명세 작성을 단순한 문서화가 아니라 **코드·설계 리뷰의 트리거**로 활용할 수 있다.

---

## 참고 링크

- Confluence 페이지: https://depromeet.atlassian.net/wiki/spaces/5/pages/8945693/API
- Figma 와이어프레임: https://www.figma.com/design/fYBuOIomsns8il6E5wreZQ/peektime-와이어프레임
- Figma REST API 문서: https://www.figma.com/developers/api
- Confluence REST API 문서: https://developer.atlassian.com/cloud/confluence/rest/v1/intro/
