package com.extremis.db;

import com.extremis.core.Competence;
import com.extremis.core.ViceEtVertu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insere un catalogue temporaire de personnages disponibles au recrutement,
 * inspire des sous-pages "Persos - catalogue" du wiki. Idempotent.
 */
@Component
@Order(10)
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
            semerNouveauxPersonnages();
            return;
        }
        semerCatalogue();
    }

    /** Ajoute au catalogue les personnages apparus apres la premiere version, sur une base existante. */
    @Transactional
    public void semerNouveauxPersonnages() {
        boolean ajoute = false;
        if (!repository.existsById("seed-11")) {
            generePereDamianKarras();
            ajoute = true;
        }
        if (!repository.existsById("seed-12")) {
            genereJeanTrenton();
            ajoute = true;
        }
        if (ajoute) {
            log.info("Personnages manquants ajoutes au catalogue");
        }
        repository.findById("seed-01").ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-01 (Alphonse Hercule de Gascoigne) retire du catalogue");
        });
    }

    private void genereJeanTrenton() {
        seed("seed-12", "Jean Trenton", "jacobin", "enquêteur",
                new Competence[]{Competence.ANIMAUX, Competence.COMMANDEMENT, Competence.ELOQUENCE, Competence.INTIMIDATION,
                        Competence.MAGIE, Competence.PERCEPTION, Competence.PROTECTION_DU_CONSUL, Competence.RAGOT,
                        Competence.VIGILANCE},
                new int[]{-2, 2, 14, 12, -4, 15, 34, 15, 10},
                new String[]{ViceEtVertu.CARTESIEN.vertu(), ViceEtVertu.NAIF.vice()}, 24);
        repository.findById("seed-12").ifPresent(c -> {
            c.setVitalite(13);
            c.setSangFroid(13);
            c.setDescription("Jean Trenton est un reflet, c'est à dire un personnage créé par un génie du divertissement "
                    + "au point d'être incarné par la volonté de ses innombrables fans.");
            repository.save(c);
        });
    }

    private void generePereDamianKarras() {
        seed("seed-11", "Père Damian Karras", "Cathare", "Curé",
                new Competence[]{Competence.ANIMAUX, Competence.ARMES_CORPS_A_CORPS, Competence.COMMANDEMENT, Competence.ELOQUENCE,
                        Competence.ENDURANCE, Competence.FORCE_MENTALE, Competence.INTUITION, Competence.MARCHANDAGE,
                        Competence.PERIPLE, Competence.PROTECTION_DU_CONSUL, Competence.RAGOT, Competence.RICHESSE,
                        Competence.TIR},
                new int[]{9, 5, 5, 15, 9, 15, 15, -9, 15, -10, 10, -10, -15},
                new String[]{ViceEtVertu.DESINTERESSE.vertu(), ViceEtVertu.CHASTE.vertu()}, 36);
        repository.findById("seed-11").ifPresent(c -> {
            c.setVitalite(19);
            c.setSangFroid(18);
            repository.save(c);
        });
    }

    @Transactional
    public void semerCatalogue() {
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
        seed("seed-07", "Casanova", "Culte du plaisir", "charlatan",
                new Competence[]{Competence.CHARME, Competence.ELOQUENCE, Competence.EVALUATION, Competence.INTIMIDATION,
                        Competence.INTUITION, Competence.MARCHANDAGE, Competence.PERCEPTION, Competence.PERIPLE,
                        Competence.PROTECTION_DU_CONSUL, Competence.RAGOT, Competence.RICHESSE, Competence.VIGILANCE},
                new int[]{25, 15, 10, -20, 10, 15, 20, 15, 5, 15, -20, -10},
                new String[]{ViceEtVertu.CHASTE.vice(2), ViceEtVertu.ESTHETE.vertu(), ViceEtVertu.SOCIABLE.vertu()}, 33);
        repository.findById("seed-07").ifPresent(c -> {
            c.setVitalite(15);
            c.setSangFroid(15);
            repository.save(c);
        });
        seed("seed-08", "Odysseus", "demokratos", "voyageur",
                new Competence[]{Competence.COMMANDEMENT, Competence.ARMES_CORPS_A_CORPS, Competence.INTELLIGENCE}, new int[]{7, 6, 9},
                new String[]{"Stratege"});
        seed("seed-09", "Vidocq", "citadin", "enqueteur",
                new Competence[]{Competence.PERCEPTION, Competence.DISCRETION, Competence.ARMES_CORPS_A_CORPS}, new int[]{8, 8, 5},
                new String[]{"Inspecteur"});
        seed("seed-10", "Welf Schwarzschutze", "elfe", "voyageur",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.PERCEPTION, Competence.MOUVEMENT}, new int[]{8, 7, 6},
                new String[]{"Trackeur"});
        generePereDamianKarras();
        genereJeanTrenton();
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
