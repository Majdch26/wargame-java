package wargame;
import java.io.Serializable;

/**
 * Classe abstraite représentant un soldat dans le jeu.
 * Un soldat possède des points de vie, une portée visuelle et des capacités de combat.
 * 
 * <p>Les soldats peuvent être de deux types :</p>
 * <ul>
 *   <li>{@link Heros} - Contrôlés par le joueur</li>
 *   <li>{@link Monstre} - Contrôlés par l'IA</li>
 * </ul>
 * 
 * <p>Chaque soldat a des caractéristiques qui dépendent de son type :
 * points de vie, puissance de frappe (corps-à-corps), puissance de tir (à distance),
 * et portée visuelle.</p>
 * 
 */
public abstract class Soldat extends Element implements ISoldat, Serializable {
    protected final int POINTS_DE_VIE_MAX;
    protected final int PUISSANCE;
    protected final int TIR;
    protected final int PORTEE_VISUELLE;
    protected int pointsDeVie;
    protected int tourDerniereAction;
    protected Carte carte;
    
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
    
    /**
     * Retourne le numéro du dernier tour où le soldat a agi.
     * 
     * @return Le numéro du dernier tour d'action
     */
    @Override
    public int getTour() {
        return tourDerniereAction;
    }
    
    /**
     * Retourne la portée visuelle du soldat.
     * 
     * @return Portée visuelle en nombre de cases
     */
    @Override
    public int getPortee() {
        return PORTEE_VISUELLE;
    }
    
    // NOUVELLE MÉTHODE : obtenir la puissance de tir
    public int getTir() {
        return TIR;
    }
    
    /**
     * Enregistre le tour actuel comme le dernier tour d'action du soldat.
     * 
     * @param tour Le numéro du tour actuel
     * 
     */
    @Override
    public void joueTour(int tour) {
        this.tourDerniereAction = tour;
    }
    
    @Override
    public void combat(Soldat adversaire) {
        if (this.pointsDeVie <= 0 || adversaire.pointsDeVie <= 0) {
            return;
        }
        
        boolean estCorpsACorps = this.pos.estVoisine(adversaire.pos);
        int puissanceAttaque;
        
        if (estCorpsACorps) {
            puissanceAttaque = (int)(Math.random() * (this.PUISSANCE + 1));
        } else {
            puissanceAttaque = (int)(Math.random() * (this.TIR + 1));
        }
        
        adversaire.pointsDeVie -= puissanceAttaque;
        
        if (adversaire.pointsDeVie > 0) {
            int puissanceReponse;
            if (estCorpsACorps) {
                puissanceReponse = (int)(Math.random() * (adversaire.PUISSANCE + 1));
            } else {
                puissanceReponse = (int)(Math.random() * (adversaire.TIR + 1));
            }
            this.pointsDeVie -= puissanceReponse;
        }
        
        if (this.pointsDeVie <= 0) {
            carte.mort(this);
        }
        if (adversaire.pointsDeVie <= 0) {
            carte.mort(adversaire);
        }
    }
    
    @Override
    public void seDeplace(Position newPos) {
        this.pos = newPos;
    }
    
    public boolean estVivant() {
        return pointsDeVie > 0;
    }
    
    public void repos() {
        if (pointsDeVie < POINTS_DE_VIE_MAX) {
            pointsDeVie += 2;
            if (pointsDeVie > POINTS_DE_VIE_MAX) {
                pointsDeVie = POINTS_DE_VIE_MAX;
            }
        }
    }
    
    public boolean peutJouer(int tourActuel) {
        return tourDerniereAction < tourActuel;
    }

    public int getPointsMax() {
        return POINTS_DE_VIE_MAX;
    }
}