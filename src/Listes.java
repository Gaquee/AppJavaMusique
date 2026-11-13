import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;
import java.util.function.Predicate;


// ===================== Liste dont les lignes prennent la largeur visible =====================
// Les textes trop longs sont tronqués au lieu de pousser les icônes de droite hors de l'écran.
class ListeLarge<T> extends JList<T> {
    public ListeLarge(ListModel<T> modele, int hauteurLigne) {
        super(modele);
        setOpaque(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFixedCellHeight(hauteurLigne);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    // Distance entre le point et le bord droit de la liste (pour savoir quelle icône est visée)
    public int depuisLaDroite(Point p) {
        return getWidth() - p.x;
    }
}


// ===================== Ligne de morceau : pochette, titre + artiste, durée, action =====================
class RenduMorceau extends JPanel implements ListCellRenderer<Morceau> {
    // Icône cliquable à droite de la ligne
    enum Action { AUCUNE, FAVORI, NON_FAVORI, AJOUTER, DEJA_PRESENT, SUPPRIMER, CONFIRMER }

    static final int HAUTEUR = 52;
    static final int ZONE_ACTION = 38;      // largeur cliquable à droite de la ligne
    private static final int POCHETTE = 38;

    private final JLabel pochette = new JLabel();
    private final JLabel titre = Theme.label("", 13, false, Theme.TEXTE);
    private final JLabel artiste = Theme.label("", 11, false, Theme.TEXTE_2);
    private final JLabel duree = Theme.label("", 11, false, Theme.TEXTE_2);
    private final Function<Morceau, Action> actionDe;
    private final Predicate<Morceau> estEnCours;
    private Action action = Action.AUCUNE;

    public RenduMorceau(Function<Morceau, Action> actionDe, Predicate<Morceau> estEnCours) {
        this.actionDe = actionDe;
        this.estEnCours = estEnCours;
        setLayout(new BorderLayout(10, 0));
        setBackground(Theme.CHAMP);

        JPanel textes = new JPanel(new GridLayout(2, 1));
        textes.setOpaque(false);
        textes.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        textes.add(titre);
        textes.add(artiste);

        // L'icône d'action est peinte dans la marge droite (voir paintComponent)
        duree.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ZONE_ACTION - 6));

        add(pochette, BorderLayout.WEST);
        add(textes, BorderLayout.CENTER);
        add(duree, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Morceau> liste, Morceau m,
                                                  int index, boolean selectionne, boolean focus) {
        titre.setText(m.titre);
        titre.setForeground(estEnCours.test(m) ? Theme.ACCENT : Theme.TEXTE);
        artiste.setText(m.genre.isEmpty() ? m.artiste : m.artiste + " · " + Catalogue.libelle(m.genre));
        duree.setText(m.duree > 0 ? Morceau.formaterDuree(m.duree) : "");
        pochette.setIcon(Images.pochette(m.image, POCHETTE, liste::repaint));
        action = actionDe.apply(m);
        setOpaque(selectionne);
        if (selectionne) {   // barre or à gauche
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.ACCENT),
                BorderFactory.createEmptyBorder(7, 5, 7, 6)));
        } else {
            setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 6));
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (action == Action.AUCUNE) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double t = 15;
        double x = getWidth() - ZONE_ACTION + (ZONE_ACTION - t) / 2.0 - 2, y = (getHeight() - t) / 2.0;
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x0 = (int) x, y0 = (int) y, x1 = (int) (x + t), y1 = (int) (y + t), mx = (int) (x + t / 2), my = (int) (y + t / 2);

        switch (action) {
            case FAVORI     -> Theme.dessinerCoeur(g2, x, y, t, true, Theme.ACCENT);
            case NON_FAVORI -> Theme.dessinerCoeur(g2, x, y, t, false, Theme.TEXTE_2);
            case AJOUTER -> {            // +
                g2.setColor(Theme.ACCENT);
                g2.drawLine(mx, y0 + 1, mx, y1 - 1);
                g2.drawLine(x0 + 1, my, x1 - 1, my);
            }
            case DEJA_PRESENT -> {       // coche
                g2.setColor(Theme.TEXTE_2);
                g2.drawPolyline(new int[]{x0 + 1, x0 + 6, x1 - 1}, new int[]{my + 1, y1 - 3, y0 + 3}, 3);
            }
            case SUPPRIMER, CONFIRMER -> {   // croix (rouge quand un second clic va supprimer)
                g2.setColor(action == Action.CONFIRMER ? Theme.ERREUR : Theme.TEXTE_2);
                g2.drawLine(x0 + 2, y0 + 2, x1 - 2, y1 - 2);
                g2.drawLine(x1 - 2, y0 + 2, x0 + 2, y1 - 2);
            }
            default -> { }
        }
        g2.dispose();
    }
}


