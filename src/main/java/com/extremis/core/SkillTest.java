package com.extremis.core;

import java.util.List;
import java.util.Optional;

/**
 * Test de competence D100 a la maniere de Brigandyne (wiki, page
 * "Competences") : le resultat doit etre inferieur ou egal a la valeur
 * de competence du personnage (base + competence).
 *
 * La difficulte est un ajustement applique a la cible (negatif = plus
 * dur, positif = plus facile).
 */
public interface SkillTest {
    Competence skill();

    int difficulty();

    default int target(Character character) {
        return character.skillTarget(skill()) + difficulty();
    }

    /**
     * Cible du test pour un personnage au sein d'une équipe : par défaut
     * la même que la cible individuelle (les tests "aidés", wiki, page
     * "Tests de caracs", en dépendent).
     */
    default int target(Character character, List<Character> equipe) {
        return target(character);
    }

    /**
     * Le PJ du groupe qui a la plus haute valeur de la competence
     * concernee tente le test (wiki, page "Missions"), sauf exclusion.
     */
    default Optional<Character> bestCandidate(List<Character> team) {
        return team.stream()
                .filter(Character::isActif)
                .max((a, b) -> Integer.compare(target(a), target(b)));
    }

    /**
     * @return le niveau de reussite du jet D100, jamais null
     */
    default ResultatTest attempt(Character character, RandomSource random) {
        return ResultatTest.duJet(target(character), random.roll());
    }
}
