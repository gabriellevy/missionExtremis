package com.extremis.web;

import java.time.Instant;
import java.util.List;

public record OngletMissionView(
        Long id,
        String titre,
        List<String> journal,
        List<TeamMemberView> equipe,
        String tempsAvantProchainEvenement,
        Long secondesAvantProchainEvenement,
        Instant heureProchainEvenement,
        String heureVirtuelle,
        boolean terminee) {}
