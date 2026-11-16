package wargame;

/**
 * Exception personnalisée pour les erreurs liées aux positions sur la carte de jeu.
 */
public class PositionException extends Exception {
    public PositionException(String message) {
        super(message);
    }
    
    public PositionException(String message, Throwable cause) {
        super(message, cause);
    }
}