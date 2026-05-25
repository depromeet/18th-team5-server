package com.team.peektime_api.domain.mission.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import com.team.peektime_api.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.team.peektime_api.domain.mission.entity.QDailyMission.dailyMission;
import static com.team.peektime_api.domain.mission.entity.QMission.mission;
import static com.team.peektime_api.domain.mission.entity.QRecommendedMissionPool.recommendedMissionPool;
import static com.team.peektime_api.domain.mission.entity.QUserSelectedMission.userSelectedMission;

@RequiredArgsConstructor
public class MissionRepositoryImpl implements MissionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Mission> findSelectedMissions(Long userId, Long solarTermId, UserType userType, SelectedMissionRequest filter) {
        return queryFactory
                .selectFrom(mission)
                .where(
                        mission.deleted.isFalse(),
                        notInDailyMissions(solarTermId),
                        notInRecommendedMissions(solarTermId, userType),
                        notInUserSelectedMissions(userId, solarTermId),
                        eqSpaceType(filter.getSpaceType()),
                        eqCompanionType(filter.getCompanionType()),
                        eqCategoryType(filter.getCategoryType())
                )
                .fetch();
    }

    private BooleanExpression notInDailyMissions(Long solarTermId) {
        return mission.id.notIn(
                queryFactory
                        .select(dailyMission.mission.id)
                        .from(dailyMission)
                        .where(dailyMission.solarTerm.id.eq(solarTermId))
        );
    }

    private BooleanExpression notInRecommendedMissions(Long solarTermId, UserType userType) {
        return mission.id.notIn(
                queryFactory
                        .select(recommendedMissionPool.mission.id)
                        .from(recommendedMissionPool)
                        .where(
                                recommendedMissionPool.solarTerm.id.eq(solarTermId),
                                recommendedMissionPool.userType.eq(userType)
                        )
        );
    }

    private BooleanExpression notInUserSelectedMissions(Long userId, Long solarTermId) {
        return mission.id.notIn(
                queryFactory
                        .select(userSelectedMission.mission.id)
                        .from(userSelectedMission)
                        .where(
                                userSelectedMission.user.id.eq(userId),
                                userSelectedMission.solarTerm.id.eq(solarTermId)
                        )
        );
    }

    private BooleanExpression eqSpaceType(SpaceType spaceType) {
        return spaceType != null ? mission.spaceType.eq(spaceType) : null;
    }

    private BooleanExpression eqCompanionType(CompanionType companionType) {
        return companionType != null ? mission.companionType.eq(companionType) : null;
    }

    private BooleanExpression eqCategoryType(CategoryType categoryType) {
        return categoryType != null ? mission.categoryType.eq(categoryType) : null;
    }
}