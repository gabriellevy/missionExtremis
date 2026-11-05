package com.extremis.catalog;

import java.time.Duration;
import java.util.List;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.MissionEvent;
import com.extremis.core.SimpleTest;
import com.extremis.core.MissionEvent.EffetEchec;
import com.extremis.core.MissionEvent.EffetReussite;

/**
 * Boites d'evenements generiques du wiki (page "Boite evenements
 * generiques") : sequences de tests reutilisables telles quelles par
 * n'importe quelle mission, sans y etre integrees.
 *
 * Chaque boite est parametree par un prefixe (pour des ids
 * d'evenements uniques dans la mission appelante, ce qui permet
 * d'instancier plusieurs fois la meme boite) et par l'id de
 * l'evenement de la mission appelante auquel mener en cas de
 * reussite finale de la boite.
 */
public final class BoitesEvenementsGeneriques {
    private BoitesEvenementsGeneriques() {}

    /**
     * Une boite d'evenements generiques : les evenements a integrer
     * dans la mission appelante et l'id de son point d'entree.
     */
    public record Boite(List<MissionEvent> evenements, String entree) {
        public Boite {
            evenements = List.copyOf(evenements);
        }
    }

    /**
     * "Marche epuisante" (wiki, page du meme nom) : test de periple
     * en boucle ; chaque echec fait perdre du temps (souffrance) et
     * rend le prochain test de periple plus facile (+20).
     */
    public static Boite marcheEpuisante(String prefixe, String evenementEnCasDeReussite) {
        String periple = prefixe + "-periple";
        String endurance = prefixe + "-endurance";
        String forceMentale = prefixe + "-force-mentale";
        return new Boite(List.of(
                MissionEvent.builder(periple, "Test de p\u00e9riple")
                        .introduction("La marche vers le chantier \u00e0 travers la for\u00eat glaciale.")
                        .duree(Duration.ofHours(4))
                        .test(new SimpleTest(Competence.PERIPLE, 0))
                        .succes(EffetReussite.builder()
                                .texteSimple("R\u00e9ussite : vous avez atteint l'objectif.")
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("Vous avez du mal \u00e0 trouver le lieu et vous perdez "
                                        + "dans la for\u00eat glaciale.")
                                .ajustementDifficulte(20)
                                .evenementAjuste(periple)
                                .evenementSuivant(endurance)
                                .build())
                        .build(),
                MissionEvent.builder(endurance, "Souffrance (endurance, pour tous les personnages)")
                        .introduction("Le froid et la fatigue p\u00e8sent sur chaque arrangeur.")
                        .duree(Duration.ofHours(2))
                        .test(new SimpleTest(Competence.ENDURANCE, 0))
                        .testPourTous()
                        .succes(EffetReussite.builder()
                                .texteSimple("Les arrangeurs tiennent le coup.")
                                .evenementSuivant(forceMentale)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("Le froid vous terrasse.")
                                .degats(2)
                                .evenementSuivant(forceMentale)
                                .build())
                        .build(),
                MissionEvent.builder(forceMentale, "Souffrance (force mentale, pour tous les personnages)")
                        .introduction("La peur de se perdre glace le sang de chaque arrangeur.")
                        .duree(Duration.ofHours(2))
                        .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                        .testPourTous()
                        .succes(EffetReussite.builder()
                                .texteSimple("Les arrangeurs se ressaisissent et repartent.")
                                .evenementSuivant(periple)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("La panique s'installe.")
                                .perteSangFroid(2)
                                .evenementSuivant(periple)
                                .build())
                        .build()),
                periple);
    }

