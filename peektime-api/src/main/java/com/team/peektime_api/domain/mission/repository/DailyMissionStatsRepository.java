package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.DailyMissionStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyMissionStatsRepository extends JpaRepository<DailyMissionStats, Long> {

    Optional<DailyMissionStats> findByMissionIdAndMissionDate(Long missionId, LocalDate missionDate);

    boolean existsByMissionIdAndMissionDate(Long missionId, LocalDate missionDate);
}