package com.team.peektime_admin.domain.stats.service;

import com.team.peektime_admin.domain.stats.dto.MissionLogRequest;
import com.team.peektime_admin.domain.stats.dto.UserRankingProjection;
import com.team.peektime_admin.domain.stats.dto.UserRankingResponse;
import com.team.peektime_admin.domain.stats.entity.UserMissionLog;
import com.team.peektime_admin.domain.stats.repository.UserMissionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMissionLogRepository userMissionLogRepository;

    /**
     * 미션 로그 저장 (멱등성 보장)
     *
     * 클라이언트(API 서버)가 생성한 멱등성 키를 그대로 사용하여 중복 요청을 방지합니다.
     *
     * @return true: 새로 저장됨, false: 이미 존재하여 무시됨
     */
    @Transactional
    public boolean saveMissionLog(MissionLogRequest request) {
        String idempotencyKey = request.idempotencyKey();

        if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("이미 존재하는 미션 로그 (멱등성 처리): idempotencyKey={}", idempotencyKey);
            return false;
        }

        UserMissionLog missionLog = UserMissionLog.create(
                idempotencyKey,
                request.userUuid(),
                request.solarTermId()
        );

        try {
            userMissionLogRepository.save(missionLog);
            log.info("미션 로그 저장 완료: idempotencyKey={}", idempotencyKey);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("동시 요청으로 인한 제약 조건 위반 (멱등성 처리): idempotencyKey={}", idempotencyKey);
            return false;
        }
    }

    /**
     * 절기별 사용자 랭킹 조회
     */
    @Transactional(readOnly = true)
    public List<UserRankingResponse> getRankingBySolarTerm(Long solarTermId, int limit) {
        List<UserRankingProjection> projections = userMissionLogRepository
                .findRankingBySolarTerm(solarTermId);

        return toRankingResponse(projections, limit);
    }

    /**
     * 전체 기간 사용자 랭킹 조회
     */
    @Transactional(readOnly = true)
    public List<UserRankingResponse> getOverallRanking(int limit) {
        List<UserRankingProjection> projections = userMissionLogRepository
                .findOverallRanking();

        return toRankingResponse(projections, limit);
    }

    private List<UserRankingResponse> toRankingResponse(List<UserRankingProjection> projections, int limit) {
        List<UserRankingResponse> rankings = new ArrayList<>();
        int rank = 1;

        for (UserRankingProjection projection : projections) {
            if (rank > limit) break;
            rankings.add(UserRankingResponse.of(rank++, projection));
        }

        return rankings;
    }
}
