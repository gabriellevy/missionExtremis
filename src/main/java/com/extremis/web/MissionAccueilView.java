package com.extremis.web;

import java.util.List;

/**
 * Mission (en cours ou terminee) presentee dans l'onglet Accueil :
 * son titre, son etat et les evenements survenus depuis la derniere
 * visite de l'onglet Accueil.
 */
public record MissionAccueilView(
        Long id,
        String titre,
        boolean terminee,
        List<LigneJournalView> evenementsRecents) {}
