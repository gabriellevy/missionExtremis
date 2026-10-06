package com.extremis.db;

import com.extremis.core.Competence;
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

    private static final int VITALITE_PERE_DAMIAN_KARRAS = 9;
    private static final int SANG_FROID_PERE_DAMIAN_KARRAS = 18;

    private final CharacterRepository repository;

    public CharacterSeeder(CharacterRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            semerNouveauxPersonnages();
            return;
        }
        semerCatalogue();
    }

    /** Ajoute au catalogue les personnages apparus apres la premiere version, sur une base existante. */
    @Transactional
    public void semerNouveauxPersonnages() {
        if (repository.existsById("seed-11")) {
            miseAJourPereDamianKarras();
            return;
        }
        seedPereDamianKarras();
        log.info("Personnage seed-11 ajoute au catalogue");
    }

    /** Applique les caracs du wiki (vitalite, sang-froid) au Pere Damian Karras sur une base existante. */
    private void miseAJourPereDamianKarras() {
        repository.findById("seed-11").ifPresent(c -> {
            c.setVitalite(VITALITE_PERE_DAMIAN_KARRAS);
            c.setSangFroid(SANG_FROID_PERE_DAMIAN_KARRAS);
            repository.save(c);
        });
    }

    private void seedPereDamianKarras() {
        seed("seed-11", "Père Damian Karras", "cathare", "cure",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.CHANCE, Competence.COMMANDEMENT, Competence.ELOQUENCE,
                        Competence.ENDURANCE, Competence.FORCE_MENTALE, Competence.INTUITION, Competence.MARCHANDAGE,
                        Competence.PERIPLE, Competence.RAGOT, Competence.RICHESSE, Competence.SURVIE_EXTERIEUR, Competence.TIR},
                new int[]{5, -10, 5, 15, 9, 15, 15, -9, 9, 10, -10, 15, -15},
                new String[]{"Désintéressé", "Chaste"}, 36);
        repository.findById("seed-11").ifPresent(c -> {
            c.setVitalite(VITALITE_PERE_DAMIAN_KARRAS);
            c.setSangFroid(SANG_FROID_PERE_DAMIAN_KARRAS);
            repository.save(c);
        });
    }

    @Transactional
    public void semerCatalogue() {
        seed("seed-01", "Alphonse Hercule de Gascoigne", "performeur", "enqueteur",
                new Competence[]{Competence.INTUITION, Competence.MOUVEMENT, Competence.ARMES_CORPS_A_CORPS}, new int[]{10, 5, 0},
                new String[]{"Esthete", "Orgueilleux", "Chaste"});
        seed("seed-02", "Jean-Paul Marat", "jacobin", "enqueteur",
                new Competence[]{Competence.ELOQUENCE, Competence.INTELLIGENCE}, new int[]{8, 6},
                new String[]{"Radical"});
        seed("seed-03", "Voltaire", "lumieres", "specialiste-volonte",
                new Competence[]{Competence.INTUITION, Competence.ELOQUENCE}, new int[]{9, 7},
                new String[]{"Esprit"});
        seed("seed-04", "Rorschach", "khaos", "enqueteur",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.PERCEPTION, Competence.DISCRETION}, new int[]{7, 9, 6},
                new String[]{"Implacable"});
        seed("seed-05", "Arsene Lupin", "esthete", "enqueteur",
                new Competence[]{Competence.DISCRETION, Competence.MOUVEMENT, Competence.REFLEXES}, new int[]{10, 8, 6},
                new String[]{"Cambrioleur"});
        seed("seed-06", "Albios le barde", "celte", "specialiste-volonte",
                new Competence[]{Competence.ELOQUENCE, Competence.INTUITION}, new int[]{8, 5},
                new String[]{"Barde"});
        seed("seed-07", "Mata-Hari", "lotus-blanc", "enqueteur",
                new Competence[]{Competence.DISCRETION, Competence.TROMPERIE}, new int[]{9, 8},
                new String[]{"Espionne"});
        seed("seed-08", "Odysseus", "demokratos", "voyageur",
                new Competence[]{Competence.COMMANDEMENT, Competence.ARMES_CORPS_A_CORPS, Competence.INTELLIGENCE}, new int[]{7, 6, 9},
                new String[]{"Stratege"});
        seed("seed-09", "Vidocq", "citadin", "enqueteur",
                new Competence[]{Competence.PERCEPTION, Competence.DISCRETION, Competence.ARMES_CORPS_A_CORPS}, new int[]{8, 8, 5},
                new String[]{"Inspecteur"});
        seed("seed-10", "Welf Schwarzschutze", "elfe", "voyageur",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.PERCEPTION, Competence.MOUVEMENT}, new int[]{8, 7, 6},
                new String[]{"Trackeur"});
        seedPereDamianKarras();
        log.info("Catalogue de personnages initialise : {} personnages", repository.count());
    }

    private void seed(String id, String name, String coterie, String role, Competence[] skills, int[] values, String[] traits) {
        seed(id, name, coterie, role, skills, values, traits, com.extremis.core.Character.DEFAULT_SKILL_BASE);
    }

    private void seed(String id, String name, String coterie, String role, Competence[] skills, int[] values, String[] traits,
                      int baseDeCompetences) {
        CharacterEntity c = new CharacterEntity(id, name);
        c.setCoterie(coterie);
        c.setRole(role);
        c.setSkillBase(baseDeCompetences);
        for (int i = 0; i < skills.length; i++) {
            c.addSkill(skills[i], values[i]);
        }
        for (String trait : traits) {
            c.addTrait(trait);
        }
        repository.save(c);
    }
}
