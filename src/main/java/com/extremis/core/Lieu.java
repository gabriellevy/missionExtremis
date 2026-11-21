package com.extremis.core;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lieux du jeu : les arrondissements de Paris, les villes d'Île-de-France
 * et l'Ukraine. Un lieu est l'endroit où vit un personnage ou l'endroit où
 * se rendre pour accomplir une mission.
 *
 * Chaque lieu porte ses coordonnées géographiques réelles (latitude,
 * longitude en degrés décimaux, standard WGS84), utilisées pour placer
 * les marqueurs sur la carte du monde affichée dans l'onglet Missions.
 */
public enum Lieu {
    PARIS_1ER("Paris 1er", 48.8622, 2.3364),
    PARIS_2E("Paris 2e", 48.8686, 2.3413),
    PARIS_3E("Paris 3e", 48.8631, 2.3600),
    PARIS_4E("Paris 4e", 48.8543, 2.3572),
    PARIS_5E("Paris 5e", 48.8443, 2.3455),
    PARIS_6E("Paris 6e", 48.8494, 2.3325),
    PARIS_7E("Paris 7e", 48.8560, 2.3124),
    PARIS_8E("Paris 8e", 48.8734, 2.3077),
    PARIS_9E("Paris 9e", 48.8774, 2.3398),
    PARIS_10E("Paris 10e", 48.8760, 2.3602),
    PARIS_11E("Paris 11e", 48.8589, 2.3800),
    PARIS_12E("Paris 12e", 48.8355, 2.3895),
    PARIS_13E("Paris 13e", 48.8290, 2.3555),
    PARIS_14E("Paris 14e", 48.8300, 2.3267),
    PARIS_15E("Paris 15e", 48.8393, 2.2977),
    PARIS_16E("Paris 16e", 48.8637, 2.2634),
    PARIS_17E("Paris 17e", 48.8867, 2.3074),
    PARIS_18E("Paris 18e", 48.8925, 2.3477),
    PARIS_19E("Paris 19e", 48.8882, 2.3818),
    PARIS_20E("Paris 20e", 48.8637, 2.3990),
    VERSAILLES("Versailles", 48.8014, 2.1350),
    SAINT_DENIS("Saint-Denis", 48.9362, 2.3574),
    BOULOGNE_BILLANCOURT("Boulogne-Billancourt", 48.8352, 2.2409),
    ARGENTEUIL("Argenteuil", 48.9478, 2.2470),
    MONTREUIL("Montreuil", 48.8636, 2.4436),
    NANTERRE("Nanterre", 48.8920, 2.2064),
    VITRY_SUR_SEINE("Vitry-sur-Seine", 48.7876, 2.3928),
    CRETEIL("Créteil", 48.7904, 2.4557),
    ASNIERES_SUR_SEINE("Asnières-sur-Seine", 48.9108, 2.2851),
    COLOMBES("Colombes", 48.9254, 2.2653),
    AUBERVILLIERS("Aubervilliers", 48.9133, 2.3812),
    COURBEVOIE("Courbevoie", 48.8968, 2.2561),
    EVRY("Évry", 48.6288, 2.4470),
    MEAUX("Meaux", 48.9539, 2.8990),
    MELUN("Melun", 48.5429, 2.6600),
    FONTAINEBLEAU("Fontainebleau", 48.4036, 2.6989),
    CERGY("Cergy", 49.0386, 2.0750),
    POISSY("Poissy", 48.9243, 2.0440),
    SURESNES("Suresnes", 48.8695, 2.2296),
    IVRY_SUR_SEINE("Ivry-sur-Seine", 48.8125, 2.3880),
    ANTONY("Antony", 48.7527, 2.5194),
    CLAMART("Clamart", 48.7997, 2.2667),
    SARCELLES("Sarcelles", 48.9730, 2.3780),
    UKRAINE("Ukraine", 50.4501, 30.5234);

    /** Longitude par défaut de la vue initiale : Notre-Dame de Paris. */
    public static final double CARTE_LONGITUDE_CENTRE = 2.3522;
    /** Latitude par défaut de la vue initiale : Notre-Dame de Paris. */
    public static final double CARTE_LATITUDE_CENTRE = 48.8566;
    /** Niveau de zoom initial : la région parisienne entière est visible. */
    public static final int CARTE_ZOOM_INITIAL = 10;
    public static final int ZOOM_PAS_LEGER = 1;
    public static final int ZOOM_PAS_FORTE = 10;

    private static final Lieu[] TOUS = values();

    private final String label;
    private final double latitude;
    private final double longitude;

    Lieu(String label, double latitude, double longitude) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String label() {
        return label;
    }

    /** Latitude réelle du lieu en degrés décimaux (WGS84). */
    public double latitude() {
        return latitude;
    }

    /** Longitude réelle du lieu en degrés décimaux (WGS84). */
    public double longitude() {
        return longitude;
    }

    /** Tire un lieu au hasard parmi tous les lieux du jeu. */
    public static Lieu auHasard() {
        return TOUS[ThreadLocalRandom.current().nextInt(TOUS.length)];
    }
}
