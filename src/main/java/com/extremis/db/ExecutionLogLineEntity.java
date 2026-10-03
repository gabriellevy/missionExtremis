package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_log_lines")
public class ExecutionLogLineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private MissionExecutionEntity execution;

    private int lineIndex;

    private String line;

    public ExecutionLogLineEntity() {}

    public ExecutionLogLineEntity(MissionExecutionEntity execution, int lineIndex, String line) {
        this.execution = execution;
        this.lineIndex = lineIndex;
        this.line = line;
    }

    public int getLineIndex() { return lineIndex; }
    public String getLine() { return line; }
}
