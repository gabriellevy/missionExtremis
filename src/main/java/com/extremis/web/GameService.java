package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
import com.extremis.core.SeededRandom;
import com.extremis.db.CharacterEntity;
import com.extremis.db.CharacterRepository;
import com.extremis.db.ConsulStateEntity;
import com.extremis.db.ConsulStateRepository;
import com.extremis.db.EntityMapper;
import com.extremis.db.ExecutionTeamMemberEntity;
import com.extremis.db.MissionExecutionEntity;
import com.extremis.db.MissionExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Etat du jeu persiste en base : roster, etat du consul et execution
 * de mission en cours. Les definitions de mission restent en code.
 */
@Service
public class GameService {
    private final CharacterRepository characters;
    private final ConsulStateRepository consulState;
    private final MissionExecutionRepository executions;
    private final List<Mission> missionDefinitions;

    public GameService(CharacterRepository characters,
                       ConsulStateRepository consulState,
                       MissionExecutionRepository executions,
                       List<Mission> missionDefinitions) {
        this.characters = characters;
        this.consulState = consulState;
        this.executions = executions;
        this.missionDefinitions = missionDefinitions;
    }

    @Transactional(readOnly = true)
    public List<Character> roster() {
        return EntityMapper.toCoreList(characters.findAll());
    }

    @Transactional(readOnly = true)
    public List<CharacterEntity> availableCharacters() {
        return characters.findAll();
    }

    @Transactional
    public void recruit(Character recruit, Instant now) {
        Character copy = new Character("rec-" + recruit.id() + "-" + System.nanoTime(), recruit.name());
        copy.withSkillBase(recruit.skillBase()); recruit.skillsSnapshot().forEach((skill, value) -> copy.withSkill(skill, value));
        CharacterEntity source = characters.findById(recruit.id()).orElse(null);
        CharacterEntity entity = EntityMapper.toEntity(copy,
                source != null ? source.getCoterie() : null,
                source != null ? source.getRole() : null);
        characters.save(entity);
        ConsulStateEntity state = consul();
        if (state.getLastRecruitment() != null
                && now.isBefore(state.getLastRecruitment().plusSeconds(24 * 3600))) {
            throw new IllegalStateException("Un seul recrutement par 24h.");
        }
        state.setLastRecruitment(now);
        consulState.save(state);
    }

    @Transactional(readOnly = true)
    public Optional<Character> characterById(String id) {
        return characters.findById(id).map(EntityMapper::toCore);
    }

    @Transactional(readOnly = true)
    public int jokers() {
        return consul().getJokers();
    }

    @Transactional(readOnly = true)
    public int simulationsLeft() {
        return consul().getSimulationsLeft();
    }

    @Transactional
    public boolean spendJoker() {
        ConsulStateEntity state = consul();
        if (state.getJokers() <= 0) {
            return false;
        }
        state.setJokers(state.getJokers() - 1);
        consulState.save(state);
        return true;
    }

    @Transactional
    public MissionExecutionEntity startMission(Mission mission, List<Character> team, Instant now) {
        MissionExecutionEntity entity = new MissionExecutionEntity(mission.id(), mission.title(), now);
        for (Character c : team) {
            entity.addTeamMember(new ExecutionTeamMemberEntity(c.id(), c.name(), c.health(), c.isAlive()));
        }
        MissionExecution execution = MissionExecution.start(mission, team, new SeededRandom(42L), now);
        syncProgress(entity, execution);
        return executions.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<MissionExecution> activeExecution() {
        Optional<MissionExecutionEntity> entity = executions.findFirstByStatusOrderByIdDesc(
                MissionExecutionEntity.Status.IN_PROGRESS);
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        return rebuild(entity.get());
    }

    @Transactional
    public void saveProgress(MissionExecutionEntity entity, MissionExecution execution, Instant now) {
        syncProgress(entity, execution);
        executions.save(entity);
    }

    public Optional<MissionExecution> rebuild(MissionExecutionEntity entity) {
        Optional<Mission> def = missionDefinitions.stream()
                .filter(m -> m.id().equals(entity.getMissionId()))
                .findFirst();
        if (def.isEmpty()) {
            abandon(entity);
            return Optional.empty();
        }
        List<Character> team = new ArrayList<>();
        for (ExecutionTeamMemberEntity m : entity.getTeam()) {
            Character c = new Character(m.getCharacterId(), m.getName());
            c.setHealth(m.getHealth());
            if (!m.isAlive()) {
                c.kill();
            }
            team.add(c);
        }
        MissionExecution execution = MissionExecution.start(def.get(), team,
                new SeededRandom(42L), entity.getStartedAt());
        for (int i = 0; i < entity.getNextEventIndex(); i++) {
            execution.skipEvent();
        }
        for (ExecutionTeamMemberEntity m : entity.getTeam()) {
            if (!m.isAlive()) {
                execution.markDead(m.getCharacterId());
            }
        }
        return Optional.of(execution);
    }

    @Transactional
    public void abandon(MissionExecutionEntity entity) {
        entity.setStatus(MissionExecutionEntity.Status.ABANDONED);
        executions.save(entity);
    }

    public MissionExecutionEntity activeExecutionEntity() {
        return executions.findFirstByStatusOrderByIdDesc(MissionExecutionEntity.Status.IN_PROGRESS)
                .orElse(null);
    }

    @Transactional
    public void finish(MissionExecutionEntity entity, MissionReport report, Instant now) {
        entity.setStatus(MissionExecutionEntity.Status.FINISHED);
        entity.setFinishedAt(now);
        entity.setTeamWiped(report.teamWiped());
        for (ExecutionTeamMemberEntity m : entity.getTeam()) {
            report.casualties().stream()
                    .filter(name -> name.equals(m.getName()))
                    .findFirst()
                    .ifPresent(name -> {
                        m.setAlive(false);
                        m.setHealth(0);
                    });
        }
        syncRosterAfterMission(report);
        executions.save(entity);
    }

    private void syncRosterAfterMission(MissionReport report) {
        for (String casualty : report.casualties()) {
            characters.findAll().stream()
                    .filter(c -> c.getName().equals(casualty))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setAlive(false);
                        c.setHealth(0);
                        characters.save(c);
                    });
        }
    }

    private void syncProgress(MissionExecutionEntity entity, MissionExecution execution) {
        entity.setNextEventIndex(execution.eventsResolved());
        entity.setLastEventTime(String.valueOf(execution.nextEventTime()));
        entity.getLogLines().clear();
        for (String line : execution.logLines()) {
            entity.addLogLine(line);
        }
        for (int i = 0; i < entity.getTeam().size() && i < execution.teamSnapshot().size(); i++) {
            ExecutionTeamMemberEntity m = entity.getTeam().get(i);
            Character c = execution.teamSnapshot().get(i);
            m.setHealth(c.health());
            m.setAlive(c.isAlive());
        }
    }

    @Transactional
    public void clearRoster() {
        characters.deleteAll();
    }

    @Transactional
    public void saveQuickRecruit(Character recruit) {
        characters.save(EntityMapper.toEntity(recruit, "test", "test"));
    }

    private ConsulStateEntity consul() {
        return consulState.findById("consul").orElseGet(() -> consulState.save(new ConsulStateEntity()));
    }
}
