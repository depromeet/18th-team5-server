package com.team.peektime_admin.domain.stats.service;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import com.team.peektime_admin.domain.stats.repository.UserMissionLogRepository;
import com.team.peektime_admin.global.exception.BusinessException;
import com.team.peektime_admin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMissionLogRepository userMissionLogRepository;

    @Transactional
    public void saveMissionLog(MissionLogRequest request) {
        LocalDate completedDate = request.completedAt().toLocalDate();
        String idempotencyKey = generateIdempotencyKey(request.userUuid(), request.missionId(), completedDate);

        // 1차 방어: 존재 여부 체크 (대부분의 중복 요청 필터링)
        if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("이미 존재하는 미션 로그: idempotencyKey={}", idempotencyKey);
            throw new BusinessException(ErrorCode.DUPLICATE_MISSION_LOG);
        }

        // 2차 방어: UNIQUE 제약 위반 시 예외 처리 (TOCTOU 레이스 컨디션 방지)
        UserMissionLog missionLog = createMissionLogBy(request, idempotencyKey, completedDate);

        try {
            userMissionLogRepository.save(missionLog);
            log.info("미션 로그 저장 완료: idempotencyKey={}", idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            log.info("중복 요청으로 인한 제약 조건 위반: idempotencyKey={}", idempotencyKey);
            throw new BusinessException(ErrorCode.DUPLICATE_MISSION_LOG);
        }
    }

    private static UserMissionLog createMissionLogBy(MissionLogRequest request, String idempotencyKey, LocalDate completedDate) {
        UserMissionLog missionLog = UserMissionLog.create(
                idempotencyKey,
                request.userUuid(),
                request.missionId(),
                request.missionType(),
                request.solarTermId(),
                completedDate,
                request.completedAt()
        );
        return missionLog;
    }

    private String generateIdempotencyKey(String userUuid, Long missionId, LocalDate completedDate) {
        String raw = userUuid + ":" + missionId + ":" + completedDate;
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
}