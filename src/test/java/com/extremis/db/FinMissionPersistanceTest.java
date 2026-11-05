package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
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
 * Regression : a la fin d'une mission, les pertes de vitalite et de
 * sang-froid subies par les membres de l'equipe sont repercutees sur le
 * roster (page personnages), et pas seulement le sang-froid.
 */
@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "spring.profiles.active=test-rapide"
})
@Import(FinMissionPersistanceTest.MissionDeTestConfig.class)
class FinMissionPersistanceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private GameService game;

    @Autowired
    private CharacterRepository characters;

    @Test
    @Transactional
    void finDeMissionRepercuteLaVitalitePerdueSurLeRoster() {
        Mission mission = com.extremis.catalog.Missions.fousDeGotheimRapide();
        Character david = new Character("test-david", "David");
        game.saveQuickRecruit(david);
        int vitaliteDepart = david.vitalite();
        int sangFroidDepart = david.sangFroid();

        // la mission se termine avec 9 points de vitalite perdus
        MissionExecutionEntity entity = game.startMission(mission, List.of(david), START);
        ExecutionTeamMemberEntity membre = entity.getTeam().get(0);
        membre.setVitalite(vitaliteDepart - 9);
        membre.setSangFroid(sangFroidDepart - 3);
        MissionReport report = new MissionReport(mission.id(), List.of(), 1, false, "");

        game.finish(entity, report, START);

        CharacterEntity apres = characters.findById("test-david").orElseThrow();
        assertThat(apres.getVitalite()).isEqualTo(vitaliteDepart - 9);
        assertThat(apres.getSangFroid()).isEqualTo(sangFroidDepart - 3);
        assertThat(apres.isAlive()).isTrue();
    }

    @Test
    @Transactional
    void finDeMissionRepercuteLEtatFinalDeTouteLequipeSurLeRoster() {
        Mission mission = com.extremis.catalog.Missions.fousDeGotheimRapide();
        Character agent = new Character("test-fin", "Alice");
        game.saveQuickRecruit(agent);

        MissionExecutionEntity entity = game.startMission(mission, List.of(agent), START);
        MissionExecution execution = game.rebuild(entity).orElseThrow();
        Instant maintenant = START;
        while (!execution.advance(maintenant)) {
            maintenant = maintenant.plus(Duration.ofSeconds(1));
        }
        game.saveProgress(entity, execution, maintenant);

        game.finish(entity, execution.report(), maintenant);

        Character snapshot = execution.teamSnapshot().get(0);
        CharacterEntity apres = characters.findById("test-fin").orElseThrow();
        assertThat(apres.getVitalite()).isEqualTo(snapshot.vitalite());
        assertThat(apres.getSangFroid()).isEqualTo(snapshot.sangFroid());
        assertThat(apres.isAlive()).isEqualTo(snapshot.isAlive());
    }

    @TestConfiguration
    static class MissionDeTestConfig {
        @Bean
        public Mission missionRapide() {
            return com.extremis.catalog.Missions.fousDeGotheimRapide();
        }
    }
}
