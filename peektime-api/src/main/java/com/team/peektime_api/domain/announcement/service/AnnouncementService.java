package com.team.peektime_api.domain.announcement.service;

import com.team.peektime_api.domain.announcement.dto.AnnouncementRequest;
import com.team.peektime_api.domain.announcement.dto.AnnouncementResponse;
import com.team.peektime_api.domain.announcement.entity.Announcement;
import com.team.peektime_api.domain.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.create(request.getTitle(), request.getContent());
        Announcement saved = announcementRepository.save(announcement);
        return AnnouncementResponse.from(saved);
    }
}