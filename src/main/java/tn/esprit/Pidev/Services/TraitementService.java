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
import com.opencsv.CSVWriter;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.Pidev.Models.Traitement;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service class for managing Traitement entities.
 * Provides methods for CRUD operations, OCR processing, searching, statistics, and exporting treatments.
 */
public class TraitementService {
    private static final Logger logger = LoggerFactory.getLogger(TraitementService.class);
    private final DatabaseConnection dbConnection;
    private final Map<Integer, Traitement> cache;
    private static final int CACHE_SIZE = 100;
    private final Tesseract tesseract;
    private boolean isOcrEnabled = true;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Constructs a new TraitementService with database connection and Tesseract OCR setup.
     */
    public TraitementService() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.cache = new ConcurrentHashMap<>();
        this.tesseract = new Tesseract();

        try {
            // Determine tessdata path dynamically
            String tessdataPath = determineTessdataPath();
            File tessdataDir = new File(tessdataPath);
            File fraTrainedData = new File(tessdataPath, "fra.traineddata");

            // Validate tessdata directory
            if (!tessdataDir.exists() || !tessdataDir.isDirectory()) {
                logger.error("tessdata directory does not exist or is not a directory: {}. Please ensure Tesseract-OCR is installed correctly.", tessdataPath);
                isOcrEnabled = false;
            } else {
                logger.info("tessdata directory found: {}", tessdataPath);
            }

            // Validate fra.traineddata file
            if (isOcrEnabled && !fraTrainedData.exists()) {
                logger.error("fra.traineddata file not found in tessdata directory: {}. Please download the French language data file.", fraTrainedData.getAbsolutePath());
                isOcrEnabled = false;
            } else if (isOcrEnabled) {
                logger.info("fra.traineddata file found: {}", fraTrainedData.getAbsolutePath());
            }

            // Configure Tesseract
            if (isOcrEnabled) {
                tesseract.setDatapath(tessdataPath);
                tesseract.setLanguage("fra");
                logger.info("Tesseract configured with tessdata path: {}", tessdataPath);

                // Log Tesseract4J library version
                Package pkg = Tesseract.class.getPackage();
                String version = (pkg != null && pkg.getImplementationVersion() != null)
                        ? pkg.getImplementationVersion()
                        : "unknown";
                logger.info("Using Tesseract4J library version: {}", version);
            } else {
                logger.warn("OCR functionality is disabled due to missing or incorrect Tesseract components.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error during Tesseract initialization: {}. OCR functionality will be disabled.", e.getMessage(), e);
            isOcrEnabled = false;
        }
    }

    /**
     * Determines the tessdata path for Tesseract OCR.
     *
     * @return The path to the tessdata directory.
     */
    private String determineTessdataPath() {
        // Check TESSDATA_PREFIX environment variable first
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix != null && !tessdataPrefix.isEmpty()) {
            File tessdataDir = new File(tessdataPrefix);
            if (tessdataDir.exists() && tessdataDir.isDirectory()) {
                logger.info("Using TESSDATA_PREFIX environment variable for tessdata path: {}", tessdataPrefix);
                return tessdataPrefix;
            } else {
                logger.warn("TESSDATA_PREFIX environment variable set to {}, but directory does not exist or is not a directory.", tessdataPrefix);
            }
        }

        // Fallback to common Tesseract installation paths
        String[] possiblePaths = {
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                "/usr/share/tessdata",  // Common path for Linux
                "/usr/share/tesseract-ocr/5/tessdata", // Alternative Linux path
                "/usr/local/share/tessdata"  // Common path for macOS
        };

        for (String path : possiblePaths) {
            File tessdataDir = new File(path);
            if (tessdataDir.exists() && tessdataDir.isDirectory()) {
                logger.info("Found tessdata directory at: {}", path);
                return path;
            } else {
                logger.debug("Checked tessdata path, but it does not exist: {}", path);
            }
        }

