package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionDetailResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.S3Service;
import com.team.peektime_api.domain.mission.event.MissionCompletedEvent;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMissionCompletionService {

    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserMissionCompletionResponse completeMission(Long userId, Long missionId, UserMissionCompletionRequest request) {
        User user = findUser(userId);

        if (userMissionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), missionId)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.of(user, missionId, request)
        );

        MissionLogPayload payload = createMissionLogPayload(missionId, request, user, completion);

        OutboxEvent outbox = outboxRepository.save(new OutboxEvent(toJson(payload)));
        eventPublisher.publishEvent(MissionCompletedEvent.from(outbox, payload));

        return UserMissionCompletionResponse.from(completion);
    }

    private static MissionLogPayload createMissionLogPayload(Long missionId, UserMissionCompletionRequest request, User user, UserMissionCompletion completion) {
        MissionLogPayload payload = MissionLogPayload.of(
                user.getDeviceUuid(),
                missionId,
                request.missionType(),
                request.solarTermId(),
                completion.getCompletedAt()
        );
        return payload;
    }


    private String toJson(MissionLogPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }



    @Transactional(readOnly = true)
    public List<UserMissionCompletionDetailResponse> getMissionCompletions(Long userId, Long missionId) {
        return userMissionCompletionRepository.findByUser_IdAndMissionId(userId, missionId)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    /* 완성된 미션 세부사항 조회 */
    private UserMissionCompletionDetailResponse toDetailResponse(UserMissionCompletion completion) {
        String presignedUrl = completion.getObjectKey() != null
                ? s3Service.generatePresignedViewUrl(completion.getObjectKey())
                : null;

        return UserMissionCompletionDetailResponse.of(completion, presignedUrl);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

