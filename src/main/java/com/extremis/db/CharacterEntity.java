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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "characters")
public class CharacterEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 64)
    private String coterie;

    @Column(length = 64)
    private String role;

    private int health = 10;

    private boolean alive = true;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    private List<CharacterSkillEntity> skills = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    private List<CharacterTraitEntity> traits = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    private List<CharacterItemEntity> inventory = new ArrayList<>();

    public CharacterEntity() {}

    public CharacterEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCoterie() { return coterie; }
    public String getRole() { return role; }
    public int getHealth() { return health; }
    public boolean isAlive() { return alive; }
    public List<CharacterSkillEntity> getSkills() { return skills; }
    public List<CharacterTraitEntity> getTraits() { return traits; }
    public List<CharacterItemEntity> getInventory() { return inventory; }

    public void setCoterie(String coterie) { this.coterie = coterie; }
    public void setRole(String role) { this.role = role; }
    public void setHealth(int health) { this.health = health; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public void addSkill(com.extremis.core.Skill skill, int value) {
        skills.removeIf(s -> s.getSkill() == skill);
        CharacterSkillEntity e = new CharacterSkillEntity(this, skill, value);
        skills.add(e);
    }

    public void addTrait(String name) {
        traits.add(new CharacterTraitEntity(this, name));
    }

    public void addItem(String label) {
        inventory.add(new CharacterItemEntity(this, label));
    }
}
