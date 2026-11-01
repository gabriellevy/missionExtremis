package com.extremis.web;

/**
 * Ligne du journal affichee dans l'onglet mission : le texte, plus
 * eventuellement les deux faces du jet de D100 (dizaines puis
 * unites) quand la ligne est le resultat d'un test.
 */
public record LigneJournalView(
        String texte,
        boolean aUnJet,
        Integer deDizaines,
        Integer deUnites) {

    public static LigneJournalView texte(String texte) {
        return new LigneJournalView(texte, false, null, null);
    }

    public static LigneJournalView jet(String texte, int dizaines, int unites) {
        return new LigneJournalView(texte, true, dizaines, unites);
    }
}
