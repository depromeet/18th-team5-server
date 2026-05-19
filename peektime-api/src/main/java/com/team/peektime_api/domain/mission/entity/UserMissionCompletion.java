package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "memo", length = 200)
    private String memo;

    @Builder(access = AccessLevel.PRIVATE)
    private UserMissionCompletion(User user, Long missionId, Long solarTermId,
                                  MissionType missionType, String objectKey, String memo) {
        this.user = user;
        this.missionId = missionId;
        this.solarTermId = solarTermId;
        this.missionType = missionType;
        this.objectKey = objectKey;
        this.memo = memo;
    }

    public static UserMissionCompletion of(User user, Long missionId, UserMissionCompletionRequest request) {
        return UserMissionCompletion.builder()
                .user(user)
                .missionId(missionId)
                .solarTermId(request.solarTermId())
                .missionType(request.missionType())
                .objectKey(request.objectKey())
                .memo(request.memo())
                .build();
    }
}
