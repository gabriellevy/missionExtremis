package com.extremis.core;

import java.util.List;

public record Mission(String id, String title, String brief, List<Competence> criticalSkills, List<MissionEvent> events) {
    public Mission {
        events = List.copyOf(events);
        criticalSkills = List.copyOf(criticalSkills);
    }
}
