package wargame;

import java.util.ArrayList;
import java.util.List;

/**
 * Comportement décrit dans le sujet : si un héros est à portée, le monstre l'attaque ;
 * sinon il se déplace sur une case voisine libre choisie au hasard.
 */
public class StrategieAleatoire implements StrategieMonstres {
    private static final long serialVersionUID = 1L;

    @Override
    public void jouer(Carte carte, Monstre monstre) {
        List<Heros> cibles = new ArrayList<>();
        for (Heros h : carte.getHeros()) {
            if (carte.peutAttaquer(monstre, h)) {
                cibles.add(h);
            }
        }
        if (!cibles.isEmpty()) {
            monstre.combat(cibles.get(Hasard.entier(cibles.size())));
        } else {
            StrategieMonstres.deplacementAleatoire(carte, monstre);
        }
    }
}
