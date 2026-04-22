# 미션 아키텍처

## 핵심 원칙

**"선택 미션"은 미션 자체의 타입이 아니라 사용자에게 제공되는 방식이다.**

같은 미션이:
- A 절기에서는 "오늘의 미션"으로 배정
- B 절기에서는 "추천 미션"으로 배정
- C 사용자에게는 "선택 미션"으로 필터링되어 보임

---

## 테이블 구조

| 테이블 | 역할 |
|--------|------|
| `Mission` | 미션 풀 (타입 없음, 태그만 있음) |
| `DailyMission` | 특정 날짜에 배정된 오늘의 미션 |
| `RecommendedMissionPool` | 절기 + 사용자타입별 추천 미션 |
| **선택 미션** | = Mission 풀에서 위 두 개에 포함 안 된 것들 |

---

## Mission 엔티티 필드

Mission 엔티티에는 `MissionType` 필드가 **없다**. 대신 태그만 있음:

```java
public class Mission {
    // 필터링용 태그
    private SpaceType spaceType;       // 실내/실외
    private IntensityType intensityType; // 가벼운/보통/적극적
    private CompanionType companionType; // 혼자/같이
    private CategoryType categoryType;   // 음식/자연/기록/장소/감각

    // 추천미션 분류용 태그
    private EnjoyType enjoyType;       // 자연야외/제철음식/감성콘텐츠
    private UserType userType;         // 어떤 사용자 타입을 위해 생성됐는지 (참고용)

    // 상태
    private MissionStatus status;      // POOL/PENDING/ASSIGNED
}
```

---

## MissionType 사용처

`MissionType` enum (DAILY, RECOMMENDED, SELECTED)은 다음 용도로만 사용:

### 1. 통계 기록 (MissionCompletionStats)
```java
// 이 미션이 어떤 경로로 완료됐는지 기록
public class MissionCompletionStats {
    private MissionType missionType;  // DAILY, RECOMMENDED, SELECTED
}
```

### 2. API 응답
```java
// 사용자에게 "이 미션이 어떻게 제공됐는지" 표시
public class MissionResponse {
    private MissionType type;  // DAILY, RECOMMENDED, SELECTED
}
```

---

## 선택 미션 로직

선택 미션은 별도 테이블이 아니라 **쿼리로 필터링**:

```sql
SELECT * FROM mission m
WHERE m.id NOT IN (
    SELECT dm.mission_id FROM daily_mission dm
    WHERE dm.solar_term_id = :currentSolarTermId
)
AND m.id NOT IN (
    SELECT rmp.mission_id FROM recommended_mission_pool rmp
    WHERE rmp.solar_term_id = :currentSolarTermId
    AND rmp.user_type = :userType
)
AND m.space_type = :filterSpace        -- 필터 조건
AND m.intensity_type = :filterIntensity
-- ...
```
