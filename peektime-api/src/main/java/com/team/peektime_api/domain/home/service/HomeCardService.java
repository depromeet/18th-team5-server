package com.team.peektime_api.domain.home.service;

import com.team.peektime_api.domain.home.dto.HomeResponse;
import com.team.peektime_api.domain.home.dto.HomeResponse.DailyMissionInfo;
import com.team.peektime_api.domain.home.dto.HomeResponse.SolarTermInfo;
import com.team.peektime_api.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCardService {

    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final SolarTermRepository solarTermRepository;

    public HomeResponse getHome(Long userId) {
        LocalDate today = LocalDate.now();

        // 절기는 오늘의 미션과 독립적으로 조회 (절기가 없으면 화면 구성 불가 → 에러)
        SolarTerm solarTerm = solarTermRepository.findByDate(today)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        // 오늘의 미션은 없을 수 있음 → 없으면 null (404 대신 정상 응답)
        DailyMissionInfo dailyMission = dailyMissionRepository.findByMissionDateWithDetails(today)
                .map(dm -> DailyMissionInfo.from(dm, userMissionCompletionRepository
                        .existsByUser_IdAndMission_Id(userId, dm.getMission().getId())))
                .orElse(null);

        return HomeResponse.of(SolarTermInfo.from(solarTerm), dailyMission);
    }
}