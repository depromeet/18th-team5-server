package com.team.peektime_api.global.infra.cloudfront;

import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.infra.cloudfront.dto.SignedCookieResponse;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Media", description = "미디어 접근 인증")
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaAuthController {

    private final CloudFrontCookieService cookieService;

    @Operation(
            summary = "CloudFront Signed Cookie 발급",
            description = "인증된 사용자에게 이미지 조회용 CloudFront Signed Cookie를 발급합니다. (유효시간 7일)"
    )
    @PostMapping("/auth")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<SignedCookieResponse> issueMediaAuth(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SignedCookieResponse response = cookieService.issueCookies();
        return SuccessResponse.of(SuccessCode.MEDIA_AUTH_ISSUED, response);
    }
}
