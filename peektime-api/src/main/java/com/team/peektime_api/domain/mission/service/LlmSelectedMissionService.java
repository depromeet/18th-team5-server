package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.SelectedMissionRequest;
import com.team.peektime_api.domain.mission.dto.SelectedMissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * LLM 선택 미션 생성의 비동기 경계.
 * 생성 플로우(분산 락 포함)는 LlmSelectedMissionGenerator가 담당한다.
 * @Async와 @DistributedLock을 같은 메서드에 두면 프록시 적용 순서에 따라
 * 락이 future 반환 직후 풀릴 수 있어 별도 빈으로 분리했다.
 */
@Service
@RequiredArgsConstructor
public class LlmSelectedMissionService {

    private final LlmSelectedMissionGenerator llmSelectedMissionGenerator;

    // @Async 프록시가 메서드 전체를 llmExecutor(요청당 가상 스레드)에서 실행하고
    // CompletableFuture 반환으로 톰캣 워커는 즉시 반납된다.
    // 메서드가 리턴하는 시점엔 결과가 이미 있으므로 completedFuture로 감싼다
    @Async("llmExecutor")
    public CompletableFuture<SelectedMissionResponse> generateSelectedMission(Long userId, SelectedMissionRequest filter) {
        return CompletableFuture.completedFuture(
                SelectedMissionResponse.from(llmSelectedMissionGenerator.generate(userId, filter))
        );
    }
}