package gestionmission.IHM;

import gestionmission.DAO.MissionDAO;
import gestionmission.DAO.ParticipationDAO;
import gestionmission.Metier.Agent;
import gestionmission.Metier.Mission;
import gestionmission.Metier.Participation;
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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Fenêtre de gestion des affectations des agents aux missions.
 * <p>
 * Affiche dans une JTable les participations obtenues par jointure
 * (mission + agent + rôle + date), avec un filtre par mission.
 * Permet d'affecter un agent, de modifier son rôle / sa date et de le
 * retirer d'une mission.
 */
public class GestionAffectations extends JFrame {

    /** Premier choix de la liste : pas de filtre. */
    private static final String TOUTES = "-- Toutes les missions --";

    private final JComboBox<Object> comboFiltre = new JComboBox<>();
    private final JTable tableAffectations = new JTable();
    private final JLabel labelNombre = new JLabel();
    private final JButton boutonAffecter = new JButton("Affecter un agent");
    private final JButton boutonModifier = new JButton("Modifier");
    private final JButton boutonRetirer = new JButton("Retirer de la mission");
    private final JButton boutonRetour = new JButton("Retour");

    /** Affectations affichées, dans le même ordre que les lignes du modèle. */
    private List<Participation> participations = new ArrayList<>();
    /** Évite de recharger la table pendant le remplissage du filtre. */
    private boolean chargementEnCours = false;

    public GestionAffectations() {
        setTitle("Affectations des agents aux missions");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construireInterface();
        chargerFiltre();
        chargerAffectations();
        setLocationRelativeTo(null);
    }

    private void construireInterface() {
        JLabel titre = new JLabel("Affectations des agents aux missions");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titre.setForeground(new Color(204, 0, 0));

        comboFiltre.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Mission) {
                    Mission m = (Mission) value;
                    setText(m.getId() + " - " + m.getTitre());
                }
                return this;
            }
        });
        comboFiltre.addActionListener(e -> {
            if (!chargementEnCours) {
                chargerAffectations();
            }
        });

        JPanel filtre = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filtre.add(new JLabel("Mission :"));
        filtre.add(comboFiltre);

        JPanel entete = new JPanel(new BorderLayout());
        entete.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 8));
        entete.add(titre, BorderLayout.WEST);
        entete.add(filtre, BorderLayout.EAST);

        JScrollPane defilement = new JScrollPane(tableAffectations);
        defilement.setPreferredSize(new Dimension(980, 300));
        tableAffectations.setToolTipText("Double-cliquez sur une affectation pour la modifier");
        tableAffectations.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    modifierAffectation();
                }
            }
        });
        tableAffectations.getSelectionModel().addListSelectionListener(e -> majBoutons());

        boutonAffecter.setBackground(new Color(0, 153, 0));
        boutonAffecter.setForeground(Color.WHITE);
        boutonRetirer.setBackground(new Color(153, 0, 0));
        boutonRetirer.setForeground(Color.WHITE);

        boutonAffecter.addActionListener(e -> affecterAgent());
        boutonModifier.addActionListener(e -> modifierAffectation());
        boutonRetirer.addActionListener(e -> retirerAgent());
        boutonRetour.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(boutonAffecter);
        actions.add(boutonModifier);
        actions.add(boutonRetirer);

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
     * Remplit la liste de filtre avec les missions de la base.
     */
    private void chargerFiltre() {
        chargementEnCours = true;
        try {
            comboFiltre.removeAllItems();
            comboFiltre.addItem(TOUTES);
            for (Mission m : new MissionDAO().lister()) {
                comboFiltre.addItem(m);
            }
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Chargement des missions", e);
        } finally {
            chargementEnCours = false;
        }
    }

    /**
     * @return la mission choisie dans le filtre, ou null pour « toutes »
     */
    private Mission missionFiltree() {
        Object choix = comboFiltre.getSelectedItem();
        return (choix instanceof Mission) ? (Mission) choix : null;
    }

    /**
     * (Re)charge les affectations : toutes, ou celles de la mission filtrée.
     */
    private void chargerAffectations() {
        ModeleTable modele = new ModeleTable("Mission", "Début", "Fin", "Statut",
                "Agent", "Nom", "Spécialité", "Rôle", "Affecté le");
        ParticipationDAO dao = new ParticipationDAO();
        Mission filtre = missionFiltree();
        try {
            participations = (filtre == null) ? dao.listerToutes() : dao.listerParMission(filtre.getId());
        } catch (SQLException e) {
            participations = new ArrayList<>();
            MessageErreur.afficher(this, "Chargement des affectations", e);
        }
        for (Participation p : participations) {
            Mission m = p.getMission();
            Agent a = p.getAgent();
            modele.addRow(new Object[]{
                m.getTitre(), m.getDateDebut(), m.getDateFin(), m.getStatut(),
                a.getPseudo() + (a.isActif() ? "" : " (inactif)"), a.getNom(), a.getSpecialite(),
                p.getRole(), p.getDateAffectation()
            });
        }
        ModeleTable.appliquer(tableAffectations, modele);
        tableAffectations.getColumnModel().getColumn(0).setPreferredWidth(220);
        tableAffectations.getColumnModel().getColumn(7).setPreferredWidth(180);
        labelNombre.setText(participations.size() + " affectation(s)");
        majBoutons();
    }

    private void majBoutons() {
        boolean selection = tableAffectations.getSelectedRow() != -1;
        boutonModifier.setEnabled(selection);
        boutonRetirer.setEnabled(selection);
    }

    private Participation participationSelectionnee() {
        int ligneVue = tableAffectations.getSelectedRow();
        if (ligneVue == -1) {
            return null;
        }
        return participations.get(tableAffectations.convertRowIndexToModel(ligneVue));
    }

    private void affecterAgent() {
        AffectationDialog dialogue = new AffectationDialog(this, missionFiltree());
        dialogue.setVisible(true); // bloquant : fenêtre modale
        if (dialogue.isEnregistre()) {
            chargerAffectations();
            JOptionPane.showMessageDialog(this, "Agent affecté à la mission.");
        }
    }

    private void modifierAffectation() {
        Participation p = participationSelectionnee();
        if (p == null) {
            return;
        }
        AffectationDialog dialogue = new AffectationDialog(this, p);
        dialogue.setVisible(true);
        if (dialogue.isEnregistre()) {
            chargerAffectations();
        }
    }

    private void retirerAgent() {
        Participation p = participationSelectionnee();
        if (p == null) {
            return;
        }
        int choix = JOptionPane.showConfirmDialog(this,
                "Retirer « " + p.getAgent().getPseudo() + " » de la mission « " + p.getMission().getTitre() + " » ?",
                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (!new ParticipationDAO().supprimer(p)) {
                MessageErreur.saisie(this, "Cette affectation n'existe plus.");
            }
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Retrait de l'agent", e);
        }
        chargerAffectations();
    }
}
