import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javafx.application.Platform;

public final class App {
    private static final Preferences PREFS = Preferences.userRoot().node("lecteur-musique");
    private static final int MDP_MIN = 6;

    private final JFrame frame = new JFrame("Lecteur musique");
    private final CardLayout cards = new CardLayout();
    private final JPanel conteneur = new JPanel(cards);
    private final PageLecteur pageLecteur = new PageLecteur(PREFS, this::seDeconnecter);

    // Connexion
    private final ChampArrondi champNom = new ChampArrondi(10, 12);
    private final MdpArrondi champMdp = new MdpArrondi(10, 12);
    private final BoutonArrondi connexion = new BoutonArrondi("Connexion", 38);
    private final JLabel messageLogin = Theme.label(" ", 12, false, Theme.ERREUR);

    // Inscription
    private final ChampArrondi registerNom = new ChampArrondi(10, 12);
    private final MdpArrondi registerMdp = new MdpArrondi(10, 12);
    private final MdpArrondi registerConfirmation = new MdpArrondi(10, 12);
    private final BoutonArrondi registerInscription = new BoutonArrondi("S'inscrire", 38);
    private final JLabel messageRegister = Theme.label(" ", 12, false, Theme.ERREUR);

    public static void main(String[] args) {
        Platform.startup(() -> { });          // démarre JavaFX (utilisé uniquement pour le son)
        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(() -> new App().demarrer());
    }

    private void demarrer() {
        UIManager.put("ToolTip.background", Theme.CHAMP);
        UIManager.put("ToolTip.foreground", Theme.TEXTE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));
        UIManager.put("ToolTip.font", Theme.police(11, false));

        conteneur.setBackground(Theme.FOND);
        conteneur.add(construireConnexion(), "LOGIN");
        conteneur.add(construireInscription(), "REGISTER");
        conteneur.add(pageLecteur, "MENU");

