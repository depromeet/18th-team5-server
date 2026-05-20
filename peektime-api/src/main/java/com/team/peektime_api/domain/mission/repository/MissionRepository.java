package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findAllByDeletedFalse();

    List<Mission> findAllByIdIn(List<Long> ids);
}