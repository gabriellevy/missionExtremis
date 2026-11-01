package com.extremis.core;

/**
 * Resultat d'un jet de D100 affiche sous forme de deux des a dix
 * faces (style jeu de role) : un de pour les dizaines, un de pour
 * les unites. La convention du D100 en percentiles est que le de des
 * dizaines vaut 0 (et non 10) quand le jet tombe sur 100.
 */
public record JetDes(int dizaines, unites) {

    public static JetDes d100(int valeur) {
        int borne = Math.floorMod(valeur - 1, 100) + 1;
        int dizaines = borne == 100 ? 0 : borne / 10;
        int unites = borne % 10;
        return new JetDes(dizaines, unites);
    }

    public int valeur() {
        return dizaines * 10 + unites;
    }
}
