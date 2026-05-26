package com.team.peektime_admin.domain.sync.controller;

import com.team.peektime_admin.domain.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/missions")
    public ResponseEntity<Map<String, String>> syncMissions() {
        log.info("[SyncController] 미션 동기화 요청 시작");
        syncService.syncAllMissions();
        log.info("[SyncController] 미션 동기화 요청 완료");
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