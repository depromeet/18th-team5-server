package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendedMissionPoolRepository extends JpaRepository<RecommendedMissionPool, Long> {

    List<RecommendedMissionPool> findBySolarTermId(Long solarTermId);

    long countBySolarTermId(Long solarTermId);

    long countBySolarTermIdAndUserType(Long solarTermId, UserType userType);

    List<RecommendedMissionPool> findBySolarTermIdAndUserType(Long solarTermId, UserType userType);

    boolean existsByMissionId(Long missionId);

    List<RecommendedMissionPool> findByMissionIdIn(List<Long> missionIds);

    @Query("SELECT COUNT(r) FROM RecommendedMissionPool r " +
            "WHERE r.solarTerm.id = :solarTermId " +
            "AND r.userType = :userType " +
            "AND r.mission.enjoyType = :enjoyType")
    long countBySolarTermIdAndUserTypeAndEnjoyType(
            @Param("solarTermId") Long solarTermId,
            @Param("userType") UserType userType,
            @Param("enjoyType") EnjoyType enjoyType
    );
}
