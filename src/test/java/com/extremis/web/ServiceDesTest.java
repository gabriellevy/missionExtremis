package com.extremis.web;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDesTest {

    private final ServiceDes service = new ServiceDes();

    @Test
    void leDeEstRenduEnSvgDataUri() {
        String uri = service.dataUri(7);
        assertThat(uri).startsWith("data:image/svg+xml;base64,");
        String svg = new String(Base64.getDecoder().decode(uri.substring("data:image/svg+xml;base64,".length())),
                StandardCharsets.UTF_8);
        assertThat(svg).contains("<svg").contains("<polygon").contains("<circle");
    }

    @Test
    void chaqueFacePossedeSesPips() {
        // 1 pip pour la face 1, 9 pips pour la face 9, aucun pour 0
        assertThat(nombreDePips(1)).isEqualTo(1);
        assertThat(nombreDePips(9)).isEqualTo(9);
        assertThat(nombreDePips(0)).isZero();
    }

    private int nombreDePips(int face) {
        String uri = service.dataUri(face);
        String svg = new String(Base64.getDecoder().decode(uri.substring("data:image/svg+xml;base64,".length())),
                StandardCharsets.UTF_8);
        return svg.split("<circle", -1).length - 1;
    }
}
