package gestionmission;

import gestionmission.DAO.AgentDAO;
import gestionmission.DAO.MissionDAO;
import gestionmission.DAO.ParticipationDAO;
import gestionmission.Metier.Agent;
import gestionmission.Metier.Mission;
import gestionmission.Metier.Participation;
import gestionmission.Metier.Priorite;
import gestionmission.Metier.Statut;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Programme de test des DAO en mode console (sans interface graphique).
 * <p>
 * Il crée des données temporaires, les modifie puis les supprime : la base
 * se retrouve dans son état initial à la fin de l'exécution.
 * L'application graphique se lance avec {@link gestionmission.IHM.Menu}.
 *
 * @author matteo.douteaud
 */
public class GestionMission {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            testerMissionDAO();
            testerAgentEtParticipationDAO();
        } catch (SQLException ex) {
            System.getLogger(GestionMission.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * Tests du TP1 : CRUD sur les missions.
     */
    private static void testerMissionDAO() throws SQLException {
        System.out.println("===== Missions =====");
        MissionDAO uneMissionDAO = new MissionDAO();
        ArrayList<Mission> lesMissions = uneMissionDAO.lister();
        for (Mission uneMission : lesMissions) {
            System.out.println(uneMission.toString());
        }

        Mission missionTest = new Mission(
                "Mission test  DAO",
                "Mission temporaire créée depuis la console. ",
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                Statut.A_PREPARER,
                Priorite.NORMALE,
                1250.00
        );

        int resultat = uneMissionDAO.ajouter(missionTest);
        System.out.println(resultat == 1 ? "Mission ajoutée" : "echec");

        missionTest.setTitre("Mission modifiée");
        boolean res = uneMissionDAO.modifier(missionTest);
        System.out.println(res ? "Mission modifier" : "modification echouée");

        res = uneMissionDAO.supprimer(missionTest);
        System.out.println(res ? "Mission supprimer" : "supprimer echouée");
    }

    /**
     * Tests du TP2 : agents, affectations et requêtes avec jointures.
     */
    private static void testerAgentEtParticipationDAO() throws SQLException {
        AgentDAO agentDAO = new AgentDAO();
        MissionDAO missionDAO = new MissionDAO();
        ParticipationDAO participationDAO = new ParticipationDAO();

        System.out.println();
        System.out.println("===== Agents =====");
        for (Agent agent : agentDAO.lister()) {
            System.out.println(agent.getIdAgent() + " - " + agent + " - " + agent.getSpecialite()
                    + " - niveau " + agent.getNiveau() + (agent.isActif() ? "" : " (inactif)"));
        }

        System.out.println();
        System.out.println("===== Agents mobilisés par mission (JOIN) =====");
        for (Participation p : participationDAO.listerToutes()) {
            System.out.println(p.getMission().getTitre() + " -> " + p.getAgent().getPseudo()
                    + " [" + p.getRole() + ", le " + p.getDateAffectation() + "]");
        }

        System.out.println();
        System.out.println("===== Scénario de test =====");
        // 1. Création d'un agent et d'une mission temporaires
        Agent agentTest = new Agent("TestConsole", "Agent Temporaire", "Tests", 1, true);
        System.out.println(agentDAO.ajouter(agentTest) == 1
                ? "Agent ajouté (id " + agentTest.getIdAgent() + ")" : "Ajout agent échoué");

        Mission missionTest = new Mission("Mission test affectation", "Mission temporaire.",
                LocalDate.now(), LocalDate.now().plusDays(1), Statut.A_PREPARER, Priorite.BASSE, 0);
        missionDAO.ajouter(missionTest);

        // 2. Le pseudo est UNIQUE : un doublon doit être refusé par la base
        try {
            agentDAO.ajouter(new Agent("TestConsole", "Doublon", "Tests", 1, true));
            System.out.println("ERREUR : le doublon de pseudo a été accepté");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Doublon de pseudo refusé (attendu) : " + e.getMessage());
        }

        // 3. Affectation de l'agent à la mission
        Participation participation = new Participation(agentTest, missionTest, "Testeur", LocalDate.now());
        System.out.println(participationDAO.ajouter(participation) == 1 ? "Affectation ajoutée" : "Affectation échouée");

        // 4. Une seconde affectation identique viole la clé primaire composée
        try {
            participationDAO.ajouter(participation);
            System.out.println("ERREUR : la double affectation a été acceptée");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Double affectation refusée (attendu) : " + e.getMessage());
        }

        // 5. Modification du rôle puis lecture par jointure
        participation.setRole("Testeur principal");
        System.out.println(participationDAO.modifier(participation) ? "Affectation modifiée" : "Modification échouée");
        for (Participation p : participationDAO.listerParMission(missionTest.getId())) {
            System.out.println("  " + p);
        }
        System.out.println("Agents disponibles pour la mission test : "
                + agentDAO.listerDisponiblesPourMission(missionTest.getId()).size());
        System.out.println("Nombre d'agents sur la mission test : "
                + participationDAO.compterAgentsParMission().get(missionTest.getId()));

        // 6. Suppression de l'agent : ON DELETE CASCADE supprime son affectation
        System.out.println(agentDAO.supprimer(agentTest) ? "Agent supprimé" : "Suppression agent échouée");
        System.out.println("Affectation encore présente après suppression de l'agent : "
                + participationDAO.existe(agentTest.getIdAgent(), missionTest.getId()));
        missionDAO.supprimer(missionTest);
        System.out.println("Données de test nettoyées.");
    }
}
