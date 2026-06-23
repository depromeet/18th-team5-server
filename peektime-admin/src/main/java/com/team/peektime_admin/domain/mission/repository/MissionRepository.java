package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.global.common.enums.CategoryType;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.SpaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    long countByDeletedFalse();

    Page<Mission> findAllByDeletedFalse(Pageable pageable);

    List<Mission> findAllByDeletedFalse();

    @Query("SELECT m FROM Mission m WHERE m.deleted = false " +
            "AND (:spaceType IS NULL OR m.spaceType = :spaceType) " +
            "AND (:categoryType IS NULL OR m.categoryType = :categoryType) " +
            "AND (:enjoyType IS NULL OR m.enjoyType = :enjoyType)")
    Page<Mission> findAllWithFilters(
            @Param("spaceType") SpaceType spaceType,
            @Param("categoryType") CategoryType categoryType,
            @Param("enjoyType") EnjoyType enjoyType,
            Pageable pageable
    );
}
