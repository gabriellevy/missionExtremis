package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
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
        for (CharacterSpecialiteEntity s : e.getSpecialites()) {
            c.withSpecialite(s.getNom());
        }
        for (CharacterMaitriseEntity m : e.getMaitrises()) {
            c.withMaitrise(m.getNom());
        }
        for (CharacterItemEntity i : e.getInventory()) {
            c.addItem(i.getLabel());
        }
        c.withVitaliteDepart(e.getVitaliteDepart());
        c.withSangFroidDepart(e.getSangFroidDepart());
        c.setVitalite(e.getVitalite());
        c.setSangFroid(e.getSangFroid());
        if (!e.isAlive()) {
            c.kill();
        }
        return c;
    }

    public static CharacterEntity toEntity(Character c, String coterie, String carriere) {
        CharacterEntity e = new CharacterEntity(c.id(), c.name());
        e.setCoterie(coterie);
        e.setCarriere(carriere); e.setSkillBase(c.skillBase());
        for (java.util.Map.Entry<Competence, Integer> entry : c.skillsSnapshot().entrySet()) {
            e.addSkill(entry.getKey(), entry.getValue());
        }
        for (Trait t : c.traits()) {
            e.addTrait(t.name());
        }
        for (String specialite : c.specialites()) {
            e.addSpecialite(specialite);
        }
        for (String maitrise : c.maitrises()) {
            e.addMaitrise(maitrise);
        }
        for (String item : c.inventory()) {
            e.addItem(item);
        }
        e.setVitalite(c.vitalite());
        e.setSangFroid(c.sangFroid());
        e.setVitaliteDepart(c.vitaliteDepart());
        e.setSangFroidDepart(c.sangFroidDepart());
        e.setAlive(c.isAlive());
        return e;
    }

    public static List<Character> toCoreList(List<CharacterEntity> entities) {
        return entities.stream().map(EntityMapper::toCore).toList();
    }
}
