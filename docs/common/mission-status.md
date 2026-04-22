# 미션 상태 관리 구조

## 핵심 원칙

**Mission 엔티티에 "배정 상태"는 없다.**

배정 여부는 관계 테이블(DailyMission, RecommendedMissionPool)의 존재로 판단한다.

---

## 테이블 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     Mission (미션풀)                         │
│  - 모든 미션이 여기에 저장                                    │
│  - 상태 없음, 태그만 있음                                     │
│  - deleted (soft delete용)만 있으면 됨                       │
└─────────────────────────────────────────────────────────────┘
          │                              │
          ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────────┐
│     DailyMission        │    │   RecommendedMissionPool    │
│   (오늘의 미션 배정)     │    │   (추천 미션 배정)           │
│                         │    │                             │
│  - solar_term_id        │    │   - solar_term_id           │
│  - mission_id           │    │   - user_type               │
│  - date (nullable)      │    │   - mission_id              │
│  - display_order        │    │   - enjoy_type              │
│                         │    │   - display_order           │
│  date=NULL → 배정대기   │    └─────────────────────────────┘
│  date=값   → 배정완료   │
└─────────────────────────┘
```

---

## 미션 분류 방식

| 분류 | 판단 기준 |
|------|----------|
| **오늘의 미션** | `DailyMission` 테이블에 존재 |
| **추천 미션** | `RecommendedMissionPool` 테이블에 존재 |
| **선택 미션** | 위 두 테이블 어디에도 없는 Mission |

---

## Mission 엔티티 필드

```java
public class Mission {
    private Long id;
    private String title;
    private String description;

    // 필터링용 태그
    private SpaceType spaceType;       // INDOOR, OUTDOOR
    private IntensityType intensityType; // LIGHT(5분이내), MODERATE(30분이내), ACTIVE(1시간+)
    private CompanionType companionType; // ALONE, TOGETHER
    private CategoryType categoryType;   // FOOD, NATURE, RECORD, PLACE, SENSE

    // 추천미션 분류용 태그 (LLM 생성 시 부여)
    private EnjoyType enjoyType;       // OUTDOOR_NATURE, SEASONAL_FOOD, EMOTIONAL_CONTENT
    private UserType userType;         // NATURE_EXPLORER, NEIGHBORHOOD_WALKER, SEASONAL_FOODIE, DAILY_OBSERVER

    // Soft Delete
    private boolean deleted;
    private LocalDateTime deletedAt;
}
```

---

## DailyMission 엔티티 필드

```java
public class DailyMission {
    private Long id;
    private Mission mission;
    private SolarTerm solarTerm;
    private LocalDate date;        // NULL이면 배정대기, 값 있으면 배정완료
    private Integer displayOrder;
}
```

**배정 상태 판단:**
- `date = NULL` → 배정대기 (오늘의미션 페이지 좌측 패널)
- `date = 값` → 배정완료 (스케줄 보드에 표시)

```sql
-- 배정대기 미션 조회
SELECT * FROM daily_mission
WHERE solar_term_id = :solarTermId AND date IS NULL;

-- 배정완료 미션 조회 (날짜별)
SELECT * FROM daily_mission
WHERE solar_term_id = :solarTermId AND date IS NOT NULL
ORDER BY date, display_order;
```

---

## Admin 플로우

### 1. 미션 생성 (미션풀 관리)

```
LLM으로 미션 생성
    ↓
Mission 테이블에 저장 (모든 태그 포함)
    ↓
미션풀에 표시됨
```

### 2. 오늘의 미션 배정

**Step 1: 배정대기로 이동**
```
미션풀 관리 페이지
    ↓
미션 선택 → "오늘의미션으로" 클릭
    ↓
DailyMission 테이블에 저장 (date = NULL)
    ↓
오늘의 미션 페이지 "배정대기" 목록에 표시
```

**Step 2: 날짜에 배정**
```
오늘의 미션 페이지 진입
    ↓
절기 선택 (예: 입춘)
    ↓
배정대기 목록에서 미션 드래그
    ↓
날짜 슬롯에 드롭
    ↓
DailyMission.date 업데이트 (NULL → 실제 날짜)
```

### 3. 추천 미션 배정

```
미션풀에서 미션 선택 (EnjoyType, UserType 태그 있는 미션)
    ↓
"추천미션으로 이동" 클릭
    ↓
RecommendedMissionPool 테이블에 저장
    ↓
해당 UserType + EnjoyType 그룹에 자동 분류
```

---

## Admin 필터 (미션풀 관리)

| 필터 | 옵션 |
|------|------|
| **배정** | 전체 / 오늘의미션 / 추천미션 / 미배정 |
| **공간** | 전체 / 실내 / 실외 |
| **강도** | 전체 / 5분이내 / 30분이내 / 1시간+ |
| **동반** | 전체 / 혼자 / 같이 |
| **카테고리** | 전체 / 음식 / 자연 / 기록 / 장소 / 감각 |

---

## 선택 미션 쿼리 예시

```sql
SELECT * FROM mission m
WHERE m.deleted = false
AND m.id NOT IN (
    SELECT dm.mission_id FROM daily_mission dm
    WHERE dm.solar_term_id = :currentSolarTermId
)
AND m.id NOT IN (
    SELECT rmp.mission_id FROM recommended_mission_pool rmp
    WHERE rmp.solar_term_id = :currentSolarTermId
)
-- 추가 필터 조건
AND (:spaceType IS NULL OR m.space_type = :spaceType)
AND (:intensityType IS NULL OR m.intensity_type = :intensityType)
AND (:companionType IS NULL OR m.companion_type = :companionType)
AND (:categoryType IS NULL OR m.category_type = :categoryType)
```

---

## 페이지별 기능 권한

| 페이지 | 생성 | 수정 | 삭제 | 풀로 이동 | 배정 |
|--------|------|------|------|----------|------|
| **미션풀 관리** | O | O | O | - | O (오늘의/추천미션으로) |
| **오늘의 미션** | - | - | - | O | O (날짜에 드래그) |
| **추천 미션** | - | - | - | O | - |
| **선택 미션** | - | - | - | - | - (미리보기 전용) |

### 원칙
- **미션 데이터 수정/삭제는 미션풀 관리에서만 가능**
- 오늘의 미션, 추천 미션 페이지에서는 **"풀로 이동"** 기능만 제공
- "풀로 이동" = 관계 테이블에서 레코드 삭제 (DailyMission 또는 RecommendedMissionPool)
- 미션 자체는 삭제되지 않고 미션풀로 돌아감

---

## 기존 MissionStatus 제거

기존에 있던 `MissionStatus` enum (POOL, PENDING, ASSIGNED)은 **제거**한다.

- ~~POOL~~ → Mission 테이블에 있으면 풀에 있는 것
- ~~PENDING~~ → 불필요 (중간 상태 없음)
- ~~ASSIGNED~~ → DailyMission 또는 RecommendedMissionPool에 있으면 배정된 것