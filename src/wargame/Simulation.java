package wargame;

/**
 * Fait s'affronter le conseiller (qui joue les héros) et chaque niveau de l'ordinateur,
 * sans interface graphique, pour mesurer la force des stratégies.
 *
 * <p>Usage : {@code java -cp wargame.jar wargame.Simulation [parties] [graine]}</p>
 */
public final class Simulation {
    /** Au-delà, la partie est déclarée nulle. */
    public static final int TOURS_MAX = 300;

    private Simulation() { }

    /** Résultat d'une série de parties. */
    public static final class Bilan {
        int victoiresHeros, victoiresMonstres, nulles, toursTotal;

        /** @return le pourcentage de victoires des héros */
        public double pourcentageHeros() {
            int total = victoiresHeros + victoiresMonstres + nulles;
            return total == 0 ? 0 : 100.0 * victoiresHeros / total;
        }
    }

    /**
     * Joue une partie complète, héros dirigés par le conseiller.
     * @param difficulte niveau des monstres
     * @param herosAutomatiques vrai pour faire jouer les héros par le conseiller, faux pour qu'ils restent passifs
     * @return la carte en fin de partie
     */
    public static Carte partie(Difficulte difficulte, boolean herosAutomatiques) {
        Carte carte = new Carte(difficulte);
        Conseiller conseiller = new Conseiller(carte);
        while (!carte.partieTerminee() && carte.getTourActuel() < TOURS_MAX) {
            if (herosAutomatiques) {
                conseiller.jouerTourHeros();
            }
            if (!carte.partieTerminee()) {
                carte.jouerSoldats(null);
            }
        }
        return carte;
    }

    /**
     * Joue plusieurs parties pour un niveau.
     * @param difficulte niveau des monstres
     * @param parties nombre de parties
     * @param graine graine du hasard (les mêmes cartes pour chaque niveau)
     * @return le bilan
     */
    public static Bilan serie(Difficulte difficulte, int parties, long graine) {
        return serie(difficulte, parties, graine, true);
    }

    /**
     * Joue plusieurs parties pour un niveau.
     * @param difficulte niveau des monstres
     * @param parties nombre de parties
     * @param graine graine du hasard (les mêmes cartes pour chaque niveau)
     * @param conseiller vrai : héros joués par le conseiller ; faux : héros qui se contentent de se reposer
     * @return le bilan
     */
    public static Bilan serie(Difficulte difficulte, int parties, long graine, boolean conseiller) {
        Bilan bilan = new Bilan();
        for (int i = 0; i < parties; i++) {
            Hasard.setGraine(graine + i);
            Carte carte = partie(difficulte, conseiller);
            bilan.toursTotal += carte.getTourActuel();
            switch (carte.getGagnant()) {
                case "Héros": bilan.victoiresHeros++; break;
                case "Monstres": bilan.victoiresMonstres++; break;
                default: bilan.nulles++;
            }
        }
        return bilan;
    }

    /**
     * Point d'entrée.
     * @param args nombre de parties par niveau, puis graine
     */
    public static void main(String[] args) {
        int parties = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        long graine = args.length > 1 ? Long.parseLong(args[1]) : 2026;
        System.out.printf("%d parties par niveau (graine %d), %d tours au plus%n%n", parties, graine, TOURS_MAX);
        System.out.println("Niveau des monstres   Héros joués par    Héros gagnent   Monstres gagnent   Nulles   Tours en moyenne");
        for (Difficulte d : Difficulte.values()) {
            for (boolean conseiller : new boolean[] {false, true}) {
                Bilan b = serie(d, parties, graine, conseiller);
                System.out.printf("%-21s %-18s %9.1f %%      %9.1f %%    %6d   %10.1f%n", d,
                        conseiller ? "le conseiller" : "personne (repos)",
                        b.pourcentageHeros(), 100.0 * b.victoiresMonstres / parties, b.nulles, b.toursTotal / (double) parties);
            }
        }
    }
}
