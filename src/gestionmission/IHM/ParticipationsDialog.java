package gestionmission.IHM;

import gestionmission.DAO.ParticipationDAO;
import gestionmission.Metier.Agent;
import gestionmission.Metier.Mission;
import gestionmission.Metier.Participation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

/**
 * Fenêtre modale de consultation (lecture seule) des affectations.
 * <p>
 * Deux usages :
 * <ul>
 *   <li>les agents mobilisés sur une mission (depuis « Consulter les missions ») ;</li>
 *   <li>les missions d'un agent (depuis « Gestion des agents »).</li>
 * </ul>
 * Les données proviennent de requêtes avec jointures de {@link ParticipationDAO}.
 */
public class ParticipationsDialog extends JDialog {

    private final JTable table = new JTable();
    private final JLabel labelResume = new JLabel();

    /**
     * Affiche les agents mobilisés sur une mission.
     */
    public ParticipationsDialog(Window parent, Mission mission) {
        super(parent, "Agents mobilisés", ModalityType.APPLICATION_MODAL);
        construireInterface("Agents mobilisés sur « " + mission.getTitre() + " »",
                "Du " + mission.getDateDebut() + " au " + mission.getDateFin()
                + "  -  statut : " + mission.getStatut() + "  -  priorité : " + mission.getPriorite());

        ModeleTable modele = new ModeleTable("Pseudo", "Nom", "Spécialité", "Niveau", "Actif", "Rôle", "Affecté le");
        List<Participation> participations = charger(() -> new ParticipationDAO().listerParMission(mission.getId()));
        for (Participation p : participations) {
            Agent a = p.getAgent();
            modele.addRow(new Object[]{
                a.getPseudo(), a.getNom(), a.getSpecialite(), a.getNiveau(), a.isActif(),
                p.getRole(), p.getDateAffectation()
            });
        }
        terminer(modele, participations.size() + " agent(s) affecté(s) à cette mission.");
    }

    /**
     * Affiche les missions auxquelles participe un agent.
     */
    public ParticipationsDialog(Window parent, Agent agent) {
        super(parent, "Missions de l'agent", ModalityType.APPLICATION_MODAL);
        construireInterface("Missions de " + agent.getPseudo() + " (" + agent.getNom() + ")",
                agent.getSpecialite() + "  -  niveau " + agent.getNiveau()
                + (agent.isActif() ? "" : "  -  agent inactif"));

        ModeleTable modele = new ModeleTable("Mission", "Début", "Fin", "Statut", "Priorité", "Rôle", "Affecté le");
        List<Participation> participations = charger(() -> new ParticipationDAO().listerParAgent(agent.getIdAgent()));
        for (Participation p : participations) {
            Mission m = p.getMission();
            modele.addRow(new Object[]{
                m.getTitre(), m.getDateDebut(), m.getDateFin(), m.getStatut(), m.getPriorite(),
                p.getRole(), p.getDateAffectation()
            });
        }
        terminer(modele, "Participe à " + participations.size() + " mission(s).");
    }

    /**
     * Interface fonctionnelle permettant de passer un appel DAO en paramètre.
     */
    @FunctionalInterface
    private interface RequeteParticipations {
        List<Participation> executer() throws SQLException;
    }

    /**
     * Exécute la requête et intercepte une éventuelle SQLException :
     * la fenêtre s'affiche alors avec une liste vide.
     */
    private List<Participation> charger(RequeteParticipations requete) {
        try {
            return requete.executer();
        } catch (SQLException e) {
            MessageErreur.afficher(getOwner(), "Chargement des affectations", e);
            return new ArrayList<>();
        }
    }

    private void construireInterface(String titre, String sousTitre) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel labelTitre = new JLabel(titre);
        labelTitre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        labelTitre.setForeground(new Color(204, 0, 0));
        JLabel labelSousTitre = new JLabel(sousTitre);

        JPanel entete = new JPanel(new BorderLayout(0, 4));
        entete.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        entete.add(labelTitre, BorderLayout.NORTH);
        entete.add(labelSousTitre, BorderLayout.SOUTH);

        JScrollPane defilement = new JScrollPane(table);
        defilement.setPreferredSize(new Dimension(760, 220));

        JButton boutonFermer = new JButton("Fermer");
        boutonFermer.addActionListener(e -> dispose());

        JPanel bas = new JPanel(new BorderLayout());
        bas.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 4));
        bas.add(labelResume, BorderLayout.WEST);
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boutons.add(boutonFermer);
        bas.add(boutons, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(entete, BorderLayout.NORTH);
        getContentPane().add(defilement, BorderLayout.CENTER);
        getContentPane().add(bas, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(boutonFermer);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void terminer(ModeleTable modele, String resume) {
        ModeleTable.appliquer(table, modele);
        labelResume.setText(resume);
        pack();
        setLocationRelativeTo(getOwner());
    }
}
