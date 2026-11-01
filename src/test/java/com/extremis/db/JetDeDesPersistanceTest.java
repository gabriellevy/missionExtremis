package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.LigneJournal;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.SeededRandom;
import com.extremis.web.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que le jet de des (dizaines + unites) d'une ligne de test
 * est bien ecrit en base et relu apres "redemarrage" (reconstruction
 * du journal depuis les entites), afin que l'affichage en images des
 * des survive a la fermeture du jeu.
 */
@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "spring.profiles.active=test-rapide"
})
@Import(JetDeDesPersistanceTest.MissionDeTestConfig.class)
class JetDeDesPersistanceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private GameService game;

    @Autowired
    private MissionExecutionRepository executions;

    @Test
    @Transactional
    void leJetDeDesEstPersistePuisReluDepuisLaBase() {
        Mission mission = missionDeTest();
        Character agent = new Character("test-jet", "Alice")
                .withSkill(Competence.DISCRETION, 50);
        MissionExecutionEntity entity = game.startMission(mission, List.of(agent), START);

        MissionExecution execution = game.rebuild(entity).orElseThrow();
        execution.advance(START.plus(Duration.ofDays(1)));
        game.saveProgress(entity, execution, START);

        // au moins une ligne de test avec un jet
        List<LigneJournal> lignesAvecJet = execution.logLines().stream()
                .filter(LigneJournal::aUnJet)
                .toList();
        assertThat(lignesAvecJet).isNotEmpty();
        LigneJournal attendu = lignesAvecJet.get(0);

        MissionExecutionEntity rechargee = executions.findById(entity.getId()).orElseThrow();
        List<ExecutionLogLineEntity> lignes = rechargee.getLogLines();
        assertThat(lignes).isNotEmpty();
        ExecutionLogLineEntity ligneJet = lignes.stream()
                .filter(l -> "jet".equals(l.getType()))
                .findFirst()
                .orElseThrow();
        assertThat(ligneJet.getLine()).isEqualTo(attendu.texte());
        assertThat(ligneJet.getDeDizaines()).isEqualTo(attendu.jet().dizaines());
        assertThat(ligneJet.getDeUnites()).isEqualTo(attendu.jet().unites());

        // les lignes sans jet n'ont pas de type renseigne
        assertThat(lignes.stream().filter(l -> l.getType() == null)).isNotEmpty();
    }

    private Mission missionDeTest() {
        return com.extremis.catalog.Missions.fousDeGotheimRapide();
    }

    @TestConfiguration
    static class MissionDeTestConfig {
        @Bean
        public Mission missionRapide() {
            return com.extremis.catalog.Missions.fousDeGotheimRapide();
        }
    }
}
