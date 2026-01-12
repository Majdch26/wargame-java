package wargame;
import java.awt.Graphics;

import wargame.Heros;
import wargame.PanneauJeu;
import wargame.Soldat;

/**
 * Interface représentant les opérations possibles sur la carte de jeu.
 */
public interface ICarte {
	Element getElement(Position pos);
	Position trouvePositionVide(); // Trouve aleatoirement une position vide sur la carte
	Position trouvePositionVide(Position pos); // Trouve une position vide choisie
								// aleatoirement parmi les 8 positions adjacentes de pos
	Heros trouveHeros(); // Trouve aleatoirement un heros sur la carte
	Heros trouveHeros(Position pos); // Trouve un heros choisi aleatoirement
									 // parmi les 8 positions adjacentes de pos
	boolean deplaceSoldat(Position pos, Soldat soldat) throws CarteException;
	void mort(Soldat perso);
	boolean actionHeros(Position pos, Position pos2) throws SoldatException, CarteException, PositionException;
	void jouerSoldats(PanneauJeu pj);
	void toutDessiner(Graphics g, PanneauJeu panneau); // j'ai ajoute PanneauJeu panneau
}