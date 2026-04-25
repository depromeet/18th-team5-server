package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_auth", indexes = {
        @Index(name = "idx_device_uuid", columnList = "device_uuid"),
        @Index(name = "idx_refresh_token", columnList = "refresh_token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceAuth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_uuid", nullable = false, length = 36)
    private String deviceUuid;

    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Builder
    public DeviceAuth(User user, String deviceUuid, String refreshToken, LocalDateTime expiresAt) {
        this.user = user;
        this.deviceUuid = deviceUuid;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revoked = true;
    }

    public void rotate(String newRefreshToken, LocalDateTime newExpiresAt) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
