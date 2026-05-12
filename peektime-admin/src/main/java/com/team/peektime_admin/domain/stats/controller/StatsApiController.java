package com.team.peektime_admin.domain.stats.controller;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsApiController {

    private final StatsService statsService;

    @PostMapping("/mission-log")
    public ResponseEntity<Void> saveMissionLog(@RequestBody MissionLogRequest request) {
        statsService.saveMissionLog(request);
        return ResponseEntity.ok().build();
    }
}