package com.team.peektime_api.domain.record.controller;

import com.team.peektime_api.domain.record.service.UserRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "UserRecord", description = "기록 관리")
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class UserRecordController {

    private final UserRecordService userRecordService;
}
