import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

// Accès à l'API Jamendo (musique libre). Méthodes bloquantes : à appeler via Tache.
final class Jamendo {
    private static final String BASE = "https://api.jamendo.com/v3.0/tracks/";
    private static final int LIMITE = 30;
    private static final int ESSAIS = 4;

    static final HttpClient CLIENT = creerClient();

    private static HttpClient creerClient() {
        // Sous Windows, le client HTTP de Java ouvre une socket interne dans le dossier temporaire.
        // Sur certains postes ce dossier la refuse (« Unable to establish loopback connection ») :
        // on la place donc dans le dossier personnel, sauf réglage explicite.
        if (System.getProperty("jdk.net.unixdomain.tmpdir") == null) {
            System.setProperty("jdk.net.unixdomain.tmpdir", System.getProperty("user.home"));
        }
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    private Jamendo() {}

    static List<Morceau> rechercher(String texte) throws IOException, InterruptedException {
        return avecReprise("&search=" + URLEncoder.encode(texte, StandardCharsets.UTF_8));
    }

    // Les titres les plus écoutés de la semaine
    static List<Morceau> tendances() throws IOException, InterruptedException {
        return avecReprise("&order=popularity_week");
    }

    // Jamendo répond parfois « 0 résultat » à tort (environ une fois sur trois) :
    // une liste vide n'est donc acceptée qu'après plusieurs essais.
    private static List<Morceau> avecReprise(String parametres) throws IOException, InterruptedException {
        List<Morceau> trouves = appeler(parametres);
        for (int essai = 1; essai < ESSAIS && trouves.isEmpty(); essai++) {
            Thread.sleep(300);
            trouves = appeler(parametres);
        }
        return trouves;
    }

    private static List<Morceau> appeler(String parametres) throws IOException, InterruptedException {
        String url = BASE + "?client_id=" + URLEncoder.encode(Config.clientJamendo(), StandardCharsets.UTF_8)
                   + "&format=json&limit=" + LIMITE + "&audioformat=mp32" + parametres;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> rep = CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rep.statusCode() != 200) {
            throw new IOException("Jamendo a répondu " + rep.statusCode());
        }

        JSONObject racine = new JSONObject(rep.body());
        JSONObject entetes = racine.optJSONObject("headers");
        if (entetes != null && entetes.optInt("code", 0) != 0) {
            throw new IOException("Jamendo : " + entetes.optString("error_message", "erreur inconnue"));
        }

        JSONArray resultats = racine.optJSONArray("results");
        List<Morceau> morceaux = new ArrayList<>();
        if (resultats == null) return morceaux;
        for (int i = 0; i < resultats.length(); i++) {
            JSONObject m = resultats.getJSONObject(i);
            String audio = m.optString("audio", "");
            if (audio.isEmpty()) continue;   // morceau non diffusable
            morceaux.add(new Morceau(
                m.optString("id"),
                m.optString("name", "Sans titre"),
                m.optString("artist_name", "Artiste inconnu"),
                m.optString("album_name", ""),
                m.optInt("duration", 0),
                audio,
                m.optString("album_image", "")));
        }
        return morceaux;
    }
}
