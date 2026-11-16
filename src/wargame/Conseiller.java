package wargame;

import java.util.ArrayList;
import java.util.List;

/**
 * Conseiller du joueur : une IA qui évalue toutes les actions possibles des héros
 * (attaquer, se déplacer, se reposer) et propose la meilleure, avec une explication.
 *
 * <p>Il ne voit que ce que voient les héros (les monstres dans le brouillard sont ignorés).
 * Chaque action reçoit une note :</p>
 * <ul>
 *   <li>une attaque vaut par ses chances d'achever le monstre, les dégâts attendus et la menace
 *       qu'il représente, moins la riposte probable ;</li>
 *   <li>un déplacement vaut par le danger évité ou la position d'attaque gagnée pour le tour suivant ;</li>
 *   <li>le repos vaut par les points de vie regagnés quand le héros est blessé et à l'abri.</li>
 * </ul>
 * <p>Le même calcul permet de jouer automatiquement le tour des héros.</p>
 */
public class Conseiller {

    /** Types d'action possibles pour un héros. */
    public enum TypeAction { ATTAQUE, DEPLACEMENT, REPOS }

    /** Une action proposée pour un héros, avec sa note et son explication. */
    public static final class Conseil {
        private final Heros heros;
        private final TypeAction type;
        private final Position cible;
        private final double note;
        private final String explication;

        Conseil(Heros heros, TypeAction type, Position cible, double note, String explication) {
            this.heros = heros;
            this.type = type;
            this.cible = cible;
            this.note = note;
            this.explication = explication;
        }

        /** @return le héros concerné */
        public Heros getHeros() { return heros; }
        /** @return le type d'action */
        public TypeAction getType() { return type; }
        /** @return la case visée (déplacement ou attaque), la case du héros pour le repos */
        public Position getCible() { return cible; }
        /** @return la note de l'action (plus elle est haute, meilleure est l'action) */
        public double getNote() { return note; }
        /** @return une phrase qui explique le conseil */
        public String getExplication() { return explication; }

        @Override
        public String toString() {
            return explication;
        }
    }

    private final Carte carte;

    /**
     * @param carte la partie à analyser
     */
    public Conseiller(Carte carte) {
        this.carte = carte;
    }

    /** Monstres visibles par l'armée des héros. */
    private List<Monstre> monstresVisibles() {
        boolean[][] vu = carte.zoneVisibleHeros();
        List<Monstre> visibles = new ArrayList<>();
        for (Monstre m : carte.getMonstres()) {
            if (vu[m.getPosition().getY()][m.getPosition().getX()]) {
                visibles.add(m);
            }
        }
        return visibles;
    }

    /**
     * Dégâts moyens que le héros risque de subir sur une case au prochain tour des monstres
     * (seuls les monstres visibles sont pris en compte).
     */
    private double danger(Position pos, List<Monstre> monstres, Monstre ignore) {
        double total = 0;
        for (Monstre m : monstres) {
            if (m == ignore) {
                continue;
            }
            int d = m.getPosition().distance(pos);
            double menace = 0;
            if (d <= 2) {
                menace = m.getPuissance() / 2.0;
            }
            if (m.getTir() > 0 && d > 1 && d <= m.getPortee() + 1) {
                menace = Math.max(menace, m.getTir() / 2.0);
            }
            total += menace;
        }
        return total;
    }

