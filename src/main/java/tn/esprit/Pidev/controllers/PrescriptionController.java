package tn.esprit.Pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.Services.PrescriptionService;
import tn.esprit.Pidev.Services.PatientService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller for managing prescriptions in the JavaFX UI.
 * Handles displaying prescriptions as cards, filtering, sorting, pagination, and CRUD operations.
 */
public class PrescriptionController {

    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField adresseField;
    @FXML private TextField gmailField;
    @FXML private TextField patientIdField;
    @FXML private TextField filterField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ComboBox<String> sortField;
    @FXML private ComboBox<String> sortOrder;
    @FXML private Button archiveButton;
    @FXML private CheckBox archiveToggle;
    @FXML private Label statsLabel;
    @FXML private VBox statsPanel;
    @FXML private FlowPane prescriptionsContainer; // Changed from HBox to FlowPane for better layout
    @FXML private Label pageLabel;

    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final PatientService patientService = new PatientService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private boolean showArchived = false;
    private Prescription selectedPrescription;
    private List<Prescription> currentPrescriptions;
    private boolean sortAscending = true;
    private int currentPage = 1;
    private final int prescriptionsPerPage = 5;

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    private void initialize() {
        setupFilterComboBox();
        setupSortComboBoxes();
        setupSearchListener();
        loadPrescriptions();

        if (statsPanel != null) {
            statsPanel.setVisible(false);
        }
    }

    /**
     * Sets up the filter ComboBox with available options.
     */
    private void setupFilterComboBox() {
        if (filterComboBox == null) {
            showError("Erreur d'initialisation : ComboBox de filtrage non trouvé.");
            return;
        }
        filterComboBox.getItems().addAll("Tout", "Date", "Adresse", "Gmail", "Statut");
        filterComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Sets up the sort ComboBoxes for sorting prescriptions.
     */
    private void setupSortComboBoxes() {
        if (sortField == null || sortOrder == null) {
            showError("Erreur d'initialisation : ComboBox de tri non trouvé.");
            return;
        }
        sortField.getItems().addAll("Statut", "Date Début", "Patient ID");
        sortField.setValue("Statut");
        sortOrder.getItems().addAll("Ascendant", "Descendant");
        sortOrder.setValue("Ascendant");
    }

    /**
     * Sets up listeners for filter field and ComboBox to trigger filtering.
     */
    private void setupSearchListener() {
        if (filterField == null || filterComboBox == null) {
            showError("Erreur d'initialisation : Champs de filtrage non trouvés.");
            return;
        }
        filterField.textProperty().addListener((obs, oldValue, newValue) -> filtrerPrescriptions());
        filterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> filtrerPrescriptions());
    }

    /**
     * Loads prescriptions from the service and displays them as cards with pagination.
     */
    private void loadPrescriptions() {
        if (prescriptionsContainer == null) {
            showError("Erreur d'initialisation : Conteneur de prescriptions non trouvé.");
            return;
        }
        prescriptionsContainer.getChildren().clear();
        selectedPrescription = null;

        try {
            currentPrescriptions = prescriptionService.getAllPrescriptions(showArchived);

            if (currentPrescriptions.isEmpty()) {
                Text noPrescriptionsText = new Text("Aucune prescription trouvée.");
                noPrescriptionsText.getStyleClass().add("no-data-text");
                prescriptionsContainer.getChildren().add(noPrescriptionsText);
                updatePaginationLabel();
                return;
            }

            // Apply pagination
            int totalPages = (int) Math.ceil((double) currentPrescriptions.size() / prescriptionsPerPage);
            currentPage = Math.min(currentPage, totalPages);
            currentPage = Math.max(currentPage, 1);

            int startIndex = (currentPage - 1) * prescriptionsPerPage;
            int endIndex = Math.min(startIndex + prescriptionsPerPage, currentPrescriptions.size());

            List<Prescription> prescriptionsToShow = currentPrescriptions.subList(startIndex, endIndex);

            for (Prescription prescription : prescriptionsToShow) {
                VBox card = createPrescriptionCard(prescription);
                prescriptionsContainer.getChildren().add(card);
            }

            updatePaginationLabel();
        } catch (RuntimeException e) {
            showError("Erreur lors du chargement des prescriptions : " + e.getMessage());
        }
    }

