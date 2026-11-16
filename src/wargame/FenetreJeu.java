package wargame;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Fenêtre principale du jeu : la carte, le menu latéral, la barre de menus,
 * les raccourcis clavier, le conseiller et le journal des combats.
 *
 * <p>Le joueur donne ses ordres par glisser-déposer, ou en cliquant sur un héros puis sur la case visée.</p>
 */
public class FenetreJeu extends JFrame implements IConfig {
    private static final long serialVersionUID = 2L;

    private static final Color OR = new Color(212, 175, 55);
    private static final Color BRONZE = new Color(165, 110, 40);
    private static final Color FOND_MENU = new Color(25, 20, 15);
    private static final Color BLEU = new Color(60, 80, 150);
    private static final Color VERT = new Color(60, 140, 80);
    private static final Color VIOLET = new Color(110, 70, 150);
    private static final int LARGEUR_MENU = 270;

    private Carte carte;
    private Conseiller conseiller;
    private Difficulte difficulte = Difficulte.NORMAL;
    private final PanneauJeu panneauJeu;
    private final JLabel labelStats = new JLabel("", SwingConstants.CENTER);
    private final JLabel labelInfo = new JLabel("", SwingConstants.CENTER);
    private final JTextArea journal = new JTextArea();
    private JToggleButton togglePortee, toggleDeplacement;
    private JCheckBoxMenuItem itemPortee, itemDeplacement;
    private JComboBox<Difficulte> choixDifficulte;
    private final ButtonGroup groupeDifficulte = new ButtonGroup();
    private JButton boutonConseil, boutonSon;

    private Heros herosSelectionne;
    private Position positionSurvolee;
    private boolean afficherPortee, afficherDeplacement;
    private Conseiller.Conseil conseil;
    private boolean finAnnoncee;

    private Clip musique;
    private boolean sonActif = true;
    private ImageIcon iconeSon, iconeMuet;

