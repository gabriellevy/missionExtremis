package com.extremis.core;

/**
 * Une ligne du journal de mission : un texte, un type pour le rendu
 * formate (titre, ambiance, texte ordinaire), et eventuellement le
 * jet de des qui l'a produite (affiche sous forme d'images de des
 * dans les logs).
 */
public record LigneJournal(String texte, JetDes jet, TypeLigne type) {

    public LigneJournal(String texte) {
        this(texte, null, TypeLigne.TEXTE);
    }

    public LigneJournal(String texte, JetDes jet) {
        this(texte, jet, TypeLigne.TEXTE);
    }

    public static LigneJournal titre(String texte) {
        return new LigneJournal(texte, null, TypeLigne.TITRE);
    }

    public static LigneJournal ambiance(String texte) {
        return new LigneJournal(texte, null, TypeLigne.AMBIANCE);
    }

    public boolean aUnJet() {
        return jet != null;
    }
}
