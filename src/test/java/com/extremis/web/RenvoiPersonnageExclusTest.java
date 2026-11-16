package com.extremis.web;

import com.extremis.catalog.Missions;
import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Mission;
import com.extremis.core.MissionEvent;
import com.extremis.core.SimpleTest;
import com.extremis.db.MissionExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=3600000",
        "extremis.debug=true",
        "spring.profiles.active=test-rapide"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(RenvoiPersonnageExclusTest.DeuxMissionsConfig.class)
class RenvoiPersonnageExclusTest {

    @Autowired
    private GameController controller;

    @Autowired
    private GameService game;

    @Autowired
    private MissionExecutionRepository executions;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @TestConfiguration
    static class DeuxMissionsConfig {
        @Bean
        public Mission missionExclusion() {
            return new Mission("mission-exclusion", "Mission exclusion", "brief",
                    List.of(Competence.DISCRETION),
                    List.of(
                            MissionEvent.builder("e1", "Epreuve")
                                    .introduction("Une épreuve.")
                                    .duree(Duration.ofSeconds(1))
                                    .test(new SimpleTest(Competence.DISCRETION, -200))
                                    .succes(MissionEvent.EffetReussite.builder()
                                            .texteSimple("Epreuve réussie.")
                                            .evenementSuivant("fin")
                                            .build())
                                    .echec(MissionEvent.EffetEchec.builder()
                                            .texteSimple("Epreuve échouée.")
                                            .exclureJoueur()
                                            .evenementSuivant("e1")
                                            .build())
                                    .build(),
                            MissionEvent.builder("fin", "Fin")
                                    .duree(Duration.ZERO)
                                    .effet("Mission accomplie.")
                                    .succes(MissionEvent.EffetReussite.builder()
                                            .evenementSuivant(null)
                                            .build())
                                    .build()));
        }

        @Bean
        public Mission missionRapide() {
            return Missions.fousDeGotheimRapide();
        }
    }

    private void recruterAliceEtBob() {
        game.saveQuickRecruit(new Character("test-alice", "Alice")
                .withSkill(Competence.DISCRETION, 90));
        game.saveQuickRecruit(new Character("test-bob", "Bob")
                .withSkill(Competence.DISCRETION, 10));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void unPersonnageExcluDuneMissionEnCoursPeutEtreRenvoyeImmediatement() throws Exception {
        controller.reinitialiserUsine();
        recruterAliceEtBob();
        controller.lancerMission("mission-exclusion", List.of("test-alice", "test-bob"));
        Long idA = executions.findAll().get(0).getId();
        controller.avancerMission(idA);

        String journal = controller.journal();
        assertThat(journal).contains("exclu de l'aventure");

        String html = mockMvc.perform(get("/").param("onglet", "missions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("value=\"test-alice\"");
        assertThat(html).doesNotContain("value=\"test-bob\"");

        controller.lancerMission("gotheim", List.of("test-alice"));
        assertThat(executions.count()).isEqualTo(2);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void unPersonnageRemplaceParJokerPeutEtreRenvoyeDansUneAutreMission() throws Exception {
        controller.reinitialiserUsine();
        recruterAliceEtBob();
        controller.lancerMission("gotheim", List.of("test-alice"));
        Long idA = executions.findAll().get(0).getId();
        controller.avancerMission(idA);

        int jokersAvant = game.jokers();
        mockMvc.perform(post("/mission/" + idA + "/joker/remplacer")
                        .param("membreId", "test-alice")
                        .param("remplaceParId", "test-bob"))
                .andExpect(status().is3xxRedirection());
        assertThat(game.jokers()).isEqualTo(jokersAvant - 1);

        controller.lancerMission("mission-exclusion", List.of("test-alice"));
        assertThat(executions.count()).isEqualTo(2);

        Long idB = game.activeExecutionEntities().get(1).getId();
        mockMvc.perform(get("/").param("onglet", String.valueOf(idA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/").param("onglet", String.valueOf(idB)))
                .andExpect(status().isOk());

        controller.avancerMissionJusquAuBout();
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }
}
