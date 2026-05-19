package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    boolean existsByUserIdAndMissionId(Long userId, Long missionId);

    List<UserMissionCompletion> findByUser_IdAndMissionId(Long userId, Long missionId);

    long countByMissionId(Long missionId);
}
