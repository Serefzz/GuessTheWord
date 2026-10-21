package com.mycompany.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
     * @param stage finestra primaria fornita dal framework JavaFX
     * @throws Exception se il caricamento dell'FXML fallisce
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gamelogin.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("GuessTheWord");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo-GuessTheWord.png")));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
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