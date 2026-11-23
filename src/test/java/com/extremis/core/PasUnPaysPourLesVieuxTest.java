package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la mission "Pas un pays pour les vieux" (wiki, page du meme
 * nom) : la mission n'est reussie que si Chigur et Loreline sont
 * abattus ou arretes tous les deux.
 */
class PasUnPaysPourLesVieuxTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void laMissionReussitSiLorelineSeRendPuisSiChigurEstAbattu() {
        Character agent = agent();
        // ragot, interrogation, localisation, eloquence, discretion et tir reussissent
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 1, 1, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Loreline se rend et vous avez r\u00e9cup\u00e9r\u00e9 "
                        + "l'argent du cartel."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Les arrangeurs abattent Chigur."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("la mission est accomplie."));
        assertThat(agent.inventory()).contains("L'argent du cartel (+10 Richesse)");
    }

    @Test
    void laMissionReussitSiChigurEstAbattuAvantDeRetrouverLoreline() {
        Character agent = agent();
        // ragot reussit, l'interrogatoire echoue et ouvre la traque, puis
        // la traque, le tir et la sommation reussissent
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 100, 1, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Vous abattez le tueur Chigur."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Chigur et Loreline sont neutralis\u00e9s : victoire."));
        assertThat(agent.inventory()).contains("L'argent du cartel (+10 Richesse)");
    }

    @Test
    void unArrangeurCupidePeutVolerLArgentMaisLaMissionEchoueSiChigurSurvit() {
        Character cupide = agent().withTrait(new Trait("Cupide", Set.of()));
        // ragot, interrogation et localisation reussissent, la tentation
        // fait echouer la force mentale, le vol reussit puis tout le
        // monde reste discret : Chigur survit et la mission echoue
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(cupide),
                script(1, 1, 1, 100, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Les arrangeurs abattent Loreline et s'emparent "
                        + "de l'argent du cartel."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Le tueur a surv\u00e9cu : les arrangeurs ont \u00e9chou\u00e9"));
        assertThat(cupide.inventory()).contains("L'argent du cartel (+10 Richesse)");
    }

    @Test
    void lechecDeDiscretionProvoqueLAttaqueDeChigur() {
        Character alice = agent();
        Character bob = agent("b", "Bob");
        // ragot, interrogation et localisation reussissent, la sommation
        // reussit, Bob est repere a la discretion, l'attaque de Chigur
        // reussit : victoire
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(alice, bob),
                script(1, 1, 1, 1, 1, 100, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Le tueur a rep\u00e9r\u00e9 les arrangeurs."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Les arrangeurs abattent Chigur."));
    }

    @Test
    void lechecDuTirFaceAChigurFaitPasserUnTestDeChanceATous() {
        Character agent = agent();
        Character bob = agent("b", "Bob");
        // ragot reussit, l'interrogatoire echoue, la traque reussit,
        // le tir contre Chigur echoue, puis les deux arrangeurs ratent
        // leur test de chance (3D10 chacun), la reprise d'interrogatoire
        // ne donne rien et la deuxieme traque reussit, le tir aussi :
        // victoire apres avoir abattu Chigur
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent, bob),
                script(1, 100, 1, 100, 100, 100, 100, 100, 100, 100, 100, 100, 1, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(2)))).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Celui qui rate son test de chance subit 3D10 "
                        + "de d\u00e9g\u00e2ts."));
        assertThat(textesDuJournal(execution).stream()
                .filter(l -> l.contains("subit 3D10 degats")).count()).isEqualTo(2);
    }

    @Test
    void leTroisiemeEchecDeTraqueLaisseChigurSEchapperEtFaitEchouerLaMission() {
        Character agent = agent();
        // ragot reussit, l'interrogatoire echoue, les trois enquetes
        // de traque echouent : Chigur echappe aux arrangeurs
        Mission mission = Missions.pasUnPaysPourLesVieuxRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 100, 100, 100, 100), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Plus rien \u00e0 faire : Chigur vous a \u00e9chapp\u00e9, "
                        + "la mission \u00e9choue."));
        assertThat(agent.inventory()).isEmpty();
    }

    private Character agent() {
        return agent("a", "Alice");
    }

    private Character agent(String id, String nom) {
        return new Character(id, nom)
                .withSkill(Competence.RAGOT, 50)
                .withSkill(Competence.ELOQUENCE, 50)
                .withSkill(Competence.EVALUATION, 50)
                .withSkill(Competence.TIR, 50)
                .withSkill(Competence.FORCE_MENTALE, 50)
                .withSkill(Competence.DISCRETION, 50)
                .withSkill(Competence.CHANCE, 50);
    }

    private static RandomSource script(int... jets) {
        return new RandomSource() {
            private int i = 0;

            @Override
            public int roll() {
                return jets[Math.min(i++, jets.length - 1)];
            }
        };
    }

    private static List<String> textesDuJournal(MissionExecution execution) {
        return execution.logLines().stream().map(LigneJournal::texte).toList();
    }
}
