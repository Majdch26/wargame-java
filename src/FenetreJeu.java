package wargame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

//  Audio WAV Java standard
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Fenêtre principale du jeu WARGAME.
 */
public class FenetreJeu extends JFrame implements IConfig, MouseListener, MouseMotionListener {
    private Carte carte;
    private PanneauJeu panneauJeu;
    private JLabel labelStats;
    private JLabel labelInfo;
    private JButton boutonFinTour, boutonSauvegarder, boutonRestaurer, boutonRedemarrer;
    private Position positionSelectionnee;
    private boolean dragEnCours;
    
    // Variables pour la visualisation de portée/déplacement
    private boolean afficherPortee = false;
    private boolean afficherDeplacement = false;
    private Position positionSurvolee;
    private List<Position> casesPortee = new ArrayList<>();
    private List<Position> casesDeplacement = new ArrayList<>();

    //  Boutons son
    private JButton boutonSon;
    private JButton boutonSonMute;
    
    // Boutons portée/déplacement
    private JToggleButton togglePortee;
    private JToggleButton toggleDeplacement;

    //  Son (WAV)
    private Clip clip;
    private boolean sonCoupe = false;

    // Couleurs du thème Seigneur des Anneaux
    private final Color COLOR_DARK = new Color(20, 15, 10);
    private final Color COLOR_GOLD = new Color(212, 175, 55);
    private final Color COLOR_BRONZE = new Color(165, 110, 40);
    private final Color COLOR_PARCHMENT = new Color(245, 235, 220);
    private final Color COLOR_DARK_BROWN = new Color(70, 50, 30);
    private final Color COLOR_LIGHT_BROWN = new Color(120, 90, 60);
    
    // Couleurs pour la visualisation
    private final Color COLOR_PORTEE = new Color(0, 100, 255, 80);
    private final Color COLOR_DEPLACEMENT = new Color(0, 255, 0, 80);
    private final Color COLOR_SURVOLE = new Color(255, 255, 0, 100);

    public FenetreJeu() {
        super("WARGAME");
        this.carte = new Carte();
        initialiserInterface();
    }

    private void initialiserInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Utiliser un look simple
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // MÉTHODE SIMPLE : BorderLayout
        setLayout(new BorderLayout());

