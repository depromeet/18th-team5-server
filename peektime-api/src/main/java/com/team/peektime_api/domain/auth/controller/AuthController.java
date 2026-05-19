package com.team.peektime_api.domain.auth.controller;

import com.team.peektime_api.domain.auth.dto.AuthRequest;
import com.team.peektime_api.domain.auth.dto.AuthResponse;
import com.team.peektime_api.domain.auth.dto.TokenRefreshRequest;
import com.team.peektime_api.domain.auth.dto.TokenRefreshResponse;
import com.team.peektime_api.domain.auth.service.AuthService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "디바이스 UUID 기반 인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인/자동가입", description = "iOS 디바이스 UUID로 로그인합니다. 신규 UUID면 자동 가입 후 토큰 발급.")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return SuccessResponse.of(SuccessCode.LOGIN_SUCCESS, authService.login(request));
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<TokenRefreshResponse> refresh(@RequestBody @Valid TokenRefreshRequest request) {
        return SuccessResponse.of(SuccessCode.TOKEN_REFRESHED, authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "Redis에서 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUserId(), principal.getDeviceUuid());
    }
}
