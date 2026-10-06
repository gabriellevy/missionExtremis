package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissionExecutionTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final RandomSource ALWAYS_HIGH = () -> 100;

    @Test
    void eventsAreOnlyResolvedWhenTheirScheduledTimeIsReached() {
        Mission mission = MissionsForTest.withDelays();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_HIGH, START);

        assertThat(execution.advance(START)).isFalse();
        assertThat(execution.eventsResolved()).isEqualTo(1);
        assertThat(execution.nextEventTime()).isEqualTo(START.plus(Duration.ofHours(4)));

        assertThat(execution.advance(START.plus(Duration.ofHours(3)))).isFalse();
        assertThat(execution.eventsResolved()).isEqualTo(1);

        assertThat(execution.advance(START.plus(Duration.ofHours(4)))).isFalse();
        assertThat(execution.eventsResolved()).isEqualTo(2);
        assertThat(execution.isFinished()).isFalse();

        assertThat(execution.advance(START.plus(Duration.ofHours(10)))).isTrue();
        assertThat(execution.eventsResolved()).isEqualTo(3);
        assertThat(execution.isFinished()).isTrue();
    }

    @Test
    void advanceIsIdempotentWithinTheSameTick() {
        Mission mission = MissionsForTest.withDelays();
        MissionExecution execution = MissionExecution.start(mission, List.of(agent()), ALWAYS_HIGH, START);

        Instant mid = START.plus(Duration.ofHours(5));
        execution.advance(mid);
        int resolved = execution.eventsResolved();
        execution.advance(mid);
        assertThat(execution.eventsResolved()).isEqualTo(resolved);
    }

    @Test
    void teamWipeStopsScheduling() {
        Character frail = new Character("a", "Alice");
        Mission brutal = new Mission(
                "brutal",
                "Brutale",
                "b",
                List.of(),
                List.of(
                        MissionEvent.builder("e1", "piege")
                                .test(new SimpleTest(Competence.DISCRETION, -1000))
                                .damageOnFailure(100)
                                .delayFromPrevious(Duration.ZERO)
                                .build(),
                        MissionEvent.builder("e2", "jamais atteint")
                                .delayFromPrevious(Duration.ofHours(1))
                                .build()));
        MissionExecution execution = MissionExecution.start(brutal, List.of(frail), ALWAYS_HIGH, START);

        assertThat(execution.advance(START.plus(Duration.ofHours(10)))).isTrue();
        assertThat(execution.teamWiped()).isTrue();
        assertThat(execution.logLines()).noneMatch(l -> l.contains("jamais atteint"));
    }

    private Character agent() {
        return new Character("a", "Alice").withSkill(Competence.DISCRETION, 50).withSkill(Competence.ARMES_CORPS_A_CORPS, 50);
    }

    private static final class MissionsForTest {
        static Mission withDelays() {
            return new Mission(
                    "m",
                    "Test",
                    "b",
                    List.of(),
                    List.of(
                            MissionEvent.builder("e1", "Reperage").delayFromPrevious(Duration.ZERO).build(),
                            MissionEvent.builder("e2", "Infiltration").delayFromPrevious(Duration.ofHours(4)).build(),
                            MissionEvent.builder("e3", "Confrontation").delayFromPrevious(Duration.ofHours(6)).build()));
        }
    }
}
