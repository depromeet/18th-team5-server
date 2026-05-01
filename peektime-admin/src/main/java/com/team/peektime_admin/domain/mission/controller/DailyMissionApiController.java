package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.entity.DailyMission;
import com.team.peektime_admin.domain.mission.repository.DailyMissionRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/daily-missions")
@RequiredArgsConstructor
public class DailyMissionApiController {

    private final DailyMissionRepository dailyMissionRepository;

    /**
     * 배정대기 미션을 특정 날짜에 배정
     * date가 설정됨 (NULL → 실제 날짜)
     */
    @PostMapping("/{id}/assign")
    @Transactional
    public ResponseEntity<Map<String, String>> assignToDate(
            @PathVariable Long id,
            @RequestBody AssignRequest request) {

        DailyMission dailyMission = dailyMissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DailyMission not found: " + id));

        // 해당 날짜에 이미 배정된 미션이 있는지 확인
        dailyMissionRepository.findBySolarTermIdAndMissionDateIsNotNull(dailyMission.getSolarTerm().getId())
                .stream()
                .filter(dm -> request.getDate().equals(dm.getMissionDate()))
                .findFirst()
                .ifPresent(existing -> {
                    // 기존 미션을 배정대기로 이동
                    existing.unassign();
                });

        dailyMission.assignToDate(request.getDate(), 1);

        return ResponseEntity.ok(Map.of("message", "배정되었습니다."));
    }

    /**
     * 날짜에 배정된 미션을 배정대기로 이동
     * date가 NULL로 설정됨
     */
    @PostMapping("/{id}/unassign")
    @Transactional
    public ResponseEntity<Map<String, String>> unassignFromDate(@PathVariable Long id) {

        DailyMission dailyMission = dailyMissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DailyMission not found: " + id));

        dailyMission.unassign();

        return ResponseEntity.ok(Map.of("message", "배정 해제되었습니다."));
    }

    /**
     * 배정대기 미션을 풀로 이동 (DailyMission 레코드 삭제)
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> removeFromDaily(@PathVariable Long id) {
        dailyMissionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Getter
    @NoArgsConstructor
    public static class AssignRequest {
        private LocalDate date;
    }
}