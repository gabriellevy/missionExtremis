package com.extremis.core;

import java.util.List;
import java.util.function.Consumer;

/**
 * Execution complete et immediate d'une mission, evenement apres
 * evenement, jusqu'a la reussite ou l'echec finale.
 */
public final class MissionRunner {
    private final RandomSource random;

    public MissionRunner(RandomSource random) {
        this.random = random;
    }

    public MissionReport run(Mission mission, List<Character> team, Consumer<String> log) {
        MissionExecution execution = MissionExecution.start(mission, team, random, java.time.Instant.now());
        return run(execution, log);
    }

    public MissionReport run(MissionExecution execution, Consumer<String> log) {
        java.time.Instant maintenant = java.time.Instant.now();
        // en mode immediate, chaque duree est franchie d'un bloc
        while (!execution.advance(maintenant.plus(java.time.Duration.ofDays(36500)))) {
            // rien : l'horloge passee couvre toutes les durees restantes
        }
        String summary = String.join("\n", execution.logLines());
        log.accept(summary);
        return execution.report();
    }
}
