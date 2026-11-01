package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.LigneJournal;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.web.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression : apres un redemarrage du serveur, la mission reprend la
 * ou elle en etait sans republier ni dupliquer les evenements deja
 * affiches (l'historique du journal est recharge depuis la base).
 */
@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "spring.profiles.active=test-rapide"
})
@Import(RepriseMissionPersistanceTest.MissionDeTestConfig.class)
class RepriseMissionPersistanceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private GameService game;

    @Autowired
    private MissionExecutionRepository executions;

    @Test
    @Transactional
    void apresRedemarrageLeJournalContinueSansDoublonNiDesordre() {
        Mission mission = com.extremis.catalog.Missions.fousDeGotheimRapide();
        Character agent = new Character("test-reprise", "Alice")
                .withSkill(Competence.DISCRETION, 90)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 90);

        // lancement + premier evenement resolu, persistes en base
        MissionExecutionEntity entity = game.startMission(mission, List.of(agent), START);
        MissionExecution execution = game.rebuild(entity).orElseThrow();
        execution.advance(START.plus(Duration.ofHours(1)));
        game.saveProgress(entity, execution, START);
        List<String> avantRedemarrage = execution.logLines().stream().map(LigneJournal::texte).toList();
        assertThat(avantRedemarrage).isNotEmpty();

        // "redemarrage" : on reconstruit l'execution depuis la base seule
        MissionExecutionEntity rechargee = executions.findById(entity.getId()).orElseThrow();
        MissionExecution repris = game.rebuild(rechargee).orElseThrow();
        assertThat(repris.logLines().stream().map(LigneJournal::texte).toList())
                .containsExactlyElementsOf(avantRedemarrage);

        // la mission continue : les nouvelles lignes s'ajoutent a la suite
        repris.advance(START.plus(Duration.ofDays(2)));
        game.saveProgress(rechargee, repris, START);
        List<String> apres = repris.logLines().stream().map(LigneJournal::texte).toList();
        assertThat(apres).startsWith(avantRedemarrage.toArray(new String[0]));
        // pas de re-publication des lignes deja persistantes : chaque
        // ligne anterieure n'apparait qu'une seule fois au total
        for (String ligne : avantRedemarrage) {
            assertThat(apres.stream().filter(ligne::equals)).hasSize(1);
        }

        // et la base contient exactement le journal complet, une seule fois
        MissionExecutionEntity finalEntity = executions.findById(entity.getId()).orElseThrow();
        List<String> enBase = finalEntity.getLogLines().stream()
                .map(ExecutionLogLineEntity::getLine).toList();
        assertThat(enBase).containsExactlyElementsOf(apres);
    }

    @TestConfiguration
    static class MissionDeTestConfig {
        @Bean
        public Mission missionRapide() {
            return com.extremis.catalog.Missions.fousDeGotheimRapide();
        }
    }
}
