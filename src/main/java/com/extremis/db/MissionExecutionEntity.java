package com.extremis.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission_executions")
public class MissionExecutionEntity {
    public enum Status { IN_PROGRESS, FINISHED, ABANDONED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", length = 64)
    private String missionId;

    private String missionTitle;

    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;

    private Instant startedAt;

    private Instant finishedAt;

    private boolean teamWiped;

    private int nextEventIndex;

    private String lastEventTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "execution")
    private List<ExecutionTeamMemberEntity> team = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "execution")
    private List<ExecutionLogLineEntity> logLines = new ArrayList<>();

    public MissionExecutionEntity() {}

    public MissionExecutionEntity(String missionId, String missionTitle, Instant startedAt) {
        this.missionId = missionId;
        this.missionTitle = missionTitle;
        this.startedAt = startedAt;
    }

    public Long getId() { return id; }
    public String getMissionId() { return missionId; }
    public String getMissionTitle() { return missionTitle; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public boolean isTeamWiped() { return teamWiped; }
    public int getNextEventIndex() { return nextEventIndex; }
    public String getLastEventTime() { return lastEventTime; }
    public List<ExecutionTeamMemberEntity> getTeam() { return team; }
    public List<ExecutionLogLineEntity> getLogLines() { return logLines; }

    public void setStatus(Status status) { this.status = status; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public void setTeamWiped(boolean teamWiped) { this.teamWiped = teamWiped; }
    public void setNextEventIndex(int nextEventIndex) { this.nextEventIndex = nextEventIndex; }
    public void setLastEventTime(String lastEventTime) { this.lastEventTime = lastEventTime; }

    public void addLogLine(String line) {
        logLines.add(new ExecutionLogLineEntity(this, logLines.size(), line));
    }

    public void addTeamMember(ExecutionTeamMemberEntity member) {
        member.setExecution(this);
        team.add(member);
    }
}
