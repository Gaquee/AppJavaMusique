import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Le catalogue de l'application : les morceaux rangés par genre.
// Une Map plutôt qu'une liste : « tous les morceaux d'un genre » s'obtient directement,
// sans parcourir tout le catalogue, et les genres disponibles sont simplement ses clés.
final class Catalogue {
    // Genres proposés (étiquettes Jamendo) et leur nom affiché
    static final Map<String, String> GENRES = new LinkedHashMap<>();
    static {
        GENRES.put("pop", "Pop");
        GENRES.put("rock", "Rock");
        GENRES.put("electronic", "Électro");
        GENRES.put("hiphop", "Hip-hop");
        GENRES.put("jazz", "Jazz");
        GENRES.put("classical", "Classique");
        GENRES.put("ambient", "Ambient");
        GENRES.put("metal", "Metal");
    }
    private static final int MORCEAUX_PAR_GENRE = 12;

    private final Map<String, List<Morceau>> parGenre = new TreeMap<>();
    private final Map<String, Morceau> parId = new HashMap<>();

    static String libelle(String genre) {
        return GENRES.getOrDefault(genre, genre);
    }

    // Lit le catalogue en base ; s'il est vide (premier lancement), l'alimente depuis Jamendo.
    // Bloquant : à appeler via Tache.
    static Catalogue charger() throws Exception {
        Catalogue c = new Catalogue();
        BaseDonnees.catalogue().forEach(c::ajouter);
        if (c.taille() == 0) {
            for (String genre : GENRES.keySet()) {
                for (Morceau m : Jamendo.parGenre(genre, MORCEAUX_PAR_GENRE)) {
                    if (c.ajouter(m)) BaseDonnees.ajouterMorceau(m);
                }
            }
        }
        return c;
    }

    // Renvoie false si le morceau y est déjà
    boolean ajouter(Morceau m) {
        if (parId.containsKey(m.id)) return false;
        parId.put(m.id, m);
        parGenre.computeIfAbsent(m.genre, g -> new ArrayList<>()).add(m);
        return true;
    }

    boolean supprimer(String id) {
        Morceau m = parId.remove(id);
        if (m == null) return false;
        List<Morceau> duGenre = parGenre.get(m.genre);
        duGenre.remove(m);
        if (duGenre.isEmpty()) parGenre.remove(m.genre);
        return true;
    }

    boolean contient(String id)         { return parId.containsKey(id); }
    int taille()                        { return parId.size(); }
    List<String> genres()               { return new ArrayList<>(parGenre.keySet()); }
    List<Morceau> duGenre(String genre) { return List.copyOf(parGenre.getOrDefault(genre, List.of())); }

    List<Morceau> tous() {
        List<Morceau> tous = new ArrayList<>();
        parGenre.values().forEach(tous::addAll);
        return tous;
    }

    // Artistes du catalogue (ordre alphabétique) avec leur nombre de morceaux
    Map<String, Integer> artistes() {
        Map<String, Integer> artistes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Morceau m : parId.values()) artistes.merge(m.artiste, 1, Integer::sum);
        return artistes;
    }
}
