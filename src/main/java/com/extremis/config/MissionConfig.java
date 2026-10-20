package com.extremis.config;

import com.extremis.catalog.Missions;
import com.extremis.core.Mission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!test-rapide")
public class MissionConfig {

    @Bean
    public List<Mission> missionsDisponibles() {
        return List.of(Missions.fousDeGotheim(), Missions.rumeursDePossessionDemoniaque());
    }

    @Bean
    public Mission defaultMission(List<Mission> missionsDisponibles) {
        return missionsDisponibles.get(0);
    }
}
