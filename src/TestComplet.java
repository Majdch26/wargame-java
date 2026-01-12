package wargame;

public class TestComplet {
    public static void main(String[] args) {
        System.out.println("=== TEST COMPLET WARGAME ===\n");
        
        try {
            // 1. Test des classes de base
            testClassesDeBase();
            
            // 2. Test de la carte
            testCarte();
            
            // 3. Test des combats
            testCombats();
            
            // 4. Test d'une partie complète
            testPartieComplete();
            
            System.out.println("\n=== TOUS LES TESTS SONT TERMINÉS ===");
        } catch (Exception e) {
            System.err.println("Erreur lors des tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 1. Test des classes de base
    private static void testClassesDeBase() throws PositionException {
        System.out.println("1. TEST DES CLASSES DE BASE");
        System.out.println("---------------------------");
        
        // Test Position
        System.out.println("\n→ Test Position:");
        Position pos1 = new Position(5, 5);
        Position pos2 = new Position(5, 6);
        Position pos3 = new Position(10, 10);
        
        System.out.println("  Position " + pos1 + " valide: " + pos1.estValide());
        System.out.println("  " + pos1 + " et " + pos2 + " voisines: " + pos1.estVoisine(pos2));
        System.out.println("  " + pos1 + " et " + pos3 + " voisines: " + pos1.estVoisine(pos3));
        
        // Test position invalide
        System.out.println("\n→ Test position invalide:");
        try {
            Position posInvalide = new Position(100, 100);
            System.out.println("  Position " + posInvalide + " valide: " + posInvalide.estValide());
        } catch (PositionException e) {
            System.out.println("  Position invalide correctement détectée: " + e.getMessage());
        }
        
        // Test Obstacle
        System.out.println("\n→ Test Obstacle:");
        Obstacle rocher = new Obstacle(Obstacle.TypeObstacle.ROCHER, pos1);
        Obstacle foret = new Obstacle(Obstacle.TypeObstacle.FORET, pos2);
        Obstacle eau = new Obstacle(Obstacle.TypeObstacle.EAU, pos3);
        
        System.out.println("  Obstacle 1: " + rocher + " à " + rocher.getPosition());
        System.out.println("  Obstacle 2: " + foret + " à " + foret.getPosition());
        System.out.println("  Obstacle 3: " + eau + " à " + eau.getPosition());
        
        // Test génération aléatoire d'obstacles
        System.out.println("\n→ Test génération aléatoire d'obstacles:");
        for (int i = 0; i < 5; i++) {
            Obstacle.TypeObstacle typeAlea = Obstacle.TypeObstacle.getObstacleAlea();
            System.out.println("  Obstacle aléatoire " + (i+1) + ": " + typeAlea);
        }
        
        System.out.println("✓ Classes de base testées avec succès\n");
    }
    
    // 2. Test de la carte
    private static void testCarte() {
        System.out.println("2. TEST DE LA CARTE");
        System.out.println("-------------------");
        
        Carte carte = new Carte();
        
        // Test initialisation
        System.out.println("\n→ Test initialisation:");
        System.out.println("  Nombre d'héros: " + carte.getHeros().size());
        System.out.println("  Nombre de monstres: " + carte.getMonstres().size());
        System.out.println("  Nombre d'obstacles: " + carte.getObstacles().size());
        
        // Test recherche de positions
        System.out.println("\n→ Test recherche de positions:");
        Position posVide = carte.trouvePositionVide();
        System.out.println("  Position vide trouvée: " + posVide);
        
        if (!carte.getHeros().isEmpty()) {
            Position posHeros = carte.getHeros().get(0).getPosition();
            Position posVideVoisine = carte.trouvePositionVide(posHeros);
            System.out.println("  Position vide voisine de " + posHeros + ": " + posVideVoisine);
        }
        
        // Test recherche d'héros
        System.out.println("\n→ Test recherche d'héros:");
        Heros herosAlea = carte.trouveHeros();
        System.out.println("  Héros aléatoire trouvé: " + (herosAlea != null ? herosAlea.getNom() : "Aucun"));
        
        if (!carte.getMonstres().isEmpty()) {
            Position posMonstre = carte.getMonstres().get(0).getPosition();
            Heros herosVoisin = carte.trouveHeros(posMonstre);
            System.out.println("  Héros voisin de monstre à " + posMonstre + ": " + 
                             (herosVoisin != null ? herosVoisin.getNom() : "Aucun"));
        }
        
        // Test affichage de la carte
        System.out.println("\n→ État initial de la carte:");
        afficherCarteSimple(carte);
        
        System.out.println("✓ Carte testée avec succès\n");
    }
    
    // 3. Test des combats
    private static void testCombats() throws PositionException {
        System.out.println("3. TEST DES COMBATS");
        System.out.println("-------------------");
        
        Carte carte = new Carte();
        
        // Créer des soldats spécifiques pour les tests de combat
        System.out.println("\n→ Test combat corps à corps:");
        Position pos1 = new Position(1, 1);
        Position pos2 = new Position(1, 2); // Position voisine
        
        Heros heros = new Heros(carte, ISoldat.TypesH.ELF, "TestH", pos1);
        Monstre monstre = new Monstre(carte, ISoldat.TypesM.ORC, 999, pos2);
        
        System.out.println("  Avant combat - " + heros.getNom() + ": " + heros.getPoints() + " PV");
        System.out.println("  Avant combat - Monstre: " + monstre.getPoints() + " PV");
        
        heros.combat(monstre);
        
        System.out.println("  Après combat - " + heros.getNom() + ": " + heros.getPoints() + " PV");
        System.out.println("  Après combat - Monstre: " + monstre.getPoints() + " PV");
        
        // Test repos
        System.out.println("\n→ Test repos:");
        int pvAvantRepos = heros.getPoints();
        heros.repos();
        System.out.println("  PV avant repos: " + pvAvantRepos);
        System.out.println("  PV après repos: " + heros.getPoints());
        
        // Test mort
        System.out.println("\n→ Test mort:");
        try {
            Heros herosMort = new Heros(carte, ISoldat.TypesH.HOBBIT, "Mort", new Position(3, 3));
            herosMort.combat(new Monstre(carte, ISoldat.TypesM.TROLL, 888, new Position(3, 4)));
            System.out.println("  Héros vivant: " + herosMort.estVivant());
        } catch (PositionException e) {
            System.out.println("  Erreur de position dans le test de mort: " + e.getMessage());
        }
        
        System.out.println("✓ Combats testés avec succès\n");
    }
    
    // 4. Test d'une partie complète
    private static void testPartieComplete() {
        System.out.println("4. TEST PARTIE COMPLÈTE");
        System.out.println("-----------------------");
        
        Carte carte = new Carte();
        int toursMax = 20; // Augmenter pour voir plus d'actions
        
        System.out.println("\n→ Début de la simulation sur " + toursMax + " tours:");
        
        for (int tour = 1; tour <= toursMax; tour++) {
            System.out.println("\n  --- TOUR " + tour + " ---");
            
            // Actions des héros
            int actionsHeros = simulerActionsHerosAutomatiques(carte, tour);
            System.out.println("  Actions des héros: " + actionsHeros + " action(s)");
            
            // Actions des monstres
            carte.jouerSoldats(null);
            
            // NETTOYAGE : Retirer les soldats morts après le tour
            carte.nettoyerSoldatsMorts();
            
            // Statistiques
            int herosVivants = compterHerosVivants(carte);
            int monstresVivants = compterMonstresVivants(carte);
            System.out.println("  Héros vivants: " + herosVivants + " / Monstres vivants: " + monstresVivants);
            
            // Afficher quelques détails de combat
            if (herosVivants < 6 || monstresVivants < 15) {
                System.out.println("  *** COMBATS EN COURS ***");
            }
            
            // Vérifier fin de partie
            if (carte.partieTerminee()) {
                System.out.println("\n  *** PARTIE TERMINÉE AU TOUR " + tour + " ***");
                System.out.println("  Gagnant: " + carte.getGagnant());
                
                // Afficher le bilan final
                System.out.println("\n  BILAN FINAL:");
                for (Heros h : carte.getHeros()) {
                    if (h.estVivant()) {
                        System.out.println("  " + h.getNom() + " - " + h.getPoints() + " PV");
                    }
                }
                break;
            }
            
            if (tour == toursMax) {
                System.out.println("\n  *** LIMITE DE TOURS ATTEINTE ***");
                System.out.println("  État final: " + carte.getGagnant());
            }
        }
        
        System.out.println("\n✓ Partie complète testée avec succès");
    }
    
    // Méthodes utilitaires
    private static void afficherCarteSimple(Carte carte) {
        System.out.println("  Légende: . = vide, H = Héros, M = Monstre, R = Rocher, F = Forêt, E = Eau");
        System.out.println();
        
        for (int y = 0; y < IConfig.HAUTEUR_CARTE; y++) {
            System.out.print("  ");
            for (int x = 0; x < IConfig.LARGEUR_CARTE; x++) {
                // Utiliser le constructeur sans exception pour les positions valides
                Position pos = new Position(x, y, false);
                Element element = carte.getElement(pos);
                
                if (element == null) {
                    System.out.print(" . ");
                } else if (element instanceof Heros) {
                    System.out.print(" H ");
                } else if (element instanceof Monstre) {
                    System.out.print(" M ");
                } else if (element instanceof Obstacle) {
                    Obstacle obs = (Obstacle) element;
                    switch (obs.getType()) {
                        case ROCHER: System.out.print(" R "); break;
                        case FORET: System.out.print(" F "); break;
                        case EAU: System.out.print(" E "); break;
                    }
                }
            }
            System.out.println();
        }
    }
    
    private static int simulerActionsHerosAutomatiques(Carte carte, int tour) {
        int actions = 0;
        
        for (Heros heros : carte.getHeros()) {
            if (heros.estVivant() && heros.peutJouer(tour)) {
                Position posHeros = heros.getPosition();
                
                // Essayer de trouver un monstre à attaquer
                boolean aAgit = false;
                for (int dx = -1; dx <= 1 && !aAgit; dx++) {
                    for (int dy = -1; dy <= 1 && !aAgit; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        
                        try {
                            Position posCible = new Position(posHeros.getX() + dx, posHeros.getY() + dy);
                            if (posCible.estValide()) {
                                Element cible = carte.getElement(posCible);
                                if (cible instanceof Monstre) {
                                    try {
                                        if (carte.actionHeros(posHeros, posCible)) {
                                            actions++;
                                            aAgit = true;
                                        }
                                    } catch (SoldatException | CarteException | PositionException e) {
                                        // Ignorer les exceptions pendant la simulation automatique
                                    }
                                }
                            }
                        } catch (PositionException e) {
                            // Position invalide, on ignore
                        }
                    }
                }
                
                // Si pas d'attaque, essayer de se déplacer
                if (!aAgit) {
                    Position posDeplacement = carte.trouvePositionVide(posHeros);
                    if (posDeplacement != null) {
                        try {
                            if (carte.actionHeros(posHeros, posDeplacement)) {
                                actions++;
                                aAgit = true;
                            }
                        } catch (SoldatException | CarteException | PositionException e) {
                            // Ignorer les exceptions pendant la simulation automatique
                        }
                    }
                }
                
                // Si pas d'action, se reposer
                if (!aAgit) {
                    heros.repos();
                    heros.joueTour(tour);
                    actions++;
                }
            }
        }
        
        return actions;
    }
    
    private static int compterHerosVivants(Carte carte) {
        int count = 0;
        for (Heros h : carte.getHeros()) {
            if (h.estVivant()) count++;
        }
        return count;
    }
    
    private static int compterMonstresVivants(Carte carte) {
        int count = 0;
        for (Monstre m : carte.getMonstres()) {
            if (m.estVivant()) count++;
        }
        return count;
    }
}