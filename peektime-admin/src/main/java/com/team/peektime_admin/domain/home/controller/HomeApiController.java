package com.team.peektime_admin.domain.home.controller;

import com.team.peektime_admin.domain.home.dto.HomeDataResponse;
import com.team.peektime_admin.domain.home.service.HomeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeApiController {

    private final HomeDataService homeDataService;

    @GetMapping
    public ResponseEntity<HomeDataResponse> getHomeData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(homeDataService.getHomeData(date));
    }
}