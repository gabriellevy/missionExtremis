package com.extremis.catalog;

import com.extremis.core.Mission;
import com.extremis.core.MissionEvent;
import com.extremis.core.SimpleTest;
import com.extremis.core.Skill;

import java.time.Duration;
import java.util.List;

public final class Missions {
    private Missions() {}

    public static Mission foolsOfGotheim() {
        return build(Duration.ofHours(4), Duration.ofHours(6));
    }

    public static Mission foolsOfGotheimRapide() {
        return build(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static Mission build(Duration delay2, Duration delay3) {
        return new Mission(
                "gotheim",
                "Les fous de Gotheim",
                "Une secte agite la ville de Gotheim. Les arrangeurs doivent infiltrer puis neutraliser ses meneurs.",
                List.of(Skill.DISCRETION, Skill.ARMES_CORPS_A_CORPS),
                List.of(
                        MissionEvent.builder("e1", "Arrivee a Gotheim, reperage de la secte.")
                                .delayFromPrevious(Duration.ZERO)
                                .build(),
                        MissionEvent.builder("e2", "Infiltration du meeting nocturne.")
                                .test(new SimpleTest(Skill.DISCRETION, -10))
                                .damageOnFailure(2)
                                .rewardOnSuccess("Toge de sectateur")
                                .delayFromPrevious(delay2)
                                .build(),
                        MissionEvent.builder("e3", "Confrontation avec les meneurs.")
                                .test(new SimpleTest(Skill.ARMES_CORPS_A_CORPS, -20))
                                .damageOnFailure(4)
                                .delayFromPrevious(delay3)
                                .build()));
    }
}
