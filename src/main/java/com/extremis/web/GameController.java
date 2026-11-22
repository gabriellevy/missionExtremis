package com.extremis.web;

import com.extremis.core.*;
import com.extremis.core.LigneJournal;
import com.extremis.core.Character;
import com.extremis.db.CharacterEntity;
import com.extremis.db.EntityMapper;
import com.extremis.db.MissionExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/")
public class GameController {
    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService game;
    private final List<Mission> missions;
    private final PortraitService portraits;
    private final ServiceDes serviceDes;
    private final ServiceWikiNotion serviceWiki;
    private final boolean modeDebug;
    private final Map<Long, EtatMission> missionsActives = new LinkedHashMap<>();

    public GameController(GameService game, List<Mission> missions, PortraitService portraits,
                          ServiceDes serviceDes, ServiceWikiNotion serviceWiki,
                          @Value("${extremis.debug:false}") boolean modeDebug) {
        this.game = game;
        this.missions = missions;
        this.portraits = portraits;
        this.serviceDes = serviceDes;
        this.serviceWiki = serviceWiki;
        this.modeDebug = modeDebug;
    }

    private static final class EtatMission {
        MissionExecution execution;
        MissionExecutionEntity entity;
        List<LigneJournalView> journal = new ArrayList<>();
        int lignesPubliees = 0;
        int lignesConstatesAccueil = -1;
        Duration decalageTemps = Duration.ZERO;
        boolean fini = false;
    }

    /**
     * Missions en cours ou terminees avec les evenements survenus dans
     * chacune depuis la derniere visite de l'onglet Accueil ; met a
     * jour le repere de lecture pour la prochaine visite.
     */
    private List<MissionAccueilView> missionsAccueil() {
        List<MissionAccueilView> missions = new ArrayList<>();
        for (Map.Entry<Long, EtatMission> e : missionsActives.entrySet()) {
            EtatMission etat = e.getValue();
            List<LigneJournalView> recents = etat.lignesConstatesAccueil < 0
                    ? etat.journal
                    : etat.journal.subList(etat.lignesConstatesAccueil, etat.journal.size());
            missions.add(new MissionAccueilView(
                    e.getKey(),
                    etat.entity.getMissionTitle(),
                    etat.fini,
                    new ArrayList<>(recents)));
            etat.lignesConstatesAccueil = etat.journal.size();
        }
        return missions;
    }

    private Instant maintenant(EtatMission etat) {
        return Instant.now().plus(etat.decalageTemps);
    }

    public static final String ONGLET_EXTREMIS = "extremis";
    public static final String ONGLET_ACCUEIL = "accueil";
    public static final String FILTRE_ARRANGEURS = "arrangeurs";
    public static final String FILTRE_RECRUTEMENT = "recrutement";
    public static final String FILTRE_MISSIONS = "missions";

