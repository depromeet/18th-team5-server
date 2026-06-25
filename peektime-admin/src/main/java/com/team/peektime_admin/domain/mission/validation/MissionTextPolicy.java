package com.team.peektime_admin.domain.mission.validation;

public final class MissionTextPolicy {

    public static final int TITLE_MAX_LENGTH = 16;
    public static final int DESCRIPTION_MAX_LENGTH = 30;

    private MissionTextPolicy() {
    }

    public static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("미션 title은 필수입니다.");
        }

        validateMaxLength(title, TITLE_MAX_LENGTH, "title");
    }

    public static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            return;
        }

        validateMaxLength(description, DESCRIPTION_MAX_LENGTH, "description");
    }

    private static void validateMaxLength(String text, int maxLength, String fieldName) {
        int length = countCharacters(text);
        if (length > maxLength) {
            throw new IllegalArgumentException(
                    "미션 " + fieldName + "은 공백 포함 " + maxLength + "자 이내여야 합니다. 현재 " + length + "자입니다.");
        }
    }

    private static int countCharacters(String text) {
        return text.codePointCount(0, text.length());
    }
}
