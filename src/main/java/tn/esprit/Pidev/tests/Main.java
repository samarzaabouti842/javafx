package tn.esprit.Pidev.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le fichier FXML depuis le dossier /fxml dans resources
            URL fxmlLocation = getClass().getResource("/fxml/main.fxml");
            if (fxmlLocation == null) {
                System.err.println("Erreur : Fichier main.fxml introuvable dans /fxml !");
                throw new IOException("Fichier main.fxml introuvable");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            primaryStage.setTitle("Application de Gestion Médicale");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'application : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}