    @GetMapping
    @Transactional
    public String accueil(Model model, @RequestParam(required = false) String onglet,
                          @RequestParam(required = false) String filtre) {
        game.gagnerJokerQuotidien(Instant.now());
        rechargerMissionsActives();
        List<OngletMissionView> onglets = new ArrayList<>();
        for (Map.Entry<Long, EtatMission> e : missionsActives.entrySet()) {
            EtatMission etat = e.getValue();
            onglets.add(new OngletMissionView(
                    e.getKey(),
                    etat.entity.getMissionTitle(),
                    etat.execution.lieu().label(),
                    etat.journal,
                    missionTeam(etat),
                    etat.fini ? null : tempsAvantProchainEvenement(etat),
                    etat.fini ? null : secondesAvantProchainEvenement(etat),
                    etat.execution.nextEventTime(),
                    heureFrancaise(maintenant(etat)),
                    etat.fini,
                    !etat.fini && etat.execution.peutCorrigerDernierTest(),
                    descriptionDernierTestRate(etat),
                    remplacantsDisponibles(),
                    etat.execution.lieu().latitude(),
                    etat.execution.lieu().longitude()));
        }
        boolean aliasPersonnages = "personnages".equals(onglet);
        boolean aliasMissions = "missions".equals(onglet);
        OngletMissionView ongletMission = null;
        if (onglet != null && !aliasPersonnages && !aliasMissions && !ONGLET_ACCUEIL.equals(onglet)) {
            try {
                long id = Long.parseLong(onglet);
                ongletMission = onglets.stream().filter(o -> o.id() == id).findFirst().orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }
        boolean ongletAccueil = ONGLET_ACCUEIL.equals(onglet);
        boolean ongletExtremis = !ongletAccueil && ongletMission == null;
        String filtreExtremis;
        if (aliasPersonnages) {
            filtreExtremis = FILTRE_RECRUTEMENT;
        } else if (FILTRE_ARRANGEURS.equals(filtre) || FILTRE_RECRUTEMENT.equals(filtre)) {
            filtreExtremis = filtre;
        } else {
            filtreExtremis = FILTRE_MISSIONS;
        }
        model.addAttribute("ongletExtremis", ongletExtremis);
        model.addAttribute("filtreExtremis", filtreExtremis);
        model.addAttribute("categoriesCarte", categoriesVisibles(filtreExtremis));
        model.addAttribute("ongletAccueil", ongletAccueil);
        model.addAttribute("missionsAccueil", ongletAccueil ? missionsAccueil() : List.of());
        model.addAttribute("roster", game.roster());
        model.addAttribute("profils", profilsRoster());
        model.addAttribute("disponibles", game.availableCharacters());
        boolean recrutementDisponible = game.peutRecruter(Instant.now());
        model.addAttribute("recrutementDisponible", recrutementDisponible);
        Instant prochainRecrutement = game.prochainRecrutement();
        model.addAttribute("prochainRecrutement",
                recrutementDisponible || prochainRecrutement == null
                        ? null
                        : heureFrancaise(prochainRecrutement));
        model.addAttribute("missions", missionsDisponibles());
        model.addAttribute("missionsActives", onglets);
        model.addAttribute("ongletActif", ongletMission);
        model.addAttribute("rosterAvecMission", rosterAvecMission());
        model.addAttribute("marqueursCarte", marqueursCarte());
        model.addAttribute("carteLatitudeCentre", Lieu.CARTE_LATITUDE_CENTRE);
        model.addAttribute("carteLongitudeCentre", Lieu.CARTE_LONGITUDE_CENTRE);
        model.addAttribute("carteZoomInitial", Lieu.CARTE_ZOOM_INITIAL);
        model.addAttribute("carteZoomPasLeger", Lieu.ZOOM_PAS_LEGER);
        model.addAttribute("carteZoomPasFort", Lieu.ZOOM_PAS_FORTE);
        model.addAttribute("jokers", game.jokers());
        model.addAttribute("simulations", game.simulationsLeft());
        model.addAttribute("modeDebug", modeDebug);
        model.addAttribute("portraitService", portraits);
        model.addAttribute("serviceDes", serviceDes);
        return "home";
    }

    /**
     * Catégories de marqueurs visibles par défaut sur la carte selon
     * le sous-onglet actif de l'onglet Extremis ; le joueur peut ensuite
     * les contredire via l'interface en bas de carte.
     */
    private List<String> categoriesVisibles(String filtre) {
        return switch (filtre) {
            case FILTRE_ARRANGEURS -> List.of("disponible", "en-mission", "mort");
            case FILTRE_RECRUTEMENT -> List.of("disponible", "recrutable", "mort");
            default -> List.of("disponible", "mission", "mission-active");
        };
    }

    @GetMapping("/wiki/liens")
    @ResponseBody
    public List<LienWikiView> liensWiki() {
        return serviceWiki.liensAleatoires(5);
    }

    private List<ProfilPersonnageView> profilsRoster() {
        List<ProfilPersonnageView> profils = new ArrayList<>();
        for (CharacterEntity entity : game.tousLesPersonnages()) {
            Character c = EntityMapper.toCore(entity);
            Map<String, Integer> competences = new LinkedHashMap<>();
            c.skillsSnapshot().forEach((skill, value) -> competences.put(skill.label(), c.skillTarget(skill)));
            List<String> traits = new ArrayList<>();
            c.traits().forEach(t -> traits.add(t.name()));
            profils.add(new ProfilPersonnageView(
                    entity.getName(),
                    entity.getCoterie(),
                    entity.getCarriere(),
                    entity.getLieu() == null ? "—" : entity.getLieu().label(),
                    entity.getDescription(),
                    c.isAlive(),
                    c.vitalite(),
                    c.vitaliteDepart(),
                    c.sangFroid(),
                    c.sangFroidDepart(),
                    c.skillBase(),
                    competences,
                    traits,
                    new ArrayList<>(c.specialites()),
                    new ArrayList<>(c.maitrises()),
                    new ArrayList<>(c.sorts()),
                    c.inventory()));
        }
        return profils;
    }

    private List<Mission> missionsDisponibles() {
        List<String> enCours = missionsActives.values().stream()
                .filter(e -> !e.fini)
                .map(e -> e.entity.getMissionId())
                .toList();
        return missions.stream().filter(m -> !enCours.contains(m.id())).toList();
    }

    private record CarteMarqueurView(
            String type,
            String nom,
            String cible,
            String titre,
            boolean recrute,
            boolean mort,
            boolean enMission,
            int decalage,
            double latitude,
            double longitude) {}

    /**
     * Construit les marqueurs de la carte du monde pour l'onglet Missions :
     * tous les personnages (recrutés, recrutable et morts) à leur lieu réel,
     * les missions disponibles et les missions actives.
     */
    private List<CarteMarqueurView> marqueursCarte() {
        List<CarteMarqueurView> marqueurs = new ArrayList<>();

        Map<String, String> personnagesEnMission = rosterAvecMission();
        Map<Lieu, Integer> numerosAuMemeLieu = new EnumMap<>(Lieu.class);
        for (CharacterEntity entity : game.tousLesPersonnages()) {
            Lieu lieu = entity.getLieu();
            if (lieu == null) {
                continue;
            }
            int decalage = numerosAuMemeLieu.merge(lieu, 1, Integer::sum) - 1;
            marqueurs.add(new CarteMarqueurView(
                    "personnage",
                    entity.getName(),
                    entity.getName(),
                    null,
                    entity.isRecrute(),
                    !entity.isAlive(),
                    personnagesEnMission.containsKey(entity.getName()),
                    decalage,
                    lieu.latitude(),
                    lieu.longitude()));
        }
        for (Mission mission : missionsDisponibles()) {
            marqueurs.add(new CarteMarqueurView(
                    "mission",
                    null,
                    mission.id(),
                    mission.titre(),
                    false,
                    false,
                    false,
                    0,
                    mission.lieu().latitude(),
                    mission.lieu().longitude()));
        }
        for (EtatMission etat : missionsActives.values()) {
            marqueurs.add(new CarteMarqueurView(
                    "mission-active",
                    null,
                    String.valueOf(etat.entity.getId()),
                    etat.entity.getMissionTitle(),
                    false,
                    false,
                    false,
                    0,
                    etat.execution.lieu().latitude(),
                    etat.execution.lieu().longitude()));
        }
        return marqueurs;
    }

    private Set<String> personnagesOccupes() {
        Set<String> occupes = new HashSet<>();
        for (EtatMission etat : missionsActives.values()) {
            if (etat.fini) {
                continue;
            }
            for (Character c : etat.execution.teamSnapshot()) {
                if (c.isActif()) {
                    occupes.add(c.id());
                    occupes.add(c.name());
                }
            }
        }
        return occupes;
    }

    private Map<String, String> rosterAvecMission() {
        Map<String, String> resultat = new LinkedHashMap<>();
        for (EtatMission etat : missionsActives.values()) {
            if (etat.fini) {
                continue;
            }
            for (String nom : etat.execution.teamSnapshot().stream()
                    .filter(Character::isActif)
                    .map(Character::name)
                    .toList()) {
                resultat.put(nom, etat.entity.getMissionTitle());
            }
        }
        return resultat;
    }

    private List<TeamMemberView> missionTeam(EtatMission etat) {
        List<Character> snapshot = etat.execution.teamSnapshot();
        List<Character> roster = game.roster();
        List<TeamMemberView> views = new ArrayList<>();
        for (Character c : snapshot) {
            Character rosterEntry = roster.stream()
                    .filter(r -> r.id().equals(c.id()) || r.name().equals(c.name()))
                    .findFirst()
                    .orElse(null);
            Map<String, Integer> skills = new LinkedHashMap<>();
            List<String> traits = new ArrayList<>();
            List<String> specialites = new ArrayList<>();
            List<String> maitrises = new ArrayList<>();
            if (rosterEntry != null) {
                rosterEntry.skillsSnapshot().forEach((skill, value) -> skills.put(skill.label(), rosterEntry.skillTarget(skill)));
                rosterEntry.traits().forEach(t -> traits.add(t.name()));
                rosterEntry.specialites().forEach(specialites::add);
                rosterEntry.maitrises().forEach(maitrises::add);
            }
            views.add(new TeamMemberView(
                    c.id(),
                    c.name(),
                    c.vitalite(),
                    c.vitaliteDepart(),
                    c.sangFroid(),
                    c.sangFroidDepart(),
                    c.isAlive(),
                    rosterEntry != null ? rosterEntry.skillBase() : Character.DEFAULT_SKILL_BASE,
                    skills,
                    traits,
                    specialites,
                    maitrises));
        }
        return views;
    }

    private static final ZoneId FUSEAU_PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter FORMAT_HEURE =
            DateTimeFormatter.ofPattern("H'h'mm", Locale.FRANCE).withZone(FUSEAU_PARIS);
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.FRANCE).withZone(FUSEAU_PARIS);

