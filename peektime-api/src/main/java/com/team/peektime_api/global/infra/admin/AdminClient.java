package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminApiResponse;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
            log.info("미션 로그 전송 성공: idempotencyKey={}, message={}", payload.idempotencyKey(), response.message());
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

            log.info("미션 로그 전송 성공: idempotencyKey={}", payload.idempotencyKey());
            return new SendResult.Success(eventId);

        } catch (HttpClientErrorException.Conflict e) {
            // 409 Conflict: 동시에 같은 요청이 처리 중 → 일시 실패 (재시도 필요)
            log.info("미션 로그 처리 중 (409 Conflict): idempotencyKey={}", payload.idempotencyKey());
            return new SendResult.TransientFailure(eventId, "409 Conflict: 처리 중");

        } catch (HttpClientErrorException e) {
            // 4xx (409 제외): 클라이언트 에러 → 영구 실패 (재시도 무의미)
            log.warn("미션 로그 클라이언트 에러 (영구 실패): idempotencyKey={}, status={}",
                    payload.idempotencyKey(), e.getStatusCode());
            return new SendResult.PermanentFailure(eventId, "4xx: " + e.getStatusCode());

        } catch (HttpServerErrorException e) {
            // 5xx: 서버 에러 → 일시 실패 (재시도 가능)
            log.warn("미션 로그 서버 에러 (일시 실패): idempotencyKey={}, status={}",
                    payload.idempotencyKey(), e.getStatusCode());
            return new SendResult.TransientFailure(eventId, "5xx: " + e.getStatusCode());

        } catch (Exception e) {
            // 네트워크 오류, 타임아웃 등 → 일시 실패 (재시도 가능)
            log.error("미션 로그 전송 실패 (일시 실패): {}", e.getMessage());
            return new SendResult.TransientFailure(eventId, e.getMessage());
        }
    }
}
