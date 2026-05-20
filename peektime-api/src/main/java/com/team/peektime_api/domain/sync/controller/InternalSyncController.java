package com.team.peektime_api.domain.sync.controller;

import com.team.peektime_api.domain.sync.dto.DailyMissionSyncDto;
import com.team.peektime_api.domain.sync.dto.MissionSyncDto;
import com.team.peektime_api.domain.sync.dto.RecommendedMissionSyncDto;
import com.team.peektime_api.domain.sync.service.DailyMissionSyncService;
import com.team.peektime_api.domain.sync.service.MissionSyncService;
import com.team.peektime_api.domain.sync.service.RecommendedMissionSyncService;
import com.team.peektime_api.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/sync")
@RequiredArgsConstructor
public class InternalSyncController {

    private final MissionSyncService missionSyncService;
    private final DailyMissionSyncService dailyMissionSyncService;
    private final RecommendedMissionSyncService recommendedMissionSyncService;

    @PostMapping("/missions")
    public ResponseEntity<SuccessResponse<Void>> syncMissions(
            @RequestBody List<MissionSyncDto> missions) {
        missionSyncService.syncMissions(missions);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @PostMapping("/daily-missions")
    public ResponseEntity<SuccessResponse<Void>> syncDailyMissions(
            @RequestParam Long solarTermId,
            @RequestBody List<DailyMissionSyncDto> dailyMissions) {
        dailyMissionSyncService.syncDailyMissions(solarTermId, dailyMissions);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @PostMapping("/recommended-missions")
    public ResponseEntity<SuccessResponse<Void>> syncRecommendedMissions(
            @RequestParam Long solarTermId,
            @RequestBody List<RecommendedMissionSyncDto> recommendedMissions) {
        recommendedMissionSyncService.syncRecommendedMissions(solarTermId, recommendedMissions);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}