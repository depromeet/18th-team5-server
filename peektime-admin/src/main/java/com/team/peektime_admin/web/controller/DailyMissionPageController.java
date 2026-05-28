package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/daily-missions")
@RequiredArgsConstructor
public class DailyMissionPageController {

    private final DailyMissionRepository dailyMissionRepository;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String dailyMission(
            @RequestParam(required = false) Long solarTermId,
            Model model
    ) {
        List<SolarTerm> solarTerms = solarTermRepository.findAll();
        model.addAttribute("solarTerms", solarTerms);

        // 연도 목록
        Set<Integer> years = solarTerms.stream()
                .map(SolarTerm::getYear)
                .collect(Collectors.toCollection(TreeSet::new));
        model.addAttribute("years", years);

        // 선택된 절기
        SolarTerm currentTerm = null;
        if (solarTermId != null) {
            currentTerm = solarTermRepository.findById(solarTermId).orElse(null);
        } else if (!solarTerms.isEmpty()) {
            // 현재 날짜 기준 절기 또는 첫 번째 절기
            LocalDate today = LocalDate.now();
            currentTerm = solarTermRepository.findByDate(today)
                    .orElse(solarTerms.get(0));
        }

        // 기본값 설정 (null 방지)
        model.addAttribute("pendingMissions", Collections.emptyList());
        model.addAttribute("dateSlots", Collections.emptyList());

        if (currentTerm != null) {
            model.addAttribute("currentTerm", currentTerm);
            model.addAttribute("selectedSolarTermId", currentTerm.getId());
            model.addAttribute("selectedYear", currentTerm.getYear());

            // 배정 대기 미션 (날짜 없는 것)
            List<DailyMission> pendingMissions = dailyMissionRepository
                    .findBySolarTermIdAndMissionDateIsNull(currentTerm.getId());
            model.addAttribute("pendingMissions", pendingMissions);

            // 배정된 미션들
            List<DailyMission> assignedMissions = dailyMissionRepository
                    .findBySolarTermIdAndMissionDateIsNotNull(currentTerm.getId());
            Map<LocalDate, DailyMission> assignedMap = assignedMissions.stream()
                    .collect(Collectors.toMap(DailyMission::getMissionDate, dm -> dm));

            // 날짜 슬롯 생성
            List<DateSlot> dateSlots = new ArrayList<>();
            LocalDate date = currentTerm.getStartDate();
            while (!date.isAfter(currentTerm.getEndDate())) {
                DailyMission dm = assignedMap.get(date);
                dateSlots.add(new DateSlot(
                        date,
                        getKoreanWeekday(date.getDayOfWeek()),
                        isWeekend(date.getDayOfWeek()),
                        dm != null ? dm.getMission() : null,
                        dm != null ? dm.getId() : null
                ));
                date = date.plusDays(1);
            }
            model.addAttribute("dateSlots", dateSlots);
        }

        model.addAttribute("menu", "daily-mission");
        return "mission/daily";
    }

    private String getKoreanWeekday(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    private boolean isWeekend(DayOfWeek dow) {
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    @Getter
    public static class DateSlot {
        private final LocalDate date;
        private final String weekday;
        private final boolean isWeekend;
        private final Mission mission;
        private final Long dailyMissionId;

        public DateSlot(LocalDate date, String weekday, boolean isWeekend, Mission mission, Long dailyMissionId) {
            this.date = date;
            this.weekday = weekday;
            this.isWeekend = isWeekend;
            this.mission = mission;
            this.dailyMissionId = dailyMissionId;
        }
    }
}