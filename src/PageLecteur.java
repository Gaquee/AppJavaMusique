import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.prefs.Preferences;

// Écran principal : recherche, favoris et barre de lecture.
final class PageLecteur extends JPanel implements Lecteur.Ecouteur {
    private static final int POCHETTE = 46;

    private final Preferences prefs;
    private final Lecteur lecteur = new Lecteur(this);
    private final Random hasard = new Random();

    // En-tête
    private final JLabel titrePage = Theme.label("Bonjour", 16, true, Theme.TEXTE);
    private final ChampArrondi recherche = new ChampArrondi(10, 12);
    private final BoutonOnglet ongletDecouvrir = new BoutonOnglet("Découvrir");
    private final BoutonOnglet ongletFavoris = new BoutonOnglet("Favoris");

    // Liste
    private final DefaultListModel<Morceau> modele = new DefaultListModel<>();
    private final ListeMorceaux liste = new ListeMorceaux(modele);
    private final JLabel message = Theme.label("", 12, false, Theme.TEXTE_2);
    private final CardLayout cartesCentre = new CardLayout();
    private final JPanel centre = new JPanel(cartesCentre);

    // Barre de lecture
    private final JLabel pochette = new JLabel(Images.vide(POCHETTE));
    private final JLabel lecteurTitre = Theme.label("Aucun morceau", 13, true, Theme.TEXTE);
    private final JLabel lecteurArtiste = Theme.label("Double-clique sur un titre pour l'écouter", 11, false, Theme.TEXTE_2);
    private final BoutonIcone boutonFavori = new BoutonIcone(BoutonIcone.Type.COEUR, 34, false, "Ajouter aux favoris");
    private final JLabel temps = Theme.label("0:00", 11, false, Theme.TEXTE_2);
    private final JLabel dureeTotale = Theme.label("0:00", 11, false, Theme.TEXTE_2);
    private final Curseur progression = new Curseur();
    private final BoutonIcone boutonLecture = new BoutonIcone(BoutonIcone.Type.LECTURE, 40, true, "Lecture / pause");
    private final BoutonIcone boutonAleatoire = new BoutonIcone(BoutonIcone.Type.ALEATOIRE, 30, false, "Lecture aléatoire");
    private final BoutonIcone boutonRepeter = new BoutonIcone(BoutonIcone.Type.REPETER, 30, false, "Répéter la liste");
    private final Curseur volume = new Curseur();

    // État
    private Utilisateur utilisateur;
    private boolean surFavoris;
    private List<Morceau> resultats = List.of();          // contenu de l'onglet Découvrir
    private String messageResultats = "";
    private final List<Morceau> favoris = new ArrayList<>();
    private final Set<String> idsFavoris = new HashSet<>();
    private boolean favorisDisponibles = true;
    private List<Morceau> file = List.of();               // liste en cours de lecture
    private int indexFile = -1;
    private Morceau enCours;
    private double dureeEnCours;
    private boolean aRelancer;                            // lecteur libéré (fin de liste, erreur) : « lecture » relance le morceau
    private int numeroRecherche;                          // pour ignorer les réponses périmées

    PageLecteur(Preferences prefs, Runnable surDeconnexion) {
        super(new BorderLayout(0, 10));
        this.prefs = prefs;
        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        add(construireHaut(surDeconnexion), BorderLayout.NORTH);
        add(construireCentre(), BorderLayout.CENTER);
        add(construireBarreLecture(), BorderLayout.SOUTH);

        double v = prefs.getDouble("volume", 0.8);
        lecteur.setVolume(v);
        volume.setValeur(v);
        boutonAleatoire.setActif(prefs.getBoolean("aleatoire", false));
        boutonRepeter.setActif(prefs.getBoolean("repeter", false));
        progression.setEnabled(false);
    }

    // ------------------------------------------------------------------ construction

