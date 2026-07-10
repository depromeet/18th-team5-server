package com.team.peektime_api.domain.mission.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LLM(Gemini) JSON 응답 파싱용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class GeneratedSelectedMissionDto {

    private String title;
    private String description;
    private String spaceType;
    private String companionType;
    private String categoryType;
}