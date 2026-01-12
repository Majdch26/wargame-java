package wargame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;
import java.io.Serializable;

/**
 * Gère la carte de jeu avec tous les éléments (soldats, obstacles).
 * Contrôle le déroulement des tours de jeu et les interactions entre entités.
 * 
 * <p>La carte est représentée par une grille rectangulaire de {@value IConfig#LARGEUR_CARTE}x{@value IConfig#HAUTEUR_CARTE} cases.
 * Chaque case peut contenir un héros, un monstre ou un obstacle. Les soldats sont placés
 * aléatoirement au démarrage : héros dans la moitié gauche, monstres dans la moitié droite.</p>
 * 
 * <p>Implémente la sérialisation pour permettre la sauvegarde et la restauration des parties.</p>
 * 
 * @see Element
 * @see Soldat
 * @see ICarte
 */

public class Carte implements ICarte, IConfig, Serializable {
    private Element[][] grille;
    private List<Heros> heros;
    private List<Monstre> monstres;
    private List<Obstacle> obstacles;
    private int tourActuel;
    private Random random;
    
    /**
     * Constructeur de la carte.
     * Initialise la grille et place aléatoirement les soldats et obstacles.
     */
    public Carte() {
        this.grille = new Element[HAUTEUR_CARTE][LARGEUR_CARTE];
        this.heros = new ArrayList<>();
        this.monstres = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.tourActuel = 0;
        this.random = new Random();
        
        try {
            initialiserCarte();
        } catch (PositionException e) {
            System.err.println("Erreur d'initialisation de la carte: " + e.getMessage());
        }
    }
    
    /**
     * Initialise la carte en plaçant obstacles, héros et monstres.
     * Les héros sont placés dans la partie gauche, les monstres à droite.
     * 
     * @throws PositionException Si une position invalide est générée
     */
    private void initialiserCarte() throws PositionException {
        // Créer les obstacles
        for (int i = 0; i < NB_OBSTACLES; i++) {
            Position pos = trouvePositionVide();
            if (pos != null) {
                Obstacle obstacle = new Obstacle(Obstacle.TypeObstacle.getObstacleAlea(), pos);
                obstacles.add(obstacle);
                grille[pos.getY()][pos.getX()] = obstacle;
            }
        }
        
        // Créer les héros (partie gauche de la carte)
        String[] nomsHeros = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < NB_HEROS; i++) {
            Position pos;
            do {
                pos = new Position(random.nextInt(LARGEUR_CARTE / 2), random.nextInt(HAUTEUR_CARTE));
            } while (pos.estValide() && grille[pos.getY()][pos.getX()] != null);
            
            if (pos.estValide()) {
                Heros heros = new Heros(this, ISoldat.TypesH.getTypeHAlea(), nomsHeros[i], pos);
                this.heros.add(heros);
                grille[pos.getY()][pos.getX()] = heros;
            }
        }
        
