package com.extremis.web;

import com.extremis.core.Mission;
import com.extremis.db.CharacterEntity;
import com.extremis.db.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=3600000",
        "spring.profiles.active=test-rapide"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class CarteMissionsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameService game;

    @Autowired
    private CharacterRepository personnages;

    @Autowired
    private List<Mission> missions;

    private String htmlOngletMissions() throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .param("onglet", "missions"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void lOngletMissionsAfficheUneCarteZoomableEtDesplaceable() throws Exception {
        String html = htmlOngletMissions();

        assertThat(html).contains("id=\"carte\"");
        assertThat(html).contains("id=\"carte-plan\"");
        assertThat(html).contains("id=\"carte-zoom-avant\"");
        assertThat(html).contains("id=\"carte-zoom-arriere\"");
        assertThat(html).contains("id=\"carte-reinitialiser\"");
        assertThat(html).contains("carte.addEventListener('wheel'");
    }

    @Test
    @Transactional
    void laCarteAfficheUnMarqueurParPersonnageDansSonLieu() throws Exception {
        CharacterEntity personnage = personnages.findAll().get(0);

        String html = htmlOngletMissions();

        assertThat(personnage.getLieu()).isNotNull();
        assertThat(html).contains("class=\"marqueur");
        assertThat(html).contains("left:" + personnage.getLieu().carteX() + "px; top:" + personnage.getLieu().carteY() + "px");
    }

    @Test
    void laCarteAfficheLesMissionsDisponiblesCommeMarqueursSelectionnables() throws Exception {
        Mission mission = missions.get(0);

        String html = htmlOngletMissions();

        assertThat(html).contains("class=\"marqueur mission\"");
        assertThat(html).contains("data-mission=\"" + mission.id() + "\"");
        assertThat(html).contains("left:" + mission.lieu().carteX() + "px; top:" + (mission.lieu().carteY() - 24) + "px");
    }

    @Test
    void lesCartesDeMissionPortentLeurIdentifiantPourLaSelectionDepuisLaCarte() throws Exception {
        String html = htmlOngletMissions();

        for (Mission mission : missions) {
            assertThat(html).contains("class=\"mission-carte\" data-mission=\"" + mission.id() + "\"");
        }
    }

    @Test
    @Transactional
    void unMarqueurRecruteEstDistingueDUnMarqueurRecrutable() throws Exception {
        CharacterEntity recrute = personnages.findAll().stream()
                .filter(CharacterEntity::isRecrute)
                .findFirst()
                .orElseGet(() -> {
                    CharacterEntity cible = personnages.findByRecruteFalse().get(0);
                    game.recruit(cible.getId(), java.time.Instant.now());
                    return cible;
                });

        String html = htmlOngletMissions();

        assertThat(html).contains("personnage-recrute");
        assertThat(html).contains("personnage-recrutable");
    }
}
