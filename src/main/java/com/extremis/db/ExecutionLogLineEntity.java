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

    /**
     * "jet" si la ligne est le resultat d'un test D100 affiche sous
     * forme de deux des, null sinon.
     */
    @Column(length = 16)
    private String type;

    private Integer deDizaines;

    private Integer deUnites;

    public ExecutionLogLineEntity() {}

    public ExecutionLogLineEntity(MissionExecutionEntity execution, int lineIndex, String line) {
        this.execution = execution;
        this.lineIndex = lineIndex;
        this.line = line;
    }

    public ExecutionLogLineEntity(MissionExecutionEntity execution, int lineIndex, String line,
                                  String type, Integer deDizaines, Integer deUnites) {
        this(execution, lineIndex, line);
        this.type = type;
        this.deDizaines = deDizaines;
        this.deUnites = deUnites;
    }

    public int getLineIndex() { return lineIndex; }
    public String getLine() { return line; }
    public String getType() { return type; }
    public Integer getDeDizaines() { return deDizaines; }
    public Integer getDeUnites() { return deUnites; }
}
