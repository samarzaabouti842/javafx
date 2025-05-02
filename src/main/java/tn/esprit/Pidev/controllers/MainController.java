package tn.esprit.Pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private void handlePatient(ActionEvent event) {
        ouvrirFenetre("/fxml/patientDashboard.fxml", "Vue Patient");
    }

    @FXML
    private void handlePrescription(ActionEvent event) {
        ouvrirFenetre("/fxml/interfacepresc.fxml", "Gestion des Prescriptions");
    }

    @FXML
    private void handleTraitement(ActionEvent event) {
        ouvrirFenetre("/fxml/interfacetraite.fxml", "Gestion des Traitements");
    }

    @FXML
    private void handleAide(ActionEvent event) {
        // Display a help message
        showInfo("Aide pour MaSanté\n\n" +
                "Vue Patient : Gérer les informations des patients.\n" +
                "Prescription : Créer et gérer des prescriptions.\n" +
                "Traitement : Gérer les traitements associés aux prescriptions.\n" +
                "Déconnexion : Quitter l'application.");
    }

    @FXML
    private void handleDeconnexion(ActionEvent event) {
        // Close the application
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void ouvrirFenetre(String fxmlFile, String title) {
        try {
            logger.info("Tentative de chargement du fichier FXML : {}", fxmlFile);
            URL resourceUrl = getClass().getResource(fxmlFile);
            if (resourceUrl == null) {
                throw new IOException("Resource not found: " + fxmlFile);
            }
            logger.info("Resource URL: {}", resourceUrl);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de l'interface : {}", e.getMessage(), e);
            showError("Erreur lors du chargement de l'interface : " + e.getMessage());
        }
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