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
    public Mission fousDeGotheim() {
        return Missions.fousDeGotheim();
    }

    @Bean
    public Mission rumeursDePossessionDemoniaque() {
        return Missions.rumeursDePossessionDemoniaque();
    }
}
