package com.team.peektime_api.domain.mission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.domain.mission.dto.MissionCompletionRequest;
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
import com.team.peektime_api.global.outbox.entity.OutboxEvent;
import com.team.peektime_api.global.outbox.repository.OutboxRepository;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    @Transactional
    public UserMissionCompletionResponse completeDailyMission(Long userId, Long missionId, UserMissionCompletionRequest request) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now();

        DailyMission dailyMission = getDailyMission(missionId, today);
        Mission mission = dailyMission.getMission();

        validateSameMission(missionId, user);

        SolarTerm solarTerm = getCurrentSolarTerm(today);

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.create(user, mission, solarTerm, MissionType.DAILY,
                        request.objectKey(), request.memo())
        );

        dailyMissionRepository.incrementParticipantCount(dailyMission.getId());

        OutboxEvent outbox = saveOutboxEvent(user, missionId, solarTerm, completion);

        // 팩트 이벤트 발행 (Pull 방식: ID만 전달)
        publishMissionCompletedEvent(completion.getId(), outbox.getId());

        return UserMissionCompletionResponse.from(completion);
    }

    private SolarTerm getCurrentSolarTerm(LocalDate date) {
        return solarTermRepository.findByDate(date)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOLAR_TERM_NOT_FOUND));
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

    private static final int RECOMMENDED_DAILY_LIMIT = 3;
    private static final int SELECTED_DAILY_LIMIT = 1;

    @Transactional
    public UserMissionCompletionResponse completeRecommendedMission(Long userId, Long missionId, MissionCompletionRequest request) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now();

        // 같은 미션 중복 완료 방지
        validateSameMission(missionId, user);

        // 하루 3회 제한 체크
        long todayCount = userMissionCompletionRepository.countTodayByUserIdAndMissionType(
                userId, MissionType.RECOMMENDED,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        if (todayCount >= RECOMMENDED_DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.RECOMMENDED_MISSION_LIMIT_EXCEEDED);
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        SolarTerm solarTerm = getCurrentSolarTerm(today);

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.create(user, mission, solarTerm, MissionType.RECOMMENDED,
                        request.objectKey(), request.memo())
        );

        return UserMissionCompletionResponse.from(completion);
    }

    @Transactional
    public UserMissionCompletionResponse completeSelectedMission(Long userId, Long missionId, MissionCompletionRequest request) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now();

        // 같은 미션 중복 완료 방지
        validateSameMission(missionId, user);

        // 하루 1회 제한 체크
        long todayCount = userMissionCompletionRepository.countTodayByUserIdAndMissionType(
                userId, MissionType.SELECTED,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        if (todayCount >= SELECTED_DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.SELECTED_MISSION_LIMIT_EXCEEDED);
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        SolarTerm solarTerm = getCurrentSolarTerm(today);

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.create(user, mission, solarTerm, MissionType.SELECTED,
                        request.objectKey(), request.memo())
        );

        return UserMissionCompletionResponse.from(completion);
    }

    private OutboxEvent saveOutboxEvent(User user, Long missionId,
                                         SolarTerm solarTerm, UserMissionCompletion completion) {
        String idempotencyKey = generateIdempotencyKey(
                user.getId(),
                missionId,
                completion.getCreatedAt().toLocalDate()
        );

        MissionLogPayload payload = MissionLogPayload.of(
                idempotencyKey,
                user.getId(),
                solarTerm.getId()
        );
        return outboxRepository.save(new OutboxEvent(toJson(payload)));
    }

    private String generateIdempotencyKey(Long userId, Long missionId, LocalDate completedDate) {
        String raw = userId + ":" + missionId + ":" + completedDate;
        String hash = sha256(raw).substring(0, 8);
        return raw + ":" + hash;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    private void publishMissionCompletedEvent(Long completionId, Long outboxId) {
        eventPublisher.publishEvent(MissionCompletedEvent.of(completionId, outboxId));
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
