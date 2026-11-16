package wargame;
/**
 * Exception personnalisée pour les erreurs liées à la carte de jeu.
 */
public class CarteException extends Exception {
    public CarteException(String message) {
        super(message);
    }
    
    public CarteException(String message, Throwable cause) {
        super(message, cause);
    }
}