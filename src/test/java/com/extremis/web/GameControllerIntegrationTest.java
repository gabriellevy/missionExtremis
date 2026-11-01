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
        "mission.tick-rate-ms=3600000",
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
    @org.springframework.transaction.annotation.Transactional
    void tickerAdvancesMissionWithoutManualAction() {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();
        controller.avancerMissionJusquAuBout();

        assertThat(controller.journal()).contains("Infiltration du meeting nocturne");
        assertThat(controller.journal()).containsAnyOf("Mission réussie", "Mission terminée en échec", "Équipe éliminée");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void apresRedemarrageLeJournalNeRepubliePasLesEvenementsDejaAffiches() {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();
        // fait avancer la mission de deux événements en mode debug
        controller.avancerMissionEvenementSuivant();
        controller.avancerMissionEvenementSuivant();
        controller.avancerMissionEvenementSuivant();
        String avant = controller.journal();
        assertThat(avant).contains("tente le test");
        long lignesAvant = avant.lines().count();

        // simule l'extinction du serveur : le cache mémoire est perdu,
        // seules les lignes de la base de données restent
        controller.simulerRedemarrage();
        controller.rechargerDepuisLaBase();
        String apres = controller.journal();

        // le journal rechargé contient exactement les lignes persistées :
        // ni doublon des événements déjà affichés, ni ligne de plus
        assertThat(apres).contains("tente le test");
        assertThat(apres.lines().count()).isEqualTo(lignesAvant - 2);
    }

    @TestConfiguration
    static class FastMissions {
        @Bean
        public Mission missionRapide() {
            return com.extremis.catalog.Missions.fousDeGotheimRapide();
        }
    }
}
