package com.extremis.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Profil par defaut : demarre un PostgreSQL embarque (binaires Zonky charges par
 * Maven, aucun Docker ni installation requise). La base persiste sur disque
 * dans ./data/postgres, donc l'etat du jeu survit aux redemarrages.
 */
@Configuration
@Profile("!test-rapide")
public class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres postgresEmbarque() throws IOException {
        return EmbeddedPostgres.builder()
                .setDataDirectory("data/postgres")
                .setCleanDataDirectory(false)
                .setServerConfig("listen_addresses", "127.0.0.1")
                .setPort(5433)
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgresEmbarque) {
        String url = postgresEmbarque.getJdbcUrl("postgres", "postgres");
        DriverManagerDataSource ds = new DriverManagerDataSource(url);
        ds.setUsername("postgres");
        ds.setPassword("postgres");
        return ds;
    }
}
