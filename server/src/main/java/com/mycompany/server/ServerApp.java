package com.mycompany.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

/**
 * Punto di ingresso dell'applicazione server JavaFX.
 *
 * Sequenza di avvio:
 * 
 *   Inizializza il database SQLite ({@link DatabaseManager#inizializza()})
 *   Carica l'analisi salvata da {@code analisi.ser}, se disponibile
 *   Avvia {@link GameServer} in un thread daemon
 *   Mostra la schermata di login admin ({@code login.fxml})
 *
 * Credenziali admin di default: {@code admin} / {@code admin}
 */
public class ServerApp extends Application {

    /**
     * Punto di ingresso JavaFX. Inizializza database, analisi, server di gioco
     * e mostra la schermata di login.
     *
     * @param stage finestra primaria fornita dal framework JavaFX
     * @throws Exception se il caricamento dell'FXML fallisce
     */
    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.inizializza();
        caricaAnalisiSerializzata();

        Thread serverThread = new Thread(new GameServer());
        serverThread.setDaemon(true);
        serverThread.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("GuessTheWord — Server");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Tenta di caricare l'analisi precedentemente salvata dal file {@code analisi.ser}.
     * Se il file non esiste o è corrotto, il server parte comunque senza analisi precaricata
     * (l'admin potrà caricarla manualmente dal pannello).
     */
    @SuppressWarnings("unchecked")
    private void caricaAnalisiSerializzata() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("analisi.ser"))) {
            Map<String, Long> tf    = (Map<String, Long>) ois.readObject();
            List<String>      frasi = (List<String>)      ois.readObject();
            ClientHandler.setTfMap(tf);
            ClientHandler.setFrasi(frasi);
            System.out.println("[ServerApp] Analisi caricata: "
                    + frasi.size() + " frasi, " + tf.size() + " parole.");
        } catch (Exception e) {
            System.out.println("[ServerApp] Nessuna analisi salvata trovata.");
        }
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
