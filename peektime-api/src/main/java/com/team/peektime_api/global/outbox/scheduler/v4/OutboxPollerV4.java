package com.team.peektime_api.global.outbox.scheduler.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
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
 * - 실패는 성공/실패 2분류로 전부 재시도: 이 도메인엔 비즈니스 거절이 없고 수신 측이 멱등해서
 *   어떤 실패든 대응이 "재시도" 하나로 수렴 (SendResult 주석 참조)
 * - 실패 시 next_retry_at 백오프: 재시도 폭풍 방지 + 실패 건의 배치 슬롯 독점(starvation) 방지
 * - 재시도 상한(15회) 초과 시 FAILED 승격: payload 보존한 채 자동 재시도만 정지, 운영자 호출
 *
 * 주기 5분 근거: 해피 패스는 리스너가 커밋 직후 즉시 전송하므로 폴러는 실패 수거 전용이고,
 * 최종 마감이 하루 단위 집계(랭킹/보상)라 실패 건 재시도가 분 단위면 충분하다.
 *
 * 배수 루프: Admin 장애 시 리스너 전송이 전부 실패해 이벤트가 폴러로 쏟아지므로
 * (초당 1건 가정 시 5분에 최대 300건), 한 사이클 안에서 빌 때까지 배치 단위로 반복 처리한다.
 * 루프는 반드시 종료한다 — 매 배치의 모든 row가 SENT/FAILED(claim 대상 아님) 또는
 * READY+미래 next_retry_at(claim 조건 미달) 중 하나로 전이되기 때문.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV4 {

    private final OutboxV4TransactionManager transactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 300_000)
    public void pollAndProcess() {
        int processedCount = 0;

        while (true) {
            // Tx1: claim (짧은 트랜잭션)
            List<OutboxEvent> events = transactionManager.claimBatch();
            if (events.isEmpty()) {
                break;
            }

            // 트랜잭션 없음: 외부 API 호출 구간
            List<SendResult> results = events.stream()
                    .map(this::processEvent)
                    .toList();

            // Tx2: 결과 반영 (짧은 트랜잭션)
            transactionManager.applyResults(results);
            processedCount += events.size();
        }

        if (processedCount > 0) {
            log.info("[V4] Outbox 폴링 처리 완료: {}건", processedCount);
        }
    }

    private SendResult processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLog(payload, event.getId());

        } catch (Exception e) {
            // 파싱 실패도 일반 실패로 취급 — 재시도가 무의미하지만 상한이 FAILED로 정리해준다
            return new SendResult.Failure(event.getId(), "payload 파싱 실패: " + e.getMessage());
        }
    }
}
