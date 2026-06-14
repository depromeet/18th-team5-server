package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.dto.MissionGenerationRequest;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationResponse;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationResult;
import com.team.peektime_admin.domain.mission.service.MissionGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
public class MissionGenerationController {

    private final MissionGenerationService missionGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<MissionGenerationResponse> generateMissions(
            @RequestBody MissionGenerationRequest request) {

        MissionGenerationResult result;

        if (request.getSolarTermId() != null && request.getUserType() != null && request.getEnjoyType() != null) {
            result = missionGenerationService.generateMissionsWithSolarTermAndUserTypeAndEnjoyType(
                    request.getSolarTermId(), request.getUserType(), request.getEnjoyType(), request.getCount());
        } else if (request.getSolarTermId() != null && request.getUserType() != null) {
            result = missionGenerationService.generateMissionsWithSolarTermAndUserType(
                    request.getSolarTermId(), request.getUserType(), request.getCount());
        } else if (request.getSolarTermId() != null) {
            result = missionGenerationService.generateMissionsWithSolarTerm(
                    request.getSolarTermId(), request.getCount());
        } else if (request.getTheme() != null && !request.getTheme().isBlank()) {
            result = missionGenerationService.generateMissionsWithTheme(
                    request.getTheme(), request.getCount());
        } else {
            result = missionGenerationService.generateMissions(request.getCount());
        }

        return ResponseEntity.ok(MissionGenerationResponse.of(result));
    }
}
