package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.dto.RecommendedMissionResponse;
import com.team.peektime_admin.domain.mission.dto.RecommendedMissionResponse.HeaderInfo;
import com.team.peektime_admin.domain.mission.dto.RecommendedMissionResponse.MissionItem;
import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedMissionService {

    private final SolarTermRepository solarTermRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;

    public RecommendedMissionResponse getRecommendedMissions(
            UserType userType,
            EnjoyType enjoyTypeFirst,
            EnjoyType enjoyTypeSecond,
            EnjoyType enjoyTypeThird
    ) {
        SolarTerm currentSolarTerm = solarTermRepository.findByDate(LocalDate.now())
                .orElse(null);

        if (currentSolarTerm == null) {
            HeaderInfo header = new HeaderInfo(userType.getLabel() + " 미션", "지금 어떤 기록을 남겨볼까요?");
            return new RecommendedMissionResponse(header, List.of());
        }

        List<RecommendedMissionPool> pools =
                recommendedMissionPoolRepository.findBySolarTermIdAndUserType(currentSolarTerm.getId(), userType);

        List<EnjoyType> priority = List.of(enjoyTypeFirst, enjoyTypeSecond, enjoyTypeThird);
        List<MissionItem> sorted = sortByPriority(pools, priority);

        HeaderInfo header = HeaderInfo.of(userType.getLabel(), currentSolarTerm.getName());
        return new RecommendedMissionResponse(header, sorted);
    }

    private List<MissionItem> sortByPriority(List<RecommendedMissionPool> pools, List<EnjoyType> priority) {
        Map<EnjoyType, List<RecommendedMissionPool>> grouped = pools.stream()
                .collect(Collectors.groupingBy(p -> p.getMission().getEnjoyType()));

        List<MissionItem> result = new ArrayList<>();
        int order = 1;

        for (EnjoyType enjoyType : priority) {
            List<RecommendedMissionPool> group = grouped.getOrDefault(enjoyType, List.of());
            List<RecommendedMissionPool> sorted = group.stream()
                    .sorted(Comparator.comparingInt(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : Integer.MAX_VALUE))
                    .toList();
            for (RecommendedMissionPool pool : sorted) {
                result.add(MissionItem.from(pool, order++));
            }
        }

        return result;
    }
}
