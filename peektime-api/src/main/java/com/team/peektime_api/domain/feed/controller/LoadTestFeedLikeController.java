package com.team.peektime_api.domain.feed.controller;

import com.team.peektime_api.domain.feed.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 전용 좋아요 엔드포인트 — {@code loadtest} 프로필에서만 빈으로 등록된다(운영엔 존재 X).
 *
 * 목적: 단일 행 좋아요 카운터({@code user_mission_completion.like_count})의 행 락 경합을
 * ~1000 TPS에서 관측하기 위함. 운영 {@link GlobalFeedController}와 달리 두 가지만 다르다:
 *   1) userId를 JWT principal이 아니라 요청 파라미터로 받는다 — 부하 생성기가 "서로 다른" 유저를
 *      뿌려야 유니크 제약(user_id, completion_id)을 통과해 실제로 카운터 UPDATE가 일어난다.
 *      (같은 유저로 때리면 첫 요청만 INSERT되고 나머지는 멱등 무변화 → 카운터 경합이 안 생김)
 *   2) 인증을 요구하지 않는다 (SecurityConfig에서 /loadtest/** permitAll).
 *
 * 비즈니스 경로는 운영과 완전히 동일하다 — 같은 {@link FeedLikeService#like}를 호출하므로
 * 같은 멤버십 INSERT + 같은 핫-행 카운터 UPDATE를 탄다.
 */
@Profile("loadtest")
@RestController
@RequestMapping("/loadtest/feed")
@RequiredArgsConstructor
public class LoadTestFeedLikeController {

    private final FeedLikeService feedLikeService;

    @PostMapping("/{feedItemId}/like")
    public void like(@PathVariable Long feedItemId, @RequestParam Long userId) {
        feedLikeService.like(userId, feedItemId);
    }

    @PostMapping("/{feedItemId}/unlike")
    public void unlike(@PathVariable Long feedItemId, @RequestParam Long userId) {
        feedLikeService.unlike(userId, feedItemId);
    }
}
