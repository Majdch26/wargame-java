package wargame;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dessine la carte : plateau, obstacles, soldats, brouillard de guerre,
 * cases accessibles, cibles possibles et conseil de l'IA.
 */
public class PanneauJeu extends JPanel implements IConfig {
    private static final long serialVersionUID = 2L;

    private static final Color BROUILLARD = new Color(25, 25, 30, 170);
    private static final Color SURVOL = new Color(255, 255, 255, 60);
    private static final Color DEPLACEMENT = new Color(70, 220, 90, 90);
    private static final Color PORTEE = new Color(60, 120, 255, 55);
    private static final Color CIBLE = new Color(235, 50, 50);
    private static final Color OR = new Color(212, 175, 55);

    /** Images de chaque type de soldat ; plusieurs images par type pour varier les monstres. */
    private static final Map<ISoldat.TypesH, int[]> IMAGES_HEROS = Map.of(
            ISoldat.TypesH.HUMAIN, new int[] {1},
            ISoldat.TypesH.NAIN, new int[] {2, 6},
            ISoldat.TypesH.ELF, new int[] {3, 5},
            ISoldat.TypesH.HOBBIT, new int[] {4});
    private static final Map<ISoldat.TypesM, int[]> IMAGES_MONSTRES = Map.of(
            ISoldat.TypesM.TROLL, new int[] {1, 6, 7, 11},
            ISoldat.TypesM.ORC, new int[] {3, 10, 4},
            ISoldat.TypesM.GOBELIN, new int[] {2, 5, 9});

    private Carte carte;
    private final FenetreJeu fenetre;
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, Image> imagesRedimensionnees = new HashMap<>();

    /**
     * @param carte la partie à afficher
     * @param fenetre la fenêtre (pour connaître la sélection, le survol et le conseil en cours)
     */
    public PanneauJeu(Carte carte, FenetreJeu fenetre) {
        this.carte = carte;
        this.fenetre = fenetre;
        setPreferredSize(new Dimension(LARGEUR_CARTE * NB_PIX_CASE, HAUTEUR_CARTE * NB_PIX_CASE));
        setToolTipText("");   // active les infobulles (texte fourni par getToolTipText)
        setOpaque(true);
        setBackground(new Color(50, 40, 30));
    }

    /** @param carte la nouvelle partie à afficher */
    public void setCarte(Carte carte) {
        this.carte = carte;
        repaint();
    }

    /** Redessine la carte. */
    public void redessiner() {
        repaint();
    }

    // ------------------------------------------------------------------
    // Géométrie
    // ------------------------------------------------------------------

    /** @return la taille d'une case en pixels (les cases restent carrées) */
    public int tailleCase() {
        return Math.max(8, Math.min(getWidth() / LARGEUR_CARTE, getHeight() / HAUTEUR_CARTE));
    }

    private int marge(boolean horizontal) {
        return horizontal ? (getWidth() - tailleCase() * LARGEUR_CARTE) / 2
                          : (getHeight() - tailleCase() * HAUTEUR_CARTE) / 2;
    }

    /**
     * @param x abscisse en pixels
     * @param y ordonnée en pixels
     * @return la case sous ce point (éventuellement hors de la carte)
     */
    public Position caseSous(int x, int y) {
        int t = tailleCase();
        int cx = Math.floorDiv(x - marge(true), t);
        int cy = Math.floorDiv(y - marge(false), t);
        return new Position(cx, cy, false);
    }

    private int px(int caseX) {
        return marge(true) + caseX * tailleCase();
    }

    private int py(int caseY) {
        return marge(false) + caseY * tailleCase();
    }

    // ------------------------------------------------------------------
    // Images
    // ------------------------------------------------------------------

    private BufferedImage image(String chemin) {
        return images.computeIfAbsent(chemin, c -> {
            try (InputStream in = PanneauJeu.class.getResourceAsStream("/wargame/images/" + c)) {
                return in == null ? null : ImageIO.read(in);
            } catch (IOException e) {
                return null;
            }
        });
    }

