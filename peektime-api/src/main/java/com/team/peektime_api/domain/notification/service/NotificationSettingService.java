package com.team.peektime_api.domain.notification.service;

import com.team.peektime_api.domain.notification.dto.NotificationSettingRequest;
import com.team.peektime_api.domain.notification.dto.NotificationSettingResponse;
import com.team.peektime_api.domain.notification.entity.NotificationSetting;
import com.team.peektime_api.domain.notification.repository.NotificationSettingRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingResponse getSettings(Long userId) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));

        return NotificationSettingResponse.from(setting);
    }

    public NotificationSettingResponse updateSettings(Long userId, NotificationSettingRequest request) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));

        setting.update(request.getDailyMission(), request.getSolarTermEnd(), request.getSolarTermChange());

        return NotificationSettingResponse.from(setting);
    }

    private NotificationSetting createDefaultSetting(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return notificationSettingRepository.save(NotificationSetting.createDefault(user));
    }
}