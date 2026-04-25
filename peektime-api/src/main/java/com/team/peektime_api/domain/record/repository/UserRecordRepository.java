package com.team.peektime_api.domain.record.repository;

import com.team.peektime_api.domain.record.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {
}
