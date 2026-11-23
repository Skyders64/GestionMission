package gestionmission.IHM;

import gestionmission.DAO.AgentDAO;
import gestionmission.Metier.Agent;
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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

/**
 * Fenêtre modale (JDialog) de saisie d'un agent.
 * <p>
 * Utilisée en création (agent null) ou en modification (agent existant).
 * La saisie est validée avant tout accès à la base ; les erreurs SQL
 * (pseudo déjà utilisé, base indisponible...) sont interceptées et
 * affichées sans fermer la fenêtre.
 */
public class AgentDialog extends JDialog {

    /** Spécialités proposées dans la liste (la saisie libre reste possible). */
    private static final String[] SPECIALITES = {
        "Pentest web", "Réseaux", "Systèmes Linux", "Systèmes Windows", "Forensique",
        "Cryptographie", "Ingénierie sociale", "Développement sécurisé", "Cloud"
    };

    private static final int LONGUEUR_PSEUDO = 50;
    private static final int LONGUEUR_NOM = 80;
    private static final int LONGUEUR_SPECIALITE = 50;

    /** Agent modifié, ou null en création. */
    private final Agent agent;
    /** Passe à true quand l'agent a bien été enregistré en base. */
    private boolean enregistre = false;

    private final JTextField champPseudo = new JTextField(20);
    private final JTextField champNom = new JTextField(20);
    private final JComboBox<String> comboSpecialite = new JComboBox<>(SPECIALITES);
    private final JSpinner spinnerNiveau =
            new JSpinner(new SpinnerNumberModel(Agent.NIVEAU_MIN, Agent.NIVEAU_MIN, Agent.NIVEAU_MAX, 1));
    private final JCheckBox caseActif = new JCheckBox("Agent actif (peut être affecté à une mission)", true);

    /**
     * @param parent fenêtre appelante (bloquée tant que la fenêtre est ouverte)
     * @param agentAModifier agent à modifier, ou null pour créer un nouvel agent
     */
    public AgentDialog(Window parent, Agent agentAModifier) {
        super(parent, agentAModifier == null ? "Nouvel agent" : "Modifier l'agent",
                ModalityType.APPLICATION_MODAL);
        this.agent = agentAModifier;
        construireInterface();
        remplirChamps();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Indique si l'agent a été enregistré (la fenêtre appelante peut alors
     * recharger sa liste).
     */
    public boolean isEnregistre() {
        return enregistre;
    }

    private void construireInterface() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel titre = new JLabel(agent == null ? "Créer un agent" : "Modifier l'agent « " + agent.getPseudo() + " »");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titre.setForeground(new Color(0, 102, 255));
        titre.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        comboSpecialite.setEditable(true);

        // Formulaire : une ligne par champ (libellé à gauche, saisie à droite)
        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        int ligne = 0;
        ajouterLigne(formulaire, ligne++, "Pseudo * :", champPseudo);
        ajouterLigne(formulaire, ligne++, "Nom * :", champNom);
        ajouterLigne(formulaire, ligne++, "Spécialité * :", comboSpecialite);
        ajouterLigne(formulaire, ligne++, "Niveau (" + Agent.NIVEAU_MIN + " à " + Agent.NIVEAU_MAX + ") :", spinnerNiveau);
        ajouterLigne(formulaire, ligne, "", caseActif);

        JButton boutonEnregistrer = new JButton("Enregistrer");
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

        // Entrée = Enregistrer, Échap = Annuler
        getRootPane().setDefaultButton(boutonEnregistrer);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Ajoute une ligne « libellé : champ » au formulaire.
     */
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
     * En modification, pré-remplit les champs avec les valeurs de l'agent.
     */
    private void remplirChamps() {
        if (agent == null) {
            comboSpecialite.setSelectedIndex(-1);
            return;
        }
        champPseudo.setText(agent.getPseudo());
        champNom.setText(agent.getNom());
        comboSpecialite.setSelectedItem(agent.getSpecialite());
        spinnerNiveau.setValue(agent.getNiveau());
        caseActif.setSelected(agent.isActif());
    }

    /**
     * Valide la saisie puis insère ou met à jour l'agent en base.
     */
    private void enregistrer() {
        // ---- 1. Validation de la saisie ----
        String pseudo = champPseudo.getText().trim();
        String nom = champNom.getText().trim();
        Object saisieSpecialite = comboSpecialite.getEditor().getItem();
        String specialite = saisieSpecialite == null ? "" : saisieSpecialite.toString().trim();
        int niveau = (Integer) spinnerNiveau.getValue();

        if (pseudo.isEmpty() || nom.isEmpty() || specialite.isEmpty()) {
            MessageErreur.saisie(this, "Le pseudo, le nom et la spécialité sont obligatoires.");
            return;
        }
        if (!pseudo.matches("[\\p{L}\\p{N}_.-]+")) {
            MessageErreur.saisie(this, "Le pseudo ne doit contenir que des lettres, des chiffres, '_', '-' ou '.' (pas d'espace).");
            champPseudo.requestFocus();
            return;
        }
        if (pseudo.length() > LONGUEUR_PSEUDO || nom.length() > LONGUEUR_NOM || specialite.length() > LONGUEUR_SPECIALITE) {
            MessageErreur.saisie(this, "Longueurs maximales : pseudo " + LONGUEUR_PSEUDO + ", nom " + LONGUEUR_NOM
                    + ", spécialité " + LONGUEUR_SPECIALITE + " caractères.");
            return;
        }
        if (niveau < Agent.NIVEAU_MIN || niveau > Agent.NIVEAU_MAX) {
            MessageErreur.saisie(this, "Le niveau doit être compris entre " + Agent.NIVEAU_MIN + " et " + Agent.NIVEAU_MAX + ".");
            return;
        }

        // ---- 2. Enregistrement en base ----
        // On travaille sur un nouvel objet : si l'enregistrement échoue,
        // l'agent d'origine n'est pas modifié.
        Agent saisi = new Agent(pseudo, nom, specialite, niveau, caseActif.isSelected());
        if (agent != null) {
            saisi.setIdAgent(agent.getIdAgent());
        }

        AgentDAO dao = new AgentDAO();
        try {
            if (agent == null) {
                dao.ajouter(saisi);
            } else if (!dao.modifier(saisi)) {
                MessageErreur.saisie(this, "Cet agent n'existe plus en base (il a peut-être été supprimé).");
                return;
            }
            enregistre = true;
            dispose();
        } catch (SQLIntegrityConstraintViolationException e) {
            // Violation de la contrainte UNIQUE sur le pseudo
            MessageErreur.saisie(this, "Le pseudo « " + pseudo + " » est déjà utilisé par un autre agent.");
            champPseudo.requestFocus();
        } catch (SQLException e) {
            MessageErreur.afficher(this, "Enregistrement de l'agent", e);
        }
    }
}
