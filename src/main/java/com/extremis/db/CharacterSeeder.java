package com.extremis.db;

import com.extremis.core.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insere un catalogue temporaire de personnages disponibles au recrutement,
 * inspire des sous-pages "Persos - catalogue" du wiki. Idempotent.
 */
@Component
public class CharacterSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CharacterSeeder.class);

    private final CharacterRepository repository;

    public CharacterSeeder(CharacterRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        semerCatalogue();
    }

    @Transactional
    public void semerCatalogue() {
        seed("seed-01", "Alphonse Hercule de Gascoigne", "performeur", "enqueteur",
                new Skill[]{Skill.INTUITION, Skill.MOUVEMENT, Skill.ARMES_CORPS_A_CORPS}, new int[]{10, 5, 0},
                new String[]{"Esthete", "Orgueilleux", "Chaste"});
        seed("seed-02", "Jean-Paul Marat", "jacobin", "enqueteur",
                new Skill[]{Skill.ELOQUENCE, Skill.INTELLIGENCE}, new int[]{8, 6},
                new String[]{"Radical"});
        seed("seed-03", "Voltaire", "lumieres", "specialiste-volonte",
                new Skill[]{Skill.INTUITION, Skill.ELOQUENCE}, new int[]{9, 7},
                new String[]{"Esprit"});
        seed("seed-04", "Rorschach", "khaos", "enqueteur",
                new Skill[]{Skill.ARMES_CORPS_A_CORPS, Skill.PERCEPTION, Skill.DISCRETION}, new int[]{7, 9, 6},
                new String[]{"Implacable"});
        seed("seed-05", "Arsene Lupin", "esthete", "enqueteur",
                new Skill[]{Skill.DISCRETION, Skill.MOUVEMENT, Skill.REFLEXES}, new int[]{10, 8, 6},
                new String[]{"Cambrioleur"});
        seed("seed-06", "Albios le barde", "celte", "specialiste-volonte",
                new Skill[]{Skill.ELOQUENCE, Skill.INTUITION}, new int[]{8, 5},
                new String[]{"Barde"});
        seed("seed-07", "Mata-Hari", "lotus-blanc", "enqueteur",
                new Skill[]{Skill.DISCRETION, Skill.TROMPERIE}, new int[]{9, 8},
                new String[]{"Espionne"});
        seed("seed-08", "Odysseus", "demokratos", "voyageur",
                new Skill[]{Skill.COMMANDEMENT, Skill.ARMES_CORPS_A_CORPS, Skill.INTELLIGENCE}, new int[]{7, 6, 9},
                new String[]{"Stratege"});
        seed("seed-09", "Vidocq", "citadin", "enqueteur",
                new Skill[]{Skill.PERCEPTION, Skill.DISCRETION, Skill.ARMES_CORPS_A_CORPS}, new int[]{8, 8, 5},
                new String[]{"Inspecteur"});
        seed("seed-10", "Welf Schwarzschutze", "elfe", "voyageur",
                new Skill[]{Skill.ARMES_CORPS_A_CORPS, Skill.PERCEPTION, Skill.MOUVEMENT}, new int[]{8, 7, 6},
                new String[]{"Trackeur"});
        log.info("Catalogue de personnages initialise : {} personnages", repository.count());
    }

    private void seed(String id, String name, String coterie, String role, Skill[] skills, int[] values, String[] traits) {
        CharacterEntity c = new CharacterEntity(id, name);
        c.setCoterie(coterie);
        c.setRole(role);
        for (int i = 0; i < skills.length; i++) {
            c.addSkill(skills[i], values[i]);
        }
        for (String trait : traits) {
            c.addTrait(trait);
        }
        repository.save(c);
    }
}
