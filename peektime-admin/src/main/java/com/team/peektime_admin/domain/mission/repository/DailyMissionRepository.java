package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {

    long countBySolarTermId(Long solarTermId);

    long countBySolarTermIdAndMissionDateIsNotNull(Long solarTermId);

    List<DailyMission> findBySolarTermIdAndMissionDateIsNull(Long solarTermId);

    List<DailyMission> findBySolarTermIdAndMissionDateIsNotNull(Long solarTermId);
}
