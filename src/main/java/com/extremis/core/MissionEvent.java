package com.extremis.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class MissionEvent {
    private final String id;
    private final String description;
    private final MissionEventType type;
    private final SkillTest test;
    private final int damageOnFailure;
    private final String rewardOnSuccess;
    private final Duration delayFromPrevious;

    private MissionEvent(Builder builder) {
        this.id = builder.id;
        this.description = builder.description;
        this.type = builder.test == null ? MissionEventType.NARRATIVE : MissionEventType.SKILL_CHECK;
        this.test = builder.test;
        this.damageOnFailure = builder.damageOnFailure;
        this.rewardOnSuccess = builder.rewardOnSuccess;
        this.delayFromPrevious = builder.delayFromPrevious;
    }

    public String id() { return id; }
    public String description() { return description; }
    public MissionEventType type() { return type; }
    public Optional<SkillTest> test() { return Optional.ofNullable(test); }
    public int damageOnFailure() { return damageOnFailure; }
    public String rewardOnSuccess() { return rewardOnSuccess; }
    public Duration delayFromPrevious() { return delayFromPrevious; }

    public void resolve(List<Character> team, RandomSource random, Consumer<String> log) {
        log.accept(description);
        if (type != MissionEventType.SKILL_CHECK || test == null) {
            return;
        }
        Optional<Character> candidate = test.bestCandidate(team);
        if (candidate.isEmpty()) {
            log.accept("Aucun arrangeur ne maitrise " + test.skill() + " : echec automatique.");
            applyFailure(team, log);
            return;
        }
        Character performer = candidate.get();
        boolean success = test.attempt(performer, random);
        if (success) {
            log.accept(performer.name() + " reussit le test de " + test.skill() + ".");
            if (rewardOnSuccess != null) {
                performer.addItem(rewardOnSuccess);
                log.accept(performer.name() + " obtient : " + rewardOnSuccess);
            }
        } else {
            log.accept(performer.name() + " echoue le test de " + test.skill() + ".");
            applyFailure(team, log);
        }
    }

    private void applyFailure(List<Character> team, Consumer<String> log) {
        if (damageOnFailure <= 0) {
            return;
        }
        Character victim = team.stream().filter(Character::isAlive).findFirst().orElse(null);
        if (victim == null) {
            return;
        }
        victim.applyDamage(damageOnFailure);
        log.accept(victim.name() + " subit " + damageOnFailure + " degats (sante restante : " + victim.health() + ").");
        if (!victim.isAlive()) {
            log.accept(victim.name() + " succombe.");
        }
    }

    public static Builder builder(String id, String description) {
        return new Builder(id, description);
    }

    public static final class Builder {
        private final String id;
        private final String description;
        private SkillTest test;
        private int damageOnFailure;
        private String rewardOnSuccess;
        private Duration delayFromPrevious = Duration.ZERO;

        private Builder(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public Builder test(SkillTest test) {
            this.test = test;
            return this;
        }

        public Builder damageOnFailure(int damage) {
            this.damageOnFailure = damage;
            return this;
        }

        public Builder rewardOnSuccess(String reward) {
            this.rewardOnSuccess = reward;
            return this;
        }

        public Builder delayFromPrevious(Duration delay) {
            this.delayFromPrevious = delay;
            return this;
        }

        public MissionEvent build() {
            return new MissionEvent(this);
        }
    }
}
