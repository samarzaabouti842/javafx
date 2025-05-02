package tn.esprit.Pidev.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.Pidev.Models.Patient;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);
    private final DatabaseConnection dbConnection;

    public PatientService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Patient getPatientById(int id) {
        String sql = "SELECT * FROM patient WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Patient(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getDate("dateNaissance"),
                            rs.getString("email"),
                            rs.getString("telephone"),
                            rs.getString("adresse"),
                            rs.getString("genre")
                    );
                }
            }
            logger.debug("No patient found with id: {}", id);
            return null;

        } catch (SQLException e) {
            logger.error("Error retrieving patient with id: {}", id, e);
            throw new RuntimeException("Failed to retrieve patient", e);
        }
    }

    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patient";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getDate("dateNaissance"),
                        rs.getString("email"),
                        rs.getString("telephone"),
                        rs.getString("adresse"),
                        rs.getString("genre")
                );
                patients.add(patient);
            }
            logger.debug("Retrieved {} patients from database", patients.size());
            return patients;

        } catch (SQLException e) {
            logger.error("Error retrieving patients", e);
            throw new RuntimeException("Erreur lors de la récupération des patients : " + e.getMessage(), e);
        }
    }
}