package com.extremis.web;

import java.util.List;
import java.util.Map;

public record TeamMemberView(
        String id,
        String name,
        int vitalite,
        int vitaliteDepart,
        int sangFroid,
        int sangFroidDepart,
        boolean alive,
        int skillBase,
        Map<String, Integer> skills,
        List<String> traits,
        List<String> specialites,
        List<String> maitrises) {}
