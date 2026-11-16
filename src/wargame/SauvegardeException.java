package wargame;

/**
 * Exception personnalisée pour les erreurs liées à la sauvegarde du jeu.
 */
public class SauvegardeException extends Exception {
    public SauvegardeException(String message) {
        super(message);
    }
    
    public SauvegardeException(String message, Throwable cause) {
        super(message, cause);
    }
}