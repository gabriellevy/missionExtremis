package com.extremis.core;

/**
 * Niveaux de reussite d'un test D100 (wiki, page "Competences") : la
 * marge est la difference entre la cible et le de.
 */
public enum ResultatTest {
    REUSSITE_CRITIQUE(true),
    REUSSITE(true),
    ECHEC(false),
    ECHEC_CRITIQUE(false);

    public static final int MARGE_REUSSITE_CRITIQUE = 50;
    public static final int MARGE_ECHEC_CRITIQUE = 50;

    private final boolean reussite;

    ResultatTest(boolean reussite) {
        this.reussite = reussite;
    }

    public boolean estReussite() {
        return reussite;
    }

    public static ResultatTest duJet(int cible, int de) {
        if (de < cible) {
            return cible - de >= MARGE_REUSSITE_CRITIQUE ? REUSSITE_CRITIQUE : REUSSITE;
        }
        return de - cible >= MARGE_ECHEC_CRITIQUE ? ECHEC_CRITIQUE : ECHEC;
    }
}
