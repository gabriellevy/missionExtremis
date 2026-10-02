package com.extremis.core;

import java.util.List;
import java.util.Optional;

public interface SkillTest {
    Skill skill();

    int difficulty();

    default Optional<Character> bestCandidate(List<Character> team) {
        return team.stream()
                .filter(Character::isAlive)
                .filter(c -> c.hasSkill(skill()))
                .max((a, b) -> Integer.compare(a.skillValue(skill()), b.skillValue(skill())));
    }

    default boolean attempt(Character character, RandomSource random) {
        return character.skillValue(skill()) + random.roll() >= difficulty();
    }
}
