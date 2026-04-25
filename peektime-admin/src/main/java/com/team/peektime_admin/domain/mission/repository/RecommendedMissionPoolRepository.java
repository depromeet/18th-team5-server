package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendedMissionPoolRepository extends JpaRepository<RecommendedMissionPool, Long> {

    long countBySolarTermId(Long solarTermId);
}