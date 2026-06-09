package com.team.peektime_api.domain.announcement.service;

import com.team.peektime_api.domain.announcement.dto.AnnouncementListResponse;
import com.team.peektime_api.domain.announcement.dto.AnnouncementRequest;
import com.team.peektime_api.domain.announcement.dto.AnnouncementResponse;
import com.team.peektime_api.domain.announcement.entity.Announcement;
import com.team.peektime_api.domain.announcement.repository.AnnouncementRepository;
import com.team.peektime_api.global.exception.BusinessException;
import com.team.peektime_api.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public List<AnnouncementListResponse> getAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AnnouncementListResponse::from)
                .toList();
    }

    public AnnouncementResponse getAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return AnnouncementResponse.from(announcement);
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.create(request.getTitle(), request.getContent());
        Announcement saved = announcementRepository.save(announcement);
        return AnnouncementResponse.from(saved);
    }
}