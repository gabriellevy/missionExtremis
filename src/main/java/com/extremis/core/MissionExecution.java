package com.extremis.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution incrementale d'une mission : chaque evenement ne se resout
 * que lorsque l'horloge atteint son heure planifiee (delais cumules).
 */
public final class MissionExecution {
    private final Mission mission;
    private final List<Character> team;
    private final RandomSource random;
    private final List<Instant> scheduledTimes;
    private final List<String> logLines = new ArrayList<>();
    private int nextEvent = 0;
    private boolean finished;

    private MissionExecution(Mission mission, List<Character> team, RandomSource random, List<Instant> scheduledTimes) {
        this.mission = mission;
        this.team = team;
        this.random = random;
        this.scheduledTimes = scheduledTimes;
    }

    public static MissionExecution start(Mission mission, List<Character> team, RandomSource random, Instant startTime) {
        List<Instant> times = new ArrayList<>();
        Instant cursor = startTime;
        for (int i = 0; i < mission.events().size(); i++) {
            Duration delay = mission.events().get(i).delayFromPrevious();
            cursor = cursor.plus(delay);
            times.add(cursor);
        }
        return new MissionExecution(mission, team, random, times);
    }

    /**
     * Resout tous les evenements dont l'heure planifiee est atteinte.
     *
     * @return true si la mission est terminee (tous les evenements ou equipe eliminee)
     */
    public boolean advance(Instant now) {
        while (nextEvent < mission.events().size() && !teamWiped()) {
            if (scheduledTimes.get(nextEvent).isAfter(now)) {
                return false;
            }
            MissionEvent event = mission.events().get(nextEvent);
            event.resolve(team, random, logLines::add);
            nextEvent++;
        }
        finished = true;
        return true;
    }

    public boolean teamWiped() {
        return team.stream().noneMatch(Character::isAlive);
    }

    public boolean isFinished() {
        return finished;
    }

    public Instant nextEventTime() {
        if (nextEvent >= scheduledTimes.size()) {
            return null;
        }
        return scheduledTimes.get(nextEvent);
    }

    public int eventsResolved() {
        return nextEvent;
    }

    public List<String> logLines() {
        return List.copyOf(logLines);
    }

    public void skipEvent() {
        if (nextEvent < mission.events().size()) {
            nextEvent++;
        }
    }

    public void markDead(String characterId) {
        team.stream().filter(c -> c.id().equals(characterId)).findFirst().ifPresent(Character::kill);
    }

    public List<Character> teamSnapshot() {
        return List.copyOf(team);
    }

    public MissionReport report() {
        return new MissionReport(
                mission.id(),
                team.stream().filter(c -> !c.isAlive()).map(Character::name).toList(),
                team.stream().filter(Character::isAlive).count(),
                teamWiped(),
                String.join("\n", logLines));
    }
}
