package com.extremis.web;

import com.extremis.core.Mission;
import java.util.List;
import com.extremis.db.CharacterRepository;
import com.extremis.db.MissionExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "extremis.debug=true",
        "spring.profiles.active=test-rapide"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(GameControllerIntegrationTest.FastMissions.class)
class GameControllerIntegrationTest {

    @Autowired
    private GameController controller;

    @Autowired
    private MissionExecutionRepository executions;

    @Autowired
    private CharacterRepository personnages;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @org.junit.jupiter.api.Test
    void laPageDAccueilAvecOngletMissionsSeRendSansErreur() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .param("onglet", "missions"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    void reinitialisationUsineSupprimeLesActionsUtilisateur() {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();
        controller.reinitialiserUsine();
        assertThat(executions.count()).isZero();
        assertThat(personnages.findAll())
                .noneMatch(c -> c.getName().equals("Alice"))
                .isNotEmpty();
        assertThat(controller.journal()).isEmpty();
    }

    @Test
    void tickerAdvancesMissionWithoutManualAction() throws InterruptedException {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (controller.journal().contains("Mission terminee")
                    || controller.journal().contains("Equipe eliminee")) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(controller.journal()).contains("Infiltration du meeting nocturne");
        assertThat(controller.journal()).containsAnyOf("Mission reussie", "Mission terminee en echec", "Equipe eliminee");
    }

    @TestConfiguration
    static class FastMissions {
        @Bean
        public List<Mission> missionsTest() {
            return List.of(com.extremis.catalog.Missions.fousDeGotheimRapide());
        }
    }
}
