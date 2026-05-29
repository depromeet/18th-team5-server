package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.home.dto.RecentRecordCache;
import com.team.peektime_api.domain.mission.dto.MissionRecordPageResponse;
import com.team.peektime_api.domain.mission.dto.RecommendedMissionCountResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionDetailResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.global.common.enums.MissionType;
import com.team.peektime_api.domain.mission.entity.DailyMission;
import com.team.peektime_api.domain.mission.entity.Mission;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.event.MissionCompletedEvent;
import com.team.peektime_api.domain.mission.event.MissionLogPayload;
import com.team.peektime_api.domain.mission.repository.DailyMissionRepository;
import com.team.peektime_api.domain.mission.repository.MissionRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.solarterm.entity.SolarTerm;
import com.team.peektime_api.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.S3Service;
import com.team.peektime_api.global.infra.cache.RecentRecordsCacheRepository;
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMissionCompletionService {

    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final MissionRepository missionRepository;
    private final SolarTermRepository solarTermRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RecentRecordsCacheRepository recentRecordsCacheRepository;

    @Transactional
    public UserMissionCompletionResponse completeDailyMission(Long userId, Long missionId, UserMissionCompletionRequest request) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now();

        DailyMission dailyMission = getDailyMission(missionId, today);
        Mission mission = dailyMission.getMission();

        validateSameMission(missionId, user);

        SolarTerm solarTerm = solarTermRepository.findById(request.solarTermId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));

        UserMissionCompletion completion = saveMissionCompletion(request, user, mission, solarTerm);

        updateRecentRecordsCache(userId, completion);

        dailyMissionRepository.incrementParticipantCount(dailyMission.getId());

        MissionLogPayload payload = MissionLogPayload.of(
                user.getDeviceUuid(),
                missionId,
                request.missionType(),
                solarTerm.getId(),
                completion.getCreatedAt()
        );
        OutboxEvent outbox = outboxRepository.save(new OutboxEvent(toJson(payload)));
        eventPublisher.publishEvent(MissionCompletedEvent.from(outbox, payload));

        return UserMissionCompletionResponse.from(completion);
    }

    private void validateSameMission(Long missionId, User user) {
        if (userMissionCompletionRepository.existsByUser_IdAndMission_Id(user.getId(), missionId)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }
    }

    private DailyMission getDailyMission(Long missionId, LocalDate today) {
        return dailyMissionRepository
                .findByMission_IdAndMissionDate(missionId, today)
                .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_MISSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<UserMissionCompletionDetailResponse> getMissionCompletions(Long userId, Long missionId) {
        return userMissionCompletionRepository.findByUser_IdAndMission_Id(userId, missionId)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionRecordPageResponse getMissionRecordPage(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));
        return MissionRecordPageResponse.from(mission);
    }

    @Transactional(readOnly = true)
    public RecommendedMissionCountResponse getRecommendedMissionCount(Long userId) {
        long count = userMissionCompletionRepository.countByUser_IdAndMissionType(userId, MissionType.RECOMMENDED);
        return RecommendedMissionCountResponse.of(count);
    }

    private UserMissionCompletion saveMissionCompletion(UserMissionCompletionRequest request, User user,
                                                        Mission mission, SolarTerm solarTerm) {
        return userMissionCompletionRepository.save(
                UserMissionCompletion.create(user, mission, solarTerm, request.missionType(),
                        request.objectKey(), request.memo())
        );
    }

    private void updateRecentRecordsCache(Long userId, UserMissionCompletion completion) {
        if (completion.getObjectKey() == null) {
            return;
        }
        try {
            recentRecordsCacheRepository.addRecord(userId, RecentRecordCache.from(completion));
        } catch (Exception e) {
            log.warn("Redis 캐시 업데이트 실패 (userId={}): {}", userId, e.getMessage());
        }
    }

    private String toJson(MissionLogPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

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
