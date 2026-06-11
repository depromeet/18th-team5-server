package com.team.peektime_admin.domain.stats.controller;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.service.StatsService;
import com.team.peektime_admin.global.response.SuccessCode;
import com.team.peektime_admin.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
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
    public SuccessResponse<Void> saveMissionLog(@RequestBody MissionLogRequest request) {
        boolean isNewLog = statsService.saveMissionLog(request);

        if (isNewLog) {
            return SuccessResponse.of(SuccessCode.MISSION_LOG_SAVED, null);
        }
        return SuccessResponse.of(SuccessCode.MISSION_LOG_ALREADY_EXISTS, null);
    }
}