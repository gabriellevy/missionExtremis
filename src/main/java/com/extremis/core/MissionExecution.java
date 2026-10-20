package com.extremis.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Execution incrementale d'une mission conforme aux regles du wiki
 * (page "Missions") :
 * - chaque evenement a une duree ; au debut de la duree son texte
 *   d'introduction s'affiche, et seulement une fois la duree ecoulee
 *   son effet s'affiche, y compris les resultats des tests ;
 * - les reussites ou echecs menent a d'autres evenements ("aller a")
 *   jusqu'a la reussite ou l'echec finale ;
 * - "tant que" decrit une boucle a condition.
 */
public final class MissionExecution {
    private final Mission mission;
    private final List<Character> team;
    private final ResolveurDEvenement resolveur;
    private final ResolveurDEvenement.EtatMission etat;
    private final List<String> logLines = new ArrayList<>();

    private String evenementCourant;
    private String introductionPublieePour;
    private int passagesIntroduction = 0;
    private Instant heureEffetCourant;
    private boolean attenteEffet;
    private boolean finished;
    private boolean reussie;
    private int evenementsResolus = 0;

    private static final int MAX_EVENEMENTS = 1000;

    private static final DateTimeFormatter FORMAT_HEURE_EVENEMENT =
            DateTimeFormatter.ofPattern("H'h'mm", Locale.FRANCE).withZone(ZoneId.of("Europe/Paris"));

    private MissionExecution(Mission mission, List<Character> team, ResolveurDEvenement resolveur,
                             ResolveurDEvenement.EtatMission etat, String evenementCourant) {
        this.mission = mission;
        this.team = team;
        this.resolveur = resolveur;
        this.etat = etat;
        this.evenementCourant = evenementCourant;
    }

    public static MissionExecution start(Mission mission, List<Character> team, RandomSource random, Instant startTime) {
        ResolveurDEvenement resolveur = new ResolveurDEvenement(random);
        MissionExecution execution = new MissionExecution(mission, List.copyOf(team), resolveur,
                new ResolveurDEvenement.EtatMission(), mission.evenements().get(0).id());
        execution.heureEffetCourant = startTime.plus(mission.evenements().get(0).duree());
        execution.attenteEffet = true;
        return execution;
    }

    /**
     * Avance l'execution jusqu'a l'heure donnee : publie l'introduction
     * des evenements des le debut de leur duree, puis leur effet une
     * fois la duree ecoulee.
     *
     * @return true si la mission est terminee (reussite, echec finale
     *         ou equipe eliminee / entierement exclue)
     */
    public boolean advance(Instant now) {
        while (!finished) {
            MissionEvent evenement = mission.evenement(evenementCourant);
            publierIntroduction(evenement);
            if (heureEffetCourant.isAfter(now)) {
                return false;
            }
            if (attenteEffet) {
                attenteEffet = false;
                ResolveurDEvenement.ResultatEvenement resultat =
                        resolveur.resoudre(evenement, team, etat, avecHorodatage(heureEffetCourant));
                if (resultat.victoire()) {
                    reussie = true;
                    finished = true;
                    return true;
                }
                String suivant = resultat.evenementSuivant();
                if (suivant == null || plusAucunJoueurActif()) {
                    finished = true;
                    if (plusAucunJoueurActif()) {
                        logLines.add("Plus personne pour continuer : la mission echoue.");
                    }
                    return true;
                }
                if (++evenementsResolus >= MAX_EVENEMENTS) {
                    journalBloque();
                    finished = true;
                    return true;
                }
                evenementCourant = suivant;
                passagesIntroduction++;
                MissionEvent prochain = mission.evenement(suivant);
                heureEffetCourant = heureEffetCourant.plus(prochain.duree());
                attenteEffet = true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void publierIntroduction(MissionEvent evenement) {
        // chaque nouveau passage sur l'evenement republie son titre
        // (balise "aller a") et son introduction
        if (introductionPublieePour != evenement.id() + "#" + passagesIntroduction) {
            introductionPublieePour = evenement.id() + "#" + passagesIntroduction;
            logLines.add("=== " + evenement.titre() + " ===");
            if (evenement.introduction() != null) {
                logLines.add(evenement.introduction());
            }
        }
    }

    private java.util.function.Consumer<String> avecHorodatage(Instant heureEvenement) {
        String prefixe = "[" + FORMAT_HEURE_EVENEMENT.format(heureEvenement) + "] ";
        return ligne -> logLines.add(prefixe + ligne);
    }

    public boolean teamWiped() {
        return team.stream().noneMatch(Character::isAlive);
    }

    public boolean plusAucunJoueurActif() {
        return team.stream().noneMatch(Character::isActif);
    }

    /**
     * Reprise apres rechargement depuis la base : replace l'execution
     * sur l'evenement courant avec son heure d'effet et les
     * ajustements de difficulte accumules.
     */
    public void reprendre(String evenementId, java.time.Instant heureEffet,
                          java.util.Map<String, Integer> ajustements) {
        if (evenementId == null || finished) {
            return;
        }
        this.evenementCourant = evenementId;
        this.heureEffetCourant = heureEffet;
        this.attenteEffet = true;
        etat.restaurerAjustements(ajustements);
    }

    public java.util.Map<String, Integer> ajustementsDifficulte() {
        return etat.snapshotAjustements();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isReussie() {
        return reussie;
    }

    private void journalBloque() {
        logLines.add("La mission s'enlise sans fin : le Consul met fin aux operations.");
    }

    public String evenementCourant() {
        return evenementCourant;
    }

    public Instant heureEffetCourant() {
        return heureEffetCourant;
    }

    public Instant nextEventTime() {
        if (finished || !attenteEffet) {
            return null;
        }
        return heureEffetCourant;
    }

    public List<String> logLines() {
        return List.copyOf(logLines);
    }

    public List<Character> teamSnapshot() {
        return List.copyOf(team);
    }

    public MissionReport report() {
        return new MissionReport(
                mission.id(),
                team.stream().filter(c -> !c.isAlive()).map(Character::name).toList(),
                team.stream().filter(Character::isAlive).count(),
                teamWiped(),
                String.join("\n", logLines));
    }
}
