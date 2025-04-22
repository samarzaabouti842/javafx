package tn.esprit.Pidev.controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.Models.Traitement;
import tn.esprit.Pidev.Services.PrescriptionService;
import tn.esprit.Pidev.Services.TraitementService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Added import for Collectors

public class TraitementController {

    @FXML
    @SuppressWarnings("unused") // Suppress the "never assigned" warning for FXML-injected fields
    private TableView<Traitement> traitementTable;
    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Traitement, String> medicamentColumn;
    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Traitement, String> descriptionColumn;
    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Traitement, LocalDate> dateColumn;
    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Traitement, String> statutColumn;
    @FXML
    @SuppressWarnings("unused")
    private TableColumn<Traitement, String> prescriptionColumn;
    @FXML
    @SuppressWarnings("unused")
    private TextField medicamentField;
    @FXML
    @SuppressWarnings("unused")
    private TextField doseField;
    @FXML
    @SuppressWarnings("unused")
    private TextField descriptionField;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<Integer> prescriptionComboBox;
    @FXML
    @SuppressWarnings("unused")
    private TextField searchField;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<String> filterTypeComboBox;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<String> sortField;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<String> sortOrder;
    @FXML
    @SuppressWarnings("unused")
    private Button archiveButton;
    @FXML
    @SuppressWarnings("unused")
    private CheckBox archiveToggle;
    @FXML
    @SuppressWarnings("unused")
    private Label statsLabel;
    @FXML
    @SuppressWarnings("unused")
    private VBox statsPanel;
    @FXML
    @SuppressWarnings("unused")
    private DatePicker datePicker;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<String> statutComboBox;
    @FXML
    @SuppressWarnings("unused")
    private Label pageLabel;

    private final TraitementService traitementService = new TraitementService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final ObservableList<Traitement> traitementsList = FXCollections.observableArrayList();
    private final List<Traitement> archivedTraitements = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;
    private boolean showArchived = false;
    private int currentPatientId = 1; // Assuming a default patient ID

    @FXML
    private void initialize() {
        try {
            configureTableColumns();
            setupSelectionListener();
            setupTableSorting();
            setupSearchListener();
            setupFilterComboBox();
            setupStatusComboBox();
            setupSortOptions();
            loadPrescriptions();
            loadTraitements();

            if (statsPanel != null) {
                statsPanel.setVisible(false);
            }
        } catch (RuntimeException e) {
            showError("Error during initialization: " + e.getMessage());
        }
    }

    private void setupFilterComboBox() {
        if (filterTypeComboBox != null) {
            ObservableList<String> filterOptions = FXCollections.observableArrayList(
                    "Tout", "Médicament", "Statut", "Date"
            );
            filterTypeComboBox.setItems(filterOptions);
            filterTypeComboBox.setValue("Tout");
        }
    }

    private void setupStatusComboBox() {
        if (statutComboBox != null) {
            ObservableList<String> statusOptions = FXCollections.observableArrayList(
                    "En cours", "Traité", "Résolu"
            );
            statutComboBox.setItems(statusOptions);
            statutComboBox.setValue("En cours");
        }
    }

    private void setupSortOptions() {
        if (sortField != null && sortOrder != null) {
            sortField.setItems(FXCollections.observableArrayList("Date", "Médicament", "Statut"));
            sortOrder.setItems(FXCollections.observableArrayList("Croissant", "Décroissant"));
            sortField.setValue("Date");
            sortOrder.setValue("Croissant");
        }
    }

