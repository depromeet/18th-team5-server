package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uuid", nullable = false, unique = true, length = 36)
    private String deviceUuid;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "push_token", length = 255)
    private String pushToken;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserOnboarding onboarding;

    @Builder
    public User(String deviceUuid, String nickname, String pushToken) {
        this.deviceUuid = deviceUuid;
        this.nickname = nickname;
        this.pushToken = pushToken;
    }

    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw(String anonymizedUuid) {
        this.deviceUuid = anonymizedUuid;
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isWithdrawn() {
        return this.withdrawnAt != null;
    }
}
