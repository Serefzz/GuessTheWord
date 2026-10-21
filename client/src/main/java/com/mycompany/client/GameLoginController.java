package com.mycompany.client;

import com.mycompany.common.Messaggio;
import com.mycompany.common.Messaggio.Tipo;
import java.io.FileInputStream;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.InputStream;
import java.util.Properties;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Controller della schermata di login/registrazione del client.
 * FXML associato: {@code /login.fxml}
 */
public class GameLoginController {

    @FXML private Label titoloLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;
    @FXML private Button btnLogin;
    @FXML private Button btnRegistra;

    private ServerConnection connessioneOk;

    @FXML
    public void initialize() {
        Font fontPersonalizzato = Font.loadFont(getClass().getResourceAsStream("/fonts/ZagDropsThin.ttf"), 54);
        titoloLabel.setFont(fontPersonalizzato);
    }

    @FXML
    private void handleLogin() {
        autentica(Tipo.LOGIN);
    }

    @FXML
    private void handleRegistra() {
        autentica(Tipo.REGISTRAZIONE);
    }
    
    @FXML
    private void vaiSuccessivo(KeyEvent event) {
        if (usernameField.getText().trim().equals("") && event.getCode() == KeyCode.ENTER) {
            erroreLabel.setText("Inserisci username e password.");
        } else if (event.getCode() == KeyCode.ENTER) {
            passwordField.requestFocus();
        }
    }

    /**
     * Esegue la richiesta di autenticazione o registrazione verso il server.
     *
     * Valida i campi di input e avvia un {@link Task} JavaFX in un thread in background
     * per evitare di bloccare l'interfaccia grafica durante la comunicazione socket.
     * In caso di esito positivo memorizza la connessione attiva e apre la finestra di gioco
     * tramite {@link #apriGameWindow(String)}; in caso contrario mostra il messaggio d'errore
     * e riabilita i pulsanti.
     *
     * @param tipo tipo di richiesta di autenticazione ({@link Tipo#LOGIN} o {@link Tipo#REGISTRAZIONE})
     */
    private void autentica(final Tipo tipo) {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            erroreLabel.setText("Inserisci username e password.");
            return;
        }

        btnLogin.setDisable(true);
        btnRegistra.setDisable(true);
        erroreLabel.setText("");

        Task<Messaggio> task = new Task<Messaggio>() {
            @Override
            protected Messaggio call() throws Exception {
                ServerConnection conn = new ServerConnection(leggiHost(), leggiPorta());
                conn.invia(new Messaggio(tipo, username, password));
                Messaggio risposta = conn.leggiMessaggio();
                if (risposta.getTipo() == Tipo.OK) {
                    GameLoginController.this.connessioneOk = conn;
                }
                return risposta;
            }
        };

        final String u = username;
        task.setOnSucceeded(e -> {
            Messaggio risposta = task.getValue();
            if (risposta != null && risposta.getTipo() == Tipo.OK) {
                apriGameWindow(u);
            } else {
                erroreLabel.setText(risposta != null ? risposta.getParam(0) : "Errore sconosciuto.");
                btnLogin.setDisable(false);
                btnRegistra.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            erroreLabel.setText("Impossibile connettersi al server.");
            btnLogin.setDisable(false);
            btnRegistra.setDisable(false);
        });

        new Thread(task).start();
    }

    /**
     * Carica e visualizza la finestra principale di gioco ({@code /game.fxml}).
     *
     * Sostituisce la scena corrente nello stesso {@link Stage}, inizializza il
     * {@link GameController} con la connessione socket attiva e l'username del giocatore,
     * e configura titolo, dimensioni, icona e centratura della finestra.
     *
     * @param username nome utente del giocatore autenticato
     */
    private void apriGameWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/game.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 880, 640));
            stage.setTitle("GuessTheWord");
            stage.setResizable(false);
            stage.centerOnScreen();
            
            GameController ctrl = loader.getController();
            ctrl.inizializza(connessioneOk, username);
            
            try (InputStream iconIs = getClass().getResourceAsStream("/images/logo-GuessTheWord.png")) {
                if (iconIs != null) {
                    stage.getIcons().clear();
                    stage.getIcons().add(new Image(iconIs));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            erroreLabel.setText("Errore apertura finestra principale.");
            btnLogin.setDisable(false);
            btnRegistra.setDisable(false);
        }
    }

    private String leggiHost() {
        return leggiProps().getProperty("server.host", "localhost");
    }

    private int leggiPorta() {
        return Integer.parseInt(leggiProps().getProperty("server.port", "5000"));
    }

    private Properties leggiProps() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            if (fis != null) p.load(fis);
        } catch (Exception ignored) {}
        return p;
    }
    
}