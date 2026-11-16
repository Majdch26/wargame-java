package wargame;
import java.io.Serializable;
import wargame.ISoldat.TypesM;

/**
 * Représente un monstre contrôlé par l'ordinateur.
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
    private static final long serialVersionUID = 2L;
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
    
    @Override
    public String identifiant() {
        return String.valueOf(NUMERO);
    }

    @Override
    public String getNomType() {
        switch (TYPE) {
            case TROLL: return "Troll";
            case ORC: return "Orc";
            default: return "Gobelin";
        }
    }

    @Override
    public String toString() {
        return getNomType() + " " + NUMERO + " (" + pointsDeVie + "/" + POINTS_DE_VIE_MAX + " PV)";
    }
}