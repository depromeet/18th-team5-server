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
            // 상태코드 무관 전부 재시도 대상 — 수신 측 멱등키 dedup 전제.
            // 상한 초과 시 폴러가 FAILED로 승격하므로 무한 재시도는 없다
            log.warn("미션 로그 전송 실패 (재시도): idempotencyKey={}, status={}",
                    payload.idempotencyKey(), e.getStatusCode().value());
            return new SendResult.Failure(eventId, "HTTP " + e.getStatusCode().value());

        } catch (Exception e) {
            // 타임아웃, 연결 오류 등 — 전송 여부 불명이어도 dedup 덕에 재시도 안전
            log.warn("미션 로그 전송 실패 (재시도): idempotencyKey={}, error={}",
                    payload.idempotencyKey(), e.getMessage());
            return new SendResult.Failure(eventId, e.getMessage());
        }
    }
}