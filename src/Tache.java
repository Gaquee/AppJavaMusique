import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

// Exécute un travail lent (réseau, base de données) en arrière-plan,
// puis appelle succes ou echec sur le thread Swing.
final class Tache {
    private Tache() {}

    static <T> void lancer(Callable<T> travail, Consumer<T> succes, Consumer<Exception> echec) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception {
                return travail.call();
            }

            @Override protected void done() {
                T resultat;
                try {
                    resultat = get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    cause.printStackTrace();
                    echec.accept(cause instanceof Exception ex ? ex : new Exception(cause));
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    echec.accept(e);
                    return;
                }
                succes.accept(resultat);
            }
        }.execute();
    }
}
