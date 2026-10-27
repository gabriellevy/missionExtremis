package com.extremis.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "character_maitrises")
public class CharacterMaitriseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private CharacterEntity owner;

    private String nom;

    public CharacterMaitriseEntity() {}

    public CharacterMaitriseEntity(CharacterEntity owner, String nom) {
        this.owner = owner;
        this.nom = nom;
    }

    public String getNom() { return nom; }
}
