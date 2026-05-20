package com.team.peektime_admin.domain.stats.repository;

import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMissionLogRepository extends JpaRepository<UserMissionLog, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}