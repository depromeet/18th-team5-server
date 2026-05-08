package com.team.peektime_api.domain.user.controller;

import com.team.peektime_api.domain.user.dto.UserProfileResponse;
import com.team.peektime_api.domain.user.service.UserService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 정보")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보와 온보딩 완료 여부를 조회합니다.")
    @GetMapping("/me")
    public SuccessResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.USER_FOUND,
                userService.getMyProfile(principal.getUserId()));
    }
}
