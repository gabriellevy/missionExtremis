package com.extremis.web;

import com.extremis.core.Character;
import com.extremis.core.LigneJournal;
import com.extremis.core.Mission;
import com.extremis.core.MissionExecution;
import com.extremis.core.MissionReport;
import com.extremis.core.SeededRandom;
import com.extremis.db.CharacterEntity;
import com.extremis.db.CharacterRepository;
import com.extremis.db.ConsulStateEntity;
import com.extremis.db.ConsulStateRepository;
import com.extremis.db.CharacterSeeder;
import com.extremis.db.EntityMapper;
import com.extremis.db.ExecutionTeamMemberEntity;
import com.extremis.db.MissionExecutionEntity;
import com.extremis.db.MissionExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
    private final CharacterSeeder seeder;

    public GameService(CharacterRepository characters,
                       ConsulStateRepository consulState,
                       MissionExecutionRepository executions,
                       List<Mission> missionDefinitions,
                       CharacterSeeder seeder) {
        this.characters = characters;
        this.consulState = consulState;
        this.executions = executions;
        this.missionDefinitions = missionDefinitions;
        this.seeder = seeder;
    }

    @Transactional(readOnly = true)
    public List<Character> roster() {
        return EntityMapper.toCoreList(characters.findByRecruteTrue());
    }

    @Transactional(readOnly = true)
    public List<CharacterEntity> availableCharacters() {
        return characters.findByRecruteFalse();
    }

    @Transactional(readOnly = true)
    public List<CharacterEntity> tousLesPersonnages() {
        return characters.findAll();
    }

    @Transactional(readOnly = true)
    public boolean peutRecruter(Instant now) {
        Instant dernier = consul().getLastRecruitment();
        return dernier == null || !now.isBefore(dernier.plusSeconds(24 * 3600));
    }

    @Transactional
    public void recruit(String characterId, Instant now) {
        ConsulStateEntity state = consul();
        if (state.getLastRecruitment() != null
                && now.isBefore(state.getLastRecruitment().plusSeconds(24 * 3600))) {
            throw new IllegalStateException("Un seul recrutement par 24h.");
        }
        CharacterEntity entity = characters.findById(characterId).orElseThrow();
        if (entity.isRecrute()) {
            throw new IllegalStateException("Personnage deja recrute.");
        }
        entity.setRecrute(true);
        characters.save(entity);
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
        MissionExecutionEntity entity = new MissionExecutionEntity(mission.id(), mission.titre(), now);
        for (Character c : team) {
            entity.addTeamMember(new ExecutionTeamMemberEntity(c.id(), c.name(),
                    c.vitalite(), c.sangFroid(), c.isAlive()));
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

    @Transactional
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
            Character c = characters.findById(m.getCharacterId())
                    .map(EntityMapper::toCore)
                    .orElseGet(() -> new Character(m.getCharacterId(), m.getName()));
            c.setVitalite(m.getVitalite());
            c.setSangFroid(m.getSangFroid());
            if (!m.isAlive()) {
                c.kill();
            }
            if (m.isExclu()) {
                c.exclure();
            }
            team.add(c);
        }
        MissionExecution execution = MissionExecution.start(def.get(), team,
                new SeededRandom(42L, entity.getJetsEffectues()), entity.getStartedAt());
        if (entity.getEvenementCourant() != null) {
            java.util.Map<String, Integer> ajustements = new java.util.LinkedHashMap<>();
            for (com.extremis.db.ExecutionAjustementEntity a : entity.getAjustements()) {
                ajustements.put(a.getEvenementId(), a.getAjustement());
            }
            List<LigneJournal> historique = entity.getLogLines().stream()
                    .map(l -> {
                        com.extremis.core.TypeLigne type = "titre".equals(l.getType())
                                ? com.extremis.core.TypeLigne.TITRE
                                : "ambiance".equals(l.getType())
                                        ? com.extremis.core.TypeLigne.AMBIANCE
                                        : com.extremis.core.TypeLigne.TEXTE;
                        if ("jet".equals(l.getType()) && l.getDeDizaines() != null && l.getDeUnites() != null) {
                            return new LigneJournal(l.getLine(),
                                    new com.extremis.core.JetDes(l.getDeDizaines(), l.getDeUnites()), type);
                        }
                        return new LigneJournal(l.getLine(), null, type);
                    })
                    .toList();
            execution.reprendre(entity.getEvenementCourant(), entity.getHeureEffetCourant(),
                    ajustements, historique);
        }
        return Optional.of(execution);
    }

    @Transactional
    public void abandon(MissionExecutionEntity entity) {
        entity.setStatus(MissionExecutionEntity.Status.ABANDONED);
        executions.save(entity);
    }

    @Transactional(readOnly = true)
    public List<MissionExecutionEntity> activeExecutionEntities() {
        return executions.findAllByStatusOrderByIdAsc(MissionExecutionEntity.Status.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public Optional<MissionExecutionEntity> activeExecutionEntity(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        MissionExecutionEntity entity = executions.findById(id).orElse(null);
        if (entity == null || entity.getStatus() != MissionExecutionEntity.Status.IN_PROGRESS) {
            return Optional.empty();
        }
        return Optional.of(entity);
    }

    @Transactional
    public void finish(MissionExecutionEntity entity, MissionReport report, Instant now) {
        entity.setStatus(MissionExecutionEntity.Status.FINISHED);
        entity.setFinishedAt(now);
        entity.setTeamWiped(report.teamWiped());
        entity.setEvenementCourant(null);
        for (ExecutionTeamMemberEntity m : entity.getTeam()) {
            report.casualties().stream()
                    .filter(name -> name.equals(m.getName()))
                    .findFirst()
                    .ifPresent(name -> {
                        m.setAlive(false);
                        m.setVitalite(0);
                    });
        }
        syncRosterAfterMission(report);
        syncEquipeRoster(entity);
        executions.save(entity);
    }

    /**
     * Repercute la vitalite et le sang-froid finaux de l'execution sur le
     * roster : les pertes subies en mission persistent (wiki, page
     * "Personnage jdr").
     */
    private void syncEquipeRoster(MissionExecutionEntity entity) {
        for (ExecutionTeamMemberEntity m : entity.getTeam()) {
            characters.findById(m.getCharacterId()).ifPresent(c -> {
                c.setVitalite(m.getVitalite());
                c.setSangFroid(m.getSangFroid());
                if (!m.isAlive()) {
                    c.setAlive(false);
                }
                characters.save(c);
            });
        }
    }

    private void syncRosterAfterMission(MissionReport report) {
        for (String casualty : report.casualties()) {
            characters.findAll().stream()
                    .filter(c -> c.getName().equals(casualty))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setAlive(false);
                        c.setVitalite(0);
                        characters.save(c);
                    });
        }
    }

    private void syncProgress(MissionExecutionEntity entity, MissionExecution execution) {
        entity.setEvenementCourant(execution.evenementCourant());
        entity.setHeureEffetCourant(execution.heureEffetCourant());
        entity.setReussie(execution.isReussie());
        if (execution.sourceAleatoire() instanceof SeededRandom seeded) {
            entity.setJetsEffectues(seeded.getJetsEffectues());
        }
        execution.ajustementsDifficulte().forEach(entity::addAjustement);
        List<LigneJournal> lignes = execution.logLines();
        while (entity.getLogLines().size() < lignes.size()) {
            entity.addLogLine(lignes.get(entity.getLogLines().size()));
        }
        for (int i = 0; i < entity.getTeam().size() && i < execution.teamSnapshot().size(); i++) {
            ExecutionTeamMemberEntity m = entity.getTeam().get(i);
            Character c = execution.teamSnapshot().get(i);
            m.setVitalite(c.vitalite());
            m.setSangFroid(c.sangFroid());
            m.setAlive(c.isAlive());
            m.setExclu(c.isExclu());
        }
    }

    @Transactional
    public void viderRoster() {
        characters.findAll().forEach(c -> {
            c.setRecrute(false);
            characters.save(c);
        });
    }

    /**
     * Remise a zero usine : tous les personnages redeviennent recrutables,
     * puis 3 sont tires au hasard pour rejoindre les personnages recrutes.
     */
    @Transactional
    public void reinitialiserUsine() {
        executions.deleteAll();
        characters.deleteAll();
        consulState.deleteAll();
        consulState.save(new ConsulStateEntity());
        seeder.semerCatalogue();
        List<CharacterEntity> catalogue = new ArrayList<>(characters.findByRecruteFalse());
        Collections.shuffle(catalogue, new Random());
        catalogue.stream().limit(3).forEach(c -> {
            c.setRecrute(true);
            characters.save(c);
        });
    }

    @Transactional
    public void saveQuickRecruit(Character recruit) {
        CharacterEntity entity = EntityMapper.toEntity(recruit, "test", "test");
        entity.setRecrute(true);
        characters.save(entity);
    }

    private ConsulStateEntity consul() {
        return consulState.findById("consul").orElseGet(() -> consulState.save(new ConsulStateEntity()));
    }
}
