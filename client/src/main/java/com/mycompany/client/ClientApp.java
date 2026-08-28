package com.mycompany.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto di ingresso dell'applicazione client JavaFX.
 *
 * Mostra la schermata di login. 
 * Il passaggio alla schermata principale avviene tramite
 * {@link LoginController} al termine dell'autenticazione.
 */
public class ClientApp extends Application {

    /**
     * Punto di ingresso JavaFX. Carica e mostra la schermata di login.
     *
     * @param primaryStage finestra primaria fornita dal framework JavaFX
     * @throws Exception se il caricamento dell'FXML fallisce
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        primaryStage.setTitle("GuessTheWord");
        primaryStage.setScene(new Scene(root, 400, 320));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Avvia l'applicazione JavaFX.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        launch(args);
    }
}