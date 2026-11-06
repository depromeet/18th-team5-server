package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "user_mission_completion", indexes = {
        @Index(name = "idx_completion_user_term", columnList = "user_id, solar_term_id"),
        @Index(name = "idx_completion_user_term_date", columnList = "user_id, solar_term_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMissionCompletion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_term_id", nullable = false)
    private SolarTerm solarTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "memo", length = 200)
    private String memo;

    // 전역 피드 좋아요 수 — 비정규화 카운터. 진실원본은 feed_like 테이블이고 이 컬럼은
    // 피드 스냅샷이 완료 행을 읽을 때 개수를 공짜로 딸려오게 하는 읽기 캐시다.
    // 토글 시 FeedLikeService가 원자 UPDATE(=±1)로만 갱신하며 엔티티 세터로 바꾸지 않는다
    // (핫 완료행의 read-modify-write 유실 방지). 크래시로 생기는 드문 off-by-1은
    // COUNT(*) 재집계로 복구 가능하다. @ColumnDefault는 ddl-auto update로 컬럼이 추가될 때
    // 기존 행이 0으로 채워지게 한다
    @ColumnDefault("0")
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionCompletion(User user, Mission mission, SolarTerm solarTerm,
                                  MissionType missionType, String objectKey, String memo) {
        this.user = user;
        this.mission = mission;
        this.solarTerm = solarTerm;
        this.missionType = missionType;
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public void update(String objectKey, String memo) {
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public static UserMissionCompletion create(User user, Mission mission, SolarTerm solarTerm,
                                               MissionType missionType, String objectKey, String memo) {
        return UserMissionCompletion.builder()
                .user(user)
                .mission(mission)
                .solarTerm(solarTerm)
                .missionType(missionType)
                .objectKey(objectKey)
                .memo(memo)
                .build();
    }
}
