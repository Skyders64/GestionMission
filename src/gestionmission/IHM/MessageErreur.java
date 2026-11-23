package gestionmission.IHM;

import java.awt.Component;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Centralise l'affichage des erreurs SQL dans l'interface graphique.
 * <p>
 * Les DAO propagent les {@link SQLException} ; les fenêtres les interceptent
 * et appellent {@link #afficher(Component, String, SQLException)} qui traduit
 * le code d'erreur MySQL en un message compréhensible par l'utilisateur.
 */
public final class MessageErreur {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(MessageErreur.class.getName());

    // Quelques codes d'erreur MySQL courants
    private static final int ACCES_REFUSE = 1045;
    private static final int BASE_INCONNUE = 1049;
    private static final int TABLE_INCONNUE = 1146;
    private static final int TEXTE_TROP_LONG = 1406;
    private static final int DOUBLON = 1062;
    private static final int LIGNE_REFERENCEE = 1451;
    private static final int REFERENCE_INEXISTANTE = 1452;
    private static final int CONTRAINTE_CHECK = 3819;

    private MessageErreur() {
    }

    /**
     * Affiche une boîte de dialogue d'erreur et trace l'exception dans les logs.
     *
     * @param parent fenêtre parente de la boîte de dialogue
     * @param action ce que l'utilisateur essayait de faire (ex : "Ajout de l'agent")
     * @param e      l'exception interceptée
     */
    public static void afficher(Component parent, String action, SQLException e) {
        logger.log(java.util.logging.Level.WARNING, action, e);
        JOptionPane.showMessageDialog(parent, action + " impossible.\n" + traduire(e),
                "Erreur base de données", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Affiche un simple message d'erreur de saisie (validation).
     */
    public static void saisie(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Saisie incorrecte", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Traduit une SQLException en message lisible à partir de son code
     * d'erreur MySQL ou de son SQLState.
     */
    public static String traduire(SQLException e) {
        String etat = e.getSQLState();
        // SQLState "08xxx" : problème de connexion (serveur arrêté, mauvais port...)
        if (etat != null && etat.startsWith("08")) {
            return "Connexion à la base impossible : vérifiez que le serveur MySQL est démarré.";
        }
        switch (e.getErrorCode()) {
            case ACCES_REFUSE:
                return "Accès refusé : vérifiez l'utilisateur et le mot de passe dans ConnexionBDD.";
            case BASE_INCONNUE:
                return "La base gestion_missions n'existe pas : exécutez le script sql/gestion_missions.sql.";
            case TABLE_INCONNUE:
                return "Une table est manquante : exécutez le script sql/gestion_missions.sql.";
            case TEXTE_TROP_LONG:
                return "Un des textes saisis est trop long.";
            case DOUBLON:
                return "Cet enregistrement existe déjà (doublon).";
            case LIGNE_REFERENCEE:
                return "Cet élément est encore utilisé par d'autres données.";
            case REFERENCE_INEXISTANTE:
                return "L'agent ou la mission concerné(e) n'existe plus.";
            case CONTRAINTE_CHECK:
                return "Une règle de la base n'est pas respectée (dates ou budget).";
            default:
                return "Détail : " + e.getMessage();
        }
    }
}
