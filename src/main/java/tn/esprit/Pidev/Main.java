package tn.esprit.Pidev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le fichier FXML principal
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/patientprescription.fxml"));
        
        // Créer la scène
        Scene scene = new Scene(root);
        
        // Configurer la fenêtre principale
        primaryStage.setTitle("Ma Santé - Gestion des Prescriptions");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
