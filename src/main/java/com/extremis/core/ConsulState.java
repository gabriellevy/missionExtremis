package com.extremis.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConsulState {
    private final List<Character> roster = new ArrayList<>();
    private final List<Mission> missions = new ArrayList<>();
    private int jokers = 1;
    private int simulationsLeft = 3;
    private Instant lastRecruitment;
    private Instant lastMissionTaken;
    private static final Duration RECRUITMENT_COOLDOWN = Duration.ofHours(24);
    private static final Duration MISSION_COOLDOWN = Duration.ofHours(24);

    public boolean canRecruit(Instant now) {
        return lastRecruitment == null || !now.isBefore(lastRecruitment.plus(RECRUITMENT_COOLDOWN));
    }

    public void recruit(Character character, Instant now) {
        if (!canRecruit(now)) {
            throw new IllegalStateException("Un seul recrutement par 24h.");
        }
        roster.add(character);
        lastRecruitment = now;
    }

    public List<Character> roster() { return roster; }
    public List<Mission> missions() { return missions; }
    public int jokers() { return jokers; }
    public int simulationsLeft() { return simulationsLeft; }

    public void addJoker() { jokers++; }

    public boolean canTakeMission(Instant now) {
        return lastMissionTaken == null || !now.isBefore(lastMissionTaken.plus(MISSION_COOLDOWN));
    }

    public void takeMission(Mission mission, Instant now) {
        if (!canTakeMission(now)) {
            throw new IllegalStateException("Une seule mission par 24h.");
        }
        lastMissionTaken = now;
    }

    public boolean spendJoker() {
        if (jokers <= 0) {
            return false;
        }
        jokers--;
        return true;
    }

    public Optional<Mission> missionById(String id) {
        return missions.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public Optional<Character> characterById(String id) {
        return roster.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public void offerMission(Mission mission) {
        missions.add(mission);
    }
}
