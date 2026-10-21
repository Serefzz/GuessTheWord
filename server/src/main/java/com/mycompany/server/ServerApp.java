package com.mycompany.server;

import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Punto di ingresso dell'applicazione server JavaFX.
 *
 * Sequenza di avvio:
 * 
 *   Inizializza il database SQLite ({@link DatabaseManager#inizializza()})
 *   Carica le analisi salvate in locale, se disponibile
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
        caricaAnalisiSerializzate();

        Thread serverThread = new Thread(new GameServer());
        serverThread.setDaemon(true);
        serverThread.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/adminlogin.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("GuessTheWord — Admin");
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

    /**
     * Tenta di caricare le analisi precedentemente salvate dai file.
     * Se i file non esistono o sono corrotti, il server parte comunque senza analisi precaricata
     * (l'admin potrà caricarla manualmente dal pannello).
     */
    private void caricaAnalisiSerializzate() {
        List<AnalisiFrase> lista = new ArrayList<AnalisiFrase>();
        Path cartellaAnalisi = Paths.get("data", "analisi");
        try(Stream<Path> stream = Files.list(cartellaAnalisi)){
            stream.filter(p -> p.toString().endsWith(".ser")).forEach(p -> {
                try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(p))){
                    lista.add((AnalisiFrase) ois.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClientHandler.setTfFrasi(lista);
    }
    
}
