package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissionRunnerTest {

    private static final RandomSource ALWAYS_HIGH = () -> 100;
    private static final RandomSource ALWAYS_LOW = () -> 1;

    @Test
    void successfulTestGrantsRewardAndNoDamage() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 50);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_LOW).run(mission, List.of(agent), m -> {});
        assertThat(report.teamWiped()).isFalse();
        assertThat(agent.inventory()).contains("Toge de sectateur");
        assertThat(agent.vitalite()).isEqualTo(10);
    }

    @Test
    void failingTestDamagesTheTeam() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 0);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_HIGH).run(mission, List.of(agent), m -> {});
        assertThat(agent.vitalite()).isLessThan(10);
        assertThat(report.log()).contains("echoue");
    }

    @Test
    void untrainedCharacterStillUsesSkillBase() {
        Character agent = new Character("a", "Alice").withSkill(Competence.INTUITION, 90);
        Mission mission = mission();
        new MissionRunner(ALWAYS_HIGH).run(mission, List.of(agent), m -> {});
        assertThat(agent.inventory()).isEmpty();
    }

    @Test
    void teamWipeStopsTheMission() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 0);
        Mission brutal = new Mission(
                "brutal",
                "Mission brutale",
                "Test",
                List.of(),
                List.of(
                        MissionEvent.builder("e1", "piege")
                                .test(new SimpleTest(Competence.DISCRETION, -1000))
                                .damageOnFailure(100)
                                .build(),
                        MissionEvent.builder("e2", "jamais atteint").build()));
        MissionReport report = new MissionRunner(ALWAYS_HIGH).run(brutal, List.of(agent), m -> {});
        assertThat(report.teamWiped()).isTrue();
        assertThat(report.log()).doesNotContain("jamais atteint");
    }

    @Test
    void skillTestIsBasePlusSkillUnderD100() {
        Character prodigy = new Character("p", "Prodige").withSkillBase(40).withSkill(Competence.DISCRETION, 10);
        assertThat(prodigy.skillTarget(Competence.DISCRETION)).isEqualTo(50);
        SimpleTest test = new SimpleTest(Competence.DISCRETION, 0);
        assertThat(test.attempt(prodigy, () -> 49)).isTrue();
        assertThat(test.attempt(prodigy, () -> 50)).isFalse();
        assertThat(test.attempt(prodigy, () -> 100)).isFalse();
    }

    @Test
    void untrainedCharacterCanSucceedWithBaseOnly() {
        Character novice = new Character("n", "Novice");
        SimpleTest test = new SimpleTest(Competence.TIR, 0);
        assertThat(test.attempt(novice, () -> 29)).isTrue();
        assertThat(test.attempt(novice, () -> 30)).isFalse();
    }

    @Test
    void difficultyShiftsTheTarget() {
        Character agent = new Character("a", "Alice").withSkill(Competence.DISCRETION, 20);
        SimpleTest hard = new SimpleTest(Competence.DISCRETION, -20);
        assertThat(hard.target(agent)).isEqualTo(30);
        SimpleTest easy = new SimpleTest(Competence.DISCRETION, 10);
        assertThat(easy.target(agent)).isEqualTo(60);
    }

    private Mission mission() {
        return new Mission(
                "m",
                "Test mission",
                "Brief",
                List.of(Competence.DISCRETION),
                List.of(MissionEvent.builder("e1", "Infiltration")
                        .test(new SimpleTest(Competence.DISCRETION, 0))
                        .damageOnFailure(3)
                        .rewardOnSuccess("Toge de sectateur")
                        .build()));
    }
}
