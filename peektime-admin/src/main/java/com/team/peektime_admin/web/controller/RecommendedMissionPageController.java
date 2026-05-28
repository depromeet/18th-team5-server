package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/recommended-missions")
@RequiredArgsConstructor
public class RecommendedMissionPageController {

    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String recommendedMission(
            @RequestParam(required = false) Long solarTermId,
            @RequestParam(required = false, defaultValue = "EXPLORER") String userType,
            Model model
    ) {
        List<SolarTerm> solarTerms = solarTermRepository.findAll();
        model.addAttribute("solarTerms", solarTerms);

        Set<Integer> years = solarTerms.stream()
                .map(SolarTerm::getYear)
                .collect(Collectors.toCollection(TreeSet::new));
        model.addAttribute("years", years);

        // 선택된 절기
        SolarTerm currentTerm = null;
        if (solarTermId != null) {
            currentTerm = solarTermRepository.findById(solarTermId).orElse(null);
        } else if (!solarTerms.isEmpty()) {
            LocalDate today = LocalDate.now();
            currentTerm = solarTermRepository.findByDate(today)
                    .orElse(solarTerms.get(0));
        }

        UserType selectedUserType = UserType.valueOf(userType);
        model.addAttribute("selectedUserType", userType);

        // 기본값 설정 (null 방지)
        model.addAttribute("natureOutdoorMissions", Collections.emptyList());
        model.addAttribute("seasonalFoodMissions", Collections.emptyList());
        model.addAttribute("emotionalContentMissions", Collections.emptyList());
        model.addAttribute("totalCount", 0L);
        model.addAttribute("userTypeStats", Collections.emptyMap());

        if (currentTerm != null) {
            model.addAttribute("currentTerm", currentTerm);
            model.addAttribute("selectedSolarTermId", currentTerm.getId());
            model.addAttribute("selectedYear", currentTerm.getYear());

            // 해당 절기+사용자타입의 추천 미션 조회
            List<RecommendedMissionPool> missions = recommendedMissionPoolRepository
                    .findBySolarTermIdAndUserType(currentTerm.getId(), selectedUserType);

            // EnjoyType별로 분류
            Map<EnjoyType, List<RecommendedMissionPool>> groupedByEnjoyType = missions.stream()
                    .filter(m -> m.getMission().getEnjoyType() != null)
                    .collect(Collectors.groupingBy(m -> m.getMission().getEnjoyType()));

            model.addAttribute("natureOutdoorMissions",
                    groupedByEnjoyType.getOrDefault(EnjoyType.NATURE_OUTDOOR, Collections.emptyList()));
            model.addAttribute("seasonalFoodMissions",
                    groupedByEnjoyType.getOrDefault(EnjoyType.SEASONAL_FOOD, Collections.emptyList()));
            model.addAttribute("emotionalContentMissions",
                    groupedByEnjoyType.getOrDefault(EnjoyType.CULTURE_CONTENT, Collections.emptyList()));

            // 전체 통계
            long totalCount = recommendedMissionPoolRepository.countBySolarTermId(currentTerm.getId());
            model.addAttribute("totalCount", totalCount);

            // 사용자 타입별 통계
            Map<UserType, Long> userTypeStats = new LinkedHashMap<>();
            for (UserType ut : UserType.values()) {
                long count = recommendedMissionPoolRepository.countBySolarTermIdAndUserType(currentTerm.getId(), ut);
                userTypeStats.put(ut, count);
            }
            model.addAttribute("userTypeStats", userTypeStats);
        }

        model.addAttribute("menu", "recommended-mission");
        return "mission/recommended";
    }
}
