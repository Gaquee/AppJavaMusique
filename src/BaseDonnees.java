import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

record Utilisateur(int id, String login, String role) {
    boolean estAdmin() { return "admin".equals(role); }
}

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

                Utilisateur u = new Utilisateur(rs.getInt("id"), rs.getString("login"), rs.getString("role"));
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
        String sql = "SELECT jamendo_id, titre, artiste, album, duree, url_audio, url_image "
                   + "FROM favori WHERE utilisateur_id = ? ORDER BY date_ajout DESC, id DESC";
        List<Morceau> liste = new ArrayList<>();
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Morceau(rs.getString("jamendo_id"), rs.getString("titre"), rs.getString("artiste"),
                        rs.getString("album"), rs.getInt("duree"), rs.getString("url_audio"), rs.getString("url_image")));
                }
            }
        }
        return liste;
    }

    static void ajouterFavori(int idUtilisateur, Morceau m) throws SQLException {
        String sql = "INSERT IGNORE INTO favori (utilisateur_id, jamendo_id, titre, artiste, album, duree, url_audio, url_image) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.setString(2, m.id);
            ps.setString(3, m.titre);
            ps.setString(4, m.artiste);
            ps.setString(5, m.album);
            ps.setInt(6, m.duree);
            ps.setString(7, m.url);
            ps.setString(8, m.image);
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
}
