# Lecteur musique

Lecteur de musique libre (catalogue [Jamendo](https://www.jamendo.com)) en Java Swing, avec comptes utilisateurs et favoris stockés dans MySQL.

## Fonctionnalités

- Connexion et inscription (mots de passe hachés en PBKDF2, anciens comptes convertis à la première connexion)
- Titres du moment à l'ouverture, recherche par titre ou artiste
- Lecture en streaming : lecture/pause, précédent/suivant, barre de progression cliquable, volume, lecture aléatoire, répétition de la liste, enchaînement automatique
- Favoris par utilisateur (clic sur le cœur), onglet dédié avec filtre instantané
- Pochettes d'album, mémorisation du volume et du dernier identifiant

## Prérequis

- JDK 21 ou plus récent
- MySQL ou MariaDB (XAMPP convient) démarré sur `localhost:3306`
- Dans `lib/` (non versionné) : le SDK JavaFX dans `lib/javafx-sdk`, `mysql-connector-j-*.jar` et `json-*.jar`

## Installation

1. Créer les tables avec un compte administrateur : `mysql -u root < sql/schema.sql`
2. Donner les droits au compte applicatif :
   `GRANT SELECT, INSERT, UPDATE, DELETE ON musique.* TO 'app_musique'@'localhost';`
3. Copier `config.example.properties` en `config.properties` et renseigner le mot de passe de la base.

## Lancement

Double-cliquer sur `lancer.bat` (compile puis démarre), ou depuis VS Code avec la configuration fournie dans `.vscode/`.

## Organisation du code (`src/`)

| Fichier | Rôle |
| --- | --- |
| `App.java` | Fenêtre, écrans de connexion et d'inscription |
| `PageLecteur.java` | Écran principal : recherche, favoris, barre de lecture |
| `Lecteur.java` | Lecture audio (JavaFX Media) |
| `Jamendo.java` | Appels à l'API Jamendo |
| `BaseDonnees.java` | Requêtes MySQL (utilisateurs, favoris) |
| `MotDePasse.java` | Hachage et vérification des mots de passe |
| `Images.java` | Téléchargement et cache des pochettes |
| `BoutonArrondi.java`, `Composants.java` | Charte graphique et composants Swing maison |
| `Config.java`, `Tache.java`, `Morceau.java` | Réglages, tâches de fond, modèle |