    /**
     * "Travail epuisant" (wiki, page du meme nom) : test d'endurance
     * au travail, avec une perte de vitalite en cas d'echec.
     */
    public static Boite travailEpuisant(String prefixe, Duration duree, String evenementEnCasDeReussite) {
        String travail = prefixe + "-travail";
        return new Boite(List.of(
                MissionEvent.builder(travail, "Travail \u00e9puisant (pour tous les personnages)")
                        .introduction("Chaque arrangeur travaille sur le chantier, dans le froid.")
                        .duree(duree)
                        .test(new SimpleTest(Competence.ENDURANCE, 0))
                        .testPourTous()
                        .succes(EffetReussite.builder()
                                .texteSimple("Travail termin\u00e9.")
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("L'\u00e9puisement vous terrasse (-2 vitalit\u00e9).")
                                .texteCritique("\u00c9chec critique : l'\u00e9puisement vous terrasse "
                                        + "(-5 vitalit\u00e9).")
                                .degats(2)
                                .degatsCritique(5)
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .build()),
                travail);
    }

    /**
     * "Escarmouches de bandits" (wiki, page du meme nom) : selon que
     * l'equipe possede une arme de tir ou non, échange de tir ou
     * protection du consul, puis corps a corps ; les echecs infligent
     * 3D10 vitalite a un personnage au hasard.
     */
    public static Boite escarmouchesDeBandits(String prefixe, String evenementEnCasDeReussite) {
        String attaque = prefixe + "-attaque";
        String tir = prefixe + "-tir";
        String protection = prefixe + "-protection";
        String corpsACorps = prefixe + "-corps-a-corps";
        return new Boite(List.of(
                MissionEvent.builder(attaque, "Attaque de bandits")
                        .introduction("Des bandits attaquent !")
                        .duree(Duration.ofHours(1))
                        .branche("Un arrangeur poss\u00e8de une arme de tir : il tente d'abattre les bandits.",
                                equipe -> equipe.stream()
                                        .anyMatch(BoitesEvenementsGeneriques::aUneArmeDeTir),
                                tir)
                        .branche("Personne n'est arm\u00e9 d'une arme de tir : il faut prot\u00e9ger le consul.",
                                equipe -> true,
                                protection)
                        .build(),
                MissionEvent.builder(tir, "\u00c9change de tir")
                        .introduction("L'arrangeur arm\u00e9 tente d'abattre les bandits.")
                        .testeurFiltre(BoitesEvenementsGeneriques::aUneArmeDeTir)
                        .test(new SimpleTest(Competence.TIR, 0))
                        .succes(EffetReussite.builder()
                                .texteSimple("Les bandits sont fauch\u00e9s par vos attaques et s'enfuient.")
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("Les tirs manquent les bandits.")
                                .evenementSuivant(corpsACorps)
                                .build())
                        .build(),
                MissionEvent.builder(protection, "Protection du consul")
                        .introduction("Sans arme de tir, il faut prot\u00e9ger le consul des assauts.")
                        .test(new SimpleTest(Competence.PROTECTION_DU_CONSUL, 0))
                        .succes(EffetReussite.builder()
                                .texteSimple("Le consul est \u00e0 l'abri.")
                                .evenementSuivant(corpsACorps)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("Les bandits passent la protection.")
                                .degatsAleatoiresMembreAuHasard(3, 10)
                                .evenementSuivant(corpsACorps)
                                .build())
                        .build(),
                MissionEvent.builder(corpsACorps, "Corps \u00e0 corps")
                        .introduction("Les arrangeurs affrontent les bandits au corps \u00e0 corps.")
                        .test(new SimpleTest(Competence.ARMES_CORPS_A_CORPS, 0))
                        .succes(EffetReussite.builder()
                                .texteSimple("Les bandits sont fauch\u00e9s par vos attaques et s'enfuient.")
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .echec(EffetEchec.builder()
                                .texteSimple("Les bandits ont le dessus.")
                                .degatsAleatoiresMembreAuHasard(3, 10)
                                .evenementSuivant(evenementEnCasDeReussite)
                                .build())
                        .build()),
                attaque);
    }

    /**
     * Un personnage possede une arme de tir (fusil, carabine,
     * pistolet, revolver, arquebuse, mousquet...) dans son inventaire.
     */
    public static boolean aUneArmeDeTir(Character personnage) {
        return personnage.inventory().stream().anyMatch(item -> {
            String i = item.toLowerCase();
            return i.contains("fusil") || i.contains("carabine") || i.contains("pistolet")
                    || i.contains("revolver") || i.contains("arquebuse") || i.contains("mousquet")
                    || i.contains("arme de tir");
        });
    }
}
