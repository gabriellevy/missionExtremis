package com.extremis.core;

/**
 * Paires de vices et vertus issues de la base "Vices et vertus" du wiki
 * (page "Personnage jdr" du systeme de jeu de role).
 * Chaque constante designe une paire : la vertu et son vice oppose.
 */
public enum ViceEtVertu {
    BIENVEILLANT("Bienveillant", "Cruel"),
    SOBRE("Sobre", "Gourmand"),
    VALEUREUX("Valeureux", "Lâche"),
    PRODIGUE("Prodigue", "Cupide"),
    ARTIFICIALISTE("Artificialiste", "Naturaliste"),
    TRAVAILLEUR("Travailleur", "Paresseux"),
    DESINTERESSE("Désintéressé", "Envieux"),
    PRUDENT("Prudent", "Impulsif"),
    HUMBLE("Humble", "Orgueilleux"),
    CHASTE("Chaste", "Luxurieux"),
    LOYAL("Loyal", "Trompeur"),
    PLACIDE("Placide", "Colérique"),
    SOCIABLE("Sociable", "Solitaire"),
    DISCIPLINE("Discipliné", "Rebelle"),
    EMPATHIQUE("Empathique", "Sociopathique"),
    CARTESIEN("Cartésien", "Imaginatif"),
    CASANIER("Casanier", "Aventureux"),
    NAIF("Naïf", "Méfiant"),
    ESTHETE("Esthète", "Grossier");

    private static final int DEGRE_MIN = 1;
    private static final int DEGRE_MAX = 3;

    private final String vertu;
    private final String vice;

    ViceEtVertu(String vertu, String vice) {
        this.vertu = vertu;
        this.vice = vice;
    }

    public String vertu() {
        return vertu;
    }

    public String vice() {
        return vice;
    }

    public String vertu(int degre) {
        return avecDegre(vertu, degre);
    }

    public String vice(int degre) {
        return avecDegre(vice, degre);
    }

    private String avecDegre(String libelle, int degre) {
        if (degre < DEGRE_MIN || degre > DEGRE_MAX) {
            throw new IllegalArgumentException("Degre de vice ou vertu hors bornes : " + degre);
        }
        return switch (degre) {
            case 2 -> "très " + libelle;
            case 3 -> "extrêmement " + libelle;
            default -> libelle;
        };
    }
}
