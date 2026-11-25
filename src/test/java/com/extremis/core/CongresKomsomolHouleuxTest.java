package com.extremis.core;

import com.extremis.catalog.Missions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la mission "Congrès Komsomol houleux" (wiki, page du même
 * nom) : la prise de température est un test de ragot aidé (+10 par
 * autre arrangeur) et sa réussite apporte un +20 aux tests suivants.
 */
class CongresKomsomolHouleuxTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void leTestDeRagotEstAideParLesAutresArrangeurs() {
        Character alice = agent();
        Character bob = agent("b", "Bob");
        Character carole = agent("c", "Carole");
        Mission mission = Missions.congresKomsomolHouleuxRapide();
        MissionExecution execution = MissionExecution.start(mission,
                List.of(alice, bob, carole), script(1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        // 50 de ragot + 30 de base + 10 par coéquipier = cible 100
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("(cible 100 sur D100)"));
        // la réussite de la prise de température baisse la difficulté
        // des trois tests suivants
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("La difficulté du test de l'événement discours sera baissée de 20."));
    }

    @Test
    void laReussiteDeLaPriseDeTemperatureBaisseLaDifficulteDuDiscours() {
        Character agent = agent();
        // le ragot réussit : le discours est testé à 50 - 10 + 20 = 60
        Mission mission = Missions.congresKomsomolHouleuxRapide();
        MissionExecution execution = MissionExecution.start(mission,
                List.of(agent), script(1, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Vous avez réussi à calmer le jeu"));
    }

    @Test
    void lechecDuDiscoursPeutEtreRattrapeParLeStatutDEnvoyeDuConsul() {
        Character alice = agent();
        Character bob = agent("b", "Bob");
        // prise de température et influence (aidée) réussissent,
        // seul le discours échoue : victoire
        Mission mission = Missions.congresKomsomolHouleuxRapide();
        MissionExecution execution = MissionExecution.start(mission,
                List.of(alice, bob), script(1, 100, 1), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isReussie()).isTrue();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Votre statut d’envoyé du consul les impressionne"));
    }

    @Test
    void lechecDeTousLesTestsProvoqueLaBagarreGeneralePuisLEchec() {
        Character alice = agent();
        Character bob = agent("b", "Bob");
        Character carole = agent("c", "Carole");
        // tous les tests échouent : chaque arrangeur rate la bagarre
        // (test pour chacun, 1D10 de dégâts) et le congrès échoue
        Mission mission = Missions.congresKomsomolHouleuxRapide();
        MissionExecution execution = MissionExecution.start(mission,
                List.of(alice, bob, carole), script(100, 100, 100, 100, 5, 100, 5, 100, 5), START);

        assertThat(execution.advance(START.plus(Duration.ofDays(1)))).isTrue();
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.isReussie()).isFalse();
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Cet arrangeur prend des coups."));
        assertThat(textesDuJournal(execution).stream()
                .filter(l -> l.contains("subit 1D10 degats")).count()).isEqualTo(3);
        assertThat(textesDuJournal(execution))
                .anyMatch(l -> l.contains("Il y a eu de nombreux blessés et le congrès est un échec complet."));
    }

    private Character agent() {
        return agent("a", "Alice");
    }

    private Character agent(String id, String nom) {
        return new Character(id, nom)
                .withSkill(Competence.RAGOT, 50)
                .withSkill(Competence.ELOQUENCE, 50)
                .withSkill(Competence.INTIMIDATION, 50)
                .withSkill(Competence.BAGARRE, 50);
    }

    private static RandomSource script(int... jets) {
        return new RandomSource() {
            private int i = 0;

            @Override
            public int roll() {
                return jets[Math.min(i++, jets.length - 1)];
            }
        };
    }

    private static List<String> textesDuJournal(MissionExecution execution) {
        return execution.logLines().stream().map(LigneJournal::texte).toList();
    }
}
