package com.team.peektime_api.domain.calendar.dto;

import com.team.peektime_api.domain.calendar.entity.UserRecord;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record CalendarCardResponse(

        Long id,
        MissionType cardType,
        String missionTitle,
        String presignedImageUrl,
        String memo,
        LocalDateTime recordedAt
) {

    public static CalendarCardResponse fromMissionCompletion(UserMissionCompletion completion, String presignedImageUrl) {
        return new CalendarCardResponse(
                completion.getId(),
                completion.getMissionType(),
                completion.getMission().getTitle(),
                presignedImageUrl,
                completion.getMemo(),
                completion.getCreatedAt()
        );
    }

    public static CalendarCardResponse fromUserRecord(UserRecord record, String presignedImageUrl) {
        return new CalendarCardResponse(
                record.getId(),
                MissionType.FREE,
                null,
                presignedImageUrl,
                record.getMemo(),
                record.getCreatedAt()
        );
    }
}
