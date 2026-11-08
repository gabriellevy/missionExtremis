package com.extremis.db;

import com.extremis.core.Character;
import com.extremis.core.Competence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que Lune (qui remplace Albios le barde en seed-06) est bien
 * semee avec ses sorts de l'air, et que les sorts survivent a l'aller-retour
 * entite -> modele -> entite.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test-rapide"
})
@Transactional
class LuneSortsPersistanceTest {

    @Autowired
    private CharacterRepository repository;

    @Test
    void luneEstSeedeeAvecSesTroisSorts() {
        CharacterEntity lune = repository.findById("seed-06").orElseThrow();
        assertThat(lune.getName()).isEqualTo("Lune");
        assertThat(lune.getCoterie()).isEqualTo("F\u00e9riques");
        assertThat(lune.getCarriere()).isEqualTo("mendiante");
        assertThat(lune.getSkillBase()).isEqualTo(24);
        assertThat(lune.getSkills().stream()
                .filter(s -> s.getSkill() == Competence.VOLONTE)
                .findFirst().orElseThrow().getValue()).isEqualTo(20);
        assertThat(lune.getSkills().stream()
                .filter(s -> s.getSkill() == Competence.RICHESSE)
                .findFirst().orElseThrow().getValue()).isEqualTo(-30);
        assertThat(lune.getVitaliteDepart()).isEqualTo(12);
        assertThat(lune.getSangFroidDepart()).isEqualTo(14);
        assertThat(lune.getSpecialites()).extracting(CharacterSpecialiteEntity::getNom)
                .containsExactly("Tromperie + 10");
        assertThat(lune.getSorts()).extracting(CharacterSortEntity::getNom)
                .containsExactly("Brise l\u00e9g\u00e8re", "Sons et silence", "Passe-muraille");
    }

    @Test
    void lesSortsSurviventAuMappingEntiteModeleEntite() {
        CharacterEntity lune = repository.findById("seed-06").orElseThrow();
        Character core = EntityMapper.toCore(lune);
        assertThat(core.sorts()).containsExactly("Brise l\u00e9g\u00e8re", "Sons et silence", "Passe-muraille");

        CharacterEntity rechargee = EntityMapper.toEntity(core, lune.getCoterie(), lune.getCarriere());
        List<String> sorts = rechargee.getSorts().stream().map(CharacterSortEntity::getNom).toList();
        assertThat(sorts).containsExactly("Brise l\u00e9g\u00e8re", "Sons et silence", "Passe-muraille");
    }
}
