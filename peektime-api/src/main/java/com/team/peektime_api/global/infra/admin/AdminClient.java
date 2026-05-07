package com.team.peektime_api.global.infra.admin;

import com.team.peektime_api.global.infra.admin.dto.AdminHomeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    private final RestClient adminRestClient;

    public AdminHomeResponse getHomeData() {
        try {
            return adminRestClient.get()
                    .uri("/api/home")
                    .retrieve()
                    .body(AdminHomeResponse.class);
        } catch (Exception e) {
            log.error("Admin API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("홈 데이터를 가져올 수 없습니다.", e);
        }
    }
}