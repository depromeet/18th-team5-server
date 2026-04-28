package com.team.peektime_admin.domain.mission.prompt;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.enums.UserType;

public class MissionPromptTemplate {

    private static final String SYSTEM_PROMPT = """
            당신은 계절과 절기에 맞는 일상 미션을 생성하는 전문가입니다.
            사용자가 일상에서 쉽게 실천할 수 있는 미션을 생성해주세요.
            """;

    private static final String ATTRIBUTE_DESCRIPTION = """

            ## 미션 속성 체계

            ### 1. 공간 (spaceType)
            - INDOOR: 실내에서 수행하는 미션
            - OUTDOOR: 실외에서 수행하는 미션

            ### 2. 강도 (intensityType)
            - LIGHT: 이동 없음, 5분 이내로 할 수 있는 가벼운 미션
            - MODERATE: 동네 범위, 30분 이내로 할 수 있는 보통 미션
            - ACTIVE: 이동 필요, 1시간 이상 소요되는 적극적인 미션

            ### 3. 동반 (companionType)
            - SOLO: 혼자서 할 수 있는 미션
            - TOGETHER: 다른 사람과 함께하면 더 좋은 미션

            ### 4. 카테고리 (categoryType)
            - FOOD: 음식 관련 미션
            - NATURE: 자연 관련 미션
            - RECORD: 기록 관련 미션
            - PLACE: 장소 관련 미션
            - SENSE: 감각 관련 미션

            ### 5. 즐기는 방식 (enjoyType)
            - NATURE_OUTDOOR: 자연이나 야외에서 즐기는 활동
            - SEASONAL_FOOD: 제철 음식을 맛보거나 요리하는 활동
            - CULTURE_CONTENT: 감성적인 콘텐츠나 문화 활동

            ### 6. 추천 사용자 타입 (userType)
            - NATURE_EXPLORER: 밖에서 적극적으로 활동하는 자연 탐험가
            - NEIGHBORHOOD_WALKER: 밖에서 가볍게 활동하는 동네 산책러
            - SEASONAL_GOURMET: 실내에서 적극적으로 활동하는 제철 미식가
            - DAILY_OBSERVER: 실내에서 가볍게 활동하는 일상 관찰자
            """;

    private static final String OUTPUT_FORMAT = """

            ## 출력 형식
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

            {
              "missions": [
                {
                  "title": "미션 제목 (20자 이내)",
                  "description": "미션 설명 (50자 이내)",
                  "spaceType": "INDOOR 또는 OUTDOOR",
                  "intensityType": "LIGHT, MODERATE, ACTIVE 중 하나",
                  "companionType": "SOLO 또는 TOGETHER",
                  "categoryType": "FOOD, NATURE, RECORD, PLACE, SENSE 중 하나",
                  "enjoyType": "NATURE_OUTDOOR, SEASONAL_FOOD, CULTURE_CONTENT 중 하나",
                  "userType": "NATURE_EXPLORER, NEIGHBORHOOD_WALKER, SEASONAL_GOURMET, DAILY_OBSERVER 중 하나"
                }
              ]
            }
            """;

    public static String generate(int count) {
        return SYSTEM_PROMPT +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                count + "개의 미션을 생성해주세요.";
    }

    public static String generateWithTheme(String theme, int count) {
        return SYSTEM_PROMPT +
                "\n## 테마: " + theme + "\n" +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + theme + "' 테마에 맞는 " + count + "개의 미션을 생성해주세요.";
    }

    public static String generateWithSolarTerm(SolarTerm solarTerm, int count) {
        String solarTermInfo = buildSolarTermInfo(solarTerm);
        return SYSTEM_PROMPT +
                solarTermInfo +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + solarTerm.getName() + "' 절기에 맞는 " + count + "개의 미션을 생성해주세요.";
    }

    public static String generateWithSolarTermAndUserType(SolarTerm solarTerm, UserType userType, int count) {
        String solarTermInfo = buildSolarTermInfo(solarTerm);
        String userTypeInfo = buildUserTypeInfo(userType);
        return SYSTEM_PROMPT +
                solarTermInfo +
                userTypeInfo +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + solarTerm.getName() + "' 절기에 맞고, '" + userType.getLabel() + "' 타입의 사용자를 위한 " + count + "개의 미션을 생성해주세요.\n" +
                "생성되는 모든 미션의 userType은 반드시 '" + userType.name() + "'이어야 합니다.";
    }

    private static String buildUserTypeInfo(UserType userType) {
        return "\n## 대상 사용자 타입\n" +
                "- 타입: " + userType.getLabel() + "\n" +
                "- 설명: " + userType.getDescription() + "\n" +
                "\n이 사용자 타입에 맞는 미션을 생성해주세요.\n";
    }

    private static String buildSolarTermInfo(SolarTerm solarTerm) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 현재 절기 정보\n");
        sb.append("- 절기 이름: ").append(solarTerm.getName()).append("\n");

        if (solarTerm.getDescription() != null && !solarTerm.getDescription().isBlank()) {
            sb.append("- 설명: ").append(solarTerm.getDescription()).append("\n");
        }

        sb.append("\n위 절기 정보를 참고하여 해당 시기에 어울리는 미션을 생성해주세요.\n");
        return sb.toString();
    }
}