    private JComponent construireHaut(Runnable surDeconnexion) {
        JPanel entete = new JPanel(new BorderLayout());
        entete.setOpaque(false);
        BoutonLien deconnexion = new BoutonLien("Déconnexion", Theme.TEXTE_2);
        deconnexion.addActionListener(e -> surDeconnexion.run());
        entete.add(titrePage, BorderLayout.WEST);
        entete.add(deconnexion, BorderLayout.EAST);

        recherche.setPreferredSize(new Dimension(10, 36));
        recherche.addActionListener(e -> {
            if (!surFavoris) chercher(recherche.getText().trim());
        });
        recherche.getDocument().addDocumentListener(new DocumentListener() {   // filtre instantané des favoris
            @Override public void insertUpdate(DocumentEvent e)  { if (surFavoris) rafraichirListe(); }
            @Override public void removeUpdate(DocumentEvent e)  { if (surFavoris) rafraichirListe(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        JPanel onglets = new JPanel(new GridLayout(1, 2, 8, 0));
        onglets.setOpaque(false);
        onglets.setPreferredSize(new Dimension(10, 30));
        onglets.add(ongletDecouvrir);
        onglets.add(ongletFavoris);
        ongletDecouvrir.addActionListener(e -> choisirOnglet(false));
        ongletFavoris.addActionListener(e -> choisirOnglet(true));

        JPanel haut = new JPanel(new BorderLayout(0, 10));
        haut.setOpaque(false);
        haut.add(entete, BorderLayout.NORTH);
        haut.add(recherche, BorderLayout.CENTER);
        haut.add(onglets, BorderLayout.SOUTH);
        return haut;
    }

    private JComponent construireCentre() {
        liste.setCellRenderer(new RenduMorceau(
            m -> idsFavoris.contains(m.id),
            m -> enCours != null && enCours.id.equals(m.id)));
        liste.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = liste.locationToIndex(e.getPoint());
                if (index < 0 || !liste.getCellBounds(index, index).contains(e.getPoint())) return;
                if (liste.surCoeur(e.getPoint())) {
                    if (e.getClickCount() == 1) basculerFavori(modele.get(index));
                } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    jouerDepuisListe(index);
                }
            }
        });
        liste.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "jouer");
        liste.getActionMap().put("jouer", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (liste.getSelectedIndex() >= 0) jouerDepuisListe(liste.getSelectedIndex());
            }
        });

        JScrollPane scroll = new JScrollPane(liste);
        Theme.styliserScroll(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        message.setHorizontalAlignment(SwingConstants.CENTER);

        centre.setOpaque(false);
        centre.add(scroll, "LISTE");
        centre.add(message, "MESSAGE");

        PanneauArrondi zone = new PanneauArrondi(new BorderLayout(), Theme.SURFACE, 12);
        zone.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        zone.add(centre);
        return zone;
    }

    private JComponent construireBarreLecture() {
        // Ligne 1 : pochette, titre + artiste, favori
        JPanel infos = new JPanel(new GridLayout(2, 1));
        infos.setOpaque(false);
        infos.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        infos.add(lecteurTitre);
        infos.add(lecteurArtiste);

        JPanel ligneInfos = new JPanel(new BorderLayout(10, 0));
        ligneInfos.setOpaque(false);
        ligneInfos.add(pochette, BorderLayout.WEST);
        ligneInfos.add(infos, BorderLayout.CENTER);
        ligneInfos.add(centrer(boutonFavori), BorderLayout.EAST);
        boutonFavori.setEnabled(false);
        boutonFavori.addActionListener(e -> {
            if (enCours != null) basculerFavori(enCours);
        });

        // Ligne 2 : temps écoulé, progression, durée
        Dimension largeurTemps = new Dimension(32, 18);
        temps.setPreferredSize(largeurTemps);
        dureeTotale.setPreferredSize(largeurTemps);
        dureeTotale.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel ligneProgression = new JPanel(new BorderLayout(2, 0));
        ligneProgression.setOpaque(false);
        ligneProgression.add(temps, BorderLayout.WEST);
        ligneProgression.add(progression, BorderLayout.CENTER);
        ligneProgression.add(dureeTotale, BorderLayout.EAST);
        progression.surGlissement(v -> temps.setText(Morceau.formaterDuree(v * dureeEnCours)));
        progression.surRelachement(lecteur::allerA);

        // Ligne 3 : modes, commandes, volume
        BoutonIcone precedent = new BoutonIcone(BoutonIcone.Type.PRECEDENT, 34, false, "Morceau précédent");
        BoutonIcone suivant = new BoutonIcone(BoutonIcone.Type.SUIVANT, 34, false, "Morceau suivant");
        precedent.addActionListener(e -> precedent());
        suivant.addActionListener(e -> suivant(true));
        boutonLecture.addActionListener(e -> lectureOuPause());
        boutonAleatoire.addActionListener(e -> {
            boutonAleatoire.setActif(!boutonAleatoire.estActif());
            prefs.putBoolean("aleatoire", boutonAleatoire.estActif());
        });
        boutonRepeter.addActionListener(e -> {
            boutonRepeter.setActif(!boutonRepeter.estActif());
            prefs.putBoolean("repeter", boutonRepeter.estActif());
        });
        volume.setToolTipText("Volume");
        volume.surGlissement(lecteur::setVolume);
        volume.surRelachement(v -> prefs.putDouble("volume", v));

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modes.setOpaque(false);
        modes.add(boutonAleatoire);
        modes.add(boutonRepeter);

        JPanel commandes = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        commandes.setOpaque(false);
        commandes.add(centrer(precedent));
        commandes.add(boutonLecture);
        commandes.add(centrer(suivant));

        // Même largeur à gauche et à droite pour que les commandes restent centrées
        Dimension cote = new Dimension(84, 40);
        JPanel gauche = centrer(modes);
        gauche.setPreferredSize(cote);
        JPanel droite = new JPanel(new BorderLayout());
        droite.setOpaque(false);
        droite.setPreferredSize(cote);
        droite.add(volume, BorderLayout.CENTER);

        JPanel ligneCommandes = new JPanel(new BorderLayout());
        ligneCommandes.setOpaque(false);
        ligneCommandes.add(gauche, BorderLayout.WEST);
        ligneCommandes.add(commandes, BorderLayout.CENTER);
        ligneCommandes.add(droite, BorderLayout.EAST);

        PanneauArrondi barre = new PanneauArrondi(null, Theme.SURFACE, 12);
        barre.setLayout(new BoxLayout(barre, BoxLayout.Y_AXIS));
        barre.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        barre.add(ligneInfos);
        barre.add(Box.createVerticalStrut(6));
        barre.add(ligneProgression);
        barre.add(Box.createVerticalStrut(4));
        barre.add(ligneCommandes);
        return barre;
    }

    // Centre un composant verticalement sans l'étirer
    private static JPanel centrer(JComponent c) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    // ------------------------------------------------------------------ session

    // Appelé après une connexion réussie
    void ouvrir(Utilisateur u) {
        utilisateur = u;
        titrePage.setText("Bonjour " + u.login());
        favoris.clear();
        idsFavoris.clear();
        favorisDisponibles = true;
        resultats = List.of();
        recherche.setText("");
        choisirOnglet(false);
        chercher("");

        Tache.lancer(() -> BaseDonnees.favoris(u.id()), charges -> {
            if (utilisateur != u) return;
            favoris.addAll(charges);
            charges.forEach(m -> idsFavoris.add(m.id));
            majBoutonFavori();
            rafraichirListe();
        }, ex -> {
            if (utilisateur != u) return;
            favorisDisponibles = false;
            rafraichirListe();
        });
    }

    // Appelé à la déconnexion et à la fermeture de la fenêtre
    void fermer() {
        utilisateur = null;
        numeroRecherche++;
        lecteur.arreter();
        file = List.of();
        indexFile = -1;
        enCours = null;
        afficherMorceauEnCours();
    }

    // ------------------------------------------------------------------ recherche et onglets

    private void choisirOnglet(boolean favorisChoisis) {
        boolean change = surFavoris != favorisChoisis;
        surFavoris = favorisChoisis;
        ongletDecouvrir.setSelectionne(!surFavoris);
        ongletFavoris.setSelectionne(surFavoris);
        recherche.setPlaceholder(surFavoris ? "Filtrer mes favoris" : "Rechercher un titre, un artiste...");
        if (change) recherche.setText("");
        rafraichirListe();
    }

    // Texte vide = titres populaires de la semaine
    private void chercher(String texte) {
        int numero = ++numeroRecherche;
        resultats = List.of();
        messageResultats = texte.isEmpty() ? "Chargement des titres du moment..." : "Recherche en cours...";
        if (!surFavoris) rafraichirListe();

        Tache.lancer(() -> texte.isEmpty() ? Jamendo.tendances() : Jamendo.rechercher(texte), trouves -> {
            if (numero != numeroRecherche) return;
            resultats = trouves;
            messageResultats = texte.isEmpty() ? "Aucun titre à proposer pour le moment"
                                               : "Aucun résultat pour « " + texte + " »";
            if (!surFavoris) rafraichirListe();
        }, ex -> {
            if (numero != numeroRecherche) return;
            messageResultats = "<html><center>Impossible de joindre Jamendo.<br>Vérifie ta connexion puis réessaie.</center></html>";
            if (!surFavoris) rafraichirListe();
        });
    }

    // Remplit la liste selon l'onglet actif (et le filtre pour les favoris)
    private void rafraichirListe() {
        List<Morceau> contenu;
        String siVide;
        if (surFavoris) {
            String filtre = recherche.getText().trim().toLowerCase(Locale.ROOT);
            contenu = favoris.stream()
                .filter(m -> filtre.isEmpty()
                    || m.titre.toLowerCase(Locale.ROOT).contains(filtre)
                    || m.artiste.toLowerCase(Locale.ROOT).contains(filtre))
                .toList();
            if (!favorisDisponibles) {
                siVide = "<html><center>Favoris indisponibles.<br>Vérifie la base de données (sql/schema.sql).</center></html>";
            } else if (favoris.isEmpty()) {
                siVide = "<html><center>Aucun favori pour l'instant.<br>Clique sur le coeur d'un titre pour l'ajouter.</center></html>";
            } else {
                siVide = "Aucun favori ne correspond";
            }
        } else {
            contenu = resultats;
            siVide = messageResultats;
        }

        Morceau selection = liste.getSelectedValue();
        modele.clear();
        modele.addAll(contenu);
        if (selection != null) {
            for (int i = 0; i < contenu.size(); i++) {
                if (contenu.get(i).id.equals(selection.id)) { liste.setSelectedIndex(i); break; }
            }
        }
        message.setText(siVide);
        cartesCentre.show(centre, contenu.isEmpty() ? "MESSAGE" : "LISTE");
    }

    // ------------------------------------------------------------------ favoris

    private void basculerFavori(Morceau m) {
        Utilisateur u = utilisateur;
        if (u == null || !favorisDisponibles) return;
        boolean ajout = !idsFavoris.contains(m.id);
        appliquerFavori(m, ajout);

        Tache.lancer(() -> {
            if (ajout) BaseDonnees.ajouterFavori(u.id(), m); else BaseDonnees.retirerFavori(u.id(), m.id);
            return null;
        }, ok -> { }, ex -> {
            if (utilisateur != u) return;
            appliquerFavori(m, !ajout);   // on annule : l'enregistrement a échoué
            JOptionPane.showMessageDialog(this, "Impossible d'enregistrer le favori (base de données injoignable).",
                "Favoris", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void appliquerFavori(Morceau m, boolean favori) {
        if (favori) {
            if (idsFavoris.add(m.id)) favoris.add(0, m);
        } else {
            idsFavoris.remove(m.id);
            favoris.removeIf(f -> f.id.equals(m.id));
        }
        majBoutonFavori();
        if (surFavoris) rafraichirListe(); else liste.repaint();
    }

    private void majBoutonFavori() {
        boolean favori = enCours != null && idsFavoris.contains(enCours.id);
        boutonFavori.setEnabled(enCours != null);
        boutonFavori.setActif(favori);
        boutonFavori.setToolTipText(favori ? "Retirer des favoris" : "Ajouter aux favoris");
    }

    // ------------------------------------------------------------------ lecture

    private void jouerDepuisListe(int index) {
        file = new ArrayList<>(java.util.Collections.list(modele.elements()));
        jouerIndex(index);
    }

    private void jouerIndex(int index) {
        indexFile = index;
        enCours = file.get(index);
        dureeEnCours = enCours.duree;
        afficherMorceauEnCours();
        lecteur.jouer(enCours);
    }

    private void lectureOuPause() {
        if (enCours != null && aRelancer) {
            jouerIndex(indexFile);
        } else if (enCours != null) {
            lecteur.basculer();
        } else if (!modele.isEmpty()) {   // rien de chargé : on lance la sélection, sinon le premier titre
            jouerDepuisListe(Math.max(0, liste.getSelectedIndex()));
        }
    }

    // manuel = clic sur « suivant » ; sinon fin naturelle du morceau
    private void suivant(boolean manuel) {
        if (file.isEmpty()) return;
        int prochain;
        if (boutonAleatoire.estActif() && file.size() > 1) {
            do { prochain = hasard.nextInt(file.size()); } while (prochain == indexFile);
        } else {
            prochain = indexFile + 1;
            if (prochain >= file.size()) {
                if (!manuel && !boutonRepeter.estActif()) {   // fin de la liste
                    lecteur.arreter();
                    progression.setValeur(0);
                    temps.setText("0:00");
                    aRelancer = true;
                    return;
                }
                prochain = 0;
            }
        }
        jouerIndex(prochain);
    }

    private void precedent() {
        if (file.isEmpty()) return;
        // Comme sur la plupart des lecteurs : après quelques secondes, « précédent » relance le morceau
        if (progression.getValeur() * dureeEnCours > 3 || file.size() == 1) {
            lecteur.allerA(0);
            return;
        }
        jouerIndex((indexFile - 1 + file.size()) % file.size());
    }

    private void afficherMorceauEnCours() {
        aRelancer = false;
        progression.setValeur(0);
        progression.setEnabled(enCours != null);
        temps.setText("0:00");
        lecteurArtiste.setForeground(Theme.TEXTE_2);
        if (enCours == null) {
            lecteurTitre.setText("Aucun morceau");
            lecteurArtiste.setText("Double-clique sur un titre pour l'écouter");
            dureeTotale.setText("0:00");
            pochette.setIcon(Images.vide(POCHETTE));
            boutonLecture.setType(BoutonIcone.Type.LECTURE);
        } else {
            Morceau m = enCours;
            lecteurTitre.setText(m.titre);
            lecteurTitre.setToolTipText(m.titre);
            lecteurArtiste.setText(m.album == null || m.album.isBlank() ? m.artiste : m.artiste + " · " + m.album);
            dureeTotale.setText(Morceau.formaterDuree(m.duree));
            pochette.setIcon(Images.pochette(m.image, POCHETTE, () -> {
                if (enCours == m) pochette.setIcon(Images.pochette(m.image, POCHETTE, null));
            }));
        }
        majBoutonFavori();
        liste.repaint();
    }

    // ------------------------------------------------------------------ Lecteur.Ecouteur (thread Swing)

    @Override
    public void etat(boolean enLecture) {
        boutonLecture.setType(enLecture ? BoutonIcone.Type.PAUSE : BoutonIcone.Type.LECTURE);
    }

    @Override
    public void progression(double secondes, double total) {
        if (enCours == null) return;
        if (total > 0) {
            dureeEnCours = total;
            dureeTotale.setText(Morceau.formaterDuree(total));
        }
        if (!progression.estEnGlissement()) {
            progression.setValeur(dureeEnCours > 0 ? secondes / dureeEnCours : 0);
            temps.setText(Morceau.formaterDuree(secondes));
        }
    }

    @Override
    public void fin() {
        suivant(false);
    }

    @Override
    public void erreur(String detail) {
        System.err.println("Lecture impossible : " + detail);
        boutonLecture.setType(BoutonIcone.Type.LECTURE);
        aRelancer = true;
        lecteurArtiste.setForeground(Theme.ERREUR);
        lecteurArtiste.setText("Lecture impossible (connexion ou fichier indisponible)");
    }
}
