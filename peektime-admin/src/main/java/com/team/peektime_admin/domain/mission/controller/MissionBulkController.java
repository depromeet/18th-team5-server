package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.dto.BulkDailyMissionRequest;
import com.team.peektime_admin.domain.mission.dto.BulkDeleteRequest;
import com.team.peektime_admin.domain.mission.dto.BulkRecommendedMissionRequest;
import com.team.peektime_admin.domain.mission.dto.MissionRequest;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.mission.service.MissionBulkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
public class MissionBulkController {

    private final MissionBulkService missionBulkService;
    private final MissionRepository missionRepository;

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

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createMission(@RequestBody MissionRequest request) {


        Mission mission = Mission.create(request);

        Mission saved = missionRepository.save(mission);
        return ResponseEntity.ok(Map.of(
                "message", "미션이 등록되었습니다.",
                "id", saved.getId()
        ));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> updateMission(
            @PathVariable Long id,
            @RequestBody MissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("미션을 찾을 수 없습니다: " + id));

        mission.update(
                request.getTitle(),
                request.getDescription(),
                request.getSpaceType(),
                request.getIntensityType(),
                request.getCategoryType(),
                request.getCompanionType(),
                request.getEnjoyType(),
                request.getUserType()
        );

        return ResponseEntity.ok(Map.of("message", "미션이 수정되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mission> getMission(@PathVariable Long id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("미션을 찾을 수 없습니다: " + id));
        return ResponseEntity.ok(mission);
    }
}