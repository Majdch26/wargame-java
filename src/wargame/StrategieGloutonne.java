package wargame;

/**
 * Stratégie de la version rendue, corrigée : chaque monstre choisit le héros le plus faible
 * et le plus proche parmi ceux qu'il voit, l'attaque s'il le peut, sinon fait un pas
 * vers lui (en ligne droite, sans contourner les obstacles). Sans héros en vue,
 * il se déplace au hasard.
 */
public class StrategieGloutonne implements StrategieMonstres {
    private static final long serialVersionUID = 1L;

    @Override
    public void jouer(Carte carte, Monstre monstre) {
        Position ici = monstre.getPosition();
        Heros cible = null;
        double meilleur = Double.NEGATIVE_INFINITY;
        for (Heros h : carte.getHeros()) {
            int d = ici.distance(h.getPosition());
            if (d <= monstre.getPortee()) {
                double score = 1.0 / (d + 1) / (h.getPoints() + 1) * (carte.peutAttaquer(monstre, h) ? 4 : 1);
                if (score > meilleur) {
                    meilleur = score;
                    cible = h;
                }
            }
        }

        if (cible != null && carte.peutAttaquer(monstre, cible)) {
            monstre.combat(cible);
            return;
        }
        if (cible == null) {
            StrategieMonstres.deplacementAleatoire(carte, monstre);
            return;
        }
        Position objectif = cible.getPosition();
        Position meilleureCase = null;
        int meilleureDistance = ici.distance(objectif);
        for (Position p : carte.voisinesLibres(ici)) {
            if (p.distance(objectif) < meilleureDistance) {
                meilleureDistance = p.distance(objectif);
                meilleureCase = p;
            }
        }
        if (meilleureCase != null) {
            StrategieMonstres.deplacer(carte, monstre, meilleureCase);
        }
    }
}
