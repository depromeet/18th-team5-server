package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserProfileResponse {

    private final Long id;
    private final String deviceUuid;
    private final String nickname;
    private final LocalDateTime createdAt;

    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.deviceUuid = user.getDeviceUuid();
        this.nickname = user.getNickname();
        this.createdAt = user.getCreatedAt();
    }
}
