package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminApiResponse;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("미션 로그 이미 처리됨: missionId={}", payload.missionId());
                return new SendResult.AlreadyProcessed(eventId);
            }
            log.error("미션 로그 전송 실패: {}", e.getMessage());
            return new SendResult.Unknown(eventId, e.getMessage());

        } catch (Exception e) {
            log.error("미션 로그 전송 실패: {}", e.getMessage());
            return new SendResult.Unknown(eventId, e.getMessage());
        }
    }
}
