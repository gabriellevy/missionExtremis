package com.extremis.core;

import java.util.List;

/**
 * Test de carac aidé (wiki, page "Tests de caracs") abrégé
 * "t-aide-Carac" dans les schémas de mission : le personnage qui a la
 * plus haute valeur de la compétence effectue le test et gagne +10 à
 * sa cible pour chaque autre arrangeur actif de l'équipe.
 */
public record TestAide(Competence skill, int difficulty) implements SkillTest {

    @Override
    public int target(Character character, List<Character> equipe) {
        long nombreDAides = equipe == null ? 0
                : equipe.stream()
                        .filter(Character::isActif)
                        .filter(membre -> !membre.equals(character))
                        .count();
        return skillTarget(character) + (int) (10 * nombreDAides);
    }

    private int skillTarget(Character character) {
        return character.skillTarget(skill()) + difficulty();
    }
}