    /**
     * Updates the pagination label with the current page and total pages.
     */
    private void updatePaginationLabel() {
        if (pageLabel == null) return;
        int totalPages = (int) Math.ceil((double) currentPrescriptions.size() / prescriptionsPerPage);
        pageLabel.setText("Page " + currentPage + "/" + Math.max(1, totalPages));
    }

    /**
     * Creates a card (VBox) for a prescription with details and action buttons.
     *
     * @param prescription The prescription to display.
     * @return A VBox representing the prescription card.
     */
    private VBox createPrescriptionCard(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");

        VBox card = new VBox();
        card.getStyleClass().add("prescription-card");
        card.setPrefWidth(320);
        card.setPrefHeight(180);
        card.setSpacing(12);

        // Header: Date range with archived badge
        HBox headerBox = new HBox();
        headerBox.setSpacing(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Text dateRange = new Text(
                (prescription.getDateDeb() != null ? prescription.getDateDeb().format(dateFormatter) : "N/A") + " - " +
                        (prescription.getDateFin() != null ? prescription.getDateFin().format(dateFormatter) : "N/A")
        );
        dateRange.getStyleClass().add("card-title");

        if (prescription.isArchived()) {
            Text archivedBadge = new Text("Archivé");
            archivedBadge.getStyleClass().add("archived-badge");
            headerBox.getChildren().addAll(dateRange, archivedBadge);
        } else {
            headerBox.getChildren().add(dateRange);
        }

        // Details
        Text adresse = new Text("Adresse: " + (prescription.getAdresse() != null ? prescription.getAdresse() : "N/A"));
        adresse.getStyleClass().add("card-detail");

        Text gmail = new Text("Gmail: " + (prescription.getGmail() != null ? prescription.getGmail() : "N/A"));
        gmail.getStyleClass().add("card-detail");

        Text status = new Text("Statut: " + (prescription.getStatut() != null ? prescription.getStatut() : "N/A"));
        status.getStyleClass().add("card-status");
        if (prescription.getStatut() != null) {
            switch (prescription.getStatut().toLowerCase()) {
                case "en cours":
                    status.getStyleClass().add("status-en-cours");
                    break;
                case "traité":
                    status.getStyleClass().add("status-traité");
                    break;
                case "résolu":
                    status.getStyleClass().add("status-résolu");
                    break;
                case "archivé":
                    status.getStyleClass().add("status-archivé");
                    break;
                default:
                    status.getStyleClass().add("status-default");
                    break;
            }
        }

        // Buttons
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button suivantButton = new Button("Suivant");
        suivantButton.getStyleClass().add("card-action-button");
        suivantButton.setOnAction(e -> handleSuivant(prescription));

        Button voirArchivButton = new Button(prescription.isArchived() ? "Déjà Archivé" : "Archiver");
        voirArchivButton.getStyleClass().add("card-action-button-secondary");
        voirArchivButton.setDisable(prescription.isArchived());
        voirArchivButton.setOnAction(e -> handleVoirArchiv(prescription));

        buttonBox.getChildren().addAll(suivantButton, voirArchivButton);

        card.getChildren().addAll(headerBox, adresse, gmail, status, buttonBox);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        card.setOnMouseExited(e -> {
            if (selectedPrescription != prescription) {
                card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.3, 0, 0);");
            }
        });

        card.setOnMouseClicked(event -> selectPrescription(prescription, card));

        return card;
    }

    /**
     * Selects a prescription card and populates the input fields with its data.
     *
     * @param prescription The prescription to select.
     * @param card The card (VBox) representing the prescription.
     */
    private void selectPrescription(Prescription prescription, VBox card) {
        for (var node : prescriptionsContainer.getChildren()) {
            node.setStyle("-fx-border-color: #d3d3d3;");
        }

        card.setStyle("-fx-border-color: #3498db; -fx-border-width: 2;");
        selectedPrescription = prescription;

        dateDebutPicker.setValue(prescription.getDateDeb());
        dateFinPicker.setValue(prescription.getDateFin());
        adresseField.setText(prescription.getAdresse());
        gmailField.setText(prescription.getGmail());
        patientIdField.setText(String.valueOf(prescription.getPatientId()));
    }

