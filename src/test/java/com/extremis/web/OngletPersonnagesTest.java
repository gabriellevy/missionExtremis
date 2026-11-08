package com.extremis.web;

import com.extremis.db.CharacterEntity;
import com.extremis.db.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=3600000",
        "spring.profiles.active=test-rapide"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class OngletPersonnagesTest {

    @Autowired
    private GameService game;

    @Autowired
    private CharacterRepository personnages;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void laPagePersonnagesAfficheLesDeuxListes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .param("onglet", "personnages"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Personnages recrut&#233;s")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Personnages recrutables")));
    }

    @Test
    void lesRecrutablesSontCliquablesCommeLesRecrutesEtSansCoterie() throws Exception {
        CharacterEntity recrutable = game.availableCharacters().get(0);

        String html = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .param("onglet", "personnages"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-nom=\"" + recrutable.getName() + "\"");
        assertThat(html).doesNotContain(recrutable.getName() + " (" + recrutable.getCoterie() + ")");
    }

    @Test
    @Transactional
    void apresReinitialisationUsineTroisPersonnagesSontRecrutesAuHasard() {
        game.reinitialiserUsine();

        assertThat(personnages.findAll()).hasSize(10);
        assertThat(personnages.findByRecruteTrue()).hasSize(3);
        assertThat(personnages.findByRecruteFalse()).hasSize(7);
        assertThat(game.roster()).hasSize(3);
        assertThat(game.availableCharacters()).hasSize(7).allMatch(c -> !c.isRecrute());
    }

    @Test
    @Transactional
    void recruterRetireLePersonnageDeLaListeDesRecrutables() {
        CharacterEntity cible = personnages.findByRecruteFalse().get(0);
        String id = cible.getId();

        game.recruit(id, Instant.now());

        assertThat(personnages.findById(id).orElseThrow().isRecrute()).isTrue();
        assertThat(game.availableCharacters()).noneMatch(c -> c.getId().equals(id));
        assertThat(game.roster()).anyMatch(c -> c.id().equals(id));
    }

    @Test
    @Transactional
    void recruterDeuxFoisEnVingtQuatreHeuresEstRefuse() {
        Instant maintenant = Instant.now();
        game.recruit(personnages.findByRecruteFalse().get(0).getId(), maintenant);
        String autre = personnages.findByRecruteFalse().get(0).getId();

        assertThatThrownBy(() -> game.recruit(autre, maintenant.plusSeconds(3600)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(personnages.findById(autre).orElseThrow().isRecrute()).isFalse();
        assertThat(game.peutRecruter(maintenant.plusSeconds(3600))).isFalse();
        assertThat(game.peutRecruter(maintenant.plusSeconds(24 * 3600))).isTrue();
    }

    @Test
    @Transactional
    void laListeDesRecrutablesPeutDevenirVide() {
        personnages.findAll().forEach(c -> {
            c.setRecrute(true);
            personnages.save(c);
        });

        assertThat(game.availableCharacters()).isEmpty();
        assertThat(game.roster()).hasSize((int) personnages.count());
    }
}
