-- Migration des anciens noms de competences vers la liste du wiki.
-- Idempotent : ne touche que les lignes encore en ancien nom.
-- Hibernate 6 avait cree une contrainte CHECK avec l'ancienne liste de valeurs :
-- il faut la retirer pour pouvoir ecrire les nouveaux noms.
ALTER TABLE character_skills DROP CONSTRAINT IF EXISTS character_skills_skill_check;
UPDATE character_skills SET skill = 'ARMES_CORPS_A_CORPS' WHERE skill = 'COMBAT';
UPDATE character_skills SET skill = 'MOUVEMENT'          WHERE skill = 'AGILITE';
UPDATE character_skills SET skill = 'INTUITION'          WHERE skill = 'ERUDITION';
UPDATE character_skills SET skill = 'ELOQUENCE'         WHERE skill = 'DIPLOMATIE';
UPDATE character_skills SET skill = 'HABILETE'           WHERE skill = 'TECHNIQUE';

-- Migration health -> vitalite + ajout de la colonne sang_froid.
-- Idempotent : chaque bloc DO verifie d'abord la presence de la colonne.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'characters' AND column_name = 'health')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'characters' AND column_name = 'vitalite') THEN
        ALTER TABLE characters RENAME COLUMN health TO vitalite;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'characters' AND column_name = 'health')
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'characters' AND column_name = 'vitalite') THEN
        EXECUTE 'UPDATE characters SET vitalite = health';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'execution_team_members' AND column_name = 'health')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'execution_team_members' AND column_name = 'vitalite') THEN
        ALTER TABLE execution_team_members RENAME COLUMN health TO vitalite;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'execution_team_members' AND column_name = 'health')
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'execution_team_members' AND column_name = 'vitalite') THEN
        EXECUTE 'UPDATE execution_team_members SET vitalite = health';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'characters' AND column_name = 'sang_froid') THEN
        ALTER TABLE characters ADD COLUMN sang_froid integer NOT NULL DEFAULT 10;
    END IF;
END $$;
