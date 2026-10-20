package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la mission "Rumeurs de possession démoniaque" (wiki, page du
 * même nom) : les règles spécifiques du début de mission doivent être
 * respectées.
 */
class RumeursPossessionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource TOUJOURS_100 = () -> 100;
    private static final RandomSource TOUJOURS_1 = () -> 1;

    @Test
    void laMissionSArreteSurUnSuccesSiTousLesTestsReussissent() {
        Character enqueteur = agent();
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Le foyer est purifié"));
    }

    @Test
    void lechecSimpleDuTestDeRagotRecommenceLaRecherche() {
        // cible 30, de 40 : echec simple
        RandomSource de40 = () -> 40;
        Character enqueteur = new Character("a", "Alice");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), de40, START);

        execution.advance(START.plus(Duration.ofDays(1)));
        assertThat(execution.isFinished()).isFalse();
        assertThat(enqueteur.isExclu()).isFalse();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Recommencer"));
        assertThat(execution.logLines())
                .filteredOn(l -> l.contains("=== Recherche du cas ==="))
                .hasSize(2);
    }

    @Test
    void lechecCritiqueDuTestDeRagotExclutLeJoueur() {
        Character enqueteur = new Character("a", "Alice");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), TOUJOURS_100, START);

        execution.advance(START.plus(Duration.ofDays(1)));
        assertThat(enqueteur.isExclu()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Alice s'est fait détester"));
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("La difficulté du test de l'événement exorcisme sera baissée de 10"));
    }

    @Test
    void laDifficulteBaisseeRendLexorcismePlusFacile() {
        Character enqueteur = agent();
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), TOUJOURS_100, START);

        // toutes les reussites sont impossibles avec un de de 100 mais
        // l'echec cumule des ajustements : on verifie l'ajustement cible
        execution.advance(START.plus(Duration.ofDays(1)));
        assertThat(execution.ajustementsDifficulte()).containsKey("exorcisme");
    }

    @Test
    void lExclusionDeTousLesJoueursTermineLaMission() {
        Character enqueteur = new Character("a", "Alice");
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(enqueteur), TOUJOURS_100, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(365)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Plus personne pour continuer"));
    }

    @Test
    void uneGrandeEquipeUtiliseLeMeilleurRagotEtPasseAuSuivant() {
        Character mauvais = new Character("b", "Bob");
        Character bon = agent();
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(mauvais, bon), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Tante Véréna tente le test de Ragot"));
    }

    private Character agent() {
        return new Character("v", "Tante Véréna")
                .withSkill(Competence.RAGOT, 50)
                .withSkill(Competence.ELOQUENCE, 50)
                .withSkill(Competence.FORCE_MENTALE, 50)
                .withSkill(Competence.VIGILANCE, 50)
                .withSkill(Competence.MAGIE, 50);
    }
}
