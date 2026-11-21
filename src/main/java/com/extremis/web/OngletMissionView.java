package com.extremis.web;

import java.time.Instant;
import java.util.List;

public record OngletMissionView(
        Long id,
        String titre,
        String lieu,
        List<LigneJournalView> journal,
        List<TeamMemberView> equipe,
        String tempsAvantProchainEvenement,
        Long secondesAvantProchainEvenement,
        Instant heureProchainEvenement,
        String heureVirtuelle,
        boolean terminee,
        boolean peutCorrigerDernierTest,
        String dernierTestRate,
        List<RemplacantView> remplacants,
        double lieuLatitude,
        double lieuLongitude) {}
