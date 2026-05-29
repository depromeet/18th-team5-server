package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.team.peektime_api.global.common.enums.MissionType;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    boolean existsByUser_IdAndMission_Id(Long userId, Long missionId);

    long countByUser_IdAndMissionType(Long userId, MissionType missionType);

    @Query("SELECT COUNT(c) FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.missionType = :missionType " +
            "AND c.createdAt BETWEEN :startOfDay AND :endOfDay")
    long countTodayByUserIdAndMissionType(
            @Param("userId") Long userId,
            @Param("missionType") MissionType missionType,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    List<UserMissionCompletion> findByUser_IdAndMission_Id(Long userId, Long missionId);

    long countByMission_Id(Long missionId);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.objectKey IS NOT NULL " +
            "ORDER BY c.createdAt DESC LIMIT 3")
    List<UserMissionCompletion> findRecentRecordsWithImage(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate " +
            "AND c.objectKey IS NOT NULL " +
            "ORDER BY c.createdAt DESC LIMIT 3")
    List<UserMissionCompletion> findRecentRecordsWithImageByPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM UserMissionCompletion c " +
            "WHERE c.user.id = :userId AND c.createdAt BETWEEN :start AND :end " +
            "ORDER BY c.createdAt ASC")
    List<UserMissionCompletion> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
