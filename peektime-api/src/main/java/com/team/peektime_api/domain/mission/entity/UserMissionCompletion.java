package com.team.peektime_api.domain.mission.entity;

import com.team.peektime_api.domain.record.entity.UserRecord;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.MissionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_mission_completion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_completion", columnNames = {"user_id", "mission_id", "solar_term_id"})
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

    @Column(name = "solar_term_id", nullable = false)
    private Long solarTermId;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private UserRecord record;

    @Builder
    public UserMissionCompletion(User user, Long missionId, MissionType missionType,
                                  Long solarTermId, LocalDate completedDate, UserRecord record) {
        this.user = user;
        this.missionId = missionId;
        this.missionType = missionType;
        this.solarTermId = solarTermId;
        this.completedDate = completedDate;
        this.record = record;
    }

    public void linkRecord(UserRecord record) {
        this.record = record;
    }
}
