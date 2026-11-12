-- Schéma de la base « musique » (MySQL / MariaDB).
-- À exécuter avec un compte administrateur (root) : le compte applicatif n'a pas le droit CREATE.
--   mysql -u root < sql/schema.sql

CREATE DATABASE IF NOT EXISTS musique CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE musique;

CREATE TABLE IF NOT EXISTS utilisateur (
  id            INT          NOT NULL AUTO_INCREMENT,
  login         VARCHAR(50)  NOT NULL,
  mdp           VARCHAR(255) NOT NULL,           -- haché par l'application (PBKDF2)
  role          ENUM('utilisateur','admin') NOT NULL DEFAULT 'utilisateur',
  date_creation DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY login (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS favori (
  id             INT          NOT NULL AUTO_INCREMENT,
  utilisateur_id INT          NOT NULL,
  jamendo_id     VARCHAR(20)  NOT NULL,
  titre          VARCHAR(255) NOT NULL,
  artiste        VARCHAR(255) NOT NULL,
  album          VARCHAR(255) NOT NULL DEFAULT '',
  genre          VARCHAR(50)  NOT NULL DEFAULT '',
  duree          INT          NOT NULL DEFAULT 0,
  url_audio      VARCHAR(500) NOT NULL,
  url_image      VARCHAR(500) NOT NULL DEFAULT '',
  date_ajout     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY favori_unique (utilisateur_id, jamendo_id),
  CONSTRAINT fk_favori_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Catalogue de l'application (alimenté depuis Jamendo au premier lancement, puis géré par les admins)
CREATE TABLE IF NOT EXISTS morceau (
  jamendo_id VARCHAR(20)  NOT NULL,
  titre      VARCHAR(255) NOT NULL,
  artiste    VARCHAR(255) NOT NULL,
  album      VARCHAR(255) NOT NULL DEFAULT '',
  genre      VARCHAR(50)  NOT NULL,
  duree      INT          NOT NULL DEFAULT 0,
  url_audio  VARCHAR(500) NOT NULL,
  url_image  VARCHAR(500) NOT NULL DEFAULT '',
  date_ajout DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (jamendo_id),
  KEY morceau_genre (genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Artistes mis en favoris par un auditeur
CREATE TABLE IF NOT EXISTS favori_artiste (
  utilisateur_id INT          NOT NULL,
  artiste        VARCHAR(191) NOT NULL,
  PRIMARY KEY (utilisateur_id, artiste),
  CONSTRAINT fk_favori_artiste_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Artistes auxquels un auditeur est abonné
CREATE TABLE IF NOT EXISTS abonnement (
  utilisateur_id INT          NOT NULL,
  artiste        VARCHAR(191) NOT NULL,
  date_ajout     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (utilisateur_id, artiste),
  CONSTRAINT fk_abonnement_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
