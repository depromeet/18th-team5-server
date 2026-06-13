package com.team.peektime_api.domain.user.service;

import com.team.peektime_api.domain.calendar.repository.UserRecordRepository;
import com.team.peektime_api.domain.mission.repository.UserMissionCompletionRepository;
import com.team.peektime_api.domain.mission.repository.UserSelectedMissionRepository;
import com.team.peektime_api.domain.notification.repository.NotificationSettingRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserOnboardingRepository;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private static final int WITHDRAWAL_RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserMissionCompletionRepository userMissionCompletionRepository;
    private final UserSelectedMissionRepository userSelectedMissionRepository;
    private final UserRecordRepository userRecordRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String originalUuid = user.getDeviceUuid();
        String anonymizedUuid = originalUuid + "_withdrawn_" + System.currentTimeMillis();
        user.withdraw(anonymizedUuid);
        userRepository.flush();

        userRepository.save(User.builder()
                .deviceUuid(originalUuid)
                .build());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(WITHDRAWAL_RETENTION_DAYS);
        List<User> withdrawnUsers = userRepository.findWithdrawnBefore(cutoff);

        for (User user : withdrawnUsers) {
            Long userId = user.getId();
            userMissionCompletionRepository.deleteByUser_Id(userId);
            userSelectedMissionRepository.deleteByUser_Id(userId);
            userRecordRepository.deleteByUser_Id(userId);
            notificationSettingRepository.findByUserId(userId)
                    .ifPresent(notificationSettingRepository::delete);
            userOnboardingRepository.findByUserId(userId)
                    .ifPresent(userOnboardingRepository::delete);
            userRepository.delete(user);
            log.info("탈퇴 유저 완전 삭제 완료: userId={}", userId);
        }
    }
}