// ===================== Ligne d'artiste : initiale, nom, nombre de titres, abonnement, favori =====================
class RenduArtiste extends JPanel implements ListCellRenderer<String> {
    static final int HAUTEUR = 52;
    static final int ZONE_COEUR = 38;        // à l'extrême droite
    static final int ZONE_ABONNEMENT = 92;   // juste avant le coeur
    private static final int AVATAR = 38;

    private final JLabel nom = Theme.label("", 13, false, Theme.TEXTE);
    private final JLabel detail = Theme.label("", 11, false, Theme.TEXTE_2);
    private final Function<String, Integer> nombreDeTitres;
    private final Predicate<String> estFavori;
    private final Predicate<String> estAbonne;
    private String initiale = "";
    private boolean favori, abonne;

    public RenduArtiste(Function<String, Integer> nombreDeTitres, Predicate<String> estFavori, Predicate<String> estAbonne) {
        this.nombreDeTitres = nombreDeTitres;
        this.estFavori = estFavori;
        this.estAbonne = estAbonne;
        setLayout(new GridLayout(2, 1));
        setBackground(Theme.CHAMP);
        add(nom);
        add(detail);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> liste, String artiste,
                                                  int index, boolean selectionne, boolean focus) {
        nom.setText(artiste);
        int n = nombreDeTitres.apply(artiste);
        detail.setText(n + (n > 1 ? " titres" : " titre"));
        initiale = artiste.isBlank() ? "?" : artiste.trim().substring(0, 1).toUpperCase();
        favori = estFavori.test(artiste);
        abonne = estAbonne.test(artiste);
        setOpaque(selectionne);
        // Marges : place pour l'initiale à gauche, pour l'abonnement et le coeur à droite
        setBorder(BorderFactory.createEmptyBorder(10, 8 + AVATAR + 10, 10, ZONE_ABONNEMENT + ZONE_COEUR));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int h = getHeight(), w = getWidth();

        // Initiale dans un rond
        int ay = (h - AVATAR) / 2;
        g2.setColor(Theme.CHAMP.brighter());
        g2.fillOval(8, ay, AVATAR, AVATAR);
        g2.setColor(Theme.TEXTE);
        g2.setFont(Theme.police(15, true));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initiale, 8 + (AVATAR - fm.stringWidth(initiale)) / 2, ay + (AVATAR + fm.getAscent() - fm.getDescent()) / 2);

        // Bouton d'abonnement
        String texte = abonne ? "Abonné" : "S'abonner";
        g2.setFont(Theme.police(11, true));
        fm = g2.getFontMetrics();
        int lb = ZONE_ABONNEMENT - 12, hb = 24;
        int bx = w - ZONE_COEUR - ZONE_ABONNEMENT + 6, by = (h - hb) / 2;
        RoundRectangle2D.Float pilule = new RoundRectangle2D.Float(bx, by, lb, hb, hb, hb);
        if (abonne) {
            g2.setColor(Theme.ACCENT);
            g2.fill(pilule);
            g2.setColor(Theme.FOND);
        } else {
            g2.setColor(Theme.TEXTE_2);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(pilule);
        }
        g2.drawString(texte, bx + (lb - fm.stringWidth(texte)) / 2, by + (hb + fm.getAscent() - fm.getDescent()) / 2);

        // Coeur (artiste favori)
        double t = 15;
        Theme.dessinerCoeur(g2, w - ZONE_COEUR + (ZONE_COEUR - t) / 2.0 - 2, (h - t) / 2.0, t,
            favori, favori ? Theme.ACCENT : Theme.TEXTE_2);
        g2.dispose();
    }
}
