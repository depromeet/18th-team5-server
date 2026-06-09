package com.team.peektime_api.domain.announcement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "공지사항 생성 요청")
@Getter
@Setter
public class AnnouncementRequest {

    @Schema(description = "공지사항 제목 (최대 40자)", example = "서비스 점검 안내")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 40, message = "제목은 40자 이내로 입력해주세요")
    private String title;

    @Schema(description = "공지사항 본문", example = "서비스 점검이 예정되어 있습니다.")
    @NotBlank(message = "본문은 필수입니다")
    private String content;
}