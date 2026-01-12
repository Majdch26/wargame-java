package wargame;
import java.io.Serializable;

/**
 * Représente une position sur la carte de jeu.
 */

public class Position implements IConfig, Serializable {
    private int x, y;
    public Position(int x, int y) throws PositionException {
        if (x < 0 || x >= LARGEUR_CARTE || y < 0 || y >= HAUTEUR_CARTE) {
            throw new PositionException("Position invalide : (" + x + "," + y + ")");
        }
        this.x = x;
        this.y = y;
    }
    public Position(int x, int y, boolean verifier) {
        this.x = x;
        this.y = y;
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public boolean estValide() {
        if (x<0 || x>=LARGEUR_CARTE || y<0 || y>=HAUTEUR_CARTE) return false; else return true;
    }
    public String toString() { return "("+x+","+y+")"; }
    public boolean estVoisine(Position pos) {
        return ((Math.abs(x-pos.x)<=1) && (Math.abs(y-pos.y)<=1));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;                     // Même référence
        if (obj == null) return false;                    // obj est null
        if (getClass() != obj.getClass()) return false;   // Classes différentes
        
        Position other = (Position) obj;                  // Transtypage sécurisé
        return this.x == other.x && this.y == other.y;    // Comparaison des coordonnées
    }
    
    @Override
    public int hashCode() {
        // Toujours redéfinir hashCode() quand on redéfinit equals()
        return 31 * x + y;  // Formule simple qui respecte les bonnes pratiques
    }
}