package com.team.peektime_api.domain.stats.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일별 미션 수행 통계의 확정분. 새벽 배치가 원본(user_mission_completion)을 날짜별로
// 재집계해 쓰고, 한 번 확정된 날짜는 다시 덮어쓰지 않는다(동결) — 이후 원본이 탈퇴 등으로
// 삭제돼도 통계는 소급 변경되지 않는다
@Entity
@Table(name = "daily_completion_stats", uniqueConstraints = {
        // 날짜당 1행 보장. 다중 인스턴스가 같은 날짜를 동시에 확정하려는 레이스는
        // 이 제약의 duplicate key 실패로 끝난다 — 멱등한 재집계라 실패한 쪽은 무시하면 된다
        @UniqueConstraint(name = "uk_daily_completion_stats_date", columnNames = {"stat_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCompletionStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "completion_count", nullable = false)
    private Long completionCount;

    @Column(name = "photo_count", nullable = false)
    private Long photoCount;

    @Builder(access = AccessLevel.PRIVATE)
    private DailyCompletionStats(LocalDate statDate, Long completionCount, Long photoCount) {
        this.statDate = statDate;
        this.completionCount = completionCount;
        this.photoCount = photoCount;
    }

    public static DailyCompletionStats create(LocalDate statDate, Long completionCount, Long photoCount) {
        return DailyCompletionStats.builder()
                .statDate(statDate)
                .completionCount(completionCount)
                .photoCount(photoCount)
                .build();
    }
}
