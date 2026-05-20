package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminApiResponse;
=======
import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import com.team.peektime_api.global.infra.admin.dto.AdminRecommendedMissionResponse;
>>>>>>> af89dd1 (feat: 추천 미션 조회 API 구현)
    public void sendMissionLog(MissionLogPayload payload) {
        try {
            AdminApiResponse response = adminRestClient.post()
                    .uri("/api/stats/mission-log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AdminApiResponse.class);
            log.info("미션 로그 전송 성공: missionId={}, message={}", payload.missionId(), response.message());
        } catch (Exception e) {
            log.error("미션 로그 전송 실패: {}", e.getMessage());
            throw new RuntimeException("미션 로그 전송 실패", e);
        }
    }
}
