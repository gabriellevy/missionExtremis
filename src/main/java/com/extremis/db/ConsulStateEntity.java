package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "consul_state")
public class ConsulStateEntity {
    @Id
    private String id = "consul";

    private int jokers = 1;

    private int simulationsLeft = 3;

    private Instant lastRecruitment;

    private Instant lastMissionTaken;

    private Instant gameClock;

    public String getId() { return id; }
    public int getJokers() { return jokers; }
    public int getSimulationsLeft() { return simulationsLeft; }
    public Instant getLastRecruitment() { return lastRecruitment; }
    public Instant getLastMissionTaken() { return lastMissionTaken; }
    public Instant getGameClock() { return gameClock; }

    public void setJokers(int jokers) { this.jokers = jokers; }
    public void setSimulationsLeft(int simulationsLeft) { this.simulationsLeft = simulationsLeft; }
    public void setLastRecruitment(Instant lastRecruitment) { this.lastRecruitment = lastRecruitment; }
    public void setLastMissionTaken(Instant lastMissionTaken) { this.lastMissionTaken = lastMissionTaken; }
    public void setGameClock(Instant gameClock) { this.gameClock = gameClock; }
}
