package com.team.peektime_admin.web.controller;

import com.team.peektime_admin.domain.solarterm.entity.SolarTerm;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.domain.stats.dto.UserRankingResponse;
import com.team.peektime_admin.domain.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/ranking")
@RequiredArgsConstructor
public class RankingPageController {

    private final StatsService statsService;
    private final SolarTermRepository solarTermRepository;

    @GetMapping
    public String rankingPage(
            @RequestParam(required = false) Long solarTermId,
            @RequestParam(defaultValue = "50") int limit,
            Model model
    ) {
        // 절기 목록
        List<SolarTerm> solarTerms = solarTermRepository.findAll();
        model.addAttribute("solarTerms", solarTerms);

        // 현재 절기
        LocalDate today = LocalDate.now();
        Optional<SolarTerm> currentTermOpt = solarTermRepository.findByDate(today);
        currentTermOpt.ifPresent(term -> model.addAttribute("currentTerm", term));

        // 랭킹 조회
        List<UserRankingResponse> rankings;
        if (solarTermId != null) {
            rankings = statsService.getRankingBySolarTerm(solarTermId, limit);
            model.addAttribute("selectedSolarTermId", solarTermId);
        } else if (currentTermOpt.isPresent()) {
            rankings = statsService.getRankingBySolarTerm(currentTermOpt.get().getId(), limit);
            model.addAttribute("selectedSolarTermId", currentTermOpt.get().getId());
        } else {
            rankings = statsService.getOverallRanking(limit);
            model.addAttribute("selectedSolarTermId", null);
        }

        model.addAttribute("rankings", rankings);
        model.addAttribute("menu", "ranking");

        return "stats/ranking";
    }
}