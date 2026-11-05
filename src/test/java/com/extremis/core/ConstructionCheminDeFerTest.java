package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la mission "Construction de chemin de fer" (wiki, page du
 * meme nom) et des boites d'evenements generiques qu'elle reference
 * ("Marche epuisante", "Travail epuisant", "Escarmouches de bandits").
 */
class ConstructionCheminDeFerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource TOUJOURS_1 = () -> 1;

    @Test
    void laMissionReussitParLaVoieDirecte() {
        Character agent = agent();
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent), TOUJOURS_1, START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("R\u00e9ussite : vous avez atteint l'objectif."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Vous avez atteint le chantier."));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Le chantier a \u00e9t\u00e9 men\u00e9 \u00e0 son terme."));
        assertThat(agent.vitalite()).isEqualTo(10);
    }

    @Test
    void laDesertionFaitParticiperLesArrangeursAuTravail() {
        Character agent = agent();
        // periple ok, commandement echoue, puis tous les travaux et la vigilance reussissent
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 100, 1, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("ont d\u00e9sert\u00e9"));
        // les trois instances reutilisables de la boite "Travail epuisant" ont joue
        assertThat(textesDuJournal(execution).stream()
                .filter(l -> l.contains("Travail termin\u00e9.")).count()).isEqualTo(3);
        assertThat(mission.evenementsParId()).containsKeys("travail-desertion-travail",
                "travail-supplementaire-travail", "travail-travail");
    }

    @Test
    void lechecDuPeripleBoucleLaMarcheEpuisanteAvecUnBonusDe20() {
        Character agent = agent();
        // periple echoue, endurance et force mentale reussissent, periple retente avec +20 reussit
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(100, 1, 1, 50, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("vous perdez dans la for\u00eat glaciale"));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("marche-periple sera baiss\u00e9e de 20"));
    }

    @Test
    void lechecDeVigilanceDeclencheLesEscarmouchesDeBanditsAvecUneArmeDeTir() {
        Character tireur = agent();
        tireur.addItem("Fusil");
        // periple, commandement et travail ok, vigilance echoue, le tir abat les bandits
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(tireur),
                script(1, 1, 1, 100, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("poss\u00e8de une arme de tir"));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("fauch\u00e9s par vos attaques"));
    }

    @Test
    void sansArmeDeTirLesBanditsBlesseUnPersonnageAuHasardEn3D10() {
        Character agent = agent();
        // vigilance echoue, protection du consul echoue (3D10 sur un perso au hasard : 2+2+2),
        // puis le corps a corps abat les bandits
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 1, 1, 100, 100, 1, 1, 1, 1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("prot\u00e9ger le consul"));
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("subit 3D10 degats (6, vitalite restante : 4)"));
        assertThat(agent.vitalite()).isEqualTo(4);
    }

    @Test
    void leTravailEpuisantInflige5DegatsEnEchecCritique() {
        Character agent = new Character("a", "Alice")
                .withSkill(Competence.PERIPLE, 50)
                .withSkill(Competence.COMMANDEMENT, 50)
                .withSkill(Competence.VIGILANCE, 50);
        // periple et commandement ok, travail : echec critique (cible 30, de 100), vigilance ok
        Mission mission = Missions.constructionDeCheminDeFerRapide();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent),
                script(1, 1, 100, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("subit 5 degats (vitalite restante : 5)"));
        assertThat(agent.vitalite()).isEqualTo(5);
    }

    private Character agent() {
        return new Character("a", "Alice")
                .withSkill(Competence.PERIPLE, 50)
                .withSkill(Competence.COMMANDEMENT, 50)
                .withSkill(Competence.ENDURANCE, 50)
                .withSkill(Competence.FORCE_MENTALE, 50)
                .withSkill(Competence.VIGILANCE, 50)
                .withSkill(Competence.TIR, 50)
                .withSkill(Competence.PROTECTION_DU_CONSUL, 50)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 50);
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