        // Default to a common path and let validation fail
        String defaultPath = "C:\\Program Files\\Tesseract-OCR\\tessdata";
        logger.warn("No valid tessdata directory found. Defaulting to: {}. Please ensure Tesseract-OCR is installed at this location.", defaultPath);
        return defaultPath;
    }

    /**
     * Performs OCR on a PDF or image file to extract text.
     *
     * @param filePath The path to the file to process.
     * @return The extracted text.
     * @throws UnsupportedOperationException If OCR is disabled.
     * @throws IllegalArgumentException If the file does not exist.
     * @throws RuntimeException If an error occurs during OCR processing.
     */
    public String performOCR(String filePath) {
        if (!isOcrEnabled) {
            logger.warn("OCR functionality is disabled because Tesseract could not be initialized.");
            String errorMessage = "Erreur lors de l'extraction OCR : OCR est désactivé.\n" +
                    "Assurez-vous que Tesseract-OCR est installé correctement et que 'fra.traineddata' est présent dans le dossier 'tessdata'.\n" +
                    "Vérifiez également que Tesseract est ajouté au PATH système.";
            throw new UnsupportedOperationException(errorMessage);
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            logger.error("File path is null or empty");
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être vide");
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.error("File does not exist: {}", filePath);
                throw new IllegalArgumentException("Le fichier n'existe pas : " + filePath);
            }

            logger.info("Starting OCR process on file: {}", filePath);

            if (filePath.toLowerCase().endsWith(".pdf")) {
                logger.info("Processing PDF file with OCR: {}", filePath);
                try (PDDocument document = PDDocument.load(file)) {
                    if (document.getNumberOfPages() == 0) {
                        logger.error("PDF file has no pages: {}", filePath);
                        throw new IllegalArgumentException("Le PDF ne contient aucune page");
                    }

                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    StringBuilder extractedText = new StringBuilder();

                    int totalPages = document.getNumberOfPages();
                    for (int page = 0; page < totalPages; page++) {
                        logger.info("Processing page {} of {} in PDF", page + 1, totalPages);
                        java.awt.image.BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                        String pageText = tesseract.doOCR(image);
                        logger.debug("Text extracted from page {}: {}", page + 1, pageText.substring(0, Math.min(100, pageText.length())));
                        extractedText.append(pageText).append("\n");

                        // Free memory by setting the image to null
                        image.flush();
                    }

                    logger.info("OCR completed successfully on PDF with {} pages", totalPages);
                    return extractedText.toString();
                }
            } else {
                logger.info("Processing image file with OCR: {}", filePath);
                String result = tesseract.doOCR(file);
                logger.info("OCR completed successfully on image file: {}", filePath);
                return result;
            }
        } catch (TesseractException e) {
            logger.error("Tesseract error during OCR: {}", e.getMessage(), e);
            isOcrEnabled = false;
            throw new RuntimeException("Erreur Tesseract lors de l'OCR. Vérifiez que Tesseract est installé correctement : " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            logger.error("File not found for OCR: {}", filePath, e);
            throw new IllegalArgumentException("Le fichier est introuvable : " + filePath, e);
        } catch (IOException e) {
            logger.error("Error reading file for OCR: {}", filePath, e);
            throw new RuntimeException("Erreur lors de la lecture du fichier pour OCR : " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during OCR: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de l'OCR : " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all treatments from the database.
     *
     * @return A list of Traitement objects.
     * @throws RuntimeException If a database error occurs.
     */
    public List<Traitement> getAllTraitements() {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Traitement traitement = mapResultSetToTraitement(rs);
                traitements.add(traitement);
                updateCache(traitement);
            }
            logger.info("Retrieved {} treatments from the database", traitements.size());
            return traitements;

        } catch (SQLException e) {
            logger.error("Error retrieving treatments: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des traitements : " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves treatments associated with a specific prescription.
     *
     * @param prescriptionId The ID of the prescription.
     * @param includeArchived Whether to include archived treatments.
     * @return A list of Traitement objects.
     * @throws RuntimeException If a database error occurs.
     */
    public List<Traitement> getTraitementsByPrescription(int prescriptionId, boolean includeArchived) {
        if (prescriptionId <= 0) {
            throw new IllegalArgumentException("L'ID de la prescription doit être positif : " + prescriptionId);
        }

        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement WHERE id_p = ?" + (includeArchived ? "" : " AND archived = FALSE");

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prescriptionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Traitement traitement = mapResultSetToTraitement(rs);
                    traitements.add(traitement);
                    updateCache(traitement);
                }
                logger.info("Retrieved {} treatments for prescription ID {} (includeArchived={})", traitements.size(), prescriptionId, includeArchived);
                return traitements;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving treatments for prescription ID {}: {}", prescriptionId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la récupération des traitements : " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new treatment to the database.
     *
     * @param traitement The Traitement object to add.
     * @throws IllegalArgumentException If the treatment is invalid.
     * @throws RuntimeException If a database error occurs.
     */
    public void addTraitement(Traitement traitement) {
        validateTraitement(traitement, false);

        String sql = "INSERT INTO traitement (id_p, medicament, dose, description, statut, date, completed, archived) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, traitement.getId_p());
            stmt.setString(2, traitement.getMedicament());
            stmt.setString(3, traitement.getDose());
            stmt.setString(4, traitement.getDescription());
            stmt.setString(5, traitement.getStatut());
            stmt.setDate(6, Date.valueOf(traitement.getDate()));
            stmt.setBoolean(7, traitement.isCompleted());
            stmt.setBoolean(8, traitement.isArchived());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du traitement a échoué, aucune ligne insérée");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    traitement.setId(generatedKeys.getInt(1));
                    updateCache(traitement);
                    logger.info("Treatment added successfully, ID: {}", traitement.getId());
                } else {
                    throw new SQLException("La création du traitement a échoué, aucun ID obtenu");
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding treatment for prescription ID {}: {}", traitement.getId_p(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'ajout du traitement : " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing treatment in the database.
     *
     * @param traitement The Traitement object to update.
     * @throws IllegalArgumentException If the treatment is invalid.
     * @throws RuntimeException If a database error occurs.
     */
    public void updateTraitement(Traitement traitement) {
        validateTraitement(traitement, true);

        String sql = "UPDATE traitement SET id_p = ?, medicament = ?, dose = ?, description = ?, " +
                "statut = ?, date = ?, completed = ?, archived = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, traitement.getId_p());
            stmt.setString(2, traitement.getMedicament());
            stmt.setString(3, traitement.getDose());
            stmt.setString(4, traitement.getDescription());
            stmt.setString(5, traitement.getStatut());
            stmt.setDate(6, Date.valueOf(traitement.getDate()));
            stmt.setBoolean(7, traitement.isCompleted());
            stmt.setBoolean(8, traitement.isArchived());
            stmt.setInt(9, traitement.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La mise à jour du traitement a échoué, aucun traitement trouvé pour l'ID " + traitement.getId());
            }

            updateCache(traitement);
            logger.info("Treatment updated successfully, ID: {}", traitement.getId());

        } catch (SQLException e) {
            logger.error("Error updating treatment with ID {}: {}", traitement.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la mise à jour du traitement : " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a treatment from the database.
     *
     * @param id The ID of the treatment to delete.
     * @throws IllegalArgumentException If the ID is invalid.
     * @throws RuntimeException If a database error occurs.
     */
    public void deleteTraitement(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du traitement doit être positif : " + id);
        }

        String sql = "DELETE FROM traitement WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La suppression du traitement a échoué, aucun traitement trouvé pour l'ID " + id);
            }
            cache.remove(id);
            logger.info("Treatment deleted successfully, ID: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting treatment with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la suppression du traitement : " + e.getMessage(), e);
        }
    }

    /**
     * Archives a treatment by setting its archived flag to true.
     *
     * @param id The ID of the treatment to archive.
     * @throws IllegalArgumentException If the ID is invalid.
     * @throws RuntimeException If a database error occurs.
     */
    public void archiveTraitement(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du traitement doit être positif : " + id);
        }

        String sql = "UPDATE traitement SET archived = TRUE WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'archivage du traitement a échoué, aucun traitement trouvé pour l'ID " + id);
            }
            cache.computeIfPresent(id, (key, traitement) -> {
                traitement.setArchived(true);
                return traitement;
            });
            logger.info("Treatment archived successfully, ID: {}", id);
        } catch (SQLException e) {
            logger.error("Error archiving treatment with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'archivage du traitement : " + e.getMessage(), e);
        }
    }

    /**
     * Searches for treatments based on a keyword and optional filters.
     *
     * @param keyword The keyword to search for in medicament, description, or statut.
     * @param filters A map of filters (e.g., "statut" to filter by status).
     * @return A list of matching Traitement objects.
     */
    public List<Traitement> searchTraitements(String keyword, Map<String, String> filters) {
        if (keyword == null) {
            keyword = "";
        }
        if (filters == null) {
            filters = new HashMap<>();
        }

        List<Traitement> allTraitements = getAllTraitements();
        String searchLower = keyword.trim().toLowerCase();
        String filterType = filters.getOrDefault("statut", "");

        return allTraitements.stream()
                .filter(t -> {
                    boolean matches = true;
                    if (!searchLower.isEmpty()) {
                        matches = (t.getMedicament() != null && t.getMedicament().toLowerCase().contains(searchLower)) ||
                                (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchLower)) ||
                                (t.getStatut() != null && t.getStatut().toLowerCase().contains(searchLower));
                    }
                    if (!filterType.isEmpty()) {
                        matches = matches && t.getStatut() != null && t.getStatut().equalsIgnoreCase(filterType);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed statistics about treatments.
     *
     * @return A map containing statistics such as monthly distribution and medication frequency.
     */
    public Map<String, Object> getDetailedStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Traitement> traitements = getAllTraitements();

        Map<String, Long> monthlyDistribution = traitements.stream()
                .filter(t -> t.getDate() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().format(MONTH_FORMATTER),
                        Collectors.counting()
                ));
        stats.put("monthly_distribution", monthlyDistribution);

        Map<String, Long> medicationFrequency = traitements.stream()
                .filter(t -> t.getMedicament() != null)
                .collect(Collectors.groupingBy(
                        Traitement::getMedicament,
                        Collectors.counting()
                ));
        stats.put("medication_frequency", medicationFrequency);

        return stats;
    }

    /**
     * Exports treatments to the specified format (PDF or CSV).
     *
     * @param format The export format ("pdf" or "csv").
     * @param filePath The path to save the exported file.
     * @param progressCallback A callback to report export progress.
     * @throws IllegalArgumentException If the format or file path is invalid.
     * @throws IllegalStateException If there are no treatments to export.
     * @throws RuntimeException If an error occurs during export.
     */
    public void exportTraitements(String format, String filePath, Consumer<Double> progressCallback) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Le format d'exportation ne peut pas être vide");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être vide");
        }
        if (progressCallback == null) {
            progressCallback = progress -> {};
        }

        List<Traitement> traitements = getAllTraitements();
        if (traitements.isEmpty()) {
            throw new IllegalStateException("Aucun traitement à exporter");
        }

        switch (format.toLowerCase()) {
            case "pdf":
                exportTraitementsToPDF(traitements, filePath, progressCallback);
                break;
            case "csv":
                exportTraitementsToCSV(traitements, filePath, progressCallback);
                break;
            default:
                throw new IllegalArgumentException("Format d'exportation non supporté : " + format);
        }
    }

    /**
     * Exports treatments to a PDF file.
     *
     * @param traitements The list of treatments to export.
     * @param filePath The path to save the PDF file.
     * @param progressCallback A callback to report export progress.
     * @throws RuntimeException If an error occurs during export.
     */
    private void exportTraitementsToPDF(List<Traitement> traitements, String filePath, Consumer<Double> progressCallback) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Liste des Traitements")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            float[] columnWidths = {1, 1, 2, 2, 3, 2, 2, 1};
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();
            String[] headers = {"ID", "Prescription ID", "Médicament", "Dose", "Description", "Statut", "Date", "Archivé"};
            for (String header : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }

            for (int i = 0; i < traitements.size(); i++) {
                Traitement t = traitements.get(i);
                table.addCell(String.valueOf(t.getId()));
                table.addCell(String.valueOf(t.getId_p()));
                table.addCell(t.getMedicament() != null ? t.getMedicament() : "N/A");
                table.addCell(t.getDose() != null ? t.getDose() : "N/A");
                table.addCell(t.getDescription() != null ? t.getDescription() : "N/A");
                table.addCell(t.getStatut() != null ? t.getStatut() : "N/A");
                table.addCell(t.getDate() != null ? t.getDate().toString() : "N/A");
                table.addCell(t.isArchived() ? "Oui" : "Non");

                double progress = (double) (i + 1) / traitements.size();
                progressCallback.accept(progress);
            }

            document.add(table);
            logger.info("Treatments exported to PDF: {}", filePath);
        } catch (IOException e) {
            logger.error("Error exporting treatments to PDF at {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'exportation des traitements en PDF : " + e.getMessage(), e);
        }
    }

    /**
     * Exports treatments to a CSV file.
     *
     * @param traitements The list of treatments to export.
     * @param filePath The path to save the CSV file.
     * @param progressCallback A callback to report export progress.
     * @throws RuntimeException If an error occurs during export.
     */
    private void exportTraitementsToCSV(List<Traitement> traitements, String filePath, Consumer<Double> progressCallback) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] headers = {"ID", "Prescription ID", "Médicament", "Dose", "Description", "Statut", "Date", "Archivé"};
            writer.writeNext(headers);

            for (int i = 0; i < traitements.size(); i++) {
                Traitement t = traitements.get(i);
                String[] row = {
                        String.valueOf(t.getId()),
                        String.valueOf(t.getId_p()),
                        t.getMedicament() != null ? t.getMedicament() : "",
                        t.getDose() != null ? t.getDose() : "",
                        t.getDescription() != null ? t.getDescription() : "",
                        t.getStatut() != null ? t.getStatut() : "",
                        t.getDate() != null ? t.getDate().toString() : "",
                        String.valueOf(t.isArchived())
                };
                writer.writeNext(row);

                double progress = (double) (i + 1) / traitements.size();
                progressCallback.accept(progress);
            }
            logger.info("Treatments exported to CSV: {}", filePath);
        } catch (IOException e) {
            logger.error("Error exporting treatments to CSV at {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'exportation des traitements en CSV : " + e.getMessage(), e);
        }
    }

    /**
     * Maps a ResultSet row to a Traitement object.
     *
     * @param rs The ResultSet containing the treatment data.
     * @return A Traitement object.
     * @throws SQLException If a database access error occurs.
     */
    private Traitement mapResultSetToTraitement(ResultSet rs) throws SQLException {
        Objects.requireNonNull(rs, "ResultSet cannot be null");

        LocalDate date = rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null;
        if (date == null) {
            throw new SQLException("Le champ 'date' ne peut pas être nul dans la base de données pour l'ID " + rs.getInt("id"));
        }

        return new Traitement(
                rs.getInt("id"),
                rs.getInt("id_p"),
                rs.getString("medicament"),
                rs.getString("dose"),
                rs.getString("description"),
                rs.getString("statut"),
                date,
                rs.getBoolean("completed"),
                rs.getBoolean("archived")
        );
    }

    /**
     * Validates a Traitement object before database operations.
     *
     * @param traitement The Traitement to validate.
     * @param isUpdate Whether this is an update operation (requires ID check).
     * @throws IllegalArgumentException If the treatment is invalid.
     */
    private void validateTraitement(Traitement traitement, boolean isUpdate) {
        if (traitement == null) {
            throw new IllegalArgumentException("Le traitement ne peut pas être nul");
        }
        if (isUpdate && traitement.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du traitement doit être positif pour une mise à jour : " + traitement.getId());
        }
        if (traitement.getId_p() <= 0) {
            throw new IllegalArgumentException("L'ID de la prescription doit être positif : " + traitement.getId_p());
        }
        if (traitement.getMedicament() == null || traitement.getMedicament().trim().isEmpty()) {
            throw new IllegalArgumentException("Le médicament ne peut pas être vide");
        }
        if (traitement.getDose() == null || traitement.getDose().trim().isEmpty()) {
            throw new IllegalArgumentException("La dose ne peut pas être vide");
        }
        if (traitement.getStatut() == null || traitement.getStatut().trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
        if (traitement.getDate() == null) {
            throw new IllegalArgumentException("La date ne peut pas être nulle");
        }
        if (traitement.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date du traitement ne peut pas être dans le passé : " + traitement.getDate());
        }
    }

    /**
     * Updates the cache with the given treatment, maintaining the cache size limit.
     *
     * @param traitement The Traitement to add or update in the cache.
     */
    private void updateCache(Traitement traitement) {
        if (cache.size() >= CACHE_SIZE) {
            // Simple eviction: remove the first entry
            Iterator<Map.Entry<Integer, Traitement>> iterator = cache.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        cache.put(traitement.getId(), traitement);
    }
}