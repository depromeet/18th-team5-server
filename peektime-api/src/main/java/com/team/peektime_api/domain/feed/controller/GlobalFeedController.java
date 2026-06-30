package com.team.peektime_api.domain.feed.controller;

import com.team.peektime_api.domain.feed.dto.GlobalFeedResponse;
import com.team.peektime_api.domain.feed.service.GlobalFeedService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전역 공유 피드 API.
 * 콘텐츠는 익명(사진+시각)이지만 조회에는 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class GlobalFeedController {

    private final GlobalFeedService globalFeedService;

    @GetMapping("/recent")
    public SuccessResponse<GlobalFeedResponse> getRecentFeed(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.FEED_FOUND, globalFeedService.getRecentFeed());
    }
}