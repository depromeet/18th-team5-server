package com.team.peektime_api.domain.stats.dto;

import java.time.LocalDate;

public interface DailyCompletionStatsProjection {

    LocalDate getStatDate();

    Long getCompletionCount();

    Long getPhotoCount();
}
