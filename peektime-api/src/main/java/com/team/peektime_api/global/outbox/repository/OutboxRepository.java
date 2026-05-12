package com.team.peektime_api.global.outbox.repository;

import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByCreatedAtBefore(LocalDateTime dateTime);
}