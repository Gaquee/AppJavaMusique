import java.util.List;

// Un utilisateur connecté. La classe est abstraite : un utilisateur est forcément
// un Auditeur ou un Admin, et c'est le type concret qui décide du menu affiché.
abstract class Utilisateur {
    private final int id;
    private final String login;

    protected Utilisateur(int id, String login) {
        this.id = id;
        this.login = login;
    }

    int id()       { return id; }
    String login() { return login; }

    // Nom affiché à côté du prénom (« Auditeur », « Administrateur »)
    abstract String libelle();

    // Les onglets proposés à cet utilisateur, dans l'ordre d'affichage
    abstract List<Onglet> menu();

    // Construit le bon type à partir du rôle stocké en base
    static Utilisateur creer(int id, String login, String role) {
        return "admin".equals(role) ? new Admin(id, login) : new Auditeur(id, login);
    }
}
