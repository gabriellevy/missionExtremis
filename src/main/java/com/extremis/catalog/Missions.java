package com.extremis.catalog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.extremis.catalog.BoitesEvenementsGeneriques.Boite;
import com.extremis.core.Character;
import com.extremis.core.Competence;
import com.extremis.core.Lieu;
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
                Lieu.SURESNES,
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
                                        equipe -> equipe.stream().anyMatch(Missions::saitExorciser),
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
                                .testeurFiltre(Missions::saitExorciser)
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
                                       Duration dTravailSupplementaire) {
        Boite marche = BoitesEvenementsGeneriques.marcheEpuisante("marche", "site-atteint");
        Boite travailSupplementaire = BoitesEvenementsGeneriques.travailEpuisant("travail-supplementaire",
                dTravailSupplementaire, "au-boulot");
        Boite travailDesertion = BoitesEvenementsGeneriques.travailEpuisant("travail-desertion",
                Duration.ofHours(4), travailSupplementaire.entree());
        Boite travail = BoitesEvenementsGeneriques.travailEpuisant("travail", Duration.ofHours(4),
                "surveillance-bandits");
        Boite escarmouche = BoitesEvenementsGeneriques.escarmouchesDeBandits("escarmouche", "victoire");

        List<MissionEvent> evenements = new ArrayList<>();
        evenements.add(MissionEvent.builder("atteindre-le-site", "Atteindre le site")
                .introduction("Les arrangeurs partent pour le chantier de chemin de fer, "
                        + "\u00e0 travers les plaines enneig\u00e9es.")
                .duree(dMarche)
                .succes(EffetReussite.builder()
                        .evenementSuivant(marche.entree())
                        .build())
                .build());
        evenements.addAll(marche.evenements());
        evenements.add(MissionEvent.builder("site-atteint", "Vous avez atteint le chantier")
                .introduction("Vous avez atteint le chantier.")
                .duree(dSite)
                .succes(EffetReussite.builder()
                        .evenementSuivant("commandement")
                        .build())
                .build());
        evenements.add(MissionEvent.builder("commandement", "Test de commandement")
                .introduction("Il faut mener les ouvriers du chantier d'une main de fer.")
                .test(new SimpleTest(Competence.COMMANDEMENT, 0))
                .succes(EffetReussite.builder()
                        .texteSimple("Les ouvriers ob\u00e9issent aux arrangeurs.")
                        .evenementSuivant("au-boulot")
                        .build())
                .echec(EffetEchec.builder()
                        .texteSimple("De nombreux ouvriers ont d\u00e9sert\u00e9, ce qui va retarder le travail "
                                + "et obliger les arrangeurs \u00e0 participer.")
                        .evenementSuivant("desertion")
                        .build())
                .build());
        evenements.add(MissionEvent.builder("desertion", "D\u00e9sertion des ouvriers")
                .introduction("De nombreux ouvriers ont d\u00e9sert\u00e9 le chantier.")
                .duree(dDesertion)
                .succes(EffetReussite.builder()
                        .evenementSuivant(travailDesertion.entree())
                        .build())
                .build());
        evenements.addAll(travailDesertion.evenements());
        evenements.addAll(travailSupplementaire.evenements());
        evenements.add(MissionEvent.builder("au-boulot", "Au boulot")
                .introduction("La neige commence \u00e0 tomber et le sol \u00e0 geler, "
                        + "rendant le travail encore plus \u00e9puisant.")
                .succes(EffetReussite.builder()
                        .evenementSuivant(travail.entree())
                        .build())
                .build());
        evenements.addAll(travail.evenements());
        evenements.add(MissionEvent.builder("surveillance-bandits", "Surveillance des environs")
                .introduction("Il faut sans cesse surveiller les environs pour pr\u00e9venir "
                        + "les attaques de bandits.")
                .succes(EffetReussite.builder()
                        .evenementSuivant("vigilance")
                        .build())
                .build());
        evenements.add(MissionEvent.builder("vigilance", "Test de vigilance")
                .introduction("Les arrangeurs scrutent les environs \u00e0 la recherche de bandits.")
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
        evenements.add(MissionEvent.builder("victoire", "Victoire")
                .effet("Le chantier a \u00e9t\u00e9 men\u00e9 \u00e0 son terme.")
                .succes(EffetReussite.builder()
                        .evenementSuivant(null)
                        .build())
                .build());

        return new Mission(
                "construction-chemin-de-fer",
                "Construction de chemin de fer",
                "Les villages de zaporogues dans les plaines d'Ukraine ont un besoin urgent de bois "
                        + "pour passer l'hiver mais les travaux n'en finissent pas. Allez mettre de l'ordre "
                        + "sur le chantier. Attention, les conditions sont rudes : pas d'\u00e9lectricit\u00e9, "
                        + "pas d'engins de chantier et la neige commence \u00e0 tomber. Il y a m\u00eame des "
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
                "Plusieurs meurtres et r\u00e8glements de compte ont eu lieu et la police semble "
                        + "impuissante. Un nomm\u00e9 Loreline a pris l'argent d'une bande de "
                        + "narcotrafiquants et des tueurs, dont le redoutable Chigur, sont \u00e0 sa "
                        + "poursuite. La mission n'est r\u00e9ussie que si Chigur et Loreline sont "
                        + "abattus ou arr\u00eat\u00e9s tous les deux.",
                Lieu.SAINT_DENIS,
                List.of(Competence.RAGOT, Competence.ELOQUENCE, Competence.EVALUATION,
                        Competence.TIR, Competence.DISCRETION),
                List.of(
                        MissionEvent.builder("enquete-de-quartier", "Interroger les t\u00e9moins")
                                .introduction("Plusieurs meurtres et r\u00e8glements de compte ont eu "
                                        + "lieu dans le quartier et la police semble impuissante. "
                                        + "Les arrangeurs interrogent discr\u00e8tement les t\u00e9moins.")
                                .duree(d.enqueteTemoins())
                                .test(new SimpleTest(Competence.RAGOT, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Un nomm\u00e9 Loreline a pris l'argent d'une bande "
                                                + "de narcotrafiquants et des tueurs sont lanc\u00e9s "
                                                + "\u00e0 sa poursuite.")
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Personne ne veut parler. Recommencer \u00e0 "
                                                + "\"Interroger les t\u00e9moins\".")
                                        .evenementSuivant("enquete-de-quartier")
                                        .build())
                                .build(),
                        MissionEvent.builder("interroger-la-femme", "Interrogatoire de sa femme")
                                .introduction("La femme de Loreline pourrait r\u00e9v\u00e9ler o\u00f9 il se "
                                        + "terre.")
                                .duree(d.interrogationFemme())
                                .test(new SimpleTest(Competence.ELOQUENCE, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Elle vous met sur la piste de son mari pour "
                                                + "le prot\u00e9ger des tueurs.")
                                        .evenementSuivant("localiser-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Elle se mure dans le silence : il ne reste "
                                                + "plus qu'\u00e0 tenter de retrouver les tueurs.")
                                        .evenementSuivant("piste-du-tueur-1")
                                        .build())
                                .build(),
                        MissionEvent.builder("localiser-lorelime", "Retrouver Loreline")
                                .introduction("La piste m\u00e8ne \u00e0 Loreline, l'homme qui a pris "
                                        + "l'argent du cartel.")
                                .duree(d.recherche())
                                .test(new SimpleTest(Competence.EVALUATION, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez Loreline et il a l'argent "
                                                + "avec lui. Une vraie fortune.")
                                        .evenementSuivant("la-tentation-du-butin")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline est introuvable. Recommencer \u00e0 "
                                                + "\"Retrouver Loreline\".")
                                        .evenementSuivant("localiser-lorelime")
                                        .build())
                                .build(),
                        MissionEvent.builder("la-tentation-du-butin", "Une vraie fortune")
                                .introduction("Loreline et sa fortune sont \u00e0 port\u00e9e de main.")
                                .duree(Duration.ZERO)
                                .branche("Un arrangeur cupide ou impulsif n'arrive pas \u00e0 quitter "
                                        + "la mallette des yeux.",
                                        equipe -> equipe.stream().anyMatch(Missions::estCupideOuImpulsif),
                                        "resister-a-la-tentation")
                                .branche("Les arrangeurs tentent de convaincre Loreline de se "
                                        + "rendre.",
                                        equipe -> true,
                                        "convaincre-lorelime")
                                .build(),
                        MissionEvent.builder("resister-a-la-tentation", "R\u00e9sister \u00e0 la tentation")
                                .introduction("Il faut tenir bon face \u00e0 un pareil butin.")
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("L'envie de s'emparer de l'argent est "
                                                + "ma\u00eetris\u00e9e : les arrangeurs tendent la main \u00e0 "
                                                + "Loreline.")
                                        .evenementSuivant("convaincre-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs c\u00e8dent \u00e0 la tentation : ils "
                                                + "d\u00e9cident de voler l'argent.")
                                        .evenementSuivant("voler-le-butin")
                                        .build())
                                .build(),
                        MissionEvent.builder("voler-le-butin", "Voler l'argent")
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
                                                + "hasard subit 2D10 de d\u00e9g\u00e2ts. Recommencer \u00e0 "
                                                + "\"Voler l'argent\".")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("voler-le-butin")
                                        .build())
                                .build(),
                        MissionEvent.builder("convaincre-lorelime", "Convaincre Loreline")
                                .introduction("Se rendre est la seule chance de survie de "
                                        + "Loreline : les arrangeurs le lui expliquent.")
                                .test(new SimpleTest(Competence.ELOQUENCE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Loreline se rend et vous avez r\u00e9cup\u00e9r\u00e9 "
                                                + "l'argent du cartel.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline ne se laissera pas faire sans "
                                                + "combattre.")
                                        .evenementSuivant("la-riposte-de-lorelime")
                                        .build())
                                .build(),
                        MissionEvent.builder("la-riposte-de-lorelime", "Riposte de Loreline")
                                .introduction("Loreline d\u00e9gaine et tire avant de tenter de fuir.")
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous le couvrez avant qu'il n'atteigne la "
                                                + "porte : Loreline se rend et vous avez r\u00e9cup\u00e9r\u00e9 "
                                                + "l'argent du cartel.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline s'enfuit ! Un personnage au hasard "
                                                + "est bless\u00e9 et subit 2D10 de d\u00e9g\u00e2ts. Il faut "
                                                + "retrouver les tueurs qui le traquent.")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("piste-du-tueur-1")
                                        .build())
                                .build(),
                        MissionEvent.builder("poursuivis-par-chigur", "Poursuivis par le tueur")
                                .introduction("Le cartel a lanc\u00e9 son tueur \u00e0 vos trousses : "
                                        + "Chigur traque d\u00e9sormais les arrangeurs et leur argent.")
                                .duree(d.poursuite())
                                .succes(EffetReussite.builder()
                                        .texteSimple("Apr\u00e8s un jour de fuite, les arrangeurs "
                                                + "tentent de semer le tueur.")
                                        .evenementSuivant("rester-discrets")
                                        .build())
                                .build(),
                        MissionEvent.builder("rester-discrets", "Se cacher du tueur")
                                .introduction("Chigur est aux aguets : chaque arrangeur tente "
                                        + "de rester discret.")
                                .test(new SimpleTest(Competence.DISCRETION, -10))
                                .testPourTous()
                                .succes(EffetReussite.builder()
                                        .texteSimple("Chigur a perdu la trace des arrangeurs.")
                                        .evenementSuivant("fin-de-la-traque")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le tueur a rep\u00e9r\u00e9 les arrangeurs.")
                                        .evenementSuivant("attaque-de-chigur")
                                        .build())
                                .build(),
                        MissionEvent.builder("fin-de-la-traque", "Fin de la traque")
                                .introduction("Les arrangeurs ont sem\u00e9 le tueur\u2026 mais celui-ci "
                                        + "a surv\u00e9cu.")
                                .duree(Duration.ZERO)
                                .effet("Le tueur a surv\u00e9cu : les arrangeurs ont \u00e9chou\u00e9, m\u00eame "
                                        + "s'ils repartent avec l'argent du cartel (+10 "
                                        + "Richesse).")
                                .build(),
                        MissionEvent.builder("attaque-de-chigur", "Attaque de Chigur")
                                .introduction("Chigur surgit de nulle part et ouvre le feu.")
                                .duree(d.tir())
                                .test(new SimpleTest(Competence.TIR, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs abattent Chigur. Loreline "
                                                + "est d\u00e9j\u00e0 neutralis\u00e9 : la mission est "
                                                + "accomplie.")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("L'arrangeur attaqu\u00e9 subit 3D10 de d\u00e9g\u00e2ts. "
                                                + "Le tueur reprend la traque.")
                                        .degatsAleatoires(3, 10)
                                        .evenementSuivant("poursuivis-par-chigur")
                                        .build())
                                .build(),
                        MissionEvent.builder("piste-du-tueur-1", "Piste du tueur")
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
                        MissionEvent.builder("piste-du-tueur-2", "La piste se refroidit")
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
                                        .texteSimple("Chigur est encore pass\u00e9 par l\u00e0, mais la "
                                                + "chance de le retrouver s'amenuise.")
                                        .evenementSuivant("piste-du-tueur-3")
                                        .build())
                                .build(),
                        MissionEvent.builder("piste-du-tueur-3", "Derni\u00e8re piste")
                                .introduction("C'est la derni\u00e8re chance de remonter jusqu'au "
                                        + "tueur Chigur.")
                                .duree(d.traqueTueur())
                                .test(new SimpleTest(Competence.EVALUATION, -20))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous retrouvez le principal et il attaque "
                                                + "aussit\u00f4t.")
                                        .evenementSuivant("confrontation-avec-chigur")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Le tueur s'est volatilis\u00e9 dans la nature.")
                                        .evenementSuivant("chigur-echappe")
                                        .build())
                                .build(),
                        MissionEvent.builder("chigur-echappe", "Chigur vous a \u00e9chapp\u00e9")
                                .introduction("Le tueur et Loreline ont disparu sans laisser de "
                                        + "traces.")
                                .duree(Duration.ZERO)
                                .effet("Plus rien \u00e0 faire : Chigur vous a \u00e9chapp\u00e9, la mission "
                                        + "\u00e9choue.")
                                .build(),
                        MissionEvent.builder("confrontation-avec-chigur", "Confrontation avec "
                                + "Chigur")
                                .introduction("Vous avez retrouv\u00e9 le tueur et il ne compte pas "
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
                        MissionEvent.builder("la-fusillade-de-chigur", "Fusillade de Chigur")
                                .introduction("Le tueur canarde tout ce qui bouge : chaque "
                                        + "arrangeur tente sa chance.")
                                .test(new SimpleTest(Competence.CHANCE, 0))
                                .testPourTous()
                                .succes(EffetReussite.builder()
                                        .texteSimple("Le tueur vous a \u00e9chapp\u00e9 pour cette fois.")
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Celui qui rate son test de chance subit "
                                                + "3D10 de d\u00e9g\u00e2ts.")
                                        .degatsAleatoires(3, 10)
                                        .evenementSuivant("interroger-la-femme")
                                        .build())
                                .build(),
                        MissionEvent.builder("chigur-abattu", "Chigur est abattu")
                                .introduction("Le tueur du cartel g\u00eet au sol.")
                                .duree(Duration.ZERO)
                                .effet("Le tueur Chigur est neutralis\u00e9. Reste \u00e0 retrouver "
                                        + "Loreline et l'argent pour accomplir la mission.")
                                .succes(EffetReussite.builder()
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        MissionEvent.builder("retrouver-le-fugitif", "Retrouver le fugitif")
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
                                        .texteSimple("Loreline est introuvable. Recommencer \u00e0 "
                                                + "\"Retrouver le fugitif\".")
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        MissionEvent.builder("le-butin-sans-menace", "Le butin sans menace")
                                .introduction("Plus personne ne menace les arrangeurs : il ne "
                                        + "reste qu'\u00e0 d\u00e9cider du sort de Loreline.")
                                .duree(Duration.ZERO)
                                .branche("Un arrangeur cupide ou impulsif n'arrive pas \u00e0 quitter "
                                        + "la mallette des yeux.",
                                        equipe -> equipe.stream().anyMatch(Missions::estCupideOuImpulsif),
                                        "resister-malgre-le-butin")
                                .branche("Les arrangeurs tentent d'amener Loreline \u00e0 se rendre.",
                                        equipe -> true,
                                        "sommation-a-lorelime")
                                .build(),
                        MissionEvent.builder("resister-malgre-le-butin", "R\u00e9sister malgr\u00e9 le "
                                + "butin")
                                .introduction("Il faut tenir bon face \u00e0 un pareil butin.")
                                .test(new SimpleTest(Competence.FORCE_MENTALE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("L'envie de s'emparer de l'argent est "
                                                + "ma\u00eetris\u00e9e : les arrangeurs tendent la main \u00e0 "
                                                + "Loreline.")
                                        .evenementSuivant("sommation-a-lorelime")
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Les arrangeurs c\u00e8dent \u00e0 la tentation : ils "
                                                + "d\u00e9cident de voler l'argent.")
                                        .evenementSuivant("abattre-le-fugitif")
                                        .build())
                                .build(),
                        MissionEvent.builder("sommation-a-lorelime", "Sommation \u00e0 Loreline")
                                .introduction("Chigur est mort : les arrangeurs somment Loreline "
                                        + "de rendre l'argent et de se rendre.")
                                .test(new SimpleTest(Competence.ELOQUENCE, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Loreline se rend et vous avez r\u00e9cup\u00e9r\u00e9 "
                                                + "l'argent du cartel. Chigur et Loreline sont "
                                                + "neutralis\u00e9s : victoire.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline ne se laissera pas faire sans "
                                                + "combattre.")
                                        .evenementSuivant("la-riposte-du-fugitif")
                                        .build())
                                .build(),
                        MissionEvent.builder("la-riposte-du-fugitif", "Riposte du fugitif")
                                .introduction("Loreline d\u00e9gaine et tire avant de tenter de fuir.")
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Vous le couvrez avant qu'il n'atteigne la "
                                                + "porte : Loreline se rend et vous avez r\u00e9cup\u00e9r\u00e9 "
                                                + "l'argent du cartel. Chigur et Loreline sont "
                                                + "neutralis\u00e9s : victoire.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("Loreline s'enfuit ! Un personnage au "
                                                + "hasard est bless\u00e9 et subit 2D10 de d\u00e9g\u00e2ts.")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("retrouver-le-fugitif")
                                        .build())
                                .build(),
                        MissionEvent.builder("abattre-le-fugitif", "Abattre le fugitif")
                                .introduction("Les arrangeurs d\u00e9cident d'abattre Loreline pour "
                                        + "prendre l'argent.")
                                .duree(d.vol())
                                .test(new SimpleTest(Competence.TIR, 0))
                                .succes(EffetReussite.builder()
                                        .texteSimple("Les arrangeurs abattent Loreline et "
                                                + "s'emparent de l'argent du cartel. Chigur et "
                                                + "Loreline sont neutralis\u00e9s : victoire.")
                                        .recompense("L'argent du cartel (+10 Richesse)")
                                        .evenementSuivant(null)
                                        .build())
                                .echec(EffetEchec.builder()
                                        .texteSimple("La fusillade tourne mal : un personnage au "
                                                + "hasard subit 2D10 de d\u00e9g\u00e2ts. Recommencer \u00e0 "
                                                + "\"Abattre le fugitif\".")
                                        .degatsAleatoiresMembreAuHasard(2, 10)
                                        .evenementSuivant("abattre-le-fugitif")
                                        .build())
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