    /**
     * Handles the "Suivant" button action to select the next prescription.
     *
     * @param prescription The current prescription.
     */
    private void handleSuivant(Prescription prescription) {
        int index = currentPrescriptions.indexOf(prescription);
        if (index + 1 < currentPrescriptions.size()) {
            Prescription nextPrescription = currentPrescriptions.get(index + 1);
            int displayIndex = (index + 1) % prescriptionsPerPage;
            selectPrescription(nextPrescription, (VBox) prescriptionsContainer.getChildren().get(displayIndex));
            showInfo("Prescription suivante sélectionnée.");
        } else {
            showInfo("C'est la dernière prescription.");
        }
    }

    /**
     * Handles the "Archiver" button action to archive a prescription.
     *
     * @param prescription The prescription to archive.
     */
    private void handleVoirArchiv(Prescription prescription) {
        if (!prescription.isArchived()) {
            prescriptionService.archivePrescription(prescription.getId());
            showInfo("Prescription archivée avec succès.");
            loadPrescriptions();
        } else {
            showInfo("Cette prescription est déjà archivée.");
        }
    }

    /**
     * Validates the input fields for adding or updating a prescription.
     *
     * @return True if inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null ||
                adresseField.getText().trim().isEmpty() || gmailField.getText().trim().isEmpty() ||
                patientIdField.getText().trim().isEmpty()) {
            showError("Tous les champs sont obligatoires.");
            return false;
        }

        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();

        if (dateDebut.isAfter(dateFin)) {
            showError("La date de début doit être antérieure ou égale à la date de fin.");
            return false;
        }

        if (dateDebut.isBefore(LocalDate.now())) {
            showError("La date de début ne peut pas être dans le passé.");
            return false;
        }

        String gmail = gmailField.getText().trim();
        if (!isValidEmail(gmail)) {
            showError("L'adresse Gmail n'est pas valide : " + gmail);
            return false;
        }

        try {
            int patientId = Integer.parseInt(patientIdField.getText().trim());
            if (patientId <= 0) {
                showError("L'ID du patient doit être un nombre positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("L'ID du patient doit être un nombre valide : " + patientIdField.getText());
            return false;
        }

        return true;
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
     * Creates a Prescription object from the input fields.
     *
     * @param id The ID of the prescription (0 for new prescriptions).
     * @return A Prescription object with the input data.
     */
    private Prescription createPrescriptionFromInputs(int id) {
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();
        String adresse = adresseField.getText().trim();
        String gmail = gmailField.getText().trim();
        int patientId = Integer.parseInt(patientIdField.getText().trim());

        Prescription prescription = new Prescription();
        prescription.setId(id);
        prescription.setDateDeb(dateDebut);
        prescription.setDateFin(dateFin);
        prescription.setAdresse(adresse);
        prescription.setGmail(gmail);
        prescription.setPatientId(patientId);

        return prescription;
    }

    /**
     * Handles adding a new prescription.
     *
     * @param event The action event.
     */
    @FXML
    private void ajouterPrescription(ActionEvent event) {
        if (!validateInputs()) return;

        try {
            Prescription prescription = createPrescriptionFromInputs(0);

            if (patientService.getPatientById(prescription.getPatientId()) == null) {
                showError("Le patient avec l'ID " + prescription.getPatientId() + " n'existe pas.");
                return;
            }

            if (prescriptionService.hasDateConflict(prescription.getPatientId(),
                    prescription.getDateDeb().atStartOfDay(),
                    prescription.getDateFin().atStartOfDay())) {
                showError("Conflit de dates détecté pour ce patient entre " +
                        prescription.getDateDeb() + " et " + prescription.getDateFin() + ".");
                return;
            }

            prescription.setStatut("En cours");
            prescription.setArchived(false);
            prescriptionService.addPrescription(prescription);
            loadPrescriptions();
            clearFields();
            showInfo("Prescription ajoutée avec succès.");
        } catch (NumberFormatException e) {
            showError("L'ID du patient doit être un nombre valide : " + e.getMessage());
        } catch (Exception e) {
            showError("Erreur lors de l'ajout de la prescription : " + e.getMessage());
        }
    }

