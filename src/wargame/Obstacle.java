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
 *   <li>ROCHER - Bloque le passage et les tirs, couleur grise</li>
 *   <li>FORET - Bloque le passage et les tirs, couleur verte</li>
 *   <li>EAU - Bloque le passage mais pas les tirs, couleur bleue</li>
 * </ul>
 */
public class Obstacle extends Element implements Serializable {
    private static final long serialVersionUID = 2L;
    public enum TypeObstacle {
        ROCHER (IConfig.COULEUR_ROCHER, "Rochers"),
        FORET (IConfig.COULEUR_FORET, "Forêt"),
        EAU (IConfig.COULEUR_EAU, "Eau");
        private final Color COULEUR;
        private final String NOM;

        TypeObstacle(Color couleur, String nom) { COULEUR = couleur; NOM = nom; }
        /** @return la couleur utilisée sans image */
        public Color getCouleur() { return COULEUR; }
        /** @return le nom affiché */
        public String getNom() { return NOM; }
        public static TypeObstacle getObstacleAlea() {
            return Hasard.choisir(values());
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