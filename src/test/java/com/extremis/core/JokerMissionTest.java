package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JokerMissionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource ALWAYS_HIGH = () -> 100;
    private static final RandomSource ALWAYS_LOW = () -> 1;

    private static Mission missionEpreuve() {
        return new Mission("mission-joker", "Mission joker", "brief", Lieu.PARIS_1ER,
                List.of(Competence.DISCRETION),
                List.of(
                        MissionEvent.builder("epreuve", "Epreuve")
                                .introduction("Une épreuve.")
                                .duree(Duration.ofSeconds(1))
                                .test(new SimpleTest(Competence.DISCRETION, 0))
                                .succes(MissionEvent.EffetReussite.builder()
                                        .texteSimple("Epreuve réussie.")
                                        .evenementSuivant("fin")
                                        .build())
                                .echec(MissionEvent.EffetEchec.builder()
                                        .texteSimple("Epreuve échouée.")
                                        .degats(5)
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

    private static Character agent() {
        return new Character("a", "Alice").withSkill(Competence.DISCRETION, 50);
    }

    private static List<String> textes(MissionExecution execution) {
        return execution.logLines().stream().map(LigneJournal::texte).toList();
    }

    @Test
    void unTestRatePeutEtreAnnuleEtRemplaceParUneReussite() {
        Character alice = agent();
        MissionExecution execution = MissionExecution.start(missionEpreuve(), List.of(alice), ALWAYS_HIGH, START);
        execution.advance(START.plus(Duration.ofSeconds(1)));

        assertThat(textes(execution)).anyMatch(l -> l.contains("échec"));
        assertThat(alice.vitalite()).isEqualTo(5);
        assertThat(execution.peutCorrigerDernierTest()).isTrue();
        MissionExecution.DernierTestRate dt = execution.dernierTestRate();
        assertThat(dt.evenementTitre()).isEqualTo("Epreuve");
        assertThat(dt.competence()).isEqualTo("Discrétion");
        assertThat(dt.testeurNom()).isEqualTo("Alice");

        boolean corrige = execution.corrigerDernierTestEnReussite(START.plus(Duration.ofSeconds(2)));

        assertThat(corrige).isTrue();
        assertThat(alice.vitalite()).isEqualTo(10);
        assertThat(execution.peutCorrigerDernierTest()).isFalse();
        assertThat(execution.dernierTestRate()).isNull();
        assertThat(textes(execution)).anyMatch(l -> l.contains("coïncidence calculée"));
        assertThat(textes(execution)).anyMatch(l -> l.contains("Epreuve réussie"));
        assertThat(textes(execution)).noneMatch(l -> l.contains("Epreuve échouée"));
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isTrue();
    }

    @Test
    void pasDeCorrectionPossibleQuandLeDernierTestEstReussi() {
        MissionExecution execution = MissionExecution.start(missionEpreuve(), List.of(agent()), ALWAYS_LOW, START);
        execution.advance(START.plus(Duration.ofSeconds(1)));

        assertThat(execution.peutCorrigerDernierTest()).isFalse();
        assertThat(execution.dernierTestRate()).isNull();
        assertThat(execution.corrigerDernierTestEnReussite(START.plus(Duration.ofSeconds(2)))).isFalse();
    }

    @Test
    void pasDeCorrectionPossibleAvantLePremierTest() {
        MissionExecution execution = MissionExecution.start(missionEpreuve(), List.of(agent()), ALWAYS_HIGH, START);
        execution.advance(START);

        assertThat(execution.peutCorrigerDernierTest()).isFalse();
        assertThat(execution.corrigerDernierTestEnReussite(START)).isFalse();
    }

    @Test
    void unMembrePeutEtreRemplaceParUnAutrePersonnage() {
        Character bob = new Character("b", "Bob").withSkill(Competence.DISCRETION, 20);
        MissionExecution execution = MissionExecution.start(missionEpreuve(), List.of(agent()), ALWAYS_LOW, START);

        assertThat(execution.remplacerMembre(0, bob)).isTrue();
        assertThat(execution.teamSnapshot()).extracting(Character::name).containsExactly("Bob");
        assertThat(textes(execution)).anyMatch(l -> l.contains("remplace Alice"));
        assertThat(execution.remplacerMembre(3, bob)).isFalse();
        assertThat(execution.remplacerMembre(-1, bob)).isFalse();
    }
}
