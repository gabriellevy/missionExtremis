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
        return new Mission(
                "gotheim",
                "Les fous de Gotheim",
                "Une secte agite la ville de Gotheim. Les arrangeurs doivent infiltrer puis neutraliser ses meneurs.",
                List.of(Skill.DISCRETION, Skill.COMBAT),
                List.of(
                        MissionEvent.builder("e1", "Arrivee a Gotheim, reperage de la secte.")
                                .delayFromPrevious(Duration.ZERO)
                                .build(),
                        MissionEvent.builder("e2", "Infiltration du meeting nocturne.")
                                .test(new SimpleTest(Skill.DISCRETION, 60))
                                .damageOnFailure(2)
                                .rewardOnSuccess("Toge de sectateur")
                                .delayFromPrevious(Duration.ofHours(4))
                                .build(),
                        MissionEvent.builder("e3", "Confrontation avec les meneurs.")
                                .test(new SimpleTest(Skill.COMBAT, 70))
                                .damageOnFailure(4)
                                .delayFromPrevious(Duration.ofHours(6))
                                .build()));
    }
}