    private String heureFrancaise(Instant instant) {
        return FORMAT_HEURE.format(instant) + " le " + FORMAT_DATE.format(instant);
    }

    private Long secondesAvantProchainEvenement(EtatMission etat) {
        Instant prochain = etat.execution.nextEventTime();
        if (prochain == null) {
            return null;
        }
        Duration restant = Duration.between(maintenant(etat), prochain);
        if (restant.isNegative()) {
            restant = Duration.ZERO;
        }
        return restant.toSeconds();
    }

    private String tempsAvantProchainEvenement(EtatMission etat) {
        Instant prochain = etat.execution.nextEventTime();
        if (prochain == null) {
            return null;
        }
        Duration restant = Duration.between(maintenant(etat), prochain);
        if (restant.isNegative()) {
            restant = Duration.ZERO;
        }
        if (restant.toDays() > 0) {
            return String.format("%dj%02dh%02dmn%02ds",
                    restant.toDays(), restant.toHoursPart(), restant.toMinutesPart(), restant.toSecondsPart());
        }
        return String.format("%dh%02dmn%02ds",
                restant.toHours(), restant.toMinutesPart(), restant.toSecondsPart());
    }

    @PostMapping("/debug/reinitialiser-usine")
    public String reinitialiserUsine() {
        if (!modeDebug) {
            return "redirect:/";
        }
        game.reinitialiserUsine();
        missionsActives.clear();
        return "redirect:/";
    }

