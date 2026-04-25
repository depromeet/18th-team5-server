package com.team.peektime_api.domain.record.service;

import com.team.peektime_api.domain.mission.dto.UserMissionCompletionRequest;
import com.team.peektime_api.domain.mission.dto.UserMissionCompletionResponse;
import com.team.peektime_api.domain.mission.entity.UserMissionCompletion;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRecordService {

    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserMissionCompletionResponse completeMission(Long missionId, UserMissionCompletionRequest request) {
        User user = findUser(request.userId());

        if (userMissionCompletionRepository.existsByUserIdAndMissionId(user.getId(), missionId)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }

        UserMissionCompletion completion = userMissionCompletionRepository.save(
                UserMissionCompletion.of(user, missionId, request)
        );
        return UserMissionCompletionResponse.from(completion);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
