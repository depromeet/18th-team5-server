package com.team.peektime_api.domain.auth.service;

import com.team.peektime_api.domain.auth.dto.AuthRequest;
import com.team.peektime_api.domain.auth.dto.AuthResponse;
import com.team.peektime_api.domain.auth.dto.TokenRefreshRequest;
import com.team.peektime_api.domain.auth.dto.TokenRefreshResponse;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.auth.jwt.JwtProvider;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        boolean[] isNewUser = {false};

        User user = userRepository.findByDeviceUuid(request.deviceUuid())
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    return userRepository.save(User.builder().deviceUuid(request.deviceUuid()).build());
                });

        String accessToken = jwtProvider.generateAccessToken(user.getId(), request.deviceUuid());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), request.deviceUuid());

        saveRefreshToken(user.getId(), request.deviceUuid(), refreshToken);

        return AuthResponse.of(accessToken, refreshToken, isNewUser[0]);
    }

    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String token = request.refreshToken();

        if (jwtProvider.isExpired(token)) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
        if (!jwtProvider.isValid(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtProvider.getClaims(token);
        Long userId = claims.get("userId", Long.class);
        String deviceUuid = claims.get("deviceUuid", String.class);

        String stored = redisTemplate.opsForValue().get(redisKey(userId, deviceUuid));
        if (stored == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!stored.equals(token)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, deviceUuid);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, deviceUuid);

        saveRefreshToken(userId, deviceUuid, newRefreshToken);

        return TokenRefreshResponse.of(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId, String deviceUuid) {
        redisTemplate.delete(redisKey(userId, deviceUuid));
    }

    private void saveRefreshToken(Long userId, String deviceUuid, String refreshToken) {
        redisTemplate.opsForValue()
                .set(redisKey(userId, deviceUuid), refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    private String redisKey(Long userId, String deviceUuid) {
        return "RT:" + userId + ":" + deviceUuid;
    }
}
