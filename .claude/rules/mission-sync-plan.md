# 미션 동기화 작업 계획

## 개요

Admin과 API가 별도 DB를 사용하며, **배치 동기화 방식**으로 데이터 동기화.

```
Admin DB (마스터)
     │
     │  "동기화" 버튼 클릭 시
     ▼
API DB (서비스용)
```

---

## 동기화 방식

### 실시간 X → 배치 동기화 O

- Admin에서 CRUD 작업 자유롭게 수행
- 작업 완료 후 "동기화" 버튼 클릭
- API DB에 현재 상태 전체 반영

### 동기화 API

```
POST /internal/sync/missions
  → Admin의 모든 Mission을 API에 upsert

POST /internal/sync/daily-missions?solarTermId={id}
  → 해당 절기의 DailyMission 전체 동기화

POST /internal/sync/recommended-missions?solarTermId={id}
  → 해당 절기의 RecommendedMissionPool 전체 동기화
```

---

## Phase 1: API 엔티티 추가

API 모듈에 동기화 대상 엔티티 추가.

### 1-1. Mission 엔티티
- [ ] `Mission.java` 생성 (Admin과 동일한 구조)
- [ ] `MissionRepository.java` 생성

### 1-2. DailyMission 엔티티
- [ ] `DailyMission.java` 생성
- [ ] `DailyMissionRepository.java` 생성

### 1-3. RecommendedMissionPool 엔티티
- [ ] `RecommendedMissionPool.java` 생성
- [ ] `RecommendedMissionPoolRepository.java` 생성

### 1-4. Enum 확인
- [ ] SpaceType, IntensityType, CategoryType 등 API 모듈에 있는지 확인
- [ ] 없으면 추가

---

## Phase 2: API 동기화 엔드포인트

### 2-1. 컨트롤러 생성
- [ ] `InternalSyncController.java` 생성

```java
@RestController
@RequestMapping("/internal/sync")
public class InternalSyncController {

    @PostMapping("/missions")
    public void syncMissions(@RequestBody List<MissionSyncDto> missions) {
        // 전체 upsert
    }

    @PostMapping("/daily-missions")
    public void syncDailyMissions(
            @RequestParam Long solarTermId,
            @RequestBody List<DailyMissionSyncDto> dailyMissions) {
        // 해당 절기 전체 upsert
    }

    @PostMapping("/recommended-missions")
    public void syncRecommendedMissions(
            @RequestParam Long solarTermId,
            @RequestBody List<RecommendedMissionSyncDto> recommendedMissions) {
        // 해당 절기 전체 upsert
    }
}
```

### 2-2. 서비스 생성
- [ ] `MissionSyncService.java`
- [ ] `DailyMissionSyncService.java`
- [ ] `RecommendedMissionSyncService.java`

### 2-3. DTO 생성
- [ ] `MissionSyncDto.java`
- [ ] `DailyMissionSyncDto.java`
- [ ] `RecommendedMissionSyncDto.java`

### 2-4. Upsert 로직
```java
// Mission 동기화 예시
@Transactional
public void syncMissions(List<MissionSyncDto> dtos) {
    for (MissionSyncDto dto : dtos) {
        Mission mission = missionRepository.findById(dto.getId())
            .orElse(new Mission());

        mission.update(dto);  // 또는 upsert
        missionRepository.save(mission);
    }
}
```

---

## Phase 3: Admin 동기화 클라이언트

### 3-1. ApiSyncClient 생성
- [ ] `ApiSyncClient.java` 생성

```java
@Component
public class ApiSyncClient {

    private final RestClient restClient;

    public void syncMissions(List<Mission> missions) {
        restClient.post()
            .uri("/internal/sync/missions")
            .body(toSyncDtos(missions))
            .retrieve();
    }

    public void syncDailyMissions(Long solarTermId, List<DailyMission> dailyMissions) {
        restClient.post()
            .uri("/internal/sync/daily-missions?solarTermId={id}", solarTermId)
            .body(toSyncDtos(dailyMissions))
            .retrieve();
    }

    public void syncRecommendedMissions(Long solarTermId, List<RecommendedMissionPool> missions) {
        restClient.post()
            .uri("/internal/sync/recommended-missions?solarTermId={id}", solarTermId)
            .body(toSyncDtos(missions))
            .retrieve();
    }
}
```

### 3-2. 동기화 서비스
- [ ] `SyncService.java` 생성

