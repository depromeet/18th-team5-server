package com.team.peektime_api.domain.user.controller;

import com.team.peektime_api.domain.user.dto.UserOnboardingRequest;
import com.team.peektime_api.domain.user.dto.UserOnboardingResponse;
import com.team.peektime_api.domain.user.service.UserService;
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

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "온보딩", description = "Q1(활동 공간), Q2(활동 방식), Q3(카테고리 순위)를 받아 userType과 카테고리 선호도를 저장합니다.")
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<UserOnboardingResponse> onboarding(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid UserOnboardingRequest request) {
        return SuccessResponse.of(SuccessCode.ONBOARDING_SAVED,
                userService.saveOnboarding(principal.getUserId(), request));
    }
}
