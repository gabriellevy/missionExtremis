package com.extremis.core;

import java.util.ArrayList;
import java.util.List;

public final class Character {
    private final String id;
    private final String name;
    private final List<Trait> traits = new ArrayList<>();
    private final java.util.EnumMap<Competence, Integer> skills = new java.util.EnumMap<>(Competence.class);
    private final List<String> inventory = new ArrayList<>();
    private int skillBase = DEFAULT_SKILL_BASE;
    private int vitalite = VITALITE_DEFAUT;
    private int sangFroid = SANG_FROID_DEFAUT;
    private boolean alive = true;

    public static final int VITALITE_DEFAUT = 10;
    public static final int SANG_FROID_DEFAUT = 10;

    public static final int DEFAULT_SKILL_BASE = 30;

    public Character(String id, String name) {
        this.id = java.util.Objects.requireNonNull(id);
        this.name = java.util.Objects.requireNonNull(name);
    }

    public String id() { return id; }
    public String name() { return name; }
    public int vitalite() { return vitalite; }
    public int sangFroid() { return sangFroid; }
    public boolean isAlive() { return alive; }
    public List<Trait> traits() { return traits; }
    public List<String> inventory() { return inventory; }
    public int skillBase() { return skillBase; }

    public Character withSangFroid(int valeur) {
        this.sangFroid = valeur;
        return this;
    }

    public Character withSkillBase(int base) {
        this.skillBase = base;
        return this;
    }

    public Character withSkill(Competence skill, int value) {
        skills.put(skill, value);
        return this;
    }

    public Character withTrait(Trait trait) {
        traits.add(trait);
        return this;
    }

    public boolean hasSkill(Competence skill) {
        return skills.containsKey(skill);
    }

    public int skillValue(Competence skill) {
        return skills.getOrDefault(skill, 0);
    }

    /**
     * Cible effective d'un test de competence : base + valeur (wiki,
     * page "Base de competences"). Utilisee telle quelle par les tests en D100.
     */
    public int skillTarget(Competence skill) {
        return skillBase + skillValue(skill);
    }

    public void applyDamage(int amount) {
        vitalite = Math.max(0, vitalite - amount);
        if (vitalite == 0) {
            alive = false;
        }
    }

    public void heal(int amount) {
        if (alive) {
            vitalite = Math.min(VITALITE_DEFAUT, vitalite + amount);
        }
    }

    public void addItem(String item) {
        inventory.add(item);
    }

    public java.util.Map<Competence, Integer> skillsSnapshot() {
        return java.util.Map.copyOf(skills);
    }

    public void setVitalite(int value) {
        vitalite = Math.max(0, Math.min(VITALITE_DEFAUT, value));
    }

    public void setSangFroid(int value) {
        sangFroid = Math.max(0, value);
    }

    public void kill() {
        vitalite = 0;
        alive = false;
    }
}
