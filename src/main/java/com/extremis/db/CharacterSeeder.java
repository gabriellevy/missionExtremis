package com.extremis.db;

import com.extremis.core.Competence;
import com.extremis.core.Lieu;
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
        repository.findById("seed-05").filter(c -> c.getName().equals("Arsene Lupin")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-05 (Arsene Lupin) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-05")) {
            genereDavid();
            ajoute[0] = true;
        }
        repository.findById("seed-06").filter(c -> c.getName().equals("Albios le barde")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-06 (Albios le barde) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-06")) {
            genereLune();
            ajoute[0] = true;
        }
        repository.findById("seed-08").filter(c -> c.getName().equals("Odysseus")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-08 (Odysseus) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-08")) {
            genereMamieMahjong();
            ajoute[0] = true;
        }
        repository.findById("seed-10").filter(c -> c.getName().equals("Welf Schwarzschutze")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-10 (Welf Schwarzschutze) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-10")) {
            generePavelKortchagine();
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
        repository.findById("seed-09").filter(c -> c.getCarriere().equals("enqueteur")).ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-09 (Vidocq, version enqueteur) retire du catalogue");
            ajoute[0] = true;
        });
        if (!repository.existsById("seed-09")) {
            genereVidocq();
            ajoute[0] = true;
        }
        repository.findById("seed-01").ifPresent(c -> {
            repository.delete(c);
            log.info("Personnage seed-01 (Alphonse Hercule de Gascoigne) retire du catalogue");
        });
    }

    private void genereVidocq() {
        seed("seed-09", "Vidocq", "Saabi", "voyou",
                new Competence[]{Competence.ARMES_CORPS_A_CORPS, Competence.BAGARRE, Competence.DISCRETION,
                        Competence.ELOQUENCE, Competence.ENDURANCE, Competence.HABILETE, Competence.INTIMIDATION,
                        Competence.MARCHANDAGE, Competence.RAGOT, Competence.REFLEXES},
                new int[]{10, 5, 10, -5, -10, 5, 5, 10, 10, -6},
                new String[]{ViceEtVertu.LOYAL.vice(), ViceEtVertu.CHASTE.vice()}, 30);
        repository.findById("seed-09").ifPresent(c -> {
            c.setDescription("Fils de bourgeois aux penchants malhonn\u00eates, Vidocq va de vols en bagarre en passant "
                    + "par les tripots et les bordels. Grand et costaud, de compagnie agr\u00e9able bien qu'un peu "
                    + "rustaud. V\u00e9cus : po\u00e8te rat\u00e9, gros joueur malchanceux. Destin : 3. Vices : trompeur, "
                    + "luxurieux. 19 ans.");
            repository.save(c);
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

    private void generePavelKortchagine() {
        seed("seed-10", "Pavel Kortchagine", "jacobin", "Lyc\u00e9en",
                new Competence[]{Competence.ANIMAUX, Competence.CHANCE, Competence.COMMANDEMENT,
                        Competence.ELOQUENCE, Competence.INTELLIGENCE, Competence.INTIMIDATION,
                        Competence.MARCHANDAGE, Competence.VIGILANCE, Competence.VOLONTE},
                new int[]{-5, 30, 10, 10, 1, 5, -5, 5, -10},
                new String[]{ViceEtVertu.LOYAL.vertu(), ViceEtVertu.NAIF.vice()}, 30);
        repository.findById("seed-10").ifPresent(c -> {
            c.addMaitrise("Accord\u00e9on");
            c.addItem("Accord\u00e9on");
            c.addItem("Pistolet");
            c.setVitaliteDepart(18);
            c.setVitalite(18);
            c.setSangFroidDepart(18);
            c.setSangFroid(18);
            c.setDescription("H\u00e9ros du roman \u00ab Et l'acier fut tremp\u00e9 \u00bb de Nicolas Ostrovski : "
                    + "lyc\u00e9en de 16 ans de la coterie des jacobins, volontaire et d\u00e9vou\u00e9 \u00e0 la cause r\u00e9volutionnaire. "
                    + "Loyal mais m\u00e9fiant, il joue de l'accord\u00e9on et porte un pistolet.");
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

    private void genereDavid() {
        seed("seed-05", "David", "Ach\u00e9ron", "Lyc\u00e9en",
                new Competence[]{Competence.CHARME, Competence.FORCE, Competence.FORCE_MENTALE,
                        Competence.INTELLIGENCE, Competence.INTIMIDATION, Competence.PROTECTION_DU_CONSUL,
                        Competence.VOLONTE},
                new int[]{-16, -8, 16, 8, 8, -10, 16},
                new String[]{ViceEtVertu.EMPATHIQUE.vice(), ViceEtVertu.BIENVEILLANT.vice()}, 33);
        repository.findById("seed-05").ifPresent(c -> {
            c.addMaitrise("Mentaliste");
            c.addSort("Lecture de pens\u00e9es");
            c.addSort("Connaissance du temps \u00e9coul\u00e9");
            c.addSort("Influence insidieuse");
            c.addSort("Domination");
            c.setVitaliteDepart(15);
            c.setVitalite(15);
            c.setSangFroidDepart(18);
            c.setSangFroid(18);
            c.setDescription("Enfant sociopathique de 13 ans, \u00e0 la nature inconnue et aux \u00e9tranges pouvoirs mentaux. "
                    + "Il sent qu\u2019il n\u2019est pas de nature humaine, qu\u2019il est n\u00e9 par accident parmi cette rare "
                    + "infanterie et qu\u2019il doit l\u2019\u00e9viter ou la dominer.");
            repository.save(c);
        });
    }

    private void genereLune() {
        seed("seed-06", "Lune", "F\u00e9riques", "mendiante",
                new Competence[]{Competence.ANIMAUX, Competence.CHANCE, Competence.COMMANDEMENT,
                        Competence.DISCRETION, Competence.EVALUATION, Competence.INTELLIGENCE,
                        Competence.INTUITION, Competence.REFLEXES, Competence.RICHESSE,
                        Competence.VIGILANCE, Competence.VOLONTE},
                new int[]{10, 20, -5, 10, -16, 0, 20, 20, -30, -20, 20},
                new String[]{ViceEtVertu.ARTIFICIALISTE.vice(), ViceEtVertu.CASANIER.vertu(),
                        ViceEtVertu.DISCIPLINE.vice()}, 24);
        repository.findById("seed-06").ifPresent(c -> {
            c.addSpecialite("Tromperie + 10");
            c.addSort("Brise l\u00e9g\u00e8re");
            c.addSort("Sons et silence");
            c.addSort("Passe-muraille");
            c.setVitaliteDepart(12);
            c.setVitalite(12);
            c.setSangFroidDepart(14);
            c.setSangFroid(14);
            c.setDescription("Mendiante de 14 ans membre de la coterie des F\u00e9riques, "
                    + "elle ma\u00eetrise les sorts de l'air.");
            repository.save(c);
        });
    }

    private void genereMamieMahjong() {
        seed("seed-08", "Mamie mahjong", "Lotus blanc", "receleuse",
                new Competence[]{Competence.BAGARRE, Competence.CHANCE, Competence.DISCRETION,
                        Competence.HABILETE, Competence.INTIMIDATION, Competence.MARCHANDAGE,
                        Competence.RAGOT, Competence.RICHESSE, Competence.VOLONTE},
                new int[]{10, -5, 20, -3, 3, 20, 20, -3, 5},
                new String[]{ViceEtVertu.SOBRE.vice(), ViceEtVertu.PRODIGUE.vice(), ViceEtVertu.NAIF.vice()}, 30);
        repository.findById("seed-08").ifPresent(c -> {
            c.setVitaliteDepart(14);
            c.setVitalite(14);
            c.setSangFroidDepart(14);
            c.setSangFroid(14);
            c.addItem("Bijoux de grande valeur");
            c.addItem("Perruques de tous types avec \u00e9pingles \u00e0 cheveux de tous types");
            c.setDescription("Chinoise de 67 ans, grosse voire ob\u00e8se, tr\u00e8s maquill\u00e9e avec des ongles "
                    + "tr\u00e8s longs vernis en rouge. Elle est chauve et porte des perruques extravagantes coiff\u00e9es "
                    + "de piques \u00e0 cheveux chinois. Elle porte une profusion de bijoux : colliers de perles, "
                    + "colliers de jade, bagues \u00e0 chaque doigt, bras charg\u00e9s de bracelets d'or. Elle porte une "
                    + "tenue traditionnelle chinoise de couleur jaune. Elle se tient toujours assise, avec une amulette "
                    + "porte chance dans la main et un jeu de cartes sur elle. Handicaps : grosse (44), joueuse "
                    + "compulsive (34), doigts cass\u00e9s par un client m\u00e9content il y a longtemps.");
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
            c.setDescription("Acteur mime de genie, doux et reveur.");
            repository.save(c);
        });
    }

    @Transactional
    public void semerCatalogue() {
        genereLacenaire();
        genereBaptisteLeMime();
        genereDavid();
        genereLune();
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
        genereMamieMahjong();
        genereVidocq();
        generePavelKortchagine();
        generePereDamianKarras();
        genereJeanTrenton();
        log.info("Catalogue de personnages initialise : {} personnages", repository.count());
    }

    private void seed(String id, String name, String coterie, String carriere, Competence[] skills, int[] values, String[] traits) {
        seed(id, name, coterie, carriere, skills, values, traits, com.extremis.core.Character.DEFAULT_SKILL_BASE);
    }

    private void seed(String id, String name, String coterie, String carriere, Competence[] skills, int[] values, String[] traits,
                      int baseDeCompetences) {
        CharacterEntity c = new CharacterEntity(id, name);
        c.setCoterie(coterie);
        c.setLieu(Lieu.auHasard());
        c.setCarriere(carriere);
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
