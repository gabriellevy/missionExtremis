package com.extremis.web;

import com.extremis.catalog.Missions;
import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Mission;
import com.extremis.db.MissionExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=3600000",
        "extremis.debug=true",
        "spring.profiles.active=test-rapide"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(PersonnageEnMissionTest.DeuxMissionsRapides.class)
class PersonnageEnMissionTest {

    @Autowired
    private GameController controller;

    @Autowired
    private GameService game;

    @Autowired
    private MissionExecutionRepository executions;

    @Autowired
    private List<Mission> missions;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @TestConfiguration
    static class DeuxMissionsRapides {
        @Bean
        public Mission premiereMissionRapide() {
            return Missions.fousDeGotheimRapide();
        }

        @Bean
        public Mission deuxiemeMissionRapide() {
            return Missions.rumeursDePossessionDemoniaqueRapide();
        }
    }

    private void recruterAliceEtBob() {
        game.saveQuickRecruit(new Character("test-alice", "Alice")
                .withSkill(Competence.DISCRETION, 30)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 30));
        game.saveQuickRecruit(new Character("test-bob", "Bob")
                .withSkill(Competence.DISCRETION, 30)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 30));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void unPersonnageDejaEnMissionNePeutPasEtreEnvoyeDansUneAutre() {
        recruterAliceEtBob();
        String missionA = missions.get(0).id();
        String missionB = missions.get(1).id();

        controller.lancerMission(missionA, List.of("test-alice"));
        assertThat(executions.count()).isEqualTo(1);

        controller.lancerMission(missionB, List.of("test-alice"));
        assertThat(executions.count()).isEqualTo(1);

        controller.lancerMission(missionB, List.of("test-bob"));
        assertThat(executions.count()).isEqualTo(2);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void lePersonnageEnMissionNAPparaitPasDansLesCasesDeLOngletMissions() throws Exception {
        recruterAliceEtBob();
        controller.lancerMission(missions.get(0).id(), List.of("test-alice"));

        String html = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .param("onglet", "missions"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("value=\"test-alice\"");
        assertThat(html).contains("value=\"test-bob\"");
    }
}