    @PostMapping("/recruter")
    public String recruter(@RequestParam String characterId) {
        try {
            game.recruit(characterId, Instant.now());
        } catch (IllegalStateException | java.util.NoSuchElementException e) {
            log.info("Recrutement refuse : {}", e.getMessage());
        }
        return "redirect:/?onglet=" + ONGLET_EXTREMIS + "&filtre=" + FILTRE_RECRUTEMENT;
    }

    @PostMapping("/mission/run")
    @Transactional
    public String lancerMission(@RequestParam String missionId, @RequestParam(required = false) List<String> teamIds) {
        Mission mission = missions.stream().filter(m -> m.id().equals(missionId)).findFirst().orElse(null);
        if (mission == null) {
            return "redirect:/";
        }
        rechargerMissionsActives();
        if (missionsActives.values().stream().anyMatch(e -> e.entity.getMissionId().equals(missionId))) {
            return "redirect:/";
        }
        List<Character> equipe = new ArrayList<>();
        if (teamIds != null) {
            for (String id : teamIds) {
                game.characterById(id).ifPresent(equipe::add);
            }
        }
        if (equipe.isEmpty() || equipe.size() > 4) {
            log.info("Lancement de mission refuse : equipe invalide ({} membres).", equipe.size());
            return "redirect:/";
        }
        if (equipe.stream().anyMatch(c -> !c.isAlive())) {
            log.info("Lancement de mission refuse : un personnage de l'equipe est mort.");
            return "redirect:/";
        }
        Set<String> occupes = personnagesOccupes();
        if (equipe.stream().anyMatch(c -> occupes.contains(c.id()) || occupes.contains(c.name()))) {
            log.info("Lancement de mission refuse : un personnage de l'equipe est deja en mission.");
            return "redirect:/";
        }
        MissionExecutionEntity entity = game.startMission(mission, equipe, Instant.now());
        MissionExecution execution = game.rebuild(entity).orElse(null);
        if (execution == null) {
            return "redirect:/";
        }
        EtatMission etat = new EtatMission();
        etat.entity = entity;
        etat.execution = execution;
        Instant maintenant = Instant.now();
        etat.journal.add(LigneJournalView.texte(
                "Lancée le " + FORMAT_DATE.format(maintenant) + " à " + FORMAT_HEURE.format(maintenant) + "."));
        missionsActives.put(entity.getId(), etat);
        tick();
        return "redirect:/?onglet=" + entity.getId();
    }

