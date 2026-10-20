package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissionRunnerTest {

    private static final RandomSource ALWAYS_HIGH = () -> 100;
    private static final RandomSource ALWAYS_LOW = () -> 1;

    @Test
    void laReussitePrincipaleProgresseEtAccordeDesRecompenses() {
        Character agent = new Character("a", "Alice")
                .withSkill(Competence.DISCRETION, 50)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 50);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_LOW).run(mission, List.of(agent), m -> {});
        assertThat(report.teamWiped()).isFalse();
        assertThat(agent.inventory()).contains("Toge de sectateur");
        assertThat(agent.vitalite()).isEqualTo(10);
    }

    @Test
    void lechecInfligeLesDegats() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 0);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_HIGH).run(mission, List.of(agent), m -> {});
        assertThat(agent.vitalite()).isLessThan(10);
        assertThat(report.log()).contains("obtient un echec");
    }

    @Test
    void leD100SeJoueSousLaCibleBasePlusCompetence() {
        Character prodige = new Character("p", "Prodige").withSkillBase(40).withSkill(Competence.DISCRETION, 10);
        assertThat(prodige.skillTarget(Competence.DISCRETION)).isEqualTo(50);
        SimpleTest test = new SimpleTest(Competence.DISCRETION, 0);
        assertThat(test.attempt(prodige, () -> 49)).isEqualTo(ResultatTest.REUSSITE);
        assertThat(test.attempt(prodige, () -> 50)).isEqualTo(ResultatTest.ECHEC);
        assertThat(test.attempt(prodige, () -> 51)).isEqualTo(ResultatTest.ECHEC);
    }

    @Test
    void lesMargesCritiquesSontDetectees() {
        // Brigandyne : qualite de reussite = cible - de, gravite de l'echec
        // = de - cible ; critique a partir de 50 (echec retentissant 50+)
        // cible 50 : reussite critique si de <= 0 (impossible ici), echec
        // critique si de >= 100
        Character agent = new Character("a", "Alice").withSkillBase(50);
        SimpleTest test = new SimpleTest(Competence.TIR, 0);
        assertThat(test.attempt(agent, () -> 1)).isEqualTo(ResultatTest.REUSSITE);
        assertThat(test.attempt(agent, () -> 49)).isEqualTo(ResultatTest.REUSSITE);
        assertThat(test.attempt(agent, () -> 51)).isEqualTo(ResultatTest.ECHEC);
        assertThat(test.attempt(agent, () -> 99)).isEqualTo(ResultatTest.ECHEC);
        assertThat(test.attempt(agent, () -> 100)).isEqualTo(ResultatTest.ECHEC_CRITIQUE);
        // avec une cible de 80 la reussite critique devient possible
        Character prodige = new Character("p", "Prodige").withSkillBase(80);
        assertThat(test.attempt(prodige, () -> 30)).isEqualTo(ResultatTest.REUSSITE_CRITIQUE);
        assertThat(test.attempt(prodige, () -> 31)).isEqualTo(ResultatTest.REUSSITE);
    }

    @Test
    void unPersonnageSansCompetenceUtiliseSaBase() {
        Character novice = new Character("n", "Novice");
        SimpleTest test = new SimpleTest(Competence.TIR, 0);
        assertThat(test.attempt(novice, () -> 29)).isEqualTo(ResultatTest.REUSSITE);
        assertThat(test.attempt(novice, () -> 30)).isEqualTo(ResultatTest.ECHEC);
    }

    @Test
    void laDifficulteDecaleLaCible() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 20);
        SimpleTest dur = new SimpleTest(Competence.DISCRETION, -20);
        assertThat(dur.target(agent)).isEqualTo(30);
        SimpleTest facile = new SimpleTest(Competence.DISCRETION, 10);
        assertThat(facile.target(agent)).isEqualTo(60);
    }

    private Mission mission() {
        return new Mission(
                "m",
                "Test mission",
                "Brief",
                List.of(Competence.DISCRETION),
                List.of(MissionEvent.builder("e1", "Infiltration")
                        .introduction("Les arrangeurs s'infiltrent.")
                        .test(new SimpleTest(Competence.DISCRETION, 0))
                        .succes(MissionEvent.EffetReussite.builder()
                                .texteSimple("Les arrangeurs passent inapercus.")
                                .recompense("Toge de sectateur")
                                .evenementSuivant("e2")
                                .build())
                        .echec(MissionEvent.EffetEchec.builder()
                                .texteSimple("Les gardes reparent les arrangeurs.")
                                .degats(3)
                                .evenementSuivant("e2")
                                .build())
                        .build(),
                        MissionEvent.builder("e2", "Confrontation")
                                .introduction("Les arrangeurs affrontent les meneurs.")
                                .test(new SimpleTest(Competence.ARMES_CORPS_A_CORPS, 0))
                                .succes(MissionEvent.EffetReussite.builder()
                                        .texteSimple("Les meneurs sont neutralises.")
                                        .build())
                                .echec(MissionEvent.EffetEchec.builder()
                                        .texteSimple("Les meneurs ont le dessus.")
                                        .degats(3)
                                        .build())
                                .build()));
    }
}
