package com.extremis.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final List<LigneJournal> logLines = new ArrayList<>();

    private String evenementCourant;
    private String introductionPublieePour;
    private int passagesIntroduction = 0;
    private Instant heureEffetCourant;
    private boolean attenteEffet;
    private boolean finished;
    private boolean reussie;
    private int evenementsResolus = 0;
    private SnapshotTest snapshotCorrigeable;
    private DernierTestRate dernierTestRate;
    private boolean forcerProchaineReussite;

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
        MissionExecution execution = new MissionExecution(mission, new ArrayList<>(team), resolveur,
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
                SnapshotTest snapshot = evenement.test().isPresent() ? prendreSnapshot() : null;
                attenteEffet = false;
                ResolveurDEvenement.ResultatEvenement resultat;
                if (forcerProchaineReussite) {
                    forcerProchaineReussite = false;
                    resultat = resolveur.resoudreAvecReussiteForcee(evenement, team, etat,
                            avecHorodatage(heureEffetCourant));
                } else {
                    resultat = resolveur.resoudre(evenement, team, etat, avecHorodatage(heureEffetCourant));
                }
                if (snapshot != null) {
                    enregistrerDernierTest(evenement, resultat, snapshot);
                }
                if (resultat.victoire()) {
                    reussie = true;
                    finished = true;
                    return true;
                }
                String suivant = resultat.evenementSuivant();
                if (suivant == null || plusAucunJoueurActif()) {
                    finished = true;
                    if (plusAucunJoueurActif()) {
                        logLines.add(new LigneJournal("Plus personne pour continuer : la mission echoue."));
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
        String cle = evenement.id() + "#" + passagesIntroduction;
        if (!cle.equals(introductionPublieePour)) {
            introductionPublieePour = cle;
            logLines.add(LigneJournal.titre("=== " + evenement.titre() + " ==="));
            if (evenement.introduction() != null) {
                logLines.add(LigneJournal.ambiance(evenement.introduction()));
            }
        }
    }

    private java.util.function.Consumer<LigneJournal> avecHorodatage(Instant heureEvenement) {
        String prefixe = "[" + FORMAT_HEURE_EVENEMENT.format(heureEvenement) + "] ";
        return ligne -> logLines.add(new LigneJournal(prefixe + ligne.texte(), ligne.jet(), ligne.type()));
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
                          java.util.Map<String, Integer> ajustements,
                          List<LigneJournal> historique) {
        if (evenementId == null || finished) {
            return;
        }
        this.evenementCourant = evenementId;
        this.heureEffetCourant = heureEffet;
        this.attenteEffet = true;
        etat.restaurerAjustements(ajustements);
        // l'historique du journal est recharge depuis la base : on le
        // restaure tel quel pour que les nouvelles lignes s'y ajoutent
        // a la suite sans re-publier les introductions deja affichees
        logLines.clear();
        if (historique != null) {
            logLines.addAll(historique);
        }
        if (!logLines.isEmpty()) {
            introductionPublieePour = evenementId + "#" + passagesIntroduction;
        }
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
        logLines.add(new LigneJournal("La mission s'enlise sans fin : le Consul met fin aux opérations."));
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

    /** Le lieu ou se rendre pour accomplir la mission. */
    public Lieu lieu() {
        return mission.lieu();
    }

    public List<LigneJournal> logLines() {
        return List.copyOf(logLines);
    }

    public RandomSource sourceAleatoire() {
        return resolveur.sourceAleatoire();
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
                String.join("\n", logLines.stream().map(LigneJournal::texte).toList()));
    }

    public record DernierTestRate(String evenementTitre, String competence, String testeurNom) {}

    private static final class SnapshotTest {
        String evenementCourant;
        Instant heureEffetCourant;
        int passagesIntroduction;
        String introductionPublieePour;
        int evenementsResolus;
        int tailleJournal;
        Map<String, Integer> ajustements;
        List<Character.EtatPersonnage> etatEquipe;
    }

    private SnapshotTest prendreSnapshot() {
        SnapshotTest s = new SnapshotTest();
        s.evenementCourant = evenementCourant;
        s.heureEffetCourant = heureEffetCourant;
        s.passagesIntroduction = passagesIntroduction;
        s.introductionPublieePour = introductionPublieePour;
        s.evenementsResolus = evenementsResolus;
        s.tailleJournal = logLines.size();
        s.ajustements = etat.snapshotAjustements();
        s.etatEquipe = new ArrayList<>();
        for (Character c : team) {
            s.etatEquipe.add(c.etatActuel());
        }
        return s;
    }

    private void enregistrerDernierTest(MissionEvent evenement,
                                        ResolveurDEvenement.ResultatEvenement resultat,
                                        SnapshotTest snapshot) {
        if (resultat.resultatTest() != null && !resultat.resultatTest().estReussite()) {
            snapshotCorrigeable = snapshot;
            String testeur = resultat.testeurId() == null ? "l'equipe" : team.stream()
                    .filter(c -> c.id().equals(resultat.testeurId()))
                    .findFirst()
                    .map(Character::name)
                    .orElse("l'equipe");
            dernierTestRate = new DernierTestRate(evenement.titre(),
                    evenement.test().orElseThrow().skill().label(), testeur);
        } else {
            snapshotCorrigeable = null;
            dernierTestRate = null;
        }
    }

    public boolean peutCorrigerDernierTest() {
        return snapshotCorrigeable != null && !finished;
    }

    public DernierTestRate dernierTestRate() {
        return peutCorrigerDernierTest() ? dernierTestRate : null;
    }

    public boolean corrigerDernierTestEnReussite(Instant maintenant) {
        if (!peutCorrigerDernierTest()) {
            return false;
        }
        SnapshotTest s = snapshotCorrigeable;
        evenementCourant = s.evenementCourant;
        heureEffetCourant = s.heureEffetCourant;
        attenteEffet = true;
        passagesIntroduction = s.passagesIntroduction;
        introductionPublieePour = s.introductionPublieePour;
        evenementsResolus = s.evenementsResolus;
        while (logLines.size() > s.tailleJournal) {
            logLines.remove(logLines.size() - 1);
        }
        etat.restaurerAjustements(s.ajustements);
        for (int i = 0; i < team.size() && i < s.etatEquipe.size(); i++) {
            team.get(i).restaurer(s.etatEquipe.get(i));
        }
        snapshotCorrigeable = null;
        dernierTestRate = null;
        logLines.add(new LigneJournal("Jeton joker d\u00e9pens\u00e9 \u2014 co\u00efncidence calcul\u00e9e :"
                + " le dernier test est annul\u00e9 et r\u00e9ussi automatiquement."));
        forcerProchaineReussite = true;
        advance(maintenant);
        return true;
    }

    public boolean remplacerMembre(int index, Character nouveau) {
        if (finished || nouveau == null || index < 0 || index >= team.size()) {
            return false;
        }
        Character ancien = team.get(index);
        team.set(index, nouveau);
        logLines.add(new LigneJournal("Co\u00efncidence calcul\u00e9e : " + nouveau.name()
                + " rejoint l'\u00e9quipe \u00e0 la vol\u00e9e et remplace " + ancien.name() + "."));
        return true;
    }
}
