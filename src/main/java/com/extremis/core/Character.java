package com.extremis.core;

import java.util.ArrayList;
import java.util.List;

public final class Character {
    private final String id;
    private final String name;
    private final List<Trait> traits = new ArrayList<>();
    private final java.util.EnumMap<Skill, Integer> skills = new java.util.EnumMap<>(Skill.class);
    private final List<String> inventory = new ArrayList<>();
    private int health = 10;
    private boolean alive = true;

    public Character(String id, String name) {
        this.id = java.util.Objects.requireNonNull(id);
        this.name = java.util.Objects.requireNonNull(name);
    }

    public String id() { return id; }
    public String name() { return name; }
    public int health() { return health; }
    public boolean isAlive() { return alive; }
    public List<Trait> traits() { return traits; }
    public List<String> inventory() { return inventory; }

    public Character withSkill(Skill skill, int value) {
        skills.put(skill, value);
        return this;
    }

    public Character withTrait(Trait trait) {
        traits.add(trait);
        return this;
    }

    public boolean hasSkill(Skill skill) {
        return skills.containsKey(skill);
    }

    public int skillValue(Skill skill) {
        return skills.getOrDefault(skill, 0);
    }

    public void applyDamage(int amount) {
        health = Math.max(0, health - amount);
        if (health == 0) {
            alive = false;
        }
    }

    public void heal(int amount) {
        if (alive) {
            health = Math.min(10, health + amount);
        }
    }

    public void addItem(String item) {
        inventory.add(item);
    }

    public java.util.Map<Skill, Integer> skillsSnapshot() {
        return java.util.Map.copyOf(skills);
    }

    public void setHealth(int value) {
        health = Math.max(0, Math.min(10, value));
    }

    public void kill() {
        health = 0;
        alive = false;
    }
}
