package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissionExecutionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource ALWAYS_HIGH = () -> 100;
    private static final RandomSource ALWAYS_LOW = () -> 1;

    @Test
    void lIntroductionSAfficheDesLeDebutEtLEffetApresLaDuree() {
        Mission mission = Missions.fousDeGotheimRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_HIGH, START);

        // premiere duree : l'introduction du premier evenement est publiee,
        // pas encore son effet
        assertThat(execution.advance(START)).isFalse();
        assertThat(execution.logLines()).anyMatch(l -> l.contains("=== Arrivee a Gotheim ==="));
        assertThat(execution.logLines()).noneMatch(l -> l.contains("tente le test"));
    }

    @Test
    void unTestNeSAfficheQuApresLaDureeDeLEvenement() {
        Mission mission = Missions.fousDeGotheim();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_HIGH, START);

        assertThat(execution.advance(START.plus(Duration.ofHours(4)).minusSeconds(1))).isFalse();
        assertThat(execution.logLines()).noneMatch(l -> l.contains("tente le test de Discrétion"));

        assertThat(execution.advance(START.plus(Duration.ofHours(4)))).isFalse();
        assertThat(execution.logLines()).anyMatch(l -> l.contains("tente le test de Discrétion"));
    }

    @Test
    void leMeilleurPJEnCompetenceTenteLeTest() {
        Character menteur = new Character("a", "Alice").withSkill(Competence.RAGOT, 60);
        Character amateur = new Character("b", "Bob").withSkill(Competence.RAGOT, 10);
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(amateur, menteur), ALWAYS_LOW, START);

        execution.advance(START.plus(Duration.ofDays(1)));
        assertThat(execution.logLines()).anyMatch(l -> l.startsWith("[") && l.contains("Alice tente le test de Ragot"));
    }

    @Test
    void laReussiteDAmenEvenementsSuivantJusquaLaVictoire() {
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_LOW, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(365)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("Vous avez trouvé la victime"));
    }

    @Test
    void unEchecCritiqueExclutLeJoueurDeLAventure() {
        Character menteur = new Character("a", "Alice").withSkill(Competence.RAGOT, 0);
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(menteur), ALWAYS_HIGH, START);

        execution.advance(START.plus(Duration.ofDays(1)));
        assertThat(menteur.isExclu()).isTrue();
        assertThat(execution.logLines()).anyMatch(l -> l.contains("est exclu de l'aventure"));
    }

    @Test
    void lechecSimpleRecommenceLaBoucleEtCumuleLaDifficulteBaissee() {
        // cible 30 : un seul jet a 40 (echec simple, pas d'exclusion)
        RandomSource deUnique = new RandomSource() {
            private boolean joue = false;
            @Override
            public int roll() {
                if (!joue) { joue = true; return 40; }
                return 1;
            }
        };
        Character menteur = new Character("a", "Alice").withSkill(Competence.RAGOT, 0);
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(menteur), deUnique, START);

        // on n'avance que d'une duree d'evenement : un seul passage resolu
        execution.advance(START.plus(Duration.ofSeconds(1)));
        assertThat(menteur.isExclu()).isFalse();
        assertThat(execution.isFinished()).isFalse();
        // l'evenement suivant est a nouveau "recherche-du-cas"
        assertThat(execution.evenementCourant()).isEqualTo("recherche-du-cas");
        // l'ajustement de difficulte vise l'exorcisme
        assertThat(execution.ajustementsDifficulte()).containsEntry("exorcisme", 10);
        assertThat(execution.logLines())
                .anyMatch(l -> l.contains("La difficulté du test de l'événement exorcisme sera baissée de 10"));
        // l'introduction du nouveau passage est deja publiee
        assertThat(execution.logLines())
                .filteredOn(l -> l.contains("=== Recherche du cas ==="))
                .hasSize(2);
        // deuxieme passage : le de de 1 reussit, la boucle s'arrete
        execution.advance(START.plus(Duration.ofSeconds(2)));
        assertThat(execution.evenementCourant()).isEqualTo("convaincre-la-famille");
        assertThat(execution.logLines())
                .filteredOn(l -> l.contains("=== Recherche du cas ==="))
                .hasSize(2);
    }

    @Test
    void lEquipeEntierementExclueTermineLaMissionEnEchec() {
        Character menteur = new Character("a", "Alice").withSkill(Competence.RAGOT, 0);
        Mission mission = Missions.rumeursDePossessionDemoniaqueRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(menteur), ALWAYS_HIGH, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(365)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
    }

    @Test
    void advanceEstIdempotentDansLeMemeTick() {
        Mission mission = Missions.fousDeGotheimRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_HIGH, START);
        execution.advance(START);
        int lignes = execution.logLines().size();
        execution.advance(START);
        assertThat(execution.logLines()).hasSize(lignes);
    }

    private Character agent() {
        return new Character("a", "Alice")
                .withSkill(Competence.DISCRETION, 50)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 50)
                .withSkill(Competence.RAGOT, 40)
                .withSkill(Competence.ELOQUENCE, 40)
                .withSkill(Competence.FORCE_MENTALE, 40)
                .withSkill(Competence.VIGILANCE, 40)
                .withSkill(Competence.MAGIE, 40);
    }
}
