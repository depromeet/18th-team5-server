package com.team.peektime_admin.domain.mission.repository;

import com.team.peektime_admin.domain.mission.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    long countByDeletedFalse();

    Page<Mission> findAllByDeletedFalse(Pageable pageable);

    List<Mission> findAllByDeletedFalse();
}
