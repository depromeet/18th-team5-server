package com.team.peektime_api.domain.calendar.repository;

import com.team.peektime_api.domain.calendar.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {

    List<UserRecord> findByUser_IdAndRecordDate(Long userId, LocalDate recordDate);

    List<UserRecord> findByUser_IdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    long countByUser_IdAndRecordDate(Long userId, LocalDate recordDate);

    void deleteByUser_Id(Long userId);

    @Query("SELECT COUNT(r) FROM UserRecord r " +
            "WHERE r.user.id = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM UserRecord r " +
            "WHERE r.user.id = :userId " +
            "AND r.createdAt BETWEEN :startDate AND :endDate " +
            "AND r.objectKey IS NOT NULL " +
            "ORDER BY r.createdAt DESC LIMIT 3")
    List<UserRecord> findRecentRecordsWithImageByPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
