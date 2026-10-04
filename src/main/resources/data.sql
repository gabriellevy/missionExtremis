-- Renommage des anciennes competences vers la liste du wiki.
-- Script idempotent : ne touche que les lignes encore en ancien nom.
UPDATE character_skills SET skill = 'ARMES_CORPS_A_CORPS' WHERE skill = 'COMBAT';
UPDATE character_skills SET skill = 'MOUVEMENT'          WHERE skill = 'AGILITE';
UPDATE character_skills SET skill = 'INTUITION'          WHERE skill = 'ERUDITION';
UPDATE character_skills SET skill = 'ELOQUENCE'          WHERE skill = 'DIPLOMATIE';
UPDATE character_skills SET skill = 'HABILETE'           WHERE skill = 'TECHNIQUE';
