-- =====================================================================
--  HackAcademy - TP2 : Gestion des agents et affectation aux missions
--  Script complet : schéma de la base + jeu de données de test
--
--  Le script est rejouable : il ne supprime rien. Sur une base TP1
--  existante, il ajoute simplement les tables agent et participation
--  et complète les données de test sans créer de doublons.
--
--  Relations :
--    agent (1,n) --- participation --- (1,n) mission
--    participation = table associative (rôle + date d'affectation)
-- =====================================================================

-- Le script est encodé en UTF-8 : on l'indique au serveur pour que les
-- accents soient correctement enregistrés (client mysql, Workbench...).
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS gestion_missions
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gestion_missions;

-- ---------------------------------------------------------------------
--  Table mission (TP1, inchangée)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mission (
  id INT AUTO_INCREMENT PRIMARY KEY,
  titre VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL,
  date_debut DATE NOT NULL,
  date_fin DATE NOT NULL,
  statut ENUM('A_PREPARER','EN_COURS','TERMINEE','ANNULEE') NOT NULL,
  priorite ENUM('BASSE','NORMALE','HAUTE','CRITIQUE') NOT NULL,
  budget DECIMAL(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT chk_dates CHECK (date_fin >= date_debut),
  CONSTRAINT chk_budget CHECK (budget >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  Partie A.1 - Table agent
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent (
  id_agent INT AUTO_INCREMENT PRIMARY KEY,
  pseudo VARCHAR(50) NOT NULL UNIQUE,
  nom VARCHAR(80) NOT NULL,
  specialite VARCHAR(50) NOT NULL,
  niveau INT NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  Partie A.2 - Table associative participation
--  Clé primaire composée : un agent n'est affecté qu'une fois
--  à une même mission. ON DELETE CASCADE : supprimer un agent ou
--  une mission supprime automatiquement ses affectations.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS participation (
  id_agent INT NOT NULL,
  id_mission INT NOT NULL,
  role VARCHAR(100) NOT NULL,
  date_affectation DATE NOT NULL,
  PRIMARY KEY (id_agent, id_mission),
  FOREIGN KEY (id_agent) REFERENCES agent(id_agent) ON DELETE CASCADE,
  FOREIGN KEY (id_mission) REFERENCES mission(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
--  Jeu de données de test
-- =====================================================================

-- ---------------------------------------------------------------------
--  Missions (3 missions du TP1 + 3 nouvelles)
-- ---------------------------------------------------------------------
INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Audit du réseau', 'Cartographier le réseau et relever les vulnérabilités.',
       '2026-09-21', '2026-09-25', 'A_PREPARER', 'HAUTE', 2500.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Audit du réseau');

INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Durcissement serveur web', 'Appliquer et tester les recommandations de sécurité.',
       '2026-09-14', '2026-09-18', 'EN_COURS', 'CRITIQUE', 1800.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Durcissement serveur web');

INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Sensibilisation phishing', 'Préparer une campagne et un bilan utilisateurs.',
       '2026-10-05', '2026-10-09', 'A_PREPARER', 'NORMALE', 900.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Sensibilisation phishing');

INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Test d''intrusion application RH', 'Tester l''application RH en boîte grise et rédiger le rapport.',
       '2026-08-31', '2026-09-04', 'TERMINEE', 'HAUTE', 3200.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Test d''intrusion application RH');

INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Analyse forensique poste compromis', 'Collecter les traces et reconstituer la chronologie de l''incident.',
       '2026-11-16', '2026-11-27', 'EN_COURS', 'CRITIQUE', 4100.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Analyse forensique poste compromis');

INSERT INTO mission(titre, description, date_debut, date_fin, statut, priorite, budget)
SELECT 'Revue des sauvegardes', 'Vérifier la politique de sauvegarde et tester une restauration.',
       '2026-12-07', '2026-12-09', 'A_PREPARER', 'BASSE', 600.00
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE titre = 'Revue des sauvegardes');

-- ---------------------------------------------------------------------
--  Agents (6 agents dont 1 inactif)
--  INSERT IGNORE : le pseudo étant UNIQUE, un agent déjà présent
--  n'est pas inséré une seconde fois.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO agent(pseudo, nom, specialite, niveau, actif) VALUES
  ('Cipher',   'Claire Martin',   'Cryptographie',        4, TRUE),
  ('NullByte', 'Hugo Bernard',    'Pentest web',          5, TRUE),
  ('Kernel',   'Sofia Lefèvre',   'Systèmes Linux',       3, TRUE),
  ('Ghost',    'Yanis Moreau',    'Forensique',           4, TRUE),
  ('Phish',    'Léa Garnier',     'Ingénierie sociale',   2, TRUE),
  ('Legacy',   'Marc Dubois',     'Réseaux',              3, FALSE);

-- ---------------------------------------------------------------------
--  Participations (affectations des agents aux missions)
--  Les identifiants sont retrouvés à partir du pseudo de l'agent et
--  du titre de la mission : le script ne dépend donc pas des valeurs
--  AUTO_INCREMENT. INSERT IGNORE évite les doublons (clé primaire
--  composée id_agent + id_mission).
-- ---------------------------------------------------------------------
INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Chef de mission', '2026-09-01'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'NullByte' AND m.titre = 'Audit du réseau';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Analyste réseau', '2026-09-02'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Kernel' AND m.titre = 'Audit du réseau';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Chef de mission', '2026-09-07'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Kernel' AND m.titre = 'Durcissement serveur web';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Configuration TLS', '2026-09-08'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Cipher' AND m.titre = 'Durcissement serveur web';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Responsable campagne', '2026-09-20'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Phish' AND m.titre = 'Sensibilisation phishing';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Pentesteur principal', '2026-08-24'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'NullByte' AND m.titre = 'Test d''intrusion application RH';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Support réseau', '2026-08-24'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Legacy' AND m.titre = 'Test d''intrusion application RH';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Chef de mission', '2026-11-15'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Ghost' AND m.titre = 'Analyse forensique poste compromis';

INSERT IGNORE INTO participation(id_agent, id_mission, role, date_affectation)
SELECT a.id_agent, m.id, 'Analyse des données chiffrées', '2026-11-16'
FROM agent a CROSS JOIN mission m
WHERE a.pseudo = 'Cipher' AND m.titre = 'Analyse forensique poste compromis';

-- ---------------------------------------------------------------------
--  Requête de contrôle : agents mobilisés sur chaque mission
-- ---------------------------------------------------------------------
-- SELECT m.titre, a.pseudo, a.nom, p.role, p.date_affectation
-- FROM participation p
-- JOIN agent a   ON a.id_agent = p.id_agent
-- JOIN mission m ON m.id       = p.id_mission
-- ORDER BY m.date_debut, a.pseudo;
