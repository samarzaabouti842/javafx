package tn.esprit.Pidev.Services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service class for managing Prescription entities.
 * Provides methods for CRUD operations, date conflict checking, PDF export, and archiving.
 */
public class PrescriptionService {
    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);
    private final DatabaseConnection dbConnection;

    /**
     * Constructs a new PrescriptionService with a database connection.
     */
    public PrescriptionService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Retrieves all prescriptions from the database.
     *
     * @param includeArchived Whether to include archived prescriptions in the result.
     * @return A list of Prescription objects.
     * @throws RuntimeException if a database error occurs.
     */
    public List<Prescription> getAllPrescriptions(boolean includeArchived) {
        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM prescriptions" + (includeArchived ? "" : " WHERE archived = false") + " ORDER BY date_deb DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Prescription prescription = mapResultSetToPrescription(rs);
                prescriptions.add(prescription);
            }
            logger.info("Successfully retrieved {} prescriptions (includeArchived={})", prescriptions.size(), includeArchived);
            return prescriptions;
        } catch (SQLException e) {
            logger.error("Error retrieving prescriptions: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des prescriptions : " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new prescription to the database.
     *
     * @param prescription The Prescription object to add.
     * @throws IllegalArgumentException if the prescription is invalid.
     * @throws RuntimeException if a database error occurs.
     */
    public void addPrescription(Prescription prescription) {
        validatePrescription(prescription, false);

        String query = "INSERT INTO prescriptions (patient_id, date_deb, date_fin, adresse, gmail, statut, archived) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, prescription.getPatientId());
            stmt.setTimestamp(2, Timestamp.valueOf(prescription.getDateDeb().atStartOfDay()));
            stmt.setTimestamp(3, Timestamp.valueOf(prescription.getDateFin().atStartOfDay()));
            stmt.setString(4, prescription.getAdresse());
            stmt.setString(5, prescription.getGmail());
            stmt.setString(6, prescription.getStatut());
            stmt.setBoolean(7, prescription.isArchived());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la prescription a échoué, aucune ligne insérée");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    prescription.setId(generatedKeys.getInt(1));
                    logger.info("Successfully added prescription with ID: {}", prescription.getId());
                } else {
                    throw new SQLException("La création de la prescription a échoué, aucun ID obtenu");
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding prescription for patient ID {}: {}", prescription.getPatientId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'ajout de la prescription : " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing prescription in the database.
     *
     * @param prescription The Prescription object to update.
     * @throws IllegalArgumentException if the prescription is invalid.
     * @throws RuntimeException if a database error occurs.
     */
    public void updatePrescription(Prescription prescription) {
        validatePrescription(prescription, true);

        String query = "UPDATE prescriptions SET patient_id = ?, date_deb = ?, date_fin = ?, adresse = ?, gmail = ?, statut = ?, archived = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, prescription.getPatientId());
            stmt.setTimestamp(2, Timestamp.valueOf(prescription.getDateDeb().atStartOfDay()));
            stmt.setTimestamp(3, Timestamp.valueOf(prescription.getDateFin().atStartOfDay()));
            stmt.setString(4, prescription.getAdresse());
            stmt.setString(5, prescription.getGmail());
            stmt.setString(6, prescription.getStatut());
            stmt.setBoolean(7, prescription.isArchived());
            stmt.setInt(8, prescription.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La mise à jour de la prescription a échoué, prescription non trouvée pour l'ID " + prescription.getId());
            }
            logger.info("Successfully updated prescription with ID: {}", prescription.getId());
        } catch (SQLException e) {
            logger.error("Error updating prescription with ID {}: {}", prescription.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la mise à jour de la prescription : " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a prescription and its associated treatments from the database.
     *
     * @param id The ID of the prescription to delete.
     * @throws RuntimeException if a database error occurs.
     */
    public void deletePrescription(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la prescription doit être positif : " + id);
        }

        String deleteTraitements = "DELETE FROM traitement WHERE id_p = ?";
        String deletePrescription = "DELETE FROM prescriptions WHERE id = ?";

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(deleteTraitements)) {
                stmt.setInt(1, id);
                int treatmentsDeleted = stmt.executeUpdate();
                logger.debug("Deleted {} treatments associated with prescription ID: {}", treatmentsDeleted, id);
            }

            try (PreparedStatement stmt = conn.prepareStatement(deletePrescription)) {
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La suppression de la prescription a échoué, prescription non trouvée pour l'ID " + id);
                }
            }

            conn.commit();
            logger.info("Successfully deleted prescription with ID: {}", id);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Rolled back deletion of prescription with ID: {}", id);
                } catch (SQLException ex) {
                    logger.error("Error during rollback for prescription ID {}: {}", id, ex.getMessage(), ex);
                    throw new RuntimeException("Erreur lors du rollback : " + ex.getMessage(), ex);
                }
            }
            logger.error("Error deleting prescription with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la suppression de la prescription : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection after deletion of prescription ID {}: {}", id, e.getMessage(), e);
                    throw new RuntimeException("Erreur lors de la fermeture de la connexion : " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Retrieves all prescriptions for a specific patient.
     *
     * @param patientId The ID of the patient.
     * @param includeArchived Whether to include archived prescriptions.
     * @return A list of Prescription objects for the patient.
     * @throws RuntimeException if a database error occurs.
     */
    public List<Prescription> getPrescriptionsByPatient(int patientId, boolean includeArchived) {
        if (patientId <= 0) {
            throw new IllegalArgumentException("L'ID du patient doit être positif : " + patientId);
        }

        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT * FROM prescriptions WHERE patient_id = ? " +
                (includeArchived ? "" : "AND archived = false");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prescription prescription = mapResultSetToPrescription(rs);
                    prescriptions.add(prescription);
                }
            }
            logger.info("Successfully retrieved {} prescriptions for patient ID: {} (includeArchived={})",
                    prescriptions.size(), patientId, includeArchived);
            return prescriptions;
        } catch (SQLException e) {
            logger.error("Error retrieving prescriptions for patient ID {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des prescriptions pour le patient : " + e.getMessage(), e);
        }
    }

    /**
     * Checks if there is a date conflict for a patient between the given dates.
     *
     * @param patientId The ID of the patient.
     * @param dateDeb The start date of the prescription.
     * @param dateFin The end date of the prescription.
     * @return True if there is a conflict, false otherwise.
     * @throws RuntimeException if a database error occurs.
     */
    public boolean hasDateConflict(int patientId, LocalDateTime dateDeb, LocalDateTime dateFin) {
        if (patientId <= 0) {
            throw new IllegalArgumentException("L'ID du patient doit être positif : " + patientId);
        }
        if (dateDeb == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin ne doivent pas être nulles");
        }
        if (dateDeb.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin");
        }

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
                    boolean hasConflict = rs.getInt(1) > 0;
                    logger.info("Date conflict check for patient ID {} between {} and {}: {}", patientId, dateDeb, dateFin, hasConflict);
                    return hasConflict;
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error checking date conflict for patient ID {} between {} and {}: {}", patientId, dateDeb, dateFin, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la vérification des conflits de dates : " + e.getMessage(), e);
        }
    }

    /**
     * Exports all prescriptions to a PDF file.
     *
     * @param filePath The path where the PDF file will be saved.
     * @throws RuntimeException if an error occurs during export.
     */
    public void exportPrescriptionsToPDF(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du fichier PDF ne peut pas être vide");
        }

        String query = "SELECT * FROM prescriptions ORDER BY date_deb DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add title
            document.add(new Paragraph("Liste des Prescriptions")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Create table with 8 columns
            float[] columnWidths = {1, 1, 2, 2, 2, 2, 1, 1};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            // Style table headers
            String[] headers = {"ID", "Patient ID", "Date Début", "Date Fin", "Adresse", "Gmail", "Statut", "Archivé"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(headerCell);
            }

            // Add table rows
            while (rs.next()) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(rs.getInt("id")))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(rs.getInt("patient_id")))));
                table.addCell(new Cell().add(new Paragraph(rs.getTimestamp("date_deb").toLocalDateTime().toLocalDate().toString())));
                table.addCell(new Cell().add(new Paragraph(rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate().toString())));
                table.addCell(new Cell().add(new Paragraph(rs.getString("adresse") != null ? rs.getString("adresse") : "N/A")));
                table.addCell(new Cell().add(new Paragraph(rs.getString("gmail") != null ? rs.getString("gmail") : "N/A")));
                table.addCell(new Cell().add(new Paragraph(rs.getString("statut") != null ? rs.getString("statut") : "N/A")));
                table.addCell(new Cell().add(new Paragraph(rs.getBoolean("archived") ? "Oui" : "Non")));
            }

            document.add(table);

            // Add footer
            document.add(new Paragraph("\nExporté le : " + LocalDate.now().toString())
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();
            logger.info("Successfully exported prescriptions to PDF at: {}", filePath);

        } catch (SQLException e) {
            logger.error("Error retrieving data for PDF export: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des données pour l'export PDF : " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error creating PDF file at {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création du fichier PDF : " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error exporting prescriptions to PDF at {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de l'exportation des prescriptions en PDF : " + e.getMessage(), e);
        }
    }

    /**
     * Archives a prescription by setting its status to 'Archivé' and archived flag to true.
     *
     * @param id The ID of the prescription to archive.
     * @throws RuntimeException if a database error occurs.
     */
    public void archivePrescription(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la prescription doit être positif : " + id);
        }

        String query = "UPDATE prescriptions SET statut = 'Archivé', archived = true WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'archivage de la prescription a échoué, prescription non trouvée pour l'ID " + id);
            }
            logger.info("Successfully archived prescription with ID: {}", id);
        } catch (SQLException e) {
            logger.error("Error archiving prescription with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'archivage de la prescription : " + e.getMessage(), e);
        }
    }

    /**
     * Validates a Prescription object before database operations.
     *
     * @param prescription The Prescription to validate.
     * @param isUpdate Whether this is an update operation (requires ID check).
     * @throws IllegalArgumentException if the prescription is invalid.
     */
    private void validatePrescription(Prescription prescription, boolean isUpdate) {
        if (prescription == null) {
            throw new IllegalArgumentException("La prescription ne peut pas être nulle");
        }
        if (isUpdate && prescription.getId() <= 0) {
            throw new IllegalArgumentException("L'ID de la prescription doit être positif pour une mise à jour : " + prescription.getId());
        }
        if (prescription.getPatientId() <= 0) {
            throw new IllegalArgumentException("L'ID du patient doit être positif : " + prescription.getPatientId());
        }
        if (prescription.getDateDeb() == null || prescription.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin ne doivent pas être nulles");
        }
        if (prescription.getDateDeb().isAfter(prescription.getDateFin())) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin");
        }
        if (prescription.getAdresse() == null || prescription.getAdresse().trim().isEmpty()) {
            throw new IllegalArgumentException("L'adresse ne peut pas être vide");
        }
        if (prescription.getGmail() == null || prescription.getGmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Le Gmail ne peut pas être vide");
        }
        if (!isValidEmail(prescription.getGmail())) {
            throw new IllegalArgumentException("L'adresse Gmail n'est pas valide : " + prescription.getGmail());
        }
        if (prescription.getStatut() == null || prescription.getStatut().trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
    }

    /**
     * Validates an email address using a simple regex pattern.
     *
     * @param email The email address to validate.
     * @return True if the email is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Maps a ResultSet row to a Prescription object.
     *
     * @param rs The ResultSet containing the prescription data.
     * @return A Prescription object.
     * @throws SQLException if a database access error occurs.
     */
    private Prescription mapResultSetToPrescription(ResultSet rs) throws SQLException {
        Objects.requireNonNull(rs, "ResultSet cannot be null");

        LocalDate dateDeb = rs.getTimestamp("date_deb") != null ?
                rs.getTimestamp("date_deb").toLocalDateTime().toLocalDate() : null;
        LocalDate dateFin = rs.getTimestamp("date_fin") != null ?
                rs.getTimestamp("date_fin").toLocalDateTime().toLocalDate() : null;

        if (dateDeb == null || dateFin == null) {
            throw new SQLException("Les champs date_deb et date_fin ne peuvent pas être nuls dans la base de données pour l'ID " + rs.getInt("id"));
        }

        return new Prescription(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                dateDeb,
                dateFin,
                rs.getString("adresse"),
                rs.getString("gmail"),
                rs.getString("statut"),
                rs.getBoolean("archived")
        );
    }
}