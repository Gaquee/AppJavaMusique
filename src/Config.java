import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// Réglages lus dans config.properties (à la racine du projet).
// Une variable d'environnement MUSIQUE_XXX est prioritaire sur le fichier.
final class Config {
    private static final Properties PROPS = charger();

    private Config() {}

    static String urlBD()          { return lire("db.url", "jdbc:mysql://localhost:3306/musique"); }
    static String utilisateurBD()  { return lire("db.utilisateur", "app_musique"); }
    static String motDePasseBD()   { return lire("db.motdepasse", ""); }
    static String clientJamendo()  { return lire("jamendo.client_id", ""); }

    private static String lire(String cle, String defaut) {
        String env = System.getenv("MUSIQUE_" + cle.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        return PROPS.getProperty(cle, defaut).trim();
    }

    private static Properties charger() {
        Properties p = new Properties();
        Path fichier = Path.of(System.getProperty("musique.config", "config.properties"));
        if (Files.isRegularFile(fichier)) {
            try (Reader r = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                System.err.println("Lecture de " + fichier + " impossible : " + e.getMessage());
            }
        } else {
            System.err.println("Fichier " + fichier.toAbsolutePath()
                + " introuvable : copie config.example.properties et renseigne le mot de passe.");
        }
        return p;
    }
}
