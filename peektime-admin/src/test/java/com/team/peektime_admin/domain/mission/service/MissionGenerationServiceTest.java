package com.team.peektime_admin.domain.mission.service;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class MissionGenerationServiceTest {

    @Autowired
    private MissionGenerationService missionGenerationService;

    @Test
    void 미션_3개_생성_테스트() {
        // when
        List<GeneratedMissionDto> missions = missionGenerationService.generateMissions(5);

        // then
        assertThat(missions).hasSize(5);

        missions.forEach(mission -> {
            System.out.println("제목: " + mission.getTitle());
            System.out.println("설명: " + mission.getDescription());
            System.out.println("공간: " + mission.getSpaceType());
            System.out.println("강도: " + mission.getIntensityType());
            System.out.println("동반: " + mission.getCompanionType());
            System.out.println("카테고리: " + mission.getCategoryType());
            System.out.println("---");

            assertThat(mission.getTitle()).isNotBlank();
            assertThat(mission.getSpaceType()).isIn("INDOOR", "OUTDOOR");
            assertThat(mission.getIntensityType()).isIn("LIGHT", "MODERATE", "ACTIVE");
            assertThat(mission.getCompanionType()).isIn("SOLO", "TOGETHER");
            assertThat(mission.getCategoryType()).isIn("FOOD", "NATURE", "RECORD", "PLACE", "SENSE");
        });
    }
}