package com.team.peektime_api.global.outbox.scheduler.v4;

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
 * OutboxPoller V4 — 상태 머신 + 백오프 재시도 버전
 *
 * V2(트랜잭션 분리)에서 추가된 것:
 * - 성공 시 삭제 대신 SENT 마킹: 대사(reconciliation) 대비 전송 이력 보존
 * - 실패는 일시/영구 2분류: 일시(5xx·타임아웃)는 백오프 재시도, 영구(4xx·파싱 불가)는
 *   재시도 없이 즉시 FAILED — 분류 계약은 SendResult 주석 참조
 * - 일시 실패 시 next_retry_at 백오프: 재시도 폭풍 방지 + 실패 건의 배치 슬롯 독점(starvation) 방지
 * - 재시도 상한(3회, 총 30분 커버) 초과 시 FAILED 승격: payload 보존한 채 자동 재시도만 정지,
 *   이후는 FAILED 알림 → 운영자 수동 재전송이 복구 경로 (알림 인프라 전제)
 *
 * 주기 1분 + 호출당 한 배치(30건): 적체 해소는 호출 내 루프가 아니라 스케줄러 반복이 담당한다
 * (분당 30건 — 장애 후 적체 300건도 10분 내 배수). 해피 패스는 리스너가 커밋 직후 즉시
 * 전송하므로 폴러는 실패 수거 전용이고, 리스너가 실패한 건의 첫 재시도는 최대 1분 내다.
 *
 * fixedDelay라 호출이 길어져도(장애 시 배치 30건 × 타임아웃 5초 = 최대 150초) 다음 호출과
 * 겹치지 않는다. 다중 인스턴스 간의 동시 claim은 SKIP LOCKED(Tx1 겹침)과 claim 리스
 * (Tx1 커밋 이후, OutboxEvent.extendLease)가 막는다. 백오프(5/10/15분)는 폴러 주기가 아니라 자동 복구 커버리지(총 30분)에서
 * 역산된 값이므로 주기와 독립 — 주기를 바꿔도 백오프는 재계산 대상이 아니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV4 {

    private final OutboxV4TransactionManager transactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    // @SchedulerLock 의도적 미적용 — 동시 claim은 SKIP LOCKED + claim 리스가 이미 막고 있고(클래스 주석),
    // 다중 인스턴스의 병렬 폴링은 적체 해소 처리량을 높이는 의도된 동작이다
    // @Scheduled(fixedDelay = 60_000)  // V5(PROCESSING 가시화 + 리스 복구 스위퍼 분리) 전환으로 비활성
    public void pollAndProcess() {
        // Tx1: claim (짧은 트랜잭션)
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
        log.info("[V4] Outbox 폴링 처리 완료: {}건", events.size());
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
