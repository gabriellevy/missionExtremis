package com.extremis.db;

import com.extremis.core.Skill;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

/**
 * Convertit la colonne skill (String) vers l'enum Skill en tolerants les
 * anciens noms de competences d'avant la refonte du wiki.
 */
@Converter
public class SkillConverter implements AttributeConverter<Skill, String> {

    private static final Map<String, Skill> LEGACY_NAMES = Map.of(
            "COMBAT", Skill.ARMES_CORPS_A_CORPS,
            "AGILITE", Skill.MOUVEMENT,
            "ERUDITION", Skill.INTUITION,
            "DIPLOMATIE", Skill.ELOQUENCE,
            "TECHNIQUE", Skill.HABILETE);

    @Override
    public String convertToDatabaseColumn(Skill attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Skill convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Skill.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            Skill legacy = LEGACY_NAMES.get(dbData);
            if (legacy == null) {
                throw new IllegalArgumentException("Competence inconnue en base : " + dbData, e);
            }
            return legacy;
        }
    }
}
