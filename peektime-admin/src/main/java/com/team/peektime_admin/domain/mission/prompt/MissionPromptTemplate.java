package com.team.peektime_admin.domain.mission.prompt;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;

public class MissionPromptTemplate {

    private static final String SYSTEM_PROMPT = """
            당신은 'peektime' 앱의 미션 기획자입니다.
            'peektime'은 사용자들이 계절(절기)의 변화를 느끼고 일상에서 소소한 미션을 수행하며 기록하는 서비스입니다.
            """;

    private static final String MISSION_PRINCIPLES = """

            ## 미션 선정 원칙

            ### 실행 가능성
            - 여행, 대규모 구매 등 하루 안에 준비하기 어려운 행동은 절대 금지한다.
            - 일상 동선 안에서 30분 이내에 끝낼 수 있어야 한다.

            ### 사진 기록 중심
            - 미션의 결과물은 스마트폰 사진 한 장으로 명확하게 증명 가능해야 한다.
            - 인스타그램 등 외부 SNS 포스팅을 유도하는 미션은 절대 금지한다.
            - 오직 앱 내부에서 사진을 찍어 기록하는 행동으로 한정한다.

            ### 추상도 조절
            - 특정 장소나 상황을 지나치게 한정하지 않는다.
            - 나쁜 예: "계곡물에 발 담그기" (장소가 너무 구체적)
            - 좋은 예: "시원한 물에 발 담가보기" (계절감은 살리되 장소를 한정하지 않음)
            - 계절감과 행동의 본질을 살리면서 누구나 일상에서 실행할 수 있는 수준으로 작성한다.

            ### 오감 활용
            - 전체 미션 중 2/3 이상은 시각, 청각, 촉각, 미각, 후각을 자극하는 행동 동사를 포함해야 한다.
            """;

    private static final String ATTRIBUTE_DESCRIPTION = """

            ## 미션 속성 체계

            ### 1. 공간 (spaceType)
            - INDOOR: 실내에서 수행하는 미션
            - OUTDOOR: 실외에서 수행하는 미션

            ### 2. 동반 (companionType)
            - SOLO: 혼자 가능 (혼자서도 충분히 즐길 수 있는 미션)
            - TOGETHER: 같이하면 더 좋음 (함께하면 더 재미있는 미션)

            ### 3. 카테고리 (categoryType)
            - FOOD: 제철 음식, 요리 관련 미션
            - NATURE: 자연 감상, 야외 활동 미션
            - CONTENT: 사진, 일기 등 기록/콘텐츠 미션
            - PLACE: 특정 장소 방문 미션
            - MUSIC: 음악 관련 미션

            ### 4. 즐기는 방식 (enjoyType)
            - NATURE_OUTDOOR (자연/야외 활동): 자연이나 야외에서 즐기는 활동
            - SEASONAL_FOOD (제철 음식/요리): 제철 음식을 맛보거나 요리하는 활동
            - CULTURE_CONTENT (감성 콘텐츠/문화): 감성적인 콘텐츠나 문화 활동

            ### 5. 추천 사용자 타입 (userType)
            - EXPLORER (제철을 쫓는 탐험가)
              성향: 적극적으로 계절의 변화를 찾아 나서는 타입
              미션 방향: 외부 공간 이동, 적극적인 탐색, 시각적 발견 중심
            - WALKER (일상 속 제철 산책가)
              성향: 출퇴근길 등 일상 동선 안에서 계절을 가볍게 즐기는 타입
              미션 방향: 걷기, 일상 동선에서의 소소한 주변 관찰 중심
            - LIFE_CREATOR (제철을 채우는 라이프 크리에이터)
              성향: 집, 방, 개인 공간 안에서 제철 요소를 연출하는 것을 좋아하는 타입
              미션 방향: 공간 연출, 소품 배치, 홈카페/음료 등 실내 감성 중심
            - AESTHETE (제철을 음미하는 감상가)
              성향: 음악, 영화, 책, 전시처럼 감성적인 콘텐츠로 계절을 깊게 즐기는 타입
              미션 방향: 플레이리스트, 영화, 독서 및 필사 등 정적이고 감성적인 미션 중심
            """;