    @PostMapping("/mission/{id}/advance")
    @Transactional
    public String avancerMission(@PathVariable Long id) {
        if (!modeDebug) {
            return "redirect:/";
        }
        rechargerMissionsActives();
        EtatMission etat = missionsActives.get(id);
        if (etat == null || etat.fini || etat.execution.isFinished()) {
            return "redirect:/";
        }
        Instant prochain = etat.execution.nextEventTime();
        if (prochain != null && prochain.isAfter(maintenant(etat))) {
            etat.decalageTemps = Duration.between(Instant.now(), prochain);
        }
        tickMission(etat);
        return "redirect:/?onglet=" + id;
    }

    @PostMapping("/mission/{id}/joker/reussite")
    @Transactional
    public String depenserJokerPourReussiteAutomatique(@PathVariable Long id) {
        rechargerMissionsActives();
        EtatMission etat = missionsActives.get(id);
        if (etat == null || etat.fini || etat.execution.isFinished()
                || game.jokers() <= 0 || !etat.execution.peutCorrigerDernierTest()) {
            return "redirect:/?onglet=" + id;
        }
        if (!game.spendJoker()) {
            return "redirect:/?onglet=" + id;
        }
        etat.execution.corrigerDernierTestEnReussite(maintenant(etat));
        game.saveProgress(etat.entity, etat.execution, maintenant(etat));
        synchroniserJournal(etat);
        terminerMissionSiFinie(etat);
        return "redirect:/?onglet=" + id;
    }

    @PostMapping("/mission/{id}/joker/remplacer")
    @Transactional
    public String depenserJokerPourRemplacer(@PathVariable Long id,
                                             @RequestParam String membreId,
                                             @RequestParam String remplaceParId) {
        rechargerMissionsActives();
        EtatMission etat = missionsActives.get(id);
        if (etat == null || etat.fini || etat.execution.isFinished() || game.jokers() <= 0) {
            return "redirect:/?onglet=" + id;
        }
        List<Character> equipe = etat.execution.teamSnapshot();
        int index = -1;
        for (int i = 0; i < equipe.size(); i++) {
            if (equipe.get(i).id().equals(membreId)) {
                index = i;
                break;
            }
        }
        Character remplacant = game.characterById(remplaceParId).orElse(null);
        Set<String> occupes = personnagesOccupes();
        if (index < 0 || remplacant == null || !remplacant.isAlive()
                || occupes.contains(remplacant.id()) || occupes.contains(remplacant.name())
                || equipe.stream().anyMatch(c -> c.id().equals(remplacant.id()))) {
            return "redirect:/?onglet=" + id;
        }
        if (!game.spendJoker()) {
            return "redirect:/?onglet=" + id;
        }
        etat.execution.remplacerMembre(index, remplacant);
        game.saveProgress(etat.entity, etat.execution, maintenant(etat));
        synchroniserJournal(etat);
        return "redirect:/?onglet=" + id;
    }

    private void synchroniserJournal(EtatMission etat) {
        List<LigneJournal> lignes = etat.execution.logLines();
        etat.journal.clear();
        lignes.forEach(l -> etat.journal.add(versVue(l)));
        etat.lignesPubliees = lignes.size();
    }

    private void terminerMissionSiFinie(EtatMission etat) {
        if (etat.fini || !etat.execution.isFinished()) {
            return;
        }
        MissionReport report = etat.execution.report();
        etat.journal.add(LigneJournalView.texte(report.teamWiped()
                ? "Équipe éliminée. Mission échouée."
                : etat.execution.isReussie()
                        ? "Mission réussie. Survivants : " + report.survivors()
                        : "Mission terminée en échec. Survivants : " + report.survivors()));
        game.finish(etat.entity, report, Instant.now());
        etat.fini = true;
    }

    private String descriptionDernierTestRate(EtatMission etat) {
        MissionExecution.DernierTestRate dt = etat.execution.dernierTestRate();
        if (dt == null) {
            return null;
        }
        return dt.evenementTitre() + " — " + dt.competence() + " (" + dt.testeurNom() + ")";
    }

    private List<RemplacantView> remplacantsDisponibles() {
        Set<String> occupes = personnagesOccupes();
        return game.roster().stream()
                .filter(Character::isAlive)
                .filter(c -> !occupes.contains(c.id()) && !occupes.contains(c.name()))
                .map(c -> new RemplacantView(c.id(), c.name()))
                .toList();
    }

