package com.team.peektime_api.global.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.infra.admin.AdminClient;
import com.team.peektime_api.global.outbox.SendResult;
import com.team.peektime_api.global.outbox.SendResult.PermanentFailure;
import com.team.peektime_api.global.outbox.SendResult.Success;
import com.team.peektime_api.global.outbox.SendResult.TransientFailure;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OutboxPoller V2 - 트랜잭션 분리 버전
 *
 * V1과의 차이점:
 * - V1: 전체가 하나의 트랜잭션 (조회 + 외부 API 호출 + 삭제)
 * - V2: 트랜잭션 분리 (Tx1: 조회+상태변경, 외부 API 호출, Tx2: 삭제)
 *
 * 장점:
 * - DB 커넥션 점유 시간 최소화
 * - 외부 API 호출 중 락을 잡지 않음
 * - 상태 기반으로 중복 처리 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollerV2 {

    private final OutboxTransactionManager transactionManager;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * 미션 수행 요청이 평균,초당 1개라고 가정
     * Admin 서버가 죽을 경우,최악의 상황 -> 1분 동안 최대 60개 쌓일수 있음
     * 인스턴스 개수 2개 -> 30개 정도 Batch_Size 잡기 ( 30 개 + 30 개)
     *
     * Admin 서버 timeout 3초
     * Admin 서버 장애시, outbox 에 존재하는 row 30개를 처리하는 동안 최악의 상황 : 90초 소요
     * 커넥션 소유시간 최대 90초 <---- 스케줄링 시간(60초)보다 더 길다.
     * ----> 즉 Admin 서버가 죽으면, 죽어있는 동안 계속해서 커넥션을 소요하는 상황
     * ----> 스케줄링 소요시간을 90초 보다는 길게 잡는것이 좋다.(Admin 서버가 죽을경우 불필요한 스케줄링이 발생가능)
     *
     *
     *  - 스케줄링 주기 : 60초
     *  - 하나의 트랜잭션으로 처리할 경우, skip lock 사용
     *  - timeout 3초
     *  - Batch_Size : 2개의 인스턴스 30개씩
     *  - Admin 이 죽었지만 초당 요청이 1개인 상황, Outbox 에 30개 정도 쌓여있다.>
     * 0 초 ----> 90 초 : 90초 동안 커넥션 소유 ----- (커넥션 1번)
     * 60초 ----> 150 초 : skip lock 을 통해 (30개 처리) -> 30 * 3 = 90초 동안 커넥션 소유 ----- (커넥션 2번)
     * 120초 -----> 210초 : skip lock 을 통해 (30개 처리) -> 30 * 3 = 90초 동안 커넥션 소유 ----- (커넥션 1번)
     *
     * 즉, 2개의 커넥션을 매우 길게 잡고 있다.
     * 커넥션 1번 : (0초 ----- 90초) + (120초 ------- 210초)
     */

    // @Scheduled(fixedDelay = 60000)  // V1과 충돌 방지를 위해 주석 처리
    public void pollAndProcess() {

        // @Tx1
        List<OutboxEvent> events = transactionManager.fetchAndMarkProcessing();

        if (events.isEmpty()) {
            log.info("[V2] 기록해야 하는 미션이 존재하지 않습니다.");
            return;
        }

        log.info("[V2] Outbox 폴링: {}건 처리 시작", events.size());

        ProcessingResult result = processAllEvents(events);

        //@Tx2
        transactionManager.handleResults(result.toDelete(), result.toRetry());
    }

    /**
     * 문제 1: Tx1 에서 Progressing 으로 전환후 commit 후 서버 다운
     * 문제 2: 외부API 성공했지만, Tx2 에서 삭제 실패
     * 문제 3: 알수없음 에러
     * ---> 모두 Progressing 으로 존재한다.
     *
     *
     * 해결책: Progressing 을 조회하는 스케줄러를 별도로 만들자.
     * 스케줄러 주기 설정:
     * 정상적으로 처리될 경우 DB에 존재하는 Progressing 은 최대 3초 * 30 이내로 존재한다.
     * updateAt 시간이 (현재 시간 - 90 초) 후면 실제 처리중일 가능성.
     * updateAt 시간이 (현재 시간 - 90 초_ 전이면 row -> @Tx2 에서 롤백되거나, @Tx1 commit 이후 서버 장애
     * -> 90초 보다 큰 100 초 마다 Progressing 스케줄링을 진행하자. ( Progressing -> Ready 로 전환하자.)
     */


    // 유지보수가 힘들다. 다른개발자가 알기가 힘들다.

    private ProcessingResult processAllEvents(List<OutboxEvent> events) {
        List<Long> toDelete = new ArrayList<>();
        List<Long> toRetry = new ArrayList<>();

        for (OutboxEvent event : events) {
            SendResult result = processEvent(event);

            if (result instanceof Success s) {
                toDelete.add(s.eventId());
            } else if (result instanceof PermanentFailure pf) {
                toDelete.add(pf.eventId());
                log.warn("[V2] Outbox 영구 실패 (삭제): id={}, reason={}", pf.eventId(), pf.reason());
            } else if (result instanceof TransientFailure tf) {
                toRetry.add(tf.eventId());
                log.warn("[V2] Outbox 일시 실패 (재시도 대기): id={}, reason={}", tf.eventId(), tf.reason());
            }
        }

        return new ProcessingResult(toDelete, toRetry);
    }

    private SendResult processEvent(OutboxEvent event) {
        try {
            MissionLogPayload payload = objectMapper.readValue(
                    event.getPayload(), MissionLogPayload.class);

            return adminClient.sendMissionLog(payload, event.getId());

        } catch (Exception e) {
            // 파싱 실패 등은 영구 실패로 처리
            return new PermanentFailure(event.getId(), e.getMessage());
        }
    }

    private record ProcessingResult(List<Long> toDelete, List<Long> toRetry) {}
}

/**
 *
 * 실패 건은 Redis String 자료구조에 저장(@Sync 안에서 실패할 경우 )
 * 시간 순서대로 e, e, e, e, e 저장
 * OutBox 에도 그래도 존재할것임(실패건)
 *
 * @TX(x)
 * 30 개씩 조회후 가져오기 LEFT 30개 제거( 2개 인스턴스에서 )
 * HTTP CALL
 * @TX
 * 성공 -> Redis에서 가져온 30개 Outbox 제거
 * Fail -> 마찬가지로 Outbox에서 제거
 * 알 수 없는 에러 -> 다시 LEFT 에 insert
 *
 */