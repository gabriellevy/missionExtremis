package com.extremis.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.extremis.core.MissionEvent.EffetEchec;
import com.extremis.core.MissionEvent.EffetReussite;

/**
 * Moteur de resolution des evenements de mission (wiki, page
 * "Missions") : le PJ avec la plus haute valeur de la competence
 * concernee tente le test D100, et la reussite ou l'echec (critique
 * ou non) declenche l'effet correspondant et designe l'evenement
 * suivant.
 */
public final class ResolveurDEvenement {
    private final RandomSource random;

    public ResolveurDEvenement(RandomSource random) {
        this.random = random;
    }

    public RandomSource sourceAleatoire() {
        return random;
    }

    /**
     * Etat mutable porte par la resolution : exclusions,
     * ajustements de difficulte par evenement, essais de boucles.
     */
    public static final class EtatMission {
        private final Map<String, Integer> ajustements = new LinkedHashMap<>();

        public int ajustement(String evenementId) {
            return ajustements.getOrDefault(evenementId, 0);
        }

        public void appliquerAjustement(String evenementId, int valeur) {
            ajustements.merge(evenementId, valeur, Integer::sum);
        }

        public void restaurerAjustements(Map<String, Integer> valeurs) {
            ajustements.clear();
            if (valeurs != null) {
                ajustements.putAll(valeurs);
            }
        }

        public Map<String, Integer> snapshotAjustements() {
            return new LinkedHashMap<>(ajustements);
        }
    }

    /**
     * Resultat de la resolution d'un evenement.
     *
     * @param evenementSuivant id de l'evenement suivant, null si la mission est terminee
     * @param victoire         true si l'evenement declare la victoire de la mission
     */
    public record ResultatEvenement(String evenementSuivant, boolean victoire) {}

    public ResultatEvenement resoudre(MissionEvent evenement, List<Character> equipe,
                                      EtatMission etat, Consumer<LigneJournal> journal) {
        Optional<SkillTest> testOpt = evenement.test();
        if (testOpt.isEmpty()) {
            if (evenement.effet() != null) {
                journal.accept(new LigneJournal(evenement.effet()));
            }
            appliquerEffetsEquipe(evenement, equipe, journal);
            Optional<String> branche = premiereBrancheRemplie(evenement, equipe, journal);
            if (branche.isPresent()) {
                return new ResultatEvenement(branche.get(), false);
            }
            return new ResultatEvenement(evenement.succes().map(EffetReussite::evenementSuivant).orElse(null),
                    evenement.succes().isPresent() && evenement.succes().get().evenementSuivant() == null
                            && evenement.effet() != null);
        }
        SkillTest test = testOpt.get();
        int ajustement = etat.ajustement(evenement.id());
        SkillTest testAjuste = ajustement == 0 ? test
                : new TestAjuste(test, ajustement);
        if (evenement.testPourTous()) {
            return resoudreTestPourTous(evenement, testAjuste, equipe, etat, journal);
        }
        Predicate<Character> filtre = evenement.testeurFiltre().orElse(c -> true);
        Optional<Character> candidat = testAjuste.bestCandidate(equipe.stream()
                .filter(filtre).toList());
        if (candidat.isEmpty()) {
            journal.accept(new LigneJournal("Aucun membre de l'equipe n'est en état de tenter le test de "
                    + test.skill().label() + "."));
            return new ResultatEvenement(evenement.echec().map(EffetEchec::evenementSuivant).orElse(null), false);
        }
        Character joueur = candidat.get();
        JetDes jet = random.jetD100();
        int de = jet.valeur();
        ResultatTest resultat = ResultatTest.duJet(testAjuste.target(joueur), de);
        evenement.coutSangFroidTesteur().ifPresent(cout -> {
            joueur.perteSangFroid(cout);
            journal.accept(new LigneJournal(joueur.name() + " perd " + cout
                    + " de sang-froid avant le test (restant : " + joueur.sangFroid() + ")."));
        });
        journal.accept(new LigneJournal(joueur.name() + " tente le test de " + test.skill().label()
                + " (cible " + testAjuste.target(joueur) + " sur D100) et obtient un " + de
                + " : " + libelle(resultat) + ".", jet));
        if (resultat.estReussite()) {
            return appliquerReussite(evenement, resultat, joueur, journal);
        }
        return appliquerEchec(evenement, resultat, joueur, equipe, etat, journal);
    }

    private static String libelle(ResultatTest resultat) {
        return switch (resultat) {
            case REUSSITE_CRITIQUE -> "une réussite critique";
            case REUSSITE -> "une réussite";
            case ECHEC -> "un échec";
            case ECHEC_CRITIQUE -> "un échec critique";
        };
    }