    private void configureTableColumns() {
        if (medicamentColumn != null) {
            medicamentColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getMedicament()));
        }

        if (descriptionColumn != null) {
            descriptionColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getDescription()));
        }

        if (dateColumn != null) {
            dateColumn.setCellValueFactory(cellData ->
                    new SimpleObjectProperty<>(cellData.getValue().getDate()));
        }

        if (statutColumn != null) {
            statutColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getStatut()));

            statutColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        getStyleClass().add("status-" + item.toLowerCase().replace(" ", "-"));
                    }
                }
            });
        }

        if (prescriptionColumn != null) {
            prescriptionColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(String.valueOf(cellData.getValue().getId_p())));
        }
    }

    private void setupTableSorting() {
        if (traitementTable != null) {
            traitementTable.setSortPolicy(table -> {
                Comparator<Traitement> comparator = (t1, t2) -> {
                    String field = sortField.getValue();
                    boolean ascending = "Croissant".equals(sortOrder.getValue());

                    if (field == null) return 0;

                    switch (field) {
                        case "Médicament":
                            return ascending ?
                                    compareStringSafely(t1.getMedicament(), t2.getMedicament()) :
                                    compareStringSafely(t2.getMedicament(), t1.getMedicament());
                        case "Statut":
                            return ascending ?
                                    compareStringSafely(t1.getStatut(), t2.getStatut()) :
                                    compareStringSafely(t2.getStatut(), t1.getStatut());
                        case "Date":
                            if (t1.getDate() == null || t2.getDate() == null) {
                                return t1.getDate() == null ? (t2.getDate() == null ? 0 : -1) : 1;
                            }
                            return ascending ?
                                    t1.getDate().compareTo(t2.getDate()) :
                                    t2.getDate().compareTo(t1.getDate());
                        default:
                            return 0;
                    }
                };

                FXCollections.sort(traitementsList, comparator);
                return true;
            });
        }
    }

    private int compareStringSafely(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareTo(s2);
    }

    private void setupSelectionListener() {
        if (traitementTable != null) {
            traitementTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    medicamentField.setText(newSelection.getMedicament());
                    doseField.setText(newSelection.getDose());
                    descriptionField.setText(newSelection.getDescription());
                    prescriptionComboBox.setValue(newSelection.getId_p());
                    datePicker.setValue(newSelection.getDate());
                    statutComboBox.setValue(newSelection.getStatut());
                }
            });
        }
    }

    private void setupSearchListener() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> filterTraitements());
        }

        if (filterTypeComboBox != null) {
            filterTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                    filterTraitements());
        }
    }

    private void loadPrescriptions() {
        if (prescriptionComboBox != null) {
            try {
                List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(currentPatientId, false);
                ObservableList<Integer> prescriptionIds = FXCollections.observableArrayList();
                for (Prescription p : prescriptions) {
                    prescriptionIds.add(p.getId());
                }
                prescriptionComboBox.setItems(prescriptionIds);
                if (!prescriptionIds.isEmpty()) {
                    prescriptionComboBox.setValue(prescriptionIds.get(0));
                }
            } catch (Exception e) {
                showError("Error loading prescriptions: " + e.getMessage());
            }
        }
    }

    private void loadTraitements() {
        if (traitementTable != null) {
            traitementsList.clear();
            try {
                List<Traitement> allTraitements;
                if (showArchived) {
                    allTraitements = archivedTraitements;
                } else {
                    allTraitements = traitementService.getAllTraitements().stream()
                            .filter(t -> !t.isArchived())
                            .collect(Collectors.toList());
                }

                int start = (currentPage - 1) * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, allTraitements.size());

                if (start < allTraitements.size()) {
                    traitementsList.addAll(allTraitements.subList(start, end));
                }

                traitementTable.setItems(traitementsList);
                updatePageLabel(allTraitements.size());
            } catch (RuntimeException e) {
                showError("Error loading treatments: " + e.getMessage());
            }
        }
    }

    @FXML
    private void ajouterTraitement(ActionEvent event) {
        if (!validateInputs()) return;

        try {
            Traitement traitement = createTraitementFromInputs(0);
            traitement.setStatut(statutComboBox.getValue());
            traitement.setDate(datePicker.getValue());

            traitementService.addTraitement(traitement);
            loadTraitements();
            clearFields();
            showInfo("Treatment added successfully.");
        } catch (Exception e) {
            showError("Error adding treatment: " + e.getMessage());
        }
    }

    @FXML
    private void modifierTraitement(ActionEvent event) {
        Traitement selected = traitementTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a treatment to modify.");
            return;
        }

        if (!validateInputs()) return;

        try {
            Traitement updated = createTraitementFromInputs(selected.getId());
            updated.setStatut(statutComboBox.getValue());
            updated.setDate(datePicker.getValue());
            updated.setCompleted(selected.isCompleted());
            updated.setArchived(selected.isArchived());

            traitementService.updateTraitement(updated);
            loadTraitements();
            clearFields();
            showInfo("Treatment updated successfully.");
        } catch (Exception e) {
            showError("Error updating treatment: " + e.getMessage());
        }
    }

    @FXML
    private void supprimerTraitement(ActionEvent event) {
        Traitement selected = traitementTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a treatment to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Delete Treatment");
        confirmation.setContentText("Are you sure you want to delete this treatment?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                traitementService.deleteTraitement(selected.getId());
                loadTraitements();
                clearFields();
                showInfo("Treatment deleted successfully.");
            } catch (Exception e) {
                showError("Error deleting treatment: " + e.getMessage());
            }
        }
    }

    private void filterTraitements() {
        String searchText = searchField.getText().toLowerCase();
        String filterType = filterTypeComboBox.getValue();

        if (searchText.isEmpty()) {
            loadTraitements();
            return;
        }

        try {
            List<Traitement> filtered = traitementService.searchTraitements(searchText);
            if (filterType != null && !filterType.equals("Tout")) {
                filtered = filtered.stream()
                        .filter(traitement -> {
                            switch (filterType) {
                                case "Médicament":
                                    return traitement.getMedicament().toLowerCase().contains(searchText);
                                case "Date":
                                    return traitement.getDate() != null && traitement.getDate().toString().contains(searchText);
                                case "Statut":
                                    return traitement.getStatut().toLowerCase().contains(searchText);
                                default:
                                    return true;
                            }
                        })
                        .collect(Collectors.toList());
            }

            traitementsList.setAll(filtered);
            currentPage = 1;
            updatePageLabel(filtered.size());
            showInfo("Filter applied for: " + searchText);
        } catch (Exception e) {
            showError("Error applying filter: " + e.getMessage());
        }
    }

    @FXML
    private void trierTraitements(ActionEvent event) {
        if (sortField != null && sortOrder != null) {
            traitementTable.getSortOrder().clear();
            loadTraitements(); // This will trigger the sort policy
            showInfo("Table sorted by " + sortField.getValue() + " (" + sortOrder.getValue() + ").");
        }
    }

    @FXML
    private void archiverTraitementsExpirés(ActionEvent event) {
        List<Traitement> resolved = traitementService.getAllTraitements()
                .stream()
                .filter(t -> !t.isArchived() && "Résolu".equals(t.getStatut()))
                .collect(Collectors.toList());

        if (resolved.isEmpty()) {
            showInfo("No resolved treatments to archive.");
            return;
        }

        for (Traitement t : resolved) {
            traitementService.archiveTraitement(t.getId());
            archivedTraitements.add(t);
        }

        loadTraitements();
        showInfo(resolved.size() + " resolved treatments archived.");
    }

    @FXML
    private void basculerVueArchivées(ActionEvent event) {
        if (archiveToggle != null) {
            showArchived = archiveToggle.isSelected();
            currentPage = 1;
            loadTraitements();
            String message = showArchived ? "Displaying archived treatments." : "Displaying active treatments.";
            showInfo(message);
        }
    }

    @FXML
    private void exporterTraitements(ActionEvent event) {
        try {
            String fileName = "traitements_export_" + LocalDate.now().format(dateFormatter) + ".csv";
            traitementService.exportTraitementsToCSV(fileName);
            showInfo("Treatments exported successfully to " + fileName);
        } catch (Exception e) {
            showError("Error exporting: " + e.getMessage());
        }
    }

    @FXML
    private void pagePrecedente() {
        if (currentPage > 1) {
            currentPage--;
            loadTraitements();
            showInfo("Previous page loaded.");
        } else {
            showInfo("You are already on the first page.");
        }
    }

    @FXML
    private void pageSuivante() {
        try {
            int totalTraitements = (showArchived ?
                    archivedTraitements : traitementService.getAllTraitements().stream()
                    .filter(t -> !t.isArchived())
                    .collect(Collectors.toList())).size();

            if ((currentPage * ITEMS_PER_PAGE) < totalTraitements) {
                currentPage++;
                loadTraitements();
                showInfo("Next page loaded.");
            } else {
                showInfo("You are on the last page.");
            }
        } catch (RuntimeException e) {
            showError("Error loading next page: " + e.getMessage());
        }
    }

    @FXML
    private void afficherStatistiques(ActionEvent event) {
        if (statsLabel == null || statsPanel == null) return;

        try {
            Map<String, Object> stats = traitementService.getStatistics();
            List<Traitement> allTraitements = traitementService.getAllTraitements();

            long activeCount = allTraitements.stream().filter(t -> !t.isArchived()).count();
            long archivedCount = allTraitements.stream().filter(Traitement::isArchived).count();

            long enCours = allTraitements.stream()
                    .filter(t -> !t.isArchived() && "En cours".equals(t.getStatut()))
                    .count();
            long traite = allTraitements.stream()
                    .filter(t -> !t.isArchived() && "Traité".equals(t.getStatut()))
                    .count();
            long resolu = allTraitements.stream()
                    .filter(t -> !t.isArchived() && "Résolu".equals(t.getStatut()))
                    .count();

            StringBuilder statsText = new StringBuilder();
            statsText.append("Total active treatments: ").append(activeCount).append("\n");
            statsText.append("Total archived treatments: ").append(archivedCount).append("\n");
            statsText.append("In progress: ").append(enCours).append("\n");
            statsText.append("Treated: ").append(traite).append("\n");
            statsText.append("Resolved: ").append(resolu).append("\n");

            if (stats.containsKey("monthCounts")) {
                Map<String, Long> monthCounts = (Map<String, Long>) stats.get("monthCounts");
                statsText.append("\nTreatments by month:\n");
                monthCounts.forEach((month, count) ->
                        statsText.append(month).append(": ").append(count).append("\n"));
            }

            if (stats.containsKey("medicationCounts")) {
                Map<String, Long> medCounts = (Map<String, Long>) stats.get("medicationCounts");
                statsText.append("\nTop medications:\n");
                medCounts.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .forEach(entry ->
                                statsText.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
            }

            statsLabel.setText(statsText.toString());
            statsPanel.setVisible(true);
            showInfo("Statistics displayed.");
        } catch (Exception e) {
            showError("Error displaying statistics: " + e.getMessage());
        }
    }

    @FXML
    private void cacherStatistiques(ActionEvent event) {
        if (statsPanel != null) {
            statsPanel.setVisible(false);
            showInfo("Statistics hidden.");
        }
    }

    private Traitement createTraitementFromInputs(int id) {
        Traitement traitement = new Traitement();
        traitement.setId(id);
        traitement.setMedicament(medicamentField.getText().trim());
        traitement.setDose(doseField.getText().trim());
        traitement.setDescription(descriptionField.getText().trim());

        if (prescriptionComboBox != null && prescriptionComboBox.getValue() != null) {
            traitement.setId_p(prescriptionComboBox.getValue());
        } else {
            throw new IllegalArgumentException("Prescription must be selected");
        }

        return traitement;
    }

    private boolean validateInputs() {
        if (medicamentField == null || medicamentField.getText().trim().isEmpty()) {
            showError("Medication name is required.");
            return false;
        }

        if (doseField == null || doseField.getText().trim().isEmpty()) {
            showError("Dose is required.");
            return false;
        }

        if (descriptionField == null || descriptionField.getText().trim().isEmpty()) {
            showError("Description is required.");
            return false;
        }

        if (prescriptionComboBox == null || prescriptionComboBox.getValue() == null) {
            showError("Prescription must be selected.");
            return false;
        }

        if (datePicker == null || datePicker.getValue() == null) {
            showError("Date is required.");
            return false;
        }

        if (statutComboBox == null || statutComboBox.getValue() == null) {
            showError("Status must be selected.");
            return false;
        }

        return true;
    }

    private void clearFields() {
        if (medicamentField != null) medicamentField.clear();
        if (doseField != null) doseField.clear();
        if (descriptionField != null) descriptionField.clear();
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        if (statutComboBox != null) statutComboBox.setValue("En cours");
        if (searchField != null) searchField.clear();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void reinitialiserChamps(ActionEvent event) {
        clearFields();
        traitementTable.getSelectionModel().clearSelection();
        showInfo("Fields reset.");
    }

    @FXML
    private void refreshTraitements(ActionEvent event) {
        loadTraitements();
        showInfo("Treatments refreshed.");
    }

    private void updatePageLabel(int totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        totalPages = Math.max(1, totalPages); // Ensure at least 1 page
        pageLabel.setText(String.format("Page %d/%d", currentPage, totalPages));
    }

    @FXML
    private void ouvrirNouveauTraitement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/nouveautraitement.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nouveau Traitement");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTraitements();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        }
    }
}
