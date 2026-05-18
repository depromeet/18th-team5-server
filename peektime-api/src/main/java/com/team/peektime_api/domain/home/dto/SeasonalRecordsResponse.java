package com.team.peektime_api.domain.home.dto;

import java.util.List;

public record SeasonalRecordsResponse(
        long recordCount,
        List<RecentRecord> recentRecords
) {
    public record RecentRecord(
            Long completionId,
            String imageUrl
    ) {
        public static RecentRecord of(Long completionId, String presignedUrl) {
            return new RecentRecord(completionId, presignedUrl);
        }
    }

    public static SeasonalRecordsResponse of(long recordCount, List<RecentRecord> recentRecords) {
        return new SeasonalRecordsResponse(recordCount, recentRecords);
    }
}