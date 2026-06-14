package com.team.peektime_api.domain.home.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SeasonalRecordsResponse(
        long recordCount,
        List<RecentRecord> recentRecords
) {
    public record RecentRecord(
            Long completionId,
            String imageUrl,
            LocalDateTime recordedAt
    ) {
        public static RecentRecord of(Long completionId, String presignedUrl, LocalDateTime recordedAt) {
            return new RecentRecord(completionId, presignedUrl, recordedAt);
        }
    }

    public static SeasonalRecordsResponse of(long recordCount, List<RecentRecord> recentRecords) {
        return new SeasonalRecordsResponse(recordCount, recentRecords);
    }
}