    /**
     * Handles updating an existing prescription.
     *
     * @param event The action event.
     */
    @FXML
    private void modifierPrescription(ActionEvent event) {
        if (selectedPrescription == null) {
            showError("Veuillez sélectionner une prescription à modifier.");
            return;
        }
        if (!validateInputs()) return;

        try {
            Prescription updated = createPrescriptionFromInputs(selectedPrescription.getId());

            if (patientService.getPatientById(updated.getPatientId()) == null) {
                showError("Le patient avec l'ID " + updated.getPatientId() + " n'existe pas.");
                return;
            }

            boolean otherConflicts = prescriptionService.getAllPrescriptions(false).stream()
                    .filter(p -> p.getId() != selectedPrescription.getId() && p.getPatientId() == updated.getPatientId())
                    .anyMatch(p -> (p.getDateDeb().atStartOfDay().isBefore(updated.getDateFin().atStartOfDay()) &&
                            p.getDateFin().atStartOfDay().isAfter(updated.getDateDeb().atStartOfDay())));

            if (otherConflicts) {
                showError("Conflit de dates détecté pour ce patient entre " +
                        updated.getDateDeb() + " et " + updated.getDateFin() + ".");
                return;
            }

            updated.setStatut(selectedPrescription.getStatut());
            updated.setArchived(selectedPrescription.isArchived());
            prescriptionService.updatePrescription(updated);
            loadPrescriptions();
            clearFields();
            showInfo("Prescription modifiée avec succès.");
        } catch (NumberFormatException e) {
            showError("L'ID du patient doit être un nombre valide : " + e.getMessage());
        } catch (Exception e) {
            showError("Erreur lors de la modification de la prescription : " + e.getMessage());
        }
    }

