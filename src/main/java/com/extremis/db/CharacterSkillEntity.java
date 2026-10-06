package com.extremis.db;

import com.extremis.core.Competence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "character_skills",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "skill"}))
public class CharacterSkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private CharacterEntity owner;

    @Enumerated(EnumType.STRING)
    private Competence skill;

    @Column(name = "skill_value")
    private int value;

    public CharacterSkillEntity() {}

    public CharacterSkillEntity(CharacterEntity owner, Competence skill, int value) {
        this.owner = owner;
        this.skill = skill;
        this.value = value;
    }

    public Competence getSkill() { return skill; }
    public int getValue() { return value; }
}
