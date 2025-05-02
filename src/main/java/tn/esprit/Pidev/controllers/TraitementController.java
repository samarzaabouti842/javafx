package tn.esprit.Pidev.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import tn.esprit.Pidev.Models.Traitement;
import tn.esprit.Pidev.Services.PrescriptionService;
import tn.esprit.Pidev.Services.TraitementService;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing treatments in the JavaFX UI.
 * Handles displaying treatments as cards, filtering, OCR extraction, statistics, and CRUD operations.
 */
public class TraitementController implements Initializable {

    @FXML private ComboBox<Integer> prescriptionComboBox;
    @FXML private TextField medicamentField;
    @FXML private TextField doseField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> statutField; // Changed to ComboBox for predefined status values
    @FXML private DatePicker datePicker;
    @FXML private CheckBox completedCheckBox;
    @FXML private CheckBox archivedCheckBox;
    @FXML private TextField filterField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private FlowPane traitementsContainer; // Changed from HBox to FlowPane for better layout
    @FXML private TextArea ocrResultArea;
    @FXML private TextArea statsLabel;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button archiverButton;

    private final TraitementService traitementService;
    private final PrescriptionService prescriptionService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private Traitement selectedTraitement;
    private List<Traitement> currentTraitements;
    private static final List<String> VALID_STATUSES = Arrays.asList("En cours", "Terminé", "Annulé");

    public TraitementController() {
        this.traitementService = new TraitementService();
        this.prescriptionService = new PrescriptionService();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilterComboBox();
        setupStatutComboBox();
        loadPrescriptions();
        loadTraitements();
        setupSearchAndFilter();
        displayStatistics();
        setupButtonStates();
    }

    /**
     * Sets up the filter ComboBox with available options.
     */
    private void setupFilterComboBox() {
        if (filterComboBox == null) {
            showError("Erreur d'initialisation : ComboBox de filtrage non trouvé.");
            return;
        }
        filterComboBox.getItems().addAll("Tout", "En cours", "Terminé", "Annulé");
        filterComboBox.setValue("Tout");
    }

    /**
     * Sets up the status ComboBox with predefined status values.
     */
    private void setupStatutComboBox() {
        if (statutField == null) {
            showError("Erreur d'initialisation : ComboBox de statut non trouvé.");
            return;
        }
        statutField.getItems().addAll(VALID_STATUSES);
        statutField.setValue("En cours"); // Default value
    }

