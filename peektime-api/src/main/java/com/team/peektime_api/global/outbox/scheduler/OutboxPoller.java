package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)  // 1분마다
    @Transactional
    public void pollAndProcess() {
        // 생성된 지 3초 이상 된 것만 조회 (즉시 처리 중인 것 제외)
        // SKIP LOCKED: 다른 서버가 락 건 행은 건너뜀 → 분산 환경 중복 방지
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        List<OutboxEvent> events = outboxRepository.findByCreatedAtBeforeWithSkipLocked(threshold);

        if (events.isEmpty()) {
            log.info("기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("Outbox 폴링: {}건 처리 시작", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            adminClient.sendMissionLog(payload);
            outboxRepository.delete(event);

            log.info("Outbox 재시도 성공: id={}", event.getId());
        } catch (Exception e) {
            log.error("Outbox 재시도 실패: id={}, error={}", event.getId(), e.getMessage());
        }
    }



//    @Scheduled(fixedDelay = 60000)
//    public void pollAndProcess() { // @Tx 존재하지 않음
//
//        // 1. [트랜잭션 1] OutBox 테이블에 존재하는 row 가져옴
//        List<OutboxEvent> events = outboxService.조회();
//
//        if (events.isEmpty()) {
//            return;
//        }
//
//        // 2. 외부 API 호출 및 후처리는 트랜잭션 없이(혹은 건별로 따로) 진행합니다.
//        for (OutboxEvent event : events) {
//            processEvent(event);
//        }
//    }
//
//    private void processEvent(OutboxEvent event) {
//        try {
//            MissionLogPayload payload = objectMapper.readValue(event.getPayload(), MissionLogPayload.class);
//
//            // 트랜잭션 밖에서 외부 API 호출 (시간이 오래 걸려도 DB 커넥션을 붙잡지 않음!)
//            adminClient.sendMissionLog(payload);
//
//            // 3. [트랜잭션 2] 성공 시 삭제 작업만 별도 트랜잭션으로 처리
//            outboxService.deleteEvent(event.getId());
//
//            log.info("Outbox 재시도 성공: id={}", event.getId());
//        } catch (Exception e) {
//            log.error("Outbox 재시도 실패: id={}, error={}", event.getId(), e.getMessage());
//        }
//    }

}