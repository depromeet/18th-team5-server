package com.team.peektime_api.domain.calendar.dto;

import com.team.peektime_api.domain.calendar.entity.UserRecord;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.MissionType;

import java.time.LocalDateTime;

public record CalendarCardResponse(

        Long id,
        MissionType cardType,
        Long missionId,
        String missionTitle,
        String missionDescription,
        CategoryType categoryType,
        String presignedImageUrl,
        String memo,
        LocalDateTime recordedAt
) {

    public static CalendarCardResponse fromMissionCompletion(UserMissionCompletion completion, String presignedImageUrl) {
        return new CalendarCardResponse(
                completion.getId(),
                completion.getMissionType(),
                completion.getMission().getId(),
                completion.getMission().getTitle(),
                completion.getMission().getDescription(),
                completion.getMission().getCategoryType(),
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
                null,
                null,
                null,
                presignedImageUrl,
                record.getMemo(),
                record.getCreatedAt()
        );
    }
}
