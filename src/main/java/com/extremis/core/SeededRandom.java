package com.extremis.core;

import java.util.random.RandomGenerator;

public final class SeededRandom implements RandomSource {
    private final RandomGenerator generator;
    private long jetsEffectues;

    public SeededRandom(long seed) {
        this.generator = new java.util.Random(seed);
    }

    /**
     * Reprend la sequence la ou elle s'etait arretee apres un
     * redemarrage : saute les jets deja effectues pour ne pas rejouer
     * les memes valeurs.
     */
    public SeededRandom(long seed, long jetsDejaEffectues) {
        this.generator = new java.util.Random(seed);
        for (long i = 0; i < jetsDejaEffectues; i++) {
            generator.nextInt(1, 101);
        }
        this.jetsEffectues = jetsDejaEffectues;
    }

    @Override
    public int roll() {
        jetsEffectues++;
        return generator.nextInt(1, 101);
    }

    @Override
    public JetDes jetD100() {
        jetsEffectues++;
        return JetDes.d100(generator.nextInt(1, 101));
    }

    public long getJetsEffectues() {
        return jetsEffectues;
    }
}
