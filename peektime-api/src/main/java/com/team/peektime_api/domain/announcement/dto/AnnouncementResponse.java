package com.team.peektime_api.domain.announcement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team.peektime_api.domain.announcement.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "공지사항 응답")
@Getter
@Builder
public class AnnouncementResponse {

    @Schema(description = "공지사항 ID")
    private Long id;

    @Schema(description = "공지사항 제목")
    private String title;

    @Schema(description = "공지사항 본문")
    private String content;

    @Schema(description = "생성일시", example = "2026-06-10T13:14:47")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AnnouncementResponse from(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}