    /**
     * Applique les effets portant sur toute l'equipe (wiki, page
     * "Missions") : pertes de sang-froid ou degats collectifs.
     */
    private void appliquerEffetsEquipe(MissionEvent evenement, List<Character> equipe,
                                           Consumer<LigneJournal> journal) {
        evenement.perteSangFroidEquipe().ifPresent(perte -> {
            for (Character membre : equipe) {
                if (membre.isActif()) {
                    membre.perteSangFroid(perte);
                    journal.accept(new LigneJournal(membre.name() + " perd " + perte
                            + " de sang-froid (restant : " + membre.sangFroid() + ")."));
                }
            }
        });
        evenement.degatsEquipe().ifPresent(degats -> {
            for (Character membre : equipe) {
                if (membre.isActif()) {
                    membre.applyDamage(degats);
                    journal.accept(new LigneJournal(membre.name() + " subit " + degats
                            + " degats (vitalite restante : " + membre.vitalite() + ")."));
                    if (!membre.isAlive()) {
                        journal.accept(new LigneJournal(membre.name() + " succombe."));
                    }
                }
            }
        });
    }

    private Optional<String> premiereBrancheRemplie(MissionEvent evenement, List<Character> equipe,
                                                     Consumer<LigneJournal> journal) {
        for (MissionEvent.Branche branche : evenement.branches()) {
            List<Character> actifs = equipe.stream().filter(Character::isActif).toList();
            if (branche.condition().test(actifs)) {
                if (branche.texte() != null) {
                    journal.accept(new LigneJournal(branche.texte()));
                }
                return Optional.of(branche.evenementSuivant());
            }
        }
        return Optional.empty();
    }

    private ResultatEvenement appliquerReussite(MissionEvent evenement, ResultatTest resultat,
                                                Character joueur, Consumer<LigneJournal> journal) {
        EffetReussite succes = evenement.succes().orElse(null);
        String suivant = null;
        if (succes != null) {
            String texte = resultat == ResultatTest.REUSSITE_CRITIQUE && succes.texteCritique() != null
                    ? succes.texteCritique() : succes.texteSimple();
            if (texte != null) {
                journal.accept(new LigneJournal(texte.replace("\\[nom du perso\\]", joueur.name())));
            }
            if (succes.recompense() != null) {
                joueur.addItem(succes.recompense());
                journal.accept(new LigneJournal(joueur.name() + " obtient : " + succes.recompense()));
            }
            suivant = succes.evenementSuivant();
        } else if (evenement.effet() != null) {
            journal.accept(new LigneJournal(evenement.effet()));
        }
        return new ResultatEvenement(suivant, suivant == null && succes != null);
    }

    /**
     * Test "pour tous" (wiki, boites generiques) : chaque personnage
     * actif tente le test individuellement, avec ses propres effets de
     * reussite ou d'echec. L'evenement suivant est celui de l'echec si
     * au moins un personnage a echoue, celui de la reussite sinon.
     */
    private ResultatEvenement resoudreTestPourTous(MissionEvent evenement, SkillTest test,
                                                   List<Character> equipe, EtatMission etat,
                                                   Consumer<LigneJournal> journal) {
        Predicate<Character> filtre = evenement.testeurFiltre().orElse(c -> true);
        List<Character> testeurs = equipe.stream().filter(Character::isActif).filter(filtre).toList();
        if (testeurs.isEmpty()) {
            journal.accept(new LigneJournal("Aucun membre de l'equipe n'est en état de tenter le test de "
                    + test.skill().label() + "."));
            return new ResultatEvenement(evenement.echec().map(EffetEchec::evenementSuivant).orElse(null), false);
        }
        boolean auMoinsUnEchec = false;
        for (Character joueur : testeurs) {
            JetDes jet = random.jetD100();
            int de = jet.valeur();
            ResultatTest resultat = ResultatTest.duJet(test.target(joueur), de);
            evenement.coutSangFroidTesteur().ifPresent(cout -> {
                joueur.perteSangFroid(cout);
                journal.accept(new LigneJournal(joueur.name() + " perd " + cout
                        + " de sang-froid avant le test (restant : " + joueur.sangFroid() + ")."));
            });
            journal.accept(new LigneJournal(joueur.name() + " tente le test de " + test.skill().label()
                    + " (cible " + test.target(joueur) + " sur D100) et obtient un " + de
                    + " : " + libelle(resultat) + ".", jet));
            if (resultat.estReussite()) {
                appliquerReussite(evenement, resultat, joueur, journal);
            } else {
                auMoinsUnEchec = true;
                appliquerEchecIndividuel(evenement, resultat, joueur, equipe, journal);
            }
        }
        EffetEchec echec = evenement.echec().orElse(null);
        EffetReussite succes = evenement.succes().orElse(null);
        if (auMoinsUnEchec && echec != null && echec.ajustementDifficulte() != null) {
            String cible = echec.evenementAjuste() != null ? echec.evenementAjuste() : evenement.id();
            etat.appliquerAjustement(cible, echec.ajustementDifficulte());
            journal.accept(new LigneJournal("La difficulté du test de l'événement " + cible
                    + " sera baissée de " + Math.abs(echec.ajustementDifficulte()) + "."));
        }
        String suivant;
        if (auMoinsUnEchec && echec != null && echec.evenementSuivant() != null) {
            suivant = echec.evenementSuivant();
        } else if (succes != null) {
            suivant = succes.evenementSuivant();
        } else {
            suivant = null;
        }
        return new ResultatEvenement(suivant, false);
    }

