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

import java.time.LocalDateTime;

@Entity
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

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Builder
    public UserMissionCompletion(User user, Long missionId, MissionType missionType,
                                  String objectKey, String memo, LocalDateTime completedAt) {
        this.user = user;
        this.missionId = missionId;
        this.missionType = missionType;
        this.objectKey = objectKey;
        this.memo = memo;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
    }

    public static UserMissionCompletion of(User user, Long missionId, UserMissionCompletionRequest request) {
        return UserMissionCompletion.builder()
                .user(user)
                .missionId(missionId)
                .missionType(request.missionType())
                .objectKey(request.objectKey())
                .memo(request.memo())
                .completedAt(request.completedAt() != null
                        ? request.completedAt().toLocalDateTime()
                        : LocalDateTime.now())
                .build();
    }
}
