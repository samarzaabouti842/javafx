package tn.esprit.Pidev.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.Services.PrescriptionService;
import tn.esprit.Pidev.Services.PatientService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrescriptionController {

    @FXML private TableView<Prescription> prescriptionTable;
    @FXML private TableColumn<Prescription, String> adresseColumn;
    @FXML private TableColumn<Prescription, String> gmailColumn;
    @FXML private TableColumn<Prescription, String> dateDebutColumn;
    @FXML private TableColumn<Prescription, String> statutColumn;
    @FXML private TextField dateDebutField;
    @FXML private TextField dateFinField;
    @FXML private TextField adresseField;
    @FXML private TextField gmailField;
    @FXML private TextField patientIdField;
    @FXML private TextField filterField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button archiveButton;
    @FXML private CheckBox archiveToggle;
    @FXML private Label statsLabel;
    @FXML private VBox statsPanel;
    @FXML private Pagination pagination;

    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final PatientService patientService = new PatientService();
    private final ObservableList<Prescription> prescriptionsList = FXCollections.observableArrayList();
    private final List<Prescription> archivedPrescriptions = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;
    private boolean showArchived = false;
    private boolean sortAscending = true;

    @FXML
    private void initialize() {
        configureTableColumns();
        setupSelectionListener();
        setupTableSorting();
        setupFilterComboBox();
        setupSearchListener();
        setupPagination();

        try {
            loadPrescriptions();
        } catch (RuntimeException e) {
            showError("Erreur lors du chargement initial des prescriptions : " + e.getMessage());
        }

        if (statsPanel != null) {
            statsPanel.setVisible(false);
        }
    }

    private void setupFilterComboBox() {
        ObservableList<String> filterOptions = FXCollections.observableArrayList(
                "Tout", "Date", "Adresse", "Gmail", "Statut"
        );
        filterComboBox.setItems(filterOptions);
        filterComboBox.getSelectionModel().selectFirst();
    }

    private void configureTableColumns() {
        adresseColumn.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        gmailColumn.setCellValueFactory(new PropertyValueFactory<>("gmail"));
        dateDebutColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDeb().format(dateFormatter)));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statutColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Traité":
                            setStyle("-fx-text-fill: #008000; -fx-font-weight: bold;");
                            break;
                        case "Résolu":
                            setStyle("-fx-text-fill: #0000ff; -fx-font-weight: bold;");
                            break;
                        case "En cours":
                            setStyle("-fx-text-fill: #ff8c00; -fx-font-weight: bold;");
                            break;
                        case "Archivé":
                            setStyle("-fx-text-fill: #999999; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });
    }

    private void setupTableSorting() {
        prescriptionTable.setSortPolicy(table -> {
            Comparator<Prescription> comparator = (p1, p2) -> {
                for (TableColumn<Prescription, ?> column : table.getSortOrder()) {
                    if (column == adresseColumn) {
                        return column.getSortType() == TableColumn.SortType.ASCENDING ?
                                p1.getAdresse().compareTo(p2.getAdresse()) :
                                p2.getAdresse().compareTo(p1.getAdresse());
                    } else if (column == gmailColumn) {
                        return column.getSortType() == TableColumn.SortType.ASCENDING ?
                                p1.getGmail().compareTo(p2.getGmail()) :
                                p2.getGmail().compareTo(p1.getGmail());
                    } else if (column == dateDebutColumn) {
                        return column.getSortType() == TableColumn.SortType.ASCENDING ?
                                p1.getDateDeb().compareTo(p2.getDateDeb()) :
                                p2.getDateDeb().compareTo(p1.getDateDeb());
                    } else if (column == statutColumn) {
                        return column.getSortType() == TableColumn.SortType.ASCENDING ?
                                p1.getStatut().compareTo(p2.getStatut()) :
                                p2.getStatut().compareTo(p1.getStatut());
                    }
                }
                return 0;
            };
            FXCollections.sort(prescriptionsList, comparator);
            return true;
        });
    }

    private void setupSelectionListener() {
        prescriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                dateDebutField.setText(newSelection.getDateDeb().format(formatter));
                dateFinField.setText(newSelection.getDateFin().format(formatter));
                adresseField.setText(newSelection.getAdresse());
                gmailField.setText(newSelection.getGmail());
                patientIdField.setText(String.valueOf(newSelection.getPatientId()));
            }
        });
    }

    private void setupSearchListener() {
        filterField.textProperty().addListener((obs, oldValue, newValue) -> filtrerPrescriptions());
        filterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> filtrerPrescriptions());
    }

    private void setupPagination() {
        if (pagination != null) {
            pagination.setPageFactory(this::createPage);
            pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = newVal.intValue();
                loadPrescriptions();
            });
        }
    }

    private javafx.scene.Node createPage(int pageIndex) {
        currentPage = pageIndex;
        loadPrescriptions();
        return prescriptionTable;
    }

    private void loadPrescriptions() {
        prescriptionsList.clear();
        try {
            List<Prescription> allPrescriptions;
            if (showArchived) {
                allPrescriptions = new ArrayList<>(archivedPrescriptions);
            } else {
                allPrescriptions = prescriptionService.getAllPrescriptions();
            }

            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, allPrescriptions.size());
            if (start < allPrescriptions.size()) {
                prescriptionsList.addAll(allPrescriptions.subList(start, end));
            }
            prescriptionTable.setItems(prescriptionsList);

            // Mettre à jour la pagination
            int totalPages = (int) Math.ceil((double) allPrescriptions.size() / PAGE_SIZE);
            totalPages = Math.max(1, totalPages); // Ensure at least 1 page
            updatePaginationInfo(totalPages);
        } catch (RuntimeException e) {
            showError("Erreur lors du chargement des prescriptions : " + e.getMessage());
        }
    }

    private void updatePaginationInfo(int totalPages) {
        if (pagination != null) {
            pagination.setPageCount(totalPages);
            pagination.setCurrentPageIndex(currentPage);
        }
    }

    private boolean validateInputs() {
        if (dateDebutField.getText().trim().isEmpty() || dateFinField.getText().trim().isEmpty() ||
                adresseField.getText().trim().isEmpty() || gmailField.getText().trim().isEmpty() ||
                patientIdField.getText().trim().isEmpty()) {
            showError("Tous les champs sont obligatoires.");
            return false;
        }

        try {
            LocalDateTime dateDebut = LocalDateTime.parse(dateDebutField.getText(), formatter);
            LocalDateTime dateFin = LocalDateTime.parse(dateFinField.getText(), formatter);

            if (dateDebut.isAfter(dateFin)) {
                showError("La date de début doit être antérieure à la date de fin.");
                return false;
            }

            if (dateDebut.isBefore(LocalDateTime.now())) {
                showError("La date de début ne peut pas être dans le passé.");
                return false;
            }

            return true;
        } catch (DateTimeParseException e) {
            showError("Format de date invalide. Utilisez yyyy-MM-dd HH:mm:ss");
            return false;
        }
    }

    private Prescription createPrescriptionFromInputs(int id) {
        LocalDateTime dateDebut = LocalDateTime.parse(dateDebutField.getText(), formatter);
        LocalDateTime dateFin = LocalDateTime.parse(dateFinField.getText(), formatter);
        String adresse = adresseField.getText();
        String gmail = gmailField.getText();
        int patientId = Integer.parseInt(patientIdField.getText());

        Prescription prescription = new Prescription();
        prescription.setId(id);
        prescription.setDateDeb(dateDebut);
        prescription.setDateFin(dateFin);
        prescription.setAdresse(adresse);
        prescription.setGmail(gmail);
        prescription.setPatientId(patientId);

        return prescription;
    }

    @FXML
    private void ajouterPrescription(ActionEvent event) {
        if (!validateInputs()) return;

        try {
            Prescription prescription = createPrescriptionFromInputs(0);

            // Vérifier que le patient existe
            if (patientService.getPatientById(prescription.getPatientId()) == null) {
                showError("Le patient avec l'ID " + prescription.getPatientId() + " n'existe pas.");
                return;
            }

            // Vérifier les conflits de dates
            if (prescriptionService.hasDateConflict(prescription.getPatientId(), prescription.getDateDeb(), prescription.getDateFin())) {
                showError("Conflit de dates détecté pour ce patient.");
                return;
            }

            prescription.setStatut("En cours"); // Default status
            prescriptionService.addPrescription(prescription);
            loadPrescriptions();
            clearFields();
            showInfo("Prescription ajoutée avec succès.");
        } catch (DateTimeParseException e) {
            showError("Format de date invalide. Utilisez yyyy-MM-dd HH:mm:ss");
        } catch (NumberFormatException e) {
            showError("L'ID du patient doit être un nombre valide.");
        } catch (Exception e) {
            showError("Erreur lors de l'ajout de la prescription : " + e.getMessage());
        }
    }

    @FXML
    private void modifierPrescription(ActionEvent event) {
        Prescription selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une prescription à modifier.");
            return;
        }
        if (!validateInputs()) return;

        try {
            Prescription updated = createPrescriptionFromInputs(selected.getId());

            // Vérifier que le patient existe
            if (patientService.getPatientById(updated.getPatientId()) == null) {
                showError("Le patient avec l'ID " + updated.getPatientId() + " n'existe pas.");
                return;
            }

            // Vérifier les conflits de dates (en excluant la prescription actuelle)
            boolean otherConflicts = prescriptionService.getAllPrescriptions().stream()
                    .filter(p -> p.getId() != selected.getId() && p.getPatientId() == updated.getPatientId())
                    .anyMatch(p -> (p.getDateDeb().isBefore(updated.getDateFin()) && p.getDateFin().isAfter(updated.getDateDeb())));

            if (otherConflicts) {
                showError("Conflit de dates détecté pour ce patient.");
                return;
            }

            updated.setStatut(selected.getStatut()); // Preserve status
            updated.setArchived(selected.isArchived());
            prescriptionService.updatePrescription(updated);
            loadPrescriptions();
            clearFields();
            showInfo("Prescription modifiée avec succès.");
        } catch (DateTimeParseException e) {
            showError("Format de date invalide. Utilisez yyyy-MM-dd HH:mm:ss");
        } catch (NumberFormatException e) {
            showError("L'ID du patient doit être un nombre valide.");
        } catch (Exception e) {
            showError("Erreur lors de la modification de la prescription : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerPrescription(ActionEvent event) {
        Prescription selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une prescription à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette prescription ?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    prescriptionService.deletePrescription(selected.getId());
                    loadPrescriptions();
                    clearFields();
                    showInfo("Prescription supprimée avec succès.");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de la prescription : " + e.getMessage());
                }
            }
        });
    }

    private void filtrerPrescriptions() {
        String filterType = filterComboBox.getSelectionModel().getSelectedItem();
        String filterText = filterField.getText().trim().toLowerCase();

        if (filterType == null || filterText.isEmpty()) {
            loadPrescriptions();
            return;
        }

        try {
            List<Prescription> filtered = (showArchived ? archivedPrescriptions : prescriptionService.getAllPrescriptions());

            switch (filterType) {
                case "Tout":
                    filtered = filtered.stream()
                            .filter(p -> p.getAdresse().toLowerCase().contains(filterText) ||
                                    p.getGmail().toLowerCase().contains(filterText) ||
                                    p.getStatut().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
                case "Date":
                    try {
                        LocalDate filterDate = LocalDate.parse(filterText, dateFormatter);
                        filtered = filtered.stream()
                                .filter(p -> p.getDateDeb().toLocalDate().equals(filterDate))
                                .collect(Collectors.toList());
                    } catch (DateTimeParseException e) {
                        showError("Format de date invalide. Utilisez yyyy-MM-dd");
                        return;
                    }
                    break;
                case "Adresse":
                    filtered = filtered.stream()
                            .filter(p -> p.getAdresse().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
                case "Gmail":
                    filtered = filtered.stream()
                            .filter(p -> p.getGmail().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
                case "Statut":
                    filtered = filtered.stream()
                            .filter(p -> p.getStatut().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
            }

            prescriptionsList.setAll(filtered);
            currentPage = 0; // Reset pagination
            updatePaginationInfo((int) Math.ceil((double) filtered.size() / PAGE_SIZE));
            showInfo("Filtrage appliqué : " + filtered.size() + " résultats");
        } catch (Exception e) {
            showError("Erreur lors du filtrage : " + e.getMessage());
        }
    }

    @FXML
    private void trierPrescriptions(ActionEvent event) {
        Comparator<Prescription> comparator = sortAscending ?
                Comparator.comparing(Prescription::getStatut) :
                Comparator.comparing(Prescription::getStatut).reversed();

        FXCollections.sort(prescriptionsList, comparator);
        sortAscending = !sortAscending;

        String message = sortAscending ?
                "Tableau trié par statut (ascendant)." :
                "Tableau trié par statut (descendant).";

        showInfo(message);
    }

    @FXML
    private void archiverPrescriptionsExpirées(ActionEvent event) {
        LocalDateTime now = LocalDateTime.now();
        List<Prescription> expired = prescriptionService.getAllPrescriptions()
                .stream()
                .filter(p -> !p.isArchived() && p.getDateFin().isBefore(now))
                .collect(Collectors.toList());

        if (expired.isEmpty()) {
            showInfo("Aucune prescription expirée à archiver.");
            return;
        }

        for (Prescription p : expired) {
            p.setStatut("Archivé");
            p.setArchived(true);
            prescriptionService.archivePrescription(p.getId());
            archivedPrescriptions.add(p);
        }

        loadPrescriptions();
        showInfo(expired.size() + " prescriptions expirées archivées.");
    }

    @FXML
    private void basculerVueArchivées(ActionEvent event) {
        showArchived = archiveToggle.isSelected();
        currentPage = 0;
        loadPrescriptions();
        String message = showArchived ? "Affichage des prescriptions archivées." : "Affichage des prescriptions actives.";
        showInfo(message);
    }

    @FXML
    private void afficherStatistiques(ActionEvent event) {
        try {
            List<Prescription> allPrescriptions = prescriptionService.getAllPrescriptions();
            long activeCount = allPrescriptions.stream().filter(p -> !p.isArchived()).count();
            long archivedCount = archivedPrescriptions.size();

            // Calcul de la durée moyenne des prescriptions
            double averageDurationDays = allPrescriptions.stream()
                    .mapToLong(p -> java.time.Duration.between(p.getDateDeb(), p.getDateFin()).toDays())
                    .average()
                    .orElse(0.0);

            // Distribution par statut
            long enCours = allPrescriptions.stream().filter(p -> "En cours".equals(p.getStatut())).count();
            long traite = allPrescriptions.stream().filter(p -> "Traité".equals(p.getStatut())).count();
            long resolu = allPrescriptions.stream().filter(p -> "Résolu".equals(p.getStatut())).count();

            // Statistiques par patient
            Map<Integer, Long> prescriptionsParPatient = allPrescriptions.stream()
                    .collect(Collectors.groupingBy(Prescription::getPatientId, Collectors.counting()));

            // Patients avec le plus de prescriptions
            String topPatients = prescriptionsParPatient.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> "Patient ID " + entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));

            // Construire la chaîne de statistiques
            StringBuilder stats = new StringBuilder();
            stats.append("Statistiques des prescriptions:\n\n");
            stats.append("Nombre total de prescriptions actives: ").append(activeCount).append("\n");
            stats.append("Nombre total de prescriptions archivées: ").append(archivedCount).append("\n\n");
            stats.append("Durée moyenne des prescriptions: ").append(String.format("%.1f", averageDurationDays)).append(" jours\n\n");
            stats.append("Distribution par statut:\n");
            stats.append("- En cours: ").append(enCours).append(" (").append(activeCount > 0 ? String.format("%.1f", (double)enCours/activeCount*100) : "0.0").append("%)\n");
            stats.append("- Traité: ").append(traite).append(" (").append(activeCount > 0 ? String.format("%.1f", (double)traite/activeCount*100) : "0.0").append("%)\n");
            stats.append("- Résolu: ").append(resolu).append(" (").append(activeCount > 0 ? String.format("%.1f", (double)resolu/activeCount*100) : "0.0").append("%)\n\n");
            stats.append("Patients avec le plus de prescriptions:\n").append(topPatients);

            // Afficher dans une boîte de dialogue
            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques des prescriptions");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText(stats.toString());
            statsAlert.showAndWait();

            // Mettre à jour le label de statistiques
            if (statsLabel != null) {
                statsLabel.setText("Total: " + activeCount + " actives, " + archivedCount + " archivées");
                statsPanel.setVisible(true);
            }

        } catch (Exception e) {
            showError("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    @FXML
    private void cacherStatistiques(ActionEvent event) {
        if (statsPanel != null) {
            statsPanel.setVisible(false);
            showInfo("Statistiques masquées.");
        }
    }

    @FXML
    private void changerStatut(ActionEvent event) {
        Prescription selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une prescription pour changer son statut.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le statut");
        dialog.setHeaderText("Choisir un nouveau statut pour la prescription");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("En cours", "Traité", "Résolu");
        statusComboBox.setValue(selected.getStatut());

        dialog.getDialogPane().setContent(statusComboBox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                selected.setStatut(newStatus);
                prescriptionService.updatePrescription(selected);
                loadPrescriptions();
                showInfo("Statut modifié avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de la modification du statut : " + e.getMessage());
            }
        });
    }

    @FXML
    private void exporterPrescriptions(ActionEvent event) {
        try {
            String fileName = "prescriptions_export_" + LocalDate.now().format(dateFormatter) + ".csv";
            prescriptionService.exportPrescriptionsToCSV(fileName);
            showInfo("Prescriptions exportées avec succès vers " + fileName);
        } catch (Exception e) {
            showError("Erreur lors de l'exportation : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirNouvellePrescription(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/nouvelleprescription.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Prescription");
            stage.setScene(new Scene(root));

            stage.showAndWait();
            loadPrescriptions();

        } catch (Exception e) {
            showError("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void clearFields() {
        dateDebutField.clear();
        dateFinField.clear();
        adresseField.clear();
        gmailField.clear();
        patientIdField.clear();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
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
    private void pagePrecedente(ActionEvent event) { // Fixed typo
        if (currentPage > 0) {
            currentPage--;
            loadPrescriptions();
            showInfo("Page précédente chargée.");
        } else {
            showInfo("Vous êtes déjà sur la première page.");
        }
    }

    @FXML
    private void pageSuivante(ActionEvent event) {
        List<Prescription> allPrescriptions = showArchived ? archivedPrescriptions : prescriptionService.getAllPrescriptions();
        int totalPages = (int) Math.ceil((double) allPrescriptions.size() / PAGE_SIZE);

        if (currentPage < totalPages - 1) {
            currentPage++;
            loadPrescriptions();
            showInfo("Page suivante chargée.");
        } else {
            showInfo("Vous êtes sur la dernière page.");
        }
    }

    @FXML
    private void reinitialiserFiltre(ActionEvent event) {
        filterField.clear();
        filterComboBox.getSelectionModel().selectFirst();
        loadPrescriptions();
        showInfo("Filtre réinitialisé.");
    }
}
