package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class LieuCarteTest {

    @Test
    void tousLesLieuxOntDesCoordonneesDansLeRepereDeLaCarte() {
        for (Lieu lieu : EnumSet.allOf(Lieu.class)) {
            assertThat(lieu.carteX()).isBetween(0, Lieu.CARTE_LARGEUR);
            assertThat(lieu.carteY()).isBetween(0, Lieu.CARTE_HAUTEUR);
            assertThat(lieu.label()).isNotBlank();
        }
    }

    @Test
    void lesArrondissementsDeParisRestentGroupesSurLaCarte() {
        int xMin = Lieu.PARIS_16E.carteX();
        int xMax = Lieu.PARIS_20E.carteX();
        int yMin = Lieu.PARIS_18E.carteY();
        int yMax = Lieu.PARIS_13E.carteY();

        assertThat(xMax - xMin).isLessThan(Lieu.CARTE_LARGEUR / 4);
        assertThat(yMax - yMin).isLessThan(Lieu.CARTE_HAUTEUR / 4);
    }

    @Test
    void lUkraineEstLeLieuLePlusAEstDeLaCarte() {
        int xMax = 0;
        for (Lieu lieu : EnumSet.allOf(Lieu.class)) {
            xMax = Math.max(xMax, lieu.carteX());
        }
        assertThat(xMax).isEqualTo(Lieu.UKRAINE.carteX());
        assertThat(Lieu.UKRAINE.carteX()).isGreaterThan(2 * (Lieu.PARIS_1ER.carteX()));
    }
}
