package com.extremis.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migrations de schema imperatives a la main : Hibernate (ddl-auto=update) ne
 * renomme jamais une colonne et data.sql est decoupe sur les ';' (les blocs
 * PL/pgSQL DO $$ ... $$ y sont donc tronques). Ce runner s'execute avant le
 * CharacterSeeder (@Order) et reste idempotent.
 */
@Component
@Order(0)
public class SchemaMigrator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final JdbcTemplate jdbc;

    public SchemaMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        renommerColonneSiPresente("characters", "health", "vitalite");
        renommerColonneSiPresente("execution_team_members", "health", "vitalite");
        ajouterColonneSiAbsente("characters", "sang_froid");
    }

    private boolean colonneExiste(String table, String colonne) {
        Integer nb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                Integer.class, table, colonne);
        return nb != null && nb > 0;
    }

    private void renommerColonneSiPresente(String table, String ancienNom, String nouveauNom) {
        if (colonneExiste(table, ancienNom) && !colonneExiste(table, nouveauNom)) {
            jdbc.execute("ALTER TABLE " + table + " RENAME COLUMN " + ancienNom + " TO " + nouveauNom);
            log.info("Colonne {}.{} renommee en {}", table, ancienNom, nouveauNom);
        }
    }

    private void ajouterColonneSiAbsente(String table, String colonne) {
        if (!colonneExiste(table, colonne)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " integer NOT NULL DEFAULT 10");
            log.info("Colonne {} ajoutee a la table {}", colonne, table);
        }
    }
}
