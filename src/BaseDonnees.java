import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

// Tous les accès MySQL. Ces méthodes sont bloquantes : les appeler hors du thread Swing (voir Tache).
final class BaseDonnees {
    private BaseDonnees() {}

    static Connection connexion() throws SQLException {
        Properties p = new Properties();
        p.setProperty("user", Config.utilisateurBD());
        p.setProperty("password", Config.motDePasseBD());
        p.setProperty("connectTimeout", "4000");
        p.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(Config.urlBD(), p);
    }

    // Renvoie l'utilisateur, ou null si les identifiants sont incorrects.
    static Utilisateur authentifier(String login, char[] mdp) throws SQLException {
        String sql = "SELECT id, login, mdp, role FROM utilisateur WHERE login = ?";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String stocke = rs.getString("mdp");
                if (!MotDePasse.verifier(mdp, stocke)) return null;

                Utilisateur u = Utilisateur.creer(rs.getInt("id"), rs.getString("login"), rs.getString("role"));
                if (!MotDePasse.estHache(stocke)) {
                    // Ancien compte enregistré en clair : on le convertit maintenant qu'il est vérifié.
                    try (PreparedStatement maj = conn.prepareStatement("UPDATE utilisateur SET mdp = ? WHERE id = ?")) {
                        maj.setString(1, MotDePasse.hacher(mdp));
                        maj.setInt(2, u.id());
                        maj.executeUpdate();
                    }
                }
                return u;
            }
        }
    }

    // Renvoie false si l'identifiant est déjà pris.
    static boolean inscrire(String login, char[] mdp) throws SQLException {
        String sql = "INSERT INTO utilisateur (login, mdp) VALUES (?, ?)";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, MotDePasse.hacher(mdp));
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        }
    }

    static List<Morceau> favoris(int idUtilisateur) throws SQLException {
        String sql = "SELECT jamendo_id, titre, artiste, album, genre, duree, url_audio, url_image "
                   + "FROM favori WHERE utilisateur_id = ? ORDER BY date_ajout DESC, id DESC";
        List<Morceau> liste = new ArrayList<>();
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(lireMorceau(rs));
                }
            }
        }
        return liste;
    }

    static void ajouterFavori(int idUtilisateur, Morceau m) throws SQLException {
        String sql = "INSERT IGNORE INTO favori (utilisateur_id, jamendo_id, titre, artiste, album, genre, duree, url_audio, url_image) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setString(2, m.id);
            ps.setString(3, m.titre);
            ps.setString(4, m.artiste);
            ps.setString(5, m.album);
            ps.setString(6, m.genre);
            ps.setInt(7, m.duree);
            ps.setString(8, m.url);
            ps.setString(9, m.image);
            ps.executeUpdate();
        }
    }

    static void retirerFavori(int idUtilisateur, String idMorceau) throws SQLException {
        String sql = "DELETE FROM favori WHERE utilisateur_id = ? AND jamendo_id = ?";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setString(2, idMorceau);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ catalogue

    static List<Morceau> catalogue() throws SQLException {
        String sql = "SELECT jamendo_id, titre, artiste, album, genre, duree, url_audio, url_image "
                   + "FROM morceau ORDER BY genre, date_ajout, titre";
        List<Morceau> liste = new ArrayList<>();
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) liste.add(lireMorceau(rs));
        }
        return liste;
    }

    static void ajouterMorceau(Morceau m) throws SQLException {
        String sql = "INSERT IGNORE INTO morceau (jamendo_id, titre, artiste, album, genre, duree, url_audio, url_image) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.id);
            ps.setString(2, m.titre);
            ps.setString(3, m.artiste);
            ps.setString(4, m.album);
            ps.setString(5, m.genre);
            ps.setInt(6, m.duree);
            ps.setString(7, m.url);
            ps.setString(8, m.image);
            ps.executeUpdate();
        }
    }

    static void supprimerMorceau(String idMorceau) throws SQLException {
        try (Connection conn = connexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM morceau WHERE jamendo_id = ?")) {
            ps.setString(1, idMorceau);
            ps.executeUpdate();
        }
    }

    private static Morceau lireMorceau(ResultSet rs) throws SQLException {
        return new Morceau(rs.getString("jamendo_id"), rs.getString("titre"), rs.getString("artiste"),
            rs.getString("album"), rs.getString("genre"), rs.getInt("duree"),
            rs.getString("url_audio"), rs.getString("url_image"));
    }

    // ------------------------------------------------------------------ artistes (favoris et abonnements)

    static Set<String> artistesFavoris(int idUtilisateur) throws SQLException {
        return artistes("favori_artiste", idUtilisateur);
    }

    static Set<String> abonnements(int idUtilisateur) throws SQLException {
        return artistes("abonnement", idUtilisateur);
    }

    static void definirArtisteFavori(int idUtilisateur, String artiste, boolean favori) throws SQLException {
        definirArtiste("favori_artiste", idUtilisateur, artiste, favori);
    }

    static void definirAbonnement(int idUtilisateur, String artiste, boolean abonne) throws SQLException {
        definirArtiste("abonnement", idUtilisateur, artiste, abonne);
    }

    // table : nom fixe choisi par les méthodes ci-dessus, jamais une saisie de l'utilisateur
    private static Set<String> artistes(String table, int idUtilisateur) throws SQLException {
        Set<String> artistes = new HashSet<>();
        try (Connection conn = connexion();
             PreparedStatement ps = conn.prepareStatement("SELECT artiste FROM " + table + " WHERE utilisateur_id = ?")) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) artistes.add(rs.getString(1));
            }
        }
        return artistes;
    }

    private static void definirArtiste(String table, int idUtilisateur, String artiste, boolean present) throws SQLException {
        String sql = present ? "INSERT IGNORE INTO " + table + " (utilisateur_id, artiste) VALUES (?, ?)"
                             : "DELETE FROM " + table + " WHERE utilisateur_id = ? AND artiste = ?";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setString(2, artiste);
            ps.executeUpdate();
        }
    }
}
