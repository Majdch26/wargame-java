package wargame;

import java.util.ArrayList;
import java.util.List;

/**
 * Stratégie « Difficile » du général-ordinateur.
 *
 * <ul>
 *   <li><b>Vision partagée</b> : un monstre connaît tous les héros vus par l'armée des monstres,
 *       et se souvient de leur dernière position quand ils disparaissent de la vue.</li>
 *   <li><b>Choix de la cible</b> : chaque attaque possible reçoit une note qui tient compte des chances
 *       d'achever le héros, des dégâts attendus, du danger que représente ce héros et de la riposte probable.</li>
 *   <li><b>Déplacement</b> : un parcours en largeur calcule, en contournant obstacles et soldats,
 *       le chemin le plus court vers une case d'où le monstre pourra attaquer (au contact pour un troll,
 *       à distance avec une ligne de vue dégagée pour un archer). À distance égale, il choisit
 *       la case la moins exposée.</li>
 *   <li><b>Repos</b> : un monstre gravement blessé et hors de danger se repose au lieu d'avancer.</li>
 * </ul>
 */
public class StrategieTactique implements StrategieMonstres {
    private static final long serialVersionUID = 1L;

    /** En dessous de cette part de ses points de vie, un monstre hors de danger se repose. */
    private static final double SEUIL_REPOS = 0.4;

    private final List<Position> derniersReperes = new ArrayList<>();
    private transient List<Heros> herosConnus;

    @Override
    public void debutTour(Carte carte) {
        herosConnus = new ArrayList<>();
        boolean[][] vu = carte.zoneVisible(carte.getMonstres());
        for (Heros h : carte.getHeros()) {
            if (vu[h.getPosition().getY()][h.getPosition().getX()]) {
                herosConnus.add(h);
            }
        }
        if (!herosConnus.isEmpty()) {
            derniersReperes.clear();
            for (Heros h : herosConnus) {
                derniersReperes.add(h.getPosition());
            }
        }
    }

    @Override
    public void jouer(Carte carte, Monstre monstre) {
        if (herosConnus == null) {
            debutTour(carte);
        }
        herosConnus.removeIf(h -> !h.estVivant());

        // 1. Attaquer la meilleure cible à portée
        Heros cible = null;
        double meilleure = Double.NEGATIVE_INFINITY;
        for (Heros h : carte.getHeros()) {
            if (carte.peutAttaquer(monstre, h)) {
                double note = noteAttaque(carte, monstre, h);
                if (note > meilleure) {
                    meilleure = note;
                    cible = h;
                }
            }
        }
        if (cible != null) {
            monstre.combat(cible);
            return;
        }

        // 2. Blessé et hors de danger : se reposer
        if (monstre.getPoints() < SEUIL_REPOS * monstre.getPointsMax()
                && danger(carte, monstre.getPosition(), monstre) == 0) {
            monstre.repos();
            return;
        }

        // 3. Avancer vers une case d'attaque, sinon vers la dernière position connue des héros
        List<Position> objectifs = casesDAttaque(carte, monstre);
        if (objectifs.isEmpty()) {
            objectifs = new ArrayList<>(derniersReperes);
        }
        if (objectifs.isEmpty()) {
            // Personne en vue ni en mémoire : on explore les cases que l'armée ne voit pas
            objectifs = casesInexplorees(carte);
        }
        if (!avancerVers(carte, monstre, objectifs)) {
            StrategieMonstres.deplacementAleatoire(carte, monstre);
        }
        // Repère atteint sans trouver personne : on l'oublie
        derniersReperes.removeIf(p -> p.distance(monstre.getPosition()) <= 1);
    }

    /**
     * Note d'une attaque : achever un héros vaut beaucoup, encaisser une forte riposte coûte cher.
     */
    static double noteAttaque(Carte carte, Soldat attaquant, Soldat cible) {
        int p = attaquant.puissanceContre(cible);
        double chanceAchever = Math.max(0, p - cible.getPoints() + 1) / (double) (p + 1);
        double degats = Math.min(p / 2.0, cible.getPoints());
        double riposte = carte.peutAttaquer(cible, attaquant) ? cible.puissanceContre(attaquant) / 2.0 * (1 - chanceAchever) : 0;
        double menace = (cible.getPuissance() + cible.getTir()) / 40.0;
        return 5 * chanceAchever + 2 * degats / cible.getPointsMax() + menace
                - 2 * riposte / Math.max(1, attaquant.getPoints());
    }

