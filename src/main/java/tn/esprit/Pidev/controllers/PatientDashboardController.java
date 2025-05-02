package tn.esprit.Pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.Services.PrescriptionService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PatientDashboardController {

    @FXML private Text welcomeText;
    @FXML private HBox prescriptionsContainer;
    @FXML private Button logoutButton;

    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Assume the patient ID is passed or retrieved from a login session
    private final int loggedInPatientId = 1; // Replace with actual patient ID from login

    @FXML
    private void initialize() {
        // Set welcome text (replace "Thomas" with the actual patient name)
        welcomeText.setText("Bonjour, Thomas");

        // Load prescriptions for the logged-in patient
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        prescriptionsContainer.getChildren().clear();

        try {
            // Fetch prescriptions for the logged-in patient
            List<Prescription> patientPrescriptions = prescriptionService.getAllPrescriptions(false)
                    .stream()
                    .filter(p -> p.getPatientId() == loggedInPatientId && !p.isArchived())
                    .collect(Collectors.toList());

            if (patientPrescriptions.isEmpty()) {
                Text noPrescriptionsText = new Text("Aucune prescription en cours.");
                noPrescriptionsText.getStyleClass().add("no-data-text");
                prescriptionsContainer.getChildren().add(noPrescriptionsText);
                return;
            }

            for (Prescription prescription : patientPrescriptions) {
                VBox card = createPrescriptionCard(prescription);
                prescriptionsContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des prescriptions : " + e.getMessage());
        }
    }

    private VBox createPrescriptionCard(Prescription prescription) {
        VBox card = new VBox();
        card.getStyleClass().add("prescription-card");
        card.setSpacing(10);

        // Date range
        Text dateRange = new Text(prescription.getDateDeb().format(dateFormatter) + " - " +
                prescription.getDateFin().format(dateFormatter));
        dateRange.getStyleClass().add("card-title");

        // Address
        Text adresse = new Text("Adresse: " + prescription.getAdresse());
        adresse.getStyleClass().add("card-detail");

        // Gmail
        Text gmail = new Text("Gmail: " + prescription.getGmail());
        gmail.getStyleClass().add("card-detail");

        // Status
        Text status = new Text("Statut: " + prescription.getStatut());
        status.getStyleClass().add("card-status");
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
                status.setStyle("-fx-fill: #000000;");
                break;
        }

        // Buttons for "Suivant" and "Voir Archiv"
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        Button suivantButton = new Button("Suivant");
        suivantButton.getStyleClass().add("card-action-button");
        suivantButton.setOnAction(e -> handleSuivant(prescription));

        Button voirArchivButton = new Button("Voir Archiv");
        voirArchivButton.getStyleClass().add("card-action-button-secondary");
        voirArchivButton.setOnAction(e -> handleVoirArchiv(prescription));

        buttonBox.getChildren().addAll(suivantButton, voirArchivButton);

        card.getChildren().addAll(dateRange, adresse, gmail, status, buttonBox);

        return card;
    }

    private void handleSuivant(Prescription prescription) {
        List<Prescription> patientPrescriptions = prescriptionService.getAllPrescriptions(false)
                .stream()
                .filter(p -> p.getPatientId() == loggedInPatientId && !p.isArchived())
                .collect(Collectors.toList());

        int index = patientPrescriptions.indexOf(prescription);
        if (index + 1 < patientPrescriptions.size()) {
            Prescription nextPrescription = patientPrescriptions.get(index + 1);
            // Optionally, scroll to the next card or highlight it
            showInfo("Prescription suivante affichée.");
            loadPrescriptions(); // Reload to reflect any changes
        } else {
            showInfo("C'est la dernière prescription.");
        }
    }

    private void handleVoirArchiv(Prescription prescription) {
        if (!prescription.isArchived()) {
            prescriptionService.archivePrescription(prescription.getId());
            showInfo("Prescription archivée avec succès.");
            loadPrescriptions();
        } else {
            showInfo("Cette prescription est déjà archivée.");
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        // Implement logout logic (e.g., redirect to login screen)
        showInfo("Déconnexion réussie.");
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
}