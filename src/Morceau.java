public class Morceau {
    final String id;      // identifiant Jamendo
    final String titre;
    final String artiste;
    final String album;
    final String genre;   // étiquette Jamendo (voir Catalogue.GENRES), vide si inconnu
    final int duree;      // en secondes (0 si inconnue)
    final String url;     // flux audio
    final String image;   // pochette

    public Morceau(String id, String titre, String artiste, String album, String genre, int duree, String url, String image) {
        this.id = id;
        this.titre = titre;
        this.artiste = artiste;
        this.album = album;
        this.genre = genre == null ? "" : genre;
        this.duree = duree;
        this.url = url;
        this.image = image;
    }

    Morceau avecGenre(String nouveauGenre) {
        return new Morceau(id, titre, artiste, album, nouveauGenre, duree, url, image);
    }

    static String formaterDuree(double secondes) {
        if (Double.isNaN(secondes) || secondes < 0) secondes = 0;
        int s = (int) secondes;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    @Override
    public String toString() {
        return titre + " - " + artiste;
    }
}
