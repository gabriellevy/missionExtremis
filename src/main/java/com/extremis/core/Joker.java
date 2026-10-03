package com.extremis.core;

public record Joker(String id, String label) {
    public static Joker calculatedCoincidence() {
        return new Joker("coincidence", "Coïncidence calculée");
    }
}
