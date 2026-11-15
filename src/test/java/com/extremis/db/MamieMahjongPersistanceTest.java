package com.extremis.db;

import com.extremis.core.Competence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que Mamie mahjong (qui remplace Odysseus en seed-08) est bien
 * semee avec ses competences, ses vices et ses possessions.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test-rapide"
})
@Transactional
class MamieMahjongPersistanceTest {

    @Autowired
    private CharacterRepository repository;

    @Test
    void mamieMahjongRemplaceOdysseusEnSeed08() {
        CharacterEntity mamie = repository.findById("seed-08").orElseThrow();
        assertThat(mamie.getName()).isEqualTo("Mamie mahjong");
        assertThat(mamie.getCoterie()).isEqualTo("Lotus blanc");
        assertThat(mamie.getCarriere()).isEqualTo("receleuse");
        assertThat(mamie.getSkillBase()).isEqualTo(30);
        assertThat(mamie.getSkills()).hasSize(9);
        assertThat(valeur(mamie, Competence.BAGARRE)).isEqualTo(10);
        assertThat(valeur(mamie, Competence.CHANCE)).isEqualTo(-5);
        assertThat(valeur(mamie, Competence.DISCRETION)).isEqualTo(20);
        assertThat(valeur(mamie, Competence.HABILETE)).isEqualTo(-3);
        assertThat(valeur(mamie, Competence.INTIMIDATION)).isEqualTo(3);
        assertThat(valeur(mamie, Competence.MARCHANDAGE)).isEqualTo(20);
        assertThat(valeur(mamie, Competence.RAGOT)).isEqualTo(20);
        assertThat(valeur(mamie, Competence.RICHESSE)).isEqualTo(-3);
        assertThat(valeur(mamie, Competence.VOLONTE)).isEqualTo(5);
        assertThat(mamie.getTraits()).extracting(CharacterTraitEntity::getName)
                .containsExactly("Gourmand", "Cupide", "M\u00e9fiant");
        assertThat(mamie.getVitaliteDepart()).isEqualTo(14);
        assertThat(mamie.getVitalite()).isEqualTo(14);
        assertThat(mamie.getSangFroidDepart()).isEqualTo(14);
        assertThat(mamie.getSangFroid()).isEqualTo(14);
        assertThat(mamie.getInventory()).extracting(CharacterItemEntity::getLabel)
                .containsExactly("Bijoux de grande valeur",
                        "Perruques de tous types avec \u00e9pingles \u00e0 cheveux de tous types");
        assertThat(repository.findAll()).noneMatch(c -> c.getName().equals("Odysseus"));
    }

    private static int valeur(CharacterEntity c, Competence competence) {
        return c.getSkills().stream()
                .filter(s -> s.getSkill() == competence)
                .findFirst().orElseThrow().getValue();
    }
}
