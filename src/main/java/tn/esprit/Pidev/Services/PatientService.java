package tn.esprit.Pidev.Services;

import tn.esprit.Pidev.Models.Patient;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientService {
    private static final Logger LOGGER = Logger.getLogger(PatientService.class.getName());
    private final DatabaseConnection dbConnection;

    public PatientService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Patient getPatientById(int id) {
        String query = "SELECT * FROM patient WHERE id = ?";
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Patient patient = new Patient();
                    patient.setId(resultSet.getInt("id"));
                    patient.setNom(resultSet.getString("nom"));
                    patient.setPrenom(resultSet.getString("prenom"));
                    patient.setDateNaissance(resultSet.getDate("date_naissance"));
                    patient.setAdresse(resultSet.getString("adresse"));
                    patient.setTelephone(resultSet.getString("telephone"));
                    patient.setEmail(resultSet.getString("email"));
                    patient.setGenre(resultSet.getString("genre"));
                    return patient;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving patient with ID: " + id, e);
        }
        return null;
    }

    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        String query = "SELECT * FROM patient";
        
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            
            while (resultSet.next()) {
                Patient patient = new Patient();
                patient.setId(resultSet.getInt("id"));
                patient.setNom(resultSet.getString("nom"));
                patient.setPrenom(resultSet.getString("prenom"));
                patient.setDateNaissance(resultSet.getDate("date_naissance"));
                patient.setAdresse(resultSet.getString("adresse"));
                patient.setTelephone(resultSet.getString("telephone"));
                patient.setEmail(resultSet.getString("email"));
                patient.setGenre(resultSet.getString("genre"));
                patients.add(patient);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all patients", e);
        }
        return patients;
    }

    public void addPatient(Patient patient) {
        String query = "INSERT INTO patient (nom, prenom, date_naissance, adresse, telephone, email, genre) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, patient.getNom());
            preparedStatement.setString(2, patient.getPrenom());
            preparedStatement.setDate(3, new java.sql.Date(patient.getDateNaissance().getTime()));
            preparedStatement.setString(4, patient.getAdresse());
            preparedStatement.setString(5, patient.getTelephone());
            preparedStatement.setString(6, patient.getEmail());
            preparedStatement.setString(7, patient.getGenre());
            
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding patient", e);
        }
    }

    public void updatePatient(Patient patient) {
        String query = "UPDATE patient SET nom=?, prenom=?, date_naissance=?, adresse=?, telephone=?, email=?, genre=? " +
                      "WHERE id=?";
        
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query)) {
            preparedStatement.setString(1, patient.getNom());
            preparedStatement.setString(2, patient.getPrenom());
            preparedStatement.setDate(3, new java.sql.Date(patient.getDateNaissance().getTime()));
            preparedStatement.setString(4, patient.getAdresse());
            preparedStatement.setString(5, patient.getTelephone());
            preparedStatement.setString(6, patient.getEmail());
            preparedStatement.setString(7, patient.getGenre());
            preparedStatement.setInt(8, patient.getId());
            
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating patient with ID: " + patient.getId(), e);
        }
    }

    public void deletePatient(int id) {
        String query = "DELETE FROM patient WHERE id=?";
        
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting patient with ID: " + id, e);
        }
    }

    public List<Patient> searchPatients(String keyword) {
        List<Patient> patients = new ArrayList<>();
        String query = "SELECT * FROM patient WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ?";
        
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            preparedStatement.setString(1, searchPattern);
            preparedStatement.setString(2, searchPattern);
            preparedStatement.setString(3, searchPattern);
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Patient patient = new Patient();
                    patient.setId(resultSet.getInt("id"));
                    patient.setNom(resultSet.getString("nom"));
                    patient.setPrenom(resultSet.getString("prenom"));
                    patient.setDateNaissance(resultSet.getDate("date_naissance"));
                    patient.setAdresse(resultSet.getString("adresse"));
                    patient.setTelephone(resultSet.getString("telephone"));
                    patient.setEmail(resultSet.getString("email"));
                    patient.setGenre(resultSet.getString("genre"));
                    patients.add(patient);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching patients with keyword: " + keyword, e);
        }
        return patients;
    }
}
