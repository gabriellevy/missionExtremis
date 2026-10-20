package com.extremis.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Une mission (wiki, page "Missions") : une succession d'evenements
 * dont les reussites ou echecs menent a d'autres evenements jusqu'a
 * l'echec ou la reussite finale. Chaque titre est une balise pouvant
 * etre ciblee par un "Aller a".
 */
public record Mission(String id, String titre, String brief, List<Competence> competencesCritiques, List<MissionEvent> evenements) {

    public Mission {
        evenements = List.copyOf(evenements);
        competencesCritiques = List.copyOf(competencesCritiques);
    }

    public MissionEvent evenement(String id) {
        return evenements.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evenement inconnu : " + id));
    }

    public Map<String, MissionEvent> evenementsParId() {
        return evenements.stream().collect(Collectors.toMap(MissionEvent::id, Function.identity()));
    }
}
