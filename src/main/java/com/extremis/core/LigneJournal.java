package com.extremis.core;

/**
 * Une ligne du journal de mission : un texte, eventuellement
 * accompagne du jet de des qui l'a produite (affiche sous forme
 * d'images de des dans les logs).
 */
public record LigneJournal(String texte, JetDes jet) {

    public LigneJournal(String texte) {
        this(texte, null);
    }

    public boolean aUnJet() {
        return jet != null;
    }
}
