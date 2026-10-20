package com.extremis.catalog;

import java.time.Duration;
import java.util.List;

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
     * "Rumeurs de possession démoniaque" (wiki, page du meme nom) :
     * une sonde espionne a rapporte des conversations contenant des
     * mots cles associes aux possessions demoniaques. La mission
     * complete l'enquete jusqu'a l'exorcisme.
     */
    public static Mission rumeursDePossessionDemoniaque() {
        return possession(Duration.ofHours(6), Duration.ofHours(3), Duration.ofHours(4),
                Duration.ofHours(2), Duration.ofHours(8), Duration.ofHours(5));
    }

    public static Mission rumeursDePossessionDemoniaqueRapide() {
        return possession(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static Mission possession(Duration dRecherche, Duration dConvaincre, Duration dExorcisme,
                                      Duration dRituel, Duration dSurveillance, Duration dPurification) {
        return new Mission(
                "possession",
                "Rumeurs de possession démoniaque",
                "Une sonde espionne a rapporte plusieurs conversations eparses contenant des mots cles "
                        + "associes aux possessions demoniaques : folie, langues inconnues, prescience, "
                        + "profanites... Rien n'est prouve et les pistes sont faibles mais cela est plus "
                        + "prudent d'enqueter.",
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.FORCE_MENTALE),
                List.of(
                        MissionEvent.builder("recherche-du-cas", "Recherche du cas")
                                .introduction("Les arrangeurs s'enquièrent discrètement autour d'eux.")
                                .duree(dRecherche)
                                .test(new SimpleTest(Competence.RAGOT, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous avez trouvé la victime de possession démoniaque "
                                                + "que sa famille tentait de cacher.")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs ne trouvent aucune piste fiable. "
                                                + "Recommencer à \"Recherche du cas\". La difficulté du test "
                                                + "d'exorcisme sera baissée de 10.")
                                        .texteCritique("\\[nom du perso\\] s'est fait détester par tout le "
                                                + "voisinage avec ses questions. Il a été chassé du quartier. "
                                                + "La difficulté du test d'exorcisme sera baissée de 10.")
                                        .exclureJoueur()
                                        .ajustementDifficulte(10)
                                        .evenementAjuste("exorcisme")
                                        .evenementSuivant("recherche-du-cas")
                                        .build())
                                .build(),
                        MissionEvent.builder("convaincre-la-famille", "Convaincre la famille")
                                .introduction("La famille de la victime refuse de laisser des étrangers s'approcher.")
                                .duree(dConvaincre)
                                .test(new SimpleTest(Competence.ELOQUENCE, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("La famille accepte de laisser les arrangeurs tenter "
                                                + "l'exorcisme.")
                                        .evenementSuivant("exorcisme")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La famille reste hermétique. Recommencer à "
                                                + "\"Convaincre la famille\". La difficulté du test "
                                                + "d'exorcisme sera baissée de 10.")
                                        .ajustementDifficulte(10)
                                        .evenementAjuste("exorcisme")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .build(),
                        MissionEvent.builder("exorcisme", "Exorcisme")
                                .introduction("Les arrangeurs commencent le rituel d'exorcisme pendant que "
                                        + "la créature se débat.")
                                .duree(dExorcisme)
                                .test(new SimpleTest(Competence.FORCE_MENTALE, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le rituel achevé, la victime redevient elle-même. "
                                                + "La créature est bannie.")
                                        .evenementSuivant("surveillance")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le rituel échoue. Le démon se manifeste avec rage. "
                                                + "Recommencer à \"Exorcisme\".")
                                        .texteCritique("Le rituel tourne mal : le démon hurle dans une langue "
                                                + "inconnue et l'un des arrangeurs perd pied. "
                                                + "Recommencer à \"Exorcisme\".")
                                        .degats(2)
                                        .perteSangFroid(2)
                                        .evenementSuivant("exorcisme")
                                        .build())
                                .build(),
                        MissionEvent.builder("surveillance", "Surveillance de la victime")
                                .introduction("Tant que la victime ne fait aucune rechute, les arrangeurs "
                                        + "la surveillent la nuit venue.")
                                .duree(dSurveillance)
                                .test(new SimpleTest(Competence.VIGILANCE, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("La nuit se passe sans incident : la victime dort "
                                                + "paisiblement, définitivement délivrée.")
                                        .evenementSuivant("purification")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La victime fait une rechute nocturne : les arrangeurs "
                                                + "doivent recommencer la surveillance.")
                                        .evenementSuivant("exorcisme")
                                        .build())
                                .build(),
                        MissionEvent.builder("purification", "Purification du foyer")
                                .introduction("Le foyer doit être purifié pour empêcher toute nouvelle intrusion.")
                                .duree(dPurification)
                                .test(new SimpleTest(Competence.MAGIE, -30))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le foyer est purifié. La famille est sauvée et la "
                                                + "possession démoniaque ne reviendra pas.")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La purification est imparfaite : le mal est affaibli "
                                                + "mais pas dissipé. Recommencer à \"Purification du foyer\".")
                                        .evenementSuivant("purification")
                                        .build())
                                .build()));
    }
}
