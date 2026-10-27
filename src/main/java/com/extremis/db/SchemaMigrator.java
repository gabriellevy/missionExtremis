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
        rendreCascadeSiNecessaire("execution_log_lines", "execution_id", "mission_executions");
        rendreCascadeSiNecessaire("execution_team_members", "execution_id", "mission_executions");
        rendreCascadeSiNecessaire("execution_ajustements", "execution_id", "mission_executions");
        migrerExecutionEvenementCourant();
        ajouterColonneBooleenneSiAbsente("execution_team_members", "exclu");
        ajouterColonneSiAbsente("execution_team_members", "sang_froid");
        deplacerSpecialitesDesTraits();
    }

    /**
     * Migration du modele lineaire (next_event_index + last_event_time en
     * texte) vers le modele evenement courant du wiki : colonnes
     * evenement_courant, heure_effet_courant et reussie. Hibernate
     * ddl-auto=update ne fait pas les renommages et peut laisser la base
     * anterieure incoherente (colonne reussie absente par exemple).
     */
    private void migrerExecutionEvenementCourant() {
        ajouterColonneTexteSiAbsente("mission_executions", "evenement_courant");
        ajouterColonneHorodatageSiAbsente("mission_executions", "heure_effet_courant");
        ajouterColonneBooleenneSiAbsente("mission_executions", "reussie");
        supprimerColonneSiPresente("mission_executions", "next_event_index");
        supprimerColonneSiPresente("mission_executions", "last_event_time");
    }

    private void ajouterColonneTexteSiAbsente(String table, String colonne) {
        if (!colonneExiste(table, colonne)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " varchar(128)");
            log.info("Colonne {} ajoutee a la table {}", colonne, table);
        }
    }

    private void ajouterColonneHorodatageSiAbsente(String table, String colonne) {
        if (!colonneExiste(table, colonne)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " timestamp");
            log.info("Colonne {} ajoutee a la table {}", colonne, table);
        }
    }

    private void ajouterColonneBooleenneSiAbsente(String table, String colonne) {
        if (!colonneExiste(table, colonne)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + colonne + " boolean NOT NULL DEFAULT false");
            log.info("Colonne {} ajoutee a la table {}", colonne, table);
        }
    }

    private void supprimerColonneSiPresente(String table, String colonne) {
        if (colonneExiste(table, colonne)) {
            jdbc.execute("ALTER TABLE " + table + " DROP COLUMN " + colonne);
            log.info("Colonne {} supprimee de la table {}", colonne, table);
        }
    }

    /**
     * Les specialites etaient stockees comme des traits avant d'avoir leur
     * propre table : on les deplace (une seule fois) vers character_specialites.
     */
    private void deplacerSpecialitesDesTraits() {
        Integer nb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM character_traits WHERE name LIKE '%+%'", Integer.class);
        if (nb == null || nb == 0) {
            return;
        }
        jdbc.execute("INSERT INTO character_specialites (owner_id, nom) "
                + "SELECT owner_id, name FROM character_traits WHERE name LIKE '%+%'");
        jdbc.execute("DELETE FROM character_traits WHERE name LIKE '%+%'");
        log.info("Specialites deplacees de character_traits vers character_specialites");
    }

    /**
     * Hibernate ne recree pas une cle etrangere existante en ON DELETE CASCADE
     * (ddl-auto=update) : sur une base ancienne, la FK reste en NO ACTION et le
     * deleteAll() de reinitialiserUsine() echoue. On la recree a la main.
     */
    private void rendreCascadeSiNecessaire(String tableEnfant, String colonneFk, String tableParente) {
        String contrainte = nomContrainte(tableEnfant, colonneFk);
        if (contrainte == null) {
            return;
        }
        Integer cascade = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.referential_constraints"
                        + " WHERE constraint_name = ? AND delete_rule = 'CASCADE'",
                Integer.class, contrainte);
        if (cascade != null && cascade > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE " + tableEnfant + " DROP CONSTRAINT " + contrainte);
        jdbc.execute("ALTER TABLE " + tableEnfant + " ADD CONSTRAINT " + contrainte
                + " FOREIGN KEY (" + colonneFk + ") REFERENCES " + tableParente + " (id) ON DELETE CASCADE");
        log.info("Cle etrangere {}({}) recreee en ON DELETE CASCADE", tableEnfant, colonneFk);
    }

    private String nomContrainte(String tableEnfant, String colonneFk) {
        var resultats = jdbc.queryForList(
                "SELECT tc.constraint_name FROM information_schema.table_constraints tc"
                        + " JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name"
                        + " WHERE tc.constraint_type = 'FOREIGN KEY' AND lower(tc.table_name) = lower(?)"
                        + " AND lower(kcu.column_name) = lower(?)",
                String.class, tableEnfant, colonneFk);
        return resultats.isEmpty() ? null : resultats.get(0);
    }

    private boolean colonneExiste(String table, String colonne) {
        Integer nb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE lower(table_name) = lower(?) AND lower(column_name) = lower(?)",
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
