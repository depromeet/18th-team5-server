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
            int status = e.getStatusCode().value();

            // 4xx = 요청 자체의 결정론적 결함 (Admin은 계약상 결정론적 결함에만 4xx를 응답).
            // 재시도해도 같은 응답이 보장되므로 백오프를 태우지 않고 즉시 FAILED로 보낸다.
            // 단 429만 예외 — 상태코드는 4xx지만 의미가 "나중에 다시"인 일시 상태라 재시도 쪽.
            // 지금 Admin엔 rate limit이 없지만, 앞단에 프록시/게이트웨이가 붙는 순간을 대비한 방어
            if (e.getStatusCode().is4xxClientError() && status != 429) {
                log.error("미션 로그 전송 거절 (영구 실패): idempotencyKey={}, status={}",
                        payload.idempotencyKey(), status);
                return new SendResult.PermanentFailure(eventId, "HTTP " + status);
            }

            // 5xx = 수신 측 일시 장애 — 백오프 재시도, 상한 초과 시 폴러가 FAILED로 승격
            log.warn("미션 로그 전송 실패 (재시도): idempotencyKey={}, status={}",
                    payload.idempotencyKey(), status);
            return new SendResult.RetryableFailure(eventId, "HTTP " + status);

        } catch (Exception e) {
            // 타임아웃, 연결 오류 등 — 전송 여부 불명이어도 dedup 덕에 재시도 안전
            log.warn("미션 로그 전송 실패 (재시도): idempotencyKey={}, error={}",
                    payload.idempotencyKey(), e.getMessage());
            return new SendResult.RetryableFailure(eventId, e.getMessage());
        }
    }
}