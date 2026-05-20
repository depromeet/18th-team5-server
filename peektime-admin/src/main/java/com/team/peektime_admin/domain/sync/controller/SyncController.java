package com.team.peektime_admin.domain.sync.controller;

import com.team.peektime_admin.domain.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/missions")
    public ResponseEntity<Map<String, String>> syncMissions() {
        syncService.syncAllMissions();
        return ResponseEntity.ok(Map.of("message", "미션 동기화 완료"));
    }

    @PostMapping("/daily-missions/{solarTermId}")
    public ResponseEntity<Map<String, String>> syncDailyMissions(@PathVariable Long solarTermId) {
        syncService.syncDailyMissions(solarTermId);
        return ResponseEntity.ok(Map.of("message", "오늘의 미션 동기화 완료"));
    }

    @PostMapping("/recommended-missions/{solarTermId}")
    public ResponseEntity<Map<String, String>> syncRecommendedMissions(@PathVariable Long solarTermId) {
        syncService.syncRecommendedMissions(solarTermId);
        return ResponseEntity.ok(Map.of("message", "추천 미션 동기화 완료"));
    }
}