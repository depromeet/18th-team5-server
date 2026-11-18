package com.team.peektime_api.domain.stats.repository;

import com.team.peektime_api.domain.stats.dto.DailyCompletionStatsProjection;
import com.team.peektime_api.domain.stats.entity.DailyCompletionStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyCompletionStatsRepository extends JpaRepository<DailyCompletionStats, Long> {

    // 하이브리드 조회를 UNION ALL 한 문장으로 — 확정분(통계 테이블)과 미확정 구간(원본 집계)을
    // DB에서 병합해 왕복 1번 (앱 병합 대비 경계 조회 포함 3번 → 1번).
    // 동적 경계 MAX(stat_date)가 서브쿼리로 내장되어 배치 전(00:00~02:00)이나 배치 누락 시에도
    // 미확정 날짜가 아래 분기로 흡수된다. 통계 테이블이 비면 COALESCE 최솟값 경계로 전 구간 원본 집계.
    // 조회 범위가 전부 확정분이면 아래 분기는 빈 인덱스 범위라 사실상 비용 0.
    // UNION ALL이 JPQL에 없어 네이티브 — H2/MySQL 공통 문법만 사용
    @Query(value = """
            SELECT s.stat_date AS statDate,
                   s.completion_count AS completionCount,
                   s.photo_count AS photoCount
            FROM daily_completion_stats s
            WHERE s.stat_date BETWEEN :startDate AND :endDate

            UNION ALL

            SELECT c.completed_date, COUNT(*), COUNT(c.object_key)
            FROM user_mission_completion c
            WHERE c.completed_date BETWEEN :startDate AND :endDate
              AND c.completed_date > (SELECT COALESCE(MAX(stat_date), CAST('1000-01-01' AS DATE))
                                      FROM daily_completion_stats)
            GROUP BY c.completed_date

            ORDER BY statDate
            """, nativeQuery = true)
    List<DailyCompletionStatsProjection> findDailyStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 확정/미확정의 경계. 확정 배치가 "여기 다음날부터 어제까지"를 확정한다
    @Query("SELECT MAX(s.statDate) FROM DailyCompletionStats s")
    Optional<LocalDate> findMaxStatDate();

    @Modifying
    @Query("DELETE FROM DailyCompletionStats s WHERE s.statDate >= :startDate AND s.statDate <= :endDate")
    void deleteByStatDateBetween(LocalDate startDate, LocalDate endDate);
}