    /** Ouvre la fenêtre avec une nouvelle partie. */
    public FenetreJeu() {
        super("Wargame - La Bataille de la Terre du Milieu");
        carte = new Carte(difficulte);
        conseiller = new Conseiller(carte);
        panneauJeu = new PanneauJeu(carte, this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(creerContenu());
        setJMenuBar(creerBarreMenu());
        installerRaccourcis();
        installerSouris();
        ToolTipManager.sharedInstance().setInitialDelay(250);

        rafraichir("Glissez un héros vers une case voisine pour le déplacer, ou vers un monstre pour l'attaquer.");
        setMinimumSize(new Dimension(1000, 640));
        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        demarrerMusique();
    }

    // ------------------------------------------------------------------
    // Construction de l'interface
    // ------------------------------------------------------------------

    private JPanel creerContenu() {
        JPanel fond = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 25, 20), getWidth(), getHeight(), new Color(15, 10, 5)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        labelStats.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelStats.setForeground(OR);
        labelStats.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));

        labelInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        labelInfo.setForeground(Color.WHITE);
        labelInfo.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        panneauJeu.setBorder(BorderFactory.createLineBorder(BRONZE, 2));
        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centre.add(panneauJeu);

        fond.add(labelStats, BorderLayout.NORTH);
        fond.add(centre, BorderLayout.CENTER);
        fond.add(labelInfo, BorderLayout.SOUTH);
        fond.add(creerMenuLateral(), BorderLayout.EAST);
        return fond;
    }

    private JPanel creerMenuLateral() {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setPreferredSize(new Dimension(LARGEUR_MENU, 600));
        menu.setBackground(FOND_MENU);
        menu.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, BRONZE));

        JPanel boutons = new JPanel();
        boutons.setOpaque(false);
        boutons.setLayout(new BoxLayout(boutons, BoxLayout.Y_AXIS));
        boutons.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        int l = LARGEUR_MENU - 40, h = 50;
        ajouter(boutons, boutonImage("fin_tour.png", "Fin du tour (Espace)", l, h, e -> finDuTour()), 8);
        ajouter(boutons, boutonImage("sauvegarder.png", "Sauvegarder (Ctrl+S)", l, h, e -> sauvegarder()), 8);
        ajouter(boutons, boutonImage("restaurer.png", "Restaurer (Ctrl+R)", l, h, e -> restaurer()), 8);
        ajouter(boutons, boutonImage("redemarrer.png", "Nouvelle partie (Ctrl+N)", l, h, e -> redemarrer(true)), 8);
        ajouter(boutons, boutonImage("quitter.png", "Quitter (Ctrl+Q)", l, h, e -> quitter()), 14);

        // Intelligence artificielle
        boutons.add(titreSection("Intelligence artificielle"));
        choixDifficulte = new JComboBox<>(Difficulte.values());
        choixDifficulte.setSelectedItem(difficulte);
        choixDifficulte.setMaximumSize(new Dimension(l, 30));
        choixDifficulte.setAlignmentX(Component.CENTER_ALIGNMENT);
        choixDifficulte.setToolTipText("Stratégie des monstres (s'applique immédiatement)");
        choixDifficulte.addActionListener(e -> changerDifficulte((Difficulte) choixDifficulte.getSelectedItem()));
        ajouter(boutons, choixDifficulte, 8);

        JPanel ligneIA = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        ligneIA.setOpaque(false);
        boutonConseil = boutonTexte("Conseil (H)", VIOLET, "L'IA propose la meilleure action ; cliquez à nouveau pour la suivre");
        boutonConseil.addActionListener(e -> conseil());
        JButton auto = boutonTexte("Tour auto (A)", VIOLET, "L'IA joue tous vos héros puis termine le tour");
        auto.addActionListener(e -> tourAutomatique());
        ligneIA.add(boutonConseil);
        ligneIA.add(auto);
        ajouter(boutons, ligneIA, 12);

        // Affichage et son
        boutons.add(titreSection("Affichage"));
        JPanel ligneAffichage = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        ligneAffichage.setOpaque(false);
        togglePortee = new JToggleButton("Portée");
        styliser(togglePortee, BLEU, "Montre la portée du soldat survolé (P)");
        togglePortee.addActionListener(e -> setAfficherPortee(togglePortee.isSelected()));
        toggleDeplacement = new JToggleButton("Déplacements");
        styliser(toggleDeplacement, VERT, "Montre les cases accessibles du héros survolé (D)");
        toggleDeplacement.addActionListener(e -> setAfficherDeplacement(toggleDeplacement.isSelected()));
        iconeSon = icone("sound.png", 34, 34);
        iconeMuet = icone("sound_muted.png", 34, 34);
        boutonSon = new JButton(iconeSon);
        boutonSon.setPreferredSize(new Dimension(42, 38));
        boutonSon.setContentAreaFilled(false);
        boutonSon.setBorderPainted(false);
        boutonSon.setFocusable(false);
        boutonSon.setToolTipText("Musique (M)");
        boutonSon.addActionListener(e -> basculerSon());
        ligneAffichage.add(togglePortee);
        ligneAffichage.add(toggleDeplacement);
        ligneAffichage.add(boutonSon);
        ajouter(boutons, ligneAffichage, 10);

        // Journal des combats
        journal.setEditable(false);
        journal.setLineWrap(true);
        journal.setWrapStyleWord(true);
        journal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        journal.setBackground(new Color(35, 30, 25));
        journal.setForeground(new Color(230, 220, 200));
        journal.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane defilement = new JScrollPane(journal);
        defilement.setBorder(BorderFactory.createLineBorder(BRONZE));
        JPanel bas = new JPanel(new BorderLayout(0, 4));
        bas.setOpaque(false);
        bas.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        bas.add(titreSection("Journal des combats"), BorderLayout.NORTH);
        bas.add(defilement, BorderLayout.CENTER);

        menu.add(boutons, BorderLayout.NORTH);
        menu.add(bas, BorderLayout.CENTER);
        return menu;
    }

    private static void ajouter(JPanel panneau, JComponent composant, int espace) {
        composant.setAlignmentX(Component.CENTER_ALIGNMENT);
        panneau.add(composant);
        panneau.add(Box.createVerticalStrut(espace));
    }

    private static JLabel titreSection(String texte) {
        JLabel titre = new JLabel(texte.toUpperCase(), SwingConstants.CENTER);
        titre.setForeground(OR);
        titre.setFont(new Font("SansSerif", Font.BOLD, 12));
        titre.setAlignmentX(Component.CENTER_ALIGNMENT);
        titre.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        return titre;
    }

    private static void styliser(javax.swing.AbstractButton b, Color couleur, String aide) {
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(couleur);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setToolTipText(aide);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OR, 1),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    private static JButton boutonTexte(String texte, Color couleur, String aide) {
        JButton b = new JButton(texte);
        styliser(b, couleur, aide);
        return b;
    }

    private JButton boutonImage(String fichier, String aide, int largeur, int hauteur,
                                java.awt.event.ActionListener action) {
        ImageIcon ic = icone(fichier, largeur, hauteur);
        JButton b = ic != null ? new JButton(ic) : boutonTexte(aide, new Color(60, 50, 40), aide);
        b.setToolTipText(aide);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(largeur, hauteur));
        b.setPreferredSize(new Dimension(largeur, hauteur));
        if (ic != null) {
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder());
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { b.setBorderPainted(true); b.setBorder(BorderFactory.createLineBorder(OR, 2)); }
                @Override public void mouseExited(MouseEvent e) { b.setBorderPainted(false); b.setBorder(BorderFactory.createEmptyBorder()); }
            });
        }
        b.addActionListener(action);
        return b;
    }

    /** Charge une image du jeu (dans le .jar) à la taille voulue, en gardant ses proportions. */
    private static ImageIcon icone(String fichier, int largeur, int hauteur) {
        try (InputStream in = FenetreJeu.class.getResourceAsStream("/wargame/images/" + fichier)) {
            if (in == null) {
                return null;
            }
            BufferedImage img = ImageIO.read(in);
            double echelle = Math.min(largeur / (double) img.getWidth(), hauteur / (double) img.getHeight());
            int w = (int) Math.round(img.getWidth() * echelle), h = (int) Math.round(img.getHeight() * echelle);
            BufferedImage reduite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = reduite.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
            return new ImageIcon(reduite);
        } catch (Exception e) {
            return null;
        }
    }

    private JMenuBar creerBarreMenu() {
        JMenuBar barre = new JMenuBar();

        JMenu partie = new JMenu("Partie");
        partie.add(item("Nouvelle partie", KeyEvent.VK_N, e -> redemarrer(true)));
        partie.addSeparator();
        partie.add(item("Sauvegarder...", KeyEvent.VK_S, e -> sauvegarder()));
        partie.add(item("Restaurer...", KeyEvent.VK_R, e -> restaurer()));
        partie.addSeparator();
        partie.add(item("Quitter", KeyEvent.VK_Q, e -> quitter()));

        JMenu tour = new JMenu("Tour");
        JMenuItem fin = new JMenuItem("Fin du tour");
        fin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        fin.addActionListener(e -> finDuTour());
        tour.add(fin);

        JMenu ia = new JMenu("IA");
        JMenuItem itemConseil = new JMenuItem("Conseil / suivre le conseil");
        itemConseil.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));
        itemConseil.addActionListener(e -> conseil());
        JMenuItem itemAuto = new JMenuItem("Tour automatique");
        itemAuto.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));
        itemAuto.addActionListener(e -> tourAutomatique());
        ia.add(itemConseil);
        ia.add(itemAuto);
        ia.addSeparator();
        for (Difficulte d : Difficulte.values()) {
            JRadioButtonMenuItem r = new JRadioButtonMenuItem("Monstres : " + d, d == difficulte);
            r.setActionCommand(d.name());
            r.addActionListener(e -> changerDifficulte(d));
            groupeDifficulte.add(r);
            ia.add(r);
        }

        JMenu affichage = new JMenu("Affichage");
        itemPortee = new JCheckBoxMenuItem("Portée du soldat survolé");
        itemPortee.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
        itemPortee.addActionListener(e -> setAfficherPortee(itemPortee.isSelected()));
        itemDeplacement = new JCheckBoxMenuItem("Déplacements du héros survolé");
        itemDeplacement.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
        itemDeplacement.addActionListener(e -> setAfficherDeplacement(itemDeplacement.isSelected()));
        JMenuItem son = new JMenuItem("Musique on/off");
        son.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        son.addActionListener(e -> basculerSon());
        affichage.add(itemPortee);
        affichage.add(itemDeplacement);
        affichage.add(son);

        JMenu aide = new JMenu("Aide");
        aide.add(item("Règles du jeu", -1, e -> regles()));
        aide.add(item("À propos", -1, e -> aPropos()));

        barre.add(partie);
        barre.add(tour);
        barre.add(ia);
        barre.add(affichage);
        barre.add(aide);
        return barre;
    }

    private static JMenuItem item(String texte, int touche, java.awt.event.ActionListener action) {
        JMenuItem it = new JMenuItem(texte);
        if (touche >= 0) {
            it.setAccelerator(KeyStroke.getKeyStroke(touche, InputEvent.CTRL_DOWN_MASK));
        }
        it.addActionListener(action);
        return it;
    }

    /** Échap annule la sélection (les autres raccourcis sont portés par les menus). */
    private void installerRaccourcis() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "annuler");
        getRootPane().getActionMap().put("annuler", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                herosSelectionne = null;
                conseil = null;
                rafraichir("Sélection annulée.");
            }
        });
    }

    // ------------------------------------------------------------------
    // Souris : glisser-déposer ou clic sur le héros puis sur la case visée
    // ------------------------------------------------------------------

    private void installerSouris() {
        MouseAdapter souris = new MouseAdapter() {
            private Position depart;

            @Override
            public void mousePressed(MouseEvent e) {
                if (carte.partieTerminee()) {
                    return;
                }
                Position p = panneauJeu.caseSous(e.getX(), e.getY());
                Element el = carte.getElement(p);
                if (el instanceof Heros) {
                    Heros h = (Heros) el;
                    if (!h.peutJouer(carte.getTourActuel())) {
                        rafraichir(h.getNomType() + " " + h.getNom() + " a déjà agi ce tour.");
                        herosSelectionne = null;
                        return;
                    }
                    herosSelectionne = h;
                    depart = p;
                    rafraichir(h.getNomType() + " " + h.getNom() + " sélectionné : choisissez une case verte ou un monstre entouré de rouge.");
                } else if (herosSelectionne != null) {
                    agir(herosSelectionne.getPosition(), p);
                    depart = null;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Position p = panneauJeu.caseSous(e.getX(), e.getY());
                if (depart != null && herosSelectionne != null && !p.equals(depart)) {
                    agir(depart, p);   // glisser-déposer
                }
                depart = null;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                survoler(panneauJeu.caseSous(e.getX(), e.getY()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                survoler(panneauJeu.caseSous(e.getX(), e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                positionSurvolee = null;
                panneauJeu.redessiner();
            }
        };
        panneauJeu.addMouseListener(souris);
        panneauJeu.addMouseMotionListener(souris);
    }

    private void survoler(Position p) {
        if (!p.equals(positionSurvolee)) {
            positionSurvolee = p.estValide() ? p : null;
            panneauJeu.redessiner();
        }
    }

    /** Ordre donné au héros en « de » d'agir vers la case « vers ». */
    private void agir(Position de, Position vers) {
        if (!vers.estValide()) {
            return;
        }
        try {
            if (carte.actionHeros(de, vers)) {
                herosSelectionne = null;
                conseil = null;
                List<String> j = carte.getJournal();
                Element cible = carte.getElement(vers);
                rafraichir(cible instanceof Heros ? "Déplacement effectué." : (j.isEmpty() ? "" : j.get(0)));
                verifierFin();
            }
        } catch (SoldatException | CarteException | PositionException ex) {
            rafraichir("Action impossible : " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Actions du jeu
    // ------------------------------------------------------------------

    private void finDuTour() {
        if (carte.partieTerminee()) {
            return;
        }
        herosSelectionne = null;
        conseil = null;
        int avant = carte.getJournal().size();
        carte.jouerSoldats(panneauJeu);
        int attaques = Math.max(0, carte.getJournal().size() - avant);
        rafraichir("Tour " + carte.getTourActuel() + " terminé : les monstres ont attaqué " + attaques + " fois.");
        verifierFin();
    }

    /** Affiche le conseil de l'IA, ou l'applique s'il est déjà affiché. */
    private void conseil() {
        if (carte.partieTerminee()) {
            return;
        }
        if (conseil != null && conseil.getHeros().estVivant() && conseil.getHeros().peutJouer(carte.getTourActuel())) {
            Conseiller.Conseil suivi = conseil;
            conseil = null;
            conseiller.appliquer(suivi);
            herosSelectionne = null;
            rafraichir("Conseil suivi : " + suivi.getExplication() + ".");
            verifierFin();
            return;
        }
        conseil = conseiller.meilleurConseil();
        herosSelectionne = null;
        rafraichir(conseil == null
                ? "Tous vos héros ont joué : terminez le tour (Espace)."
                : "Conseil : " + conseil.getExplication() + ". Appuyez à nouveau sur H pour le suivre.");
    }

    /** L'IA joue tous les héros qui n'ont pas encore agi, puis termine le tour. */
    private void tourAutomatique() {
        if (carte.partieTerminee()) {
            return;
        }
        conseil = null;
        herosSelectionne = null;
        List<Conseiller.Conseil> joues = conseiller.jouerTourHeros();
        if (!carte.partieTerminee()) {
            carte.jouerSoldats(panneauJeu);
        }
        rafraichir("Tour automatique : " + joues.size() + " action" + (joues.size() > 1 ? "s" : "")
                + " des héros, puis tour des monstres.");
        verifierFin();
    }

    private void changerDifficulte(Difficulte d) {
        if (d == null || d == carte.getDifficulte()) {
            return;
        }
        difficulte = d;
        carte.setDifficulte(d);
        choixDifficulte.setSelectedItem(d);
        groupeDifficulte.getElements().asIterator().forEachRemaining(b -> b.setSelected(b.getActionCommand().equals(d.name())));
        rafraichir("Les monstres jouent maintenant en mode " + d + ".");
    }

    private void verifierFin() {
        if (!carte.partieTerminee() || finAnnoncee) {
            return;
        }
        finAnnoncee = true;
        boolean victoire = carte.getGagnant().equals("Héros");
        String message = (victoire ? "Victoire ! Les monstres ont été vaincus" : "Défaite... vos héros sont tombés")
                + " en " + (carte.getTourActuel() + 1) + " tours.\n\nCommencer une nouvelle partie ?";
        SwingUtilities.invokeLater(() -> {
            int choix = JOptionPane.showConfirmDialog(this, message, victoire ? "Victoire" : "Défaite",
                    JOptionPane.YES_NO_OPTION, victoire ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            if (choix == JOptionPane.YES_OPTION) {
                redemarrer(false);
            }
        });
    }

    private void redemarrer(boolean demander) {
        if (demander && !carte.partieTerminee() && JOptionPane.showConfirmDialog(this,
                "Abandonner la partie en cours et en commencer une nouvelle ?",
                "Nouvelle partie", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        changerCarte(new Carte(difficulte), "Nouvelle partie en mode " + difficulte + ". Bonne chance !");
    }

    private void changerCarte(Carte nouvelle, String message) {
        carte = nouvelle;
        conseiller = new Conseiller(carte);
        conseil = null;
        herosSelectionne = null;
        finAnnoncee = carte.partieTerminee();
        difficulte = carte.getDifficulte();
        choixDifficulte.setSelectedItem(difficulte);
        panneauJeu.setCarte(carte);
        rafraichir(message);
    }

    private void sauvegarder() {
        String nom = (String) JOptionPane.showInputDialog(this, "Nom de la sauvegarde :", "Sauvegarder",
                JOptionPane.QUESTION_MESSAGE, null, null, "wargame");
        if (nom == null) {
            return;
        }
        try {
            carte.sauvegarder(nom);
            rafraichir("Partie sauvegardée sous « " + nom.trim() + " ».");
        } catch (SauvegardeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sauvegarde impossible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restaurer() {
        List<String> sauvegardes = Carte.getListeSauvegardes();
        if (sauvegardes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sauvegarde pour le moment.", "Restaurer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String choix = (String) JOptionPane.showInputDialog(this, "Choisissez une sauvegarde :", "Restaurer",
                JOptionPane.QUESTION_MESSAGE, null, sauvegardes.toArray(), sauvegardes.get(0));
        if (choix == null) {
            return;
        }
        try {
            Carte restauree = Carte.restaurer(choix);
            changerCarte(restauree, "Partie « " + choix + " » restaurée (tour " + (restauree.getTourActuel() + 1) + ").");
        } catch (SauvegardeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Restauration impossible", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitter() {
        if (JOptionPane.showConfirmDialog(this, "Quitter le jeu ?", "Quitter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void setAfficherPortee(boolean actif) {
        afficherPortee = actif;
        togglePortee.setSelected(actif);
        itemPortee.setSelected(actif);
        togglePortee.setBackground(actif ? BLEU.darker() : BLEU);
        panneauJeu.redessiner();
    }

    private void setAfficherDeplacement(boolean actif) {
        afficherDeplacement = actif;
        toggleDeplacement.setSelected(actif);
        itemDeplacement.setSelected(actif);
        toggleDeplacement.setBackground(actif ? VERT.darker() : VERT);
        panneauJeu.redessiner();
    }

    /** Met à jour les compteurs, le journal, le message et la carte. */
    private void rafraichir(String message) {
        labelStats.setText(String.format("HÉROS : %d     MONSTRES : %d     TOUR : %d     MONSTRES EN MODE %s",
                carte.getHeros().size(), carte.getMonstres().size(), carte.getTourActuel() + 1,
                carte.getDifficulte().toString().toUpperCase()));
        labelInfo.setText(message);
        journal.setText(String.join("\n", carte.getJournal()));
        journal.setCaretPosition(0);
        boutonConseil.setText(conseil != null ? "Suivre (H)" : "Conseil (H)");
        panneauJeu.redessiner();
    }

    private void regles() {
        JOptionPane.showMessageDialog(this,
                "BUT : éliminer tous les monstres avant qu'ils n'éliminent vos héros.\n\n"
                + "À chaque tour, chaque héros peut faire UNE action :\n"
                + "  • se déplacer d'une case (diagonales comprises) ;\n"
                + "  • attaquer un monstre au contact, ou à distance s'il sait tirer\n"
                + "    (dans sa portée, et sans rocher ni forêt sur la trajectoire) ;\n"
                + "  • ne rien faire : il se repose et regagne " + Soldat.POINTS_REPOS + " points de vie.\n\n"
                + "Glissez un héros vers la case visée, ou cliquez sur le héros puis sur la case.\n"
                + "Les cases grisées sont hors de vue de vos héros : les monstres s'y cachent.\n\n"
                + "Raccourcis : Espace fin du tour · H conseil de l'IA · A tour automatique\n"
                + "P portée · D déplacements · M musique · Échap annuler · Ctrl+S / Ctrl+R / Ctrl+N",
                "Règles du jeu", JOptionPane.INFORMATION_MESSAGE);
    }

    private void aPropos() {
        JOptionPane.showMessageDialog(this,
                "Wargame - Projet de programmation orientée objet (Licence 3 Informatique)\n"
                + "Université Jean Monnet, Saint-Étienne\n\n"
                + "Trois niveaux d'IA pour les monstres, un conseiller IA pour le joueur,\n"
                + "brouillard de guerre, ligne de vue, sauvegardes multiples.\n\n"
                + "Musique : « The Old Tower Inn » (RandomMind, domaine public).",
                "À propos", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Musique (lue depuis le .jar ; le jeu fonctionne aussi sans carte son)
    // ------------------------------------------------------------------

    private void demarrerMusique() {
        try (InputStream in = FenetreJeu.class.getResourceAsStream("/wargame/sound.wav")) {
            if (in == null) {
                throw new IllegalStateException("fichier absent");
            }
            AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            musique = AudioSystem.getClip();
            musique.open(audio);
            musique.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception | LinkageError e) {
            musique = null;
            boutonSon.setIcon(iconeMuet);
            boutonSon.setEnabled(false);
            boutonSon.setToolTipText("Musique indisponible sur cet ordinateur");
        }
    }

    private void basculerSon() {
        if (musique == null) {
            return;
        }
        sonActif = !sonActif;
        if (sonActif) {
            musique.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            musique.stop();
        }
        boutonSon.setIcon(sonActif ? iconeSon : iconeMuet);
    }

    // ------------------------------------------------------------------
    // Accesseurs utilisés par le panneau de jeu
    // ------------------------------------------------------------------

    /** @return vrai si la portée du soldat survolé est affichée */
    public boolean isAfficherPortee() { return afficherPortee; }
    /** @return vrai si les déplacements du héros survolé sont affichés */
    public boolean isAfficherDeplacement() { return afficherDeplacement; }
    /** @return la case sous la souris, ou null */
    public Position getPositionSurvolee() { return positionSurvolee; }
    /** @return le héros sélectionné, ou null */
    public Heros getHerosSelectionne() { return herosSelectionne; }
    /** @return le conseil affiché, ou null */
    public Conseiller.Conseil getConseil() { return conseil; }

    /**
     * Point d'entrée du jeu.
     * @param args non utilisés
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("ToggleButton.select", new Color(25, 30, 70));
            } catch (Exception ignore) {
                // apparence par défaut
            }
            new FenetreJeu();
        });
    }
}
