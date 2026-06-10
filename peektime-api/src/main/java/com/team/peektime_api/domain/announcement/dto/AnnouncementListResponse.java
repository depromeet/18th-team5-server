package com.team.peektime_api.domain.announcement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.peektime_api.domain.announcement.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Schema(description = "공지사항 목록 응답")
@Getter
@Builder
public class AnnouncementListResponse {

    @Schema(description = "공지사항 ID")
    private Long id;

    @Schema(description = "공지사항 제목")
    private String title;

    @Schema(description = "생성일시", example = "2026-06-10T13:14:47+09:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    public static AnnouncementListResponse from(Announcement announcement) {
        return AnnouncementListResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .createdAt(announcement.getCreatedAt().atOffset(KST))
                .build();
    }
}