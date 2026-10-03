package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
import com.extremis.core.SeededRandom;
import com.extremis.db.CharacterEntity;
import com.extremis.db.MissionExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
public class GameController {
    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameService game;
    private final List<Mission> missions;
    private final List<String> eventLog = new ArrayList<>();
    private MissionExecution activeExecution;
    private MissionExecutionEntity activeEntity;
    private Duration timeOffset = Duration.ZERO;
    private int flushedLines = 0;

    public GameController(GameService game, List<Mission> missions) {
        this.game = game;
        this.missions = missions;
    }

    private Instant now() {
        return Instant.now().plus(timeOffset);
    }

    @GetMapping
    @Transactional
    public String home(Model model) {
        List<Character> roster = game.roster();
        model.addAttribute("roster", roster);
        model.addAttribute("available", game.availableCharacters());
        model.addAttribute("missions", missions);
        model.addAttribute("jokers", game.jokers());
        model.addAttribute("simulations", game.simulationsLeft());
        model.addAttribute("missionInProgress", activeExecution != null && !activeExecution.isFinished());
        model.addAttribute("nextEventTime", activeExecution == null ? null : activeExecution.nextEventTime());
        model.addAttribute("virtualNow", activeExecution == null ? null : now());
        model.addAttribute("log", String.join("\n", eventLog));
        return "home";
    }

    @PostMapping("/recruit")
    public String recruit(@RequestParam String characterId) {
        Character recruit = game.characterById(characterId).orElse(null);
        if (recruit == null) {
            eventLog.add("Personnage inconnu : " + characterId);
            return "redirect:/";
        }
        try {
            game.recruit(recruit, Instant.now());
            eventLog.add("Recrutement de " + recruit.name());
        } catch (IllegalStateException e) {
            eventLog.add(e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/mission/run")
    public String runMission(@RequestParam String missionId, @RequestParam(required = false) List<String> teamIds) {
        if (activeExecution != null && !activeExecution.isFinished()) {
            eventLog.add("Une mission est deja en cours.");
            return "redirect:/";
        }
        Mission mission = missions.stream().filter(m -> m.id().equals(missionId)).findFirst().orElse(null);
        if (mission == null) {
            return "redirect:/";
        }
        List<Character> team = new ArrayList<>();
        if (teamIds != null) {
            for (String id : teamIds) {
                game.characterById(id).ifPresent(team::add);
            }
        }
        if (team.isEmpty()) {
            eventLog.add("Aucun arrangeur selectionne pour " + mission.title());
            return "redirect:/";
        }
        timeOffset = Duration.ZERO;
        flushedLines = 0;
        activeEntity = game.startMission(mission, team, now());
        activeExecution = game.rebuild(activeEntity).orElse(null);
        eventLog.add("=== " + mission.title() + " ===");
        eventLog.add("Mission lancee a " + now() + ". Les evenements suivront leur delai prevu.");
        tick();
        return "redirect:/";
    }

    @PostMapping("/mission/advance")
    public String advanceMission() {
        if (activeExecution == null || activeExecution.isFinished()) {
            return "redirect:/";
        }
        Instant next = activeExecution.nextEventTime();
        if (next != null && next.isAfter(now())) {
            timeOffset = Duration.between(Instant.now(), next);
        }
        tick();
        return "redirect:/";
    }

    @Scheduled(fixedRateString = "${mission.tick-rate-ms:60000}")
    @Transactional
    public void tick() {
        if (activeExecution == null) {
            reloadActiveExecution();
            if (activeExecution == null) {
                return;
            }
        }
        if (activeExecution.isFinished()) {
            return;
        }
        boolean finished = activeExecution.advance(now());
        List<String> lines = activeExecution.logLines();
        if (lines.size() > flushedLines) {
            eventLog.addAll(lines.subList(flushedLines, lines.size()));
            log.info("Tick : {} evenement(s) resolu(s)", lines.size() - flushedLines);
            flushedLines = lines.size();
        }
        game.saveProgress(activeEntity, activeExecution, now());
        if (finished) {
            MissionReport report = activeExecution.report();
            eventLog.add(report.teamWiped()
                    ? "Equipe eliminee. Mission echouee."
                    : "Mission terminee. Survivants : " + report.survivors());
            game.finish(activeEntity, report, now());
            activeExecution = null;
            activeEntity = null;
        }
    }

    private void reloadActiveExecution() {
        MissionExecutionEntity entity = game.activeExecutionEntity();
        if (entity == null) {
            return;
        }
        activeEntity = entity;
        game.rebuild(entity).ifPresentOrElse(
                execution -> {
                    activeExecution = execution;
                    flushedLines = entity.getLogLines().size();
                    eventLog.clear();
                    eventLog.addAll(entity.getLogLines().stream().map(com.extremis.db.ExecutionLogLineEntity::getLine).toList());
                },
                () -> {
                    activeEntity = null;
                });
    }

    void recrutementRapide(String name) {
        game.clearRoster();
        game.saveQuickRecruit(new Character("test-" + name, name)
                .withSkill(com.extremis.core.Skill.DISCRETION, 80)
                .withSkill(com.extremis.core.Skill.COMBAT, 80));
        eventLog.clear();
        activeExecution = null;
        activeEntity = null;
    }

    void lancementRapide() {
        runMission(missions.get(0).id(), List.of("test-" + game.roster().get(0).name()));
    }

    String journal() {
        return String.join("\n", eventLog);
    }
}
