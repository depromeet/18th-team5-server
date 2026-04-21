package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import jakarta.persistence.*;
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

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "push_token", length = 255)
    private String pushToken;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserOnboarding onboarding;

    @Builder
    public User(String nickname, String email, String pushToken) {
        this.nickname = nickname;
        this.email = email;
        this.pushToken = pushToken;
    }

    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
