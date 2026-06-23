package com.team.peektime_admin.domain.mission.dto;

import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMissionDto {

    private String title;
    private String description;
    private String spaceType;
    private String companionType;
    private String categoryType;
    private String enjoyType;
    private String userType;

    /**
     * enjoyType, userType은 LLM 응답을 신뢰하지 않고 요청 시점에 확정된 값으로 덮어쓴다.
     * (병렬 분할 생성에서 각 호출이 어떤 enjoyType/userType을 요청했는지 코드가 이미 알고 있으므로 태그 오류를 원천 차단)
     */
    public void overrideTags(UserType userType, EnjoyType enjoyType) {
        if (userType != null) {
            this.userType = userType.name();
        }
        if (enjoyType != null) {
            this.enjoyType = enjoyType.name();
        }
    }
}