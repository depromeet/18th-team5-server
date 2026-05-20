package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.dto.RecommendedMissionResponse;
import com.team.peektime_admin.domain.mission.service.RecommendedMissionService;
import com.team.peektime_admin.global.common.enums.EnjoyType;
import com.team.peektime_admin.global.common.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions/recommended")
@RequiredArgsConstructor
public class RecommendedMissionApiController {

    private final RecommendedMissionService recommendedMissionService;

    @GetMapping
    public ResponseEntity<RecommendedMissionResponse> getRecommendedMissions(
            @RequestParam UserType userType,
            @RequestParam EnjoyType enjoyTypeFirst,
            @RequestParam EnjoyType enjoyTypeSecond,
            @RequestParam EnjoyType enjoyTypeThird
    ) {
        return ResponseEntity.ok(
                recommendedMissionService.getRecommendedMissions(userType, enjoyTypeFirst, enjoyTypeSecond, enjoyTypeThird)
        );
    }
}
