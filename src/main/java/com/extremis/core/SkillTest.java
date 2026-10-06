package com.extremis.core;

import java.util.List;
import java.util.Optional;

/**
 * Test de competence a la maniere du jdr Brigandyne (wiki, page
 * "Competences") : un D100 dont le resultat doit etre inferieur a la
 * valeur de competence du personnage, c'est-a-dire sa base de
 * competences + sa valeur dans la competence.
 *
 * La difficulte est un modificateur applique a cette valeur cible
 * (negatif pour un test plus dur, positif pour un test plus facile).
 */
public interface SkillTest {
    Competence skill();

    int difficulty();

    default int target(Character character) {
        return character.skillTarget(skill()) + difficulty();
    }

    default Optional<Character> bestCandidate(List<Character> team) {
        return team.stream()
                .filter(Character::isAlive)
                .max((a, b) -> Integer.compare(target(a), target(b)));
    }

    default boolean attempt(Character character, RandomSource random) {
        return random.roll() < target(character);
    }
}
