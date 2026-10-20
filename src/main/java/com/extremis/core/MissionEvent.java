package com.extremis.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Un evenement de mission (wiki, page "Missions") : une succession
 * d'evenements dont les reussites ou echecs menent a d'autres evenements.
 *
 * Chaque evenement a une duree avant d'etre realise. Au debut son texte
 * d'introduction s'affiche, et seulement une fois la duree ecoulee son
 * effet s'affiche, y compris les resultats des tests de caracteristiques.
 */
public final class MissionEvent {
    private final String id;
    private final String titre;
    private final String introduction;
    private final String effet;
    private final Duration duree;
    private final SkillTest test;
    private final EffetReussite succes;
    private final EffetEchec echec;

    public record EffetReussite(
            String texteCritique,
            String texteSimple,
            String recompense,
            String evenementSuivant) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String texteCritique;
            private String texteSimple;
            private String recompense;
            private String evenementSuivant;

            public Builder texteCritique(String texte) {
                this.texteCritique = texte;
                return this;
            }

            public Builder texteSimple(String texte) {
                this.texteSimple = texte;
                return this;
            }

            public Builder recompense(String recompense) {
                this.recompense = recompense;
                return this;
            }

            public Builder evenementSuivant(String id) {
                this.evenementSuivant = id;
                return this;
            }

            public EffetReussite build() {
                return new EffetReussite(texteCritique, texteSimple, recompense, evenementSuivant);
            }
        }
    }

    public record EffetEchec(
            String texteSimple,
            String texteCritique,
            Integer degats,
            Integer perteSangFroid,
            boolean exclureJoueur,
            Integer ajustementDifficulte,
            String evenementAjuste,
            String evenementSuivant) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String texteSimple;
            private String texteCritique;
            private Integer degats;
            private Integer perteSangFroid;
            private boolean exclureJoueur;
            private Integer ajustementDifficulte;
            private String evenementAjuste;
            private String evenementSuivant;

            public Builder texteSimple(String texte) {
                this.texteSimple = texte;
                return this;
            }

            public Builder texteCritique(String texte) {
                this.texteCritique = texte;
                return this;
            }

            public Builder degats(int degats) {
                this.degats = degats;
                return this;
            }

            public Builder perteSangFroid(int perte) {
                this.perteSangFroid = perte;
                return this;
            }

            public Builder exclureJoueur() {
                this.exclureJoueur = true;
                return this;
            }

            public Builder ajustementDifficulte(int ajustement) {
                this.ajustementDifficulte = ajustement;
                return this;
            }

            public Builder evenementSuivant(String id) {
                this.evenementSuivant = id;
                return this;
            }

            public Builder evenementAjuste(String id) {
                this.evenementAjuste = id;
                return this;
            }

            public EffetEchec build() {
                return new EffetEchec(texteSimple, texteCritique, degats, perteSangFroid,
                        exclureJoueur, ajustementDifficulte, evenementAjuste, evenementSuivant);
            }
        }
    }

    private MissionEvent(Builder builder) {
        this.id = builder.id;
        this.titre = builder.titre;
        this.introduction = builder.introduction;
        this.effet = builder.effet;
        this.duree = builder.duree;
        this.test = builder.test;
        this.succes = builder.succes;
        this.echec = builder.echec;
    }

    public String id() { return id; }
    public String titre() { return titre; }
    public String introduction() { return introduction; }
    public String effet() { return effet; }
    public Duration duree() { return duree; }
    public Optional<SkillTest> test() { return Optional.ofNullable(test); }
    public Optional<EffetReussite> succes() { return Optional.ofNullable(succes); }
    public Optional<EffetEchec> echec() { return Optional.ofNullable(echec); }

    public static Builder builder(String id, String titre) {
        return new Builder(id, titre);
    }

    public static final class Builder {
        private final String id;
        private final String titre;
        private String introduction;
        private String effet;
        private Duration duree = Duration.ZERO;
        private SkillTest test;
        private EffetReussite succes;
        private EffetEchec echec;

        private Builder(String id, String titre) {
            this.id = id;
            this.titre = titre;
        }

        public Builder introduction(String introduction) {
            this.introduction = introduction;
            return this;
        }

        public Builder effet(String effet) {
            this.effet = effet;
            return this;
        }

        public Builder duree(Duration duree) {
            this.duree = duree;
            return this;
        }

        public Builder test(SkillTest test) {
            this.test = test;
            return this;
        }

        public Builder succes(EffetReussite succes) {
            this.succes = succes;
            return this;
        }

        public Builder echec(EffetEchec echec) {
            this.echec = echec;
            return this;
        }

        public MissionEvent build() {
            return new MissionEvent(this);
        }
    }
}
