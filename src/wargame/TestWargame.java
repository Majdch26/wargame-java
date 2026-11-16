package wargame;

import java.io.File;
import java.util.List;

/**
 * Tests automatiques des règles du jeu, de l'IA et de la sauvegarde (sans interface graphique).
 *
 * <p>Usage : {@code java -cp wargame.jar wargame.TestWargame}</p>
 */
public final class TestWargame {
    private static int reussis = 0;
    private static int echecs = 0;

    private TestWargame() { }

    private static void verifier(String nom, boolean condition) {
        if (condition) {
            reussis++;
        } else {
            echecs++;
            System.out.println("ÉCHEC : " + nom);
        }
    }

    private static void verifierException(String nom, Class<? extends Exception> attendue, Action action) {
        try {
            action.executer();
            verifier(nom + " (aucune exception)", false);
        } catch (Exception e) {
            verifier(nom + " (" + e.getClass().getSimpleName() + ")", attendue.isInstance(e));
        }
    }

    @FunctionalInterface
    private interface Action {
        void executer() throws Exception;
    }

    private static Position p(int x, int y) {
        return new Position(x, y, false);
    }

    private static Heros heros(Carte c, ISoldat.TypesH type, String nom, int x, int y) {
        Heros h = new Heros(c, type, nom, p(x, y));
        c.ajouter(h);
        return h;
    }

    private static Monstre monstre(Carte c, ISoldat.TypesM type, int numero, int x, int y) {
        Monstre m = new Monstre(c, type, numero, p(x, y));
        c.ajouter(m);
        return m;
    }

    private static void obstacle(Carte c, Obstacle.TypeObstacle type, int x, int y) {
        c.ajouter(new Obstacle(type, p(x, y)));
    }

