package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.UserSelectedMission;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * LLM이 생성한 미션을 Mission + UserSelectedMission으로 저장한다.
 * 저장 이후에는 기존 선택 미션과 동일하게 완료/기록 플로우를 탄다.
 * Mission ID는 auto increment로 채번하며, 같은 사용자의 동시 요청은
 * user_selected_mission(user_id, selected_date) unique 제약이 막는다.
 */
@Component
@RequiredArgsConstructor
public class LlmMissionRegistrar {

    private final MissionRepository missionRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final UserRepository userRepository;
    private final SolarTermRepository solarTermRepository;

    @Transactional
    public Mission register(Long userId, Long solarTermId, LocalDate today,
                            String title, String description,
                            SpaceType spaceType, CompanionType companionType, CategoryType categoryType) {
        Mission mission = missionRepository.save(Mission.createLlmGenerated(
                title, description, spaceType, categoryType, companionType));

        userSelectedMissionRepository.save(UserSelectedMission.create(
                userRepository.getReferenceById(userId),
                mission,
                solarTermRepository.getReferenceById(solarTermId),
                today
        ));

        return mission;
    }
}