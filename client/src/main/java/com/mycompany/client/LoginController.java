package com.mycompany.client;

import com.mycompany.common.Messaggio;
import com.mycompany.common.Messaggio.Tipo;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.InputStream;
import java.util.Properties;

/**
 * Controller della schermata di login/registrazione del client.
 * FXML associato: {@code /login.fxml}
 *
 * La connessione al server e l'autenticazione avvengono in un
 * {@link Task} su thread separato per non bloccare il thread JavaFX.
 * In caso di successo viene aperta la finestra principale ({@code main.fxml}).
 *
 * Host e porta del server vengono letti da {@code client.properties}
 * (chiavi {@code server.host} e {@code server.port}).
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         erroreLabel;
    @FXML private Button        btnLogin;
    @FXML private Button        btnRegistra;

    /**
     * Gestisce il click sul pulsante Login.
     * Avvia il processo di autenticazione con tipo {@link Tipo#LOGIN}.
     */
    @FXML
    private void handleLogin() {
        autentica(Tipo.LOGIN);
    }

    /**
     * Gestisce il click sul pulsante Registrati.
     * Avvia il processo di autenticazione con tipo {@link Tipo#REGISTRAZIONE}.
     */
    @FXML
    private void handleRegistra() {
        autentica(Tipo.REGISTRAZIONE);
    }

    /**
     * Esegue la connessione al server e l'autenticazione in background.
     * Disabilita i pulsanti durante l'attesa e mostra eventuali errori.
     *
     * @param tipo {@link Tipo#LOGIN} o {@link Tipo#REGISTRAZIONE}
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
                    LoginController.this.connessioneOk = conn;
                }
                return risposta;
            }
        };

        final String u = username;
        task.setOnSucceeded(e -> {
            Messaggio risposta = task.getValue();
            if (risposta.getTipo() == Tipo.OK) {
                apriMainWindow(u);
            } else {
                erroreLabel.setText(risposta.getParam(0));
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

    /** Connessione creata nel task, letta sul thread FX. */
    private volatile ServerConnection connessioneOk;

    /**
     * Carica e mostra la finestra principale ({@code main.fxml}) passando
     * la connessione e l'username al {@link MainController}.
     *
     * @param username nome utente autenticato
     */
    private void apriMainWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();
            MainController ctrl = loader.getController();
            ctrl.inizializza(connessioneOk, username);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 820, 600));
            stage.setTitle("GuessTheWord — " + username);
            stage.setResizable(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            erroreLabel.setText("Errore apertura finestra principale.");
        }
    }

    /**
     * Legge l'host del server da {@code client.properties}.
     *
     * @return hostname del server; default {@code "localhost"}
     */
    private String leggiHost() {
        return leggiProps().getProperty("server.host", "localhost");
    }

    /**
     * Legge la porta del server da {@code client.properties}.
     *
     * @return numero di porta; default {@code 5000}
     */
    private int leggiPorta() {
        return Integer.parseInt(leggiProps().getProperty("server.port", "5000"));
    }

    /**
     * Carica il file {@code client.properties} dal classpath.
     *
     * @return oggetto {@link Properties} con la configurazione; vuoto se il file non esiste
     */
    private Properties leggiProps() {
        Properties p = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("client.properties")) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {}
        return p;
    }
}