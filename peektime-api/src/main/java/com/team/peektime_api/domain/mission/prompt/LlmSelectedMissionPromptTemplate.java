package com.team.peektime_api.domain.mission.prompt;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;

/**
 * 사용자가 선택한 태그(공간/동반/카테고리)와 현재 절기를 조건으로
 * 선택 미션 1개를 즉시 생성하는 프롬프트.
 * 미션 선정 원칙은 admin 모듈의 MissionPromptTemplate과 동일한 기준을 따른다.
 */
public class LlmSelectedMissionPromptTemplate {

    public static final int TITLE_MAX_LENGTH = 16;
    public static final int DESCRIPTION_MAX_LENGTH = 30;

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

            ### 2. 사진 기록 중심 (Photo Friendly, App-Centric) — 가장 중요
            - 이 서비스는 미션을 수행하며 '사진을 찍고 메모를 남기는' 서비스다. 따라서 미션에는 반드시 '사진으로 찍을 수 있는 눈에 보이는 피사체'가 있어야 한다.
            - 소리·냄새·기분처럼 눈에 보이지 않는 것만 다루는 미션은 절대 금지한다.
              - 나쁜 예: "매미 울음소리에 귀 기울이기" (찍을 대상이 없음)
              - 좋은 예: "빗방울 맺힌 창문 바라보기" (창문이라는 피사체가 있음)
            - 인스타그램 스토리 등 외부 SNS 포스팅을 강제하는 행동은 절대 금지한다.

            ### 3. 구체적 일상 오브제를 적극 활용
            - 누구나 마주치는 구체적 장면·오브제를 활용한다: 편의점, 과일가게, 가로수, 보도블록, 버스 창가, 화단, 베란다 화분 등.
            - 단, '일부러 찾아가야 하는 특정 장소'는 금지한다.
            - 색·온도·질감을 나타내는 형용사로 장면을 생생하게 만든다. (붉은/푸른/싱그러운/시원한/은은한/짙어진 등)

            ### 4. 문장 패턴
            - 권장 구조: [일상 맥락] + [감각 형용사] + [구체 오브제] + [감각 동사]
              예) "과일가게의 / 붉은 / 살구 / 구경하기"
            - 반드시 동사의 명사형(-기)으로 끝낸다. (마시기, 보기, 듣기, 맛보기, 남기기, 만져보기 등)
            - "찰칵", "먹자", "스토리 올리기" 같은 종결은 절대 금지한다.

            ### 5. 일상 언어 사용
            - title과 description은 일상 대화에서 누구나 쓰는 쉬운 단어만 사용한다.
            - 문어체·한자어·평소에 잘 쓰지 않는 단어는 금지한다.
              - 나쁜 예: 노점상, 음미하기, 청취하기
              - 좋은 예: 과일가게, 맛보기, 듣기
            """;

    public static String generate(SolarTerm solarTerm, SelectedMissionRequest filter) {
        return SYSTEM_PROMPT +
                buildSolarTermInfo(solarTerm) +
                MISSION_PRINCIPLES +
                buildTagConditions(filter) +
                buildOutputFormat();
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

    // 태그는 요청에서 모두 필수값으로 검증되므로 항상 값이 있다
    private static String buildTagConditions(SelectedMissionRequest filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 사용자가 선택한 태그 조건 (반드시 준수)\n");

        SpaceType spaceType = filter.getSpaceType();
        sb.append("- 공간(spaceType): 반드시 ").append(spaceType.name())
                .append("(").append(spaceType.getLabel()).append(") — ")
                .append(spaceType.getDescription()).append("\n");

        CompanionType companionType = filter.getCompanionType();
        sb.append("- 동반(companionType): 반드시 ").append(companionType.name())
                .append("(").append(companionType.getLabel()).append(") — ")
                .append(companionType.getDescription()).append("\n");

        CategoryType categoryType = filter.getCategoryType();
        sb.append("- 카테고리(categoryType): 반드시 ").append(categoryType.name())
                .append("(").append(categoryType.getLabel()).append(") — ")
                .append(categoryType.getDescription()).append("\n");

        return sb.toString();
    }

    private static String buildOutputFormat() {
        return """

                ## 요청
                위 절기와 태그 조건에 모두 어울리는 미션 1개를 생성하세요.

                ## 출력 형식
                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                ### 글자 수 규칙 (반드시 세어서 확인)
                - title: 동사 명사형(-기)으로 끝내고, 공백 포함 %d자 이내.
                - description: 공백 포함 %d자 이내. title을 그대로 반복하지 말고, 무엇을 어떻게 하면 되는지 앱 안내 문구처럼 자연스럽게 작성.

                {
                  "title": "미션 제목 (-기 종결, 공백 포함 %d자 이내)",
                  "description": "미션 설명 (공백 포함 %d자 이내)"
                }
                """.formatted(
                TITLE_MAX_LENGTH,
                DESCRIPTION_MAX_LENGTH,
                TITLE_MAX_LENGTH,
                DESCRIPTION_MAX_LENGTH);
    }
}