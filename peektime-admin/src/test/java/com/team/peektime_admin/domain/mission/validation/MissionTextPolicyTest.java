package com.team.peektime_admin.domain.mission.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MissionTextPolicyTest {

    @Test
    void validateTitleAllowsSixteenCharactersIncludingSpaces() {
        assertThatCode(() -> MissionTextPolicy.validateTitle("12345678 1234567"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTitleRejectsOverSixteenCharactersIncludingSpaces() {
        assertThatThrownBy(() -> MissionTextPolicy.validateTitle("12345678 12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공백 포함 16자 이내");
    }

    @Test
    void validateDescriptionAllowsTwentyCharactersIncludingSpaces() {
        assertThatCode(() -> MissionTextPolicy.validateDescription("1234567890 123456789"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateDescriptionRejectsOverTwentyCharactersIncludingSpaces() {
        assertThatThrownBy(() -> MissionTextPolicy.validateDescription("1234567890 1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("공백 포함 20자 이내");
    }
}
