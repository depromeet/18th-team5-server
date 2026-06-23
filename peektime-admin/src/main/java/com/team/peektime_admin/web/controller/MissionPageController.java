package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.entity.RecommendedMissionPool;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.CategoryType;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.MissionType;
import com.team.peektime_admin.global.common.enums.SpaceType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/missions")
@RequiredArgsConstructor
public class MissionPageController {

    private final MissionRepository missionRepository;
    private final SolarTermRepository solarTermRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;

    @GetMapping
    public String missionPool(
            @RequestParam(required = false) String spaceType,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) String enjoyType,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Enum 변환
        SpaceType spaceTypeEnum = parseEnum(spaceType, SpaceType.class);
        CategoryType categoryTypeEnum = parseEnum(categoryType, CategoryType.class);
        EnjoyType enjoyTypeEnum = parseEnum(enjoyType, EnjoyType.class);

        Page<Mission> missionPage = missionRepository.findAllWithFilters(
                spaceTypeEnum, categoryTypeEnum, enjoyTypeEnum, pageable);
        List<Mission> missions = missionPage.getContent();

        // 배정 상태 조회
        List<Long> missionIds = missions.stream().map(Mission::getId).toList();
        Map<Long, MissionType> assignmentMap = getAssignmentMap(missionIds);

        model.addAttribute("missions", missions);
        model.addAttribute("assignmentMap", assignmentMap);
        model.addAttribute("totalCount", missionPage.getTotalElements());
        model.addAttribute("totalPages", missionPage.getTotalPages());
        model.addAttribute("currentPage", page);

        model.addAttribute("spaceType", spaceType);
        model.addAttribute("categoryType", categoryType);
        model.addAttribute("enjoyType", enjoyType);

        model.addAttribute("solarTerms", solarTermRepository.findAll());
        model.addAttribute("userTypes", UserType.values());
        model.addAttribute("enjoyTypes", EnjoyType.values());
        model.addAttribute("menu", "mission-pool");

        return "mission/pool";
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<Long, MissionType> getAssignmentMap(List<Long> missionIds) {
        Map<Long, MissionType> map = new HashMap<>();

        // 오늘의 미션 배정 확인
        List<DailyMission> dailyMissions = dailyMissionRepository.findByMissionIdIn(missionIds);
        Set<Long> dailyMissionIds = dailyMissions.stream()
                .map(dm -> dm.getMission().getId())
                .collect(Collectors.toSet());

        // 추천 미션 배정 확인
        List<RecommendedMissionPool> recommendedMissions = recommendedMissionPoolRepository.findByMissionIdIn(missionIds);
        Set<Long> recommendedMissionIds = recommendedMissions.stream()
                .map(rm -> rm.getMission().getId())
                .collect(Collectors.toSet());

        for (Long id : missionIds) {
            if (dailyMissionIds.contains(id)) {
                map.put(id, MissionType.DAILY);
            } else if (recommendedMissionIds.contains(id)) {
                map.put(id, MissionType.RECOMMENDED);
            }
        }

        return map;
    }
}
