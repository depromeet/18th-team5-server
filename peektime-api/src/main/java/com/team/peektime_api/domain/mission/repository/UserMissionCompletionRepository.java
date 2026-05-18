package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    boolean existsByUser_IdAndMissionId(Long userId, Long missionId);

    List<UserMissionCompletion> findByUser_IdAndMissionId(Long userId, Long missionId);

    long countByMissionId(Long missionId);

    List<UserMissionCompletion> findTop3ByUser_IdAndObjectKeyIsNotNullOrderByCompletedAtDesc(Long userId);

    long countByUser_IdAndCompletedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    List<UserMissionCompletion> findTop3ByUser_IdAndCompletedAtBetweenAndObjectKeyIsNotNullOrderByCompletedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
