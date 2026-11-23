package gestionmission.DAO;

import gestionmission.Connexion.ConnexionBDD;
import gestionmission.Metier.Agent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * DAO (Data Access Object) de la table {@code agent}.
 * <p>
 * Toutes les requêtes passent par des {@link PreparedStatement} (requêtes
 * paramétrées, protection contre l'injection SQL) et les ressources JDBC
 * sont fermées automatiquement grâce au try-with-resources.
 * Les {@link SQLException} sont propagées à la couche IHM qui se charge
 * d'afficher un message à l'utilisateur.
 */
public class AgentDAO {

    /**
     * Liste tous les agents, triés par pseudo.
     */
    public ArrayList<Agent> lister() throws SQLException {
        String sql = "SELECT * FROM agent ORDER BY pseudo";
        return listerAvecRequete(sql);
    }

    /**
     * Liste uniquement les agents actifs, triés par pseudo.
     */
    public ArrayList<Agent> listerActifs() throws SQLException {
        String sql = "SELECT * FROM agent WHERE actif = TRUE ORDER BY pseudo";
        return listerAvecRequete(sql);
    }

    /**
     * Liste les agents actifs qui ne sont pas encore affectés à la mission
     * donnée. Utilise une jointure externe (LEFT JOIN) : les agents sans
     * participation pour cette mission ont des colonnes p.* à NULL.
     *
     * @param idMission identifiant de la mission
     */
    public ArrayList<Agent> listerDisponiblesPourMission(int idMission) throws SQLException {
        ArrayList<Agent> lesAgents = new ArrayList<>();
        String sql = "SELECT a.* FROM agent a "
                + "LEFT JOIN participation p ON p.id_agent = a.id_agent AND p.id_mission = ? "
                + "WHERE a.actif = TRUE AND p.id_agent IS NULL "
                + "ORDER BY a.pseudo";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, idMission);
            try (ResultSet resultats = requete.executeQuery()) {
                while (resultats.next()) {
                    lesAgents.add(construireAgent(resultats));
                }
            }
        }
        return lesAgents;
    }

    /**
     * Recherche un agent par son identifiant.
     *
     * @return l'agent trouvé, ou null s'il n'existe pas
     */
    public Agent trouverParId(int idAgent) throws SQLException {
        String sql = "SELECT * FROM agent WHERE id_agent = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, idAgent);
            try (ResultSet resultat = requete.executeQuery()) {
                if (resultat.next()) {
                    return construireAgent(resultat);
                }
            }
        }
        return null;
    }

    /**
     * Insère un nouvel agent. L'identifiant généré par la base est
     * renseigné dans l'objet passé en paramètre.
     *
     * @return le nombre de lignes insérées (1 si succès)
     * @throws java.sql.SQLIntegrityConstraintViolationException si le pseudo existe déjà
     */
    public int ajouter(Agent unAgent) throws SQLException {
        String sql = "INSERT INTO agent(pseudo, nom, specialite, niveau, actif) VALUES(?, ?, ?, ?, ?)";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {
            renseignerRequete(requete, unAgent);
            int nbLignes = requete.executeUpdate();

            try (ResultSet cles = requete.getGeneratedKeys()) {
                if (cles.next()) {
                    unAgent.setIdAgent(cles.getInt(1));
                }
            }
            return nbLignes;
        }
    }

    /**
     * Met à jour un agent existant (identifié par son id).
     *
     * @return true si une ligne a été modifiée
     * @throws java.sql.SQLIntegrityConstraintViolationException si le nouveau pseudo est déjà pris
     */
    public boolean modifier(Agent unAgent) throws SQLException {
        String sql = "UPDATE agent SET pseudo = ?, nom = ?, specialite = ?, niveau = ?, actif = ? WHERE id_agent = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            renseignerRequete(requete, unAgent); // paramètres 1 à 5
            requete.setInt(6, unAgent.getIdAgent()); // paramètre 6 : le WHERE
            return requete.executeUpdate() > 0;
        }
    }

    /**
     * Supprime un agent. Grâce au ON DELETE CASCADE de la table
     * participation, ses affectations sont supprimées automatiquement.
     *
     * @return true si une ligne a été supprimée
     */
    public boolean supprimer(Agent unAgent) throws SQLException {
        String sql = "DELETE FROM agent WHERE id_agent = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, unAgent.getIdAgent());
            return requete.executeUpdate() > 0;
        }
    }

    /**
     * Exécute une requête SELECT sans paramètre et construit la liste d'agents.
     */
    private ArrayList<Agent> listerAvecRequete(String sql) throws SQLException {
        ArrayList<Agent> lesAgents = new ArrayList<>();
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
            ResultSet resultats = requete.executeQuery();
        ) {
            while (resultats.next()) {
                lesAgents.add(construireAgent(resultats));
            }
        }
        return lesAgents;
    }

    /**
     * Renseigne les paramètres 1 à 5 communs à l'INSERT et à l'UPDATE.
     */
    private void renseignerRequete(PreparedStatement requete, Agent agent) throws SQLException {
        requete.setString(1, agent.getPseudo());
        requete.setString(2, agent.getNom());
        requete.setString(3, agent.getSpecialite());
        requete.setInt(4, agent.getNiveau());
        requete.setBoolean(5, agent.isActif());
    }

    /**
     * Construit un Agent à partir de la ligne courante du ResultSet.
     * Visibilité package (et static) : ParticipationDAO la réutilise pour
     * reconstruire l'agent lu par jointure.
     */
    static Agent construireAgent(ResultSet resultat) throws SQLException {
        Agent agent = new Agent();
        agent.setIdAgent(resultat.getInt("id_agent"));
        agent.setPseudo(resultat.getString("pseudo"));
        agent.setNom(resultat.getString("nom"));
        agent.setSpecialite(resultat.getString("specialite"));
        agent.setNiveau(resultat.getInt("niveau"));
        agent.setActif(resultat.getBoolean("actif"));
        return agent;
    }
}
