package gestionmission.IHM;

import gestionmission.DAO.AgentDAO;
import gestionmission.DAO.MissionDAO;
import gestionmission.DAO.ParticipationDAO;
import gestionmission.Metier.Agent;
import gestionmission.Metier.Mission;
import gestionmission.Metier.Participation;
import gestionmission.Metier.Statut;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Fenêtre modale (JDialog) d'affectation d'un agent à une mission.
 * <p>
 * En création, l'utilisateur choisit la mission puis l'agent : seuls les
 * agents actifs pas encore affectés à cette mission sont proposés.
 * En modification, l'agent et la mission (clé de la participation) sont
 * figés : seuls le rôle et la date d'affectation sont modifiables.
 */
public class AffectationDialog extends JDialog {

    /** Rôles proposés dans la liste (la saisie libre reste possible). */
    private static final String[] ROLES = {
        "Chef de mission", "Pentesteur principal", "Analyste réseau", "Analyste forensique",
        "Responsable campagne", "Configuration TLS", "Support réseau", "Rédaction du rapport"
    };
    private static final int LONGUEUR_ROLE = 100;

    /** Participation modifiée, ou null en création. */
    private final Participation participation;
    private boolean enregistre = false;
    /** Évite de recharger les agents à chaque ajout dans la liste des missions. */
    private boolean chargementEnCours = false;

    private final JComboBox<Mission> comboMission = new JComboBox<>();
    private final JComboBox<Agent> comboAgent = new JComboBox<>();
    private final JLabel labelInfoAgents = new JLabel(" ");
    private final JComboBox<String> comboRole = new JComboBox<>(ROLES);
    private final JTextField champDate = new JTextField(10);
    private final JButton boutonEnregistrer = new JButton("Enregistrer");

    /**
     * Création d'une affectation.
     *
     * @param parent fenêtre appelante
     * @param missionPreselectionnee mission sélectionnée par défaut (peut être null)
     */
    public AffectationDialog(Window parent, Mission missionPreselectionnee) {
        this(parent, null, missionPreselectionnee);
    }

    /**
     * Modification d'une affectation existante (rôle et date).
     */
    public AffectationDialog(Window parent, Participation aModifier) {
        this(parent, aModifier, aModifier.getMission());
    }

