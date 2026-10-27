package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la mission "Rumeurs de possession d\u00e9moniaque" (wiki, page du
 * m\u00eame nom) : l'enquete puis les branches selon les moyens de l'equipe
 * (exorcisme, psychiatrie, meurtre ou abandon).
 */
class RumeursPossessionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource TOUJOURS_100 = () -> 100;
    private static final RandomSource TOUJOURS_1 = () -> 1;

    @Test
    void laMissionReussitParLexorcismeSiUnArrangeurSaitExorciser() {
        Character exorciste = agent().withSpecialite("Exorcisme");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(exorciste), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Un arrangeur sait pratiquer un exorcisme."));
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Le d\u00e9mon a \u00e9t\u00e9 extrait de sa victime."));
        // l'apparence de la victime coute 2 de sang-froid, l'exorcisme 2 de plus
        assertThat(exorciste.sangFroid()).isEqualTo(6);
    }

    @Test
    void laMaitriseExorcismePermetAussiLaBrancheExorcisme() {
        Character exorciste = agent().withMaitrise("Exorcisme");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(exorciste), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Un arrangeur sait pratiquer un exorcisme."));
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Le d\u00e9mon a \u00e9t\u00e9 extrait de sa victime."));
    }

    @Test
    void laMissionReussitParLaPsychiatrieSansExorciste() {
        Character psychiatre = agent().withSpecialite("Psychiatre");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(psychiatre), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("la victime sera intern\u00e9e."));
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Vous faites interner d\u00e9finitivement la malheureuse victime."));
        // 2 (victime) + 4 (psychiatrie)
        assertThat(psychiatre.sangFroid()).isEqualTo(4);
    }

    @Test
    void unArrangeurCruelPeutAssassinerLaVictime() {
        Character cruel = agent().withTrait(new Trait("Cruel", Set.of()));
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(cruel), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("a assassin\u00e9 la malheureuse victime"));
        // la victime coute 2 de sang-froid, le meurtre n'en coute pas
        assertThat(cruel.sangFroid()).isEqualTo(8);
    }

    @Test
    void lechecDuMeurtreInflige2D10EtTermineLaMissionEnEchec() {
        Character cruel = agent().withTrait(new Trait("Cruel", Set.of()));
        // les deux premiers tests reussissent, la discretion du meurtre echoue
        RandomSource des = new RandomSource() {
            private int jets = 0;
            @Override
            public int roll() {
                jets++;
                return jets <= 2 ? 1 : 100;
            }
        };
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(cruel), des, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("perd 2D10 vitalit\u00e9"));
        // 2 des de 100 : (100 % 10 + 1) x 2 = 2 degats
        assertThat(cruel.vitalite()).isEqualTo(8);
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Votre \u00e9quipe n\u2019a pas de quoi remplir la mission"));
    }

    @Test
    void uneEquipeSansSpecialisteAbandonneApresLApercuDeLaVictime() {
        Character amateur = agent();
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(amateur), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("L\u2019apparence horrible de la victime poss\u00e9d\u00e9e co\u00fbte 2 de sang-froid"));
        assertThat(amateur.sangFroid()).isEqualTo(8);
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Votre \u00e9quipe n\u2019a pas de quoi remplir la mission"));
    }

    @Test
    void lechecCritiqueDuTestDeRagotExclutLeJoueur() {
        Character enqueteur = new Character("a", "Alice");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), TOUJOURS_100, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(enqueteur.isExclu()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("s\u2019est fait d\u00e9tester par tout le voisinage"));
        assertThat(execution.isReussie()).isFalse();
    }

    @Test
    void uneGrandeEquipeUtiliseLeMeilleurRagotEtPasseAuSuivant() {
        Character mauvais = new Character("b", "Bob");
        Character bon = agent();
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(mauvais, bon), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Tante V\u00e9r\u00e9na tente le test de Ragot"));
    }

    private Character agent() {
        return new Character("v", "Tante V\u00e9r\u00e9na")
                .withSkill(Competence.RAGOT, 50)
                .withSkill(Competence.ELOQUENCE, 50)
                .withSkill(Competence.FORCE_MENTALE, 50)
                .withSkill(Competence.INTELLIGENCE, 50)
                .withSkill(Competence.DISCRETION, 50);
    }
}
