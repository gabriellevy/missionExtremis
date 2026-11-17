package com.extremis.core;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lieux du jeu : les arrondissements de Paris, les villes d'Île-de-France
 * et l'Ukraine. Un lieu est l'endroit où vit un personnage ou l'endroit où
 * se rendre pour accomplir une mission.
 */
public enum Lieu {
    PARIS_1ER("Paris 1er"),
    PARIS_2E("Paris 2e"),
    PARIS_3E("Paris 3e"),
    PARIS_4E("Paris 4e"),
    PARIS_5E("Paris 5e"),
    PARIS_6E("Paris 6e"),
    PARIS_7E("Paris 7e"),
    PARIS_8E("Paris 8e"),
    PARIS_9E("Paris 9e"),
    PARIS_10E("Paris 10e"),
    PARIS_11E("Paris 11e"),
    PARIS_12E("Paris 12e"),
    PARIS_13E("Paris 13e"),
    PARIS_14E("Paris 14e"),
    PARIS_15E("Paris 15e"),
    PARIS_16E("Paris 16e"),
    PARIS_17E("Paris 17e"),
    PARIS_18E("Paris 18e"),
    PARIS_19E("Paris 19e"),
    PARIS_20E("Paris 20e"),
    VERSAILLES("Versailles"),
    SAINT_DENIS("Saint-Denis"),
    BOULOGNE_BILLANCOURT("Boulogne-Billancourt"),
    ARGENTEUIL("Argenteuil"),
    MONTREUIL("Montreuil"),
    NANTERRE("Nanterre"),
    VITRY_SUR_SEINE("Vitry-sur-Seine"),
    CRETEIL("Créteil"),
    ASNIERES_SUR_SEINE("Asnières-sur-Seine"),
    COLOMBES("Colombes"),
    AUBERVILLIERS("Aubervilliers"),
    COURBEVOIE("Courbevoie"),
    EVRY("Évry"),
    MEAUX("Meaux"),
    MELUN("Melun"),
    FONTAINEBLEAU("Fontainebleau"),
    CERGY("Cergy"),
    POISSY("Poissy"),
    SURESNES("Suresnes"),
    IVRY_SUR_SEINE("Ivry-sur-Seine"),
    ANTONY("Antony"),
    CLAMART("Clamart"),
    SARCELLES("Sarcelles"),
    UKRAINE("Ukraine");

    private static final Lieu[] TOUS = values();

    private final String label;

    Lieu(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Tire un lieu au hasard parmi tous les lieux du jeu. */
    public static Lieu auHasard() {
        return TOUS[ThreadLocalRandom.current().nextInt(TOUS.length)];
    }
}
