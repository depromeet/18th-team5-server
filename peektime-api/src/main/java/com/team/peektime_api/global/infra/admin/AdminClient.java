package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import com.team.peektime_api.global.outbox.dto.MissionLogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    private final RestClient adminRestClient;

    public AdminHomeResponse getHomeData() {
        return getHomeData(LocalDate.now());
    }

    public AdminHomeResponse getHomeData(LocalDate date) {
        try {
            String dateParam = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            return adminRestClient.get()
                    .uri("/api/home?date={date}", dateParam)
                    .retrieve()
                    .body(AdminHomeResponse.class);
        } catch (Exception e) {
            log.error("Admin API 호출 실패 (date={}): {}", date, e.getMessage());
            throw new RuntimeException("홈 데이터를 가져올 수 없습니다.", e);
        }
    }

    public void sendMissionLog(MissionLogPayload payload) {
        try {
            adminRestClient.post()
                    .uri("/api/stats/mission-log")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("미션 로그 전송 성공: missionId={}", payload.missionId());
        } catch (Exception e) {
            log.error("미션 로그 전송 실패: {}", e.getMessage());
            throw new RuntimeException("미션 로그 전송 실패", e);
        }
    }
}