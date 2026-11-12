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
  duree          INT          NOT NULL DEFAULT 0,
  url_audio      VARCHAR(500) NOT NULL,
  url_image      VARCHAR(500) NOT NULL DEFAULT '',
  date_ajout     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY favori_unique (utilisateur_id, jamendo_id),
  CONSTRAINT fk_favori_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
