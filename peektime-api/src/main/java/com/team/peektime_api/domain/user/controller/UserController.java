package com.team.peektime_api.domain.user.controller;

import com.team.peektime_api.domain.user.dto.UserOnboardingRequest;
import com.team.peektime_api.domain.user.dto.UserOnboardingResponse;
import com.team.peektime_api.domain.user.dto.UserProfileResponse;
import com.team.peektime_api.domain.user.service.UserService;
import com.team.peektime_api.domain.user.service.UserWithdrawalService;
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
    private final UserWithdrawalService userWithdrawalService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보와 온보딩 완료 여부를 조회합니다.")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return SuccessResponse.of(SuccessCode.USER_FOUND, userService.getMyProfile(principal.getUserId()));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 유저의 UUID에 새 유저를 부여합니다. 기존 데이터는 30일 후 완전 삭제됩니다.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<Void> withdraw(
            @AuthenticationPrincipal UserPrincipal principal) {
        userWithdrawalService.withdraw(principal.getUserId());
        return SuccessResponse.of(SuccessCode.USER_WITHDRAWN, null);
    }

    @Operation(summary = "온보딩", description = "Q1(spaceType), Q2(activityStyleType), Q3(enjoyType 순위)를 받아 userType을 계산하고 저장합니다.")
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<UserOnboardingResponse> onboarding(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid UserOnboardingRequest request) {
        return SuccessResponse.of(SuccessCode.ONBOARDING_SAVED,
                userService.saveOnboarding(principal.getUserId(), request));
    }
}