    @Scheduled(fixedRateString = "${mission.tick-rate-ms:60000}")
    @Transactional
    public void tick() {
        game.gagnerJokerQuotidien(Instant.now());
        rechargerMissionsActives();
        if (missionsActives.isEmpty()) {
            return;
        }
        for (EtatMission etat : List.copyOf(missionsActives.values())) {
            if (!etat.fini && !etat.execution.isFinished()) {
                tickMission(etat);
            }
        }
    }

    private void tickMission(EtatMission etat) {
        if (etat.fini) {
            return;
        }
        etat.execution.advance(maintenant(etat));
        List<LigneJournal> lines = etat.execution.logLines();
        if (lines.size() > etat.lignesPubliees) {
            lines.subList(etat.lignesPubliees, lines.size()).forEach(l -> etat.journal.add(versVue(l)));
            log.info("Tick : {} evenement(s) resolu(s)", lines.size() - etat.lignesPubliees);
            etat.lignesPubliees = lines.size();
        }
        game.saveProgress(etat.entity, etat.execution, maintenant(etat));
        terminerMissionSiFinie(etat);
    }

    private static LigneJournalView versVue(com.extremis.db.ExecutionLogLineEntity l) {
        if ("jet".equals(l.getType()) && l.getDeDizaines() != null && l.getDeUnites() != null) {
            return LigneJournalView.jet(l.getLine(), l.getDeDizaines(), l.getDeUnites());
        }
        if ("titre".equals(l.getType())) {
            return LigneJournalView.titre(l.getLine());
        }
        if ("ambiance".equals(l.getType())) {
            return LigneJournalView.ambiance(l.getLine());
        }
        return LigneJournalView.texte(l.getLine());
    }

    private static LigneJournalView versVue(LigneJournal l) {
        if (l.aUnJet()) {
            return LigneJournalView.jet(l.texte(), l.jet().dizaines(), l.jet().unites());
        }
        return switch (l.type()) {
            case TITRE -> LigneJournalView.titre(l.texte());
            case AMBIANCE -> LigneJournalView.ambiance(l.texte());
            case TEXTE -> LigneJournalView.texte(l.texte());
        };
    }

    private void rechargerMissionsActives() {
        List<MissionExecutionEntity> entites = game.activeExecutionEntities();
        missionsActives.keySet().removeIf(id -> {
            EtatMission etat = missionsActives.get(id);
            return !etat.fini && entites.stream().noneMatch(e -> id.equals(e.getId()));
        });
        for (MissionExecutionEntity entity : entites) {
            if (missionsActives.containsKey(entity.getId())) {
                continue;
            }
            MissionExecution execution = game.rebuild(entity).orElse(null);
            if (execution == null) {
                continue;
            }
            EtatMission etat = new EtatMission();
            etat.entity = entity;
            etat.execution = execution;
            List<LigneJournal> historique = execution.logLines();
            etat.lignesPubliees = historique.size();
            historique.forEach(l -> etat.journal.add(versVue(l)));
            missionsActives.put(entity.getId(), etat);
        }
    }

    @Transactional
    void simulerRedemarrage() {
        missionsActives.clear();
    }

    @Transactional
    void rechargerDepuisLaBase() {
        rechargerMissionsActives();
    }

    @Transactional
    void avancerMissionJusquAuBout() {
        rechargerMissionsActives();
        for (EtatMission etat : List.copyOf(missionsActives.values())) {
            int gardesFous = 0;
            while (!etat.fini && !etat.execution.isFinished() && gardesFous++ < 1000) {
                Instant prochain = etat.execution.nextEventTime();
                if (prochain != null && prochain.isAfter(maintenant(etat))) {
                    etat.decalageTemps = Duration.between(Instant.now(), prochain);
                }
                tickMission(etat);
            }
        }
    }

    @Transactional
    void avancerMissionEvenementSuivant() {
        if (missionsActives.isEmpty()) {
            return;
        }
        avancerMission(missionsActives.keySet().iterator().next());
    }

    void recrutementRapide(String name) {
        game.viderRoster();
        game.saveQuickRecruit(new Character("test-" + name, name)
                .withSkill(Competence.DISCRETION, 30)
                .withSkill(Competence.ARMES_CORPS_A_CORPS, 30));
        missionsActives.clear();
    }

    void lancementRapide() {
        lancerMission(missions.get(0).id(), List.of("test-" + game.roster().get(0).name()));
    }

    String journal() {
        rechargerMissionsActives();
        return missionsActives.values().stream()
                .flatMap(e -> e.journal.stream())
                .map(LigneJournalView::texte)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
