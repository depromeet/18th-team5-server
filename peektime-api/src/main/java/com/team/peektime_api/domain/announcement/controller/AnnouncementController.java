package com.team.peektime_api.domain.announcement.controller;

import com.team.peektime_api.domain.announcement.dto.AnnouncementListResponse;
import com.team.peektime_api.domain.announcement.dto.AnnouncementRequest;
import com.team.peektime_api.domain.announcement.dto.AnnouncementResponse;
import com.team.peektime_api.domain.announcement.service.AnnouncementService;
import com.team.peektime_api.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Announcement", description = "공지사항 API")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "공지사항 전체 조회 (최신순)")
    @GetMapping
    public ResponseEntity<SuccessResponse<List<AnnouncementListResponse>>> getAnnouncements() {
        List<AnnouncementListResponse> response = announcementService.getAnnouncements();
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @Operation(summary = "공지사항 생성")
    @PostMapping
    public ResponseEntity<SuccessResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }
}