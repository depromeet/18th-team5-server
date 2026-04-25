package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final MissionRepository missionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String dashboard(Model model) {
        // 현재 절기 조회
        LocalDate today = LocalDate.now();
        Optional<SolarTerm> currentTerm = solarTermRepository.findByDate(today);

        // 통계 데이터
        long totalMissions = missionRepository.countByDeletedFalse();

        currentTerm.ifPresent(term -> {
            model.addAttribute("currentTerm", term);

            long totalDailyMissions = dailyMissionRepository.countBySolarTermId(term.getId());
            long assignedDailyMissions = dailyMissionRepository.countBySolarTermIdAndMissionDateIsNotNull(term.getId());
            long recommendedMissions = recommendedMissionPoolRepository.countBySolarTermId(term.getId());

            model.addAttribute("totalDailyMissions", totalDailyMissions);
            model.addAttribute("assignedDailyMissions", assignedDailyMissions);
            model.addAttribute("recommendedMissions", recommendedMissions);
        });

        model.addAttribute("totalMissions", totalMissions);
        model.addAttribute("unassignedMissions", totalMissions -
            (Long) model.asMap().getOrDefault("totalDailyMissions", 0L) -
            (Long) model.asMap().getOrDefault("recommendedMissions", 0L));
        model.addAttribute("menu", "dashboard");

        return "dashboard";
    }
}
