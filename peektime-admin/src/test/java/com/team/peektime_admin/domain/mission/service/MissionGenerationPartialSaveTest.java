package com.team.peektime_admin.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.peektime_admin.domain.mission.dto.GeneratedMissionDto;
import com.team.peektime_admin.domain.mission.dto.GeneratedMissionsWrapper;
import com.team.peektime_admin.domain.mission.dto.MissionGenerationResult;
import com.team.peektime_admin.domain.mission.entity.Mission;
import com.team.peektime_admin.domain.mission.repository.MissionRepository;
import com.team.peektime_admin.domain.solarterm.repository.SolarTermRepository;
import com.team.peektime_admin.domain.sync.service.SyncService;
import com.team.peektime_admin.infra.llm.GeminiClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MissionGenerationPartialSaveTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private SolarTermRepository solarTermRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private SyncService syncService;

    @InjectMocks
    private MissionGenerationService missionGenerationService;

    @Test
    void generateMissionsSavesValidMissionsAndSkipsInvalidOnes() throws Exception {
        List<GeneratedMissionDto> generatedMissions = List.of(
                mission("산책하기", "짧은 설명"),
                mission("글자수가너무긴미션제목입니다오늘하기", "짧은 설명"),
                mission("물마시기", "물 한잔 마시기")
        );

        when(geminiClient.generateContent(anyString())).thenReturn("{}");
        when(jsonMapper.readValue(anyString(), eq(GeneratedMissionsWrapper.class)))
                .thenReturn(new GeneratedMissionsWrapper(generatedMissions));

        MissionGenerationResult result = missionGenerationService.generateMissions(3);

        assertThat(result.getMissions())
                .extracting(GeneratedMissionDto::getTitle)
                .containsExactly("산책하기", "물마시기");
        assertThat(result.getSkippedReasons())
                .hasSize(1)
                .first()
                .asString()
                .contains("2번째 미션 제외", "공백 포함 16자 이내");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mission>> captor = ArgumentCaptor.forClass(List.class);
        verify(missionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        verify(syncService).syncAllMissions();
    }

    private GeneratedMissionDto mission(String title, String description) {
        return new GeneratedMissionDto(
                title,
                description,
                "INDOOR",
                "SOLO",
                "CONTENT",
                "CULTURE_CONTENT",
                "AESTHETE"
        );
    }
}
