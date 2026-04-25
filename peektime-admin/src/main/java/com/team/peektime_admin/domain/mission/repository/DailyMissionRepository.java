package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {

    long countBySolarTermId(Long solarTermId);

    long countBySolarTermIdAndMissionDateIsNotNull(Long solarTermId);
}
