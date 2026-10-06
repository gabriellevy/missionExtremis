package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_team_members")
public class ExecutionTeamMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private MissionExecutionEntity execution;

    private String characterId;

    private String name;

    private int vitalite;

    private boolean alive;

    public ExecutionTeamMemberEntity() {}

    public ExecutionTeamMemberEntity(String characterId, String name, int vitalite, boolean alive) {
        this.characterId = characterId;
        this.name = name;
        this.vitalite = vitalite;
        this.alive = alive;
    }

    public String getCharacterId() { return characterId; }
    public String getName() { return name; }
    public int getVitalite() { return vitalite; }
    public boolean isAlive() { return alive; }
    public MissionExecutionEntity getExecution() { return execution; }

    public void setExecution(MissionExecutionEntity execution) { this.execution = execution; }
    public void setVitalite(int vitalite) { this.vitalite = vitalite; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
