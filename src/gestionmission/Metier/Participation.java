package gestionmission.Metier;

import java.time.LocalDate;

/**
 * Classe métier représentant l'affectation d'un agent à une mission.
 * <p>
 * Correspond à la table associative {@code participation} qui porte la
 * relation plusieurs-à-plusieurs entre {@link Agent} et {@link Mission}.
 * Plutôt que de stocker de simples identifiants, la participation référence
 * directement les objets Agent et Mission : le DAO les reconstruit grâce à
 * des requêtes avec jointures (JOIN).
 */
public class Participation {

    private Agent agent;
    private Mission mission;
    private String role;
    private LocalDate dateAffectation;

    /**
     * Constructeur vide (utilisé par le DAO avant de remplir l'objet).
     */
    public Participation() {
    }

    public Participation(Agent agent, Mission mission, String role, LocalDate dateAffectation) {
        this.agent = agent;
        this.mission = mission;
        this.role = role;
        this.dateAffectation = dateAffectation;
    }

    public Agent getAgent() {
        return agent;
    }

    public Mission getMission() {
        return mission;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getDateAffectation() {
        return dateAffectation;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setDateAffectation(LocalDate dateAffectation) {
        this.dateAffectation = dateAffectation;
    }

    @Override
    public String toString() {
        return "Participation : agent = " + (agent != null ? agent.getPseudo() : null)
                + ", mission = " + (mission != null ? mission.getTitre() : null)
                + ", role = " + role + ", dateAffectation = " + dateAffectation;
    }
}
