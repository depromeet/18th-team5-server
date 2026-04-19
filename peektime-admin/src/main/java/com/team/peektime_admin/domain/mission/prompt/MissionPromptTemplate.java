package com.team.peektime_admin.domain.mission.prompt;

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
            - LIGHT: 가볍게 할 수 있는 미션
            - ACTIVE: 적극적으로 움직여야 하는 미션

            ### 3. 동반 (companionType)
            - SOLO: 혼자서 할 수 있는 미션
            - TOGETHER: 다른 사람과 함께하면 더 좋은 미션

            ### 4. 카테고리 (categoryType)
            - FOOD: 음식 관련 미션
            - NATURE: 자연 관련 미션
            - RECORD: 기록 관련 미션
            - PLACE: 장소 관련 미션
            - SENSE: 감각 관련 미션
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
                  "intensityType": "LIGHT 또는 ACTIVE",
                  "companionType": "SOLO 또는 TOGETHER",
                  "categoryType": "FOOD, NATURE, RECORD, PLACE, SENSE 중 하나"
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
}