    /**
     * Loads prescriptions into the ComboBox for selection.
     */
    private void loadPrescriptions() {
        int currentPatientId = 1; // This could be dynamic based on user context
        try {
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(currentPatientId, false);
            if (prescriptions.isEmpty()) {
                showInfo("Aucune prescription active trouvée pour le patient ID " + currentPatientId + ".");
                prescriptionComboBox.getItems().clear();
                return;
            }
            prescriptionComboBox.getItems().setAll(
                    prescriptions.stream().map(Prescription::getId).collect(Collectors.toList())
            );
            if (!prescriptionComboBox.getItems().isEmpty()) {
                prescriptionComboBox.setValue(prescriptionComboBox.getItems().get(0));
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des prescriptions : " + e.getMessage());
        }
    }

    /**
     * Loads treatments based on the selected prescription and displays them as cards.
     */
    private void loadTraitements() {
        if (traitementsContainer == null) {
            showError("Erreur d'initialisation : Conteneur de traitements non trouvé.");
            return;
        }
        traitementsContainer.getChildren().clear();
        selectedTraitement = null;
        updateButtonStates();

        Integer selectedPrescriptionId = prescriptionComboBox.getValue();
        if (selectedPrescriptionId == null) {
            Text noTraitementsText = new Text("Aucune prescription sélectionnée.");
            noTraitementsText.getStyleClass().add("no-data-text");
            traitementsContainer.getChildren().add(noTraitementsText);
            return;
        }

        try {
            currentTraitements = traitementService.getTraitementsByPrescription(
                    selectedPrescriptionId, archivedCheckBox.isSelected()
            );

            if (currentTraitements.isEmpty()) {
                Text noTraitementsText = new Text("Aucun traitement trouvé pour la prescription ID " + selectedPrescriptionId + ".");
                noTraitementsText.getStyleClass().add("no-data-text");
                traitementsContainer.getChildren().add(noTraitementsText);
                return;
            }

            for (Traitement traitement : currentTraitements) {
                VBox card = createTraitementCard(traitement);
                traitementsContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des traitements : " + e.getMessage());
        }
    }

    /**
     * Creates a card (VBox) for a treatment with details and action buttons.
     *
     * @param traitement The treatment to display.
     * @return A VBox representing the treatment card.
     */
    private VBox createTraitementCard(Traitement traitement) {
        Objects.requireNonNull(traitement, "Traitement cannot be null");

        VBox card = new VBox();
        card.getStyleClass().add("traitement-card");
        card.setPrefWidth(600);
        card.setPrefHeight(400);
        card.setSpacing(10);

        // Header with archived badge
        HBox headerBox = new HBox();
        headerBox.setSpacing(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Text medicament = new Text("Médicament: " + (traitement.getMedicament() != null ? traitement.getMedicament() : "N/A"));
        medicament.getStyleClass().add("card-title");

        if (traitement.isArchived()) {
            Text archivedBadge = new Text("Archivé");
            archivedBadge.getStyleClass().add("archived-badge");
            headerBox.getChildren().addAll(medicament, archivedBadge);
        } else {
            headerBox.getChildren().add(medicament);
        }

        // Details
        Text dose = new Text("Dose: " + (traitement.getDose() != null ? traitement.getDose() : "N/A"));
        dose.getStyleClass().add("card-detail");

        Text description = new Text("Description: " + (traitement.getDescription() != null ? traitement.getDescription() : "N/A"));
        description.getStyleClass().add("card-detail");

        Text status = new Text("Statut: " + (traitement.getStatut() != null ? traitement.getStatut() : "N/A"));
        status.getStyleClass().add("card-status");
        if (traitement.getStatut() != null) {
            switch (traitement.getStatut().toLowerCase()) {
                case "en cours":
                    status.getStyleClass().add("status-en-cours");
                    break;
                case "terminé":
                    status.getStyleClass().add("status-terminé");
                    break;
                case "annulé":
                    status.getStyleClass().add("status-annulé");
                    break;
                default:
                    status.getStyleClass().add("status-default");
                    break;
            }
        }

        Text date = new Text("Date: " + (traitement.getDate() != null ? traitement.getDate().format(dateFormatter) : "N/A"));
        date.getStyleClass().add("card-detail");

        // Action Buttons
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button editButton = new Button("Modifier");
        editButton.getStyleClass().add("card-action-button");
        editButton.setOnAction(e -> selectTraitement(traitement, card));

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().add("card-action-button-secondary");
        deleteButton.setOnAction(e -> {
            selectedTraitement = traitement;
            supprimerTraitement(null);
        });

        buttonBox.getChildren().addAll(editButton, deleteButton);

        card.getChildren().addAll(headerBox, dose, description, status, date, buttonBox);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.5, 0, 0);"));
        card.setOnMouseExited(e -> {
            if (selectedTraitement != traitement) {
                card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.3, 0, 0);");
            }
        });

        card.setOnMouseClicked(event -> selectTraitement(traitement, card));

        return card;
    }

    /**
     * Selects a treatment card and populates the input fields with its data.
     *
     * @param traitement The treatment to select.
     * @param card The card (VBox) representing the treatment.
     */
    private void selectTraitement(Traitement traitement, VBox card) {
        for (var node : traitementsContainer.getChildren()) {
            node.setStyle("-fx-border-color: #d3d3d3;");
        }

        card.setStyle("-fx-border-color: #3498db; -fx-border-width: 2;");
        selectedTraitement = traitement;
        updateButtonStates();

        prescriptionComboBox.setValue(traitement.getId_p());
        medicamentField.setText(traitement.getMedicament() != null ? traitement.getMedicament() : "");
        doseField.setText(traitement.getDose() != null ? traitement.getDose() : "");
        descriptionField.setText(traitement.getDescription() != null ? traitement.getDescription() : "");
        statutField.setValue(traitement.getStatut() != null ? traitement.getStatut() : "En cours");
        datePicker.setValue(traitement.getDate());
        completedCheckBox.setSelected(traitement.isCompleted());
        archivedCheckBox.setSelected(traitement.isArchived());
    }

    /**
     * Sets up listeners for filter field, ComboBox, and other controls to trigger filtering and updates.
     */
    private void setupSearchAndFilter() {
        if (filterField == null || filterComboBox == null || prescriptionComboBox == null || archivedCheckBox == null) {
            showError("Erreur d'initialisation : Champs de filtrage non trouvés.");
            return;
        }
        filterField.textProperty().addListener((obs, oldValue, newValue) -> filterTraitements());
        filterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> filterTraitements());
        archivedCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> loadTraitements());
        prescriptionComboBox.valueProperty().addListener((obs, oldValue, newValue) -> loadTraitements());
    }

    /**
     * Filters treatments based on the selected filter type and search text.
     */
    private void filterTraitements() {
        String keyword = filterField.getText() != null ? filterField.getText().trim() : "";
        String filterType = filterComboBox.getValue() != null ? filterComboBox.getValue() : "Tout";
        Map<String, String> filters = new HashMap<>();
        if (!"Tout".equals(filterType)) {
            filters.put("statut", filterType);
        }

        try {
            List<Traitement> filteredTraitements = traitementService.searchTraitements(keyword, filters);
            Integer selectedPrescriptionId = prescriptionComboBox.getValue();
            if (selectedPrescriptionId != null) {
                filteredTraitements = filteredTraitements.stream()
                        .filter(t -> t.getId_p() == selectedPrescriptionId)
                        .collect(Collectors.toList());
            }

            currentTraitements = filteredTraitements;
            refreshCards();
            showInfo("Filtrage appliqué : " + filteredTraitements.size() + " résultats");
        } catch (Exception e) {
            showError("Erreur lors du filtrage des traitements : " + e.getMessage());
        }
    }

    /**
     * Refreshes the displayed treatment cards.
     */
    private void refreshCards() {
        traitementsContainer.getChildren().clear();
        selectedTraitement = null;
        updateButtonStates();

        if (currentTraitements == null || currentTraitements.isEmpty()) {
            Text noTraitementsText = new Text("Aucun traitement trouvé.");
            noTraitementsText.getStyleClass().add("no-data-text");
            traitementsContainer.getChildren().add(noTraitementsText);
            return;
        }

        for (Traitement traitement : currentTraitements) {
            VBox card = createTraitementCard(traitement);
            traitementsContainer.getChildren().add(card);
        }
    }

    /**
     * Displays statistics about treatments in the statsLabel TextArea.
     */
    private void displayStatistics() {
        if (statsLabel == null) {
            showError("Erreur d'initialisation : Zone de statistiques non trouvée.");
            return;
        }

        try {
            Map<String, Object> stats = traitementService.getDetailedStatistics();
            StringBuilder statsText = new StringBuilder("Statistiques des Traitements :\n\n");

            // Monthly Distribution
            statsText.append("Distribution Mensuelle :\n");
            Object monthlyDistObj = stats.getOrDefault("monthly_distribution", new HashMap<>());
            if (monthlyDistObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> monthlyDistribution = (Map<String, Long>) monthlyDistObj;
                if (monthlyDistribution.isEmpty()) {
                    statsText.append("Aucune donnée disponible.\n");
                } else {
                    monthlyDistribution.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> statsText.append(entry.getKey())
                                    .append(": ")
                                    .append(entry.getValue())
                                    .append(" traitement(s)\n"));
                }
            } else {
                statsText.append("Aucune donnée de distribution mensuelle disponible.\n");
            }

            // Medication Frequency
            statsText.append("\nFréquence des Médicaments :\n");
            Object medFreqObj = stats.getOrDefault("medication_frequency", new HashMap<>());
            if (medFreqObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Long> medicationFrequency = (Map<String, Long>) medFreqObj;
                if (medicationFrequency.isEmpty()) {
                    statsText.append("Aucune donnée disponible.\n");
                } else {
                    medicationFrequency.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(10) // Limit to top 10 for brevity
                            .forEach(entry -> statsText.append(entry.getKey())
                                    .append(": ")
                                    .append(entry.getValue())
                                    .append(" fois\n"));
                }
            } else {
                statsText.append("Aucune donnée de fréquence des médicaments disponible.\n");
            }

            statsLabel.setText(statsText.toString());
        } catch (Exception e) {
            showError("Erreur lors de l'affichage des statistiques : " + e.getMessage());
        }
    }

    /**
     * Validates the input fields for adding or updating a treatment.
     *
     * @return True if inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        Integer prescriptionId = prescriptionComboBox.getValue();
        if (prescriptionId == null) {
            showError("Veuillez sélectionner une prescription.");
            return false;
        }

        if (medicamentField.getText() == null || medicamentField.getText().trim().isEmpty()) {
            showError("Le champ Médicament est obligatoire.");
            return false;
        }

        if (doseField.getText() == null || doseField.getText().trim().isEmpty()) {
            showError("Le champ Dose est obligatoire.");
            return false;
        }

        String statut = statutField.getValue();
        if (statut == null || !VALID_STATUSES.contains(statut)) {
            showError("Le statut doit être l'un des suivants : " + String.join(", ", VALID_STATUSES) + ".");
            return false;
        }

        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("La date est obligatoire.");
            return false;
        }

        if (date.isBefore(LocalDate.now())) {
            showError("La date du traitement ne peut pas être dans le passé : " + date.format(dateFormatter));
            return false;
        }

        return true;
    }

    /**
     * Handles adding a new treatment.
     *
     * @param event The action event.
     */
    @FXML
    private void ajouterTraitement(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }

        try {
            Integer prescriptionId = prescriptionComboBox.getValue();
            Traitement traitement = new Traitement(
                    0,
                    prescriptionId,
                    medicamentField.getText().trim(),
                    doseField.getText().trim(),
                    descriptionField.getText() != null ? descriptionField.getText().trim() : "",
                    statutField.getValue(),
                    datePicker.getValue(),
                    completedCheckBox.isSelected(),
                    archivedCheckBox.isSelected()
            );

            traitementService.addTraitement(traitement);
            loadTraitements();
            reinitialiserChamps(null);
            showInfo("Traitement ajouté avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de l'ajout du traitement : " + e.getMessage());
        }
    }

    /**
     * Handles updating an existing treatment.
     *
     * @param event The action event.
     */
    @FXML
    private void modifierTraitement(ActionEvent event) {
        if (selectedTraitement == null) {
            showError("Veuillez sélectionner un traitement à modifier.");
            return;
        }

        if (!validateInputs()) {
            return;
        }

        try {
            Integer prescriptionId = prescriptionComboBox.getValue();
            selectedTraitement.setId_p(prescriptionId);
            selectedTraitement.setMedicament(medicamentField.getText().trim());
            selectedTraitement.setDose(doseField.getText().trim());
            selectedTraitement.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : "");
            selectedTraitement.setStatut(statutField.getValue());
            selectedTraitement.setDate(datePicker.getValue());
            selectedTraitement.setCompleted(completedCheckBox.isSelected());
            selectedTraitement.setArchived(archivedCheckBox.isSelected());

            traitementService.updateTraitement(selectedTraitement);
            loadTraitements();
            reinitialiserChamps(null);
            showInfo("Traitement modifié avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de la modification du traitement : " + e.getMessage());
        }
    }

    /**
     * Handles deleting a selected treatment after confirmation.
     *
     * @param event The action event (can be null if called from card button).
     */
    @FXML
    private void supprimerTraitement(ActionEvent event) {
        if (selectedTraitement == null) {
            showError("Veuillez sélectionner un traitement à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer ce traitement ?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    traitementService.deleteTraitement(selectedTraitement.getId());
                    loadTraitements();
                    reinitialiserChamps(null);
                    showInfo("Traitement supprimé avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression du traitement : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handles archiving a selected treatment.
     *
     * @param event The action event.
     */
    @FXML
    private void archiverTraitement(ActionEvent event) {
        if (selectedTraitement == null) {
            showError("Veuillez sélectionner un traitement à archiver.");
            return;
        }

        if (selectedTraitement.isArchived()) {
            showInfo("Ce traitement est déjà archivé.");
            return;
        }

        try {
            traitementService.archiveTraitement(selectedTraitement.getId());
            loadTraitements();
            reinitialiserChamps(null);
            showInfo("Traitement archivé avec succès !");
        } catch (Exception e) {
            showError("Erreur lors de l'archivage du traitement : " + e.getMessage());
        }
    }

    /**
     * Exports treatments to a PDF or CSV file.
     *
     * @param event The action event.
     */
    @FXML
    private void exporterTraitements(ActionEvent event) {
        if (currentTraitements == null || currentTraitements.isEmpty()) {
            showError("Aucun traitement à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les Traitements");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("traitements_export_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");

        Stage stage = (Stage) traitementsContainer.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String filePath = file.getAbsolutePath();
                String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
                if (!filePath.toLowerCase().endsWith("." + extension)) {
                    filePath += "." + extension;
                }

                // Create a final copy of filePath for use in lambda
                final String finalFilePath = filePath;

                // Run export in a background thread
                Task<Void> exportTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        traitementService.exportTraitements(extension, finalFilePath, progress -> {
                            Platform.runLater(() -> showInfo("Exportation en cours : " + String.format("%.0f%%", progress * 100)));
                        });
                        return null;
                    }
                };

                exportTask.setOnSucceeded(e -> {
                    showInfo("Traitements exportés avec succès vers " + finalFilePath + " !");
                });

                exportTask.setOnFailed(e -> {
                    showError("Erreur lors de l'exportation : " + exportTask.getException().getMessage());
                });

                new Thread(exportTask).start();
            } catch (Exception e) {
                showError("Erreur lors de l'exportation : " + e.getMessage());
            }
        } else {
            showInfo("Exportation annulée.");
        }
    }

    /**
     * Extracts text from an image or PDF file using OCR and displays it in the ocrResultArea.
     *
     * @param event The action event.
     */
    @FXML
    private void extraireTexteAvecOCR(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier pour OCR");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images et PDFs", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.pdf")
        );

        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        java.io.File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String filePath = file.getAbsolutePath();
            ocrResultArea.clear();
            showInfo("Traitement OCR en cours... Veuillez patienter.");

            // Run OCR in a background thread
            Task<String> ocrTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return traitementService.performOCR(filePath);
                }
            };

            ocrTask.setOnSucceeded(e -> {
                String extractedText = ocrTask.getValue();
                if (extractedText == null || extractedText.trim().isEmpty()) {
                    showError("Aucun texte n'a pu être extrait du fichier. Vérifiez que le fichier contient du texte lisible.");
                } else {
                    ocrResultArea.setText(extractedText);
                    showInfo("Texte extrait avec succès via OCR !");
                }
            });

            ocrTask.setOnFailed(e -> {
                Throwable ex = ocrTask.getException();
                if (ex instanceof UnsupportedOperationException) {
                    showError("OCR n'est pas disponible : " + ex.getMessage() +
                            "\n\nConseils :\n" +
                            "- Assurez-vous que Tesseract-OCR est installé (par exemple, à C:\\Program Files\\Tesseract-OCR sur Windows).\n" +
                            "- Vérifiez que le fichier 'fra.traineddata' est présent dans le dossier 'tessdata'.\n" +
                            "- Ajoutez Tesseract au PATH système si nécessaire.");
                } else {
                    showError("Erreur lors de l'extraction OCR : " + ex.getMessage());
                }
            });

            new Thread(ocrTask).start();
        } else {
            showInfo("Aucun fichier sélectionné pour l'OCR.");
        }
    }

    /**
     * Opens a new window for adding a treatment.
     *
     * @param event The action event.
     */
    @FXML
    private void ouvrirNouveauTraitement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/nouveauTraitement.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nouveau Traitement");
            stage.setScene(new Scene(root));

            stage.showAndWait();
            loadTraitements();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture du formulaire de nouveau traitement : " + e.getMessage());
        }
    }

    /**
     * Resets all fields and filters to their default state.
     *
     * @param event The action event (can be null).
     */
    @FXML
    private void reinitialiserChamps(ActionEvent event) {
        medicamentField.clear();
        doseField.clear();
        descriptionField.clear();
        statutField.setValue("En cours");
        datePicker.setValue(null);
        completedCheckBox.setSelected(false);
        archivedCheckBox.setSelected(false);
        filterField.clear();
        filterComboBox.setValue("Tout");
        if (!prescriptionComboBox.getItems().isEmpty()) {
            prescriptionComboBox.setValue(prescriptionComboBox.getItems().get(0));
        }
        selectedTraitement = null;
        ocrResultArea.clear();
        loadTraitements();
        showInfo("Champs réinitialisés.");
    }

    /**
     * Updates the state of action buttons based on whether a treatment is selected.
     */
    private void setupButtonStates() {
        if (modifierButton == null || supprimerButton == null || archiverButton == null) {
            showError("Erreur d'initialisation : Boutons d'action non trouvés.");
            return;
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean isSelected = selectedTraitement != null;
        modifierButton.setDisable(!isSelected);
        supprimerButton.setDisable(!isSelected);
        archiverButton.setDisable(!isSelected || (isSelected && selectedTraitement.isArchived()));
    }

    /**
     * Displays an information message in an alert dialog.
     *
     * @param message The information message to display.
     */
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Displays an error message in an alert dialog.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}