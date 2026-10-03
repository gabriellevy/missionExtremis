package com.extremis.config;

import com.extremis.catalog.Missions;
import com.extremis.core.Mission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test-rapide")
public class MissionConfig {

    @Bean
    public Mission defaultMission() {
        return Missions.foolsOfGotheim();
    }
}
