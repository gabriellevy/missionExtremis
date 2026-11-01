package com.extremis.core;

/**
 * Résultat d'un jet de D100 affiché sous forme de deux dés à dix
 * faces (style jeu de rôle) : un dé pour les dizaines, un dé pour
 * les unités. La convention du D100 en percentiles est que le dé des
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