    /**
     * Évalue toutes les actions d'un héros.
     * @param h un héros qui n'a pas encore joué
     * @return la meilleure action pour ce héros
     */
    public Conseil meilleurePour(Heros h) {
        List<Monstre> visibles = monstresVisibles();
        double pv = Math.max(1, h.getPoints());
        double sante = h.getPoints() / (double) h.getPointsMax();
        double dangerIci = danger(h.getPosition(), visibles, null);
        Conseil meilleur;

        // Repos ou attente sur place (les points regagnés comptent plus quand le héros est blessé ;
        // garder sa position face à des monstres visibles vaut mieux que foncer sur eux)
        int regain = Math.min(Soldat.POINTS_REPOS, h.getPointsMax() - h.getPoints());
        double noteRepos = (1 - sante) * 2 + regain * 0.1 - 3 * dangerIci / pv + (visibles.isEmpty() ? 0 : 0.05);
        meilleur = new Conseil(h, TypeAction.REPOS, h.getPosition(), noteRepos,
                regain > 0 ? nom(h) + " se repose (+" + regain + " PV)"
                           : nom(h) + " garde sa position");

        // Attaques
        for (Monstre m : visibles) {
            if (!carte.peutAttaquer(h, m)) {
                continue;
            }
            int p = h.puissanceContre(m);
            double chanceAchever = Math.max(0, p - m.getPoints() + 1) / (double) (p + 1);
            double degats = Math.min(p / 2.0, m.getPoints());
            boolean riposte = carte.peutAttaquer(m, h);
            double riposteMoyenne = riposte ? m.puissanceContre(h) / 2.0 * (1 - chanceAchever) : 0;
            double note = 6 * chanceAchever + 3 * degats / m.getPointsMax()
                    + (m.getPuissance() + m.getTir()) / 30.0
                    - 3 * riposteMoyenne / pv
                    - 2 * Math.max(0, danger(h.getPosition(), visibles, chanceAchever > 0.5 ? m : null) - dangerIci) / pv;
            String texte = nom(h) + (h.getPosition().estVoisine(m.getPosition()) ? " frappe " : " tire sur ")
                    + "le " + m.getNomType().toLowerCase() + " " + m.getNumero()
                    + " (" + Math.round(100 * chanceAchever) + " % de chances de l'achever"
                    + (riposte ? "" : ", sans riposte possible") + ")";
            if (note > meilleur.getNote()) {
                meilleur = new Conseil(h, TypeAction.ATTAQUE, m.getPosition(), note, texte);
            }
        }

        // Déplacements
        int[][] exploration = visibles.isEmpty() ? distancesVersBrouillard(h) : null;
        for (Position c : carte.voisinesLibres(h.getPosition())) {
            double dangerLa = danger(c, visibles, null);
            double note = -3 * dangerLa / pv + 0.1 * alliesAutour(c, h);
            String raison;
            if (visibles.isEmpty()) {
                // Aucun monstre en vue : on explore vers la zone inconnue la plus proche
                int ici = exploration[h.getPosition().getY()][h.getPosition().getX()];
                int la = exploration[c.getY()][c.getX()];
                note += (ici == Integer.MAX_VALUE || la == Integer.MAX_VALUE) ? 0 : 0.3 * (ici - la);
                raison = "part en éclaireur";
            } else if (sante < 0.5) {
                note += 3 * (dangerIci - dangerLa) / pv;
                raison = "se met à l'abri";
            } else {
                // Un héros en forme cherche une case d'où il pourra attaquer au tour suivant
                double gain = 0;
                Monstre vise = null;
                for (Monstre m : visibles) {
                    if (carte.pourraitAttaquer(h, c, m) && !carte.peutAttaquer(h, m)) {
                        double g = 1.5 + (m.getPointsMax() - m.getPoints()) / (double) m.getPointsMax();
                        if (g > gain) {
                            gain = g;
                            vise = m;
                        }
                    }
                }
                note += gain * sante;
                raison = vise != null ? "se place pour attaquer le " + vise.getNomType().toLowerCase() + " " + vise.getNumero() + " au prochain tour"
                                      : "se repositionne";
            }
            if (note > meilleur.getNote()) {
                meilleur = new Conseil(h, TypeAction.DEPLACEMENT, c, note, nom(h) + " " + raison + " en " + c);
            }
        }
        return meilleur;
    }

    /**
     * @return le meilleur conseil parmi tous les héros qui n'ont pas encore joué, ou null
     */
    public Conseil meilleurConseil() {
        Conseil meilleur = null;
        for (Heros h : carte.getHeros()) {
            if (h.peutJouer(carte.getTourActuel())) {
                Conseil c = meilleurePour(h);
                if (meilleur == null || c.getNote() > meilleur.getNote()) {
                    meilleur = c;
                }
            }
        }
        return meilleur;
    }

    /**
     * Applique un conseil (le repos est appliqué en fin de tour, comme pour le joueur).
     * @param conseil le conseil à suivre
     * @return vrai si une action a eu lieu
     */
    public boolean appliquer(Conseil conseil) {
        Heros h = conseil.getHeros();
        if (conseil.getType() == TypeAction.REPOS) {
            h.repos();
            h.joueTour(carte.getTourActuel());
            return true;
        }
        try {
            return carte.actionHeros(h.getPosition(), conseil.getCible());
        } catch (SoldatException | CarteException | PositionException e) {
            return false;
        }
    }

    /**
     * Joue tout le tour des héros : à chaque étape, le meilleur conseil est appliqué,
     * puis la situation est réévaluée (une attaque peut changer les autres décisions).
     * @return la liste des actions jouées, dans l'ordre
     */
    public List<Conseil> jouerTourHeros() {
        List<Conseil> joues = new ArrayList<>();
        Conseil c;
        while (!carte.partieTerminee() && (c = meilleurConseil()) != null) {
            if (!appliquer(c)) {
                c.getHeros().joueTour(carte.getTourActuel());
            }
            joues.add(c);
        }
        return joues;
    }

    /**
     * Distance de marche vers les cases que les héros ne voient pas (le brouillard),
     * en fouillant d'abord le camp de départ des monstres (la moitié droite).
     */
    private int[][] distancesVersBrouillard(Heros h) {
        boolean[][] vu = carte.zoneVisibleHeros();
        List<Position> campMonstres = new ArrayList<>();
        List<Position> ailleurs = new ArrayList<>();
        for (int y = 0; y < IConfig.HAUTEUR_CARTE; y++) {
            for (int x = 0; x < IConfig.LARGEUR_CARTE; x++) {
                Position p = new Position(x, y, false);
                if (!vu[y][x] && carte.estLibre(p)) {
                    (x >= IConfig.LARGEUR_CARTE / 2 ? campMonstres : ailleurs).add(p);
                }
            }
        }
        return carte.distancesDeMarche(campMonstres.isEmpty() ? ailleurs : campMonstres, h.getPosition(), Heros.class);
    }

    /** Nombre d'autres héros autour d'une case (rester groupés protège). */
    private int alliesAutour(Position pos, Heros h) {
        int n = 0;
        for (Heros autre : carte.getHeros()) {
            if (autre != h && autre.getPosition().distance(pos) == 1) {
                n++;
            }
        }
        return n;
    }

    private static String nom(Heros h) {
        return h.getNomType() + " " + h.getNom();
    }
}
