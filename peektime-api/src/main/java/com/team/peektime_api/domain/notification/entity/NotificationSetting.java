package com.team.peektime_api.domain.notification.entity;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_setting_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean dailyMission = true;

    @Column(nullable = false)
    private Boolean solarTermEnd = true;

    @Column(nullable = false)
    private Boolean solarTermStart = true;

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationSetting(User user, Boolean dailyMission, Boolean solarTermEnd, Boolean solarTermStart) {
        this.user = user;
        this.dailyMission = dailyMission;
        this.solarTermEnd = solarTermEnd;
        this.solarTermStart = solarTermStart;
    }

    public static NotificationSetting createDefault(User user) {
        return NotificationSetting.builder()
                .user(user)
                .dailyMission(true)
                .solarTermEnd(true)
                .solarTermStart(true)
                .build();
    }

    public void update(Boolean dailyMission, Boolean solarTermEnd, Boolean solarTermStart) {
        if (dailyMission != null) {
            this.dailyMission = dailyMission;
        }
        if (solarTermEnd != null) {
            this.solarTermEnd = solarTermEnd;
        }
        if (solarTermStart != null) {
            this.solarTermStart = solarTermStart;
        }
    }
}
