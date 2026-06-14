package com.team.peektime_admin.domain.mission.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MissionPromptTemplateTest {

    @Test
    void generatePromptIncludesTitleLimit() {
        String prompt = MissionPromptTemplate.generate(1);

        assertThat(prompt)
                .contains("공백 포함 16자 이내")
                .contains("공백 포함 20자 이내")
                .contains("사용자가 무엇을 어떻게 하면 되는지 알 수 있어야 하며")
                .contains("title을 그대로 반복하지 마세요")
                .doesNotContain("미션 제목 (~하기 형식, 20자 이내)");
    }
}