    /** Image réduite pour tenir dans un carré de la taille donnée, sans être déformée. */
    private Image image(String chemin, int taille) {
        String cle = chemin + "@" + taille;
        if (!imagesRedimensionnees.containsKey(cle)) {
            BufferedImage source = image(chemin);
            Image reduite = null;
            if (source != null) {
                double echelle = Math.min(taille / (double) source.getWidth(), taille / (double) source.getHeight());
                int w = Math.max(1, (int) (source.getWidth() * echelle));
                int h = Math.max(1, (int) (source.getHeight() * echelle));
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.drawImage(source.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();
                reduite = image;
            }
            imagesRedimensionnees.put(cle, reduite);
        }
        return imagesRedimensionnees.get(cle);
    }

    private String cheminImage(Element e) {
        if (e instanceof Heros) {
            Heros h = (Heros) e;
            int[] choix = IMAGES_HEROS.get(h.getType());
            return "heros/hero_" + choix[(h.getNom().charAt(0) - 'A') % choix.length] + ".png";
        }
        if (e instanceof Monstre) {
            Monstre m = (Monstre) e;
            int[] choix = IMAGES_MONSTRES.get(m.getType());
            return "monstres/monstre_" + choix[m.getNumero() % choix.length] + ".png";
        }
        switch (((Obstacle) e).getType()) {
            case ROCHER: return "roches.png";
            case FORET: return "foret.png";
            default: return "eau.png";
        }
    }

    // ------------------------------------------------------------------
    // Dessin
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (carte == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int t = tailleCase();
        int x0 = marge(true), y0 = marge(false);

        BufferedImage plateau = image("plateau.jpg");
        if (plateau != null) {
            g.drawImage(plateau, x0, y0, t * LARGEUR_CARTE, t * HAUTEUR_CARTE, null);
        } else {
            g.setColor(new Color(110, 140, 80));
            g.fillRect(x0, y0, t * LARGEUR_CARTE, t * HAUTEUR_CARTE);
        }

        boolean[][] visible = carte.zoneVisibleHeros();
        Heros selection = fenetre.getHerosSelectionne();

        // Portée visuelle du soldat survolé (option)
        Position survol = fenetre.getPositionSurvolee();
        Element survole = survol == null ? null : carte.getElement(survol);
        if (fenetre.isAfficherPortee() && survole instanceof Soldat && estVisible(survol, visible)) {
            g.setColor(PORTEE);
            for (Position p : carte.getCasesPortee((Soldat) survole)) {
                g.fillRect(px(p.getX()), py(p.getY()), t, t);
            }
        }

        // Cases où le héros sélectionné peut aller
        Heros pourDeplacement = selection != null ? selection
                : (fenetre.isAfficherDeplacement() && survole instanceof Heros ? (Heros) survole : null);
        if (pourDeplacement != null && pourDeplacement.peutJouer(carte.getTourActuel())) {
            g.setColor(DEPLACEMENT);
            for (Position p : carte.getCasesDeplacement(pourDeplacement)) {
                g.fillRoundRect(px(p.getX()) + 2, py(p.getY()) + 2, t - 4, t - 4, 8, 8);
            }
        }

        // Éléments
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                Element e = carte.getElement(new Position(x, y, false));
                if (e instanceof Obstacle || (e != null && (visible[y][x] || e instanceof Heros))) {
                    dessinerElement(g, e, e.getPosition(), getWidth(), getHeight());
                }
            }
        }

