package com.team.peektime_api.domain.feed.controller;

import com.team.peektime_api.domain.feed.dto.GlobalFeedResponse;
import com.team.peektime_api.domain.feed.service.FeedLikeService;
import com.team.peektime_api.domain.feed.service.GlobalFeedService;
import com.team.peektime_api.global.auth.UserPrincipal;
import com.team.peektime_api.global.response.SuccessCode;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전역 공유 피드 API.
 * 콘텐츠는 익명(사진+시각)이지만 조회·좋아요에는 로그인이 필요하다.
 */
@Tag(name = "GlobalFeed", description = "전역 공유 피드")
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class GlobalFeedController {

    private final GlobalFeedService globalFeedService;
    private final FeedLikeService feedLikeService;

    @Operation(summary = "전역 피드 최신 20건 조회",
            description = "사진 있는 미션 완료를 작성자 무관 최신순으로. 각 항목에 좋아요 수와 내가 눌렀는지(likedByMe) 포함.")
    @GetMapping("/recent")
    public SuccessResponse<GlobalFeedResponse> getRecentFeed(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return SuccessResponse.of(SuccessCode.FEED_FOUND,
                globalFeedService.getRecentFeed(principal.getUserId()));
    }

    @Operation(summary = "피드 좋아요", description = "좋아요를 보장한다(멱등). 이미 누른 상태면 아무 일도 없다.")
    @PostMapping("/{feedItemId}/like")
    public SuccessResponse<Void> like(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long feedItemId
    ) {
        feedLikeService.like(principal.getUserId(), feedItemId);
        return SuccessResponse.of(SuccessCode.FEED_LIKED, null);
    }

    @Operation(summary = "피드 좋아요 취소", description = "좋아요 취소를 보장한다(멱등). 안 누른 상태면 아무 일도 없다.")
    @DeleteMapping("/{feedItemId}/like")
    public SuccessResponse<Void> unlike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long feedItemId
    ) {
        feedLikeService.unlike(principal.getUserId(), feedItemId);
        return SuccessResponse.of(SuccessCode.FEED_UNLIKED, null);
    }
}