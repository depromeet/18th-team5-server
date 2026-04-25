package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final String nickname;
    private final String email;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
    }
}
