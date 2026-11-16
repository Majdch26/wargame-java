package wargame;

/**
 * Niveaux de difficulté : chacun correspond à une stratégie du général-ordinateur.
 */
public enum Difficulte {
    /** Comportement demandé par le sujet : attaque si un héros est à portée, sinon déplacement au hasard. */
    FACILE("Facile"),
    /** Chaque monstre vise le héros visible le plus faible et s'en rapproche en ligne droite. */
    NORMAL("Normal"),
    /** Les monstres partagent ce qu'ils voient, contournent les obstacles, concentrent leurs attaques et se reposent quand ils sont blessés. */
    DIFFICILE("Difficile");

    private final String libelle;

    Difficulte(String libelle) {
        this.libelle = libelle;
    }

    /** @return une nouvelle stratégie correspondant à ce niveau */
    public StrategieMonstres creerStrategie() {
        switch (this) {
            case FACILE: return new StrategieAleatoire();
            case NORMAL: return new StrategieGloutonne();
            default: return new StrategieTactique();
        }
    }

    @Override
    public String toString() {
        return libelle;
    }
}
