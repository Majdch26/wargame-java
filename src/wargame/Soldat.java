package wargame;
import java.io.Serializable;

/**
 * Classe abstraite représentant un soldat dans le jeu.
 * Un soldat possède des points de vie, une portée visuelle et des capacités de combat.
 *
 * <p>Les soldats peuvent être de deux types :</p>
 * <ul>
 *   <li>{@link Heros} - Contrôlés par le joueur</li>
 *   <li>{@link Monstre} - Contrôlés par l'ordinateur</li>
 * </ul>
 *
 * <p>Chaque soldat a des caractéristiques qui dépendent de son type :
 * points de vie, puissance de frappe (corps-à-corps), puissance de tir (à distance),
 * et portée visuelle.</p>
 */
public abstract class Soldat extends Element implements ISoldat, Serializable {
    private static final long serialVersionUID = 2L;

    /** Points de vie regagnés par un soldat qui se repose. */
    public static final int POINTS_REPOS = 2;

    protected final int POINTS_DE_VIE_MAX;
    protected final int PUISSANCE;
    protected final int TIR;
    protected final int PORTEE_VISUELLE;
    protected int pointsDeVie;
    protected int tourDerniereAction;
    protected Carte carte;

    /**
     * Crée un soldat.
     * @param carte la carte sur laquelle il se trouve
     * @param pts points de vie maximum
     * @param portee portée visuelle (en cases)
     * @param puiss puissance de frappe au corps-à-corps
     * @param tir puissance de tir (0 si le soldat ne sait pas tirer)
     * @param pos position initiale
     */
    public Soldat(Carte carte, int pts, int portee, int puiss, int tir, Position pos) {
        super(pos);
        this.POINTS_DE_VIE_MAX = pts;
        this.pointsDeVie = pts;
        this.PORTEE_VISUELLE = portee;
        this.PUISSANCE = puiss;
        this.TIR = tir;
        this.carte = carte;
        this.tourDerniereAction = -1;
    }

    @Override
    public int getPoints() {
        return pointsDeVie;
    }

    /** @return le numéro du dernier tour où le soldat a agi */
    @Override
    public int getTour() {
        return tourDerniereAction;
    }

    /** @return la portée visuelle en nombre de cases */
    @Override
    public int getPortee() {
        return PORTEE_VISUELLE;
    }

    /** @return la puissance de tir (0 si le soldat ne tire pas) */
    public int getTir() {
        return TIR;
    }

    /** @return la puissance de frappe au corps-à-corps */
    public int getPuissance() {
        return PUISSANCE;
    }

    /** @return les points de vie maximum */
    public int getPointsMax() {
        return POINTS_DE_VIE_MAX;
    }

    /**
     * Puissance utilisée contre une cible : la frappe au contact, le tir à distance.
     * @param cible le soldat visé
     * @return la puissance maximale d'une attaque contre cette cible
     */
    public int puissanceContre(Soldat cible) {
        return pos.estVoisine(cible.pos) ? PUISSANCE : TIR;
    }

    /** @return une courte description du type du soldat, par exemple « Elfe » */
    public abstract String getNomType();

    /** @return la lettre d'un héros ou le numéro d'un monstre */
    public abstract String identifiant();

    /**
     * Enregistre le tour actuel comme le dernier tour d'action du soldat.
     * @param tour Le numéro du tour actuel
     */
    @Override
    public void joueTour(int tour) {
        this.tourDerniereAction = tour;
    }

    /**
     * Combat contre un adversaire. Le soldat qui attaque frappe en premier
     * (un tirage dans [0, puissance]) ; si l'adversaire survit, il réplique
     * à condition de pouvoir lui-même atteindre l'attaquant (un nain ne répond
     * pas à une flèche tirée de loin).
     * @param adversaire le soldat attaqué
     */
    @Override
    public void combat(Soldat adversaire) {
        if (!estVivant() || !adversaire.estVivant()) {
            return;
        }

        boolean contact = pos.estVoisine(adversaire.pos);
        int coup = Hasard.entier(puissanceContre(adversaire) + 1);
        adversaire.pointsDeVie -= coup;
        StringBuilder recit = new StringBuilder(getNomType() + " " + identifiant() + (contact ? " frappe " : " tire sur ") + adversaire.getNomType()
                + " " + adversaire.identifiant() + " : " + (coup == 0 ? "raté" : "-" + coup + " PV"));

        if (adversaire.estVivant() && carte.peutAttaquer(adversaire, this)) {
            int riposte = Hasard.entier(adversaire.puissanceContre(this) + 1);
            this.pointsDeVie -= riposte;
            recit.append(", riposte ").append(riposte == 0 ? "ratée" : "-" + riposte + " PV");
        }
        if (!adversaire.estVivant()) {
            recit.append(" · ").append(adversaire.getNomType()).append(" ").append(adversaire.identifiant()).append(" meurt");
        }
        if (!this.estVivant()) {
            recit.append(" · ").append(getNomType()).append(" ").append(identifiant()).append(" meurt");
        }
        carte.noter(recit.toString());

        if (!adversaire.estVivant()) {
            carte.mort(adversaire);
        }
        if (!this.estVivant()) {
            carte.mort(this);
        }
    }

    @Override
    public void seDeplace(Position newPos) {
        this.pos = newPos;
    }

    /** @return vrai si le soldat a encore des points de vie */
    public boolean estVivant() {
        return pointsDeVie > 0;
    }

    /** Le soldat se repose et regagne quelques points de vie, sans dépasser son maximum. */
    public void repos() {
        pointsDeVie = Math.min(POINTS_DE_VIE_MAX, pointsDeVie + POINTS_REPOS);
    }

    /**
     * @param tourActuel le tour en cours
     * @return vrai si le soldat n'a pas encore agi pendant ce tour
     */
    public boolean peutJouer(int tourActuel) {
        return tourDerniereAction < tourActuel;
    }

    /** Utilisé par les tests : fixe les points de vie. */
    void setPoints(int points) {
        this.pointsDeVie = points;
    }
}
