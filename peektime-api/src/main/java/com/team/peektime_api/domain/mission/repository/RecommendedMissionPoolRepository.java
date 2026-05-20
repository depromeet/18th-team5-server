package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_api.global.common.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendedMissionPoolRepository extends JpaRepository<RecommendedMissionPool, Long> {

    List<RecommendedMissionPool> findBySolarTermId(Long solarTermId);

    List<RecommendedMissionPool> findBySolarTermIdAndUserType(Long solarTermId, UserType userType);

    List<RecommendedMissionPool> findBySolarTermIdAndUserTypeOrderByDisplayOrderAsc(Long solarTermId, UserType userType);

    void deleteBySolarTermId(Long solarTermId);
}