    private AffectationDialog(Window parent, Participation aModifier, Mission mission) {
        super(parent, aModifier == null ? "Nouvelle affectation" : "Modifier l'affectation",
                ModalityType.APPLICATION_MODAL);
        this.participation = aModifier;
        construireInterface();
        if (participation == null) {
            chargerMissions(mission);
            champDate.setText(LocalDate.now().toString());
            comboRole.setSelectedIndex(-1);
        } else {
            remplirModification();
        }
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Indique si l'affectation a été enregistrée en base.
     */
    public boolean isEnregistre() {
        return enregistre;
    }

    private void construireInterface() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel titre = new JLabel(participation == null ? "Affecter un agent à une mission" : "Modifier une affectation");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titre.setForeground(new Color(0, 102, 255));
        titre.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        // Affichage lisible des missions dans la liste déroulante
        comboMission.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Mission) {
                    Mission m = (Mission) value;
                    setText(m.getTitre() + "  (" + m.getDateDebut() + ", " + m.getStatut() + ")");
                }
                return this;
            }
        });
        comboMission.addActionListener(e -> chargerAgentsDisponibles());
        comboRole.setEditable(true);
        labelInfoAgents.setForeground(new Color(153, 0, 0));

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ajouterLigne(formulaire, 0, "Mission * :", comboMission);
        ajouterLigne(formulaire, 1, "Agent * :", comboAgent);
        ajouterLigne(formulaire, 2, "", labelInfoAgents);
        ajouterLigne(formulaire, 3, "Rôle * :", comboRole);
        ajouterLigne(formulaire, 4, "Date d'affectation * :", champDate);
        ajouterLigne(formulaire, 5, "", new JLabel("Format AAAA-MM-JJ, au plus tard la date de fin de la mission."));

        boutonEnregistrer.setBackground(new Color(0, 153, 0));
        boutonEnregistrer.setForeground(Color.WHITE);
        boutonEnregistrer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boutonEnregistrer.addActionListener(e -> enregistrer());
        JButton boutonAnnuler = new JButton("Annuler");
        boutonAnnuler.addActionListener(e -> dispose());

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boutons.add(boutonAnnuler);
        boutons.add(boutonEnregistrer);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(titre, BorderLayout.NORTH);
        getContentPane().add(formulaire, BorderLayout.CENTER);
        getContentPane().add(boutons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(boutonEnregistrer);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void ajouterLigne(JPanel panel, int ligne, String libelle, JComponent champ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = ligne;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(libelle), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(champ, c);
    }

    /**
     * Création : charge toutes les missions et sélectionne celle demandée.
     */
    private void chargerMissions(Mission aSelectionner) {
        chargementEnCours = true;
        try {
            List<Mission> missions = new MissionDAO().lister();
            for (Mission m : missions) {
                comboMission.addItem(m);
                if (aSelectionner != null && m.getId() == aSelectionner.getId()) {
                    comboMission.setSelectedItem(m);
                }
            }
        } catch (SQLException e) {
            MessageErreur.afficher(getOwner(), "Chargement des missions", e);
        } finally {
            chargementEnCours = false;
        }
        chargerAgentsDisponibles();
    }

    /**
     * Recharge la liste des agents disponibles pour la mission choisie
     * (agents actifs non encore affectés : requête avec LEFT JOIN).
     */
    private void chargerAgentsDisponibles() {
        if (participation != null || chargementEnCours) {
            return; // en modification, l'agent est figé
        }
        comboAgent.removeAllItems();
        Mission mission = (Mission) comboMission.getSelectedItem();
        if (mission == null) {
            labelInfoAgents.setText("Aucune mission enregistrée.");
            boutonEnregistrer.setEnabled(false);
            return;
        }
        try {
            for (Agent a : new AgentDAO().listerDisponiblesPourMission(mission.getId())) {
                comboAgent.addItem(a);
            }
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Chargement des agents", e);
        }
        boolean disponibles = comboAgent.getItemCount() > 0;
        labelInfoAgents.setText(disponibles
                ? comboAgent.getItemCount() + " agent(s) actif(s) disponible(s)"
                : "Tous les agents actifs sont déjà affectés à cette mission.");
        boutonEnregistrer.setEnabled(disponibles);
    }

    /**
     * Modification : affiche l'agent et la mission (non modifiables),
     * puis le rôle et la date actuels.
     */
    private void remplirModification() {
        comboMission.addItem(participation.getMission());
        comboAgent.addItem(participation.getAgent());
        comboMission.setEnabled(false);
        comboAgent.setEnabled(false);
        labelInfoAgents.setText(" ");
        comboRole.setSelectedItem(participation.getRole());
        champDate.setText(participation.getDateAffectation().toString());
    }

    /**
     * Valide la saisie puis insère ou met à jour la participation.
     */
    private void enregistrer() {
        // ---- 1. Validation de la saisie ----
        Mission mission = (Mission) comboMission.getSelectedItem();
        Agent agent = (Agent) comboAgent.getSelectedItem();
        Object saisieRole = comboRole.getEditor().getItem();
        String role = saisieRole == null ? "" : saisieRole.toString().trim();

        if (mission == null || agent == null) {
            MessageErreur.saisie(this, "Choisissez une mission et un agent.");
            return;
        }
        if (participation == null && mission.getStatut() == Statut.ANNULEE) {
            MessageErreur.saisie(this, "Impossible d'affecter un agent à une mission annulée.");
            return;
        }
        if (role.isEmpty()) {
            MessageErreur.saisie(this, "Le rôle de l'agent est obligatoire.");
            return;
        }
        if (role.length() > LONGUEUR_ROLE) {
            MessageErreur.saisie(this, "Le rôle ne doit pas dépasser " + LONGUEUR_ROLE + " caractères.");
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(champDate.getText().trim());
        } catch (DateTimeParseException e) {
            MessageErreur.saisie(this, "La date doit être au format AAAA-MM-JJ (ex : 2026-10-12).");
            champDate.requestFocus();
            return;
        }
        if (date.isAfter(mission.getDateFin())) {
            MessageErreur.saisie(this, "La date d'affectation ne peut pas être après la fin de la mission ("
                    + mission.getDateFin() + ").");
            champDate.requestFocus();
            return;
        }

        // ---- 2. Enregistrement en base ----
        Participation saisie = new Participation(agent, mission, role, date);
        ParticipationDAO dao = new ParticipationDAO();
        try {
            if (participation == null) {
                dao.ajouter(saisie);
            } else if (!dao.modifier(saisie)) {
                MessageErreur.saisie(this, "Cette affectation n'existe plus en base.");
                return;
            }
            enregistre = true;
            dispose();
        } catch (SQLIntegrityConstraintViolationException e) {
            // Clé primaire (id_agent, id_mission) déjà présente, ou agent/mission supprimé entre-temps
            MessageErreur.saisie(this, "Affectation refusée par la base : l'agent est déjà affecté à cette "
                    + "mission, ou l'agent / la mission n'existe plus.");
            chargerAgentsDisponibles();
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Enregistrement de l'affectation", e);
        }
    }
}
