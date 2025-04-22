package tn.esprit.Pidev.Services;

import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionService {
    private final DatabaseConnection dbConnection;

    public PrescriptionService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Prescription> getAllPrescriptions() {
        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM prescriptions WHERE archived = false ORDER BY date_deb DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Prescription prescription = new Prescription(
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getTimestamp("date_deb").toLocalDateTime(),
                        rs.getTimestamp("date_fin").toLocalDateTime(),
                        rs.getString("adresse"),
                        rs.getString("gmail"),
                        rs.getString("statut"),
                        rs.getBoolean("archived")
                );
                prescriptions.add(prescription);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des prescriptions : " + e.getMessage(), e);
        }
        return prescriptions;
    }

    public void addPrescription(Prescription prescription) {
        String query = "INSERT INTO prescriptions (patient_id, date_deb, date_fin, adresse, gmail, statut, archived) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, prescription.getPatientId());
            stmt.setTimestamp(2, Timestamp.valueOf(prescription.getDateDeb()));
            stmt.setTimestamp(3, Timestamp.valueOf(prescription.getDateFin()));
            stmt.setString(4, prescription.getAdresse());
            stmt.setString(5, prescription.getGmail());
            stmt.setString(6, prescription.getStatut());
            stmt.setBoolean(7, prescription.isArchived());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la prescription a échoué");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    prescription.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la prescription a échoué, aucun ID obtenu");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la prescription : " + e.getMessage(), e);
        }
    }

    public void updatePrescription(Prescription prescription) {
        String query = "UPDATE prescriptions SET patient_id = ?, date_deb = ?, date_fin = ?, adresse = ?, gmail = ?, statut = ?, archived = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, prescription.getPatientId());
            stmt.setTimestamp(2, Timestamp.valueOf(prescription.getDateDeb()));
            stmt.setTimestamp(3, Timestamp.valueOf(prescription.getDateFin()));
            stmt.setString(4, prescription.getAdresse());
            stmt.setString(5, prescription.getGmail());
            stmt.setString(6, prescription.getStatut());
            stmt.setBoolean(7, prescription.isArchived());
            stmt.setInt(8, prescription.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La mise à jour de la prescription a échoué, aucune ligne modifiée");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la prescription : " + e.getMessage(), e);
        }
    }

    public void deletePrescription(int id) {
        String deleteTraitements = "DELETE FROM traitement WHERE id_p = ?";
        String deletePrescription = "DELETE FROM prescriptions WHERE id = ?";

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(deleteTraitements)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(deletePrescription)) {
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La suppression de la prescription a échoué, prescription non trouvée");
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException("Erreur lors du rollback : " + ex.getMessage(), ex);
                }
            }
            throw new RuntimeException("Erreur lors de la suppression : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Prescription> getPrescriptionsByPatient(int patientId, boolean includeArchived) {
        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM prescriptions WHERE patient_id = ? " +
                (includeArchived ? "" : "AND archived = false");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prescription prescription = new Prescription(
                            rs.getInt("id"),
                            rs.getInt("patient_id"),
                            rs.getTimestamp("date_deb").toLocalDateTime(),
                            rs.getTimestamp("date_fin").toLocalDateTime(),
                            rs.getString("adresse"),
                            rs.getString("gmail"),
                            rs.getString("statut"),
                            rs.getBoolean("archived")
                    );
                    prescriptions.add(prescription);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des prescriptions : " + e.getMessage(), e);
        }
        return prescriptions;
    }

    public boolean hasDateConflict(int patientId, LocalDateTime dateDeb, LocalDateTime dateFin) {
        String query = "SELECT COUNT(*) FROM prescriptions WHERE patient_id = ? AND archived = false " +
                "AND ((date_deb <= ? AND date_fin >= ?) OR (date_deb <= ? AND date_fin >= ?))";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            stmt.setTimestamp(2, Timestamp.valueOf(dateFin));
            stmt.setTimestamp(3, Timestamp.valueOf(dateDeb));
            stmt.setTimestamp(4, Timestamp.valueOf(dateDeb));
            stmt.setTimestamp(5, Timestamp.valueOf(dateFin));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la vérification des conflits de dates : " + e.getMessage(), e);
        }
        return false;
    }

    public void exportPrescriptionsToCSV(String filePath) {
        String query = "SELECT * FROM prescriptions ORDER BY date_deb DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery();
             FileWriter writer = new FileWriter(filePath)) {

            // En-têtes CSV
            writer.write("ID,Patient ID,Date Début,Date Fin,Adresse,Gmail,Statut,Archivé\n");

            while (rs.next()) {
                writer.write(String.format("%d,%d,%s,%s,%s,%s,%s,%b\n",
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getTimestamp("date_deb").toLocalDateTime(),
                        rs.getTimestamp("date_fin").toLocalDateTime(),
                        escapeCsv(rs.getString("adresse")),
                        escapeCsv(rs.getString("gmail")),
                        escapeCsv(rs.getString("statut")),
                        rs.getBoolean("archived")
                ));
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Erreur lors de l'exportation des prescriptions en CSV : " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public void archivePrescription(int id) {
        String query = "UPDATE prescriptions SET statut = 'Archivé', archived = true WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'archivage de la prescription a échoué, prescription non trouvée");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'archivage de la prescription : " + e.getMessage(), e);
        }
    }
}
