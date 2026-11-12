import java.util.List;

// Écoute le catalogue, gère ses favoris (morceaux et artistes) et ses abonnements.
final class Auditeur extends Utilisateur {
    Auditeur(int id, String login) {
        super(id, login);
    }

    @Override String libelle() { return "Auditeur"; }

    @Override List<Onglet> menu() {
        return List.of(Onglet.CATALOGUE, Onglet.FAVORIS, Onglet.ARTISTES);
    }
}
