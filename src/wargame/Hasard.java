package wargame;

import java.util.Random;

/**
 * Générateur aléatoire partagé par tout le jeu.
 * Fixer la graine permet de rejouer exactement la même partie (tests, simulations).
 */
public final class Hasard {
    private static Random random = new Random();

    private Hasard() { }

    /**
     * Fixe la graine du générateur.
     * @param graine la graine
     */
    public static void setGraine(long graine) {
        random = new Random(graine);
    }

    /**
     * Entier aléatoire dans [0, n[.
     * @param n borne exclue (strictement positive)
     * @return un entier entre 0 et n - 1
     */
    public static int entier(int n) {
        return random.nextInt(n);
    }

    /**
     * Élément aléatoire d'un tableau.
     * @param <T> type des éléments
     * @param valeurs tableau non vide
     * @return une des valeurs
     */
    public static <T> T choisir(T[] valeurs) {
        return valeurs[random.nextInt(valeurs.length)];
    }
}
