package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/selected-missions")
@RequiredArgsConstructor
public class SelectedMissionPageController {

    private final MissionRepository missionRepository;

    @GetMapping
    public String selectedMission(
            @RequestParam(required = false) String spaceType,
            @RequestParam(required = false) String intensityType,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) String companionType,
            Model model
    ) {
        // 선택 미션 = 전체 미션 중 오늘의미션/추천미션에 배정되지 않은 것
        // 여기서는 간단히 전체 미션 목록을 보여줌 (실제로는 제외 로직 필요)
        List<Mission> missions = missionRepository.findAllByDeletedFalse();

        model.addAttribute("missions", missions);
        model.addAttribute("totalMissions", missions.size());

        model.addAttribute("spaceType", spaceType);
        model.addAttribute("intensityType", intensityType);
        model.addAttribute("categoryType", categoryType);
        model.addAttribute("companionType", companionType);

        model.addAttribute("menu", "selected-mission");
        return "mission/selected";
    }
}