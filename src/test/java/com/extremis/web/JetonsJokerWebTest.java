package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Lieu;
import com.extremis.core.Mission;
import com.extremis.core.MissionEvent;
import com.extremis.core.SimpleTest;
import com.extremis.db.ExecutionTeamMemberEntity;
import com.extremis.db.MissionExecutionEntity;
import com.extremis.db.MissionExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
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
@Import(JetonsJokerWebTest.MissionJokerConfig.class)
class JetonsJokerWebTest {

    @Autowired
    private GameController controller;

    @Autowired
    private GameService game;

    @Autowired
    private MissionExecutionRepository executions;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @TestConfiguration
    static class MissionJokerConfig {
        @Bean
        public Mission missionJoker() {
            return new Mission("mission-joker", "Mission joker", "brief", Lieu.PARIS_1ER,
                    List.of(Competence.DISCRETION),
                    List.of(
                            MissionEvent.builder("epreuve", "Epreuve")
                                    .introduction("Une épreuve.")
                                    .duree(Duration.ofSeconds(1))
                                    .test(new SimpleTest(Competence.DISCRETION, -100))
                                    .succes(MissionEvent.EffetReussite.builder()
                                            .texteSimple("Epreuve réussie.")
                                            .evenementSuivant("fin")
                                            .build())
                                    .echec(MissionEvent.EffetEchec.builder()
                                            .texteSimple("Epreuve échouée.")
                                            .degats(2)
                                            .evenementSuivant("epreuve")
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
    }

    private Long lancerMissionAvecAlice() {
        controller.lancerMission("mission-joker", List.of("test-alice"));
        return executions.findAll().get(0).getId();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void gainQuotidienEtPlafondATroisJokers() {
        controller.reinitialiserUsine();
        Instant maintenant = Instant.now();
        assertThat(game.jokers()).isEqualTo(1);
        assertThat(game.gagnerJokerQuotidien(maintenant)).isEqualTo(2);
        assertThat(game.gagnerJokerQuotidien(maintenant.plusSeconds(3600))).isEqualTo(2);
        assertThat(game.gagnerJokerQuotidien(maintenant.plus(Duration.ofDays(1)))).isEqualTo(3);
        assertThat(game.gagnerJokerQuotidien(maintenant.plus(Duration.ofDays(2)))).isEqualTo(3);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void depenserUnJokerAnnuleLeDernierTestRateEnReussite() throws Exception {
        controller.reinitialiserUsine();
        game.saveQuickRecruit(new Character("test-alice", "Alice")
                .withSkill(Competence.DISCRETION, 50));
        Long id = lancerMissionAvecAlice();
        controller.avancerMission(id);

        String htmlAvant = mockMvc.perform(get("/").param("onglet", String.valueOf(id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlAvant).contains("/mission/" + id + "/joker/reussite");

        int jokersAvant = game.jokers();
        mockMvc.perform(post("/mission/" + id + "/joker/reussite"))
                .andExpect(status().is3xxRedirection());

        assertThat(game.jokers()).isEqualTo(jokersAvant - 1);
        assertThat(controller.journal()).contains("coïncidence calculée");
        assertThat(controller.journal()).contains("Epreuve réussie");
        assertThat(controller.journal()).doesNotContain("Epreuve échouée");
        assertThat(controller.journal()).contains("Mission réussie");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void depenserUnJokerPourRemplacerUnMembreDeLEquipe() throws Exception {
        controller.reinitialiserUsine();
        game.saveQuickRecruit(new Character("test-alice", "Alice")
                .withSkill(Competence.DISCRETION, 50));
        game.saveQuickRecruit(new Character("test-bob", "Bob")
                .withSkill(Competence.DISCRETION, 50));
        Long id = lancerMissionAvecAlice();

        int jokersAvant = game.jokers();
        mockMvc.perform(post("/mission/" + id + "/joker/remplacer")
                        .param("membreId", "test-alice")
                        .param("remplaceParId", "test-bob"))
                .andExpect(status().is3xxRedirection());

        assertThat(game.jokers()).isEqualTo(jokersAvant - 1);
        MissionExecutionEntity entity = executions.findById(id).orElseThrow();
        assertThat(entity.getTeam())
                .extracting(ExecutionTeamMemberEntity::getCharacterId)
                .containsExactly("test-bob");
        assertThat(controller.journal()).contains("remplace Alice");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void lesBoutonsJokerSontCachesQuandIlNeRestePlusDeJoker() throws Exception {
        controller.reinitialiserUsine();
        game.saveQuickRecruit(new Character("test-alice", "Alice")
                .withSkill(Competence.DISCRETION, 50));
        Long id = lancerMissionAvecAlice();
        controller.avancerMission(id);

        game.gagnerJokerQuotidien(Instant.now());
        while (game.jokers() > 0) {
            game.spendJoker();
        }
        String htmlSansJoker = mockMvc.perform(get("/").param("onglet", String.valueOf(id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlSansJoker).doesNotContain("joker/reussite");
        assertThat(htmlSansJoker).doesNotContain("joker/remplacer");

        game.ajouterJoker();
        String htmlAvecJoker = mockMvc.perform(get("/").param("onglet", String.valueOf(id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlAvecJoker).contains("/mission/" + id + "/joker/reussite");
        assertThat(htmlAvecJoker).contains("/mission/" + id + "/joker/remplacer");
    }
}
