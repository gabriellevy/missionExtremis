package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Skill;
import com.extremis.core.Trait;

import java.util.LinkedHashSet;
import java.util.List;

public final class EntityMapper {
    private EntityMapper() {}

    public static Character toCore(CharacterEntity e) {
        Character c = new Character(e.getId(), e.getName()); c.withSkillBase(e.getSkillBase());
        for (CharacterSkillEntity s : e.getSkills()) {
            c.withSkill(s.getSkill(), s.getValue());
        }
        for (CharacterTraitEntity t : e.getTraits()) {
            c.withTrait(new Trait(t.getName(), new LinkedHashSet<>()));
        }
        for (CharacterItemEntity i : e.getInventory()) {
            c.addItem(i.getLabel());
        }
        c.setHealth(e.getHealth());
        if (!e.isAlive()) {
            c.kill();
        }
        return c;
    }

    public static CharacterEntity toEntity(Character c, String coterie, String role) {
        CharacterEntity e = new CharacterEntity(c.id(), c.name());
        e.setCoterie(coterie);
        e.setRole(role); e.setSkillBase(c.skillBase());
        for (java.util.Map.Entry<Skill, Integer> entry : c.skillsSnapshot().entrySet()) {
            e.addSkill(entry.getKey(), entry.getValue());
        }
        for (Trait t : c.traits()) {
            e.addTrait(t.name());
        }
        for (String item : c.inventory()) {
            e.addItem(item);
        }
        e.setHealth(c.health());
        e.setAlive(c.isAlive());
        return e;
    }

    public static List<Character> toCoreList(List<CharacterEntity> entities) {
        return entities.stream().map(EntityMapper::toCore).toList();
    }
}