```java
@Service
public class SyncService {

    public void syncAllMissions() {
        List<Mission> missions = missionRepository.findAllByDeletedFalse();
        apiSyncClient.syncMissions(missions);
    }

    public void syncDailyMissions(Long solarTermId) {
        List<DailyMission> dailyMissions = dailyMissionRepository
            .findBySolarTermId(solarTermId);
        apiSyncClient.syncDailyMissions(solarTermId, dailyMissions);
    }

    public void syncRecommendedMissions(Long solarTermId) {
        List<RecommendedMissionPool> missions = recommendedMissionPoolRepository
            .findBySolarTermId(solarTermId);
        apiSyncClient.syncRecommendedMissions(solarTermId, missions);
    }
}
```

### 3-3. 동기화 API (Admin)
- [ ] `SyncController.java` 생성

```java
@RestController
@RequestMapping("/api/admin/sync")
public class SyncController {

    @PostMapping("/missions")
    public ResponseEntity<?> syncMissions() {
        syncService.syncAllMissions();
        return ResponseEntity.ok(Map.of("message", "미션 동기화 완료"));
    }

    @PostMapping("/daily-missions/{solarTermId}")
    public ResponseEntity<?> syncDailyMissions(@PathVariable Long solarTermId) {
        syncService.syncDailyMissions(solarTermId);
        return ResponseEntity.ok(Map.of("message", "오늘의 미션 동기화 완료"));
    }

    @PostMapping("/recommended-missions/{solarTermId}")
    public ResponseEntity<?> syncRecommendedMissions(@PathVariable Long solarTermId) {
        syncService.syncRecommendedMissions(solarTermId);
        return ResponseEntity.ok(Map.of("message", "추천 미션 동기화 완료"));
    }
}
```

### 3-4. Admin UI에 동기화 버튼 추가
- [ ] 미션 풀 페이지에 "미션 동기화" 버튼
- [ ] 오늘의 미션 페이지에 "오늘의 미션 동기화" 버튼
- [ ] 추천 미션 페이지에 "추천 미션 동기화" 버튼

---

## Phase 4: 선택 미션 API (API 모듈)

### 4-1. 선택 미션 조회 API
- [ ] `SelectedMissionController.java`
- [ ] `SelectedMissionService.java`

```java
@GetMapping("/missions/selected")
public List<MissionResponse> getSelectedMissions(
        @RequestParam Long solarTermId,
        @RequestParam UserType userType,
        @RequestParam(required = false) SpaceType spaceType,
        @RequestParam(required = false) IntensityType intensityType,
        // ... 기타 필터
) {
    return selectedMissionService.getSelectedMissions(solarTermId, userType, filters);
}
```

### 4-2. 선택 미션 쿼리
```sql
SELECT * FROM mission m
WHERE m.deleted = false
  AND m.id NOT IN (
      SELECT dm.mission_id FROM daily_mission dm
      WHERE dm.solar_term_id = :solarTermId
  )
  AND m.id NOT IN (
      SELECT rmp.mission_id FROM recommended_mission_pool rmp
      WHERE rmp.solar_term_id = :solarTermId
      AND rmp.user_type = :userType
  )
  AND (:spaceType IS NULL OR m.space_type = :spaceType)
  AND (:intensityType IS NULL OR m.intensity_type = :intensityType)
  -- 기타 필터...
```

---

## Phase 5: 기존 로직 정리

### 5-1. 홈 화면 로직 변경
- [ ] 기존: Admin API 호출 + 캐시
- [ ] 변경: 로컬 DB 조회 (DailyMission 테이블)
- [ ] `HomeCardService` 수정

### 5-2. Admin API 호출 제거 (선택)
- [ ] `AdminClient` 사용처 확인
- [ ] 로컬 DB 조회로 대체 가능한 부분 정리

---

## 작업 순서 요약

```
Phase 1: API 엔티티 추가
         └── Mission, DailyMission, RecommendedMissionPool
                      ↓
Phase 2: API 동기화 엔드포인트
         └── POST /internal/sync/*
                      ↓
Phase 3: Admin 동기화 클라이언트
         └── 동기화 버튼 + API 호출
                      ↓
Phase 4: 선택 미션 API
         └── 필터링 조회 구현
                      ↓
Phase 5: 기존 로직 정리
         └── 홈 화면 등 수정
```

---

## 사용 시나리오

### 절기 준비 (서비스 시작 전)
```
1. Admin에서 LLM으로 미션 생성
2. 미션 검토/수정
3. 오늘의 미션 배정 (드래그앤드롭)
4. 추천 미션 등록
5. "동기화" 버튼 클릭
6. API DB 준비 완료 → 서비스 시작
```

### 운영 중 수정
```
1. Admin에서 미션 수정
2. "동기화" 버튼 클릭
3. API에 반영 완료
```

---

## 결정 사항

- [x] 동기화 방식: 배치 동기화 (버튼 클릭)
- [ ] 내부 API 보안: API Key 또는 IP 화이트리스트
- [x] ID 전략: Admin ID = API ID (동일하게 유지)
