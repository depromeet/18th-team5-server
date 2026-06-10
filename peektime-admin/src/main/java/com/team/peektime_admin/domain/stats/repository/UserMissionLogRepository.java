package com.team.peektime_admin.domain.stats.repository;

import com.team.peektime_admin.domain.stats.dto.UserRankingProjection;
import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserMissionLogRepository extends JpaRepository<UserMissionLog, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT u.userUuid as userUuid, COUNT(u) as completionCount
        FROM UserMissionLog u
        WHERE u.solarTermId = :solarTermId
        GROUP BY u.userUuid
        ORDER BY COUNT(u) DESC
        """)
    List<UserRankingProjection> findRankingBySolarTerm(@Param("solarTermId") Long solarTermId);

    @Query("""
        SELECT u.userUuid as userUuid, COUNT(u) as completionCount
        FROM UserMissionLog u
        GROUP BY u.userUuid
        ORDER BY COUNT(u) DESC
        """)
    List<UserRankingProjection> findOverallRanking();
}