    private static final String OUTPUT_FORMAT = """

            ## 문장 구조 제약
            - title은 반드시 "~하기"로 끝나야 한다. ("찰칵", "찍기", "먹자" 등의 종결은 절대 금지)
            - 좋은 예: "노란 살구 반으로 갈라보기", "퇴근길 파란 하늘 사진 남기기"
            - 나쁜 예: "퇴근길 파란 하늘 스토리 올리기" (외부 SNS 유도), "노란 살구 반으로 갈라 찰칵" (~하기 미준수)

            ## 출력 형식
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

            ### 중요 규칙
            - title: 반드시 "~하기" 형식으로 끝나야 합니다. (예: "봄나물 캐러 가기", "벚꽃 사진 찍기")
            - description: 20자 이내로 간결하게 작성하세요.

            {
              "missions": [
                {
                  "title": "미션 제목 (~하기 형식, 20자 이내)",
                  "description": "간결한 미션 설명 (20자 이내)",
                  "spaceType": "INDOOR 또는 OUTDOOR",
                  "companionType": "SOLO 또는 TOGETHER",
                  "categoryType": "FOOD, NATURE, CONTENT, PLACE, MUSIC 중 하나",
                  "enjoyType": "NATURE_OUTDOOR, SEASONAL_FOOD, CULTURE_CONTENT 중 하나",
                  "userType": "EXPLORER, WALKER, LIFE_CREATOR, AESTHETE 중 하나"
                }
              ]
            }
            """;

    public static String generate(int count) {
        return SYSTEM_PROMPT +
                MISSION_PRINCIPLES +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                count + "개의 미션을 생성해주세요.";
    }

    public static String generateWithTheme(String theme, int count) {
        return SYSTEM_PROMPT +
                "\n## 테마: " + theme + "\n" +
                MISSION_PRINCIPLES +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + theme + "' 테마에 맞는 " + count + "개의 미션을 생성해주세요.";
    }

    public static String generateWithSolarTerm(SolarTerm solarTerm, int count) {
        String solarTermInfo = buildSolarTermInfo(solarTerm);
        return SYSTEM_PROMPT +
                solarTermInfo +
                MISSION_PRINCIPLES +
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
                MISSION_PRINCIPLES +
                userTypeInfo +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + solarTerm.getName() + "' 절기에 맞고, '" + userType.getLabel() + "' 타입의 사용자를 위한 " + count + "개의 미션을 생성해주세요.\n" +
                "생성되는 모든 미션의 userType은 반드시 '" + userType.name() + "'이어야 합니다.";
    }

    public static String generateWithSolarTermAndUserTypeAndEnjoyType(
            SolarTerm solarTerm, UserType userType, EnjoyType enjoyType, int count) {
        String solarTermInfo = buildSolarTermInfo(solarTerm);
        String userTypeInfo = buildUserTypeInfo(userType);
        String enjoyTypeInfo = buildEnjoyTypeInfo(enjoyType);
        return SYSTEM_PROMPT +
                solarTermInfo +
                MISSION_PRINCIPLES +
                userTypeInfo +
                enjoyTypeInfo +
                ATTRIBUTE_DESCRIPTION +
                OUTPUT_FORMAT +
                "\n## 요청\n" +
                "'" + solarTerm.getName() + "' 절기에 맞고, '" + userType.getLabel() + "' 타입의 사용자를 위한 " +
                "'" + enjoyType.getShortLabel() + "' 카테고리의 미션 " + count + "개를 생성해주세요.\n" +
                "생성되는 모든 미션의 userType은 반드시 '" + userType.name() + "'이어야 합니다.\n" +
                "생성되는 모든 미션의 enjoyType은 반드시 '" + enjoyType.name() + "'이어야 합니다.";
    }

    private static String buildEnjoyTypeInfo(EnjoyType enjoyType) {
        return "\n## 즐기는 방식 지정\n" +
                "- 타입: " + enjoyType.getLabel() + "\n" +
                "- 설명: " + enjoyType.getDescription() + "\n" +
                "\n이 즐기는 방식에 맞는 미션만 생성해주세요.\n";
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