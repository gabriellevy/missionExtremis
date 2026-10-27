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
        boolean[] ajoute = {false};
        repository.findById("seed-02").filter(c -> c.getName().equals("Jean-Paul Marat")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-02 (Jean-Paul Marat) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-02")) {
            genereMiladyDeLise();
            ajoute[0] = true;
        }
        if (!repository.existsById("seed-03") || repository.findById("seed-03").stream()
                .anyMatch(c -> c.getName().equals("Voltaire"))) {
            repository.findById("seed-03").ifPresent(c -> {
                repository.delete(c);
                log.info("Personnage seed-03 (Voltaire) retire du catalogue");
            });
        }
        if (!repository.existsById("seed-03")) {
            genereLacenaire();
            ajoute[0] = true;
        }
        if (!repository.existsById("seed-11")) {
            generePereDamianKarras();
            ajoute[0] = true;
        }
        if (!repository.existsById("seed-12")) {
            genereJeanTrenton();
            ajoute[0] = true;
        }
        repository.findById("seed-11").ifPresent(c -> {
            if (c.getMaitrises().isEmpty()) {
                c.addMaitrise("Exorcisme");
                c.addMaitrise("Th\u00e9ologie");
                repository.save(c);
                log.info("Ma\u00eetrises Exorcisme et Th\u00e9ologie ajoutees au P\u00e8re Damian Karras");
            }
        });
        if (ajoute[0]) {
            log.info("Personnages manquants ajoutes au catalogue");
        }
        repository.findById("seed-01").ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-01 (Alphonse Hercule de Gascoigne) retire du catalogue");
        });
    }

    private void genereLacenaire() {
        seed("seed-03", "Lacenaire", "aucune", "chef de bande",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.COMMANDEMENT, Competence.INTIMIDATION,
                        Competence.INTUITION, Competence.PERCEPTION, Competence.RAGOT, Competence.TIR},
                new int[]{10, 15, 20, 5, 10, 20, 10},
                new String[]{ViceEtVertu.HUMBLE.vice(), ViceEtVertu.EMPATHIQUE.vice()}, 30);
        repository.findById("seed-03").ifPresent(c -> {
            c.setVitaliteDepart(15);
            c.setVitalite(15);
            c.setSangFroidDepart(15);
            c.setSangFroid(15);
            c.setDescription("Sociopathe aigri et menaçant, Lacenaire combine sa malveillance avec une culture, "
                    + "une élégance et une intelligence peu communes. Par mépris pour l'humanité il refuse le système "
                    + "des coteries et vit en marge de la société par choix. Il dirige une bande de truands "
                    + "qui lui sont tout dévoués.");
            repository.save(c);
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
            c.setVitaliteDepart(13);
            c.setVitalite(13);
            c.setSangFroidDepart(13);
            c.setSangFroid(13);
            c.setDescription("Jean Trenton est un reflet, c'est à dire un personnage créé par un génie du divertissement "
                    + "au point d'être incarné par la volonté de ses innombrables fans.");
            repository.save(c);
        });
    }

    private void genereMiladyDeLise() {
        seed("seed-02", "Milady de Lise", "Saabi", "espionne",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.DISCRETION, Competence.ENDURANCE,
                        Competence.HABILETE, Competence.MARCHANDAGE, Competence.PERCEPTION,
                        Competence.PROTECTION_DU_CONSUL, Competence.RICHESSE},
                new int[]{10, 20, -20, 10, 20, 10, 10, -10},
                new String[]{ViceEtVertu.HUMBLE.vice(), ViceEtVertu.LOYAL.vice(2)}, 36);
        repository.findById("seed-02").ifPresent(c -> {
            c.addSpecialite("Jouer la com\u00e9die + 10");
            c.addSpecialite("D\u00e9guisement + 10");
            c.setVitaliteDepart(13);
            c.setVitalite(13);
            c.setSangFroidDepart(17);
            c.setSangFroid(17);
            c.setDescription("Espionne talentueuse et mythomane, elle a tout l'art d'une bonne agente, "
                    + "de la discrétion au marchandage. Elle est possédée par un esprit demoniaque et très belle, "
                    + "la peau bronzee dorée et les cheveux bruns. Elle est complètement devouée a sa coterie "
                    + "les Saabi, les seuls pour lesquels sa loyauté d'ordinaire douteuse devient sans faille.");
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
            if (c.getMaitrises().isEmpty()) {
                c.addMaitrise("Exorcisme");
                c.addMaitrise("Th\u00e9ologie");
            }
            c.setVitaliteDepart(19);
            c.setVitalite(19);
            c.setSangFroidDepart(18);
            c.setSangFroid(18);
            repository.save(c);
        });
    }

    private void genereBaptisteLeMime() {
        seed("seed-04", "Baptiste le mime", "esthete", "mime",
                new Competence[]{Competence.BAGARRE, Competence.CHARME, Competence.DISCRETION,
                        Competence.EVALUATION, Competence.HABILETE, Competence.INTUITION,
                        Competence.MOUVEMENT, Competence.PERCEPTION, Competence.REFLEXES},
                new int[]{-5, 5, 5, 5, 10, 5, 10, 10, -10},
                new String[]{ViceEtVertu.CARTESIEN.vice(), ViceEtVertu.ESTHETE.vertu()}, 30);
        repository.findById("seed-04").ifPresent(c -> {
            c.setVitaliteDepart(15);
            c.setVitalite(15);
            c.setSangFroidDepart(14);
            c.setSangFroid(14);
            c.setDescription("Acteur mime de genie, doux et reveur, dans la lignée de Baptiste du film Les Enfants du paradis.");
            repository.save(c);
        });
    }

    @Transactional
    public void semerCatalogue() {
        genereLacenaire();
        genereBaptisteLeMime();
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
            c.setVitaliteDepart(15);
            c.setVitalite(15);
            c.setSangFroidDepart(15);
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
