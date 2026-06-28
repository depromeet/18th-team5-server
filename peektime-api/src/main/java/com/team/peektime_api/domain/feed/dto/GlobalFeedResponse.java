package com.team.peektime_api.domain.feed.dto;

import java.util.List;

public record GlobalFeedResponse(
        int count,
        List<FeedItemResponse> items
) {
    public static GlobalFeedResponse of(List<FeedItemResponse> items) {
        return new GlobalFeedResponse(items.size(), items);
    }
}