    /**
     * Handles deleting a selected prescription after confirmation.
     *
     * @param event The action event.
     */
    @FXML
    private void supprimerPrescription(ActionEvent event) {
        if (selectedPrescription == null) {
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
                    prescriptionService.deletePrescription(selectedPrescription.getId());
                    loadPrescriptions();
                    clearFields();
                    showInfo("Prescription supprimée avec succès.");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de la prescription : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Filters prescriptions based on the selected filter type and search text.
     */
    private void filtrerPrescriptions() {
        String filterType = filterComboBox.getSelectionModel().getSelectedItem();
        String filterText = filterField.getText().trim().toLowerCase();

        if (filterType == null || filterText.isEmpty()) {
            loadPrescriptions();
            return;
        }

        try {
            List<Prescription> filtered = prescriptionService.getAllPrescriptions(showArchived);

            switch (filterType) {
                case "Tout":
                    filtered = filtered.stream()
                            .filter(p -> (p.getAdresse() != null && p.getAdresse().toLowerCase().contains(filterText)) ||
                                    (p.getGmail() != null && p.getGmail().toLowerCase().contains(filterText)) ||
                                    (p.getStatut() != null && p.getStatut().toLowerCase().contains(filterText)))
                            .collect(Collectors.toList());
                    break;
                case "Date":
                    try {
                        LocalDate filterDate = LocalDate.parse(filterText, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        filtered = filtered.stream()
                                .filter(p -> p.getDateDeb() != null && p.getDateDeb().equals(filterDate))
                                .collect(Collectors.toList());
                    } catch (DateTimeParseException e) {
                        showError("Format de date invalide. Utilisez yyyy-MM-dd : " + e.getMessage());
                        return;
                    }
                    break;
                case "Adresse":
                    filtered = filtered.stream()
                            .filter(p -> p.getAdresse() != null && p.getAdresse().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
                case "Gmail":
                    filtered = filtered.stream()
                            .filter(p -> p.getGmail() != null && p.getGmail().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
                case "Statut":
                    filtered = filtered.stream()
                            .filter(p -> p.getStatut() != null && p.getStatut().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());
                    break;
            }

            currentPrescriptions = filtered;
            currentPage = 1; // Reset to first page after filtering
            refreshCards();
            showInfo("Filtrage appliqué : " + filtered.size() + " résultats");
        } catch (Exception e) {
            showError("Erreur lors du filtrage : " + e.getMessage());
        }
    }

    /**
     * Refreshes the displayed prescription cards based on the current page.
     */
    private void refreshCards() {
        prescriptionsContainer.getChildren().clear();
        selectedPrescription = null;

        if (currentPrescriptions.isEmpty()) {
            Text noPrescriptionsText = new Text("Aucune prescription trouvée.");
            noPrescriptionsText.getStyleClass().add("no-data-text");
            prescriptionsContainer.getChildren().add(noPrescriptionsText);
            updatePaginationLabel();
            return;
        }

        int startIndex = (currentPage - 1) * prescriptionsPerPage;
        int endIndex = Math.min(startIndex + prescriptionsPerPage, currentPrescriptions.size());

        List<Prescription> prescriptionsToShow = currentPrescriptions.subList(startIndex, endIndex);

        for (Prescription prescription : prescriptionsToShow) {
            VBox card = createPrescriptionCard(prescription);
            prescriptionsContainer.getChildren().add(card);
        }

        updatePaginationLabel();
    }

    /**
     * Sorts prescriptions based on the selected field and order.
     *
     * @param event The action event.
     */
    @FXML
    private void trierPrescriptions(ActionEvent event) {
        if (currentPrescriptions == null || currentPrescriptions.isEmpty()) {
            showInfo("Aucune prescription à trier.");
            return;
        }

        String sortBy = sortField.getValue();
        sortAscending = "Ascendant".equals(sortOrder.getValue());

        Comparator<Prescription> comparator = switch (sortBy) {
            case "Date Début" -> Comparator.comparing(Prescription::getDateDeb, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Patient ID" -> Comparator.comparing(Prescription::getPatientId);
            default -> Comparator.comparing(Prescription::getStatut, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (!sortAscending) {
            comparator = comparator.reversed();
        }

        currentPrescriptions.sort(comparator);
        currentPage = 1; // Reset to first page after sorting
        refreshCards();

        String message = "Prescriptions triées par " + sortBy + " (" + sortOrder.getValue().toLowerCase() + ").";
        showInfo(message);
    }

    /**
     * Navigates to the previous page of prescriptions.
     *
     * @param event The action event.
     */
    @FXML
    private void pagePrecedente(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            refreshCards();
            showInfo("Page précédente : " + currentPage);
        } else {
            showInfo("Vous êtes déjà sur la première page.");
        }
    }

    /**
     * Navigates to the next page of prescriptions.
     *
     * @param event The action event.
     */
    @FXML
    private void pageSuivante(ActionEvent event) {
        int totalPages = (int) Math.ceil((double) currentPrescriptions.size() / prescriptionsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            refreshCards();
            showInfo("Page suivante : " + currentPage);
        } else {
            showInfo("Vous êtes déjà sur la dernière page.");
        }
    }

    /**
     * Archives all expired prescriptions.
     *
     * @param event The action event.
     */
    @FXML
    private void archiverPrescriptionsExpirées(ActionEvent event) {
        LocalDate now = LocalDate.now();
        List<Prescription> expired = prescriptionService.getAllPrescriptions(false)
                .stream()
                .filter(p -> !p.isArchived() && p.getDateFin() != null && p.getDateFin().isBefore(now))
                .collect(Collectors.toList());

        if (expired.isEmpty()) {
            showInfo("Aucune prescription expirée à archiver.");
            return;
        }

        for (Prescription p : expired) {
            prescriptionService.archivePrescription(p.getId());
        }

        loadPrescriptions();
        showInfo(expired.size() + " prescriptions expirées archivées.");
    }

    /**
     * Toggles the view to show or hide archived prescriptions.
     *
     * @param event The action event.
     */
    @FXML
    private void basculerVueArchivées(ActionEvent event) {
        showArchived = archiveToggle.isSelected();
        currentPage = 1; // Reset to first page
        loadPrescriptions();
        String message = showArchived ? "Affichage des prescriptions archivées." : "Affichage des prescriptions actives.";
        showInfo(message);
    }

    /**
     * Displays statistics about prescriptions.
     *
     * @param event The action event.
     */
    @FXML
    private void afficherStatistiques(ActionEvent event) {
        try {
            List<Prescription> allPrescriptions = prescriptionService.getAllPrescriptions(true);
            long activeCount = allPrescriptions.stream().filter(p -> !p.isArchived()).count();
            long archivedCount = allPrescriptions.stream().filter(Prescription::isArchived).count();

            double averageDurationDays = allPrescriptions.stream()
                    .filter(p -> p.getDateDeb() != null && p.getDateFin() != null)
                    .mapToLong(p -> java.time.Duration.between(p.getDateDeb().atStartOfDay(), p.getDateFin().atStartOfDay()).toDays())
                    .average()
                    .orElse(0.0);

            long enCours = allPrescriptions.stream().filter(p -> "En cours".equals(p.getStatut())).count();
            long traite = allPrescriptions.stream().filter(p -> "Traité".equals(p.getStatut())).count();
            long resolu = allPrescriptions.stream().filter(p -> "Résolu".equals(p.getStatut())).count();

            Map<Integer, Long> prescriptionsParPatient = allPrescriptions.stream()
                    .collect(Collectors.groupingBy(Prescription::getPatientId, Collectors.counting()));

            String topPatients = prescriptionsParPatient.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> "Patient ID " + entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));

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

            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques des prescriptions");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText(stats.toString());
            statsAlert.showAndWait();

            if (statsLabel != null) {
                statsLabel.setText("Total: " + activeCount + " actives, " + archivedCount + " archivées");
                statsPanel.setVisible(true);
            }
        } catch (Exception e) {
            showError("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    /**
     * Hides the statistics panel.
     *
     * @param event The action event.
     */
    @FXML
    private void cacherStatistiques(ActionEvent event) {
        if (statsPanel != null) {
            statsPanel.setVisible(false);
            showInfo("Statistiques masquées.");
        }
    }

    /**
     * Changes the status of the selected prescription.
     *
     * @param event The action event.
     */
    @FXML
    private void changerStatut(ActionEvent event) {
        if (selectedPrescription == null) {
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
        statusComboBox.setValue(selectedPrescription.getStatut());

        dialog.getDialogPane().setContent(statusComboBox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return statusComboBox.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                selectedPrescription.setStatut(newStatus);
                prescriptionService.updatePrescription(selectedPrescription);
                loadPrescriptions();
                showInfo("Statut modifié avec succès.");
            } catch (Exception e) {
                showError("Erreur lors de la modification du statut : " + e.getMessage());
            }
        });
    }

    /**
     * Exports prescriptions to a PDF file.
     *
     * @param event The action event.
     */
    @FXML
    private void exporterPrescriptions(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        fileChooser.setInitialFileName("prescriptions_export_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");

        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".pdf")) {
                    filePath += ".pdf";
                }
                prescriptionService.exportPrescriptionsToPDF(filePath);
                showInfo("Prescriptions exportées avec succès vers " + filePath);
            } catch (Exception e) {
                showError("Erreur lors de l'exportation : " + e.getMessage());
            }
        } else {
            showInfo("Exportation annulée.");
        }
    }

    /**
     * Opens a new window for adding a prescription.
     *
     * @param event The action event.
     */
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

    /**
     * Clears all input fields and deselects the current prescription.
     */
    private void clearFields() {
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        adresseField.clear();
        gmailField.clear();
        patientIdField.clear();
        selectedPrescription = null;
    }

    /**
     * Displays an error message in an alert dialog.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays an information message in an alert dialog.
     *
     * @param message The information message to display.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Resets all fields and filters to their default state.
     *
     * @param event The action event.
     */
    @FXML
    private void reinitialiserChamps(ActionEvent event) {
        filterField.clear();
        filterComboBox.getSelectionModel().selectFirst();
        sortField.setValue("Statut");
        sortOrder.setValue("Ascendant");
        currentPage = 1;
        clearFields();
        loadPrescriptions();
        showInfo("Champs et filtres réinitialisés.");
    }
}