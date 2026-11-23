package gestionmission.Metier;

/**
 * Classe métier représentant un agent de l'agence HackAcademy.
 * <p>
 * Correspond à la table {@code agent}. Un agent peut participer à
 * plusieurs missions (voir {@link Participation}).
 */
public class Agent {

    /** Niveau minimum accepté pour un agent. */
    public static final int NIVEAU_MIN = 1;
    /** Niveau maximum accepté pour un agent. */
    public static final int NIVEAU_MAX = 5;

    private int idAgent;
    private String pseudo;
    private String nom;
    private String specialite;
    private int niveau;
    private boolean actif = true;

    /**
     * Constructeur vide (utilisé par le DAO avant de remplir l'objet).
     */
    public Agent() {
    }

    /**
     * Constructeur utilisé pour créer un nouvel agent (l'id sera
     * attribué par la base lors de l'insertion).
     */
    public Agent(String pseudo, String nom, String specialite, int niveau, boolean actif) {
        this.pseudo = pseudo;
        this.nom = nom;
        this.specialite = specialite;
        this.niveau = niveau;
        this.actif = actif;
    }

    public int getIdAgent() {
        return idAgent;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getNom() {
        return nom;
    }

    public String getSpecialite() {
        return specialite;
    }

    public int getNiveau() {
        return niveau;
    }

    public boolean isActif() {
        return actif;
    }

    public void setIdAgent(int idAgent) {
        this.idAgent = idAgent;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /**
     * Texte affiché dans les listes déroulantes (JComboBox) :
     * « pseudo (nom) ».
     */
    @Override
    public String toString() {
        return pseudo + " (" + nom + ")";
    }
}
