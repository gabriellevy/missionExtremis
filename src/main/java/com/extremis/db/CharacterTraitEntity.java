package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_traits")
public class CharacterTraitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private CharacterEntity owner;

    private String name;

    public CharacterTraitEntity() {}

    public CharacterTraitEntity(CharacterEntity owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String getName() { return name; }
}
