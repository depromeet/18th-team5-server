package com.team.peektime_api.domain.mission.repository;

import com.team.peektime_api.domain.mission.entity.UserSelectedMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserSelectedMissionRepository extends JpaRepository<UserSelectedMission, Long> {

    Optional<UserSelectedMission> findByUserIdAndSelectedDate(Long userId, LocalDate selectedDate);

    Optional<UserSelectedMission> findByUserIdAndSolarTermIdAndSelectedDate(Long userId, Long solarTermId, LocalDate selectedDate);

    boolean existsByUserIdAndSelectedDate(Long userId, LocalDate selectedDate);
}