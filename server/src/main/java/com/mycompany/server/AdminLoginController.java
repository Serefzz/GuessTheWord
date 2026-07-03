package com.mycompany.server;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller della schermata di login dell'amministratore.
 * Verifica che l'utente esista nel DB e che abbia ruolo ADMIN.
 */
public class AdminLoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         erroreLabel;

    private final UtenteDAO utenteDAO = new UtenteDAO();

    @FXML
    private void handleLogin() {
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
            passwordField.clear();
        }
    }

    private void apriPannelloAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/admin.fxml")
            );
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("GuessTheWord — Pannello Admin");
            stage.setResizable(true);
            stage.setWidth(820);
            stage.setHeight(580);
            stage.centerOnScreen();
        } catch (Exception e) {
            erroreLabel.setText("Errore caricamento pannello: " + e.getMessage());
        }
    }
}