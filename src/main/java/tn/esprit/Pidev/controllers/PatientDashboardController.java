package tn.esprit.Pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.esprit.Pidev.Models.Prescription;
import tn.esprit.Pidev.Models.Traitement;
import tn.esprit.Pidev.Services.PrescriptionService;
import tn.esprit.Pidev.Services.TraitementService;
import tn.esprit.Pidev.Models.Patient;
import tn.esprit.Pidev.Services.PatientService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PatientDashboardController implements Initializable {

    @FXML private Text welcomeText;
    @FXML private VBox prescriptionsContainer;
    @FXML private Button logoutButton;

    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final TraitementService traitementService = new TraitementService();
    private final PatientService patientService = new PatientService();
    private Patient currentPatient; // This would typically be set after login
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // For this example, we'll hardcode the patient ID (e.g., 1).
        // In a real application, this would come from a login session.
        currentPatient = patientService.getPatientById(1);
        if (currentPatient == null) {
            showError("Erreur: Patient non trouvé.");
            return;
        }

        // Set welcome message
        welcomeText.setText("Bonjour, " + currentPatient.getPrenom());

        // Load prescriptions
        loadPrescriptions();
    }

    private void loadPrescriptions() {
        prescriptionsContainer.getChildren().clear();

        try {
            // Fetch prescriptions for the current patient
            List<Prescription> prescriptions = prescriptionService.getAllPrescriptions()
                    .stream()
                    .filter(p -> p.getPatientId() == currentPatient.getId() && !p.isArchived())
                    .collect(Collectors.toList());

            if (prescriptions.isEmpty()) {
                Text noPrescriptionsText = new Text("Aucune prescription en cours.");
                noPrescriptionsText.getStyleClass().add("no-data-text");
                prescriptionsContainer.getChildren().add(noPrescriptionsText);
                return;
            }

            // For each prescription, create a card
            for (Prescription prescription : prescriptions) {
                VBox card = new VBox();
                card.getStyleClass().add("prescription-card");
                card.setSpacing(10);

                // Prescription header
                Text dateRange = new Text(prescription.getDateDeb().format(dateFormatter) + " - " +
                        prescription.getDateFin().format(dateFormatter));
                dateRange.getStyleClass().add("card-title");

                Text status = new Text("Statut: " + prescription.getStatut());
                status.getStyleClass().add("card-status");
                switch (prescription.getStatut()) {
                    case "En cours":
                        status.setStyle("-fx-fill: #ff8c00;");
                        break;
                    case "Traité":
                        status.setStyle("-fx-fill: #008000;");
                        break;
                    case "Résolu":
                        status.setStyle("-fx-fill: #0000ff;");
                        break;
                }

                // Fetch associated traitements
                List<Traitement> traitements = traitementService.getTraitementsByPrescription(prescription.getId());
                VBox traitementsBox = new VBox();
                traitementsBox.setSpacing(5);

                for (Traitement traitement : traitements) {
                    HBox traitementRow = new HBox();
                    traitementRow.setSpacing(10);

                    Text medicament = new Text(traitement.getMedicament() + " - " + traitement.getDose());
                    medicament.getStyleClass().add("card-detail");

                    Text description = new Text(traitement.getDescription());
                    description.getStyleClass().add("card-description");

                    traitementRow.getChildren().addAll(medicament, description);
                    traitementsBox.getChildren().add(traitementRow);
                }

                card.getChildren().addAll(dateRange, status, traitementsBox);
                prescriptionsContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des prescriptions : " + e.getMessage());
        }
    }

    @FXML
    private void logout() {
        // In a real application, this would clear the session and redirect to the login screen
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
        showInfo("Vous avez été déconnecté.");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
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
