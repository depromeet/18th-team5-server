package com.team.peektime_api.domain.user.dto;

import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.entity.UserCategoryPreference;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.UserType;
import lombok.Getter;

import java.util.List;

@Getter
public class UserOnboardingResponse {

    private final Long userId;
    private final UserType userType;
    private final List<CategoryRankItem> categoryRanks;

    public UserOnboardingResponse(User user, List<UserCategoryPreference> preferences) {
        this.userId = user.getId();
        this.userType = user.getUserType();
        this.categoryRanks = preferences.stream()
                .map(CategoryRankItem::new)
                .toList();
    }

    @Getter
    public static class CategoryRankItem {

        private final EnjoyType category;
        private final int rank;

        public CategoryRankItem(UserCategoryPreference preference) {
            this.category = preference.getCategory();
            this.rank = preference.getRank();
        }
    }
}
