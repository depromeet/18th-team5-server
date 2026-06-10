# 랭킹 대시보드 구현 계획

## 개요

Admin에서 사용자별 미션 완료 랭킹을 확인할 수 있는 대시보드를 추가합니다.
상위권 사용자에게 보상을 제공하기 위한 목적입니다.

---

## 구현 항목

```
peektime-admin/
├── domain/stats/
│   ├── repository/UserMissionLogRepository.java  ← 쿼리 추가
│   ├── service/StatsService.java                 ← 메서드 추가
│   ├── dto/UserRankingResponse.java              ← 신규
│   └── controller/RankingController.java         ← 신규
└── resources/templates/
    ├── stats/ranking.html                        ← 신규
    └── fragments/sidebar.html                    ← 메뉴 추가
```

---

## 1. Repository 쿼리 추가

**파일**: `UserMissionLogRepository.java`

```java
public interface UserMissionLogRepository extends JpaRepository<UserMissionLog, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    // 절기별 사용자 랭킹 조회
    @Query("""
        SELECT u.userUuid as userUuid, COUNT(u) as completionCount
        FROM UserMissionLog u
        WHERE u.solarTermId = :solarTermId
        GROUP BY u.userUuid
        ORDER BY COUNT(u) DESC
        """)
    List<UserRankingProjection> findRankingBySolarTerm(@Param("solarTermId") Long solarTermId);

    // 전체 기간 사용자 랭킹 조회
    @Query("""
        SELECT u.userUuid as userUuid, COUNT(u) as completionCount
        FROM UserMissionLog u
        GROUP BY u.userUuid
        ORDER BY COUNT(u) DESC
        """)
    List<UserRankingProjection> findOverallRanking();
}
```

---

## 2. Projection 인터페이스 추가

**파일**: `UserRankingProjection.java` (신규)

```java
package com.team.peektime_admin.domain.stats.dto;

public interface UserRankingProjection {
    String getUserUuid();
    Long getCompletionCount();
}
```

---

## 3. Response DTO 추가

**파일**: `UserRankingResponse.java` (신규)

```java
package com.team.peektime_admin.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "사용자 랭킹 응답")
@Getter
@Builder
public class UserRankingResponse {

    @Schema(description = "순위")
    private int rank;

    @Schema(description = "사용자 UUID")
    private String userUuid;

    @Schema(description = "미션 완료 횟수")
    private Long completionCount;

    public static UserRankingResponse of(int rank, UserRankingProjection projection) {
        return UserRankingResponse.builder()
                .rank(rank)
                .userUuid(projection.getUserUuid())
                .completionCount(projection.getCompletionCount())
                .build();
    }
}
```

---

## 4. Service 메서드 추가

**파일**: `StatsService.java`

```java
// 기존 코드 유지 + 아래 추가

/**
 * 절기별 사용자 랭킹 조회
 */
public List<UserRankingResponse> getRankingBySolarTerm(Long solarTermId, int limit) {
    List<UserRankingProjection> projections = userMissionLogRepository
            .findRankingBySolarTerm(solarTermId);

    return toRankingResponse(projections, limit);
}

/**
 * 전체 기간 사용자 랭킹 조회
 */
public List<UserRankingResponse> getOverallRanking(int limit) {
    List<UserRankingProjection> projections = userMissionLogRepository
            .findOverallRanking();

    return toRankingResponse(projections, limit);
}

private List<UserRankingResponse> toRankingResponse(List<UserRankingProjection> projections, int limit) {
    List<UserRankingResponse> rankings = new ArrayList<>();
    int rank = 1;

    for (UserRankingProjection projection : projections) {
        if (rank > limit) break;
        rankings.add(UserRankingResponse.of(rank++, projection));
    }

    return rankings;
}
```

---

## 5. Controller 추가

**파일**: `RankingController.java` (신규)

```java
package com.team.peektime_admin.domain.stats.controller;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.domain.stats.dto.UserRankingResponse;
import com.team.peektime_admin.domain.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final StatsService statsService;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String rankingPage(
            @RequestParam(required = false) Long solarTermId,
            @RequestParam(defaultValue = "50") int limit,
            Model model
    ) {
        // 절기 목록
        List<SolarTerm> solarTerms = solarTermRepository.findAllByOrderByStartDateDesc();
        model.addAttribute("solarTerms", solarTerms);

        // 현재 절기
        SolarTerm currentTerm = solarTermRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate.now(), LocalDate.now())
                .orElse(null);
        model.addAttribute("currentTerm", currentTerm);

        // 랭킹 조회
        List<UserRankingResponse> rankings;
        if (solarTermId != null) {
            rankings = statsService.getRankingBySolarTerm(solarTermId, limit);
            model.addAttribute("selectedSolarTermId", solarTermId);
        } else if (currentTerm != null) {
            rankings = statsService.getRankingBySolarTerm(currentTerm.getId(), limit);
            model.addAttribute("selectedSolarTermId", currentTerm.getId());
        } else {
            rankings = statsService.getOverallRanking(limit);
            model.addAttribute("selectedSolarTermId", null);
        }

        model.addAttribute("rankings", rankings);
        model.addAttribute("menu", "ranking");

        return "stats/ranking";
    }
}
```

