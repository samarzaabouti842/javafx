package tn.esprit.Pidev.Services;

import tn.esprit.Pidev.Models.Traitement;
import tn.esprit.Pidev.utils.DatabaseConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date; // Use java.sql.Date explicitly
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TraitementService {
    private static final Logger LOGGER = Logger.getLogger(TraitementService.class.getName());
    private final DatabaseConnection dbConnection;

    public TraitementService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Traitement> getAllTraitements() {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Traitement traitement = new Traitement(
                        rs.getInt("id"),
                        rs.getInt("id_p"),
                        rs.getString("medicament"),
                        rs.getString("dose"),
                        rs.getString("description"),
                        rs.getString("statut"),
                        rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null,
                        rs.getBoolean("completed"),
                        rs.getBoolean("archived")
                );
                traitements.add(traitement);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des traitements : " + e.getMessage(), e);
        }
        return traitements;
    }

    public void addTraitement(Traitement traitement) {
        String sql = "INSERT INTO traitement (id_p, medicament, dose, description, statut, date, completed, archived) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, traitement.getId_p());
            stmt.setString(2, traitement.getMedicament());
            stmt.setString(3, traitement.getDose());
            stmt.setString(4, traitement.getDescription());
            stmt.setString(5, traitement.getStatut());
            stmt.setDate(6, traitement.getDate() != null ? Date.valueOf(traitement.getDate()) : null);
            stmt.setBoolean(7, traitement.isCompleted());
            stmt.setBoolean(8, traitement.isArchived());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                traitement.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du traitement : " + e.getMessage(), e);
        }
    }

    public void updateTraitement(Traitement traitement) {
        String sql = "UPDATE traitement SET id_p = ?, medicament = ?, dose = ?, description = ?, statut = ?, date = ?, completed = ?, archived = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, traitement.getId_p());
            stmt.setString(2, traitement.getMedicament());
            stmt.setString(3, traitement.getDose());
            stmt.setString(4, traitement.getDescription());
            stmt.setString(5, traitement.getStatut());
            stmt.setDate(6, traitement.getDate() != null ? Date.valueOf(traitement.getDate()) : null);
            stmt.setBoolean(7, traitement.isCompleted());
            stmt.setBoolean(8, traitement.isArchived());
            stmt.setInt(9, traitement.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La mise à jour du traitement a échoué, traitement non trouvé");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du traitement : " + e.getMessage(), e);
        }
    }

    public void deleteTraitement(int id) {
        String sql = "DELETE FROM traitement WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La suppression du traitement a échoué, traitement non trouvé");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du traitement : " + e.getMessage(), e);
        }
    }

    public void archiveTraitement(int id) {
        String query = "UPDATE traitement SET archived = true WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'archivage du traitement a échoué, traitement non trouvé");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error archiving traitement", e);
            throw new RuntimeException("Erreur lors de l'archivage du traitement : " + e.getMessage(), e);
        }
    }

    public List<Traitement> searchTraitements(String keyword) {
        List<Traitement> traitements = new ArrayList<>();
        String query = "SELECT * FROM traitement WHERE archived = false AND (medicament LIKE ? OR description LIKE ? OR statut LIKE ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Traitement traitement = new Traitement(
                            rs.getInt("id"),
                            rs.getInt("id_p"),
                            rs.getString("medicament"),
                            rs.getString("dose"),
                            rs.getString("description"),
                            rs.getString("statut"),
                            rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null,
                            rs.getBoolean("completed"),
                            rs.getBoolean("archived")
                    );
                    traitements.add(traitement);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching traitements", e);
            throw new RuntimeException("Erreur lors de la recherche des traitements : " + e.getMessage(), e);
        }
        return traitements;
    }

    public List<Traitement> getTraitementsByPrescription(int prescriptionId) {
        List<Traitement> traitements = new ArrayList<>();
        String sql = "SELECT * FROM traitement WHERE id_p = ? AND archived = false ORDER BY date";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prescriptionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Traitement traitement = new Traitement(
                            rs.getInt("id"),
                            rs.getInt("id_p"),
                            rs.getString("medicament"),
                            rs.getString("dose"),
                            rs.getString("description"),
                            rs.getString("statut"),
                            rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null,
                            rs.getBoolean("completed"),
                            rs.getBoolean("archived")
                    );
                    traitements.add(traitement);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des traitements : " + e.getMessage(), e);
        }
        return traitements;
    }

    public void exportTraitementsToCSV(String fileName) {
        List<Traitement> traitements = getAllTraitements();
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("ID,Prescription ID,Medicament,Dose,Description,Statut,Date,Complété,Archivé\n");
            for (Traitement t : traitements) {
                writer.write(String.format("%d,%d,%s,%s,%s,%s,%s,%b,%b\n",
                        t.getId(),
                        t.getId_p(),
                        escapeCsv(t.getMedicament()),
                        escapeCsv(t.getDose()),
                        escapeCsv(t.getDescription()),
                        escapeCsv(t.getStatut()),
                        t.getDate() != null ? t.getDate().toString() : "",
                        t.isCompleted(),
                        t.isArchived()
                ));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error exporting traitements to CSV", e);
            throw new RuntimeException("Erreur lors de l'exportation des traitements en CSV : " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Traitement> traitements = getAllTraitements();

        // Compter les traitements par statut
        Map<String, Long> statutCounts = traitements.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatut() != null ? t.getStatut() : "Inconnu",
                        Collectors.counting()
                ));
        stats.put("statutCounts", statutCounts);

        // Compter les traitements par mois
        Map<String, Long> monthCounts = traitements.stream()
                .filter(t -> t.getDate() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getMonth().toString(),
                        Collectors.counting()
                ));
        stats.put("monthCounts", monthCounts);

        // Compter les médicaments les plus prescrits
        Map<String, Long> medicationCounts = traitements.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getMedicament() != null ? t.getMedicament() : "Inconnu",
                        Collectors.counting()
                ));
        stats.put("medicationCounts", medicationCounts);

        // Calculer les taux de complétion
        long totalTraitements = traitements.size();
        long completedTraitements = traitements.stream()
                .filter(Traitement::isCompleted)
                .count();
        stats.put("completionRate", totalTraitements > 0 ?
                (double) completedTraitements / totalTraitements : 0);

        return stats;
    }
}
