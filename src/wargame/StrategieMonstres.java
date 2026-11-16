package wargame;

import java.io.Serializable;
import java.util.List;

/**
 * Stratégie du général-ordinateur (patron de conception Stratégie) :
 * décide de l'action de chaque monstre à la fin du tour du joueur.
 */
public interface StrategieMonstres extends Serializable {

    /**
     * Appelée une fois au début du tour des monstres (pour préparer des calculs communs).
     * @param carte la carte
     */
    default void debutTour(Carte carte) { }

    /**
     * Fait agir un monstre : attaquer, se déplacer ou se reposer.
     * @param carte la carte
     * @param monstre le monstre qui joue
     */
    void jouer(Carte carte, Monstre monstre);

    /**
     * Déplace le monstre sur une case choisie au hasard parmi les cases voisines libres.
     * @param carte la carte
     * @param monstre le monstre
     */
    static void deplacementAleatoire(Carte carte, Monstre monstre) {
        List<Position> libres = carte.voisinesLibres(monstre.getPosition());
        if (!libres.isEmpty()) {
            deplacer(carte, monstre, libres.get(Hasard.entier(libres.size())));
        }
    }

    /**
     * Déplace le monstre (la case a été choisie libre, l'exception ne devrait pas arriver).
     * @param carte la carte
     * @param monstre le monstre
     * @param cible la case d'arrivée
     */
    static void deplacer(Carte carte, Monstre monstre, Position cible) {
        try {
            carte.deplaceSoldat(cible, monstre);
        } catch (CarteException e) {
            throw new IllegalStateException(e);
        }
    }
}
