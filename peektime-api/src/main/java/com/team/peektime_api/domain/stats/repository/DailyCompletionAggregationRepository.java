package com.team.peektime_api.domain.stats.repository;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.stats.dto.DailyCompletionStatsProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// 원본(user_mission_completion) 집계 전담. 통계 테이블 도입 후에는
// 아직 확정되지 않은 날짜(마지막 확정일 이후 ~ 오늘)의 실시간 조회와
// 새벽 확정 배치의 재집계에만 쓰인다 — 둘 다 completed_date 인덱스 range scan
public interface DailyCompletionAggregationRepository extends Repository<UserMissionCompletion, Long> {

    @Query("""
            SELECT c.completedDate AS statDate,
                   COUNT(c) AS completionCount,
                   COUNT(c.objectKey) AS photoCount
            FROM UserMissionCompletion c
            WHERE c.completedDate >= :startDate
              AND c.completedDate <= :endDate
            GROUP BY c.completedDate
            ORDER BY c.completedDate
            """)
    List<DailyCompletionStatsProjection> aggregateDailyStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 통계 테이블이 비어 있을 때(최초 배치) 확정을 어느 날짜부터 시작할지 결정
    @Query("SELECT MIN(c.completedDate) FROM UserMissionCompletion c")
    Optional<LocalDate> findMinCompletedDate();
}
