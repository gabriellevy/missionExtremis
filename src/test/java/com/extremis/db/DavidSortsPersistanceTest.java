package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que David (qui remplace Arsene Lupin en seed-05) est bien
 * semé avec ses sorts, et que les sorts survivent a l'aller-retour
 * entite -> modele -> entite.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test-rapide"
})
class DavidSortsPersistanceTest {

    @Autowired
    private CharacterRepository repository;

    @Test
    void davidEstSeedAvecSesQuatreSorts() {
        CharacterEntity david = repository.findById("seed-05").orElseThrow();
        assertThat(david.getName()).isEqualTo("David");
        assertThat(david.getCoterie()).isEqualTo("Achéron");
        assertThat(david.getCarriere()).isEqualTo("Lycéen");
        assertThat(david.getSkillBase()).isEqualTo(33);
        assertThat(david.getSkills().stream()
                .filter(s -> s.getSkill() == Competence.VOLONTE)
                .findFirst().orElseThrow().getValue()).isEqualTo(16);
        assertThat(david.getVitaliteDepart()).isEqualTo(15);
        assertThat(david.getSangFroidDepart()).isEqualTo(18);
        assertThat(david.getMaitrises()).extracting(CharacterMaitriseEntity::getNom)
                .containsExactly("Mentaliste");
        assertThat(david.getSorts()).extracting(CharacterSortEntity::getNom)
                .containsExactly("Lecture de pensées", "Connaissance du temps écoulé",
                        "Influence insidieuse", "Domination");
    }

    @Test
    void lesSortsSurviventAuMappingEntiteModeleEntite() {
        CharacterEntity david = repository.findById("seed-05").orElseThrow();
        Character core = EntityMapper.toCore(david);
        assertThat(core.sorts()).containsExactly("Lecture de pensées", "Connaissance du temps écoulé",
                "Influence insidieuse", "Domination");

        CharacterEntity rechargee = EntityMapper.toEntity(core, david.getCoterie(), david.getCarriere());
        List<String> sorts = rechargee.getSorts().stream().map(CharacterSortEntity::getNom).toList();
        assertThat(sorts).containsExactly("Lecture de pensées", "Connaissance du temps écoulé",
                "Influence insidieuse", "Domination");
    }
}
