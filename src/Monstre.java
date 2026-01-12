package wargame;
import java.io.Serializable;
import wargame.ISoldat.TypesM;

/**
 * Représente un monstre contrôlé par l'IA.
 * Les monstres agissent automatiquement en fin de tour de jeu.
 * 
 * <p>Types disponibles définis dans {@link ISoldat.TypesM} :</p>
 * <ul>
 *   <li>TROLL - Très résistant, puissant</li>
 *   <li>ORC - Agressif, équilibré</li>
 *   <li>GOBELIN - Rapide, faible</li>
 * </ul>
 */
public class Monstre extends Soldat implements Serializable {
    private final TypesM TYPE;
    private final int NUMERO;
    
    /**
     * Crée un nouveau monstre avec le type spécifié.
     * 
     * @param carte La carte de jeu
     * @param type Le type de monstre (TROLL, ORC, GOBELIN)
     * @param numero Le numéro d'identification du monstre
     * @param pos La position initiale
     */
    public Monstre(Carte carte, TypesM type, int numero, Position pos) {
        super(carte, type.getPoints(), type.getPortee(), type.getPuissance(), type.getTir(), pos);
        this.TYPE = type;
        this.NUMERO = numero;
    }
    
    public TypesM getType() {
        return TYPE;
    }
    
    public int getNumero() {
        return NUMERO;
    }
    
    // NOUVELLE MÉTHODE : obtenir la puissance de tir
    public int getTir() {
        return TYPE.getTir();
    }
    
    @Override
    public String toString() {
        return "Monstre " + NUMERO + " (" + TYPE + ") PV: " + pointsDeVie + "/" + POINTS_DE_VIE_MAX;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        
        Monstre other = (Monstre) obj;
        return this.NUMERO == other.NUMERO && this.TYPE == other.TYPE;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() + 31 * NUMERO + TYPE.hashCode();
    }
}