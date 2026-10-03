package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.ConsulState;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
import com.extremis.core.SeededRandom;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
public class GameController {
    private final ConsulState consul = new ConsulState();
    private final List<String> eventLog = new ArrayList<>();
    private MissionExecution activeExecution;

    public GameController() {
        consul.offerMission(com.extremis.catalog.Missions.foolsOfGotheim());
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("roster", consul.roster());
        model.addAttribute("missions", consul.missions());
        model.addAttribute("jokers", consul.jokers());
        model.addAttribute("simulations", consul.simulationsLeft());
        model.addAttribute("missionInProgress", activeExecution != null && !activeExecution.isFinished());
        model.addAttribute("nextEventTime", activeExecution == null ? null : activeExecution.nextEventTime());
        model.addAttribute("log", String.join("\n", eventLog));
        return "home";
    }

    @PostMapping("/recruit")
    public String recruit(@RequestParam String name) {
        Character recruit = new Character("c" + System.nanoTime(), name)
                .withSkill(com.extremis.core.Skill.COMBAT, 40);
        try {
            consul.recruit(recruit, Instant.now());
            eventLog.add("Recrutement de " + name);
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
        Mission mission = consul.missionById(missionId).orElse(null);
        if (mission == null) {
            return "redirect:/";
        }
        List<Character> team = new ArrayList<>();
        if (teamIds != null) {
            for (String id : teamIds) {
                consul.characterById(id).ifPresent(team::add);
            }
        }
        if (team.isEmpty()) {
            eventLog.add("Aucun arrangeur selectionne pour " + mission.title());
            return "redirect:/";
        }
        activeExecution = MissionExecution.start(mission, team, new SeededRandom(42L), Instant.now());
        eventLog.add("=== " + mission.title() + " ===");
        eventLog.add("Mission lancee. Les evenements se derouleront selon leur delai prevu.");
        tick();
        return "redirect:/";
    }

    @PostMapping("/mission/advance")
    public String advanceMission() {
        if (activeExecution == null) {
            return "redirect:/";
        }
        tick();
        return "redirect:/";
    }

    @Scheduled(fixedRate = 60000)
    public void tick() {
        if (activeExecution == null || activeExecution.isFinished()) {
            return;
        }
        int before = activeExecution.eventsResolved();
        boolean finished = activeExecution.advance(Instant.now());
        if (activeExecution.eventsResolved() > before) {
            eventLog.addAll(activeExecution.logLines().subList(before, activeExecution.eventsResolved()));
        }
        if (finished) {
            MissionReport report = activeExecution.report();
            eventLog.add(report.teamWiped()
                    ? "Equipe eliminee. Mission echouee."
                    : "Mission terminee. Survivants : " + report.survivors());
        }
    }
}
