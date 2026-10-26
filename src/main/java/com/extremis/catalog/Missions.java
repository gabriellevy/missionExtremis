package com.extremis.catalog;

import java.time.Duration;
import java.util.List;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Mission;
import com.extremis.core.MissionEvent;
import com.extremis.core.SimpleTest;
import com.extremis.core.MissionEvent.EffetEchec;
import com.extremis.core.MissionEvent.EffetReussite;

/**
 * Catalogue des missions du wiki (page "Missions").
 */
public final class Missions {
    private Missions() {}

    public static Mission fousDeGotheim() {
        return gotheim(Duration.ofHours(4), Duration.ofHours(6));
    }

    public static Mission fousDeGotheimRapide() {
        return gotheim(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static Mission gotheim(Duration delai2, Duration delai3) {
        return new Mission(
                "gotheim",
                "Les fous de Gotheim",
                "Une secte agite la ville de Gotheim. Les arrangeurs doivent infiltrer puis neutraliser ses meneurs.",
                List.of(Competence.DISCRETION, Competence.ARMES_CORPS_A_CORPS),
                List.of(
                        MissionEvent.builder("arrivee", "Arrivee a Gotheim")
                                .introduction("Les arrangeurs arrivent a Gotheim et reperent la secte.")
                                .duree(Duration.ZERO)
                                .succes(EffetReussite.builder()
                                        .evenementSuivant("infiltration")
                                        .build())
                                .build(),
                        MissionEvent.builder("infiltration", "Infiltration du meeting nocturne")
                                .introduction("Les arrangeurs s'infiltrent dans le meeting nocturne de la secte.")
                                .duree(delai2)
                                .test(new SimpleTest(Competence.DISCRETION, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs passent inapercus au milieu des sectateurs.")
                                        .recompense("Toge de sectateur")
                                        .evenementSuivant("confrontation")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les sectateurs repèrent les arrangeurs et les passent a tabac.")
                                        .texteCritique("Repérés dès leur arrivee, les arrangeurs sont chasses a coups de pierre.")
                                        .degats(2)
                                        .evenementSuivant("confrontation")
                                        .build())
                                .build(),
                        MissionEvent.builder("confrontation", "Confrontation avec les meneurs")
                                .introduction("Les arrangeurs affrontent les meneurs de la secte.")
                                .duree(delai3)
                                .test(new SimpleTest(Competence.ARMES_CORPS_A_CORPS, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les meneurs sont neutralises. La secte se disperse.")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les meneurs ont le dessus. Les arrangeurs battent en retraite.")
                                        .degats(4)
                                        .build())
                                .build()));
    }

    /**
     * "Rumeurs de possession demoniaque" (wiki, page du meme nom) :
     * une sonde espionne a rapporte des conversations contenant des
     * mots cles associes aux possessions demoniaques. La mission
     * complete l'enquete puis branche selon les moyens de l'equipe :
     * exorcisme, psychiatrie, assassinat de la victime ou abandon.
     */
    public static Mission rumeursDePossessionDemoniaque() {
        return possession(Duration.ofHours(6), Duration.ofHours(3), Duration.ofHours(4),
                Duration.ofHours(12));
    }

    public static Mission rumeursDePossessionDemoniaqueRapide() {
        return possession(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }

    private static Mission possession(Duration dRecherche, Duration dConvaincre, Duration dExorcisme,
                                      Duration dPsychiatrie) {
        return new Mission(
                "possession",
                "Rumeurs de possession d\u00e9moniaque",
                "Une sonde espionne a rapport\u00e9 plusieurs conversations \u00e9parses contenant des mots cl\u00e9s "
                        + "associ\u00e9s aux possessions d\u00e9moniaques : folie, langues inconnues, prescience, "
                        + "profanit\u00e9s\u2026 Rien n\u2019est prouv\u00e9 et les pistes sont faibles mais cela est plus "
                        + "prudent d\u2019enqu\u00eater.",
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.FORCE_MENTALE),
                List.of(
                        MissionEvent.builder("recherche-du-cas", "Recherche du cas")
                                .introduction("Les arrangeurs s\u2019enqui\u00e8rent discr\u00e8tement autour d\u2019eux.")
                                .duree(dRecherche)
                                .test(new SimpleTest(Competence.RAGOT, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous avez trouv\u00e9 la victime de possession d\u00e9moniaque "
                                                + "que sa famille tentait de cacher.")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs ne trouvent aucune piste fiable. "
                                                + "Recommencer \u00e0 \"Recherche du cas\".")
                                        .texteCritique("Le personnage qui a \u00e9chou\u00e9 au test s\u2019est fait "
                                                + "d\u00e9tester par tout le voisinage avec ses questions. Il a \u00e9t\u00e9 "
                                                + "chass\u00e9 du quartier. Recommencer \u00e0 \"Recherche du cas\".")
                                        .exclureJoueur()
                                        .evenementSuivant("recherche-du-cas")
                                        .build())
                                .build(),
                        MissionEvent.builder("convaincre-la-famille", "Convaincre la famille")
                                .introduction("La famille de la victime refuse de laisser des \u00e9trangers s\u2019approcher.")
                                .duree(dConvaincre)
                                .test(new SimpleTest(Competence.ELOQUENCE, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("La famille accepte de laisser les arrangeurs approcher la victime.")
                                        .evenementSuivant("choix-du-traitement")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La famille reste herm\u00e9tique. Recommencer \u00e0 "
                                                + "\"Convaincre la famille\".")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .build(),
                        MissionEvent.builder("choix-du-traitement", "L\u2019horrible victime")
                                .introduction("L\u2019apparence horrible de la victime poss\u00e9d\u00e9e glace le sang des arrangeurs.")
                                .duree(Duration.ZERO)
                                .effet("L\u2019apparence horrible de la victime poss\u00e9d\u00e9e co\u00fbte 2 de sang-froid "
                                        + "\u00e0 chaque personnage.")
                                .perteSangFroidEquipe(2)
                                .branche("Un arrangeur sait pratiquer un exorcisme.",
                                        equipe -> equipe.stream().anyMatch(c -> aLaSpecialite(c, "exorcisme")),
                                        "exorcisme")
                                .branche("Personne pour exorciser, mais un arrangeur sait soigner les esprits : "
                                        + "la victime sera intern\u00e9e.",
                                        equipe -> equipe.stream().anyMatch(c -> aLaSpecialite(c, "psychiatre")),
                                        "psychiatrie")
                                .branche("Personne pour soigner, mais un arrangeur cruel pourrait se d\u00e9barrasser "
                                        + "de la victime.",
                                        equipe -> equipe.stream().anyMatch(c -> estCruelOuSociopathe(c)),
                                        "tentative-de-meurtre")
                                .branche("Votre \u00e9quipe n\u2019a pas de quoi remplir la mission "
                                        + "mais vous pouvez en renvoyer une autre.",
                                        equipe -> true,
                                        "abandon")
                                .build(),
                        MissionEvent.builder("exorcisme", "Exorcisme")
                                .introduction("Un arrangeur va tenter un exorcisme pendant que la cr\u00e9ature se d\u00e9bat. "
                                        + "Sa force mentale est mise \u00e0 rude \u00e9preuve (-2 sang-froid).")
                                .duree(dExorcisme)
                                .coutSangFroidTesteur(2)
                                .testeurFiltre(c -> aLaSpecialite(c, "exorcisme"))
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le d\u00e9mon a \u00e9t\u00e9 extrait de sa victime.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le rituel \u00e9choue. Le d\u00e9mon se manifeste avec rage. "
                                                + "Recommencer \u00e0 \"Exorcisme\".")
                                        .evenementSuivant("exorcisme")
                                        .build())
                                .build(),
                        MissionEvent.builder("psychiatrie", "Psychiatrie")
                                .introduction("Un arrangeur va soigner la victime en la faisant interner. "
                                        + "La sc\u00e8ne \u00e9prouve ses nerfs (-4 sang-froid).")
                                .duree(dPsychiatrie)
                                .coutSangFroidTesteur(4)
                                .testeurFiltre(c -> aLaSpecialite(c, "psychiatre"))
                                .test(new SimpleTest(Competence.INTELLIGENCE, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous faites interner d\u00e9finitivement la malheureuse victime.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("L\u2019internement \u00e9choue : la famille refuse de laisser "
                                                + "emmener la victime. Recommencer \u00e0 \"Psychiatrie\".")
                                        .evenementSuivant("psychiatrie")
                                        .build())
                                .build(),
                        MissionEvent.builder("tentative-de-meurtre", "Tentative de meurtre")
                                .introduction("Un arrangeur cruel va tenter d\u2019assassiner la victime "
                                        + "poss\u00e9d\u00e9e pour chasser le d\u00e9mon.")
                                .duree(Duration.ZERO)
                                .testeurFiltre(c -> estCruelOuSociopathe(c))
                                .test(new SimpleTest(Competence.DISCRETION, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le personnage a assassin\u00e9 la malheureuse victime "
                                                + "poss\u00e9d\u00e9e, chassant le d\u00e9mon.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La tentative \u00e9choue et le perso assassin perd 2D10 vitalit\u00e9. "
                                                + "Votre \u00e9quipe n\u2019a pas de quoi remplir la mission "
                                                + "mais vous pouvez en renvoyer une autre.")
                                        .degatsAleatoires(2, 10)
                                        .evenementSuivant(null)
                                        .build())
                                .build(),
                        MissionEvent.builder("abandon", "Abandon")
                                .introduction("Votre \u00e9quipe n\u2019a pas de quoi remplir la mission.")
                                .duree(Duration.ZERO)
                                .build()));
    }

    private static boolean aLaSpecialite(Character personnage, String prefixe) {
        return personnage.specialites().stream()
                .anyMatch(s -> s.toLowerCase().startsWith(prefixe));
    }

    private static boolean estCruelOuSociopathe(Character personnage) {
        return personnage.traits().stream()
                .anyMatch(t -> {
                    String nom = t.name().toLowerCase();
                    return nom.startsWith("cruel") || nom.startsWith("sociopath");
                });
    }
}
