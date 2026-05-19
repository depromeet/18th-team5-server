package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
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
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SettingsPageController {

    private final SolarTermRepository solarTermRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;

    @GetMapping
    public String settings(
            @RequestParam(required = false) Integer year,
            Model model
    ) {
        List<SolarTerm> allTerms = solarTermRepository.findAll();

        // 연도 목록 추출
        Set<Integer> years = allTerms.stream()
                .map(SolarTerm::getYear)
                .collect(Collectors.toCollection(TreeSet::new));
        model.addAttribute("years", years);

        // 선택된 연도 (기본: 현재 연도)
        int selectedYear = year != null ? year : LocalDate.now().getYear();
        model.addAttribute("selectedYear", selectedYear);

        // 해당 연도의 절기 목록
        List<SolarTerm> solarTerms = allTerms.stream()
                .filter(t -> t.getYear().equals(selectedYear))
                .sorted(Comparator.comparing(SolarTerm::getStartDate))
                .collect(Collectors.toList());
        model.addAttribute("solarTerms", solarTerms);

        // 현재 절기 ID
        LocalDate today = LocalDate.now();
        Long currentTermId = solarTermRepository.findByDate(today)
                .map(SolarTerm::getId)
                .orElse(null);
        model.addAttribute("currentTermId", currentTermId);

        // 절기별 오늘의 미션 개수
        Map<Long, Long> dailyMissionStats = new HashMap<>();
        for (SolarTerm term : solarTerms) {
            long count = dailyMissionRepository.countBySolarTermId(term.getId());
            dailyMissionStats.put(term.getId(), count);
        }
        model.addAttribute("dailyMissionStats", dailyMissionStats);

        // 절기별 추천 미션 개수
        Map<Long, Long> recommendedMissionStats = new HashMap<>();
        for (SolarTerm term : solarTerms) {
            long count = recommendedMissionPoolRepository.countBySolarTermId(term.getId());
            recommendedMissionStats.put(term.getId(), count);
        }
        model.addAttribute("recommendedMissionStats", recommendedMissionStats);

        model.addAttribute("menu", "settings");
        return "settings/index";
    }
}