    /**
     * Lance tous les tests.
     * @param args non utilisés
     * @throws Exception erreur inattendue
     */
    public static void main(String[] args) throws Exception {
        Hasard.setGraine(42);

        // ---------- Position ----------
        verifier("distance avec diagonales", p(2, 2).distance(p(5, 4)) == 3);
        verifier("cases voisines en diagonale", p(3, 3).estVoisine(p(4, 4)));
        verifier("cases non voisines", !p(3, 3).estVoisine(p(5, 3)));
        verifierException("position hors de la carte refusée", PositionException.class, () -> new Position(-1, 0));
        verifier("égalité de positions", p(1, 2).equals(p(1, 2)) && p(1, 2).hashCode() == p(1, 2).hashCode());

        // ---------- Création d'une partie ----------
        Carte carte = new Carte(Difficulte.NORMAL);
        verifier("6 héros, 15 monstres et 20 obstacles", carte.getHeros().size() == IConfig.NB_HEROS
                && carte.getMonstres().size() == IConfig.NB_MONSTRES && carte.getObstacles().size() == IConfig.NB_OBSTACLES);
        verifier("les héros sont dans la moitié gauche", carte.getHeros().stream().allMatch(h -> h.getPosition().getX() < IConfig.LARGEUR_CARTE / 2));
        verifier("les monstres sont dans la moitié droite", carte.getMonstres().stream().allMatch(m -> m.getPosition().getX() > IConfig.LARGEUR_CARTE / 2));
        verifier("les héros sont nommés de A à F", carte.getHeros().get(0).getNom().equals("A") && carte.getHeros().get(5).getNom().equals("F"));
        boolean grilleCoherente = true;
        for (Heros h : carte.getHeros()) {
            grilleCoherente &= carte.getElement(h.getPosition()) == h;
        }
        verifier("chaque soldat est bien sur sa case", grilleCoherente);

        // ---------- Déplacements ----------
        Carte c = new Carte(Difficulte.FACILE, true);
        Heros nain = heros(c, ISoldat.TypesH.NAIN, "A", 5, 5);
        Heros elfe = heros(c, ISoldat.TypesH.ELF, "B", 2, 2);
        verifier("déplacement sur une case voisine", c.actionHeros(p(5, 5), p(6, 6)) && c.getElement(p(6, 6)) == nain && c.getElement(p(5, 5)) == null);
        verifierException("un héros ne joue qu'une fois par tour", SoldatException.class, () -> c.actionHeros(p(6, 6), p(7, 6)));
        verifierException("déplacement de deux cases refusé", SoldatException.class, () -> c.actionHeros(p(2, 2), p(4, 2)));
        obstacle(c, Obstacle.TypeObstacle.ROCHER, 3, 3);
        verifierException("déplacement sur un obstacle refusé", CarteException.class, () -> c.actionHeros(p(2, 2), p(3, 3)));
        verifierException("pas de héros sur la case de départ", SoldatException.class, () -> c.actionHeros(p(10, 10), p(11, 10)));
        verifierException("case hors de la carte", PositionException.class, () -> c.actionHeros(p(2, 2), p(-1, 2)));
        verifier("cases accessibles : 7 autour de l'elfe (un rocher)", c.getCasesDeplacement(elfe).size() == 7);

        // ---------- Combats et portée ----------
        Carte k = new Carte(Difficulte.FACILE, true);
        Heros archer = heros(k, ISoldat.TypesH.ELF, "A", 2, 7);
        Heros nain2 = heros(k, ISoldat.TypesH.NAIN, "B", 2, 2);
        Monstre troll = monstre(k, ISoldat.TypesM.TROLL, 1, 6, 7);
        Monstre orc = monstre(k, ISoldat.TypesM.ORC, 2, 3, 3);
        verifier("corps-à-corps en diagonale", k.peutAttaquer(nain2, orc));
        verifier("un elfe tire à 4 cases", k.peutAttaquer(archer, troll));
        verifier("un nain ne tire pas à distance", !k.peutAttaquer(nain2, troll));
        verifier("un troll (portée 1) ne riposte pas à un tir", !k.peutAttaquer(troll, archer));
        obstacle(k, Obstacle.TypeObstacle.FORET, 4, 7);
        verifier("une forêt bloque le tir", !k.peutAttaquer(archer, troll) && !k.aLigneDeVue(p(2, 7), p(6, 7)));
        Carte eau = new Carte(Difficulte.FACILE, true);
        obstacle(eau, Obstacle.TypeObstacle.EAU, 4, 7);
        verifier("l'eau ne bloque pas le tir", eau.aLigneDeVue(p(2, 7), p(6, 7)));
        verifierException("tir bloqué : action refusée", SoldatException.class, () -> k.actionHeros(p(2, 7), p(6, 7)));

        int pvTroll = troll.getPoints();
        int pvArcher = archer.getPoints();
        Carte k2 = new Carte(Difficulte.FACILE, true);
        Heros archer2 = heros(k2, ISoldat.TypesH.ELF, "A", 2, 7);
        Monstre troll2 = monstre(k2, ISoldat.TypesM.TROLL, 1, 6, 7);
        for (int i = 0; i < 20; i++) {
            archer2.combat(troll2);
        }
        verifier("le tir blesse sans riposte du troll", troll2.getPoints() < pvTroll && archer2.getPoints() == pvArcher);
        verifier("les dégâts ne dépassent jamais la puissance de tir", pvTroll - troll2.getPoints() <= 20 * archer2.getTir());

        Monstre gobelin = monstre(k, ISoldat.TypesM.GOBELIN, 3, 10, 10);
        Heros humain = heros(k, ISoldat.TypesH.HUMAIN, "C", 11, 10);
        gobelin.setPoints(1);
        int essais = 0;
        while (gobelin.estVivant() && essais++ < 100) {
            humain.combat(gobelin);
        }
        verifier("un soldat mort quitte la carte", !gobelin.estVivant() && k.getElement(p(10, 10)) == null
                && !k.getMonstres().contains(gobelin));
        verifier("le combat est noté dans le journal", k.getJournal().get(0).contains("meurt"));

        nain2.setPoints(10);
        nain2.repos();
        verifier("le repos rend " + Soldat.POINTS_REPOS + " points", nain2.getPoints() == 10 + Soldat.POINTS_REPOS);
        nain2.setPoints(nain2.getPointsMax());
        nain2.repos();
        verifier("le repos ne dépasse pas le maximum", nain2.getPoints() == nain2.getPointsMax());

        // ---------- Fin du tour ----------
        Carte t = new Carte(Difficulte.FACILE, true);
        Heros blesse = heros(t, ISoldat.TypesH.NAIN, "A", 1, 1);
        monstre(t, ISoldat.TypesM.GOBELIN, 1, 20, 10);
        blesse.setPoints(50);
        t.jouerSoldats(null);
        verifier("un héros sans ordre se repose en fin de tour", blesse.getPoints() == 50 + Soldat.POINTS_REPOS);
        verifier("le tour avance", t.getTourActuel() == 1 && blesse.peutJouer(1));

        // ---------- Brouillard ----------
        Carte b = new Carte(Difficulte.FACILE, true);
        heros(b, ISoldat.TypesH.HOBBIT, "A", 5, 5);
        boolean[][] vu = b.zoneVisibleHeros();
        verifier("portée 3 = carré de 7x7 visible", vu[2][2] && vu[8][8] && !vu[1][5] && !vu[5][9]);

        // ---------- IA des monstres ----------
        Carte ia = new Carte(Difficulte.DIFFICILE, true);
        Heros proie = heros(ia, ISoldat.TypesH.NAIN, "A", 2, 7);
        Monstre chasseur = monstre(ia, ISoldat.TypesM.TROLL, 1, 10, 7);
        monstre(ia, ISoldat.TypesM.GOBELIN, 2, 4, 9);           // un gobelin voit le nain : toute l'armée le sait
        for (int y = 4; y <= 10; y++) {
            obstacle(ia, Obstacle.TypeObstacle.ROCHER, 6, y);   // un mur entre le troll et le nain
        }
        int[][] dist = ia.distancesDeMarche(List.of(p(3, 7)), p(10, 7));
        verifier("parcours en largeur : le mur se contourne", dist[7][10] != Integer.MAX_VALUE && dist[7][10] > 7);
        for (int i = 0; i < 14 && proie.estVivant(); i++) {
            ia.jouerSoldats(null);
        }
        verifier("mode difficile : le troll contourne le mur et rejoint le nain",
                !proie.estVivant() || chasseur.getPosition().distance(proie.getPosition()) <= 1);

        Carte focus = new Carte(Difficulte.DIFFICILE, true);
        Heros fort = heros(focus, ISoldat.TypesH.NAIN, "A", 5, 5);
        Heros faible = heros(focus, ISoldat.TypesH.HOBBIT, "B", 7, 5);
        faible.setPoints(3);
        fort.joueTour(0);
        faible.joueTour(0);                                     // ils ont déjà joué : pas de repos
        monstre(focus, ISoldat.TypesM.ORC, 1, 6, 5);
        focus.jouerSoldats(null);
        verifier("mode difficile : l'orc achève le héros le plus faible", !faible.estVivant() || faible.getPoints() < 3);
        verifier("le nain intact n'a pas été visé", fort.getPoints() == fort.getPointsMax());

        Carte repos = new Carte(Difficulte.DIFFICILE, true);
        heros(repos, ISoldat.TypesH.HOBBIT, "A", 0, 0);
        Monstre blesseM = monstre(repos, ISoldat.TypesM.TROLL, 1, 24, 14);
        blesseM.setPoints(10);
        repos.jouerSoldats(null);
        verifier("mode difficile : un monstre blessé hors de danger se repose", blesseM.getPoints() == 12);

        // ---------- Conseiller ----------
        Carte cc = new Carte(Difficulte.NORMAL, true);
        heros(cc, ISoldat.TypesH.HUMAIN, "A", 5, 5);
        Monstre presqueMort = monstre(cc, ISoldat.TypesM.ORC, 1, 6, 5);
        presqueMort.setPoints(2);
        Conseiller.Conseil conseil = new Conseiller(cc).meilleurConseil();
        verifier("le conseiller propose d'achever le monstre blessé", conseil.getType() == Conseiller.TypeAction.ATTAQUE
                && conseil.getCible().equals(p(6, 5)));
        verifier("le conseil est expliqué", conseil.getExplication().contains("orc 1"));

        Carte fuite = new Carte(Difficulte.NORMAL, true);
        Heros hobbit = heros(fuite, ISoldat.TypesH.HOBBIT, "A", 5, 5);
        hobbit.setPoints(5);
        monstre(fuite, ISoldat.TypesM.TROLL, 1, 7, 5);
        Conseiller.Conseil c2 = new Conseiller(fuite).meilleurePour(hobbit);
        verifier("un hobbit très blessé s'éloigne du troll", c2.getType() == Conseiller.TypeAction.DEPLACEMENT
                && c2.getCible().distance(p(7, 5)) > 2);

        Carte auto = new Carte(Difficulte.NORMAL);
        List<Conseiller.Conseil> joues = new Conseiller(auto).jouerTourHeros();
        verifier("tour automatique : chaque héros joue une fois", joues.size() == IConfig.NB_HEROS
                && auto.getHeros().stream().noneMatch(h -> h.peutJouer(auto.getTourActuel())));

        // ---------- Parties complètes ----------
        for (Difficulte d : Difficulte.values()) {
            Hasard.setGraine(7);
            Carte fin = Simulation.partie(d, true);
            verifier("partie complète en mode " + d + " sans erreur",
                    fin.partieTerminee() || fin.getTourActuel() == Simulation.TOURS_MAX);
        }

        // ---------- Sauvegarde ----------
        Carte s = new Carte(Difficulte.DIFFICILE);
        s.jouerSoldats(null);
        String nom = "test_" + System.nanoTime();
        s.sauvegarder(nom);
        Carte r = Carte.restaurer(nom);
        verifier("restauration : même tour, mêmes armées, même difficulté", r.getTourActuel() == 1
                && r.getHeros().size() == s.getHeros().size() && r.getMonstres().size() == s.getMonstres().size()
                && r.getDifficulte() == Difficulte.DIFFICILE);
        verifier("restauration : la partie se poursuit", ((java.util.function.BooleanSupplier) () -> { r.jouerSoldats(null); return r.getTourActuel() == 2; }).getAsBoolean());
        verifier("la sauvegarde apparaît dans la liste", Carte.getListeSauvegardes().contains(nom));
        new File(Carte.DOSSIER_SAUVEGARDES, nom + ".ser").delete();
        verifierException("sauvegarde inexistante", SauvegardeException.class, () -> Carte.restaurer("nexiste_pas_" + nom));
        verifierException("nom de sauvegarde invalide", SauvegardeException.class, () -> s.sauvegarder("../hors_du_dossier"));

        System.out.println(reussis + " / " + (reussis + echecs) + " tests réussis");
        System.exit(echecs == 0 ? 0 : 1);
    }
}
