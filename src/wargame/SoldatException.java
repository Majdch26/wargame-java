package wargame;

/**
 * Exception personnalisée pour les erreurs liées aux soldats dans le jeu.
 */

public class SoldatException extends Exception {
    public SoldatException(String message) {
        super(message);
    }
    
    public SoldatException(String message, Throwable cause) {
        super(message, cause);
    }
}