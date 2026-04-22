package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_onboarding", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboarding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_category1", nullable = false)
    private CategoryType preferredCategory1;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_category2", nullable = false)
    private CategoryType preferredCategory2;

    @Builder
    public UserOnboarding(User user, UserType userType,
                          CategoryType preferredCategory1, CategoryType preferredCategory2) {
        this.user = user;
        this.userType = userType;
        this.preferredCategory1 = preferredCategory1;
        this.preferredCategory2 = preferredCategory2;
    }
}
