package com.extremis.web;

import com.extremis.core.Mission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "mission.tick-rate-ms=100",
        "spring.profiles.active=test-rapide"
})
@Import(GameControllerIntegrationTest.FastMissions.class)
class GameControllerIntegrationTest {

    @Autowired
    private GameController controller;

    @Test
    void tickerAdvancesMissionWithoutManualAction() throws InterruptedException {
        controller.recrutementRapide("Alice");
        controller.lancementRapide();

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (controller.journal().contains("Mission terminee")
                    || controller.journal().contains("Equipe eliminee")) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(controller.journal()).contains("Infiltration du meeting nocturne");
        assertThat(controller.journal()).containsAnyOf("Mission terminee", "Equipe eliminee");
    }

    @TestConfiguration
    static class FastMissions {
        @Bean
        public Mission fastMission() {
            return com.extremis.catalog.Missions.foolsOfGotheimRapide();
        }
    }
}