        // Créer les monstres (partie droite de la carte)
        for (int i = 0; i < NB_MONSTRES; i++) {
            Position pos;
            do {
                pos = new Position(LARGEUR_CARTE / 2 + random.nextInt(LARGEUR_CARTE / 2), random.nextInt(HAUTEUR_CARTE));
            } while (pos.estValide() && grille[pos.getY()][pos.getX()] != null);
            
            if (pos.estValide()) {
                Monstre monstre = new Monstre(this, ISoldat.TypesM.getTypeMAlea(), i + 1, pos);
                this.monstres.add(monstre);
                grille[pos.getY()][pos.getX()] = monstre;
            }
        }
    }
    
    /**
     * Retourne l'élément situé à une position donnée.
     * 
     * @param pos La position à consulter
     * @return L'élément à cette position, ou null si vide ou position invalide
     */
    @Override
    public Element getElement(Position pos) {
        if (!pos.estValide()) {
            return null;
        }
        return grille[pos.getY()][pos.getX()];
    }
    
    /**
     * Trouve une position vide aléatoire sur toute la carte.
     * 
     * @return Une position vide aléatoire, ou null si aucune case libre
     */
    @Override
    public Position trouvePositionVide() {
        List<Position> positionsVides = new ArrayList<>();
        
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                if (grille[y][x] == null) {
                    // Utiliser le constructeur sans exception
                    positionsVides.add(new Position(x, y, false));
                }
            }
        }
        
        if (positionsVides.isEmpty()) {
            return null;
        }
        
        return positionsVides.get(random.nextInt(positionsVides.size()));
    }
         /**
     * Trouve une position vide adjacente à une position donnée.
     * Cherche parmi les 8 cases adjacentes (horizontale, verticale, diagonale).
     * 
     * @param pos La position de référence
     * @return Une position vide adjacente aléatoire, ou null si aucune
     */    @Override
    public Position trouvePositionVide(Position pos) {
        List<Position> positionsVides = new ArrayList<>();
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                try {
                    Position nouvellePos = new Position(pos.getX() + dx, pos.getY() + dy);
                    if (nouvellePos.estValide() && grille[nouvellePos.getY()][nouvellePos.getX()] == null) {
                        positionsVides.add(nouvellePos);
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }

            }
        }
        
        if (positionsVides.isEmpty()) {
            return null;
        }
        
        return positionsVides.get(random.nextInt(positionsVides.size()));
    }
    
    /**
     * Trouve un héros vivant aléatoire sur la carte.
     * 
     * @return Un héros vivant aléatoire, ou null si tous sont morts
     */
    @Override
    public Heros trouveHeros() {
        List<Heros> herosVivants = new ArrayList<>();
        for (Heros h : heros) {
            if (h.estVivant()) {
                herosVivants.add(h);
            }
        }
        
        if (herosVivants.isEmpty()) {
            return null;
        }
        
        return herosVivants.get(random.nextInt(herosVivants.size()));
    }
    
    /**
     * Trouve un héros vivant adjacent à une position donnée.
     * Cherche parmi les 8 cases adjacentes (horizontale, verticale, diagonale).
     * 
     * @param pos La position de référence
     * @return Un héros vivant adjacent aléatoire, ou null si aucun
     */
    @Override
    public Heros trouveHeros(Position pos) {
        List<Heros> herosProches = new ArrayList<>();
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                try {
                    Position nouvellePos = new Position(pos.getX() + dx, pos.getY() + dy);
                    if (nouvellePos.estValide()) {
                        Element element = grille[nouvellePos.getY()][nouvellePos.getX()];
                        if (element instanceof Heros) {
                            Heros h = (Heros) element;
                            if (h.estVivant()) {
                                herosProches.add(h);
                            }
                        }
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        if (herosProches.isEmpty()) {
            return null;
        }
        
        return herosProches.get(random.nextInt(herosProches.size()));
    }
    
    /**
     * Déplace un soldat vers une nouvelle position.
     * Met à jour la grille et la position du soldat.
     * 
     * @param nouvellePos La position de destination
     * @param soldat Le soldat à déplacer
     * @throws CarteException Si la position est invalide ou déjà occupée
     */
    @Override
    public boolean deplaceSoldat(Position nouvellePos, Soldat soldat) throws CarteException {
        if (!nouvellePos.estValide()) {
            throw new CarteException("Position de destination invalide: " + nouvellePos);
        }
        
        if (grille[nouvellePos.getY()][nouvellePos.getX()] != null) {
            throw new CarteException("Case de destination occupée: " + nouvellePos);
        }
        
        Position anciennePos = soldat.getPosition();
        // Libérer l'ancienne position
        grille[anciennePos.getY()][anciennePos.getX()] = null;
        
        // Occuper la nouvelle position
        grille[nouvellePos.getY()][nouvellePos.getX()] = soldat;
        soldat.seDeplace(nouvellePos);
        
        return true;
    }
    
    /**
     * Gère la mort d'un soldat en le retirant de la grille.
     */
    @Override
    public void mort(Soldat soldat) {
        Position pos = soldat.getPosition();
        if (pos.estValide()) {
            grille[pos.getY()][pos.getX()] = null;
        }
    }
    
    /**
     * Gère l'action d'un héros : déplacement ou combat.
     */
    @Override
    public boolean actionHeros(Position pos, Position pos2) throws SoldatException, CarteException, PositionException {
        if (!pos.estValide() || !pos2.estValide()) {
            throw new PositionException("Position invalide dans l'action");
        }
        
        Element element = grille[pos.getY()][pos.getX()];
        if (!(element instanceof Heros)) {
            throw new SoldatException("Pas de héros à la position source");
        }
        
        Heros heros = (Heros) element;
        if (!heros.peutJouer(tourActuel)) {
            throw new SoldatException("Le héros a déjà joué ce tour");
        }
        
        Element cible = grille[pos2.getY()][pos2.getX()];
        
        // CAS 1 : Déplacement (case vide adjacente)
        if (cible == null && pos.estVoisine(pos2)) {
            if (deplaceSoldat(pos2, heros)) {
                heros.joueTour(tourActuel);
                return true;
            }
        }
        // CAS 2 : Combat (corps à corps OU à distance)
        else if (cible instanceof Monstre) {
            Monstre monstre = (Monstre) cible;
            
            // Vérifier si le combat est possible
            if (peutCombattre(heros, monstre)) {
                heros.combat(monstre);
                heros.joueTour(tourActuel);
                return true;
            } else {
                throw new SoldatException("Cible hors de portée ou ligne de vue bloquée");
            }
        }
        // CAS 3 : Déplacement sur case non adjacente (interdit)
        else if (cible == null && !pos.estVoisine(pos2)) {
            throw new SoldatException("Déplacement possible seulement sur les cases adjacentes");
        }
        
        return false;
    }
    
    // Vérifie si un héros peut combattre un monstre
    /**
     * Vérifie si un héros peut combattre un monstre selon les règles de portée et ligne de vue.
     * @param heros
     * @param monstre
     * @return
     */
    private boolean peutCombattre(Heros heros, Monstre monstre) {
        Position posHeros = heros.getPosition();
        Position posMonstre = monstre.getPosition();
        
        // Calcul de la distance de Manhattan
        int distance = Math.abs(posHeros.getX() - posMonstre.getX()) + 
                      Math.abs(posHeros.getY() - posMonstre.getY());
        
        // 1. Combat corps à corps (distance = 1)
        if (distance == 1) {
            return true; // Toujours possible si adjacent
        }
        
        // 2. Combat à distance (distance > 1)
        if (distance > 1) {
            // Vérifier si le héros peut tirer
            if (heros.getTir() <= 0) {
                return false; // Ce héros ne peut pas attaquer à distance
            }
            
            // Vérifier si dans la portée
            if (distance > heros.getPortee()) {
                return false; // Hors de portée
            }
            
            // Vérifier la ligne de vue
            return aLigneDeVue(posHeros, posMonstre);
        }
        
        return false; // Même case (impossible)
    }
    
    /**
     * Gère le tour de jeu en faisant jouer tous les soldats.
     * Les héros qui n'ont pas joué se reposent.
     * Les monstres agissent automatiquement selon une IA.
     * 
     * @param pj Le panneau de jeu pour les mises à jour graphiques
     */
    @Override
    public void jouerSoldats(PanneauJeu pj) {
        // Repos pour les héros qui n'ont pas joué
        for (Heros heros : heros) {
            if (heros.estVivant() && heros.peutJouer(tourActuel)) {
                heros.repos();
                heros.joueTour(tourActuel);
            }
        }
        
        // Actions des monstres avec Iterator pour éviter ConcurrentModificationException
        Iterator<Monstre> iterator = monstres.iterator();
        while (iterator.hasNext()) {
            Monstre monstre = iterator.next();
            if (monstre.estVivant() && monstre.peutJouer(tourActuel)) {
                jouerMonstreIntelligent(monstre);
                monstre.joueTour(tourActuel);
            }
        }
        
        tourActuel++;
    }
    
    // IA basique des monstres (ancienne version)
    private void jouerMonstre(Monstre monstre) {
        try {
            // Chercher un héros à attaquer
            Heros cible = trouveHerosProche(monstre);
            
            if (cible != null) {
                monstre.combat(cible);
            } else {
                // Se déplacer aléatoirement
                Position nouvellePos = trouvePositionVide(monstre.getPosition());
                if (nouvellePos != null) {
                    deplaceSoldat(nouvellePos, monstre);
                }
            }
        } catch (CarteException e) {
            System.err.println("Erreur lors de l'action du monstre: " + e.getMessage());
        }
    }
    
    /**
     * IA intelligente des monstres.
     * 
     * @param monstre Le monstre qui doit jouer son tour
     */
    private void jouerMonstreIntelligent(Monstre monstre) {
        try {
            // 1. Chercher le héros le plus proche et le plus faible
            Heros cible = trouveMeilleureCible(monstre);
            
            if (cible != null) {
                // 2. Vérifier si on peut attaquer maintenant
                Position posMonstre = monstre.getPosition();
                Position posCible = cible.getPosition();
                
                if (peutAttaquerMonstre(monstre, cible)) {
                    // Attaquer directement
                    monstre.combat(cible);
                } else {
                    // 3. Se rapprocher stratégiquement du héros
                    Position meilleurePosition = trouverMeilleurePositionApproche(monstre, cible);
                    
                    if (meilleurePosition != null) {
                        deplaceSoldat(meilleurePosition, monstre);
                    } else {
                        // 4. Si pas de déplacement possible, chercher un autre héros ou se déplacer aléatoirement
                        Position posAleatoire = trouvePositionVide(posMonstre);
                        if (posAleatoire != null) {
                            deplaceSoldat(posAleatoire, monstre);
                        }
                    }
                }
            } else {
                // 5. Aucune cible visible, se déplacer vers le centre de la carte
                Position positionCentre = trouverPositionVersCentre(monstre);
                if (positionCentre != null) {
                    deplaceSoldat(positionCentre, monstre);
                } else {
                    // Fallback : déplacement aléatoire
                    Position posAleatoire = trouvePositionVide(monstre.getPosition());
                    if (posAleatoire != null) {
                        deplaceSoldat(posAleatoire, monstre);
                    }
                }
            }
        } catch (CarteException e) {
            System.err.println("Erreur lors de l'action intelligente du monstre: " + e.getMessage());
        }
    }
    
    // Trouve la meilleure cible (héros le plus proche et le plus faible)
    /**
     * Trouve le héros le plus approprié à attaquer pour un monstre donné.
     * 
     * @param monstre Le monstre qui cherche une cible
     * @return Le héros le plus approprié à attaquer, ou null s'il n'y en a pas
     */
    private Heros trouveMeilleureCible(Monstre monstre) {
        List<Heros> ciblesPotentielles = new ArrayList<>();
        Position posMonstre = monstre.getPosition();
        
        // Chercher tous les héros dans la portée visuelle du monstre
        for (Heros heros : this.heros) {
            if (heros.estVivant()) {
                Position posHeros = heros.getPosition();
                int distance = Math.abs(posMonstre.getX() - posHeros.getX()) + 
                              Math.abs(posMonstre.getY() - posHeros.getY());
                
                if (distance <= monstre.getPortee()) {
                    ciblesPotentielles.add(heros);
                }
            }
        }
        
        if (ciblesPotentielles.isEmpty()) {
            return null;
        }
        
        // Choisir la meilleure cible : priorité aux héros faibles et proches
        Heros meilleureCible = null;
        double meilleurScore = Double.NEGATIVE_INFINITY;
        
        for (Heros cible : ciblesPotentielles) {
            Position posCible = cible.getPosition();
            int distance = Math.abs(posMonstre.getX() - posCible.getX()) + 
                          Math.abs(posMonstre.getY() - posCible.getY());
            
            // Score = (1/distance) * (1/points de vie) * (si peut attaquer à distance ?)
            double score = (1.0 / (distance + 1)) * (1.0 / (cible.getPoints() + 1));
            
            // Bonus si on peut attaquer à distance
            if (monstre.getTir() > 0 && distance > 1) {
                score *= 2.0;
            }
            
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleureCible = cible;
            }
        }
        
        return meilleureCible;
    }
    
    // Vérifie si le monstre peut attaquer la cible (en tenant compte de la portée)
    /**
     * Vérifie si un monstre peut attaquer un héros selon les règles de portée et ligne de vue.
     * @param monstre
     * @param cible
     * @return
     */
    private boolean peutAttaquerMonstre(Monstre monstre, Heros cible) {
        Position posMonstre = monstre.getPosition();
        Position posCible = cible.getPosition();
        int distance = Math.abs(posMonstre.getX() - posCible.getX()) + 
                      Math.abs(posMonstre.getY() - posCible.getY());
        
        // Combat corps à corps
        if (distance == 1) {
            return true;
        }
        
        // Combat à distance
        if (distance > 1 && monstre.getTir() > 0 && distance <= monstre.getPortee()) {
            // Vérifier la ligne de vue (pas d'obstacles entre les deux)
            return aLigneDeVue(posMonstre, posCible);
        }
        
        return false;
    }
    
    // Vérifie la ligne de vue entre deux positions
    public boolean aLigneDeVue(Position pos1, Position pos2) {
        int x1 = pos1.getX();
        int y1 = pos1.getY();
        int x2 = pos2.getX();
        int y2 = pos2.getY();
        
        // Algorithme de Bresenham pour vérifier chaque case entre les deux positions
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        int x = x1;
        int y = y1;
        
        while (true) {
            // Ne pas vérifier la position de départ
            if (x != x1 || y != y1) {
                // Arrêter si on atteint la position d'arrivée
                if (x == x2 && y == y2) break;
                
                // Vérifier si la case contient un obstacle bloquant
                try {
                    Position pos = new Position(x, y);
                    Element element = grille[pos.getY()][pos.getX()];
                    if (element instanceof Obstacle) {
                        Obstacle obstacle = (Obstacle) element;
                        // Les rochers et forêts bloquent la ligne de vue
                        if (obstacle.getType() == Obstacle.TypeObstacle.ROCHER || 
                            obstacle.getType() == Obstacle.TypeObstacle.FORET) {
                            return false;
                        }
                    }
                } catch (PositionException e) {
                    // Position invalide
                    return false;
                }
            }
            
            if (x == x2 && y == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        
        return true;
    }
    
    // Trouve la meilleure position pour s'approcher de la cible
    /**
     * Trouve la meilleure position pour un monstre afin de s'approcher d'un héros cible.
     * Évalue les positions adjacentes et choisit celle qui minimise la distance à la cible.
     * 
     * @param monstre Le monstre qui cherche à se rapprocher
     * @param cible Le héros cible
     * @return La meilleure position d'approche, ou null si aucune disponible
     */
    private Position trouverMeilleurePositionApproche(Monstre monstre, Heros cible) {
        Position posMonstre = monstre.getPosition();
        Position posCible = cible.getPosition();
        List<Position> positionsPossibles = new ArrayList<>();
        
        // Évaluer les 8 positions adjacentes
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                try {
                    Position nouvellePos = new Position(posMonstre.getX() + dx, posMonstre.getY() + dy);
                    if (nouvellePos.estValide() && grille[nouvellePos.getY()][nouvellePos.getX()] == null) {
                        positionsPossibles.add(nouvellePos);
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        if (positionsPossibles.isEmpty()) {
            return null;
        }
        
        // Choisir la position qui rapproche le plus de la cible
        Position meilleurePosition = null;
        int meilleureDistance = Integer.MAX_VALUE;
        
        for (Position pos : positionsPossibles) {
            int distance = Math.abs(pos.getX() - posCible.getX()) + 
                          Math.abs(pos.getY() - posCible.getY());
            
            // Bonus si cette position permet d'attaquer
            boolean peutAttaquerDeLa = false;
            if (distance == 1 || (monstre.getTir() > 0 && distance <= monstre.getPortee() && 
                aLigneDeVue(pos, posCible))) {
                peutAttaquerDeLa = true;
            }
            
            // Si on peut attaquer depuis cette position, c'est la meilleure
            if (peutAttaquerDeLa) {
                return pos;
            }
            
            if (distance < meilleureDistance) {
                meilleureDistance = distance;
                meilleurePosition = pos;
            }
        }
        
        return meilleurePosition;
    }
    
    // Trouve une position qui se rapproche du centre de la carte
    /**
     * Trouve une position adjacente qui rapproche le monstre du centre de la carte.
     * 
     * Utile lorsque le monstre n'a pas de cible visible.
     * 
     * @param monstre Le monstre qui cherche à se rapprocher du centre
     * @return La position adjacente la plus proche du centre, ou null si aucune disponible
     */
    private Position trouverPositionVersCentre(Monstre monstre) {
        Position posMonstre = monstre.getPosition();
        int centreX = LARGEUR_CARTE / 2;
        int centreY = HAUTEUR_CARTE / 2;
        List<Position> positionsPossibles = new ArrayList<>();
        
        // Évaluer les positions adjacentes
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                try {
                    Position nouvellePos = new Position(posMonstre.getX() + dx, posMonstre.getY() + dy);
                    if (nouvellePos.estValide() && grille[nouvellePos.getY()][nouvellePos.getX()] == null) {
                        positionsPossibles.add(nouvellePos);
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        if (positionsPossibles.isEmpty()) {
            return null;
        }
        
        // Choisir la position qui rapproche le plus du centre
        Position meilleurePosition = null;
        int meilleureDistance = Integer.MAX_VALUE;
        
        for (Position pos : positionsPossibles) {
            int distance = Math.abs(pos.getX() - centreX) + Math.abs(pos.getY() - centreY);
            if (distance < meilleureDistance) {
                meilleureDistance = distance;
                meilleurePosition = pos;
            }
        }
        
        return meilleurePosition;
    }
    
    // Trouve un héros proche dans la portée du monstre
    /**
     * Trouve le héros le plus approprié à attaquer pour un monstre donné.
     * 
     * @param monstre Le monstre qui cherche une cible
     * @return Le héros le plus approprié à attaquer, ou null s'il n'y en a pas
     */
    private Heros trouveHerosProche(Monstre monstre) {
        Position posMonstre = monstre.getPosition();
        int portee = monstre.getPortee();
        
        for (int dx = -portee; dx <= portee; dx++) {
            for (int dy = -portee; dy <= portee; dy++) {
                try {
                    Position posCible = new Position(posMonstre.getX() + dx, posMonstre.getY() + dy);
                    if (posCible.estValide()) {
                        Element element = grille[posCible.getY()][posCible.getX()];
                        if (element instanceof Heros) {
                            Heros heros = (Heros) element;
                            if (heros.estVivant()) {
                                return heros;
                            }
                        }
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        return null;
    }
    
    public void toutDessiner(Graphics g, PanneauJeu panneau) {
        // Dessiner tous les éléments de la carte
        int width = panneau.getWidth();
        int height = panneau.getHeight();
        
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                Position pos = new Position(x, y, false);
                Element element = getElement(pos);
                
                if (element != null) {
                    panneau.dessinerElement((Graphics2D) g, element, pos, width, height);
                }
            }
        }
    }
    
    // Méthode pour nettoyer les soldats morts
    public void nettoyerSoldatsMorts() {
        // Nettoyer les héros morts
        Iterator<Heros> iterHeros = heros.iterator();
        while (iterHeros.hasNext()) {
            Heros h = iterHeros.next();
            if (!h.estVivant()) {
                iterHeros.remove();
            }
        }
        
        // Nettoyer les monstres morts
        Iterator<Monstre> iterMonstres = monstres.iterator();
        while (iterMonstres.hasNext()) {
            Monstre m = iterMonstres.next();
            if (!m.estVivant()) {
                iterMonstres.remove();
            }
        }
    }
    
    // Méthodes utilitaires
    public List<Heros> getHeros() {
        return new ArrayList<>(heros);
    }
    
    public List<Monstre> getMonstres() {
        return new ArrayList<>(monstres);
    }
    
    public List<Obstacle> getObstacles() {
        return new ArrayList<>(obstacles);
    }
    
    public boolean partieTerminee() {
        boolean herosVivant = false;
        for (Heros h : heros) {
            if (h.estVivant()) {
                herosVivant = true;
                break;
            }
        }
        
        boolean monstresVivants = false;
        for (Monstre m : monstres) {
            if (m.estVivant()) {
                monstresVivants = true;
                break;
            }
        }
        
        return !herosVivant || !monstresVivants;
    }
    
    /**
     * Détermine le gagnant de la partie.
     * @return "Héros", "Monstres", "Égalité" ou
     * "Partie en cours"
     */
    public String getGagnant() {
        boolean herosVivant = false;
        for (Heros h : heros) {
            if (h.estVivant()) {
                herosVivant = true;
                break;
            }
        }
        
        boolean monstresVivants = false;
        for (Monstre m : monstres) {
            if (m.estVivant()) {
                monstresVivants = true;
                break;
            }
        }
        
        if (!herosVivant && !monstresVivants) return "Égalité";
        if (!herosVivant) return "Monstres";
        if (!monstresVivants) return "Héros";
        return "Partie en cours";
    }
    
    public int getTourActuel() {
        return tourActuel;
    }
    
    // Obtenir les cases dans la portée d'un soldat
    /**
     * Obtient la liste des positions dans la portée d'un soldat.
     * @param soldat Le soldat dont on veut connaître la portée
     * @return Liste des positions dans la portée
     */
    public List<Position> getCasesPortee(Soldat soldat) {
        List<Position> casesPortee = new ArrayList<>();
        Position pos = soldat.getPosition();
        int portee = soldat.getPortee();
        
        for (int dx = -portee; dx <= portee; dx++) {
            for (int dy = -portee; dy <= portee; dy++) {
                try {
                    Position nouvellePos = new Position(pos.getX() + dx, pos.getY() + dy);
                    if (nouvellePos.estValide()) {
                        casesPortee.add(nouvellePos);
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        return casesPortee;
    }
    
    // Obtenir les cases de déplacement possibles
    /**
     * Obtient la liste des positions où un soldat peut se déplacer.
     * Considère uniquement les cases adjacentes vides.
     * @param soldat Le soldat dont on veut connaître les déplacements possibles
     * @return Liste des positions de déplacement possibles
     */
    public List<Position> getCasesDeplacement(Soldat soldat) {
        List<Position> casesDeplacement = new ArrayList<>();
        Position pos = soldat.getPosition();
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                try {
                    Position nouvellePos = new Position(pos.getX() + dx, pos.getY() + dy);
                    if (nouvellePos.estValide() && grille[nouvellePos.getY()][nouvellePos.getX()] == null) {
                        casesDeplacement.add(nouvellePos);
                    }
                } catch (PositionException e) {
                    // Position invalide, on ignore
                }
            }
        }
        
        return casesDeplacement;
    }
    
    /**
     * Calcule la zone visible de la carte en fonction des héros vivants.
     * @return
     */
    private boolean[][] calculerZoneVisible() {
        boolean[][] zoneVisible = new boolean[HAUTEUR_CARTE][LARGEUR_CARTE];
        
        // Pour chaque héros vivant, calculer sa zone de vision
        for (Heros heros : this.heros) {
            if (heros.estVivant()) {
                Position posHeros = heros.getPosition();
                int portee = heros.getPortee();
                
                for (int dx = -portee; dx <= portee; dx++) {
                    for (int dy = -portee; dy <= portee; dy++) {
                        int x = posHeros.getX() + dx;
                        int y = posHeros.getY() + dy;
                        
                        if (x >= 0 && x < LARGEUR_CARTE && y >= 0 && y < HAUTEUR_CARTE) {
                            zoneVisible[y][x] = true;
                        }
                    }
                }
            }
        }
        
        return zoneVisible;
    }

    /**
     * Sauvegarde la carte dans un fichier sérialisé.
     * @param nomFichier
     * @throws SauvegardeException
     */
    public void sauvegarder(String nomFichier) throws SauvegardeException {
        if (!nomFichier.endsWith(".ser")) {
            nomFichier += ".ser";
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(nomFichier))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new SauvegardeException("Erreur lors de la sauvegarde", e);
        }
    }
    
    /**
     * Restaure une carte à partir d'un fichier sérialisé.
     * @param nomFichier
     * @return
     * @throws SauvegardeException
     */
    public static Carte restaurer(String nomFichier) throws SauvegardeException {
        if (!nomFichier.endsWith(".ser")) {
            nomFichier += ".ser";
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(nomFichier))) {
            Carte carte = (Carte) ois.readObject();
            // Réinitialiser le générateur aléatoire
            carte.random = new Random();
            return carte;
        } catch (IOException | ClassNotFoundException e) {
            throw new SauvegardeException("Erreur lors de la restauration", e);
        }
    }

    /**
     * Obtient la liste des sauvegardes disponibles dans le répertoire courant.
     * @return
     */
    public static List<String> getListeSauvegardes() {
        List<String> sauvegardes = new ArrayList<>();
        File dossier = new File(".");
        File[] fichiers = dossier.listFiles((dir, name) -> name.endsWith(".ser"));
        
        if (fichiers != null) {
            for (File fichier : fichiers) {
                sauvegardes.add(fichier.getName().replace(".ser", ""));
            }
        }
        
        return sauvegardes;
    }
}