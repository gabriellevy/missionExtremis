package com.extremis.web;

import com.extremis.core.Mission;
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
            if (controller.journal().contains("Mission terminée")
                    || controller.journal().contains("Équipe éliminée")) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(controller.journal()).contains("Infiltration du meeting nocturne");
        assertThat(controller.journal()).containsAnyOf("Mission réussie", "Mission terminée en échec", "Équipe éliminée");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void apresRedemarrageLeJournalNeRepubliePasLesEvenementsDejaAffiches() {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();
        // fait avancer la mission jusqu'au premier test résolu
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (controller.journal().contains("tente le test")) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        String avant = controller.journal();
        assertThat(avant).contains("tente le test");

        // simule l'extinction du serveur : le cache mémoire est perdu,
        // seules les lignes de la base de données restent
        controller.simulerRedemarrage();
        String apres = controller.journal();

        assertThat(apres).isEqualTo(avant);
    }

    @TestConfiguration
    static class FastMissions {
        @Bean
        public Mission missionRapide() {
            return com.extremis.catalog.Missions.fousDeGotheimRapide();
        }
    }
}
