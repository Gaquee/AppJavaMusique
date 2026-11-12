// Les entrées possibles du menu principal (voir Utilisateur.menu()).
enum Onglet {
    CATALOGUE("Catalogue"),
    FAVORIS("Favoris"),
    ARTISTES("Artistes"),
    GESTION("Ajouter des titres");

    final String titre;

    Onglet(String titre) {
        this.titre = titre;
    }
}
