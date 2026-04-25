package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.global.common.enums.CategoryType;
import com.team.peektime_admin.global.common.enums.IntensityType;
import com.team.peektime_admin.global.common.enums.SpaceType;
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

@Controller
@RequestMapping("/admin/missions")
@RequiredArgsConstructor
public class MissionPageController {

    private final MissionRepository missionRepository;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String missionPool(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String spaceType,
            @RequestParam(required = false) String intensityType,
            @RequestParam(required = false) String categoryType,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Mission> missionPage = missionRepository.findAllByDeletedFalse(pageable);

        model.addAttribute("missions", missionPage.getContent());
        model.addAttribute("totalCount", missionPage.getTotalElements());
        model.addAttribute("totalPages", missionPage.getTotalPages());
        model.addAttribute("currentPage", page);

        model.addAttribute("keyword", keyword);
        model.addAttribute("spaceType", spaceType);
        model.addAttribute("intensityType", intensityType);
        model.addAttribute("categoryType", categoryType);

        model.addAttribute("solarTerms", solarTermRepository.findAll());
        model.addAttribute("menu", "mission-pool");

        return "mission/pool";
    }
}