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

    private int health;

    private boolean alive;

    public ExecutionTeamMemberEntity() {}

    public ExecutionTeamMemberEntity(String characterId, String name, int health, boolean alive) {
        this.characterId = characterId;
        this.name = name;
        this.health = health;
        this.alive = alive;
    }

    public String getCharacterId() { return characterId; }
    public String getName() { return name; }
    public int getHealth() { return health; }
    public boolean isAlive() { return alive; }
    public MissionExecutionEntity getExecution() { return execution; }

    public void setExecution(MissionExecutionEntity execution) { this.execution = execution; }
    public void setHealth(int health) { this.health = health; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
