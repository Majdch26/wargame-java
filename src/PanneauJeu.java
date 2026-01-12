package wargame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class PanneauJeu extends JPanel implements IConfig {
    private Carte carte;
    private FenetreJeu fenetre;

    // Cache pour les images (évite de recharger à chaque fois)
    private Map<String, Image> imagesCache = new HashMap<>();
    
    // Images de fond
    private BufferedImage imagePlateau;
    
    // Indices pour associer les héros/monstres aux images
    private Map<Heros, Integer> heroImageIndex = new HashMap<>();
    private Map<Monstre, Integer> monstreImageIndex = new HashMap<>();

    public PanneauJeu(Carte carte, FenetreJeu fenetre) {
        this.carte = carte;
        this.fenetre = fenetre;
        setBackground(COULEUR_VIDE);

        chargerImagePlateau();
        initialiserIndicesImages();
        
        // Taille fixe pour garantir le fonctionnement du drag-drop
        setPreferredSize(new Dimension(LARGEUR_CARTE * NB_PIX_CASE, HAUTEUR_CARTE * NB_PIX_CASE));
    }

    public void setCarte(Carte carte) {
        this.carte = carte;
        initialiserIndicesImages();
    }

    /**
     * Initialise les indices d'images pour chaque héros et monstre
     */
    private void initialiserIndicesImages() {
        heroImageIndex.clear();
        monstreImageIndex.clear();
        
        // Assigner un index d'image à chaque héros
        int heroCount = 1;
        for (Heros heros : carte.getHeros()) {
            heroImageIndex.put(heros, heroCount);
            heroCount++;
            if (heroCount > 6) heroCount = 1; // Boucle sur 6 images max
        }
        
        // Assigner un index d'image à chaque monstre
        int monstreCount = 1;
        for (Monstre monstre : carte.getMonstres()) {
            monstreImageIndex.put(monstre, monstreCount);
            monstreCount++;
            if (monstreCount > 15) monstreCount = 1; // Boucle sur 15 images max
        }
    }

    /**
     * Charge plateau.png depuis wargame/images/plateau.png
     */
    private void chargerImagePlateau() {
        try {
            // 1) Classpath
            URL url = getClass().getClassLoader().getResource("wargame/images/plateau.png");
            if (url != null) {
                imagePlateau = ImageIO.read(url);
                return;
            }

            // 2) Chemins relatifs
            String[] chemins = {
                    "wargame/images/plateau.png",
                    "images/plateau.png",
                    "./wargame/images/plateau.png",
                    "./images/plateau.png"
            };

            for (String c : chemins) {
                File f = new File(c);
                if (f.exists()) {
                    imagePlateau = ImageIO.read(f);
                    return;
                }
            }

            System.err.println("⚠️ plateau.png introuvable.");
            imagePlateau = null;

        } catch (Exception e) {
            System.err.println("⚠️ Erreur chargement plateau.png: " + e.getMessage());
            imagePlateau = null;
        }
    }

    /**
     * Charge une image avec cache
     */
    private Image chargerImage(String chemin, int largeur, int hauteur) {
        String cacheKey = chemin + "_" + largeur + "x" + hauteur;
        
        // Vérifier le cache
        if (imagesCache.containsKey(cacheKey)) {
            return imagesCache.get(cacheKey);
        }
        
        try {
            BufferedImage img = null;
            
            // Essayer plusieurs chemins
            String[] cheminsPossibles = {
                "wargame/images/" + chemin,
                "images/" + chemin,
                "./wargame/images/" + chemin,
                "./images/" + chemin
            };
            
            for (String path : cheminsPossibles) {
                // Essayer comme ressource
                URL url = getClass().getClassLoader().getResource(path);
                if (url != null) {
                    img = ImageIO.read(url);
                    break;
                }
                
                // Essayer comme fichier
                File file = new File(path);
                if (file.exists()) {
                    img = ImageIO.read(file);
                    break;
                }
            }
            
            if (img != null) {
                // Redimensionner et mettre en cache
                Image imageRedimensionnee = img.getScaledInstance(largeur, hauteur, Image.SCALE_SMOOTH);
                imagesCache.put(cacheKey, imageRedimensionnee);
                return imageRedimensionnee;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur chargement " + chemin + ": " + e.getMessage());
        }
        
        // Image non trouvée
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Protection : ne rien faire si la carte n'est pas initialisée
        if (carte == null) {
            System.err.println("ATTENTION: carte est null dans paintComponent!");
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        
        // Activer l'antialiasing pour de meilleurs graphismes
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Obtenir la taille réelle du panneau
        int width = getWidth();
        int height = getHeight();
        
        // Protection : dimensions invalides
        if (width <= 0 || height <= 0) {
            System.err.println("ATTENTION: dimensions invalides: " + width + "x" + height);
            return;
        }
        
        // Dessiner le fond adapté
        if (imagePlateau != null) {
            // Adapter l'image au panneau
            g2d.drawImage(imagePlateau, 0, 0, width, height, null);
        } else {
            // Fond uni si pas d'image
            g2d.setColor(new Color(50, 40, 30));
            g2d.fillRect(0, 0, width, height);
        }

        // Dessiner la visualisation des portées et déplacements
        dessinerVisualisation(g2d, width, height);

        // Dessiner les éléments PAR-DESSUS
        try {
            for (int y = 0; y < HAUTEUR_CARTE; y++) {
                for (int x = 0; x < LARGEUR_CARTE; x++) {
                    Position pos = new Position(x, y, false);
                    Element element = carte.getElement(pos);

                    if (element != null) {
                        dessinerElement(g2d, element, pos, width, height);
                    }
                
                    // Bordure de grille (subtile)
                    g2d.setColor(new Color(0, 0, 0, 50));
                    int caseWidth = width / LARGEUR_CARTE;
                    int caseHeight = height / HAUTEUR_CARTE;
                    g2d.drawRect(x * caseWidth, y * caseHeight, caseWidth, caseHeight);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du dessin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Dessine la visualisation des portées et déplacements
     */
    private void dessinerVisualisation(Graphics2D g, int width, int height) {
        if (fenetre == null) return;
        
        // Calculer la taille des cases
        int caseWidth = width / LARGEUR_CARTE;
        int caseHeight = height / HAUTEUR_CARTE;
        
        // 1. Dessiner la case survolée
        Position posSurvolee = fenetre.getPositionSurvolee();
        if (posSurvolee != null && posSurvolee.estValide()) {
            int x = posSurvolee.getX() * caseWidth;
            int y = posSurvolee.getY() * caseHeight;
            g.setColor(fenetre.getColorSurvole());
            g.fillRect(x, y, caseWidth, caseHeight);
        }
        
        // 2. Dessiner les cases de portée
        if (fenetre.isAfficherPortee()) {
            List<Position> casesPortee = fenetre.getCasesPortee();
            g.setColor(fenetre.getColorPortee());
            for (Position pos : casesPortee) {
                if (pos != null && pos.estValide()) {
                    int x = pos.getX() * caseWidth;
                    int y = pos.getY() * caseHeight;
                    g.fillRect(x, y, caseWidth, caseHeight);
                    
                    // Ajouter un contour pour mieux voir
                    g.setColor(new Color(0, 0, 255, 150));
                    g.drawRect(x, y, caseWidth, caseHeight);
                    g.setColor(fenetre.getColorPortee());
                }
            }
        }
        
        // 3. Dessiner les cases de déplacement
        if (fenetre.isAfficherDeplacement()) {
            List<Position> casesDeplacement = fenetre.getCasesDeplacement();
            g.setColor(fenetre.getColorDeplacement());
            for (Position pos : casesDeplacement) {
                if (pos != null && pos.estValide()) {
                    int x = pos.getX() * caseWidth;
                    int y = pos.getY() * caseHeight;
                    g.fillRect(x, y, caseWidth, caseHeight);
                    
                    // Ajouter un contour pour mieux voir
                    g.setColor(new Color(0, 255, 0, 150));
                    g.drawRect(x, y, caseWidth, caseHeight);
                    g.setColor(fenetre.getColorDeplacement());
                }
            }
        }
    }

    public void redessiner() {
        repaint();
    }

    public void dessinerElement(Graphics2D g, Element element, Position pos, int panelWidth, int panelHeight) {
        int caseWidth = panelWidth / LARGEUR_CARTE;
        int caseHeight = panelHeight / HAUTEUR_CARTE;
        int x = pos.getX() * caseWidth;
        int y = pos.getY() * caseHeight;

        // Dessin selon le type
        if (element instanceof Heros) {
            dessinerHeros(g, (Heros) element, x, y, caseWidth, caseHeight);
        } else if (element instanceof Monstre) {
            dessinerMonstre(g, (Monstre) element, x, y, caseWidth, caseHeight);
        } else if (element instanceof Obstacle) {
            dessinerObstacle(g, (Obstacle) element, x, y, caseWidth, caseHeight);
        }
    }

    private void dessinerHeros(Graphics2D g, Heros heros, int x, int y, int width, int height) {
        // Récupérer l'index d'image pour ce héros
        Integer index = heroImageIndex.get(heros);
        if (index == null) {
            index = 1;
        }
        
        // Charger l'image du héros (hero_1.png à hero_6.png)
        String imageName = "hero_" + index + ".png";
        Image heroImage = chargerImage("heros/" + imageName, width, height);
        
        if (heroImage != null) {
            // Dessiner l'image
            g.drawImage(heroImage, x, y, width, height, this);
            
            // Indicateur si le héros ne peut pas jouer (semi-transparence)
            if (!heros.peutJouer(carte.getTourActuel())) {
                g.setColor(new Color(0, 0, 0, 100)); // Couche noire semi-transparente
                g.fillRect(x, y, width, height);
            }
        } else {
            // Fallback : dessin original si image non trouvée
            Color couleur = heros.peutJouer(carte.getTourActuel()) ? COULEUR_HEROS : COULEUR_HEROS_DEJA_JOUE;
            g.setColor(couleur);
            g.fillRect(x, y, width, height);
            
            // Bordure
            g.setColor(COULEUR_TEXTE);
            g.drawRect(x, y, width, height);
            
            // Lettre identifiant
            g.setColor(COULEUR_TEXTE);
            g.setFont(new Font("Arial", Font.BOLD, Math.max(10, width/3)));
            g.drawString("" + heros.getNom().charAt(0), x + width / 3, y + height / 2);
        }

        // Barre de vie (toujours dessiner)
        dessinerBarreVie(g, heros, x, y, width, height);
        
        // Afficher un indicateur de portée si le héros est survolé
        Position posSurvolee = fenetre != null ? fenetre.getPositionSurvolee() : null;
        if (posSurvolee != null && heros.getPosition().equals(posSurvolee) && fenetre.isAfficherPortee()) {
            g.setColor(new Color(255, 255, 0, 100));
            g.drawRect(x, y, width, height);
            g.drawRect(x-1, y-1, width+2, height+2);
        }
    }

    private void dessinerMonstre(Graphics2D g, Monstre monstre, int x, int y, int width, int height) {
        // Récupérer l'index d'image pour ce monstre
        Integer index = monstreImageIndex.get(monstre);
        if (index == null) {
            index = 1;
        }
        
        // Charger l'image du monstre (monstre_1.png à monstre_15.png)
        String imageName = "monstre_" + index + ".png";
        Image monstreImage = chargerImage("monstres/" + imageName, width, height);
        
        if (monstreImage != null) {
            // Dessiner l'image
            g.drawImage(monstreImage, x, y, width, height, this);
        } else {
            // Fallback : dessin original si image non trouvée
            g.setColor(COULEUR_MONSTRES);
            g.fillRect(x, y, width, height);
            
            // Bordure
            g.setColor(COULEUR_TEXTE);
            g.drawRect(x, y, width, height);
            
            // Numéro identifiant
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, Math.max(10, width/3)));
            g.drawString(String.valueOf(monstre.getNumero()), x + width / 2 - 5, y + height / 2 + 5);
        }

        // Barre de vie
        dessinerBarreVie(g, monstre, x, y, width, height);
        
        // Afficher un indicateur de portée si le monstre est survolé
        Position posSurvolee = fenetre != null ? fenetre.getPositionSurvolee() : null;
        if (posSurvolee != null && monstre.getPosition().equals(posSurvolee) && fenetre.isAfficherPortee()) {
            g.setColor(new Color(255, 255, 0, 100));
            g.drawRect(x, y, width, height);
            g.drawRect(x-1, y-1, width+2, height+2);
        }
    }

    private void dessinerObstacle(Graphics2D g, Obstacle obstacle, int x, int y, int width, int height) {
        Image obstacleImage = null;
        
        // Charger l'image appropriée selon le type d'obstacle
        switch (obstacle.getType()) {
            case ROCHER:
                obstacleImage = chargerImage("roches.png", width, height);
                break;
            case FORET:
                obstacleImage = chargerImage("foret.png", width, height);
                break;
            case EAU:
                obstacleImage = chargerImage("eau.png", width, height);
                break;
            default:
                // Pas d'image pour les autres types
                break;
        }
        
        if (obstacleImage != null) {
            // Dessiner l'image de l'obstacle
            g.drawImage(obstacleImage, x, y, width, height, this);
        } else {
            // Fallback : couleurs originales
            switch (obstacle.getType()) {
                case ROCHER:
                    g.setColor(COULEUR_ROCHER);
                    break;
                case FORET:
                    g.setColor(COULEUR_FORET);
                    break;
                case EAU:
                    g.setColor(COULEUR_EAU);
                    break;
                default:
                    g.setColor(COULEUR_INCONNU);
                    break;
            }
            g.fillRect(x, y, width, height);
        }
        
        // Bordure subtile pour les obstacles
        g.setColor(new Color(0, 0, 0, 100));
        g.drawRect(x, y, width, height);
    }

    private void dessinerBarreVie(Graphics g, Soldat soldat, int x, int y, int width, int height) {
        int pourcentageVie = (soldat.getPoints() * 100) / soldat.getPointsMax();
        int largeurBarre = (width * pourcentageVie) / 100;

        // Position de la barre (en haut du personnage)
        int barreY = y - 5;
        int barreHeight = 4;
        
        // Fond de la barre (gris)
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, barreY, width, barreHeight);
        
        // Vie actuelle (couleur selon le pourcentage)
        if (pourcentageVie > 60) {
            g.setColor(Color.GREEN);
        } else if (pourcentageVie > 30) {
            g.setColor(Color.ORANGE);
        } else {
            g.setColor(Color.RED);
        }
        
        g.fillRect(x, barreY, largeurBarre, barreHeight);
        
        // Bordure de la barre
        g.setColor(Color.BLACK);
        g.drawRect(x, barreY, width, barreHeight);
        
        // Afficher les points de vie si assez de place
        if (width >= 30) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, Math.max(8, width/6)));
            String texte = soldat.getPoints() + "/" + soldat.getPointsMax();
            int texteWidth = g.getFontMetrics().stringWidth(texte);
            g.drawString(texte, x + (width - texteWidth) / 2, barreY - 2);
        }
    }
}
