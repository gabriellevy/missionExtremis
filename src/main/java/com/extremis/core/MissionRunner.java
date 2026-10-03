package com.extremis.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MissionRunner {
    private final RandomSource random;

    public MissionRunner(RandomSource random) {
        this.random = random;
    }

    public MissionReport run(Mission mission, List<Character> team, Consumer<String> log) {
        List<String> lines = new ArrayList<>();
        for (MissionEvent event : mission.events()) {
            event.resolve(team, random, lines::add);
            if (team.stream().noneMatch(Character::isAlive)) {
                break;
            }
        }
        String summary = String.join("\n", lines);
        log.accept(summary);
        boolean teamWiped = team.stream().noneMatch(Character::isAlive);
        return new MissionReport(
                mission.id(),
                team.stream().filter(c -> !c.isAlive()).map(Character::name).toList(),
                team.stream().filter(Character::isAlive).count(),
                teamWiped,
                summary);
    }
}