        // Panel principal avec fond texture
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 25, 20),
                        getWidth(), getHeight(), new Color(15, 10, 5));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(new Color(255, 255, 255, 10));
                for (int i = 0; i < getWidth(); i += 20) {
                    g2d.drawLine(i, 0, i, getHeight());
                }
                for (int i = 0; i < getHeight(); i += 20) {
                    g2d.drawLine(0, i, getWidth(), i);
                }
            }
        };

        // Stats - Barre supérieure fine
        JPanel panelStats = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        panelStats.setOpaque(false);
        panelStats.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panelStats.setPreferredSize(new Dimension(getWidth(), 40));

        labelStats = new JLabel();
        labelStats.setFont(new Font("Arial", Font.BOLD, 16));
        labelStats.setForeground(COLOR_GOLD);
        panelStats.add(labelStats);

        // Jeu - SIMPLE SANS SCROLLPANE
        panneauJeu = new PanneauJeu(carte, this);
        panneauJeu.addMouseListener(this);
        panneauJeu.addMouseMotionListener(this);
        panneauJeu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BRONZE, 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Info - Barre inférieure fine
        JPanel panelInfo = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelInfo.setOpaque(false);
        panelInfo.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        panelInfo.setPreferredSize(new Dimension(getWidth(), 30));

        labelInfo = new JLabel("Survolez un élément pour voir ses informations");
        labelInfo.setFont(new Font("Arial", Font.PLAIN, 12));
        labelInfo.setForeground(Color.WHITE);
        panelInfo.add(labelInfo);

        // Menu latéral
        JPanel panelMenu = creerPanelMenuModerne();

        // Assemblage SIMPLE
        mainPanel.add(panelStats, BorderLayout.NORTH);
        mainPanel.add(panneauJeu, BorderLayout.CENTER); // Plateau au centre directement
        mainPanel.add(panelInfo, BorderLayout.SOUTH);
        mainPanel.add(panelMenu, BorderLayout.EAST);

        add(mainPanel);

        creerBarreMenu();
        mettreAJourStats();

        //  son wav
        initialiserSon();

        // Configuration de la fenêtre
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Plein écran
        setMinimumSize(new Dimension(800, 600)); // Taille minimale
        setLocationRelativeTo(null); // Centrer
        
        // IMPORTANT : Forcer le dessin AVANT setVisible
        panneauJeu.revalidate();
        validate();
        
        setVisible(true);
        
        // Forcer plusieurs repaints après l'affichage
        panneauJeu.repaint();
        repaint();
        
        SwingUtilities.invokeLater(() -> {
            panneauJeu.repaint();
        });
    }

    // --------------------------------------------------------------------
    // ✅ MENU MODERNE (version précédente qui fonctionnait)
    // --------------------------------------------------------------------
    private JPanel creerPanelMenuModerne() {
        JPanel panelMenu = new JPanel(new BorderLayout());
        panelMenu.setPreferredSize(new Dimension(280, HAUTEUR_CARTE * NB_PIX_CASE));
        panelMenu.setBackground(new Color(25, 20, 15));
        panelMenu.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, COLOR_BRONZE));
    
        JPanel panelTitre = new JPanel(new BorderLayout());
        panelTitre.setBackground(new Color(35, 30, 25));
        panelTitre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BRONZE),
                BorderFactory.createEmptyBorder(20, 10, 20, 10)
        ));
        panelTitre.setPreferredSize(new Dimension(280, 100));
    
        JLabel titre = new JLabel("MENU", SwingConstants.CENTER);
        titre.setFont(new Font("Arial", Font.BOLD, 22));
        titre.setForeground(COLOR_GOLD);
    
        //JLabel sousTitre = new JLabel("Lord of the Rings", SwingConstants.CENTER);
        //sousTitre.setFont(new Font("Arial", Font.PLAIN, 12));
        //sousTitre.setForeground(Color.LIGHT_GRAY);
        //sousTitre.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    
        panelTitre.add(titre, BorderLayout.CENTER);
        //panelTitre.add(sousTitre, BorderLayout.SOUTH);
    
        JPanel panelBoutons = new JPanel();
        panelBoutons.setBackground(new Color(25, 20, 15));
        panelBoutons.setLayout(new BoxLayout(panelBoutons, BoxLayout.Y_AXIS));
        panelBoutons.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
        int boutonWidth = 240;
        int boutonHeight = 70;
        int espacement = 20;
    
        boutonSauvegarder = creerBoutonImageSeule("sauvegarder.png", boutonWidth, boutonHeight);
        boutonSauvegarder.addActionListener(e -> sauvegarderMultiple());
    
        boutonRestaurer = creerBoutonImageSeule("restaurer.png", boutonWidth, boutonHeight);
        boutonRestaurer.addActionListener(e -> restaurerMultiple());
    
        boutonFinTour = creerBoutonImageSeule("fin_tour.png", boutonWidth, boutonHeight);
        boutonFinTour.addActionListener(e -> finDuTour());
    
        boutonRedemarrer = creerBoutonImageSeule("redemarrer.png", boutonWidth, boutonHeight);
        boutonRedemarrer.addActionListener(e -> redemarrer());
    
        JButton boutonQuitter = creerBoutonImageSeule("quitter.png", boutonWidth, boutonHeight);
        boutonQuitter.addActionListener(e -> quitterJeu());
    
        boutonSauvegarder.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonRestaurer.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonFinTour.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonRedemarrer.setAlignmentX(Component.CENTER_ALIGNMENT);
        boutonQuitter.setAlignmentX(Component.CENTER_ALIGNMENT);
    
        panelBoutons.add(boutonSauvegarder);
        panelBoutons.add(Box.createVerticalStrut(espacement));
        panelBoutons.add(boutonRestaurer);
        panelBoutons.add(Box.createVerticalStrut(espacement));
        panelBoutons.add(boutonFinTour);
        panelBoutons.add(Box.createVerticalStrut(espacement));
        panelBoutons.add(boutonRedemarrer);
        panelBoutons.add(Box.createVerticalStrut(espacement));
    
        //  Ajouter Quitter comme un bouton normal (comme les autres)
        panelBoutons.add(boutonQuitter);
    
        //  Petit espace vertical entre Quitter et les boutons son
        int espacementSon = 12;
        panelBoutons.add(Box.createVerticalStrut(espacementSon));
    
        //  Panel horizontal uniquement pour les 2 boutons son
        JPanel panelSon = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panelSon.setOpaque(false);
    
        int tailleSon = 70;
        boutonSon = creerBoutonImageSeule("sound.png", tailleSon, tailleSon);
        boutonSonMute = creerBoutonImageSeule("sound_muted.png", tailleSon, tailleSon);
    
        boutonSon.addActionListener(e -> activerSon());
        boutonSonMute.addActionListener(e -> couperSon());
    
        panelSon.add(boutonSon);
        panelSon.add(boutonSonMute);
    
        panelSon.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelBoutons.add(panelSon);
        
        // --------------------------------------------------------------------
        // AJOUT DES BOUTONS PORTÉE ET DÉPLACEMENT SOUS LE SON
        // --------------------------------------------------------------------
        panelBoutons.add(Box.createVerticalStrut(15));
        
        // Panel pour les boutons portée/déplacement
        JPanel panelVisualisation = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelVisualisation.setOpaque(false);
        panelVisualisation.setMaximumSize(new Dimension(260, 60));
        
        // Bouton Portée
        togglePortee = new JToggleButton("Portée");
        togglePortee.setFont(new Font("Arial", Font.BOLD, 12));
        togglePortee.setForeground(Color.WHITE);
        togglePortee.setBackground(new Color(60, 80, 150));
        togglePortee.setFocusPainted(false);
        togglePortee.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_GOLD, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        togglePortee.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePortee.addActionListener(e -> {
            afficherPortee = togglePortee.isSelected();
            if (afficherPortee) {
                togglePortee.setBackground(new Color(40, 60, 130));
                togglePortee.setForeground(COLOR_GOLD);
            } else {
                togglePortee.setBackground(new Color(60, 80, 150));
                togglePortee.setForeground(Color.WHITE);
            }
            panneauJeu.redessiner();
        });
        
        // Bouton Déplacement
        toggleDeplacement = new JToggleButton("Déplacement");
        toggleDeplacement.setFont(new Font("Arial", Font.BOLD, 12));
        toggleDeplacement.setForeground(Color.WHITE);
        toggleDeplacement.setBackground(new Color(60, 150, 80));
        toggleDeplacement.setFocusPainted(false);
        toggleDeplacement.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_GOLD, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        toggleDeplacement.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleDeplacement.addActionListener(e -> {
            afficherDeplacement = toggleDeplacement.isSelected();
            if (afficherDeplacement) {
                toggleDeplacement.setBackground(new Color(40, 130, 60));
                toggleDeplacement.setForeground(COLOR_GOLD);
            } else {
                toggleDeplacement.setBackground(new Color(60, 150, 80));
                toggleDeplacement.setForeground(Color.WHITE);
            }
            panneauJeu.redessiner();
        });
        
        // Effet survol pour les boutons
        togglePortee.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!togglePortee.isSelected()) {
                    togglePortee.setBackground(new Color(80, 100, 170));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!togglePortee.isSelected()) {
                    togglePortee.setBackground(new Color(60, 80, 150));
                }
            }
        });
        
        toggleDeplacement.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!toggleDeplacement.isSelected()) {
                    toggleDeplacement.setBackground(new Color(80, 170, 100));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!toggleDeplacement.isSelected()) {
                    toggleDeplacement.setBackground(new Color(60, 150, 80));
                }
            }
        });
        
        panelVisualisation.add(togglePortee);
        panelVisualisation.add(toggleDeplacement);
        
        panelVisualisation.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelBoutons.add(panelVisualisation);
    
        // Footer (si tu veux le réactiver plus tard)
        JPanel panelFooter = new JPanel(new BorderLayout());
        panelFooter.setBackground(new Color(35, 30, 25));
        panelFooter.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, COLOR_BRONZE));
        panelFooter.setPreferredSize(new Dimension(280, 60));
    
        panelMenu.add(panelTitre, BorderLayout.NORTH);
        panelMenu.add(panelBoutons, BorderLayout.CENTER);
        // panelMenu.add(panelFooter, BorderLayout.SOUTH);
    
        return panelMenu;
    }    

    // --------------------------------------------------------------------
    //  BOUTON IMAGE (resize exact)
    // --------------------------------------------------------------------
    private JButton creerBoutonImageSeule(String nomFichier, int width, int height) {
        JButton bouton = new JButton();
        bouton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bouton.setFocusPainted(false);
        bouton.setBorderPainted(false);
        bouton.setContentAreaFilled(false);
        bouton.setOpaque(false);

        bouton.setPreferredSize(new Dimension(width, height));
        bouton.setMinimumSize(new Dimension(width, height));
        bouton.setMaximumSize(new Dimension(width, height));

        ImageIcon icon = chargerIconeGrande(nomFichier, width, height);
        if (icon != null) {
            bouton.setIcon(icon);
        } else {
            bouton.setText(nomFichier.replace(".png", "").toUpperCase());
            bouton.setFont(new Font("Arial", Font.BOLD, 12));
            bouton.setForeground(Color.WHITE);
            bouton.setBackground(new Color(60, 50, 40));
            bouton.setOpaque(true);
            bouton.setBorder(BorderFactory.createLineBorder(COLOR_BRONZE, 1));
        }

        bouton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                bouton.setBorderPainted(true);
                bouton.setBorder(BorderFactory.createLineBorder(COLOR_GOLD, 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                bouton.setBorderPainted(false);
                bouton.setBorder(null);
            }
        });

        return bouton;
    }

    private ImageIcon chargerIconeGrande(String nomFichier, int w, int h) {
        try {
            BufferedImage img = null;

            String[] chemins = {
                    "wargame/images/" + nomFichier,
                    "images/" + nomFichier,
                    "./wargame/images/" + nomFichier,
                    "./images/" + nomFichier,
                    "../images/" + nomFichier,
                    nomFichier
            };

            for (String chemin : chemins) {
                URL url = getClass().getClassLoader().getResource(chemin);
                if (url != null) {
                    img = ImageIO.read(url);
                    break;
                }
                File file = new File(chemin);
                if (file.exists()) {
                    img = ImageIO.read(file);
                    break;
                }
            }

            if (img == null) return null;

            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(img, 0, 0, w, h, null);
            g2d.dispose();

            return new ImageIcon(resized);

        } catch (Exception e) {
            System.err.println("Erreur chargement image " + nomFichier + ": " + e.getMessage());
            return null;
        }
    }

    // --------------------------------------------------------------------
    //  ACTIONS (version précédente qui fonctionnait)
    // --------------------------------------------------------------------
    private void finDuTour() {
        try {
            carte.jouerSoldats(panneauJeu);
            carte.nettoyerSoldatsMorts();
            mettreAJourStats();
            panneauJeu.redessiner();
            labelInfo.setText("Tour terminé !");

            if (carte.partieTerminee()) {
                JOptionPane.showMessageDialog(this,
                        "Partie terminée !\nGagnant: " + carte.getGagnant(),
                        "Fin de partie", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur pendant le tour : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void redemarrer() {
        int res = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment redémarrer la partie ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);

        if (res == JOptionPane.YES_OPTION) {
            try {
                this.carte = new Carte();
                panneauJeu.setCarte(carte);
                mettreAJourStats();
                panneauJeu.redessiner();
                labelInfo.setText("Nouvelle partie démarrée !");
                // Réinitialiser la visualisation
                casesPortee.clear();
                casesDeplacement.clear();
                positionSurvolee = null;
                // Désactiver les boutons
                togglePortee.setSelected(false);
                toggleDeplacement.setSelected(false);
                afficherPortee = false;
                afficherDeplacement = false;
                togglePortee.setBackground(new Color(60, 80, 150));
                togglePortee.setForeground(Color.WHITE);
                toggleDeplacement.setBackground(new Color(60, 150, 80));
                toggleDeplacement.setForeground(Color.WHITE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Erreur lors du redémarrage: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sauvegarderMultiple() {
        String nomSauvegarde = JOptionPane.showInputDialog(this,
                "Entrez un nom pour la sauvegarde :",
                "Sauvegarde multiple",
                JOptionPane.QUESTION_MESSAGE);
        
        if (nomSauvegarde != null && !nomSauvegarde.trim().isEmpty()) {
            nomSauvegarde = nomSauvegarde.trim();
            try {
                carte.sauvegarder(nomSauvegarde);
                labelInfo.setText("Partie sauvegardée sous : " + nomSauvegarde);
                JOptionPane.showMessageDialog(this,
                        "Partie sauvegardée avec succès sous : " + nomSauvegarde,
                        "Sauvegarde", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Erreur sauvegarde: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } else if (nomSauvegarde != null) {
            JOptionPane.showMessageDialog(this,
                    "Le nom de sauvegarde ne peut pas être vide.",
                    "Erreur", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void restaurerMultiple() {
        List<String> sauvegardes = Carte.getListeSauvegardes();
        
        if (sauvegardes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucune sauvegarde trouvée.",
                    "Restauration", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String[] options = sauvegardes.toArray(new String[0]);
        String choix = (String) JOptionPane.showInputDialog(this,
                "Choisissez une sauvegarde à restaurer :",
                "Restauration multiple",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        
        if (choix != null) {
            try {
                Carte nouvelleCarte = Carte.restaurer(choix);
                this.carte = nouvelleCarte;
                panneauJeu.setCarte(carte);
                mettreAJourStats();
                panneauJeu.redessiner();
                labelInfo.setText("Partie restaurée : " + choix);
                JOptionPane.showMessageDialog(this,
                        "Partie restaurée avec succès : " + choix,
                        "Restauration", JOptionPane.INFORMATION_MESSAGE);
                // Réinitialiser la visualisation
                casesPortee.clear();
                casesDeplacement.clear();
                positionSurvolee = null;
                // Désactiver les boutons
                togglePortee.setSelected(false);
                toggleDeplacement.setSelected(false);
                afficherPortee = false;
                afficherDeplacement = false;
                togglePortee.setBackground(new Color(60, 80, 150));
                togglePortee.setForeground(Color.WHITE);
                toggleDeplacement.setBackground(new Color(60, 150, 80));
                toggleDeplacement.setForeground(Color.WHITE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Erreur restauration: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void quitterJeu() {
        int res = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment quitter le jeu ?",
                "Quitter", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (res == JOptionPane.YES_OPTION) {
            try {
                if (clip != null) clip.stop();
            } catch (Exception ignored) {}
            System.exit(0);
        }
    }

    private void mettreAJourStats() {
        int herosVivants = compterHerosVivants();
        int monstresVivants = compterMonstresVivants();
        String statsText = String.format("HEROS: %d   |   MONSTRES: %d   |   TOUR: %d",
                herosVivants, monstresVivants, carte.getTourActuel());
        labelStats.setText(statsText);
    }

    // --------------------------------------------------------------------
    //  DRAG & DROP (version simple qui fonctionnait)
    // --------------------------------------------------------------------
    @Override public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) {
        Position pos = getPositionFromMouse(e.getX(), e.getY());
        Element element = carte.getElement(pos);

        if (element instanceof Heros) {
            Heros heros = (Heros) element;
            if (heros.peutJouer(carte.getTourActuel())) {
                positionSelectionnee = pos;
                dragEnCours = true;
                
                // Afficher les cases de déplacement si activé
                if (afficherDeplacement) {
                    casesDeplacement = carte.getCasesDeplacement(heros);
                    panneauJeu.redessiner();
                }
            } else {
                labelInfo.setText("Ce héros a déjà joué ce tour !");
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragEnCours && positionSelectionnee != null) {
            Position posCible = getPositionFromMouse(e.getX(), e.getY());

            if (!posCible.equals(positionSelectionnee)) {
                try {
                    boolean actionReussie = carte.actionHeros(positionSelectionnee, posCible);
                    if (actionReussie) {
                        mettreAJourStats();
                        panneauJeu.redessiner();

                        if (carte.partieTerminee()) {
                            JOptionPane.showMessageDialog(this,
                                    "Partie terminée !\nGagnant: " + carte.getGagnant(),
                                    "Fin de partie", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    labelInfo.setText("Action impossible: " + ex.getMessage());
                }
            }

            dragEnCours = false;
            positionSelectionnee = null;
            casesDeplacement.clear();
            panneauJeu.redessiner();
        }
    }

    @Override public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) {
        labelInfo.setText("Survolez un élément pour voir ses informations");
        positionSurvolee = null;
        casesPortee.clear();
        casesDeplacement.clear();
        panneauJeu.redessiner();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragEnCours) {
            Position pos = getPositionFromMouse(e.getX(), e.getY());
            mettreAJourInfo(carte.getElement(pos), pos);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Position pos = getPositionFromMouse(e.getX(), e.getY());
        Element element = carte.getElement(pos);
        mettreAJourInfo(element, pos);
        
        // Mettre à jour la position survolée pour la visualisation
        positionSurvolee = pos;
        
        // Si on affiche la portée et qu'on survole un soldat
        if (afficherPortee && element instanceof Soldat) {
            Soldat soldat = (Soldat) element;
            casesPortee = carte.getCasesPortee(soldat);
        } else {
            casesPortee.clear();
        }
        
        // Redessiner pour afficher la visualisation
        panneauJeu.redessiner();
    }

    private void mettreAJourInfo(Element element, Position pos) {
        if (element == null) {
            labelInfo.setText("Case " + pos + " - Vide");
        } else if (element instanceof Heros) {
            Heros heros = (Heros) element;
            String peutJouer = heros.peutJouer(carte.getTourActuel()) ? " (peut jouer)" : " (a déjà joué)";
            String peutTirer = heros.getTir() > 0 ? " - Peut tirer: OUI" : " - Peut tirer: NON";
            labelInfo.setText("Héros " + heros.getNom() + " " + heros.getType()
                    + " - PV: " + heros.getPoints() + "/" + heros.getPointsMax()
                    + " - Portée: " + heros.getPortee()
                    + peutTirer
                    + " - Position: " + pos + peutJouer);
        } else if (element instanceof Monstre) {
            Monstre monstre = (Monstre) element;
            String peutTirer = monstre.getTir() > 0 ? " - Peut tirer: OUI" : " - Peut tirer: NON";
            labelInfo.setText("Monstre " + monstre.getNumero() + " " + monstre.getType()
                    + " - PV: " + monstre.getPoints() + "/" + monstre.getPointsMax()
                    + " - Portée: " + monstre.getPortee()
                    + peutTirer
                    + " - Position: " + pos);
        } else if (element instanceof Obstacle) {
            Obstacle obstacle = (Obstacle) element;
            labelInfo.setText("Obstacle: " + obstacle.getType() + " - Position: " + pos);
        }
    }

    // --------------------------------------------------------------------
    //  UTILITAIRES
    // --------------------------------------------------------------------
    private Position getPositionFromMouse(int x, int y) {
        // Obtenir la taille réelle du panneau
        int width = panneauJeu.getWidth();
        int height = panneauJeu.getHeight();
        
        // Calculer la taille des cases
        int caseWidth = width / LARGEUR_CARTE;
        int caseHeight = height / HAUTEUR_CARTE;
        
        // Éviter la division par zéro
        if (caseWidth == 0) caseWidth = NB_PIX_CASE;
        if (caseHeight == 0) caseHeight = NB_PIX_CASE;
        
        int caseX = x / caseWidth;
        int caseY = y / caseHeight;
        try {
            return new Position(caseX, caseY);
        } catch (PositionException e) {
            return new Position(-1, -1, false);
        }
    }

    private int compterHerosVivants() {
        int count = 0;
        List<Heros> heroes = carte.getHeros();
        for (Heros h : heroes) if (h.estVivant()) count++;
        return count;
    }

    private int compterMonstresVivants() {
        int count = 0;
        List<Monstre> monsters = carte.getMonstres();
        for (Monstre m : monsters) if (m.estVivant()) count++;
        return count;
    }
    
    // Getters pour la visualisation
    public boolean isAfficherPortee() {
        return afficherPortee;
    }
    
    public boolean isAfficherDeplacement() {
        return afficherDeplacement;
    }
    
    public Position getPositionSurvolee() {
        return positionSurvolee;
    }
    
    public List<Position> getCasesPortee() {
        return casesPortee;
    }
    
    public List<Position> getCasesDeplacement() {
        return casesDeplacement;
    }
    
    public Color getColorPortee() {
        return COLOR_PORTEE;
    }
    
    public Color getColorDeplacement() {
        return COLOR_DEPLACEMENT;
    }
    
    public Color getColorSurvole() {
        return COLOR_SURVOLE;
    }

    // --------------------------------------------------------------------
    //  MENU AIDE
    // --------------------------------------------------------------------
    private void creerBarreMenu() {
        JMenuBar barreMenu = new JMenuBar();

        JMenu menuFichier = new JMenu("Fichier");

        JMenuItem itemNouveau = new JMenuItem("Nouvelle partie");
        itemNouveau.addActionListener(e -> redemarrer());
        itemNouveau.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));

        JMenuItem itemSauvegarder = new JMenuItem("Sauvegarder...");
        itemSauvegarder.addActionListener(e -> sauvegarderMultiple());
        itemSauvegarder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        JMenuItem itemRestaurer = new JMenuItem("Restaurer...");
        itemRestaurer.addActionListener(e -> restaurerMultiple());
        itemRestaurer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

        JMenuItem itemQuitter = new JMenuItem("Quitter");
        itemQuitter.addActionListener(e -> System.exit(0));
        itemQuitter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));

        menuFichier.add(itemNouveau);
        menuFichier.addSeparator();
        menuFichier.add(itemSauvegarder);
        menuFichier.add(itemRestaurer);
        menuFichier.addSeparator();
        menuFichier.add(itemQuitter);

        JMenu menuOptions = new JMenu("Options");
        
        JCheckBoxMenuItem itemPortee = new JCheckBoxMenuItem("Afficher portée");
        itemPortee.addActionListener(e -> {
            afficherPortee = itemPortee.isSelected();
            togglePortee.setSelected(afficherPortee);
            if (afficherPortee) {
                togglePortee.setBackground(new Color(40, 60, 130));
                togglePortee.setForeground(COLOR_GOLD);
            } else {
                togglePortee.setBackground(new Color(60, 80, 150));
                togglePortee.setForeground(Color.WHITE);
            }
            panneauJeu.redessiner();
        });
        
        JCheckBoxMenuItem itemDeplacement = new JCheckBoxMenuItem("Afficher déplacement");
        itemDeplacement.addActionListener(e -> {
            afficherDeplacement = itemDeplacement.isSelected();
            toggleDeplacement.setSelected(afficherDeplacement);
            if (afficherDeplacement) {
                toggleDeplacement.setBackground(new Color(40, 130, 60));
                toggleDeplacement.setForeground(COLOR_GOLD);
            } else {
                toggleDeplacement.setBackground(new Color(60, 150, 80));
                toggleDeplacement.setForeground(Color.WHITE);
            }
            panneauJeu.redessiner();
        });
        
        menuOptions.add(itemPortee);
        menuOptions.add(itemDeplacement);

        JMenu menuAide = new JMenu("Aide");

        JMenuItem itemAPropos = new JMenuItem("À propos");
        itemAPropos.addActionListener(e -> aPropos());

        JMenuItem itemRegles = new JMenuItem("Règles du jeu");
        itemRegles.addActionListener(e -> regles());

        menuAide.add(itemRegles);
        menuAide.add(itemAPropos);

        barreMenu.add(menuFichier);
        barreMenu.add(menuOptions);
        barreMenu.add(menuAide);

        setJMenuBar(barreMenu);
    }

    private void aPropos() {
        JOptionPane.showMessageDialog(this,
                "Wargame - Seigneur des Anneaux\nVersion 2.0\n" +
                "Améliorations :\n" +
                "- IA intelligente des monstres\n" +
                "- Sauvegardes multiples\n" +
                "- Visualisation portée/déplacement\n" +
                "- Attaques à distance fonctionnelles\n" +
                "- Gestion des obstacles (ligne de vue)\n" +
                "- Plateau adaptatif pour tous les écrans",
                "À propos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void regles() {
        JOptionPane.showMessageDialog(this,
                "RÈGLES DU JEU\n\n" +
                "1. Déplacez vos héros par glisser-déposer\n" +
                "2. Attaquez les monstres en les déplaçant dessus\n" +
                "3. Utilisez 'Portée' pour voir les zones d'attaque\n" +
                "4. Utilisez 'Déplacement' pour voir les cases accessibles\n" +
                "5. Certains héros peuvent attaquer à distance (Elfes, Humains)\n" +
                "6. Les obstacles (rochers, forêts) bloquent les attaques à distance\n" +
                "7. Cliquez sur 'Fin du tour' quand vous avez terminé\n" +
                "8. Sauvegardez plusieurs parties avec des noms différents",
                "Règles du jeu", JOptionPane.INFORMATION_MESSAGE);
    }

    // --------------------------------------------------------------------
    //  AUDIO WAV
    // --------------------------------------------------------------------
    private void initialiserSon() {
        try {
            File f = new File("images/sound.wav");
            if (!f.exists()) f = new File("sound.wav");
            if (!f.exists()) f = new File("wargame/sound.wav");
            if (!f.exists()) f = new File("./wargame/sound.wav");

            if (!f.exists()) {
                System.out.println("sound.wav introuvable (images/sound.wav / sound.wav / wargame/sound.wav)");
                return;
            }

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            sonCoupe = false;

        } catch (Exception e) {
            System.err.println("Erreur son WAV: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Le fichier sound.wav n'est pas compatible.\n"
                            + "Refais la conversion avec:\n"
                            + "ffmpeg -y -i sound.mp3 -ac 2 -ar 44100 -sample_fmt s16 sound.wav",
                    "Erreur son", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void couperSon() {
        sonCoupe = true;
        try {
            if (clip != null && clip.isRunning()) clip.stop();
        } catch (Exception ignored) {}
    }

    private void activerSon() {
        sonCoupe = false;
        try {
            if (clip != null) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new FenetreJeu();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
