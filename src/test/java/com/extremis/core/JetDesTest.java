package com.extremis.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JetDesTest {

    @Test
    void leJetEstDecomposeEnDeuxDesDeDixFaces() {
        assertThat(JetDes.d100(1)).isEqualTo(new JetDes(0, 1));
        assertThat(JetDes.d100(42)).isEqualTo(new JetDes(4, 2));
        assertThat(JetDes.d100(97)).isEqualTo(new JetDes(9, 7));
    }

    @Test
    void leCentSeLitZeroDizaineZeroUnite() {
        JetDes cent = JetDes.d100(100);
        assertThat(cent.dizaines()).isZero();
        assertThat(cent.unites()).isZero();
        assertThat(cent.valeur()).isEqualTo(100);
    }

    @Test
    void laValeurReconstitueeCorrespondAuJet() {
        for (int v = 1; v <= 100; v++) {
            assertThat(JetDes.d100(v).valeur()).isEqualTo(v);
        }
    }
}
