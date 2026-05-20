package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.global.common.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendedMissionPoolRepository extends JpaRepository<RecommendedMissionPool, Long> {

    List<RecommendedMissionPool> findBySolarTermId(Long solarTermId);

    long countBySolarTermId(Long solarTermId);

    long countBySolarTermIdAndUserType(Long solarTermId, UserType userType);

    List<RecommendedMissionPool> findBySolarTermIdAndUserType(Long solarTermId, UserType userType);

    boolean existsByMissionId(Long missionId);

    List<RecommendedMissionPool> findByMissionIdIn(List<Long> missionIds);
}
