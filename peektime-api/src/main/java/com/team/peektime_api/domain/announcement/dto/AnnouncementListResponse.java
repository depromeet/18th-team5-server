package com.team.peektime_api.domain.announcement.dto;

import com.team.peektime_api.domain.announcement.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "공지사항 목록 응답")
@Getter
@Builder
public class AnnouncementListResponse {

    @Schema(description = "공지사항 ID")
    private Long id;

    @Schema(description = "공지사항 제목")
    private String title;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static AnnouncementListResponse from(Announcement announcement) {
        return AnnouncementListResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}