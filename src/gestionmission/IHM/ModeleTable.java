package gestionmission.IHM;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Modèle de JTable en lecture seule, utilisé par les nouvelles fenêtres.
 * <ul>
 *   <li>les cellules ne sont pas modifiables directement ;</li>
 *   <li>le type de chaque colonne est déduit des valeurs : les nombres et
 *       les dates sont triés correctement et les booléens s'affichent sous
 *       forme de case à cocher.</li>
 * </ul>
 */
public class ModeleTable extends DefaultTableModel {

    public ModeleTable(String... colonnes) {
        super(colonnes, 0);
    }

    @Override
    public boolean isCellEditable(int ligne, int colonne) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int colonne) {
        for (int ligne = 0; ligne < getRowCount(); ligne++) {
            Object valeur = getValueAt(ligne, colonne);
            if (valeur != null) {
                return valeur.getClass();
            }
        }
        return Object.class;
    }

    /**
     * Applique le modèle à la table avec les réglages communs :
     * tri par clic sur l'en-tête et sélection d'une seule ligne.
     */
    public static void appliquer(JTable table, ModeleTable modele) {
        table.setModel(modele);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
    }
}
