package com.team.peektime_api.global.schedulerlock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ShedLock 락 테이블의 스키마 선언 전용 엔티티 — 애플리케이션 코드가 직접 읽고 쓰지 않는다
// (JdbcTemplateLockProvider가 SQL compare-and-set으로만 접근). API 모듈은 Flyway 없이
// ddl-auto로 스키마를 만들므로, 엔티티로 선언해 테이블 생성을 보장한다.
// 컬럼명/타입은 ShedLock JDBC provider가 기대하는 표준 스키마와 일치해야 한다
@Entity
@Table(name = "shedlock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shedlock {

    // 작업 이름 (@SchedulerLock의 name) — 작업당 1행
    @Id
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    // 이 시각까지 임대 유효. 획득 = "lock_until이 과거일 때만 미래로 미는" 원자적 UPDATE
    @Column(name = "lock_until", nullable = false)
    private LocalDateTime lockUntil;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;
}