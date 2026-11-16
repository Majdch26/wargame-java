package wargame;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Gère la carte de jeu avec tous les éléments (soldats, obstacles).
 * Contrôle le déroulement des tours de jeu et les interactions entre les soldats.
 *
 * <p>La carte est une grille de {@value IConfig#LARGEUR_CARTE}x{@value IConfig#HAUTEUR_CARTE} cases.
 * Chaque case contient au plus un élément : un héros, un monstre ou un obstacle.
 * Les soldats sont placés aléatoirement au démarrage : héros dans la moitié gauche,
 * monstres dans la moitié droite.</p>
 *
 * <p>Les distances se comptent en cases, diagonales comprises : deux soldats qui se touchent
 * (y compris en diagonale) combattent au corps-à-corps, sinon il s'agit d'un tir.</p>
 *
 * <p>La carte est sérialisable pour permettre la sauvegarde et la restauration des parties.</p>
 */
public class Carte implements ICarte, IConfig, Serializable {
    private static final long serialVersionUID = 2L;

    /** Dossier des sauvegardes, créé à côté du jeu. */
    public static final String DOSSIER_SAUVEGARDES = "sauvegardes";

    private final Element[][] grille;
    private final List<Heros> heros;
    private final List<Monstre> monstres;
    private final List<Obstacle> obstacles;
    private int tourActuel;
    private final java.util.LinkedList<String> journal = new java.util.LinkedList<>();
    private Difficulte difficulte;
    private StrategieMonstres strategie;

    /** Crée une partie de difficulté normale. */
    public Carte() {
        this(Difficulte.NORMAL);
    }

    /**
     * Crée une partie : place aléatoirement obstacles, héros et monstres.
     * @param difficulte comportement des monstres
     */
    public Carte(Difficulte difficulte) {
        this.grille = new Element[HAUTEUR_CARTE][LARGEUR_CARTE];
        this.heros = new ArrayList<>();
        this.monstres = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        this.tourActuel = 0;
        setDifficulte(difficulte);
        initialiserCarte();
    }

    /** Carte vide, pour les tests : les éléments sont ajoutés un par un. */
    Carte(Difficulte difficulte, boolean vide) {
        this.grille = new Element[HAUTEUR_CARTE][LARGEUR_CARTE];
        this.heros = new ArrayList<>();
        this.monstres = new ArrayList<>();
        this.obstacles = new ArrayList<>();
        setDifficulte(difficulte);
        if (!vide) {
            initialiserCarte();
        }
    }

    /** Place les obstacles, puis les héros à gauche et les monstres à droite. */
    private void initialiserCarte() {
        for (int i = 0; i < NB_OBSTACLES; i++) {
            Position pos = trouvePositionVide();
            if (pos != null) {
                ajouter(new Obstacle(Obstacle.TypeObstacle.getObstacleAlea(), pos));
            }
        }
        for (int i = 0; i < NB_HEROS; i++) {
            Position pos = positionVideDansColonnes(0, LARGEUR_CARTE / 2);
            if (pos != null) {
                ajouter(new Heros(this, ISoldat.TypesH.getTypeHAlea(), String.valueOf((char) ('A' + i)), pos));
            }
        }
        for (int i = 0; i < NB_MONSTRES; i++) {
            Position pos = positionVideDansColonnes(LARGEUR_CARTE / 2 + 1, LARGEUR_CARTE);
            if (pos != null) {
                ajouter(new Monstre(this, ISoldat.TypesM.getTypeMAlea(), i + 1, pos));
            }
        }
    }

    /**
     * Ajoute un élément sur une case vide de la carte.
     * @param element l'élément, déjà positionné
     */
    void ajouter(Element element) {
        Position pos = element.getPosition();
        grille[pos.getY()][pos.getX()] = element;
        if (element instanceof Heros) {
            heros.add((Heros) element);
        } else if (element instanceof Monstre) {
            monstres.add((Monstre) element);
        } else if (element instanceof Obstacle) {
            obstacles.add((Obstacle) element);
        }
    }

    private Position positionVideDansColonnes(int xMin, int xMax) {
        List<Position> libres = new ArrayList<>();
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = xMin; x < xMax; x++) {
                if (grille[y][x] == null) {
                    libres.add(new Position(x, y, false));
                }
            }
        }
        return libres.isEmpty() ? null : libres.get(Hasard.entier(libres.size()));
    }

    // ------------------------------------------------------------------
    // Consultation de la carte
    // ------------------------------------------------------------------

    @Override
    public Element getElement(Position pos) {
        return pos.estValide() ? grille[pos.getY()][pos.getX()] : null;
    }

    /**
     * @param pos une position
     * @return vrai si la position est sur la carte et ne contient rien
     */
    public boolean estLibre(Position pos) {
        return pos.estValide() && grille[pos.getY()][pos.getX()] == null;
    }

    /**
     * Les cases sur la carte autour d'une position (8 au plus).
     * @param pos la position centrale
     * @return les positions voisines valides
     */
    public List<Position> voisines(Position pos) {
        List<Position> resultat = new ArrayList<>(8);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                Position p = new Position(pos.getX() + dx, pos.getY() + dy, false);
                if ((dx != 0 || dy != 0) && p.estValide()) {
                    resultat.add(p);
                }
            }
        }
        return resultat;
    }

    /**
     * Les cases vides autour d'une position.
     * @param pos la position centrale
     * @return les positions voisines libres
     */
    public List<Position> voisinesLibres(Position pos) {
        List<Position> resultat = new ArrayList<>(8);
        for (Position p : voisines(pos)) {
            if (estLibre(p)) {
                resultat.add(p);
            }
        }
        return resultat;
    }

    @Override
    public Position trouvePositionVide() {
        return positionVideDansColonnes(0, LARGEUR_CARTE);
    }

    @Override
    public Position trouvePositionVide(Position pos) {
        List<Position> libres = voisinesLibres(pos);
        return libres.isEmpty() ? null : libres.get(Hasard.entier(libres.size()));
    }

    @Override
    public Heros trouveHeros() {
        List<Heros> vivants = getHeros();
        return vivants.isEmpty() ? null : vivants.get(Hasard.entier(vivants.size()));
    }

    @Override
    public Heros trouveHeros(Position pos) {
        List<Heros> proches = new ArrayList<>();
        for (Position p : voisines(pos)) {
            if (getElement(p) instanceof Heros) {
                proches.add((Heros) getElement(p));
            }
        }
        return proches.isEmpty() ? null : proches.get(Hasard.entier(proches.size()));
    }

    // ------------------------------------------------------------------
    // Vision, ligne de vue, portée
    // ------------------------------------------------------------------

    /**
     * Ligne de vue entre deux cases (algorithme de Bresenham) : un rocher ou une forêt
     * sur le trajet bloque les tirs. L'eau ne bloque pas la vue.
     * @param pos1 case de départ
     * @param pos2 case d'arrivée
     * @return vrai si rien ne bloque la vue entre les deux cases
     */
    public boolean aLigneDeVue(Position pos1, Position pos2) {
        int x = pos1.getX(), y = pos1.getY();
        int x2 = pos2.getX(), y2 = pos2.getY();
        int dx = Math.abs(x2 - x), dy = Math.abs(y2 - y);
        int sx = x < x2 ? 1 : -1, sy = y < y2 ? 1 : -1;
        int err = dx - dy;

        while (x != x2 || y != y2) {
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
            if (x == x2 && y == y2) {
                break;
            }
            Element e = grille[y][x];
            if (e instanceof Obstacle && ((Obstacle) e).getType() != Obstacle.TypeObstacle.EAU) {
                return false;
            }
        }
        return true;
    }

    /**
     * Un soldat peut attaquer une cible s'il la touche (corps-à-corps, diagonales comprises),
     * ou s'il sait tirer, qu'elle est dans sa portée et qu'aucun obstacle ne bloque le tir.
     * @param attaquant le soldat qui attaque
     * @param cible le soldat visé
     * @return vrai si l'attaque est possible
     */
    public boolean peutAttaquer(Soldat attaquant, Soldat cible) {
        if (!attaquant.estVivant() || !cible.estVivant()) {
            return false;
        }
        int d = attaquant.getPosition().distance(cible.getPosition());
        if (d == 1) {
            return true;
        }
        return d > 1 && attaquant.getTir() > 0 && d <= attaquant.getPortee()
                && aLigneDeVue(attaquant.getPosition(), cible.getPosition());
    }

    /**
     * Même règle que {@link #peutAttaquer} si l'attaquant se trouvait sur une autre case.
     * @param attaquant le soldat
     * @param depuis case hypothétique de l'attaquant
     * @param cible le soldat visé
     * @return vrai si l'attaque serait possible depuis cette case
     */
    public boolean pourraitAttaquer(Soldat attaquant, Position depuis, Soldat cible) {
        int d = depuis.distance(cible.getPosition());
        if (d == 1) {
            return true;
        }
        return d > 1 && attaquant.getTir() > 0 && d <= attaquant.getPortee()
                && aLigneDeVue(depuis, cible.getPosition());
    }

    /**
     * Cases vues par une armée : l'union des carrés de vision de ses soldats vivants.
     * @param soldats les soldats de l'armée
     * @return un tableau [y][x] : vrai si la case est visible
     */
    public boolean[][] zoneVisible(List<? extends Soldat> soldats) {
        boolean[][] visible = new boolean[HAUTEUR_CARTE][LARGEUR_CARTE];
        for (Soldat s : soldats) {
            if (!s.estVivant()) {
                continue;
            }
            int p = s.getPortee();
            for (int y = Math.max(0, s.getPosition().getY() - p); y <= Math.min(HAUTEUR_CARTE - 1, s.getPosition().getY() + p); y++) {
                for (int x = Math.max(0, s.getPosition().getX() - p); x <= Math.min(LARGEUR_CARTE - 1, s.getPosition().getX() + p); x++) {
                    visible[y][x] = true;
                }
            }
        }
        return visible;
    }

    /** @return les cases vues par l'armée des héros (le reste est grisé à l'écran) */
    public boolean[][] zoneVisibleHeros() {
        return zoneVisible(heros);
    }

    /**
     * Distance de marche depuis un ensemble de cases de départ, en passant seulement
     * par des cases vides (parcours en largeur). Sert à l'ordinateur pour contourner
     * les obstacles et les autres soldats.
     * @param departs les cases de départ (distance 0)
     * @param traversable case supplémentaire considérée comme libre (le soldat qui se déplace), ou null
     * @return un tableau [y][x] des distances, Integer.MAX_VALUE si la case n'est pas atteignable
     */
    public int[][] distancesDeMarche(List<Position> departs, Position traversable) {
        return distancesDeMarche(departs, traversable, null);
    }

    /**
     * Comme {@link #distancesDeMarche(List, Position)}, mais les cases occupées par des soldats
     * du camp indiqué comptent comme franchissables (ils se seront déplacés d'ici là) :
     * une armée ne se bloque pas elle-même dans un couloir.
     * @param departs les cases de départ (distance 0)
     * @param traversable case supplémentaire considérée comme libre, ou null
     * @param allies classe des soldats que l'on peut contourner plus tard (Monstre.class...), ou null
     * @return un tableau [y][x] des distances, Integer.MAX_VALUE si la case n'est pas atteignable
     */
    public int[][] distancesDeMarche(List<Position> departs, Position traversable, Class<? extends Soldat> allies) {
        int[][] dist = new int[HAUTEUR_CARTE][LARGEUR_CARTE];
        for (int[] ligne : dist) {
            Arrays.fill(ligne, Integer.MAX_VALUE);
        }
        Deque<Position> file = new ArrayDeque<>();
        for (Position p : departs) {
            if (dist[p.getY()][p.getX()] != 0) {
                dist[p.getY()][p.getX()] = 0;
                file.add(p);
            }
        }
        while (!file.isEmpty()) {
            Position p = file.poll();
            int suivante = dist[p.getY()][p.getX()] + 1;
            for (Position v : voisines(p)) {
                boolean libre = estLibre(v) || v.equals(traversable)
                        || (allies != null && allies.isInstance(getElement(v)));
                if (libre && dist[v.getY()][v.getX()] > suivante) {
                    dist[v.getY()][v.getX()] = suivante;
                    file.add(v);
                }
            }
        }
        return dist;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    @Override
    public boolean deplaceSoldat(Position nouvellePos, Soldat soldat) throws CarteException {
        if (!nouvellePos.estValide()) {
            throw new CarteException("Position de destination invalide : " + nouvellePos);
        }
        if (grille[nouvellePos.getY()][nouvellePos.getX()] != null) {
            throw new CarteException("Case de destination occupée : " + nouvellePos);
        }
        Position ancienne = soldat.getPosition();
        grille[ancienne.getY()][ancienne.getX()] = null;
        grille[nouvellePos.getY()][nouvellePos.getX()] = soldat;
        soldat.seDeplace(nouvellePos);
        return true;
    }

    /** Retire un soldat mort de la carte et de son armée. */
    @Override
    public void mort(Soldat soldat) {
        Position pos = soldat.getPosition();
        if (pos.estValide() && grille[pos.getY()][pos.getX()] == soldat) {
            grille[pos.getY()][pos.getX()] = null;
        }
        heros.remove(soldat);
        monstres.remove(soldat);
    }

    /**
     * Le héros en pos agit vers pos2 : il s'y déplace si la case voisine est vide,
     * ou attaque le monstre qui s'y trouve s'il peut l'atteindre.
     * @return vrai si l'action a eu lieu
     * @throws SoldatException pas de héros, héros ayant déjà joué, cible hors de portée...
     * @throws CarteException case occupée
     * @throws PositionException position hors de la carte
     */
    @Override
    public boolean actionHeros(Position pos, Position pos2) throws SoldatException, CarteException, PositionException {
        if (!pos.estValide() || !pos2.estValide()) {
            throw new PositionException("Position hors de la carte");
        }
        if (!(getElement(pos) instanceof Heros)) {
            throw new SoldatException("Il n'y a pas de héros sur cette case");
        }
        Heros h = (Heros) getElement(pos);
        if (!h.peutJouer(tourActuel)) {
            throw new SoldatException(h.getNom() + " a déjà joué ce tour");
        }
        if (pos.equals(pos2)) {
            return false;
        }

        Element cible = getElement(pos2);
        if (cible == null) {
            if (!pos.estVoisine(pos2)) {
                throw new SoldatException("Un héros se déplace d'une seule case à la fois");
            }
            deplaceSoldat(pos2, h);
            h.joueTour(tourActuel);
            return true;
        }
        if (cible instanceof Monstre) {
            Monstre m = (Monstre) cible;
            if (!peutAttaquer(h, m)) {
                throw new SoldatException(h.getTir() == 0
                        ? h.getNomType() + " " + h.getNom() + " ne sait pas tirer : approchez-vous du monstre"
                        : "Monstre hors de portée ou tir bloqué par un obstacle");
            }
            h.combat(m);
            h.joueTour(tourActuel);
            return true;
        }
        if (cible instanceof Heros) {
            throw new SoldatException("Cette case est occupée par un autre héros");
        }
        throw new CarteException("Cette case est occupée par un obstacle");
    }

    /**
     * Termine le tour : les héros qui n'ont pas reçu d'ordre se reposent,
     * puis chaque monstre joue selon la stratégie de la difficulté choisie.
     * @param pj panneau à redessiner (peut être null)
     */
    @Override
    public void jouerSoldats(PanneauJeu pj) {
        for (Heros h : getHeros()) {
            if (h.peutJouer(tourActuel)) {
                h.repos();
                h.joueTour(tourActuel);
            }
        }
        strategie.debutTour(this);
        for (Monstre m : getMonstres()) {
            if (m.estVivant() && !heros.isEmpty()) {
                strategie.jouer(this, m);
                m.joueTour(tourActuel);
            }
        }
        tourActuel++;
        if (pj != null) {
            pj.redessiner();
        }
    }

    @Override
    public void toutDessiner(Graphics g, PanneauJeu panneau) {
        for (int y = 0; y < HAUTEUR_CARTE; y++) {
            for (int x = 0; x < LARGEUR_CARTE; x++) {
                Position pos = new Position(x, y, false);
                if (getElement(pos) != null) {
                    panneau.dessinerElement((Graphics2D) g, getElement(pos), pos, panneau.getWidth(), panneau.getHeight());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // État de la partie
    // ------------------------------------------------------------------

    /** @return les héros vivants (copie) */
    public List<Heros> getHeros() {
        return new ArrayList<>(heros);
    }

    /** @return les monstres vivants (copie) */
    public List<Monstre> getMonstres() {
        return new ArrayList<>(monstres);
    }

    /** @return les obstacles (copie non modifiable) */
    public List<Obstacle> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    /** @return vrai quand une des deux armées a disparu */
    public boolean partieTerminee() {
        return heros.isEmpty() || monstres.isEmpty();
    }

    /** @return "Héros", "Monstres" ou "Partie en cours" */
    public String getGagnant() {
        if (monstres.isEmpty()) {
            return "Héros";
        }
        if (heros.isEmpty()) {
            return "Monstres";
        }
        return "Partie en cours";
    }

    /**
     * Ajoute une ligne au journal des combats (les 200 dernières sont gardées).
     * @param ligne le texte
     */
    public void noter(String ligne) {
        journal.addFirst("Tour " + (tourActuel + 1) + " · " + ligne);
        if (journal.size() > 200) {
            journal.removeLast();
        }
    }

    /** @return le journal, de l'événement le plus récent au plus ancien */
    public List<String> getJournal() {
        return new ArrayList<>(journal);
    }

    /** @return le numéro du tour en cours (0 au départ) */
    public int getTourActuel() {
        return tourActuel;
    }

    /** @return la difficulté de la partie */
    public Difficulte getDifficulte() {
        return difficulte;
    }

    /**
     * Change le comportement des monstres.
     * @param difficulte la nouvelle difficulté
     */
    public final void setDifficulte(Difficulte difficulte) {
        this.difficulte = difficulte;
        this.strategie = difficulte.creerStrategie();
    }

    /**
     * Cases à portée d'un soldat (carré de vision).
     * @param soldat le soldat
     * @return les positions à portée
     */
    public List<Position> getCasesPortee(Soldat soldat) {
        List<Position> cases = new ArrayList<>();
        int p = soldat.getPortee();
        Position pos = soldat.getPosition();
        for (int dy = -p; dy <= p; dy++) {
            for (int dx = -p; dx <= p; dx++) {
                Position c = new Position(pos.getX() + dx, pos.getY() + dy, false);
                if (c.estValide()) {
                    cases.add(c);
                }
            }
        }
        return cases;
    }

    /**
     * Cases où un soldat peut se déplacer ce tour-ci.
     * @param soldat le soldat
     * @return les cases voisines vides
     */
    public List<Position> getCasesDeplacement(Soldat soldat) {
        return voisinesLibres(soldat.getPosition());
    }

    /**
     * Monstres que le héros peut attaquer depuis sa case.
     * @param h le héros
     * @return les cibles possibles
     */
    public List<Monstre> getCiblesPossibles(Heros h) {
        List<Monstre> cibles = new ArrayList<>();
        for (Monstre m : monstres) {
            if (peutAttaquer(h, m)) {
                cibles.add(m);
            }
        }
        return cibles;
    }

    // ------------------------------------------------------------------
    // Sauvegarde
    // ------------------------------------------------------------------

    private static File fichierSauvegarde(String nom) throws SauvegardeException {
        String propre = nom.trim().replaceAll("\\.ser$", "");
        if (!propre.matches("[\\p{L}\\p{N} _-]{1,40}")) {
            throw new SauvegardeException("Nom de sauvegarde invalide : utilisez des lettres, chiffres, espaces, - ou _ (40 caractères au plus)");
        }
        return new File(DOSSIER_SAUVEGARDES, propre + ".ser");
    }

    /**
     * Sauvegarde la partie dans sauvegardes/nom.ser.
     * @param nom le nom de la sauvegarde
     * @throws SauvegardeException nom invalide ou erreur d'écriture
     */
    public void sauvegarder(String nom) throws SauvegardeException {
        File fichier = fichierSauvegarde(nom);
        fichier.getParentFile().mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fichier))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new SauvegardeException("Impossible d'écrire la sauvegarde : " + e.getMessage(), e);
        }
    }

    /**
     * Restaure une partie sauvegardée.
     * @param nom le nom de la sauvegarde
     * @return la carte restaurée
     * @throws SauvegardeException sauvegarde inexistante ou illisible
     */
    public static Carte restaurer(String nom) throws SauvegardeException {
        File fichier = fichierSauvegarde(nom);
        if (!fichier.exists()) {
            throw new SauvegardeException("La sauvegarde « " + nom + " » n'existe pas");
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichier))) {
            return (Carte) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new SauvegardeException("Sauvegarde illisible (peut-être créée par une ancienne version du jeu)", e);
        }
    }

    /** @return les noms des sauvegardes disponibles, de la plus récente à la plus ancienne */
    public static List<String> getListeSauvegardes() {
        File[] fichiers = new File(DOSSIER_SAUVEGARDES).listFiles((d, n) -> n.endsWith(".ser"));
        List<String> noms = new ArrayList<>();
        if (fichiers != null) {
            Arrays.sort(fichiers, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : fichiers) {
                noms.add(f.getName().substring(0, f.getName().length() - 4));
            }
        }
        return noms;
    }
}
