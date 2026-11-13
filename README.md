# Lecteur musique

Lecteur de musique libre (catalogue [Jamendo](https://www.jamendo.com)) en Java Swing, avec comptes utilisateurs et favoris stockés dans MySQL.

## Fonctionnalités

- Connexion et inscription (mots de passe hachés en PBKDF2, anciens comptes convertis à la première connexion)
- Deux types d'utilisateurs, avec un menu différent : `Auditeur` et `Admin` (classes filles de la classe abstraite `Utilisateur`)
- Catalogue de morceaux rangés par genre, alimenté depuis Jamendo au premier lancement
- Auditeur : filtre par genre, morceaux favoris, artistes favoris, abonnement à des artistes
- Admin : suppression de morceaux (deux clics sur la croix) et ajout depuis une recherche Jamendo, avec choix du genre
- Lecture en streaming : lecture/pause, précédent/suivant, progression cliquable, volume, aléatoire, répétition, enchaînement automatique
- Pochettes d'album, mémorisation du volume et du dernier identifiant

## Prérequis

- JDK 21 ou plus récent
- MySQL ou MariaDB (XAMPP convient) démarré sur `localhost:3306`
- Dans `lib/` (non versionné) : le SDK JavaFX dans `lib/javafx-sdk`, `mysql-connector-j-*.jar` et `json-*.jar`

## Installation

1. Créer les tables avec un compte administrateur : `mysql -u root < sql/schema.sql`
2. Donner les droits au compte applicatif :
   `GRANT SELECT, INSERT, UPDATE, DELETE ON musique.* TO 'app_musique'@'localhost';`
3. Copier `config.example.properties` en `config.properties`, puis renseigner le mot de passe de la base et l'identifiant d'application Jamendo (aucun des deux n'est dans le code).
4. Les comptes créés depuis l'application sont des auditeurs. Pour nommer un administrateur :
   `UPDATE utilisateur SET role = 'admin' WHERE login = '...';`

## Lancement

Double-cliquer sur `lancer.bat` (compile puis démarre), ou depuis VS Code avec la configuration fournie dans `.vscode/`.

## Organisation du code (`src/`)

| Fichier | Rôle |
| --- | --- |
| `App.java` | Fenêtre, écrans de connexion et d'inscription |
| `PageLecteur.java` | Écran principal : onglets du menu, listes, barre de lecture |
| `Utilisateur.java`, `Auditeur.java`, `Admin.java`, `Onglet.java` | Types d'utilisateurs et leur menu |
| `Catalogue.java`, `Morceau.java` | Catalogue par genre (`Map`) et morceaux |
| `Lecteur.java` | Lecture audio (JavaFX Media) |
| `Jamendo.java` | Appels à l'API Jamendo |
| `BaseDonnees.java` | Requêtes MySQL (utilisateurs, catalogue, favoris, abonnements) |
| `MotDePasse.java` | Hachage et vérification des mots de passe |
| `Images.java` | Téléchargement et cache des pochettes |
| `BoutonArrondi.java`, `Composants.java`, `Listes.java` | Charte graphique et composants Swing maison |
| `Config.java`, `Tache.java` | Réglages, tâches de fond |
