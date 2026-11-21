package com.extremis.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class LieuCarteTest {

    @Test
    void tousLesLieuxOntDesCoordonneesGeographiquesPlausibles() {
        for (Lieu lieu : EnumSet.allOf(Lieu.class)) {
            assertThat(lieu.latitude()).isBetween(48.0, 51.0);
            assertThat(lieu.longitude()).isBetween(2.0, 31.0);
            assertThat(lieu.label()).isNotBlank();
        }
    }

    @Test
    void lesArrondissementsDeParisRestentGroupesAutourDuCentreDeLaVueInitiale() {
        for (Lieu lieu : EnumSet.allOf(Lieu.class)) {
            if (lieu.name().startsWith("PARIS_")) {
                assertThat(lieu.latitude())
                        .isCloseTo(Lieu.CARTE_LATITUDE_CENTRE, org.assertj.core.data.Offset.offset(0.1));
                assertThat(lieu.longitude())
                        .isCloseTo(Lieu.CARTE_LONGITUDE_CENTRE, org.assertj.core.data.Offset.offset(0.2));
            }
        }
    }

    @Test
    void lUkraineEstLeLieuLePlusAEstDeLaCarte() {
        double longitudeMax = 0;
        for (Lieu lieu : EnumSet.allOf(Lieu.class)) {
            longitudeMax = Math.max(longitudeMax, lieu.longitude());
        }
        assertThat(longitudeMax).isEqualTo(Lieu.UKRAINE.longitude());
        assertThat(Lieu.UKRAINE.longitude()).isGreaterThan(20);
    }

    @Test
    void laVueInitialeEstCentreeSurLaRegionParisienneAvecUnPasDeZoomClavier() {
        assertThat(Lieu.CARTE_LATITUDE_CENTRE).isEqualTo(48.8566);
        assertThat(Lieu.CARTE_LONGITUDE_CENTRE).isEqualTo(2.3522);
        assertThat(Lieu.CARTE_ZOOM_INITIAL).isEqualTo(10);
        assertThat(Lieu.ZOOM_PAS_LEGER).isEqualTo(1);
        assertThat(Lieu.ZOOM_PAS_FORTE).isEqualTo(10);
    }
}
