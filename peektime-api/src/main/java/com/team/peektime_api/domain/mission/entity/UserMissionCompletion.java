package com.team.peektime_api.domain.mission.entity;

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
@Table(name = "user_mission_completion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_completion", columnNames = {"user_id", "mission_id", "completed_at"})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false)
    private MissionType missionType;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Builder
    public UserMissionCompletion(User user, Long missionId, MissionType missionType,
                                  String imageUrl, String memo, LocalDateTime completedAt) {
        this.user = user;
        this.missionId = missionId;
        this.missionType = missionType;
        this.imageUrl = imageUrl;
        this.memo = memo;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
    }
}
