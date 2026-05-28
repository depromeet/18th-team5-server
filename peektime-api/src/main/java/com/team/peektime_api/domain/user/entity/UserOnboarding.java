package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.ActivityStyleType;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.SpaceType;
import com.team.peektime_api.global.common.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_onboarding", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_onboarding", columnNames = {"user_id"})
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

    // 1번 질문: 갑자기 쉬는 날이 생겼어요. 어떻게 보내고 싶으세요?
    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private SpaceType spaceType;

    // 2번 질문: 시간내서 적극적으로 vs 일상 안에서 부담없이
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_style_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private ActivityStyleType activityStyleType;

    // 3번 질문: 계절을 주로 어떻게 즐기는 편이에요? (우선순위)
    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_first", nullable = false, columnDefinition = "VARCHAR(50)")
    private EnjoyType enjoyTypeFirst;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_second", nullable = false, columnDefinition = "VARCHAR(50)")
    private EnjoyType enjoyTypeSecond;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_third", nullable = false, columnDefinition = "VARCHAR(50)")
    private EnjoyType enjoyTypeThird;

    // 1번 + 2번 질문 조합으로 자동 계산
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private UserType userType;

    @Builder
    public UserOnboarding(User user, SpaceType spaceType, ActivityStyleType activityStyleType,
                          EnjoyType enjoyTypeFirst, EnjoyType enjoyTypeSecond, EnjoyType enjoyTypeThird) {
        this.user = user;
        this.spaceType = spaceType;
        this.activityStyleType = activityStyleType;
        this.enjoyTypeFirst = enjoyTypeFirst;
        this.enjoyTypeSecond = enjoyTypeSecond;
        this.enjoyTypeThird = enjoyTypeThird;
        this.userType = determineUserType(spaceType, activityStyleType);
    }

    private UserType determineUserType(SpaceType space, ActivityStyleType activityStyle) {
        if (space == SpaceType.OUTDOOR && activityStyle == ActivityStyleType.ACTIVE) {
            return UserType.EXPLORER;
        } else if (space == SpaceType.OUTDOOR) {
            return UserType.WALKER;
        } else if (activityStyle == ActivityStyleType.ACTIVE) {
            return UserType.LIFE_CREATOR;
        } else {
            return UserType.AESTHETE;
        }
    }

    public void update(SpaceType spaceType, ActivityStyleType activityStyleType,
                       EnjoyType enjoyTypeFirst, EnjoyType enjoyTypeSecond, EnjoyType enjoyTypeThird) {
        this.spaceType = spaceType;
        this.activityStyleType = activityStyleType;
        this.enjoyTypeFirst = enjoyTypeFirst;
        this.enjoyTypeSecond = enjoyTypeSecond;
        this.enjoyTypeThird = enjoyTypeThird;
        this.userType = determineUserType(spaceType, activityStyleType);
    }
}
