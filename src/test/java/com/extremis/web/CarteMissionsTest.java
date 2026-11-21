package com.extremis.web;

import com.extremis.core.Lieu;
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
    void lOngletMissionsAfficheUneCarteDuMondeZoomableAuClavier() throws Exception {
        String html = htmlOngletMissions();

        assertThat(html).contains("id=\"carte\"");
        assertThat(html).contains("unpkg.com/leaflet@1.9.4/dist/leaflet.js");
        assertThat(html).contains("tile.openstreetmap.org/{z}/{x}/{y}.png");
        assertThat(html).contains("L.map('carte'");
        assertThat(html).contains("carte.zoomIn(pas)");
        assertThat(html).contains("carte.zoomOut(pas)");
        assertThat(html).contains("evenement.shiftKey");
    }

    @Test
    void laVueInitialeDeLaCarteEstCentreeSurLaRegionParisienne() throws Exception {
        String html = htmlOngletMissions();

        assertThat(html).contains("data-latitude-centre=\"48.8566\"");
        assertThat(html).contains("data-longitude-centre=\"2.3522\"");
        assertThat(html).contains("data-zoom-initial=\"10\"");
        assertThat(html).contains("data-zoom-pas-leger=\"1\"");
        assertThat(html).contains("data-zoom-pas-fort=\"10\"");
    }

    @Test
    @Transactional
    void laCartePorteUnMarqueurParPersonnageAvecSesCoordonneesReelles() throws Exception {
        CharacterEntity personnage = personnages.findAll().get(0);
        Lieu lieu = personnage.getLieu();

        String html = htmlOngletMissions();

        assertThat(lieu).isNotNull();
        assertThat(html).contains("data-type=\"personnage\"");
        assertThat(html).contains("data-nom=\"" + personnage.getName() + "\"");
        assertThat(html).contains("data-latitude=\"" + lieu.latitude() + "\"");
        assertThat(html).contains("data-longitude=\"" + lieu.longitude() + "\"");
    }

    @Test
    void laCartePorteLesMissionsDisponiblesCommeMarqueursSelectionnables() throws Exception {
        Mission mission = missions.get(0);

        String html = htmlOngletMissions();

        assertThat(html).contains("data-type=\"mission\"");
        assertThat(html).contains("data-cible=\"" + mission.id() + "\"");
        assertThat(html).contains("data-latitude=\"" + mission.lieu().latitude() + "\"");
    }

    @Test
    void lesTypesDeMarqueursPersonnagesSontDistingues() throws Exception {
        String html = htmlOngletMissions();

        assertThat(html).contains("data-recrute=\"false\"");
        assertThat(html).contains("marqueur-xyz ' + classes");
        assertThat(html).contains("personnage-recrute");
        assertThat(html).contains("personnage-recrutable");
    }

    @Test
    void lesCartesDeMissionPortentLeurIdentifiantPourLaSelectionDepuisLaCarte() throws Exception {
        String html = htmlOngletMissions();

        for (Mission mission : missions) {
            assertThat(html).contains("data-mission=\"" + mission.id() + "\"");
        }
    }

}
