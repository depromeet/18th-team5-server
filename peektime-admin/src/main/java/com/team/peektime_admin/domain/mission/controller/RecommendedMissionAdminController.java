package com.team.peektime_admin.domain.mission.controller;

import com.team.peektime_admin.domain.mission.repository.RecommendedMissionPoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/recommended-missions")
@RequiredArgsConstructor
public class RecommendedMissionAdminController {

    private final RecommendedMissionPoolRepository recommendedMissionPoolRepository;

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> removeFromRecommended(@PathVariable Long id) {
        recommendedMissionPoolRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}