    private ResultatEvenement appliquerEchec(MissionEvent evenement, ResultatTest resultat,
                                             Character joueur, List<Character> equipe, EtatMission etat,
                                             Consumer<LigneJournal> journal) {
        EffetEchec echec = evenement.echec().orElse(null);
        String suivant = null;
        if (echec != null) {
            appliquerEchecIndividuel(evenement, resultat, joueur, equipe, journal);
            if (echec.ajustementDifficulte() != null) {
                String cible = echec.evenementAjuste() != null ? echec.evenementAjuste() : evenement.id();
                etat.appliquerAjustement(cible, echec.ajustementDifficulte());
                journal.accept(new LigneJournal("La difficulté du test de l'événement " + cible
                        + " sera baissée de " + Math.abs(echec.ajustementDifficulte()) + "."));
            }
            suivant = echec.evenementSuivant();
        }
        return new ResultatEvenement(suivant, false);
    }

    private void appliquerEchecIndividuel(MissionEvent evenement, ResultatTest resultat,
                                          Character joueur, List<Character> equipe,
                                          Consumer<LigneJournal> journal) {
        EffetEchec echec = evenement.echec().orElse(null);
        if (echec == null) {
            return;
        }
            String texte = resultat == ResultatTest.ECHEC_CRITIQUE && echec.texteCritique() != null
                    ? echec.texteCritique() : echec.texteSimple();
            if (texte == null && evenement.effet() != null) {
                texte = evenement.effet();
            }
            if (texte != null) {
                journal.accept(new LigneJournal(texte.replace("\\[nom du perso\\]", joueur.name())));
            }
            if (resultat == ResultatTest.ECHEC_CRITIQUE && echec.exclureJoueur()) {
                joueur.exclure();
                journal.accept(new LigneJournal(joueur.name() + " est exclu de l'aventure."));
            }
            int degatsSubis = resultat == ResultatTest.ECHEC_CRITIQUE && echec.degatsCritique() != null
                    ? echec.degatsCritique() : (echec.degats() != null ? echec.degats() : 0);
            if (degatsSubis > 0) {
                joueur.applyDamage(degatsSubis);
                journal.accept(new LigneJournal(joueur.name() + " subit " + degatsSubis
                        + " degats (vitalite restante : " + joueur.vitalite() + ")."));
                if (!joueur.isAlive()) {
                    journal.accept(new LigneJournal(joueur.name() + " succombe."));
                }
            }
            if (echec.degatsAleatoires() != null) {
                int total = lancerDes(echec.degatsAleatoires());
                joueur.applyDamage(total);
                journal.accept(new LigneJournal(joueur.name() + " subit " + echec.degatsAleatoires().nombreDeDes()
                        + "D" + echec.degatsAleatoires().faces()
                        + " degats (" + total + ", vitalite restante : " + joueur.vitalite() + ")."));
                if (!joueur.isAlive()) {
                    journal.accept(new LigneJournal(joueur.name() + " succombe."));
                }
            }
            if (echec.degatsAleatoiresMembreAuHasard() != null) {
                List<Character> actifs = equipe.stream().filter(Character::isActif).toList();
                if (!actifs.isEmpty()) {
                    Character victime = actifs.get(Math.floorMod(random.roll(), actifs.size()));
                    int total = lancerDes(echec.degatsAleatoiresMembreAuHasard());
                    victime.applyDamage(total);
                    journal.accept(new LigneJournal(victime.name() + " subit "
                            + echec.degatsAleatoiresMembreAuHasard().nombreDeDes() + "D"
                            + echec.degatsAleatoiresMembreAuHasard().faces()
                            + " degats (" + total + ", vitalite restante : " + victime.vitalite() + ")."));
                    if (!victime.isAlive()) {
                        journal.accept(new LigneJournal(victime.name() + " succombe."));
                    }
                }
            }
            if (echec.perteSangFroid() != null && echec.perteSangFroid() > 0) {
                joueur.perteSangFroid(echec.perteSangFroid());
                journal.accept(new LigneJournal(joueur.name() + " perd " + echec.perteSangFroid()
                        + " de sang-froid (restant : " + joueur.sangFroid() + ")."));
            }
    }

    private int lancerDes(MissionEvent.DesDegats des) {
        int total = 0;
        for (int i = 0; i < des.nombreDeDes(); i++) {
            total += random.roll() % des.faces() + 1;
        }
        return total;
    }

    /**
     * Test D100 avec un ajustement de difficulte supplementaire
     * (par exemple la difficulté baissée de 10 apres un échec).
     */
    private record TestAjuste(SkillTest test, int ajustement) implements SkillTest {
        @Override
        public Competence skill() {
            return test.skill();
        }

        @Override
        public int difficulty() {
            return test.difficulty() + ajustement;
        }
    }
}
