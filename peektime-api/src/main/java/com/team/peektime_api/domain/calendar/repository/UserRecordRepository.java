package com.team.peektime_api.domain.calendar.repository;

import com.team.peektime_api.domain.calendar.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {

    List<UserRecord> findByUser_IdAndRecordDate(Long userId, LocalDate recordDate);

    List<UserRecord> findByUser_IdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    long countByUser_IdAndRecordDate(Long userId, LocalDate recordDate);

    void deleteByUser_Id(Long userId);
}
