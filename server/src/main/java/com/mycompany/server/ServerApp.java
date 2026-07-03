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
 * Punto di ingresso JavaFX del server.
 *
 * All'avvio:
 *  1. Inizializza il database (crea tabelle + admin di default se non esistono).
 *  2. Avvia GameServer in un thread daemon.
 *  3. Mostra la schermata di login admin.
 *     Credenziali default: admin / admin
 */
public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Database (crea anche utente admin di default)
        DatabaseManager.inizializza();

        // 2. Carica analisi salvata (se esiste)
        caricaAnalisiSerializzata();

        // 3. GameServer in background
        Thread serverThread = new Thread(new GameServer());
        serverThread.setDaemon(true);
        serverThread.start();

        // 3. Schermata di login
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/login.fxml")
        );
        Scene scene = new Scene(loader.load());
        stage.setTitle("GuessTheWord — Server");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Prova a caricare analisi.ser dalla directory di lavoro.
     * Se non esiste o è corrotto non fa nulla (il server parte comunque).
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

    public static void main(String[] args) {
        launch(args);
    }
}