---

## 6. HTML 템플릿 추가

**파일**: `templates/stats/ranking.html` (신규)

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/default}">
<head>
    <title>사용자 랭킹</title>
    <th:block layout:fragment="styles">
        <style>
            .ranking-filters {
                display: flex;
                gap: 16px;
                margin-bottom: 24px;
            }

            .ranking-table {
                background: white;
                border-radius: 12px;
                border: 1px solid var(--gray-200);
                overflow: hidden;
            }

            .rank-badge {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 32px;
                height: 32px;
                border-radius: 50%;
                font-weight: 700;
                font-size: 14px;
            }

            .rank-1 { background: #FFD700; color: #000; }
            .rank-2 { background: #C0C0C0; color: #000; }
            .rank-3 { background: #CD7F32; color: #fff; }
            .rank-default { background: var(--gray-100); color: var(--gray-600); }

            .completion-count {
                font-weight: 600;
                color: var(--primary);
            }

            .user-uuid {
                font-family: monospace;
                font-size: 13px;
                color: var(--gray-600);
            }
        </style>
    </th:block>
</head>
<body>
<th:block layout:fragment="content">
    <div class="page-header">
        <h1 class="page-title">사용자 랭킹</h1>
        <p class="page-description">미션 완료 횟수 기준 사용자 랭킹입니다</p>
    </div>

    <!-- 필터 -->
    <div class="ranking-filters">
        <select id="solarTermSelect" class="form-control" style="width: 200px;"
                onchange="location.href='/admin/ranking?solarTermId=' + this.value">
            <option value="">전체 기간</option>
            <option th:each="term : ${solarTerms}"
                    th:value="${term.id}"
                    th:text="${term.name}"
                    th:selected="${term.id == selectedSolarTermId}">
            </option>
        </select>
    </div>

    <!-- 랭킹 테이블 -->
    <div class="ranking-table">
        <table class="table">
            <thead>
            <tr>
                <th style="width: 80px;">순위</th>
                <th>사용자 UUID</th>
                <th style="width: 150px;">미션 완료 횟수</th>
            </tr>
            </thead>
            <tbody>
            <tr th:if="${#lists.isEmpty(rankings)}">
                <td colspan="3" style="text-align: center; padding: 48px; color: var(--gray-500);">
                    아직 미션 완료 기록이 없습니다
                </td>
            </tr>
            <tr th:each="ranking : ${rankings}">
                <td>
                    <span class="rank-badge"
                          th:classappend="${ranking.rank == 1} ? 'rank-1' : (${ranking.rank == 2} ? 'rank-2' : (${ranking.rank == 3} ? 'rank-3' : 'rank-default'))"
                          th:text="${ranking.rank}">1</span>
                </td>
                <td>
                    <span class="user-uuid" th:text="${ranking.userUuid}">abc-123-def</span>
                </td>
                <td>
                    <span class="completion-count" th:text="${ranking.completionCount} + '회'">10회</span>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</th:block>
</body>
</html>
```

---

## 7. 사이드바 메뉴 추가

**파일**: `fragments/sidebar.html`

```html
<!-- 설정 메뉴 위에 추가 -->
<a th:href="@{/admin/ranking}" class="nav-item" th:classappend="${menu == 'ranking'} ? 'active'">
    <span class="nav-icon">🏆</span>
    사용자 랭킹
</a>
```

---

## 완료 후 화면

```
┌─────────────────────────────────────────────────┐
│  사용자 랭킹                                      │
│  미션 완료 횟수 기준 사용자 랭킹입니다               │
├─────────────────────────────────────────────────┤
│  [절기 선택 ▼]                                   │
├─────────────────────────────────────────────────┤
│  순위    사용자 UUID              미션 완료 횟수   │
│  🥇 1    abc-123-def-456          25회           │
│  🥈 2    xyz-789-ghi-012          20회           │
│  🥉 3    mno-345-pqr-678          18회           │
│     4    stu-901-vwx-234          15회           │
│     5    ...                      12회           │
└─────────────────────────────────────────────────┘
```

---

## 작업 순서

1. [ ] `UserRankingProjection.java` 생성
2. [ ] `UserMissionLogRepository.java` 쿼리 추가
3. [ ] `UserRankingResponse.java` 생성
4. [ ] `StatsService.java` 메서드 추가
5. [ ] `RankingController.java` 생성
6. [ ] `templates/stats/ranking.html` 생성
7. [ ] `fragments/sidebar.html` 메뉴 추가