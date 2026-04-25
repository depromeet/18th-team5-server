package com.team.peektime_api.domain.mission.service;

import com.team.peektime_api.domain.mission.dto.UserMissionCompletionDetailResponse;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.infra.S3.S3Service;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMissionCompletionService {

    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public UserMissionCompletionResponse completeMission(Long missionId, UserMissionCompletionRequest request) {
        User user = findUser(request.userId());

        if (userMissionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), missionId)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.of(user, missionId, request)
        );
        return UserMissionCompletionResponse.from(completion);
    }

    @Transactional(readOnly = true)
    public List<UserMissionCompletionDetailResponse> getMissionCompletions(Long missionId, Long userId) {
        return userMissionCompletionRepository.findByUser_IdAndMissionId(userId, missionId)
                .stream()
                .map(completion -> UserMissionCompletionDetailResponse.of(
                        completion,
                        completion.getObjectKey() != null
                                ? s3Service.generatePresignedViewUrl(completion.getObjectKey())
                                : null
                ))
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
