package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long>, MissionRepositoryCustom {

    List<Mission> findAllByDeletedFalse();

    List<Mission> findAllByIdIn(List<Long> ids);

    @Query("SELECT MAX(m.id) FROM Mission m WHERE m.id >= :offset")
    Long findMaxIdFrom(@Param("offset") long offset);
}