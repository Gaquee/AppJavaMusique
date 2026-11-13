import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;


// ===================== Charte graphique =====================
final class Theme {
    static final Color FOND         = Color.decode("#1E1F22");
    static final Color SURFACE      = Color.decode("#2B2D31");
    static final Color CHAMP        = Color.decode("#383A40");
    static final Color TEXTE        = Color.decode("#F2F3F5");
    static final Color TEXTE_2      = Color.decode("#A0A3A8");
    static final Color ACCENT       = Color.decode("#DECC65");
    static final Color ACCENT_CLAIR = Color.decode("#E8DA88");
    static final Color ACCENT_FONCE = Color.decode("#C9B652");
    static final Color ERREUR       = Color.decode("#E06C75");
    static final Color SCROLLBAR    = Color.decode("#4A4D55");

    private Theme() {}

    static Font police(int taille, boolean gras) {
        return new Font("Segoe UI", gras ? Font.BOLD : Font.PLAIN, taille);
    }

    static JLabel label(String texte, int taille, boolean gras, Color couleur) {
        JLabel l = new JLabel(texte);
        l.setFont(police(taille, gras));
        l.setForeground(couleur);
        return l;
    }

    // Page verticale avec marges de 16 px
    static JPanel page() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(FOND);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return p;
    }

    // Composant qui prend toute la largeur, avec une hauteur fixe
    static <T extends JComponent> T pleineLargeur(T c, int hauteur) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, hauteur));
        c.setPreferredSize(new Dimension(200, hauteur));
        return c;
    }

    static JLabel centre(JLabel l, int hauteur) {
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return pleineLargeur(l, hauteur);
    }

    static void styliserChamp(JTextField f) {
        f.setOpaque(false);
        f.setBackground(CHAMP);
        f.setForeground(TEXTE);
        f.setCaretColor(TEXTE);
        f.setSelectionColor(ACCENT);
        f.setSelectedTextColor(FOND);
        f.setFont(police(13, false));
        f.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.repaint(); }
            @Override public void focusLost(FocusEvent e)   { f.repaint(); }
        });
    }

    static void dessinerFondChamp(Graphics g, JTextField f, int rayon) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(f.getBackground());
        g2.fillRoundRect(0, 0, f.getWidth(), f.getHeight(), rayon, rayon);
        if (f.isFocusOwner()) {                       // contour or quand le champ est actif
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(1, 1, f.getWidth() - 3, f.getHeight() - 3, rayon, rayon);
        }
        g2.dispose();
    }

    static void dessinerPlaceholder(Graphics g, JTextField f, String texte) {
        if (texte == null || f.getDocument().getLength() > 0 || f.isFocusOwner()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(f.getFont());
        g2.setColor(TEXTE_2);
        FontMetrics fm = g2.getFontMetrics();
        int y = (f.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(texte, f.getInsets().left, y);
        g2.dispose();
    }

    // Coeur dessiné dans un carré de côté t (plein = favori)
    static void dessinerCoeur(Graphics2D g2, double x, double y, double t, boolean plein, Color couleur) {
        Path2D.Double c = new Path2D.Double();
        c.moveTo(x + .50 * t, y + .30 * t);
        c.curveTo(x + .50 * t, y + .14 * t, x + .30 * t, y + .06 * t, x + .18 * t, y + .13 * t);
        c.curveTo(x + .02 * t, y + .23 * t, x + .02 * t, y + .46 * t, x + .15 * t, y + .60 * t);
        c.lineTo(x + .50 * t, y + .93 * t);
        c.lineTo(x + .85 * t, y + .60 * t);
        c.curveTo(x + .98 * t, y + .46 * t, x + .98 * t, y + .23 * t, x + .82 * t, y + .13 * t);
        c.curveTo(x + .70 * t, y + .06 * t, x + .50 * t, y + .14 * t, x + .50 * t, y + .30 * t);
        c.closePath();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(couleur);
        if (plein) {
            g2.fill(c);
        } else {
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(c);
        }
    }

    // Scroll sombre, sans bordure, avec une barre fine
    static void styliserScroll(JScrollPane s) {
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollBar barre = s.getVerticalScrollBar();
        barre.setOpaque(false);
        barre.setPreferredSize(new Dimension(8, 0));
        barre.setUI(new BasicScrollBarUI() {
            @Override protected JButton createDecreaseButton(int o) { return boutonVide(); }
            @Override protected JButton createIncreaseButton(int o) { return boutonVide(); }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SCROLLBAR);
                g2.fillRoundRect(r.x + 2, r.y, r.width - 4, r.height, 6, 6);
                g2.dispose();
            }
            private JButton boutonVide() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }
}


// ===================== Bouton principal (or) =====================
class BoutonArrondi extends JButton {
    private final int rayon;

    public BoutonArrondi(String texte, int rayon) {
        super(texte);
        this.rayon = rayon;
        setContentAreaFilled(false);  // on dessine le fond nous-mêmes
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Theme.FOND);
        setFont(Theme.police(13, true));
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!isEnabled() || getModel().isPressed()) {
            g2.setColor(Theme.ACCENT_FONCE);
        } else if (getModel().isRollover()) {
            g2.setColor(Theme.ACCENT_CLAIR);
        } else {
            g2.setColor(Theme.ACCENT);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), rayon, rayon);

        g2.dispose();
        super.paintComponent(g);
    }
}


// ===================== Bouton secondaire (lien texte) =====================
class BoutonLien extends JButton {

    public BoutonLien(String texte, Color couleur) {
        super(texte);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(couleur);
        setFont(Theme.police(12, false));
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}


// ===================== Champs de texte =====================
class ChampArrondi extends JTextField {
    private final int rayon;
    private String placeholder;

    public ChampArrondi(int colonnes, int rayon) {
        super(colonnes);
        this.rayon = rayon;
        Theme.styliserChamp(this);
    }

    public void setPlaceholder(String texte) {
        this.placeholder = texte;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.dessinerFondChamp(g, this, rayon);
        super.paintComponent(g);  // dessine le texte et le curseur
        Theme.dessinerPlaceholder(g, this, placeholder);
    }
}

class MdpArrondi extends JPasswordField {
    private final int rayon;

    public MdpArrondi(int colonnes, int rayon) {
        super(colonnes);
        this.rayon = rayon;
        Theme.styliserChamp(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme.dessinerFondChamp(g, this, rayon);
        super.paintComponent(g);
    }
}


// ===================== Panneau à coins arrondis =====================
class PanneauArrondi extends JPanel {
    private final int rayon;

    public PanneauArrondi(LayoutManager layout, Color fond, int rayon) {
        super(layout);
        this.rayon = rayon;
        setOpaque(false);
        setBackground(fond);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), rayon, rayon);
        g2.dispose();
        super.paintComponent(g);
    }
}


