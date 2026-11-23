package gestionmission.DAO;

import gestionmission.Connexion.ConnexionBDD;
import gestionmission.Metier.Participation;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO de la table associative {@code participation}.
 * <p>
 * Les lectures utilisent des jointures (INNER JOIN) vers les tables
 * {@code agent} et {@code mission} : chaque {@link Participation} est
 * renvoyée avec son Agent et sa Mission complets, en une seule requête.
 */
public class ParticipationDAO {

    /**
     * Début commun des requêtes de lecture : participation + agent + mission.
     * Les colonnes sont listées explicitement pour éviter toute ambiguïté
     * entre les tables (ex : id_agent existe dans agent et participation).
     */
    private static final String SELECT_JOINTURE =
            "SELECT p.role, p.date_affectation, "
            + "a.id_agent, a.pseudo, a.nom, a.specialite, a.niveau, a.actif, "
            + "m.id, m.titre, m.description, m.date_debut, m.date_fin, m.statut, m.priorite, m.budget "
            + "FROM participation p "
            + "INNER JOIN agent a ON a.id_agent = p.id_agent "
            + "INNER JOIN mission m ON m.id = p.id_mission ";

    /**
     * Liste toutes les affectations, triées par date de mission puis pseudo.
     */
    public ArrayList<Participation> listerToutes() throws SQLException {
        String sql = SELECT_JOINTURE + "ORDER BY m.date_debut, m.titre, a.pseudo";
        ArrayList<Participation> lesParticipations = new ArrayList<>();
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
            ResultSet resultats = requete.executeQuery();
        ) {
            while (resultats.next()) {
                lesParticipations.add(construireParticipation(resultats));
            }
        }
        return lesParticipations;
    }

    /**
     * Liste les agents mobilisés sur une mission (avec leur rôle).
     *
     * @param idMission identifiant de la mission
     */
    public ArrayList<Participation> listerParMission(int idMission) throws SQLException {
        String sql = SELECT_JOINTURE + "WHERE p.id_mission = ? ORDER BY a.pseudo";
        return listerAvecParametre(sql, idMission);
    }

    /**
     * Liste les missions auxquelles un agent participe (avec son rôle).
     *
     * @param idAgent identifiant de l'agent
     */
    public ArrayList<Participation> listerParAgent(int idAgent) throws SQLException {
        String sql = SELECT_JOINTURE + "WHERE p.id_agent = ? ORDER BY m.date_debut, m.titre";
        return listerAvecParametre(sql, idAgent);
    }

    /**
     * Compte le nombre d'agents affectés à chaque mission.
     * La jointure externe (LEFT JOIN) permet d'obtenir aussi les missions
     * sans aucun agent (COUNT = 0).
     *
     * @return une table d'association id de mission -> nombre d'agents
     */
    public Map<Integer, Integer> compterAgentsParMission() throws SQLException {
        String sql = "SELECT m.id, COUNT(p.id_agent) AS nb_agents "
                + "FROM mission m "
                + "LEFT JOIN participation p ON p.id_mission = m.id "
                + "GROUP BY m.id";
        Map<Integer, Integer> resultat = new HashMap<>();
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
            ResultSet resultats = requete.executeQuery();
        ) {
            while (resultats.next()) {
                resultat.put(resultats.getInt("id"), resultats.getInt("nb_agents"));
            }
        }
        return resultat;
    }

    /**
     * Indique si l'agent est déjà affecté à la mission.
     */
    public boolean existe(int idAgent, int idMission) throws SQLException {
        String sql = "SELECT 1 FROM participation WHERE id_agent = ? AND id_mission = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, idAgent);
            requete.setInt(2, idMission);
            try (ResultSet resultat = requete.executeQuery()) {
                return resultat.next();
            }
        }
    }

    /**
     * Affecte un agent à une mission.
     *
     * @return le nombre de lignes insérées (1 si succès)
     * @throws java.sql.SQLIntegrityConstraintViolationException si l'agent est
     *         déjà affecté à cette mission (clé primaire composée) ou si
     *         l'agent / la mission n'existe plus (clé étrangère)
     */
    public int ajouter(Participation uneParticipation) throws SQLException {
        String sql = "INSERT INTO participation(id_agent, id_mission, role, date_affectation) VALUES(?, ?, ?, ?)";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, uneParticipation.getAgent().getIdAgent());
            requete.setInt(2, uneParticipation.getMission().getId());
            requete.setString(3, uneParticipation.getRole());
            requete.setDate(4, Date.valueOf(uneParticipation.getDateAffectation()));
            return requete.executeUpdate();
        }
    }

    /**
     * Modifie le rôle et la date d'une affectation existante.
     * (L'agent et la mission forment la clé : ils ne sont pas modifiables.)
     *
     * @return true si une ligne a été modifiée
     */
    public boolean modifier(Participation uneParticipation) throws SQLException {
        String sql = "UPDATE participation SET role = ?, date_affectation = ? WHERE id_agent = ? AND id_mission = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setString(1, uneParticipation.getRole());
            requete.setDate(2, Date.valueOf(uneParticipation.getDateAffectation()));
            requete.setInt(3, uneParticipation.getAgent().getIdAgent());
            requete.setInt(4, uneParticipation.getMission().getId());
            return requete.executeUpdate() > 0;
        }
    }

    /**
     * Retire un agent d'une mission (supprime l'affectation).
     *
     * @return true si une ligne a été supprimée
     */
    public boolean supprimer(Participation uneParticipation) throws SQLException {
        String sql = "DELETE FROM participation WHERE id_agent = ? AND id_mission = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, uneParticipation.getAgent().getIdAgent());
            requete.setInt(2, uneParticipation.getMission().getId());
            return requete.executeUpdate() > 0;
        }
    }

    /**
     * Exécute une requête de lecture ayant un seul paramètre entier.
     */
    private ArrayList<Participation> listerAvecParametre(String sql, int parametre) throws SQLException {
        ArrayList<Participation> lesParticipations = new ArrayList<>();
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, parametre);
            try (ResultSet resultats = requete.executeQuery()) {
                while (resultats.next()) {
                    lesParticipations.add(construireParticipation(resultats));
                }
            }
        }
        return lesParticipations;
    }

    /**
     * Construit une Participation (avec son Agent et sa Mission) à partir
     * de la ligne courante d'un ResultSet issu de {@link #SELECT_JOINTURE}.
     */
    private Participation construireParticipation(ResultSet resultat) throws SQLException {
        Participation participation = new Participation();
        participation.setAgent(AgentDAO.construireAgent(resultat));
        participation.setMission(MissionDAO.construireMission(resultat));
        participation.setRole(resultat.getString("role"));
        participation.setDateAffectation(resultat.getDate("date_affectation").toLocalDate());
        return participation;
    }
}
