package wargame;
import java.io.Serializable;
/**
 * Classe abstraite représentant un élément placé sur la carte.
 * Tous les éléments du jeu (soldats, obstacles) héritent de cette classe.
 */public abstract class Element implements IConfig, Serializable{
    protected Position pos;
    
    /**
     * Crée un élément à la position spécifiée.
     * 
     * @param pos La position initiale de l'élément
     */
    public Element(Position pos) {
        this.pos = pos;
    }
    
    /** Retourne la position actuelle de l'élément.
     * 
     * @return La position de l'élément
     */
    public Position getPosition() {
        return pos;
    }
    
    /** Modifie la position actuelle de l'élément.
     * 
     * @param pos La nouvelle position de l'élément
     */
    public void setPosition(Position pos) {
        this.pos = pos;
    }
    
    public abstract String toString();
}