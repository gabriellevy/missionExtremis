package com.extremis.web;

import java.util.List;
import java.util.Map;

public record ProfilPersonnageView(
        String nom,
        String coterie,
        String role,
        String description,
        boolean vivant,
        int vitalite,
        int vitaliteDepart,
        int sangFroid,
        int sangFroidDepart,
        int skillBase,
        Map<String, Integer> competences,
        List<String> traits,
        List<String> specialites,
        List<String> maitrises,
        List<String> inventaire) {}
