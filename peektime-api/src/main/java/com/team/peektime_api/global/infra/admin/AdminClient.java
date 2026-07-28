package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminApiResponse;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    private final RestClient adminRestClient;

    public SendResult sendMissionLog(MissionLogPayload payload, Long eventId) {
        log.info("미션 로그 전송 시도: payload={}", payload);
        try {
            adminRestClient.post()
                    .uri("/api/stats/mission-log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AdminApiResponse.class);

            // Admin은 신규 저장·멱등 중복 모두 2xx로 응답하므로 동일하게 성공 취급
            log.info("미션 로그 전송 성공: idempotencyKey={}", payload.idempotencyKey());
            return new SendResult.Success(eventId);

        } catch (HttpStatusCodeException e) {
            return classifyHttpFailure(e, payload, eventId);

        } catch (Exception e) {
            // 타임아웃, 연결 오류 등 — 전송 여부 불명. Admin 멱등키 dedup 전제로 재시도 안전
            log.warn("미션 로그 전송 실패 (일시 실패, 재시도): idempotencyKey={}, error={}",
                    payload.idempotencyKey(), e.getMessage());
            return new SendResult.TransientFailure(eventId, e.getMessage());
        }
    }

    /**
     * 실패 분류 기준은 "재시도하면 성공할 수 있는가"가 아니라 "실패의 원인이 어디에 있는가":
     * - 시간이 해결하는 실패(5xx, 408, 429, 409) → 일시 실패, 백오프 재시도
     * - payload 문제(400) 또는 환경 문제(401/403/404 등) → 영구 실패.
     *   자동 재시도는 무의미하지만 payload를 지우면 안 되므로 폴러가 FAILED로 보존한다
     */
    private SendResult classifyHttpFailure(HttpStatusCodeException e, MissionLogPayload payload, Long eventId) {
        int status = e.getStatusCode().value();

        boolean retryable = e.getStatusCode().is5xxServerError()
                || status == 408   // Request Timeout
                || status == 429   // Too Many Requests
                || status == 409;  // Conflict (경합)

        if (retryable) {
            log.warn("미션 로그 일시 실패 (재시도): idempotencyKey={}, status={}",
                    payload.idempotencyKey(), status);
            return new SendResult.TransientFailure(eventId, "HTTP " + status);
        }

        log.error("미션 로그 영구 실패 (FAILED 대상): idempotencyKey={}, status={}",
                payload.idempotencyKey(), status);
        return new SendResult.PermanentFailure(eventId, "HTTP " + status);
    }
}