        frame.getContentPane().setBackground(Theme.FOND);
        frame.add(conteneur);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                pageLecteur.fermer();
                frame.dispose();
                Platform.exit();
                System.exit(0);
            }
        });
        frame.setSize(400, 660);
        frame.setMinimumSize(new Dimension(360, 560));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Identifiant pré-rempli : on place le curseur directement sur le mot de passe
        champNom.setText(PREFS.get("dernierLogin", ""));
        (champNom.getText().isEmpty() ? champNom : champMdp).requestFocusInWindow();
    }

    // ------------------------------------------------------------------ écran de connexion

    private JPanel construireConnexion() {
        JPanel page = Theme.page();

        JLabel icone = Theme.label("♪", 30, false, Theme.ACCENT);
        icone.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 30));
        champMdp.setEchoChar('•');
        BoutonLien inscription = new BoutonLien(
            "<html><span style='color:#A0A3A8'>Pas de compte ?</span> S'inscrire</html>", Theme.ACCENT);

        page.add(Box.createVerticalGlue());
        page.add(Theme.centre(icone, 40));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.centre(Theme.label("Lecteur musique", 18, true, Theme.TEXTE), 26));
        page.add(Theme.centre(Theme.label("Connecte-toi pour écouter", 12, false, Theme.TEXTE_2), 18));
        page.add(Box.createVerticalStrut(28));
        page.add(Theme.pleineLargeur(Theme.label("Identifiant", 12, false, Theme.TEXTE_2), 20));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.pleineLargeur(champNom, 36));
        page.add(Box.createVerticalStrut(12));
        page.add(Theme.pleineLargeur(Theme.label("Mot de passe", 12, false, Theme.TEXTE_2), 20));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.pleineLargeur(champMdp, 36));
        page.add(Box.createVerticalStrut(8));
        page.add(Theme.centre(messageLogin, 20));
        page.add(Box.createVerticalStrut(8));
        page.add(Theme.pleineLargeur(connexion, 38));
        page.add(Box.createVerticalStrut(10));
        page.add(Theme.pleineLargeur(inscription, 24));
        page.add(Box.createVerticalGlue());
        page.add(Box.createVerticalGlue());

        connexion.addActionListener(e -> seConnecter());
        champNom.addActionListener(e -> champMdp.requestFocusInWindow());
        champMdp.addActionListener(e -> seConnecter());      // Entrée valide le formulaire
        inscription.addActionListener(e -> {
            messageRegister.setText(" ");
            cards.show(conteneur, "REGISTER");
            registerNom.requestFocusInWindow();
        });
        return page;
    }

    private void seConnecter() {
        if (!connexion.isEnabled()) return;
        String nom = champNom.getText().trim();
        char[] mdp = champMdp.getPassword();
        if (nom.isEmpty() || mdp.length == 0) {
            Arrays.fill(mdp, '\0');
            afficher(messageLogin, "Saisis ton identifiant et ton mot de passe", true);
            return;
        }

        connexion.setEnabled(false);
        afficher(messageLogin, "Connexion...", false);
        Tache.lancer(() -> {
            try {
                return BaseDonnees.authentifier(nom, mdp);
            } finally {
                Arrays.fill(mdp, '\0');
            }
        }, utilisateur -> {
            connexion.setEnabled(true);
            if (utilisateur == null) {
                afficher(messageLogin, "Identifiant ou mot de passe incorrect", true);
                champMdp.selectAll();
                champMdp.requestFocusInWindow();
                return;
            }
            messageLogin.setText(" ");
            champMdp.setText("");
            PREFS.put("dernierLogin", utilisateur.login());
            champNom.setText(utilisateur.login());
            pageLecteur.ouvrir(utilisateur);
            cards.show(conteneur, "MENU");
        }, ex -> {
            connexion.setEnabled(true);
            afficher(messageLogin, "Impossible de joindre la base de données", true);
        });
    }

    private void seDeconnecter() {
        pageLecteur.fermer();
        champMdp.setText("");
        messageLogin.setText(" ");
        cards.show(conteneur, "LOGIN");
        champMdp.requestFocusInWindow();
    }

    // ------------------------------------------------------------------ écran d'inscription

    private JPanel construireInscription() {
        JPanel page = Theme.page();

        BoutonLien retour = new BoutonLien("← Retour", Theme.TEXTE_2);
        retour.setHorizontalAlignment(SwingConstants.LEFT);
        registerMdp.setEchoChar('•');
        registerConfirmation.setEchoChar('•');

        page.add(Theme.pleineLargeur(retour, 22));
        page.add(Box.createVerticalStrut(18));
        page.add(Theme.pleineLargeur(Theme.label("Créer un compte", 18, true, Theme.TEXTE), 26));
        page.add(Theme.pleineLargeur(
            Theme.label("Choisis un identifiant et un mot de passe", 12, false, Theme.TEXTE_2), 18));
        page.add(Box.createVerticalStrut(24));
        page.add(Theme.pleineLargeur(Theme.label("Identifiant", 12, false, Theme.TEXTE_2), 20));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.pleineLargeur(registerNom, 36));
        page.add(Box.createVerticalStrut(12));
        page.add(Theme.pleineLargeur(
            Theme.label("Mot de passe (" + MDP_MIN + " caractères minimum)", 12, false, Theme.TEXTE_2), 20));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.pleineLargeur(registerMdp, 36));
        page.add(Box.createVerticalStrut(12));
        page.add(Theme.pleineLargeur(Theme.label("Confirme le mot de passe", 12, false, Theme.TEXTE_2), 20));
        page.add(Box.createVerticalStrut(4));
        page.add(Theme.pleineLargeur(registerConfirmation, 36));
        page.add(Box.createVerticalStrut(8));
        page.add(Theme.centre(messageRegister, 20));
        page.add(Box.createVerticalStrut(8));
        page.add(Theme.pleineLargeur(registerInscription, 38));

        retour.addActionListener(e -> cards.show(conteneur, "LOGIN"));
        registerInscription.addActionListener(e -> sInscrire());
        registerNom.addActionListener(e -> registerMdp.requestFocusInWindow());
        registerMdp.addActionListener(e -> registerConfirmation.requestFocusInWindow());
        registerConfirmation.addActionListener(e -> sInscrire());
        return page;
    }

    private void sInscrire() {
        if (!registerInscription.isEnabled()) return;
        String nom = registerNom.getText().trim();
        char[] mdp = registerMdp.getPassword();
        char[] confirmation = registerConfirmation.getPassword();

        String probleme = null;
        if (nom.length() < 3 || nom.length() > 50) {
            probleme = "L'identifiant doit faire entre 3 et 50 caractères";
        } else if (mdp.length < MDP_MIN) {
            probleme = "Le mot de passe doit faire au moins " + MDP_MIN + " caractères";
        } else if (!Arrays.equals(mdp, confirmation)) {
            probleme = "Les deux mots de passe sont différents";
        }
        Arrays.fill(confirmation, '\0');
        if (probleme != null) {
            Arrays.fill(mdp, '\0');
            afficher(messageRegister, probleme, true);
            return;
        }

        registerInscription.setEnabled(false);
        afficher(messageRegister, "Création du compte...", false);
        Tache.lancer(() -> {
            try {
                return BaseDonnees.inscrire(nom, mdp);
            } finally {
                Arrays.fill(mdp, '\0');
            }
        }, cree -> {
            registerInscription.setEnabled(true);
            if (!cree) {
                afficher(messageRegister, "Cet identifiant est déjà pris", true);
                return;
            }
            registerNom.setText("");
            registerMdp.setText("");
            registerConfirmation.setText("");
            messageRegister.setText(" ");
            champNom.setText(nom);
            champMdp.setText("");
            afficher(messageLogin, "Compte créé, tu peux te connecter", false);
            cards.show(conteneur, "LOGIN");
            champMdp.requestFocusInWindow();
        }, ex -> {
            registerInscription.setEnabled(true);
            afficher(messageRegister, "Impossible de joindre la base de données", true);
        });
    }

    private static void afficher(JLabel label, String texte, boolean erreur) {
        label.setForeground(erreur ? Theme.ERREUR : Theme.TEXTE_2);
        label.setText(texte);
    }
}
