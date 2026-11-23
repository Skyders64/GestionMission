package gestionmission.IHM;

import gestionmission.DAO.AgentDAO;
import gestionmission.DAO.ParticipationDAO;
import gestionmission.Metier.Agent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Fenêtre de gestion des agents : liste (JTable), création, modification,
 * suppression et consultation des missions d'un agent.
 * <p>
 * La création et la modification se font dans la fenêtre modale
 * {@link AgentDialog} ; la consultation dans {@link ParticipationsDialog}.
 */
public class GestionAgents extends JFrame {

    private final JTable tableAgents = new JTable();
    private final JCheckBox caseInactifs = new JCheckBox("Afficher les agents inactifs", true);
    private final JLabel labelNombre = new JLabel();
    private final JButton boutonAjouter = new JButton("Nouvel agent");
    private final JButton boutonModifier = new JButton("Modifier");
    private final JButton boutonSupprimer = new JButton("Supprimer");
    private final JButton boutonMissions = new JButton("Voir ses missions");
    private final JButton boutonRetour = new JButton("Retour");

    /** Agents affichés, dans le même ordre que les lignes du modèle. */
    private List<Agent> agents = new ArrayList<>();

    public GestionAgents() {
        setTitle("Gestion des agents");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construireInterface();
        chargerAgents();
        setLocationRelativeTo(null);
    }

    private void construireInterface() {
        JLabel titre = new JLabel("Gestion des agents");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titre.setForeground(new Color(204, 0, 0));

        JPanel entete = new JPanel(new BorderLayout());
        entete.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        entete.add(titre, BorderLayout.WEST);
        entete.add(caseInactifs, BorderLayout.EAST);
        caseInactifs.addActionListener(e -> chargerAgents());

        JScrollPane defilement = new JScrollPane(tableAgents);
        defilement.setPreferredSize(new Dimension(780, 280));
        tableAgents.setToolTipText("Double-cliquez sur un agent pour le modifier");
        tableAgents.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    modifierAgent();
                }
            }
        });
        tableAgents.getSelectionModel().addListSelectionListener(e -> majBoutons());

        boutonAjouter.setBackground(new Color(0, 153, 0));
        boutonAjouter.setForeground(Color.WHITE);
        boutonSupprimer.setBackground(new Color(153, 0, 0));
        boutonSupprimer.setForeground(Color.WHITE);

        boutonAjouter.addActionListener(e -> ajouterAgent());
        boutonModifier.addActionListener(e -> modifierAgent());
        boutonSupprimer.addActionListener(e -> supprimerAgent());
        boutonMissions.addActionListener(e -> voirMissions());
        boutonRetour.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(boutonAjouter);
        actions.add(boutonModifier);
        actions.add(boutonSupprimer);
        actions.add(boutonMissions);

        JPanel bas = new JPanel(new BorderLayout());
        bas.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        bas.add(actions, BorderLayout.WEST);
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        droite.add(labelNombre);
        droite.add(boutonRetour);
        bas.add(droite, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(entete, BorderLayout.NORTH);
        getContentPane().add(defilement, BorderLayout.CENTER);
        getContentPane().add(bas, BorderLayout.SOUTH);
        pack();
    }

    /**
     * (Re)charge la liste des agents depuis la base.
     */
    private void chargerAgents() {
        ModeleTable modele = new ModeleTable("ID", "Pseudo", "Nom", "Spécialité", "Niveau", "Actif");
        AgentDAO dao = new AgentDAO();
        try {
            agents = caseInactifs.isSelected() ? dao.lister() : dao.listerActifs();
        } catch (SQLException e) {
            agents = new ArrayList<>();
            MessageErreur.afficher(this, "Chargement des agents", e);
        }
        for (Agent a : agents) {
            modele.addRow(new Object[]{
                a.getIdAgent(), a.getPseudo(), a.getNom(), a.getSpecialite(), a.getNiveau(), a.isActif()
            });
        }
        ModeleTable.appliquer(tableAgents, modele);
        tableAgents.getColumnModel().getColumn(0).setMaxWidth(50);
        tableAgents.getColumnModel().getColumn(4).setMaxWidth(70);
        tableAgents.getColumnModel().getColumn(5).setMaxWidth(60);
        labelNombre.setText(agents.size() + " agent(s)");
        majBoutons();
    }

    /**
     * Active les boutons qui nécessitent une ligne sélectionnée.
     */
    private void majBoutons() {
        boolean selection = tableAgents.getSelectedRow() != -1;
        boutonModifier.setEnabled(selection);
        boutonSupprimer.setEnabled(selection);
        boutonMissions.setEnabled(selection);
    }

    /**
     * @return l'agent de la ligne sélectionnée, ou null
     */
    private Agent agentSelectionne() {
        int ligneVue = tableAgents.getSelectedRow();
        if (ligneVue == -1) {
            return null;
        }
        // Le tri est activé : on convertit l'index affiché en index du modèle
        return agents.get(tableAgents.convertRowIndexToModel(ligneVue));
    }

    private void ajouterAgent() {
        AgentDialog dialogue = new AgentDialog(this, null);
        dialogue.setVisible(true); // bloquant : fenêtre modale
        if (dialogue.isEnregistre()) {
            chargerAgents();
            JOptionPane.showMessageDialog(this, "Agent créé avec succès.");
        }
    }

    private void modifierAgent() {
        Agent agent = agentSelectionne();
        if (agent == null) {
            return;
        }
        AgentDialog dialogue = new AgentDialog(this, agent);
        dialogue.setVisible(true);
        if (dialogue.isEnregistre()) {
            chargerAgents();
        }
    }

    private void supprimerAgent() {
        Agent agent = agentSelectionne();
        if (agent == null) {
            return;
        }
        try {
            int nbMissions = new ParticipationDAO().listerParAgent(agent.getIdAgent()).size();
            String message = "Supprimer définitivement l'agent « " + agent.getPseudo() + " » ?";
            if (nbMissions > 0) {
                message += "\n\nIl est affecté à " + nbMissions + " mission(s) : ces affectations seront "
                        + "aussi supprimées.\nPour garder l'historique, vous pouvez plutôt le rendre inactif.";
            }
            int choix = JOptionPane.showConfirmDialog(this, message, "Confirmation",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choix != JOptionPane.YES_OPTION) {
                return;
            }
            if (!new AgentDAO().supprimer(agent)) {
                MessageErreur.saisie(this, "Aucun agent supprimé (il n'existe peut-être plus).");
            }
            chargerAgents();
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Suppression de l'agent", e);
        }
    }

    private void voirMissions() {
        Agent agent = agentSelectionne();
        if (agent != null) {
            new ParticipationsDialog(this, agent).setVisible(true);
        }
    }
}
