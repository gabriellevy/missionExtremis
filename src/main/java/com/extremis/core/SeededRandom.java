package com.extremis.core;

import java.util.random.RandomGenerator;

public final class SeededRandom implements RandomSource {
    private final RandomGenerator generator;

    public SeededRandom(long seed) {
        this.generator = new java.util.Random(seed);
    }

    @Override
    public int roll() {
        return generator.nextInt(1, 101);
    }

    @Override
    public JetDes jetD100() {
        return JetDes.d100(generator.nextInt(1, 101));
    }
}
