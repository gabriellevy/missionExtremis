package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_items")
public class CharacterItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private CharacterEntity owner;

    private String label;

    public CharacterItemEntity() {}

    public CharacterItemEntity(CharacterEntity owner, String label) {
        this.owner = owner;
        this.label = label;
    }

    public String getLabel() { return label; }
}
