package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationRequest;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationResponse;
import com.team.peektime_admin.domain.mission.service.MissionGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
public class MissionGenerationController {

    private final MissionGenerationService missionGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<MissionGenerationResponse> generateMissions(
            @RequestBody MissionGenerationRequest request) {

        List<GeneratedMissionDto> missions;

        if (request.getSolarTermId() != null && request.getUserType() != null) {
            missions = missionGenerationService.generateMissionsWithSolarTermAndUserType(
                    request.getSolarTermId(), request.getUserType(), request.getCount());
        } else if (request.getSolarTermId() != null) {
            missions = missionGenerationService.generateMissionsWithSolarTerm(
                    request.getSolarTermId(), request.getCount());
        } else if (request.getTheme() != null && !request.getTheme().isBlank()) {
            missions = missionGenerationService.generateMissionsWithTheme(
                    request.getTheme(), request.getCount());
        } else {
            missions = missionGenerationService.generateMissions(request.getCount());
        }

        return ResponseEntity.ok(MissionGenerationResponse.of(missions));
    }
}