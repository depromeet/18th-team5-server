package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.dto.BulkDailyMissionRequest;
import com.team.peektime_admin.domain.mission.dto.BulkDeleteRequest;
import com.team.peektime_admin.domain.mission.dto.BulkRecommendedMissionRequest;
import com.team.peektime_admin.domain.mission.service.MissionBulkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
public class MissionBulkController {

    private final MissionBulkService missionBulkService;

    @PostMapping("/bulk/daily")
    public ResponseEntity<Map<String, String>> moveToDailyMission(
            @RequestBody BulkDailyMissionRequest request) {
        missionBulkService.moveToDailyMission(request.getMissionIds(), request.getSolarTermId());
        return ResponseEntity.ok(Map.of("message", "오늘의미션으로 이동되었습니다."));
    }

    @PostMapping("/bulk/recommended")
    public ResponseEntity<Map<String, String>> moveToRecommendedMission(
            @RequestBody BulkRecommendedMissionRequest request) {
        missionBulkService.moveToRecommendedMission(
                request.getMissionIds(), request.getSolarTermId(), request.getUserType());
        return ResponseEntity.ok(Map.of("message", "추천미션으로 이동되었습니다."));
    }

    @PostMapping("/bulk/delete")
    public ResponseEntity<Map<String, String>> bulkDelete(
            @RequestBody BulkDeleteRequest request) {
        missionBulkService.bulkDelete(request.getMissionIds());
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable Long id) {
        missionBulkService.deleteMission(id);
        return ResponseEntity.ok().build();
    }
}