package wargame;
import java.io.Serializable;
import wargame.ISoldat.TypesH;

/**
 * Représente un héros contrôlé par le joueur.
 * Les héros peuvent être de différents types avec des caractéristiques variées.
 * 
 * <p>Types disponibles définis dans {@link ISoldat.TypesH} :</p>
 * <ul>
 *   <li>HUMAIN - Soldat équilibré</li>
 *   <li>NAIN - Très résistant, faible portée</li>
 *   <li>ELF - Excellent archer, bon tireur</li>
 *   <li>HOBBIT - Discret, bonne portée visuelle</li>
 * </ul>
 */

public class Heros extends Soldat implements Serializable {
    private final String NOM;
    private final TypesH TYPE;
    
    /**
     * Crée un nouveau héros avec le type spécifié.
     * Les caractéristiques (PV, portée, puissances) dépendent du type.
     * 
     * @param carte La carte de jeu
     * @param type Le type de héros (HUMAIN, ELF, NAIN, HOBBIT)
     * @param nom Le nom du héros (une lettre)
     * @param pos La position initiale
     */
    public Heros(Carte carte, TypesH type, String nom, Position pos) {
        super(carte, type.getPoints(), type.getPortee(), type.getPuissance(), type.getTir(), pos);
        this.TYPE = type;
        this.NOM = nom;
    }
    
    public String getNom() {
        return NOM;
    }
    
    public TypesH getType() {
        return TYPE;
    }
    
    // NOUVELLE MÉTHODE : obtenir la puissance de tir
    public int getTir() {
        return TYPE.getTir();
    }
    
    @Override
    public String toString() {
        return "Héros " + NOM + " (" + TYPE + ") PV: " + pointsDeVie + "/" + POINTS_DE_VIE_MAX;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;  // Vérifie d'abord l'égalité de Soldat
        if (getClass() != obj.getClass()) return false;
        
        Heros other = (Heros) obj;
        return this.NOM.equals(other.NOM) && this.TYPE == other.TYPE;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() + 31 * NOM.hashCode() + TYPE.hashCode();
    }
}