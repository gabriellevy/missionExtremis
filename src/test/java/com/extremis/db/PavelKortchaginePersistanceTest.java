package com.extremis.db;

import com.extremis.core.Competence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que Pavel Kortchagine (qui remplace Welf Schwarzschutze en seed-10) est bien
 * semé avec ses competences, ses vices et vertus, sa maitrise et ses possessions.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=test-rapide"
})
@Transactional
class PavelKortchaginePersistanceTest {

    @Autowired
    private CharacterRepository repository;

    @Test
    void pavelKortchagineRemplaceWelfSchwarzschutzeEnSeed10() {
        CharacterEntity pavel = repository.findById("seed-10").orElseThrow();
        assertThat(pavel.getName()).isEqualTo("Pavel Kortchagine");
        assertThat(pavel.getCoterie()).isEqualTo("jacobin");
        assertThat(pavel.getCarriere()).isEqualTo("Lyc\u00e9en");
        assertThat(pavel.getSkillBase()).isEqualTo(30);
        assertThat(pavel.getSkills()).hasSize(9);
        assertThat(valeur(pavel, Competence.ANIMAUX)).isEqualTo(-5);
        assertThat(valeur(pavel, Competence.CHANCE)).isEqualTo(30);
        assertThat(valeur(pavel, Competence.COMMANDEMENT)).isEqualTo(10);
        assertThat(valeur(pavel, Competence.ELOQUENCE)).isEqualTo(10);
        assertThat(valeur(pavel, Competence.INTELLIGENCE)).isEqualTo(1);
        assertThat(valeur(pavel, Competence.INTIMIDATION)).isEqualTo(5);
        assertThat(valeur(pavel, Competence.MARCHANDAGE)).isEqualTo(-5);
        assertThat(valeur(pavel, Competence.VIGILANCE)).isEqualTo(5);
        assertThat(valeur(pavel, Competence.VOLONTE)).isEqualTo(-10);
        assertThat(pavel.getTraits()).extracting(CharacterTraitEntity::getName)
                .containsExactly("Loyal", "M\u00e9fiant");
        assertThat(pavel.getMaitrises()).extracting(CharacterMaitriseEntity::getNom)
                .containsExactly("Accord\u00e9on");
        assertThat(pavel.getVitaliteDepart()).isEqualTo(18);
        assertThat(pavel.getVitalite()).isEqualTo(18);
        assertThat(pavel.getSangFroidDepart()).isEqualTo(18);
        assertThat(pavel.getSangFroid()).isEqualTo(18);
        assertThat(pavel.getInventory()).extracting(CharacterItemEntity::getLabel)
                .containsExactly("Accord\u00e9on", "Pistolet");
        assertThat(repository.findAll()).noneMatch(c -> c.getName().equals("Welf Schwarzschutze"));
    }

    private static int valeur(CharacterEntity c, Competence competence) {
        return c.getSkills().stream()
                .filter(s -> s.getSkill() == competence)
                .findFirst().orElseThrow().getValue();
    }
}
