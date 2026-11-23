/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gestionmission.DAO;

import gestionmission.Connexion.ConnexionBDD;
import gestionmission.Metier.Mission;
import gestionmission.Metier.Priorite;
import gestionmission.Metier.Statut;
import java.util.ArrayList;
import java.sql.*;

/**
 *
 * @author matteo.douteaud
 */
public class MissionDAO {
    
    public ArrayList<Mission> lister() throws SQLException{
        ArrayList<Mission> lesMissions = new ArrayList<Mission>();
        
        String sql = "SELECT * FROM mission ORDER BY date_debut, titre";
        try (
                Connection connexion = ConnexionBDD.obtenirConnexion();
                PreparedStatement requete = connexion.prepareStatement(sql);
                ResultSet lesResultats = requete.executeQuery();
           ) {
               
                while (lesResultats.next()) {
                    Mission mission = construireMission(lesResultats);
                    lesMissions.add(mission);
                }
        }
        return lesMissions;
        
        
    }
    
    public Mission trouverParId(int id) throws SQLException {
        String sql = "SELECT * FROM mission WHERE id = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
        requete.setInt(1, id);
        try (ResultSet resultat = requete.executeQuery()) {
            if (resultat.next()) {
                return construireMission(resultat);
            }
        }
    }
        return null;
    }
    
    public int ajouter(Mission uneMission) throws SQLException {
        String sql = "INSERT INTO mission(titre,description,date_debut,date_fin,statut,priorite,budget) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {
            
            renseignerRequete(requete, uneMission);
            int rep = requete.executeUpdate();
            
            ResultSet cles = requete.getGeneratedKeys(); 
            if (cles.next()){
                uneMission.setId(cles.getInt(1));
            }
            return rep;
        }
    }
    
    public boolean modifier(Mission uneMission) throws SQLException {
        String sql = "UPDATE mission SET titre = ?, description = ?, date_debut = ?, date_fin = ?, statut = ?, priorite = ?, budget = ? WHERE id = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            renseignerRequete(requete, uneMission); // paramètres 1 à 7
            requete.setInt(8, uneMission.getId());  // paramètre 8 : le WHERE
            return requete.executeUpdate() > 0;
        }
    }
    
    public boolean supprimer(Mission uneMission) throws SQLException{
        String sql = "DELETE FROM mission WHERE id = ?";
        try (
            Connection connexion = ConnexionBDD.obtenirConnexion();
            PreparedStatement requete = connexion.prepareStatement(sql);
        ) {
            requete.setInt(1, uneMission.getId());
            return requete.executeUpdate() > 0;
        }
    }
    
    private void renseignerRequete(PreparedStatement requete, Mission mission) throws SQLException {
        requete.setString(1, mission.getTitre());
        requete.setString(2, mission.getDescription());
        requete.setDate(3, Date.valueOf(mission.getDateDebut()));
        requete.setDate(4, Date.valueOf(mission.getDateFin()));
        requete.setString(5, mission.getStatut().name());
        requete.setString(6, mission.getPriorite().name());
        requete.setDouble(7, mission.getBudget());
    }
    
    /**
     * Construit une Mission à partir de la ligne courante du ResultSet.
     * Visibilité package (et static) : ParticipationDAO la réutilise pour
     * reconstruire la mission lue par jointure.
     */
    static Mission construireMission(ResultSet resultat) throws SQLException {
        Mission mission = new Mission();
        mission.setId(resultat.getInt("id"));
        mission.setTitre(resultat.getString("titre"));
        mission.setDescription(resultat.getString("description"));
        mission.setDateDebut(resultat.getDate("date_debut").toLocalDate());
        mission.setDateFin(resultat.getDate("date_fin").toLocalDate());
        mission.setStatut(Statut.valueOf(resultat.getString("statut")));
        mission.setPriorite(Priorite.valueOf(resultat.getString("priorite")));
        mission.setBudget(resultat.getDouble("budget"));
        return mission;
    }
}