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
