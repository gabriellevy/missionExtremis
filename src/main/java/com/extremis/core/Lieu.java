package com.extremis.core;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lieux du jeu : les arrondissements de Paris, les villes d'Île-de-France
 * et l'Ukraine. Un lieu est l'endroit où vit un personnage ou l'endroit où
 * se rendre pour accomplir une mission.
 *
 * Chaque lieu porte des coordonnées dans le repère de la carte du jeu
 * (repère CARTES : 1600 x 1000), utilisées pour placer les marqueurs
 * de personnages et de missions.
 */
public enum Lieu {
    PARIS_1ER("Paris 1er", 515, 369),
    PARIS_2E("Paris 2e", 518, 363),
    PARIS_3E("Paris 3e", 526, 368),
    PARIS_4E("Paris 4e", 525, 376),
    PARIS_5E("Paris 5e", 519, 386),
    PARIS_6E("Paris 6e", 513, 380),
    PARIS_7E("Paris 7e", 504, 375),
    PARIS_8E("Paris 8e", 502, 358),
    PARIS_9E("Paris 9e", 517, 355),
    PARIS_10E("Paris 10e", 526, 356),
    PARIS_11E("Paris 11e", 535, 371),
    PARIS_12E("Paris 12e", 539, 394),
    PARIS_13E("Paris 13e", 524, 400),
    PARIS_14E("Paris 14e", 511, 399),
    PARIS_15E("Paris 15e", 497, 389),
    PARIS_16E("Paris 16e", 481, 367),
    PARIS_17E("Paris 17e", 502, 345),
    PARIS_18E("Paris 18e", 520, 340),
    PARIS_19E("Paris 19e", 536, 344),
    PARIS_20E("Paris 20e", 544, 367),
    VERSAILLES("Versailles", 437, 437),
    SAINT_DENIS("Saint-Denis", 522, 294),
    BOULOGNE_BILLANCOURT("Boulogne-Billancourt", 471, 394),
    ARGENTEUIL("Argenteuil", 474, 289),
    MONTREUIL("Montreuil", 564, 367),
    NANTERRE("Nanterre", 464, 340),
    VITRY_SUR_SEINE("Vitry-sur-Seine", 541, 448),
    CRETEIL("Créteil", 567, 446),
    ASNIERES_SUR_SEINE("Asnières-sur-Seine", 490, 324),
    COLOMBES("Colombes", 482, 309),
    AUBERVILLIERS("Aubervilliers", 535, 321),
    COURBEVOIE("Courbevoie", 472, 336),
    EVRY("Évry", 558, 685),
    MEAUX("Meaux", 820, 327),
    MELUN("Melun", 709, 742),
    FONTAINEBLEAU("Fontainebleau", 727, 882),
    CERGY("Cergy", 405, 186),
    POISSY("Poissy", 396, 310),
    SURESNES("Suresnes", 466, 362),
    IVRY_SUR_SEINE("Ivry-sur-Seine", 538, 416),
    ANTONY("Antony", 600, 470),
    CLAMART("Clamart", 483, 437),
    SARCELLES("Sarcelles", 535, 265),
    UKRAINE("Ukraine", 1450, 500);

    public static final int CARTE_LARGEUR = 1600;
    public static final int CARTE_HAUTEUR = 1000;

    private static final Lieu[] TOUS = values();

    private final String label;
    private final int carteX;
    private final int carteY;

    Lieu(String label, int carteX, int carteY) {
        this.label = label;
        this.carteX = carteX;
        this.carteY = carteY;
    }

    public String label() {
        return label;
    }

    /** Abscisse du lieu sur la carte du jeu (repère CARTES_LARGEUR x CARTES_HAUTEUR). */
    public int carteX() {
        return carteX;
    }

    /** Ordonnée du lieu sur la carte du jeu (repère CARTES_LARGEUR x CARTES_HAUTEUR). */
    public int carteY() {
        return carteY;
    }

    /** Tire un lieu au hasard parmi tous les lieux du jeu. */
    public static Lieu auHasard() {
        return TOUS[ThreadLocalRandom.current().nextInt(TOUS.length)];
    }
}
