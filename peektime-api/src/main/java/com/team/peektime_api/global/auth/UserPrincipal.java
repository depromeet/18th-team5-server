package com.team.peektime_api.global.auth;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserPrincipal {

    private final Long userId;
    private final String deviceUuid;

    public static UserPrincipal from(Claims claims) {
        Long userId = claims.get("userId", Long.class);
        String deviceUuid = claims.get("deviceUuid", String.class);
        return new UserPrincipal(userId, deviceUuid);
    }
}
