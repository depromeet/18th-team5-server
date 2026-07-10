package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.UserSelectedMission;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.aop.DistributedLock;
import com.team.peektime_api.global.common.enums.CategoryType;
import com.team.peektime_api.global.common.enums.CompanionType;
import com.team.peektime_api.global.common.enums.SpaceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * LLM이 생성한 미션을 Mission + UserSelectedMission으로 저장한다.
 * 저장 이후에는 기존 선택 미션과 동일하게 완료/기록 플로우를 탄다.
 */
@Component
@RequiredArgsConstructor
public class LlmMissionRegistrar {

    /**
     * Mission ID는 Admin DB의 ID를 그대로 쓰는 할당 방식이라,
     * LLM 생성 미션은 admin ID와 충돌하지 않는 별도 대역(1,000,000~)을 사용한다.
     * admin 동기화는 upsert만 수행하므로 이 대역의 미션은 동기화에 영향받지 않는다.
     */
    private static final long LLM_MISSION_ID_OFFSET = 1_000_000L;

    private final MissionRepository missionRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final UserRepository userRepository;
    private final SolarTermRepository solarTermRepository;

    // ID 채번(max+1)의 동시성 보호를 위한 분산 락. AOP가 REQUIRES_NEW 트랜잭션으로 실행한다.
    @DistributedLock(key = "'llm-mission-register'")
    public Mission register(Long userId, Long solarTermId, LocalDate today,
                            String title, String description,
                            SpaceType spaceType, CompanionType companionType, CategoryType categoryType) {
        // LLM 호출 동안 같은 사용자의 다른 요청이 먼저 선택을 완료했을 수 있으므로 재확인
        return userSelectedMissionRepository.findByUserIdAndSelectedDate(userId, today)
                .map(selected -> missionRepository.findById(selected.getMission().getId()).orElseThrow())
                .orElseGet(() -> saveNewMission(userId, solarTermId, today,
                        title, description, spaceType, companionType, categoryType));
    }

    private Mission saveNewMission(Long userId, Long solarTermId, LocalDate today,
                                   String title, String description,
                                   SpaceType spaceType, CompanionType companionType, CategoryType categoryType) {
        Long maxId = missionRepository.findMaxIdFrom(LLM_MISSION_ID_OFFSET);
        long nextId = (maxId == null) ? LLM_MISSION_ID_OFFSET : maxId + 1;

        Mission mission = missionRepository.save(Mission.createLlmGenerated(
                nextId, title, description, spaceType, categoryType, companionType));

        userSelectedMissionRepository.save(UserSelectedMission.create(
                userRepository.getReferenceById(userId),
                mission,
                solarTermRepository.getReferenceById(solarTermId),
                today
        ));

        return mission;
    }
}