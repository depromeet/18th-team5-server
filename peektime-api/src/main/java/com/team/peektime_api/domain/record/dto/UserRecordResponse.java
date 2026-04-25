package com.team.peektime_api.domain.record.dto;

import com.team.peektime_api.domain.record.entity.UserRecord;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class UserRecordResponse {

    private final Long id;
    private final Long userId;
    private final Long solarTermId;
    private final LocalDate recordDate;
    private final String imageUrl;
    private final String memo;
    private final LocalDateTime createdAt;

    public UserRecordResponse(UserRecord record) {
        this.id = record.getId();
        this.userId = record.getUser().getId();
        this.solarTermId = record.getSolarTermId();
        this.recordDate = record.getRecordDate();
        this.imageUrl = record.getImageUrl();
        this.memo = record.getMemo();
        this.createdAt = record.getCreatedAt();
    }
}
