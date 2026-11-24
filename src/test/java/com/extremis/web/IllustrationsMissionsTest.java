package com.extremis.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des illustrations de missions affichees a cote de leur
 * description dans la liste des missions disponibles.
 */
class IllustrationsMissionsTest {

    private final IllustrationsMissions illustrations = new IllustrationsMissions();

    @Test
    void uneMissionIllustreeRenvoieUneDataUriJpeg() {
        String uri = illustrations.dataUri("possession");

        assertThat(uri).startsWith("data:image/jpeg;base64,");
    }

    @Test
    void uneMissionSansIllustrationNeRenvoieRien() {
        assertThat(illustrations.dataUri("mission-inconnue")).isNull();
    }
}
