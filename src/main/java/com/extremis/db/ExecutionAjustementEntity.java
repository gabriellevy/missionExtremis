package com.extremis.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_ajustements")
public class ExecutionAjustementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private MissionExecutionEntity execution;

    @Column(name = "evenement_id", length = 128)
    private String evenementId;

    private int ajustement;

    public ExecutionAjustementEntity() {}

    public ExecutionAjustementEntity(MissionExecutionEntity execution, String evenementId, int ajustement) {
        this.execution = execution;
        this.evenementId = evenementId;
        this.ajustement = ajustement;
    }

    public String getEvenementId() { return evenementId; }
    public int getAjustement() { return ajustement; }
    public MissionExecutionEntity getExecution() { return execution; }
}
