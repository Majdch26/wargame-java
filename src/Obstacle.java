package wargame;
import java.awt.Color;
import java.awt.Graphics;
import java.io.Serializable;

/**
 * Représente un obstacle sur la carte de jeu.
 * Les obstacles sont des éléments fixes qui bloquent le passage des soldats.
 * 
 * <p>Types disponibles :</p>
 * <ul>
 *   <li>ROCHER - Bloque le passage, couleur grise</li>
 *   <li>FORET - Bloque le passage, couleur verte</li>
 *   <li>EAU - Bloque le passage, couleur bleue</li>
 * </ul>
 */
public class Obstacle extends Element implements Serializable {
    public enum TypeObstacle {
        ROCHER (IConfig.COULEUR_ROCHER), 
        FORET (IConfig.COULEUR_FORET), 
        EAU (IConfig.COULEUR_EAU);
        private final Color COULEUR;
        
        TypeObstacle(Color couleur) { COULEUR = couleur; }
        public static TypeObstacle getObstacleAlea() {
            return values()[(int)(Math.random()*values().length)];
        }
    }
    private TypeObstacle TYPE;
    
    public Obstacle(TypeObstacle type, Position pos) { 
        super(pos);
        TYPE = type; 
    }
    
    public String toString() { return ""+TYPE; }
    
    public TypeObstacle getType() {
        return TYPE;
    }
}