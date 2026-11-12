import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

// Pochettes d'album : téléchargées en arrière-plan, arrondies et gardées en mémoire.
final class Images {
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, ImageIcon> VIDES = new ConcurrentHashMap<>();
    private static final Set<String> EN_COURS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "pochettes");
        t.setDaemon(true);
        return t;
    });

    private Images() {}

    // Renvoie tout de suite la pochette (ou une pochette vide), et appelle quandPrete
    // sur le thread Swing une fois l'image téléchargée.
    static ImageIcon pochette(String url, int taille, Runnable quandPrete) {
        if (url == null || url.isBlank()) return vide(taille);
        String cle = taille + "|" + url;
        ImageIcon icone = CACHE.get(cle);
        if (icone != null) return icone;

        if (EN_COURS.add(cle)) {
            POOL.execute(() -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build();
                    HttpResponse<InputStream> rep = Jamendo.CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream in = rep.body()) {
                        BufferedImage source = rep.statusCode() == 200 ? ImageIO.read(in) : null;
                        // En cas d'échec on garde la pochette vide pour ne pas réessayer en boucle.
                        CACHE.put(cle, source == null ? vide(taille) : new ImageIcon(arrondir(source, taille)));
                    }
                } catch (Exception e) {
                    CACHE.put(cle, vide(taille));
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                } finally {
                    EN_COURS.remove(cle);
                }
                if (quandPrete != null) SwingUtilities.invokeLater(quandPrete);
            });
        }
        return vide(taille);
    }

    static ImageIcon vide(int taille) {
        return VIDES.computeIfAbsent(taille, t -> {
            BufferedImage img = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.CHAMP);
            g.fill(new RoundRectangle2D.Float(0, 0, t, t, 10, 10));
            g.setColor(Theme.TEXTE_2);
            g.setFont(new Font("Segoe UI Symbol", Font.PLAIN, t / 2));
            FontMetrics fm = g.getFontMetrics();
            String note = "♪";
            g.drawString(note, (t - fm.stringWidth(note)) / 2, (t + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
            return new ImageIcon(img);
        });
    }

    private static BufferedImage arrondir(BufferedImage source, int taille) {
        BufferedImage img = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setClip(new RoundRectangle2D.Float(0, 0, taille, taille, 10, 10));
        // Réduction en deux temps pour un rendu plus net sur les petites tailles
        Image reduite = source.getScaledInstance(taille, taille, Image.SCALE_SMOOTH);
        g.drawImage(new ImageIcon(reduite).getImage(), 0, 0, null);
        g.dispose();
        return img;
    }
}
