import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.DoubleConsumer;


// ===================== Bouton icône (lecture, suivant, favori...) =====================
class BoutonIcone extends JButton {
    enum Type { LECTURE, PAUSE, PRECEDENT, SUIVANT, COEUR, ALEATOIRE, REPETER }

    private Type type;
    private final boolean fondPlein;   // rond or derrière l'icône (bouton principal)
    private boolean actif;             // icône en or (favori, mode activé)

    public BoutonIcone(Type type, int taille, boolean fondPlein, String infobulle) {
        this.type = type;
        this.fondPlein = fondPlein;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setToolTipText(infobulle);
        Dimension d = new Dimension(taille, taille);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setType(Type type) {
        this.type = type;
        repaint();
    }

    public void setActif(boolean actif) {
        this.actif = actif;
        repaint();
    }

    public boolean estActif() {
        return actif;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = getWidth(), h = getHeight();
        boolean survol = getModel().isRollover(), presse = getModel().isPressed();

        Color couleur;
        if (fondPlein) {
            g2.setColor(presse ? Theme.ACCENT_FONCE : survol ? Theme.ACCENT_CLAIR : Theme.ACCENT);
            g2.fillOval(0, 0, w, h);
            couleur = Theme.FOND;
        } else if (actif) {
            couleur = survol ? Theme.ACCENT_CLAIR : Theme.ACCENT;
        } else {
            couleur = survol ? Theme.TEXTE : Theme.TEXTE_2;
        }
        g2.setColor(couleur);

        // L'icône occupe un carré de côté t centré dans le bouton
        double t = Math.min(w, h) * (fondPlein ? 0.36 : 0.46);
        double x = (w - t) / 2, y = (h - t) / 2;

        switch (type) {
            case LECTURE -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x + t * .12, y);
                p.lineTo(x + t * 1.02, y + t / 2);
                p.lineTo(x + t * .12, y + t);
                p.closePath();
                g2.fill(p);
            }
            case PAUSE -> {
                double l = t * .32;
                g2.fill(new RoundRectangle2D.Double(x + t * .08, y, l, t, 2, 2));
                g2.fill(new RoundRectangle2D.Double(x + t * .60, y, l, t, 2, 2));
            }
            case SUIVANT, PRECEDENT -> {
                if (type == Type.PRECEDENT) {   // même dessin, en miroir
                    g2.translate(w, 0);
                    g2.scale(-1, 1);
                }
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x, y + t * .05);
                p.lineTo(x + t * .78, y + t / 2);
                p.lineTo(x, y + t * .95);
                p.closePath();
                g2.fill(p);
                g2.fill(new RoundRectangle2D.Double(x + t * .82, y + t * .05, t * .18, t * .9, 2, 2));
            }
            case COEUR -> Theme.dessinerCoeur(g2, x, y, t, actif, couleur);
            case ALEATOIRE -> {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x, y + t * .22);
                p.curveTo(x + t * .45, y + t * .22, x + t * .45, y + t * .78, x + t * .9, y + t * .78);
                p.moveTo(x, y + t * .78);
                p.curveTo(x + t * .45, y + t * .78, x + t * .45, y + t * .22, x + t * .9, y + t * .22);
                g2.draw(p);
                pointe(g2, x + t, y + t * .22, t * .2);
                pointe(g2, x + t, y + t * .78, t * .2);
            }
            case REPETER -> {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Double p = new Path2D.Double();   // boucle ouverte en haut à droite
                p.moveTo(x + t * .62, y + t * .18);
                p.lineTo(x + t * .28, y + t * .18);
                p.quadTo(x, y + t * .18, x, y + t * .5);
                p.quadTo(x, y + t * .82, x + t * .28, y + t * .82);
                p.lineTo(x + t * .72, y + t * .82);
                p.quadTo(x + t, y + t * .82, x + t, y + t * .5);
                p.quadTo(x + t, y + t * .34, x + t * .9, y + t * .26);
                g2.draw(p);
                pointe(g2, x + t * .78, y + t * .18, t * .22);
            }
        }
        g2.dispose();
    }

    // Petite flèche pleine pointant vers la droite, pointe en (x, y)
    private static void pointe(Graphics2D g2, double x, double y, double t) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x - t, y - t);
        p.lineTo(x - t, y + t);
        p.closePath();
        g2.fill(p);
    }
}


// ===================== Onglet (Découvrir / Favoris) =====================
class BoutonOnglet extends JButton {
    private boolean selectionne;

    public BoutonOnglet(String texte) {
        super(texte);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.police(12, true));
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setSelectionne(false);
    }

    public void setSelectionne(boolean selectionne) {
        this.selectionne = selectionne;
        setForeground(selectionne ? Theme.FOND : Theme.TEXTE_2);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (selectionne) {
            g2.setColor(Theme.ACCENT);
        } else {
            g2.setColor(getModel().isRollover() ? Theme.CHAMP : Theme.SURFACE);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}


// ===================== Curseur (progression, volume) : valeur entre 0 et 1 =====================
class Curseur extends JComponent {
    private static final int MARGE = 7;   // place pour la poignée aux extrémités

    private double valeur;
    private boolean glisse, survol;
    private DoubleConsumer surGlissement = v -> { };
    private DoubleConsumer surRelachement = v -> { };

    public Curseur() {
        setOpaque(false);
        setPreferredSize(new Dimension(80, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter souris = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                glisse = true;
                deplacer(e.getX());
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (glisse) deplacer(e.getX());
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!glisse) return;
                glisse = false;
                surRelachement.accept(valeur);
                repaint();
            }
            @Override public void mouseEntered(MouseEvent e) { survol = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { survol = false; repaint(); }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
    }

    // Appelé en continu pendant que l'utilisateur déplace la poignée
    public void surGlissement(DoubleConsumer action)  { this.surGlissement = action; }
    // Appelé une fois, quand l'utilisateur relâche la poignée
    public void surRelachement(DoubleConsumer action) { this.surRelachement = action; }

    public boolean estEnGlissement() { return glisse; }
    public double getValeur()        { return valeur; }

    // Sans effet pendant que l'utilisateur tient la poignée
    public void setValeur(double v) {
        if (glisse) return;
        valeur = borner(v);
        repaint();
    }

    private void deplacer(int x) {
        valeur = borner((x - MARGE) / (double) Math.max(1, getWidth() - 2 * MARGE));
        surGlissement.accept(valeur);
        repaint();
    }

    private static double borner(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int largeur = getWidth() - 2 * MARGE, cy = getHeight() / 2;
        int rempli = (int) Math.round(largeur * valeur);

        g2.setColor(Theme.CHAMP);
        g2.fillRoundRect(MARGE, cy - 2, largeur, 4, 4, 4);
        g2.setColor(isEnabled() ? Theme.ACCENT : Theme.SCROLLBAR);
        g2.fillRoundRect(MARGE, cy - 2, rempli, 4, 4, 4);
        if (isEnabled() && (survol || glisse)) {
            g2.setColor(Theme.TEXTE);
            g2.fillOval(MARGE + rempli - 6, cy - 6, 12, 12);
        }
        g2.dispose();
    }
}
