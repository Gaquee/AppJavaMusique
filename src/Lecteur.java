import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javax.swing.SwingUtilities;

// Lecture audio (JavaFX Media). Les méthodes peuvent être appelées depuis Swing ;
// l'écouteur est toujours rappelé sur le thread Swing.
final class Lecteur {

    interface Ecouteur {
        void etat(boolean enLecture);
        void progression(double secondes, double total);
        void fin();
        void erreur(String message);
    }

    private final Ecouteur ecouteur;
    private MediaPlayer joueur;        // manipulé uniquement sur le thread JavaFX
    private double dureeSecours;       // durée annoncée par Jamendo, si le flux ne la donne pas
    private volatile double volume = 0.8;

    Lecteur(Ecouteur ecouteur) {
        this.ecouteur = ecouteur;
    }

    void jouer(Morceau m) {
        Platform.runLater(() -> {
            liberer();
            try {
                MediaPlayer mp = new MediaPlayer(new Media(m.url));
                joueur = mp;
                dureeSecours = m.duree;
                mp.setVolume(volume);
                mp.currentTimeProperty().addListener((obs, avant, t) -> {
                    if (mp != joueur) return;
                    double pos = t.toSeconds(), total = total();
                    swing(() -> ecouteur.progression(pos, total));
                });
                mp.statusProperty().addListener((obs, avant, statut) -> {
                    if (mp != joueur) return;
                    boolean enLecture = statut == MediaPlayer.Status.PLAYING;
                    swing(() -> ecouteur.etat(enLecture));
                });
                mp.setOnEndOfMedia(() -> {
                    if (mp == joueur) swing(ecouteur::fin);
                });
                mp.setOnError(() -> {
                    if (mp != joueur) return;
                    String msg = mp.getError() == null ? "erreur inconnue" : mp.getError().getMessage();
                    liberer();
                    swing(() -> ecouteur.erreur(msg));
                });
                mp.play();
            } catch (RuntimeException ex) {   // URL invalide, format non pris en charge...
                liberer();
                swing(() -> ecouteur.erreur(ex.getMessage()));
            }
        });
    }

    // Lecture <-> pause
    void basculer() {
        Platform.runLater(() -> {
            if (joueur == null) return;
            if (joueur.getStatus() == MediaPlayer.Status.PLAYING) {
                joueur.pause();
            } else {
                joueur.play();
            }
        });
    }

    // position entre 0 et 1
    void allerA(double fraction) {
        Platform.runLater(() -> {
            if (joueur == null) return;
            double total = total();
            if (total > 0) joueur.seek(Duration.seconds(fraction * total));
        });
    }

    void setVolume(double v) {
        volume = Math.max(0, Math.min(1, v));
        Platform.runLater(() -> {
            if (joueur != null) joueur.setVolume(volume);
        });
    }

    double getVolume() {
        return volume;
    }

    void arreter() {
        Platform.runLater(() -> {
            liberer();
            swing(() -> ecouteur.etat(false));
        });
    }

    private double total() {
        Duration d = joueur.getMedia().getDuration();
        return d == null || d.isUnknown() || d.isIndefinite() ? dureeSecours : d.toSeconds();
    }

    private void liberer() {
        MediaPlayer ancien = joueur;
        joueur = null;
        if (ancien != null) {
            ancien.stop();
            ancien.dispose();
        }
    }

    private static void swing(Runnable r) {
        SwingUtilities.invokeLater(r);
    }
}
