package com.extremis.web;

import com.extremis.core.TypeLigne;

/**
 * Ligne du journal affichee dans l'onglet mission : le texte avec son
 * type de rendu (titre de taille moyenne, phrase d'ambiance en
 * italique, ou texte ordinaire), plus eventuellement les deux faces
 * du jet de D100 (dizaines puis unites) quand la ligne est le
 * resultat d'un test.
 */
public record LigneJournalView(
        String texte,
        TypeLigne type,
        boolean aUnJet,
        Integer deDizaines,
        Integer deUnites) {

    public static LigneJournalView texte(String texte) {
        return new LigneJournalView(texte, TypeLigne.TEXTE, false, null, null);
    }

    public static LigneJournalView titre(String texte) {
        return new LigneJournalView(texte, TypeLigne.TITRE, false, null, null);
    }

    public static LigneJournalView ambiance(String texte) {
        return new LigneJournalView(texte, TypeLigne.AMBIANCE, false, null, null);
    }

    public static LigneJournalView jet(String texte, int dizaines, int unites) {
        return new LigneJournalView(texte, TypeLigne.TEXTE, true, dizaines, unites);
    }

    public boolean estTitre() {
        return type == TypeLigne.TITRE;
    }

    public boolean estAmbiance() {
        return type == TypeLigne.AMBIANCE;
    }

    /**
     * Texte affiche : pour un titre, sans les "===" qui entourent le
     * texte brut (ils servent de marqueur, pas de decoration affichee).
     */
    public String texteAffiche() {
        if (estTitre() && texte.startsWith("=== ") && texte.endsWith(" ===")) {
            return texte.substring(4, texte.length() - 4);
        }
        return texte;
    }
}