        // Brouillard de guerre et grille
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                if (!visible[y][x]) {
                    g.setColor(BROUILLARD);
                    g.fillRect(px(x), py(y), t, t);
                }
                g.setColor(new Color(0, 0, 0, 45));
                g.drawRect(px(x), py(y), t, t);
            }
        }

        // Cibles possibles du héros sélectionné
        if (selection != null && selection.peutJouer(carte.getTourActuel())) {
            g.setStroke(new BasicStroke(3f));
            g.setColor(CIBLE);
            for (Monstre m : carte.getCiblesPossibles(selection)) {
                if (estVisible(m.getPosition(), visible)) {
                    g.drawRoundRect(px(m.getPosition().getX()) + 2, py(m.getPosition().getY()) + 2, t - 4, t - 4, 10, 10);
                }
            }
            g.setColor(OR);
            g.drawRoundRect(px(selection.getPosition().getX()) + 1, py(selection.getPosition().getY()) + 1, t - 2, t - 2, 10, 10);
        }

        // Case survolée
        if (survol != null && survol.estValide()) {
            g.setColor(SURVOL);
            g.fillRect(px(survol.getX()), py(survol.getY()), t, t);
        }

        dessinerConseil(g, fenetre.getConseil(), t);
    }

    private boolean estVisible(Position p, boolean[][] visible) {
        return p.estValide() && visible[p.getY()][p.getX()];
    }

    /** Flèche dorée du héros vers la case conseillée par l'IA. */
    private void dessinerConseil(Graphics2D g, Conseiller.Conseil conseil, int t) {
        if (conseil == null || !conseil.getHeros().estVivant()) {
            return;
        }
        Position de = conseil.getHeros().getPosition();
        Position vers = conseil.getCible();
        float x1 = px(de.getX()) + t / 2f, y1 = py(de.getY()) + t / 2f;
        float x2 = px(vers.getX()) + t / 2f, y2 = py(vers.getY()) + t / 2f;

        g.setColor(new Color(255, 215, 0, 230));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(px(de.getX()) + 1, py(de.getY()) + 1, t - 2, t - 2, 12, 12);
        if (!de.equals(vers)) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            float retrait = t * 0.35f;
            float xa = (float) (x2 - Math.cos(angle) * retrait), ya = (float) (y2 - Math.sin(angle) * retrait);
            g.draw(new Line2D.Float((float) (x1 + Math.cos(angle) * retrait), (float) (y1 + Math.sin(angle) * retrait), xa, ya));
            double pointe = t * 0.22;
            g.draw(new Line2D.Double(xa, ya, xa - pointe * Math.cos(angle - 0.5), ya - pointe * Math.sin(angle - 0.5)));
            g.draw(new Line2D.Double(xa, ya, xa - pointe * Math.cos(angle + 0.5), ya - pointe * Math.sin(angle + 0.5)));
            g.drawRoundRect(px(vers.getX()) + 1, py(vers.getY()) + 1, t - 2, t - 2, 12, 12);
        }
    }

    /**
     * Dessine un élément dans sa case.
     * @param g contexte graphique
     * @param element l'élément
     * @param pos sa case
     * @param panelWidth largeur du panneau (non utilisée : la taille des cases est calculée)
     * @param panelHeight hauteur du panneau (non utilisée)
     */
    public void dessinerElement(Graphics2D g, Element element, Position pos, int panelWidth, int panelHeight) {
        int t = tailleCase();
        int x = px(pos.getX()), y = py(pos.getY());
        Image img = image(cheminImage(element), t);

        if (img != null) {
            int w = img.getWidth(null), h = img.getHeight(null);
            g.drawImage(img, x + (t - w) / 2, y + (t - h) / 2, null);
        } else {
            g.setColor(couleurParDefaut(element));
            g.fillRect(x + 1, y + 1, t - 2, t - 2);
        }

        if (element instanceof Soldat) {
            Soldat s = (Soldat) element;
            if (s instanceof Heros && !s.peutJouer(carte.getTourActuel())) {
                // Héros qui a déjà joué : assombri
                java.awt.Composite avant = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g.setColor(Color.BLACK);
                g.fillRect(x, y, t, t);
                g.setComposite(avant);
            }
            dessinerBadge(g, s, x, y, t);
            dessinerBarreVie(g, s, x, y, t);
        }
    }

    private Color couleurParDefaut(Element e) {
        if (e instanceof Heros) {
            return ((Heros) e).peutJouer(carte.getTourActuel()) ? COULEUR_HEROS : COULEUR_HEROS_DEJA_JOUE;
        }
        if (e instanceof Monstre) {
            return COULEUR_MONSTRES;
        }
        switch (((Obstacle) e).getType()) {
            case ROCHER: return COULEUR_ROCHER;
            case FORET: return COULEUR_FORET;
            default: return COULEUR_EAU;
        }
    }

    /** Lettre du héros ou numéro du monstre, dans un coin de la case. */
    private void dessinerBadge(Graphics2D g, Soldat s, int x, int y, int t) {
        String texte = s.identifiant();
        int taille = Math.max(14, t / 3);
        g.setColor(s instanceof Heros ? new Color(30, 90, 200) : new Color(150, 25, 25));
        g.fillOval(x + 1, y + 1, taille, taille);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, taille * 2 / 3)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texte, x + 1 + (taille - fm.stringWidth(texte)) / 2, y + 1 + (taille + fm.getAscent() - fm.getDescent()) / 2);
    }

    /** Barre de vie en bas de la case : verte, orange puis rouge. */
    private void dessinerBarreVie(Graphics2D g, Soldat s, int x, int y, int t) {
        double part = Math.max(0, s.getPoints()) / (double) s.getPointsMax();
        int h = Math.max(4, t / 10);
        int by = y + t - h - 2;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(x + 3, by, t - 6, h);
        g.setColor(part > 0.6 ? new Color(60, 200, 80) : part > 0.3 ? new Color(240, 160, 30) : new Color(220, 50, 40));
        g.fillRect(x + 3, by, (int) Math.round((t - 6) * part), h);
    }

    /** Infobulle : détails du soldat ou de l'obstacle sous la souris. */
    @Override
    public String getToolTipText(MouseEvent e) {
        if (carte == null) {
            return null;
        }
        Position p = caseSous(e.getX(), e.getY());
        if (!p.estValide()) {
            return null;
        }
        Element el = carte.getElement(p);
        boolean visible = carte.zoneVisibleHeros()[p.getY()][p.getX()];
        if (el instanceof Soldat && (visible || el instanceof Heros)) {
            Soldat s = (Soldat) el;
            String etat = s instanceof Heros
                    ? (s.peutJouer(carte.getTourActuel()) ? "Peut agir ce tour" : "A déjà agi ce tour")
                    : "Monstre";
            return "<html><b>" + s.getNomType() + " " + s.identifiant() + "</b><br>"
                    + "Points de vie : " + s.getPoints() + " / " + s.getPointsMax() + "<br>"
                    + "Frappe : " + s.getPuissance() + (s.getTir() > 0 ? " · Tir : " + s.getTir() : " · Ne tire pas") + "<br>"
                    + "Portée : " + s.getPortee() + " case" + (s.getPortee() > 1 ? "s" : "") + "<br>"
                    + "<i>" + etat + "</i></html>";
        }
        if (el instanceof Obstacle) {
            Obstacle o = (Obstacle) el;
            return "<html><b>" + o.getType().getNom() + "</b><br>Infranchissable"
                    + (o.getType() == Obstacle.TypeObstacle.EAU ? " · les tirs passent au-dessus" : " · bloque les tirs") + "</html>";
        }
        return visible ? null : "Zone inconnue (hors de vue de vos héros)";
    }

    /** @return les positions des cases visibles, pour les tests */
    List<Position> casesVisibles() {
        List<Position> liste = new java.util.ArrayList<>();
        boolean[][] v = carte.zoneVisibleHeros();
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                if (v[y][x]) {
                    liste.add(new Position(x, y, false));
                }
            }
        }
        return liste;
    }
}
