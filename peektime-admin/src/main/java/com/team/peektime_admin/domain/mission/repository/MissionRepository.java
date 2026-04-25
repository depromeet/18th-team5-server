package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    long countByDeletedFalse();
}