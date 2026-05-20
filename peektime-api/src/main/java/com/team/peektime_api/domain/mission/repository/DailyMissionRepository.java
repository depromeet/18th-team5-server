package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.DailyMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {

    @Modifying
    @Query("UPDATE DailyMission d SET d.participantCount = d.participantCount + 1 WHERE d.id = :id")
    void incrementParticipantCount(@Param("id") Long id);

    List<DailyMission> findBySolarTermId(Long solarTermId);

    Optional<DailyMission> findBySolarTermIdAndMissionDate(Long solarTermId, LocalDate missionDate);

    Optional<DailyMission> findByMission_IdAndMissionDate(Long missionId, LocalDate missionDate);

    @Query("SELECT dm FROM DailyMission dm " +
           "JOIN FETCH dm.mission " +
           "JOIN FETCH dm.solarTerm " +
           "WHERE dm.missionDate = :date")
    Optional<DailyMission> findByMissionDateWithDetails(@Param("date") LocalDate date);

    @Query("SELECT dm FROM DailyMission dm WHERE dm.solarTerm.id = :solarTermId AND dm.missionDate IS NOT NULL")
    List<DailyMission> findAssignedBySolarTermId(@Param("solarTermId") Long solarTermId);

    void deleteBySolarTermId(Long solarTermId);
}