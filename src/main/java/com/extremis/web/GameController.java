package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class GameController {
    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService game;
    private final List<Mission> missions;
    private final PortraitService portraits;
    private final boolean modeDebug;
    private final Map<Long, EtatMission> missionsActives = new LinkedHashMap<>();

    public GameController(GameService game, List<Mission> missions, PortraitService portraits,
                          @Value("${extremis.debug:false}") boolean modeDebug) {
        this.game = game;
        this.missions = missions;
        this.portraits = portraits;
        this.modeDebug = modeDebug;
    }

    private static final class EtatMission {
        MissionExecution execution;
        MissionExecutionEntity entity;
        List<String> journal = new ArrayList<>();
        int lignesPubliees = 0;
        Duration decalageTemps = Duration.ZERO;
        boolean fini = false;
    }

    private Instant maintenant(EtatMission etat) {
        return Instant.now().plus(etat.decalageTemps);
    }

    public static final String ONGLET_PERSONNAGES = "personnages";
    public static final String ONGLET_MISSIONS = "missions";

    @GetMapping
    @Transactional
    public String accueil(Model model, @RequestParam(required = false) String onglet) {
        rechargerMissionsActives();
        List<OngletMissionView> onglets = new ArrayList<>();
        for (Map.Entry<Long, EtatMission> e : missionsActives.entrySet()) {
            EtatMission etat = e.getValue();
            onglets.add(new OngletMissionView(
                    e.getKey(),
                    etat.entity.getMissionTitle(),
                    etat.journal,
                    missionTeam(etat),
                    etat.fini ? null : tempsAvantProchainEvenement(etat),
                    etat.execution.nextEventTime(),
                    maintenant(etat),
                    etat.fini));
        }
        OngletMissionView ongletMission = null;
        if (onglet != null && !ONGLET_PERSONNAGES.equals(onglet) && !ONGLET_MISSIONS.equals(onglet)) {
            try {
                long id = Long.parseLong(onglet);
                ongletMission = onglets.stream().filter(o -> o.id() == id).findFirst().orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }
        boolean ongletPersonnages = ONGLET_PERSONNAGES.equals(onglet);
        boolean ongletMissions = ONGLET_MISSIONS.equals(onglet);
        if (!ongletPersonnages && !ongletMissions && ongletMission == null && onglet != null) {
            ongletMissions = true;
        }
        model.addAttribute("ongletPersonnages", ongletPersonnages);
        model.addAttribute("ongletMissions", ongletMissions);
        model.addAttribute("roster", game.roster());
        model.addAttribute("disponibles", game.availableCharacters());
        model.addAttribute("missions", missionsDisponibles());
        model.addAttribute("missionsActives", onglets);
        model.addAttribute("ongletActif", ongletMission);
        model.addAttribute("rosterAvecMission", rosterAvecMission());
        model.addAttribute("jokers", game.jokers());
        model.addAttribute("simulations", game.simulationsLeft());
        model.addAttribute("modeDebug", modeDebug);
        model.addAttribute("portraitService", portraits);
        return "home";
    }

    private List<Mission> missionsDisponibles() {
        List<String> enCours = missionsActives.values().stream()
                .filter(e -> !e.fini)
                .map(e -> e.entity.getMissionId())
                .toList();
        return missions.stream().filter(m -> !enCours.contains(m.id())).toList();
    }

    private Map<String, String> rosterAvecMission() {
        Map<String, String> resultat = new LinkedHashMap<>();
        for (EtatMission etat : missionsActives.values()) {
            if (etat.fini) {
                continue;
            }
            for (String nom : etat.execution.teamSnapshot().stream().map(Character::name).toList()) {
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
            if (rosterEntry != null) {
                rosterEntry.skillsSnapshot().forEach((skill, value) -> skills.put(skill.label(), value));
                rosterEntry.traits().forEach(t -> traits.add(t.name()));
            }
            views.add(new TeamMemberView(
                    c.id(),
                    c.name(),
                    c.health(),
                    c.isAlive(),
                    rosterEntry != null ? rosterEntry.skillBase() : Character.DEFAULT_SKILL_BASE,
                    skills,
                    traits));
        }
        return views;
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
        return String.format("%02d:%02d:%02d",
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
        Character recrue = game.characterById(characterId).orElse(null);
        if (recrue == null) {
            return "redirect:/";
        }
        try {
            game.recruit(recrue, Instant.now());
        } catch (IllegalStateException e) {
            log.info("Recrutement refuse : {}", e.getMessage());
        }
        return "redirect:/";
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
        if (equipe.isEmpty()) {
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
        etat.journal.add("=== " + mission.title() + " ===");
        etat.journal.add("Mission lancee a " + Instant.now() + ". Les evenements suivront leur delai prevu.");
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

    @Scheduled(fixedRateString = "${mission.tick-rate-ms:60000}")
    @Transactional
    public void tick() {
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
        boolean finished = etat.execution.advance(maintenant(etat));
        List<String> lines = etat.execution.logLines();
        if (lines.size() > etat.lignesPubliees) {
            etat.journal.addAll(lines.subList(etat.lignesPubliees, lines.size()));
            log.info("Tick : {} evenement(s) resolu(s)", lines.size() - etat.lignesPubliees);
            etat.lignesPubliees = lines.size();
        }
        game.saveProgress(etat.entity, etat.execution, maintenant(etat));
        if (finished) {
            MissionReport report = etat.execution.report();
            etat.journal.add(report.teamWiped()
                    ? "Equipe eliminee. Mission echouee."
                    : "Mission terminee. Survivants : " + report.survivors());
            game.finish(etat.entity, report, Instant.now());
            etat.fini = true;
        }
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
            etat.lignesPubliees = entity.getLogLines().size();
            entity.getLogLines().forEach(l -> etat.journal.add(l.getLine()));
            missionsActives.put(entity.getId(), etat);
        }
    }

    void recrutementRapide(String name) {
        game.clearRoster();
        game.saveQuickRecruit(new Character("test-" + name, name)
                .withSkill(com.extremis.core.Skill.DISCRETION, 30)
                .withSkill(com.extremis.core.Skill.ARMES_CORPS_A_CORPS, 30));
        missionsActives.clear();
    }

    void lancementRapide() {
        lancerMission(missions.get(0).id(), List.of("test-" + game.roster().get(0).name()));
    }

    String journal() {
        rechargerMissionsActives();
        return missionsActives.values().stream()
                .flatMap(e -> e.journal.stream())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
