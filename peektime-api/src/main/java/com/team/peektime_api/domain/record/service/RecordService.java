package com.team.peektime_api.domain.record.service;

import com.team.peektime_api.domain.record.dto.ImageUploadConfirmRequest;
import com.team.peektime_api.domain.record.dto.UserRecordResponse;
import com.team.peektime_api.domain.record.entity.UserRecord;
import com.team.peektime_api.domain.record.repository.UserRecordRepository;
import com.team.peektime_api.domain.user.entity.User;
import com.team.peektime_api.domain.user.repository.UserRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final UserRecordRepository userRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserRecordResponse confirmImageUpload(ImageUploadConfirmRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new UserRecordResponse(userRecordRepository.save(UserRecord.of(user, request)));
    }
}
