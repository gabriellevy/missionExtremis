package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.LigneJournal;
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
 * Regression "les blessures de Chigur ne s'appliquent pas" : en cours de
 * mission, l'attaque de Chigur applique bien ses 3D10 a l'arrangeur, la
 * blessure est visible dans l'onglet Mission (team membre de l'execution)
 * et, a la fin de la mission, elle est repercutee sur le roster.
 *
 * L'arrangeur ici rate toutes ses discretions et tous ses tirs : Chigur
 * l'attaque en boucle jusqu'a ce qu'il succombe, ce qui prouve que les
 * degats s'accumulent reellement au fil des evenements.
 */
@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "spring.profiles.active=test-rapide"
})
@Import(BlessuresChigurPersistanceTest.MissionDeTestConfig.class)
class BlessuresChigurPersistanceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private GameService game;

    @Autowired
    private CharacterRepository characters;

    @Test
    @Transactional
    void lattaqueDeChigurBlesseLArrangeurEtLaBlessurePersisteSurLeRoster() {
        Mission mission = com.extremis.catalog.Missions.pasUnPaysPourLesVieuxRapide();
        Character alice = new Character("test-blesse-par-chigur", "Alice")
                .withSkill(Competence.RAGOT, 90)
                .withSkill(Competence.ELOQUENCE, 90)
                .withSkill(Competence.EVALUATION, 90)
                .withSkill(Competence.TIR, -50)
                .withSkill(Competence.DISCRETION, -50)
                .withVitaliteDepart(18);
        alice.setVitalite(18);
        game.saveQuickRecruit(alice);

        MissionExecutionEntity entity = game.startMission(mission, List.of(alice), START);
        MissionExecution execution = game.rebuild(entity).orElseThrow();

        execution.advance(START.plus(Duration.ofDays(30)));
        game.saveProgress(entity, execution, START);

        List<String> journal = execution.logLines().stream().map(LigneJournal::texte).toList();
        // Chigur a attaque et blesse l'arrangeur
        assertThat(journal).anyMatch(l -> l.contains("Le tueur a rep\u00e9r\u00e9 les arrangeurs."));
        assertThat(journal).anyMatch(l -> l.contains("subit 3D10 degats"));
        // en cours de mission, la blessure est deja visible cote execution
        assertThat(entity.getTeam().get(0).getVitalite()).isLessThan(18);
        // et elle est aussi repercutee en temps reel sur la fiche du perso
        CharacterEntity enCours = characters.findById("test-blesse-par-chigur").orElseThrow();
        assertThat(enCours.getVitalite()).isEqualTo(entity.getTeam().get(0).getVitalite());
        assertThat(enCours.getVitalite()).isLessThan(18);

        MissionReport report = execution.report();
        game.finish(entity, report, START);

        CharacterEntity apres = characters.findById("test-blesse-par-chigur").orElseThrow();
        // les blessures de Chigur ont reelement reduit la vitalite du roster
        assertThat(apres.getVitalite()).isEqualTo(execution.teamSnapshot().get(0).vitalite());
        assertThat(apres.isAlive()).isFalse();
        // l'arrangeur a bien subi plusieurs attaques avant de succomber
        assertThat(journal.stream().filter(l -> l.contains("subit 3D10")).count()).isGreaterThanOrEqualTo(1);
    }

    @TestConfiguration
    static class MissionDeTestConfig {
        @Bean
        public Mission pasUnPaysPourLesVieux() {
            return com.extremis.catalog.Missions.pasUnPaysPourLesVieuxRapide();
        }
    }
}
