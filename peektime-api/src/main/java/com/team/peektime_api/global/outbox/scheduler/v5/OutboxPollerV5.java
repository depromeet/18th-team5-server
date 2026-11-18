package com.team.peektime_api.global.outbox.scheduler.v5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.SendResult.PermanentFailure;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OutboxPoller V5 — PROCESSING 가시화 + 리스 복구 스위퍼 분리 버전
 *
 * V4(리스 전용)에서 바뀐 것:
 * - claim 시 PROCESSING 전이: "전송 중"이 상태로 보인다. V4는 READY인 채 next_retry_at만
 *   미래로 밀려 백오프 대기와 전송 중이 데이터로 구분 불가였다
 * - 리스가 전용 컬럼(lease_until)로 분리: next_retry_at은 순수 백오프 예약으로 의미 단일화
 * - 고아(리스 만료 PROCESSING) 복구가 claim 쿼리가 아니라 전용 스위퍼
 *   (OutboxLeaseRecoveryScheduler)의 책임: 복구 발생이 WARN 로그/건수로 관측되고,
 *   복구 반복(크래시 루프)은 recovery_count 상한으로 FAILED 승격해 차단한다
 *
 * V2 스위퍼와의 결정적 차이: 복구 판정이 updatedAt 추정("90초 지났으면 죽은 듯")이 아니라
 * claim이 직접 적어둔 명시적 마감(lease_until) 만료다.
 *
 * 주기 1분 + 호출당 한 배치(30건), fixedDelay로 호출 겹침 없음, 리스너가 해피 패스를
 * 담당하고 폴러는 실패 수거 전용 — 이 운영 파라미터와 근거는 V4와 동일
 * (OutboxPollerV4, OutboxEvent 상수 주석 참조).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV5 {

    private final OutboxV5TransactionManager transactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    // @SchedulerLock 의도적 미적용 — 동시 claim은 SKIP LOCKED + 상태/리스가 막고 있고,
    // 다중 인스턴스의 병렬 폴링은 적체 해소 처리량을 높이는 의도된 동작이다
    @Scheduled(fixedDelay = 60_000)
    public void pollAndProcess() {
        // Tx1: claim (짧은 트랜잭션 — PROCESSING 전이 + 리스 기록)
        List<OutboxEvent> events = transactionManager.claimBatch();
        if (events.isEmpty()) {
            return;
        }

        // 트랜잭션 없음: 외부 API 호출 구간
        List<SendResult> results = events.stream()
                .map(this::processEvent)
                .toList();

        // Tx2: 결과 반영 (짧은 트랜잭션)
        transactionManager.applyResults(results);
        log.info("[V5] Outbox 폴링 처리 완료: {}건", events.size());
    }

    private SendResult processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLog(payload, event.getId());

        } catch (Exception e) {
            // 파싱 실패는 로컬에서 결정론적 — 재시도가 무의미하므로 즉시 FAILED
            return new PermanentFailure(event.getId(), "payload 파싱 실패: " + e.getMessage());
        }
    }
}