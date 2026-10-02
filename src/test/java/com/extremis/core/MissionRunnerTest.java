package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissionRunnerTest {

    private static final RandomSource ALWAYS_HIGH = () -> 100;
    private static final RandomSource ALWAYS_LOW = () -> 1;

    @Test
    void successfulTestGrantsRewardAndNoDamage() {
        Character agent = new Character("a", "Alice").withSkill(Skill.DISCRETION, 50);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_HIGH).run(mission, List.of(agent), m -> {});
        assertThat(report.teamWiped()).isFalse();
        assertThat(agent.inventory()).contains("Toge de sectateur");
        assertThat(agent.health()).isEqualTo(10);
    }

    @Test
    void failingTestDamagesTheTeam() {
        Character agent = new Character("a", "Alice").withSkill(Skill.DISCRETION, 10);
        Mission mission = mission();
        MissionReport report = new MissionRunner(ALWAYS_LOW).run(mission, List.of(agent), m -> {});
        assertThat(agent.health()).isLessThan(10);
        assertThat(report.log()).contains("echoue");
    }

    @Test
    void missingSkillMeansAutomaticFailure() {
        Character agent = new Character("a", "Alice").withSkill(Skill.ERUDITION, 90);
        Mission mission = mission();
        new MissionRunner(ALWAYS_HIGH).run(mission, List.of(agent), m -> {});
        assertThat(agent.inventory()).isEmpty();
    }

    @Test
    void teamWipeStopsTheMission() {
        Character agent = new Character("a", "Alice").withSkill(Skill.DISCRETION, 1);
        Mission brutal = new Mission(
                "brutal",
                "Mission brutale",
                "Test",
                List.of(),
                List.of(
                        MissionEvent.builder("e1", "piege")
                                .test(new SimpleTest(Skill.DISCRETION, 1000))
                                .damageOnFailure(100)
                                .build(),
                        MissionEvent.builder("e2", "jamais atteint").build()));
        MissionReport report = new MissionRunner(ALWAYS_HIGH).run(brutal, List.of(agent), m -> {});
        assertThat(report.teamWiped()).isTrue();
        assertThat(report.log()).doesNotContain("jamais atteint");
    }

    private Mission mission() {
        return new Mission(
                "m",
                "Test mission",
                "Brief",
                List.of(Skill.DISCRETION),
                List.of(MissionEvent.builder("e1", "Infiltration")
                        .test(new SimpleTest(Skill.DISCRETION, 60))
                        .damageOnFailure(3)
                        .rewardOnSuccess("Toge de sectateur")
                        .build()));
    }
}
