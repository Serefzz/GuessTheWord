package com.mycompany.server;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Controller della schermata di login dell'amministratore.
 *
 * Verifica le credenziali tramite {@link UtenteDAO} e controlla
 * che l'utente abbia ruolo {@code ADMIN}. In caso di successo carica
 * il pannello admin ({@code admin.fxml}); altrimenti mostra un messaggio
 * di errore e svuota il campo username e password.
 *
 * FXML associato: {@code /adminlogin.fxml} (server)
 */
public class AdminLoginController {

    @FXML private Label titoloLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;

    private final UtenteDAO utenteDAO = new UtenteDAO();
    
    @FXML
    public void initialize() {
        Font fontPersonalizzato = Font.loadFont(getClass().getResourceAsStream("/fonts/ZagDropsThin.ttf"), 54);
        if (fontPersonalizzato != null) {
            titoloLabel.setFont(fontPersonalizzato);
        }
    }
    
    @FXML
    private void vaiSuccessivo(KeyEvent event) {
        if (usernameField.getText().trim().equals("") && event.getCode() == KeyCode.ENTER) {
            erroreLabel.setText("Inserisci username e password.");
        } else if (event.getCode() == KeyCode.ENTER) {
            passwordField.requestFocus();
        }
    }
    
    @FXML
    private void verificaLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            erroreLabel.setText("Inserisci username e password.");
            return;
        }

        if (utenteDAO.verificaLogin(username, password) && utenteDAO.isAdmin(username)) {
            apriPannelloAdmin();
        } else {
            erroreLabel.setText("Credenziali errate o utente non amministratore.");
            usernameField.clear();
            passwordField.clear();
        }
    }

    /**
     * Carica e mostra il pannello amministratore ({@code admin.fxml}).
     * Sostituisce la scena corrente nella stessa finestra.
     */
    private void apriPannelloAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("GuessTheWord — Admin");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo-GuessTheWord.png")));
            stage.setWidth(920);
            stage.setHeight(680);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) {
            erroreLabel.setText("Errore caricamento pannello");
            System.out.println(e.getMessage() + '\n' + e.getClass().toString());
            e.printStackTrace();
        }
    }
    
}
