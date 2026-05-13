package com.team.peektime_admin.domain.stats.service;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import com.team.peektime_admin.domain.stats.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("이미 존재하는 미션 로그(이미 기록 완료), 무시: idempotencyKey={}", idempotencyKey);
            return;
        }

        UserMissionLog missionLog = createMissionLogBy(request, idempotencyKey, completedDate);

        userMissionLogRepository.save(missionLog);
        log.info("미션 로그 저장 완료: idempotencyKey={}", idempotencyKey);
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