package com.extremis.core;

/**
 * Liste de competences du wiki (page "Liste de competences").
 * Les competences de base : tous les personnages peuvent les utiliser,
 * meme sans entrainement, avec leur base de competences.
 */
public enum Skill {
    ANIMAUX("Animaux"),
    ARMES_CORPS_A_CORPS("Armes de corps a corps"),
    BAGARRE("Bagarre"),
    CHANCE("Chance"),
    CHARME("Charme"),
    COMMANDEMENT("Commandement"),
    DISCRETION("Discrétion"),
    ELOQUENCE("Éloquence"),
    ENDURANCE("Endurance"),
    EVALUATION("Évaluation"),
    FORCE("Force"),
    FORCE_MENTALE("Force mentale"),
    HABILETE("Habilete"),
    INTELLIGENCE("Intelligence"),
    INTIMIDATION("Intimidation"),
    INTUITION("Intuition"),
    MAGIE("Magie"),
    MARCHANDAGE("Marchandage"),
    MOUVEMENT("Mouvement"),
    PERCEPTION("Perception"),
    PERIPLE("Périple"),
    RAGOT("Ragot"),
    REFLEXES("Réflexes"),
    RICHESSE("Richesse"),
    SURVIE_EXTERIEUR("Survie en extérieur"),
    TIR("Tir"),
    TROMPERIE("Tromperie"),
    VIGILANCE("Vigilance"),
    VOLONTE("Volonté");

    private final String label;

    Skill(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
