package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminApiResponse;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    private final RestClient adminRestClient;

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

    public SendResult sendMissionLogWithResult(MissionLogPayload payload, Long eventId) {
        try {
            AdminApiResponse response = adminRestClient.post()
                    .uri("/api/stats/mission-log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AdminApiResponse.class);

            log.info("미션 로그 전송 성공: missionId={}", payload.missionId());
            return new SendResult.Success(eventId);

        } catch (HttpClientErrorException e) {
            // 4xx 에러는 재시도해도 결과가 바뀌지 않음 (클라이언트 요청 문제)
            log.warn("미션 로그 클라이언트 에러: missionId={}, status={}",
                    payload.missionId(), e.getStatusCode());
            return new SendResult.AlreadyProcessed(eventId);
        } catch (Exception e) {
            log.error("미션 로그 전송 실패: {}", e.getMessage());
            return new SendResult.Unknown(eventId, e.getMessage());
        }
    }
}
