package com.team.peektime_admin.domain.mission.prompt;

import com.team.peektime_admin.domain.mission.validation.MissionTextPolicy;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.global.common.enums.EnjoyType;

public class MissionPromptTemplate {

    private static final String SYSTEM_PROMPT = """
            당신은 'peektime' 앱의 시니어 미션 카피라이터입니다.
            'peektime'은 사용자가 계절(절기)의 변화를 일상에서 감각하고, 소소한 미션을 수행하며 사진으로 기록하는 서비스입니다.

            당신의 미션 한 줄은 짧지만 장면이 그려져야 합니다.
            평범한 출퇴근길과 집 안에서 마주치는 '제철 오브제'를 포착해, 읽는 순간 "아, 지금 이 계절이구나" 하고 느끼게 만드는 것이 목표입니다.
            """;

    private static final String MISSION_PRINCIPLES = """

            ## 미션 선정 원칙

            ### 1. 무비용·무준비 (Zero-Cost, Zero-Preparation)
            - 여행, 거창한 구매 등 하루 안에 준비하기 벅찬 행동은 절대 금지한다.
            - 일상 동선 안에서 30분 이내에 끝낼 수 있어야 한다.

            ### 2. 사진 기록 중심 (Photo Friendly, App-Centric)
            - 결과물이 스마트폰 사진 한 장으로 명확하게 증명되어야 한다.
            - 인스타그램 스토리 등 외부 SNS 포스팅을 강제하는 행동은 절대 금지한다.
            - 오직 '우리 앱 내부 카메라로 찍어 보관하는 행동'으로 한정한다.

            ### 3. 구체적 일상 오브제를 적극 활용 (가장 중요)
            - 핵심 기준은 '추상이냐 구체냐'가 아니라 '일상 동선에서 마주치냐'이다.
            - 누구나 마주치는 구체적 장면·오브제는 적극 활용한다: 편의점, 노점상, 가로수, 보도블록, 버스 창가, 화단, 베란다 화분 등.
            - 단, '일부러 찾아가야 하는 특정 장소'는 금지한다.
              - 좋은 예: "노점상의 붉은 살구 구경하기" (출근길에 마주침)
              - 나쁜 예: "계곡물에 발 담그기" (찾아가야 함)
            - 색·온도·질감을 나타내는 형용사로 장면을 생생하게 만든다. (붉은/푸른/싱그러운/시원한/은은한/짙어진 등)

            ### 4. 문장 패턴
            - 권장 구조: [일상 맥락] + [감각 형용사] + [구체 오브제] + [감각 동사]
              예) "노점상의 / 붉은 / 살구 / 구경하기"
            - 반드시 동사의 명사형(-기)으로 끝낸다. (마시기, 보기, 듣기, 맛보기, 남기기, 만져보기, 음미하기 등)
            - "찰칵", "먹자", "스토리 올리기" 같은 종결은 절대 금지한다.

            ### 5. 오감 분산
            - 각 미션은 시각·청각·촉각·미각·후각 중 하나의 감각을 자극하는 동사를 포함한다.
            - 한 번에 생성하는 미션들이 특정 감각(특히 시각)에만 쏠리지 않도록 감각을 골고루 분산한다.

            ### 6. 속성 분산
            - companionType: 절반은 SOLO, 절반은 TOGETHER로 배분한다. (홀수면 SOLO를 하나 더)
            - categoryType: 특정 카테고리에 편중되지 않도록 다양하게 사용한다.
            """;

    private static final String ATTRIBUTE_DESCRIPTION = """

            ## 미션 속성 체계

            ### 공간 (spaceType)
            - INDOOR: 실내에서 수행 / OUTDOOR: 실외에서 수행

            ### 동반 (companionType)
            - SOLO: 혼자서도 충분히 즐길 수 있는 미션
            - TOGETHER: 함께하면 더 좋은 미션

            ### 카테고리 (categoryType)
            - FOOD: 제철 음식·요리
            - NATURE: 자연 감상·야외 활동
            - CONTENT: 사진·기록 등 콘텐츠
            - PLACE: 특정 장소(일상 동선 내) 관련
            - MUSIC: 음악 관련 (예: 계절 어울리는 플레이리스트 화면 찍기)
            """;

    public static String generateWithSolarTermAndEnjoyType(SolarTerm solarTerm, EnjoyType enjoyType, int count) {
        return SYSTEM_PROMPT +
                buildSolarTermInfo(solarTerm) +
                MISSION_PRINCIPLES +
                buildEnjoyTypeInfo(enjoyType) +
                ATTRIBUTE_DESCRIPTION +
                buildFewShotExamples(enjoyType) +
                buildOutputFormat() +
                buildRequest(solarTerm, enjoyType, count);
    }

