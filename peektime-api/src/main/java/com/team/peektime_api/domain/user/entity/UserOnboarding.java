package com.team.peektime_api.domain.user.entity;

import com.team.peektime_api.global.common.BaseEntity;
import com.team.peektime_api.global.common.enums.EnjoyType;
import com.team.peektime_api.global.common.enums.IntensityType;
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
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

    // 2번 질문: 계절 활동, 어떤 방식이 더 잘 맞아요?
    @Enumerated(EnumType.STRING)
    @Column(name = "intensity_type", nullable = false)
    private IntensityType intensityType;

    // 3번 질문: 계절을 주로 어떻게 즐기는 편이에요? (우선순위)
    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_first", nullable = false)
    private EnjoyType enjoyTypeFirst;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_second", nullable = false)
    private EnjoyType enjoyTypeSecond;

    @Enumerated(EnumType.STRING)
    @Column(name = "enjoy_type_third", nullable = false)
    private EnjoyType enjoyTypeThird;

    // 1번 + 2번 질문 조합으로 자동 계산
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Builder
    public UserOnboarding(User user, SpaceType spaceType, IntensityType intensityType,
                          EnjoyType enjoyTypeFirst, EnjoyType enjoyTypeSecond, EnjoyType enjoyTypeThird) {
        this.user = user;
        this.spaceType = spaceType;
        this.intensityType = intensityType;
        this.enjoyTypeFirst = enjoyTypeFirst;
        this.enjoyTypeSecond = enjoyTypeSecond;
        this.enjoyTypeThird = enjoyTypeThird;
        this.userType = determineUserType(spaceType, intensityType);
    }

    private UserType determineUserType(SpaceType space, IntensityType intensity) {
        if (space == SpaceType.OUTDOOR && intensity == IntensityType.ACTIVE) {
            return UserType.NATURE_EXPLORER;
        } else if (space == SpaceType.OUTDOOR) {
            return UserType.NEIGHBORHOOD_WALKER;
        } else if (intensity == IntensityType.ACTIVE) {
            return UserType.SEASONAL_GOURMET;
        } else {
            return UserType.DAILY_OBSERVER;
        }
    }

    public void update(SpaceType spaceType, IntensityType intensityType,
                       EnjoyType enjoyTypeFirst, EnjoyType enjoyTypeSecond, EnjoyType enjoyTypeThird) {
        this.spaceType = spaceType;
        this.intensityType = intensityType;
        this.enjoyTypeFirst = enjoyTypeFirst;
        this.enjoyTypeSecond = enjoyTypeSecond;
        this.enjoyTypeThird = enjoyTypeThird;
        this.userType = determineUserType(spaceType, intensityType);
    }
}
