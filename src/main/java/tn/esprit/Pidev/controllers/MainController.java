package tn.esprit.Pidev.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    void handlePatient(ActionEvent event) {
        ouvrirFenetre("/fxml/patientDashboard.fxml");
    }

    @FXML
    void handlePrescription(ActionEvent event) {
        ouvrirFenetre("/fxml/interfacepresc.fxml");
    }

    @FXML
    void handleTraitement(ActionEvent event) {
        ouvrirFenetre("/fxml/interfacetraite.fxml");
    }

    @FXML
    void handleAide(ActionEvent event) {
        showInfo("Pour toute assistance, veuillez contacter le support technique au 123-456-7890 ou par email à support@masante.com");
    }

    @FXML
    void handleDeconnexion(ActionEvent event) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Déconnexion");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.close();
            }
        });
    }

    private void ouvrirFenetre(String fxmlPath) {
        try {
            LOGGER.info("Tentative de chargement du fichier FXML : " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                String errorMsg = "Fichier FXML introuvable : " + fxmlPath;
                LOGGER.severe(errorMsg);
                showError(errorMsg);
                return;
            }

            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("MaSanté - " + fxmlPath.substring(fxmlPath.lastIndexOf("/") + 1, fxmlPath.lastIndexOf(".")));
            stage.show();
            LOGGER.info("Interface chargée avec succès : " + fxmlPath);
        } catch (IOException e) {
            String errorMsg = "Erreur lors du chargement de l'interface : " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, e);
            showError(errorMsg);
        }
    }

    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, javafx.scene.control.ButtonType.OK);
            alert.setTitle("Erreur");
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, javafx.scene.control.ButtonType.OK);
            alert.setTitle("Information");
            alert.showAndWait();
        });
    }
}