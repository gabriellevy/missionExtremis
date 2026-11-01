package com.extremis.core;

public interface RandomSource {
    int roll();

    /**
     * Lance un D100 et renvoie le resultat sous forme de deux des a
     * dix faces (dizaines + unites) pour un affichage graphique.
     */
    default JetDes jetD100() {
        return JetDes.d100(roll());
    }
}