    /**
     * Dégâts moyens qu'un soldat posté en pos risque de subir au prochain tour adverse.
     */
    static double danger(Carte carte, Position pos, Soldat soldat) {
        List<? extends Soldat> ennemis = soldat instanceof Monstre ? carte.getHeros() : carte.getMonstres();
        double total = 0;
        for (Soldat e : ennemis) {
            int d = e.getPosition().distance(pos);
            double menace = 0;
            if (d <= 2) {
                menace = e.getPuissance() / 2.0;                  // il peut faire un pas et frapper
            }
            if (e.getTir() > 0 && d <= e.getPortee() + 1 && d > 1) {
                menace = Math.max(menace, e.getTir() / 2.0);      // ou tirer
            }
            total += menace;
        }
        return total;
    }

    /**
     * Cases libres hors de la vue des monstres : les héros s'y cachent peut-être.
     * Le camp de départ des héros (la moitié gauche) est fouillé en premier.
     */
    private List<Position> casesInexplorees(Carte carte) {
        boolean[][] vu = carte.zoneVisible(carte.getMonstres());
        List<Position> campHeros = new ArrayList<>();
        List<Position> ailleurs = new ArrayList<>();
        for (int y = 0; y < IConfig.HAUTEUR_CARTE; y++) {
            for (int x = 0; x < IConfig.LARGEUR_CARTE; x++) {
                Position p = new Position(x, y, false);
                if (!vu[y][x] && carte.estLibre(p)) {
                    (x < IConfig.LARGEUR_CARTE / 2 ? campHeros : ailleurs).add(p);
                }
            }
        }
        return campHeros.isEmpty() ? ailleurs : campHeros;
    }

    /** Cases (libres) d'où le monstre pourrait attaquer un des héros connus. */
    private List<Position> casesDAttaque(Carte carte, Monstre monstre) {
        List<Position> cases = new ArrayList<>();
        int rayon = monstre.getTir() > 0 ? monstre.getPortee() : 1;
        for (Heros h : herosConnus) {
            Position ph = h.getPosition();
            for (int dy = -rayon; dy <= rayon; dy++) {
                for (int dx = -rayon; dx <= rayon; dx++) {
                    Position c = new Position(ph.getX() + dx, ph.getY() + dy, false);
                    if (c.estValide() && (carte.estLibre(c) || c.equals(monstre.getPosition()))
                            && !c.equals(ph) && carte.pourraitAttaquer(monstre, c, h)) {
                        cases.add(c);
                    }
                }
            }
        }
        return cases;
    }

    /**
     * Fait un pas sur le plus court chemin vers une des cases objectif.
     * @return faux si aucun pas ne rapproche le monstre de ses objectifs
     */
    private boolean avancerVers(Carte carte, Monstre monstre, List<Position> objectifs) {
        Position ici = monstre.getPosition();
        int[][] dist = carte.distancesDeMarche(objectifs, ici, Monstre.class);
        int actuelle = dist[ici.getY()][ici.getX()];

        Position choix = null;
        int meilleureDistance = actuelle;
        double meilleurDanger = Double.MAX_VALUE;
        for (Position p : carte.voisinesLibres(ici)) {
            int d = dist[p.getY()][p.getX()];
            if (d == Integer.MAX_VALUE) {
                continue;
            }
            double risque = danger(carte, p, monstre);
            if (d < meilleureDistance || (d == meilleureDistance && choix != null && risque < meilleurDanger)) {
                meilleureDistance = d;
                meilleurDanger = risque;
                choix = p;
            }
        }
        if (choix == null) {
            return false;
        }
        StrategieMonstres.deplacer(carte, monstre, choix);
        return true;
    }
}