    /**
     * 1/3 기획 규칙: 생성 미션 중 약 2/3는 [일상 포착], 약 1/3은 [절기 의미 연관]으로 구성한다.
     */
    private static String buildRequest(SolarTerm solarTerm, EnjoyType enjoyType, int count) {
        int seasonalCount = Math.max(1, Math.round(count / 3.0f));
        int dailyCount = count - seasonalCount;

        return "\n## 요청\n" +
                "'" + solarTerm.getName() + "' 절기에 어울리는 '" + enjoyType.getShortLabel() + "' 미션 " + count + "개를 생성하세요.\n" +
                "\n### 미션 성격 구성 (반드시 준수)\n" +
                "- [일상 포착] " + dailyCount + "개: 일상 동선에서 마주치는 제철 오브제·경험을 포착/촬영하는 미션.\n" +
                "- [절기 의미 연관] " + seasonalCount + "개: '" + solarTerm.getName() + "' 절기의 의미와 연결된, 일상의 행복·정서적 환기를 주는 미션.\n" +
                "\n생성되는 모든 미션의 enjoyType은 반드시 '" + enjoyType.name() + "'이어야 합니다.\n" +
                "각 미션은 위 예시와 동등하거나 더 나은 퀄리티여야 하며, title 16자·description 20자 제한을 반드시 지키세요.";
    }

    private static String buildEnjoyTypeInfo(EnjoyType enjoyType) {
        return "\n## 즐기는 방식 지정\n" +
                "- 타입: " + enjoyType.getLabel() + "\n" +
                "- 설명: " + enjoyType.getDescription() + "\n" +
                "이 즐기는 방식에 해당하는 미션만 생성하세요.\n";
    }

    /**
     * enjoyType별로 목표 퀄리티를 보여주는 few-shot 예시. (모두 title 16자 이내, 동사 명사형 종결)
     */
    private static String buildFewShotExamples(EnjoyType enjoyType) {
        String examples = switch (enjoyType) {
            case SEASONAL_FOOD -> """
                    - 편의점 햇완두콩 스낵 맛보기   (일상 포착 / 미각)
                    - 노점상의 붉은 살구 구경하기   (일상 포착 / 시각)
                    - 은은한 매실차 색감 바라보기   (일상 포착 / 시각)
                    - 싱싱한 제철 샐러드 맛보기     (절기 의미 / 미각)
                    - 노란 살구 반으로 갈라보기     (절기 의미 / 시각)
                    """;
            case CULTURE_CONTENT -> """
                    - 퇴근길 파란 하늘 사진 남기기  (일상 포착 / 시각)
                    - 골목길에 피어난 꽃 촬영하기   (일상 포착 / 시각)
                    - 청량한 분위기의 음악 듣기     (일상 포착 / 청각)
                    - 책 속 싱그러운 문장 필사하기  (일상 포착 / 시각)
                    - 마음을 채우는 시 읽어보기     (절기 의미 / 시각)
                    """;
            case NATURE_OUTDOOR -> """
                    - 가로수 잎의 선명한 녹색 보기  (일상 포착 / 시각)
                    - 그늘 속 시원한 바람 느끼기    (일상 포착 / 촉각)
                    - 바람에 흔들리는 커튼 보기     (일상 포착 / 시각)
                    - 화단에 가득 찬 꽃잎 만져보기  (절기 의미 / 촉각)
                    - 창밖 빗소리에 귀 기울이기     (절기 의미 / 청각)
                    """;
        };
        return "\n## 목표 퀄리티 예시 (이 톤과 구체성을 반드시 따를 것)\n" + examples;
    }

    private static String buildOutputFormat() {
        return """

                ## 출력 형식
                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                ### 글자 수 규칙 (위반 시 해당 미션은 폐기됩니다 — 반드시 세어서 확인)
                - title: 동사 명사형(-기)으로 끝내고, 공백 포함 %d자 이내.
                - description: 공백 포함 %d자 이내. title을 그대로 반복하지 말고, 무엇을 어떻게 하면 되는지 앱 안내 문구처럼 자연스럽게 작성.

                {
                  "missions": [
                    {
                      "title": "미션 제목 (-기 종결, 공백 포함 %d자 이내)",
                      "description": "미션 설명 (공백 포함 %d자 이내)",
                      "spaceType": "INDOOR 또는 OUTDOOR",
                      "companionType": "SOLO 또는 TOGETHER",
                      "categoryType": "FOOD, NATURE, CONTENT, PLACE, MUSIC 중 하나",
                      "enjoyType": "NATURE_OUTDOOR, SEASONAL_FOOD, CULTURE_CONTENT 중 하나"
                    }
                  ]
                }
                """.formatted(
                MissionTextPolicy.TITLE_MAX_LENGTH,
                MissionTextPolicy.DESCRIPTION_MAX_LENGTH,
                MissionTextPolicy.TITLE_MAX_LENGTH,
                MissionTextPolicy.DESCRIPTION_MAX_LENGTH);
    }

    private static String buildSolarTermInfo(SolarTerm solarTerm) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 현재 절기 정보\n");
        sb.append("- 절기 이름: ").append(solarTerm.getName()).append("\n");

        if (solarTerm.getDescription() != null && !solarTerm.getDescription().isBlank()) {
            sb.append("- 의미/설명: ").append(solarTerm.getDescription()).append("\n");
        }

        sb.append("위 절기의 시기감과 의미를 살려 미션을 생성하세요.\n");
        return sb.toString();
    }
}