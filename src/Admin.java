import java.util.List;

// Gère le catalogue : supprime des morceaux et en ajoute depuis Jamendo.
final class Admin extends Utilisateur {
    Admin(int id, String login) {
        super(id, login);
    }

    @Override String libelle() { return "Administrateur"; }

    @Override List<Onglet> menu() {
        return List.of(Onglet.CATALOGUE, Onglet.GESTION);
    }
}
