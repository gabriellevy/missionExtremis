package com.extremis.catalog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.extremis.catalog.BoitesEvenementsGeneriques.Boite;
import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Lieu;
import com.extremis.core.Mission;
import com.extremis.core.EvtMission;
import com.extremis.core.SimpleTest;
import com.extremis.core.TestAide;
import com.extremis.core.EvtMission.EffetEchec;
import com.extremis.core.EvtMission.EffetReussite;

/**
 * Catalogue des missions du wiki (page "Missions").
 */
public final class Missions {
    private Missions() {}

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
                "Rumeurs de possession démoniaque",
                "Une sonde espionne a rapporté plusieurs conversations éparses contenant des mots clés "
                        + "associés aux possessions démoniaques : folie, langues inconnues, prescience, "
                        + "profanités\u2026 Rien n\u2019est prouvé et les pistes sont faibles mais cela est plus "
                        + "prudent d\u2019enqu\u00eater.",
                Lieu.SURESNES,
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.FORCE_MENTALE),
                List.of(
                        EvtMission.builder("recherche-du-cas", "Recherche du cas")
                                .introduction("Les arrangeurs s\u2019enquièrent discrètement autour d\u2019eux.")
                                .duree(dRecherche)
                                .test(new SimpleTest(Competence.RAGOT, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous avez trouvé la victime de possession démoniaque "
                                                + "que sa famille tentait de cacher.")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs ne trouvent aucune piste fiable. "
                                                + "Recommencer à \"Recherche du cas\".")
                                        .texteCritique("Le personnage qui a échoué au test s\u2019est fait "
                                                + "détester par tout le voisinage avec ses questions. Il a été "
                                                + "chassé du quartier. Recommencer à \"Recherche du cas\".")
                                        .exclureJoueur()
                                        .evenementSuivant("recherche-du-cas")
                                        .build())
                                .build(),
                        EvtMission.builder("convaincre-la-famille", "Convaincre la famille")
                                .introduction("La famille de la victime refuse de laisser des étrangers s\u2019approcher.")
                                .duree(dConvaincre)
                                .test(new SimpleTest(Competence.ELOQUENCE, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("La famille accepte de laisser les arrangeurs approcher la victime.")
                                        .evenementSuivant("choix-du-traitement")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La famille reste hermétique. Recommencer à "
                                                + "\"Convaincre la famille\".")
                                        .evenementSuivant("convaincre-la-famille")
                                        .build())
                                .build(),
                        EvtMission.builder("choix-du-traitement", "L\u2019horrible victime")
                                .introduction("L\u2019apparence horrible de la victime possédée glace le sang des arrangeurs.")
                                .duree(Duration.ZERO)
                                .effet("L\u2019apparence horrible de la victime possédée co\u00fbte 2 de sang-froid "
                                        + "à chaque personnage.")
                                .perteSangFroidEquipe(2)
                                .branche("Un arrangeur sait pratiquer un exorcisme.",
                                        equipe -> equipe.stream().anyMatch(Missions::saitExorciser),
                                        "exorcisme")
                                .branche("Personne pour exorciser, mais un arrangeur sait soigner les esprits : "
                                        + "la victime sera internée.",
                                        equipe -> equipe.stream().anyMatch(c -> aLaSpecialite(c, "psychiatre")),
                                        "psychiatrie")
                                .branche("Personne pour soigner, mais un arrangeur cruel pourrait se débarrasser "
                                        + "de la victime.",
                                        equipe -> equipe.stream().anyMatch(c -> estCruelOuSociopathe(c)),
                                        "tentative-de-meurtre")
                                .branche("Votre équipe n\u2019a pas de quoi remplir la mission "
                                        + "mais vous pouvez en renvoyer une autre.",
                                        equipe -> true,
                                        "abandon")
                                .build(),
                        EvtMission.builder("exorcisme", "Exorcisme")
                                .introduction("Un arrangeur va tenter un exorcisme pendant que la créature se débat. "
                                        + "Sa force mentale est mise à rude épreuve (-2 sang-froid).")
                                .duree(dExorcisme)
                                .coutSangFroidTesteur(2)
                                .testeurFiltre(Missions::saitExorciser)
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le démon a été extrait de sa victime.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le rituel échoue. Le démon se manifeste avec rage. "
                                                + "Recommencer à \"Exorcisme\".")
                                        .evenementSuivant("exorcisme")
                                        .build())
                                .build(),
                        EvtMission.builder("psychiatrie", "Psychiatrie")
                                .introduction("Un arrangeur va soigner la victime en la faisant interner. "
                                        + "La scène éprouve ses nerfs (-4 sang-froid).")
                                .duree(dPsychiatrie)
                                .coutSangFroidTesteur(4)
                                .testeurFiltre(c -> aLaSpecialite(c, "psychiatre"))
                                .test(new SimpleTest(Competence.INTELLIGENCE, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous faites interner définitivement la malheureuse victime.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("L\u2019internement échoue : la famille refuse de laisser "
                                                + "emmener la victime. Recommencer à \"Psychiatrie\".")
                                        .evenementSuivant("psychiatrie")
                                        .build())
                                .build(),
                        EvtMission.builder("tentative-de-meurtre", "Tentative de meurtre")
                                .introduction("Un arrangeur cruel va tenter d\u2019assassiner la victime "
                                        + "possédée pour chasser le démon.")
                                .duree(Duration.ZERO)
                                .testeurFiltre(c -> estCruelOuSociopathe(c))
                                .test(new SimpleTest(Competence.DISCRETION, 20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le personnage a assassiné la malheureuse victime "
                                                + "possédée, chassant le démon.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La tentative échoue et le perso assassin perd 2D10 vitalité. "
                                                + "Votre équipe n\u2019a pas de quoi remplir la mission "
                                                + "mais vous pouvez en renvoyer une autre.")
                                        .degatsAleatoires(2, 10)
                                        .evenementSuivant("abandon")
                                        .build())
                                .build(),
                        EvtMission.builder("abandon", "Abandon")
                                .introduction("Votre équipe n\u2019a pas de quoi remplir la mission.")
                                .duree(Duration.ZERO)
                                .build()));
    }

    /**
     * "Construction de chemin de fer" (wiki, page du meme nom) : les
     * villages de zaporogues ont un besoin urgent de bois pour passer
     * l'hiver. La mission reference les boites d'evenements generiques
     * du wiki ("Marche epuisante", "Travail epuisant" et "Escarmouches
     * de bandits") sans les integrer : elles restent reutilisables par
     * d'autres missions.
     */
    public static Mission constructionDeCheminDeFer() {
        return cheminDeFer(Duration.ofHours(24), Duration.ofHours(4), Duration.ofHours(20),
                Duration.ofHours(10));
    }

    public static Mission constructionDeCheminDeFerRapide() {
        return cheminDeFer(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }

    private static Mission cheminDeFer(Duration dMarche, Duration dSite, Duration dDesertion,
                                       Duration dTravail) {
        Boite marche = BoitesEvenementsGeneriques.marcheEpuisante("marche", "site-atteint");
        Boite travailSupplementaire = BoitesEvenementsGeneriques.travailEpuisant("travail-supplementaire",
                Duration.ofHours(4), "au-boulot");
        Boite travailDesertion = BoitesEvenementsGeneriques.travailEpuisant("travail-desertion",
                dDesertion, travailSupplementaire.entree());
        Boite travail = BoitesEvenementsGeneriques.travailEpuisant("travail", dTravail,
                "surveillance-bandits");
        Boite escarmouche = BoitesEvenementsGeneriques.escarmouchesDeBandits("escarmouche", "victoire");

        List<EvtMission> evenements = new ArrayList<>();
        evenements.add(EvtMission.builder("atteindre-le-site", "Atteindre le site")
                .introduction("Les arrangeurs partent pour le chantier de chemin de fer, "
                        + "à travers les plaines enneigées.")
                .duree(dMarche)
                .succes(EffetReussite.builder()
                        .evenementSuivant(marche.entree())
                        .build())
                .build());
        evenements.addAll(marche.evenements());
        evenements.add(EvtMission.builder("site-atteint", "Vous avez atteint le chantier")
                .introduction("Vous avez atteint le chantier.")
                .duree(dSite)
                .succes(EffetReussite.builder()
                        .evenementSuivant("commandement")
                        .build())
                .build());
        evenements.add(EvtMission.builder("commandement", "Test de commandement")
                .introduction("Il faut mener les ouvriers du chantier d'une main de fer.")
                .test(new SimpleTest(Competence.COMMANDEMENT, 0))
                .succes(EffetReussite.builder()
                        .texteSimple("Les ouvriers obéissent aux arrangeurs.")
                        .evenementSuivant("au-boulot")
                        .build())
                .echec(EffetEchec.builder()
                        .texteSimple("De nombreux ouvriers ont déserté, ce qui va retarder le travail "
                                + "et obliger les arrangeurs à participer.")
                        .evenementSuivant("desertion")
                        .build())
                .build());
        evenements.add(EvtMission.builder("desertion", "Désertion des ouvriers")
                .introduction("De nombreux ouvriers ont déserté le chantier.")
                .duree(Duration.ZERO)
                .succes(EffetReussite.builder()
                        .evenementSuivant(travailDesertion.entree())
                        .build())
                .build());
        evenements.addAll(travailDesertion.evenements());
        evenements.addAll(travailSupplementaire.evenements());
        evenements.add(EvtMission.builder("au-boulot", "Au boulot")
                .introduction("La neige commence à tomber et le sol à geler, "
                        + "rendant le travail encore plus épuisant.")
                .succes(EffetReussite.builder()
                        .evenementSuivant(travail.entree())
                        .build())
                .build());
        evenements.addAll(travail.evenements());
        evenements.add(EvtMission.builder("surveillance-bandits", "Surveillance des environs")
                .introduction("Il faut sans cesse surveiller les environs pour prévenir "
                        + "les attaques de bandits.")
                .succes(EffetReussite.builder()
                        .evenementSuivant("vigilance")
                        .build())
                .build());
        evenements.add(EvtMission.builder("vigilance", "Test de vigilance")
                .introduction("Les arrangeurs scrutent les environs à la recherche de bandits.")
                .test(new SimpleTest(Competence.VIGILANCE, 0))
                .succes(EffetReussite.builder()
                        .texteSimple("Les environs sont s\u00fbrs pour l'instant.")
                        .evenementSuivant("victoire")
                        .build())
                .echec(EffetEchec.builder()
                        .texteSimple("Les bandits profitent d'un moment d'inattention !")
                        .evenementSuivant(escarmouche.entree())
                        .build())
                .build());
        evenements.addAll(escarmouche.evenements());
        evenements.add(EvtMission.builder("victoire", "Victoire")
                .effet("Le chantier a été mené à son terme.")
                .succes(EffetReussite.builder()
                        .evenementSuivant(null)
                        .build())
                .build());

        return new Mission(
                "construction-chemin-de-fer",
                "Construction de chemin de fer",
                "Les villages de zaporogues dans les plaines d'Ukraine ont un besoin urgent de bois "
                        + "pour passer l'hiver mais les travaux de pose des rails de chemin de fer qui "
                        + "devraient les approvisionner n'en finissent pas. Allez mettre de l'ordre "
                        + "sur le chantier. Attention, les conditions sont rudes : pas d'électricité, "
                        + "pas d'engins de chantier et la neige commence à tomber. Il y a m\u00eame des "
                        + "rumeurs de bandits en maraude.",
                Lieu.UKRAINE,
                List.of(Competence.PERIPLE, Competence.COMMANDEMENT, Competence.ENDURANCE,
                        Competence.VIGILANCE),
                evenements);
    }


    /**
     * "Pas un pays pour les vieux" (wiki, page du meme nom) : plusieurs
     * meurtres et reglements de compte ont eu lieu et la police semble
     * impuissante. Loreline a pris l'argent d'une bande de narcotrafiquants
     * et le tueur Chigur est a sa poursuite. La mission n'est reussie que
     * si Chigur et Loreline sont abattus ou arretes tous les deux : garder
     * l'argent ne suffit pas si le tueur survit.
     *
     * Le compteur "3eme echec ?" du diagramme du wiki est modelise par
     * une chaine de trois enquetes sur la piste du tueur : le premier
     * echec de l'interrogatoire de la femme ouvre la traque, chaque echec
     * de la traque rend l'enquete plus desesperee, et le troisieme echec
     * laisse Chigur s'echapper definitivement.
     */
    public static Mission pasUnPaysPourLesVieux() {
        return pasUnPays(DureesMissionPays.normales());
    }

    public static Mission pasUnPaysPourLesVieuxRapide() {
        return pasUnPays(DureesMissionPays.rapides());
    }

    /**
     * Durees des evenements de la mission "Pas un pays pour les vieux",
     * raccourcissables pour les tests rapides.
     */
    private record DureesMissionPays(Duration enqueteTemoins, Duration interrogationFemme,
                                      Duration recherche, Duration traqueTueur,
                                      Duration tir, Duration vol, Duration poursuite) {
        static DureesMissionPays normales() {
            return new DureesMissionPays(Duration.ofHours(6), Duration.ofHours(3),
                    Duration.ofHours(8), Duration.ofHours(8), Duration.ofHours(3),
                    Duration.ofHours(1), Duration.ofHours(24));
        }

        static DureesMissionPays rapides() {
            return new DureesMissionPays(Duration.ofSeconds(1), Duration.ofSeconds(1),
                    Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                    Duration.ofSeconds(1), Duration.ofSeconds(1));
        }
    }

    private static Mission pasUnPays(DureesMissionPays d) {
        return new Mission(
                "pas-un-pays-pour-les-vieux",
                "Pas un pays pour les vieux",
                "Plusieurs meurtres et règlements de compte ont eu lieu successivement et la "
                        + "police semble impuissante à arrêter qui que ce soit.",
                Lieu.SAINT_DENIS,
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.EVALUATION,
                        Competence.TIR, Competence.DISCRETION),
                List.of(
                        EvtMission.builder("enquete-de-quartier", "Interroger les témoins")
                                .introduction("Plusieurs meurtres et règlements de compte ont eu "
                                        + "lieu dans le quartier et la police semble impuissante. "
                                        + "Les arrangeurs interrogent discrètement les témoins.")
                                .duree(d.enqueteTemoins())
                                .test(new SimpleTest(Competence.RAGOT, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Un nommé Loreline a pris l'argent d'une bande "
                                                + "de narcotrafiquants et des tueurs sont lancés "
                                                + "à sa poursuite.")
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Personne ne veut parler. Recommencer à "
                                                + "\"Interroger les témoins\".")
                                        .evenementSuivant("enquete-de-quartier")
                                        .build())
                                .build(),
                        EvtMission.builder("interroger-la-femme", "Interrogatoire de la femme de Loreline")
                                .introduction("La femme de Loreline pourrait révéler où il se "
                                        + "terre.")
                                .duree(d.interrogationFemme())
                                .test(new SimpleTest(Competence.ELOQUENCE, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Elle vous met sur la piste de son mari pour "
                                                + "le protéger des tueurs.")
                                        .evenementSuivant("localiser-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Elle se mure dans le silence : il ne reste "
                                                + "plus qu'à tenter de retrouver les tueurs.")
                                        .evenementSuivant("piste-du-tueur-1")
                                        .build())
                                .build(),
                        EvtMission.builder("localiser-lorelime", "Retrouver Loreline")
                                .introduction("La piste mène à Loreline, l'homme qui a pris "
                                        + "l'argent du cartel.")
                                .duree(d.recherche())
                                .test(new SimpleTest(Competence.EVALUATION, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez Loreline et il a l'argent "
                                                + "avec lui. Une vraie fortune.")
                                        .evenementSuivant("la-tentation-du-butin")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline est introuvable : il ne reste plus qu'à tenter "
                                                + "de retrouver les tueurs.")
                                        .evenementSuivant("piste-du-tueur-1")
                                        .build())
                                .build(),
                        EvtMission.builder("la-tentation-du-butin", "Une vraie fortune")
                                .introduction("Loreline et sa fortune sont à portée de main.")
                                .duree(Duration.ZERO)
                                .branche("Un arrangeur cupide ou impulsif n'arrive pas à quitter "
                                        + "la mallette des yeux.",
                                        equipe -> equipe.stream().anyMatch(Missions::estCupideOuImpulsif),
                                        "resister-a-la-tentation")
                                .branche("Les arrangeurs tentent de convaincre Loreline de se "
                                        + "rendre.",
                                        equipe -> true,
                                        "convaincre-lorelime")
                                .build(),
                        EvtMission.builder("resister-a-la-tentation", "Résister à la tentation")
                                .introduction("Il faut tenir bon face à un pareil butin.")
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("L'envie de s'emparer de l'argent est "
                                                + "ma\u00eetrisée : les arrangeurs tendent la main à "
                                                + "Loreline.")
                                        .evenementSuivant("convaincre-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs cèdent à la tentation : ils "
                                                + "décident de voler l'argent.")
                                        .evenementSuivant("voler-le-butin")
                                        .build())
                                .build(),
                        EvtMission.builder("voler-le-butin", "Voler l'argent")
                                .introduction("Les arrangeurs braquent leurs armes sur Loreline "
                                        + "et sa fortune.")
                                .duree(d.vol())
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs abattent Loreline et "
                                                + "s'emparent de l'argent du cartel.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La fusillade tourne mal : un personnage au "
                                                + "hasard subit 2D10 de dég\u00e2ts. Recommencer à "
                                                + "\"Voler l'argent\".")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("voler-le-butin")
                                        .build())
                                .build(),
                        EvtMission.builder("convaincre-lorelime", "Convaincre Loreline")
                                .introduction("Se rendre est la seule chance de survie de "
                                        + "Loreline : les arrangeurs le lui expliquent.")
                                .test(new SimpleTest(Competence.ELOQUENCE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Loreline se rend et l'argent du cartel est "
                                                + "récupéré.")
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline ne se laissera pas faire sans "
                                                + "combattre.")
                                        .evenementSuivant("la-riposte-de-lorelime")
                                        .build())
                                .build(),
                        EvtMission.builder("la-riposte-de-lorelime", "Riposte de Loreline")
                                .introduction("Loreline dégaine et tire avant de tenter de fuir.")
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous le couvrez avant qu'il n'atteigne la "
                                                + "porte : Loreline se rend et l'argent du cartel est "
                                                + "récupéré.")
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline s'enfuit ! Un personnage au hasard "
                                                + "est blessé et subit 2D10 de dég\u00e2ts. Il faut "
                                                + "retrouver les tueurs qui le traquent.")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("piste-du-tueur-1")
                                        .build())
                                .build(),
                        EvtMission.builder("poursuivis-par-chigur", "Poursuivis par le tueur")
                                .introduction("Le cartel a lancé son tueur à vos trousses : "
                                        + "Chigur traque désormais les arrangeurs et leur argent.")
                                .duree(d.poursuite())
                                .succes(EffetReussite.builder()
                                        .texteSimple("Après un jour de fuite, les arrangeurs "
                                                + "tentent de semer le tueur.")
                                        .evenementSuivant("rester-discrets")
                                        .build())
                                .build(),
                        EvtMission.builder("rester-discrets", "Se cacher du tueur")
                                .introduction("Chigur est aux aguets : chaque arrangeur tente "
                                        + "de rester discret.")
                                .test(new SimpleTest(Competence.DISCRETION, -10))
                                .testPourTous()
                                .succes(EffetReussite.builder()
                                        .texteSimple("Chigur a perdu la trace des arrangeurs.")
                                        .evenementSuivant("fin-de-la-traque")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le tueur a repéré les arrangeurs.")
                                        .evenementSuivant("attaque-de-chigur")
                                        .build())
                                .build(),
                        EvtMission.builder("fin-de-la-traque", "Fin de la traque")
                                .introduction("Les arrangeurs ont semé le tueur\u2026 mais celui-ci "
                                        + "a survécu.")
                                .duree(Duration.ZERO)
                                .effet("Le tueur a survécu : les arrangeurs ont échoué. "
                                        + "Seul un butin volé rapporte un +10 Richesse.")
                                .build(),
                        EvtMission.builder("attaque-de-chigur", "Attaque de Chigur")
                                .introduction("Chigur surgit de nulle part et ouvre le feu.")
                                .duree(d.tir())
                                .test(new SimpleTest(Competence.TIR, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs abattent Chigur. Loreline "
                                                + "est déjà neutralisé : la mission est "
                                                + "accomplie.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("L'arrangeur attaqué subit 3D10 de dég\u00e2ts. "
                                                + "Le tueur reprend la traque.")
                                        .degatsAleatoires(3, 10)
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .build(),
                        EvtMission.builder("piste-du-tueur-1", "Piste du tueur")
                                .introduction("Les arrangeurs analysent les indices pour "
                                        + "retrouver le tueur Chigur.")
                                .duree(d.traqueTueur())
                                .test(new SimpleTest(Competence.EVALUATION, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez le principal et il attaque "
                                                + "aussit\u00f4t.")
                                        .evenementSuivant("confrontation-avec-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les indices sont maigres : la traque "
                                                + "continue.")
                                        .evenementSuivant("piste-du-tueur-2")
                                        .build())
                                .build(),
                        EvtMission.builder("piste-du-tueur-2", "La piste se refroidit")
                                .introduction("Les arrangeurs reprennent la traque du tueur "
                                        + "Chigur.")
                                .duree(d.traqueTueur())
                                .test(new SimpleTest(Competence.EVALUATION, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez le principal et il attaque "
                                                + "aussit\u00f4t.")
                                        .evenementSuivant("confrontation-avec-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Chigur est encore passé par là, mais la "
                                                + "chance de le retrouver s'amenuise.")
                                        .evenementSuivant("piste-du-tueur-3")
                                        .build())
                                .build(),
                        EvtMission.builder("piste-du-tueur-3", "Dernière piste")
                                .introduction("C'est la dernière chance de remonter jusqu'au "
                                        + "tueur Chigur.")
                                .duree(d.traqueTueur())
                                .test(new SimpleTest(Competence.EVALUATION, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez le principal et il attaque "
                                                + "aussit\u00f4t.")
                                        .evenementSuivant("confrontation-avec-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le tueur s'est volatilisé dans la nature.")
                                        .evenementSuivant("chigur-echappe")
                                        .build())
                                .build(),
                        EvtMission.builder("chigur-echappe", "Chigur vous a échappé")
                                .introduction("Le tueur et Loreline ont disparu sans laisser de "
                                        + "traces.")
                                .duree(Duration.ZERO)
                                .effet("Plus rien à faire : Chigur vous a échappé, la mission "
                                        + "échoue.")
                                .build(),
                        EvtMission.builder("confrontation-avec-chigur", "Confrontation avec "
                                + "Chigur")
                                .introduction("Vous avez retrouvé le tueur et il ne compte pas "
                                        + "vous laisser vivants.")
                                .duree(d.tir())
                                .test(new SimpleTest(Competence.TIR, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous abattez le tueur Chigur.")
                                        .evenementSuivant("chigur-abattu")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La fusillade tourne mal.")
                                        .evenementSuivant("la-fusillade-de-chigur")
                                        .build())
                                .build(),
                        EvtMission.builder("la-fusillade-de-chigur", "Fusillade de Chigur")
                                .introduction("Le tueur canarde tout ce qui bouge : chaque "
                                        + "arrangeur tente sa chance.")
                                .test(new SimpleTest(Competence.CHANCE, 0))
                                .testPourTous()
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le tueur vous a échappé pour cette fois.")
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Celui qui rate son test de chance subit "
                                                + "3D10 de dég\u00e2ts.")
                                        .degatsAleatoires(3, 10)
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .build(),
                        EvtMission.builder("chigur-abattu", "Chigur est abattu")
                                .introduction("Le tueur du cartel g\u00eet au sol.")
                                .duree(Duration.ZERO)
                                .effet("Le tueur Chigur est neutralisé. Reste à retrouver "
                                        + "Loreline et l'argent pour accomplir la mission.")
                                .succes(EffetReussite.builder()
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        EvtMission.builder("retrouver-le-fugitif", "Retrouver le fugitif")
                                .introduction("Loreline se terre quelque part avec la fortune du "
                                        + "cartel.")
                                .duree(d.recherche())
                                .test(new SimpleTest(Competence.EVALUATION, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez Loreline et il a toujours "
                                                + "l'argent avec lui.")
                                        .evenementSuivant("le-butin-sans-menace")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline est introuvable. Recommencer à "
                                                + "\"Retrouver le fugitif\".")
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        EvtMission.builder("le-butin-sans-menace", "Le butin sans menace")
                                .introduction("Plus personne ne menace les arrangeurs : il ne "
                                        + "reste qu'à décider du sort de Loreline.")
                                .duree(Duration.ZERO)
                                .branche("Un arrangeur cupide ou impulsif n'arrive pas à quitter "
                                        + "la mallette des yeux.",
                                        equipe -> equipe.stream().anyMatch(Missions::estCupideOuImpulsif),
                                        "resister-malgre-le-butin")
                                .branche("Les arrangeurs tentent d'amener Loreline à se rendre.",
                                        equipe -> true,
                                        "sommation-a-lorelime")
                                .build(),
                        EvtMission.builder("resister-malgre-le-butin", "Résister malgré le "
                                + "butin")
                                .introduction("Il faut tenir bon face à un pareil butin.")
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("L'envie de s'emparer de l'argent est "
                                                + "ma\u00eetrisée : les arrangeurs tendent la main à "
                                                + "Loreline.")
                                        .evenementSuivant("sommation-a-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs cèdent à la tentation : ils "
                                                + "décident de voler l'argent.")
                                        .evenementSuivant("abattre-le-fugitif")
                                        .build())
                                .build(),
                        EvtMission.builder("sommation-a-lorelime", "Sommation à Loreline")
                                .introduction("Chigur est mort : les arrangeurs somment Loreline "
                                        + "de rendre l'argent et de se rendre.")
                                .test(new SimpleTest(Competence.ELOQUENCE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Loreline se rend et l'argent du cartel est "
                                                + "récupéré. Chigur et Loreline sont "
                                                + "neutralisés : victoire.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline ne se laissera pas faire sans "
                                                + "combattre.")
                                        .evenementSuivant("la-riposte-du-fugitif")
                                        .build())
                                .build(),
                        EvtMission.builder("la-riposte-du-fugitif", "Riposte du fugitif")
                                .introduction("Loreline dégaine et tire avant de tenter de fuir.")
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous le couvrez avant qu'il n'atteigne la "
                                                + "porte : Loreline se rend et l'argent du cartel est "
                                                + "récupéré. Chigur et Loreline sont "
                                                + "neutralisés : victoire.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline s'enfuit ! Un personnage au "
                                                + "hasard est blessé et subit 2D10 de dég\u00e2ts.")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        EvtMission.builder("abattre-le-fugitif", "Abattre le fugitif")
                                .introduction("Les arrangeurs décident d'abattre Loreline pour "
                                        + "prendre l'argent.")
                                .duree(d.vol())
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs abattent Loreline et "
                                                + "s'emparent de l'argent du cartel. Chigur et "
                                                + "Loreline sont neutralisés : victoire.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La fusillade tourne mal : un personnage au "
                                                + "hasard subit 2D10 de dég\u00e2ts. Recommencer à "
                                                + "\"Abattre le fugitif\".")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("abattre-le-fugitif")
                                        .build())
                                .build()));
    }

    /**
     * "Congrès Komsomol houleux" (wiki, page du même nom) : un congrès
     * politique de jeunes communistes tourne mal, des courants rivaux
     * s'entredéchirent. Les arrangeurs prennent la température (test de
     * ragot aidé) puis tentent successivement un discours apaisant, un
     * jeu d'influence et, s'il faut, la bagarre. Une bonne prise de
     * température (+20 aux tests suivants) peut seule sauver le congrès.
     */
    public static Mission congresKomsomolHouleux() {
        return congresKomsomol(DureesMissionCongres.normales());
    }

    public static Mission congresKomsomolHouleuxRapide() {
        return congresKomsomol(DureesMissionCongres.rapides());
    }

    /**
     * Durées des événements de la mission "Congrès Komsomol houleux",
     * raccourcissables pour les tests rapides.
     */
    private record DureesMissionCongres(Duration priseDeTemperature, Duration discours,
                                         Duration influence, Duration bagarre) {
        static DureesMissionCongres normales() {
            return new DureesMissionCongres(Duration.ofHours(6), Duration.ofHours(2),
                    Duration.ofHours(2), Duration.ofHours(2));
        }

        static DureesMissionCongres rapides() {
            return new DureesMissionCongres(Duration.ofSeconds(1), Duration.ofSeconds(1),
                    Duration.ofSeconds(1), Duration.ofSeconds(1));
        }
    }

    private static Mission congresKomsomol(DureesMissionCongres d) {
        return new Mission(
                "congres-komsomol-houleux",
                "Congrès Komsomol houleux",
                "Un congrès politique de jeunes communistes tourne mal. "
                        + "Des courants rivaux vont s’entredéchirer.",
                Lieu.UKRAINE,
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.INTIMIDATION,
                        Competence.BAGARRE),
                List.of(
                        EvtMission.builder("prise-de-temperature", "Prise de température")
                                .introduction("Les arrangeurs prennent la température du congrès "
                                        + "et cherchent à comprendre ce qui se passe mal.")
                                .duree(d.priseDeTemperature())
                                .test(new TestAide(Competence.RAGOT, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous parvenez à comprendre les bases des courants, "
                                                + "leurs divergences et les fauteurs de trouble.")
                                        .ajustementDifficulte(20, "discours", "jeu-d-influence",
                                                "bagarre-generale")
                                        .evenementSuivant("discours")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Impossible de s’y retrouver dans ces gens "
                                                + "énervés et obtus.")
                                        .evenementSuivant("discours")
                                        .build())
                                .build(),
                        EvtMission.builder("discours", "Discours apaisant")
                                .introduction("Les arrangeurs vont tenter de les convaincre de "
                                        + "continuer à débattre dans le calme et le respect.")
                                .duree(d.discours())
                                .test(new SimpleTest(Competence.ELOQUENCE, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous avez réussi à calmer le jeu : les choses "
                                                + "se passent calmement dans le respect de la "
                                                + "constitution.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Votre discours les laisse froids. Peut-être que "
                                                + "mettre en avant votre statut d’envoyé du consul "
                                                + "les impressionnera ?")
                                        .evenementSuivant("jeu-d-influence")
                                        .build())
                                .build(),
                        EvtMission.builder("jeu-d-influence", "Statut d’envoyé du consul")
                                .introduction("Un arrangeur va mettre en avant son statut d’envoyé "
                                        + "du consul pour impressionner l’assemblée.")
                                .duree(d.influence())
                                .test(new TestAide(Competence.INTIMIDATION, -10))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Votre statut d’envoyé du consul les impressionne : "
                                                + "les choses se passent calmement dans le respect "
                                                + "de la constitution.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Non seulement ils ne sont pas impressionnés mais "
                                                + "certains tentent de vous agresser.")
                                        .evenementSuivant("bagarre-generale")
                                        .build())
                                .build(),
                        EvtMission.builder("bagarre-generale", "Bagarre générale")
                                .introduction("Certains congressistes agressent les arrangeurs : "
                                        + "chacun tente de quitter la salle sans dommage.")
                                .duree(d.bagarre())
                                .test(new SimpleTest(Competence.BAGARRE, 0))
                                .testPourTous()
                                .succes(EffetReussite.builder()
                                        .texteSimple("Cet arrangeur parvient à quitter la salle "
                                                + "sans dommage.")
                                        .evenementSuivant("echec-du-congres")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Cet arrangeur prend des coups. Il subit 1D10 de "
                                                + "dégâts de vitalité.")
                                        .degatsAleatoires(1, 10)
                                        .evenementSuivant("echec-du-congres")
                                        .build())
                                .build(),
                        EvtMission.builder("echec-du-congres", "Échec du congrès")
                                .introduction("Les arrangeurs n’ont pas réussi à éviter les "
                                        + "débordements.")
                                .duree(Duration.ZERO)
                                .effet("Il y a eu de nombreux blessés et le congrès est un échec "
                                        + "complet.")
                                .build()));
    }

    private static boolean aLaSpecialite(Character personnage, String prefixe) {
        return personnage.specialites().stream()
                .anyMatch(s -> s.toLowerCase().startsWith(prefixe));
    }

    private static boolean aLaMaitrise(Character personnage, String prefixe) {
        return personnage.maitrises().stream()
                .anyMatch(m -> m.toLowerCase().startsWith(prefixe));
    }

    private static boolean saitExorciser(Character personnage) {
        return aLaMaitrise(personnage, "exorcisme") || aLaSpecialite(personnage, "exorcisme");
    }

    private static boolean estCruelOuSociopathe(Character personnage) {
        return personnage.traits().stream()
                .anyMatch(t -> {
                    String nom = t.name().toLowerCase();
                    return nom.startsWith("cruel") || nom.startsWith("sociopath");
                });
    }

    private static boolean estCupideOuImpulsif(Character personnage) {
        return personnage.traits().stream()
                .anyMatch(t -> {
                    String nom = t.name().toLowerCase();
                    return nom.startsWith("cupide") || nom.startsWith("impulsif");
                });
    }
}
