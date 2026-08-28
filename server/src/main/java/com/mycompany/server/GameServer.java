package com.mycompany.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Server di gioco che accetta le connessioni dei client in coppia.
 *
 * Gira in un thread daemon separato avviato da {@link ServerApp}.
 * Il ciclo principale accetta due socket consecutivi, crea i rispettivi
 * {@link ClientHandler}, li collega come avversari e li avvia in thread separati.
 * Il ciclo si ripete indefinitamente per supportare sessioni multiple.
 *
 * La porta viene letta da {@code server.properties} (chiave {@code server.port});
 * se il file non esiste usa la porta di default {@code 5000}.
 */
public class GameServer implements Runnable {

    private ServerSocket serverSocket;

    /**
     * Loop principale del server.
     * Apre la {@link ServerSocket} sulla porta configurata e accetta
     * coppie di client in ciclo continuo.
     */
    @Override
    public void run() {
        int porta = leggiPorta();
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Server avviato sulla porta " + porta);

            while (true) {
                Socket socket1 = serverSocket.accept();
                System.out.println("Giocatore 1 connesso: " + socket1.getInetAddress());
                ClientHandler handler1 = new ClientHandler(socket1);

                Socket socket2 = serverSocket.accept();
                System.out.println("Giocatore 2 connesso: " + socket2.getInetAddress());
                ClientHandler handler2 = new ClientHandler(socket2);

                handler1.setAvversario(handler2);
                handler2.setAvversario(handler1);

                new Thread(handler1).start();
                new Thread(handler2).start();
            }

        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Errore server: " + e.getMessage());
            }
        }
    }

    /**
     * Chiude la {@link ServerSocket}, interrompendo il ciclo di accettazione.
     * Chiamato quando il server viene fermato dall'amministratore.
     */
    public void ferma() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legge la porta dal file {@code server.properties}.
     * Se il file non esiste o il valore non è numerico, restituisce {@code 5000}.
     *
     * @return numero di porta su cui il server deve essere avviato
     */
    private int leggiPorta() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("server.properties non trovato, uso porta 5000");
        }
        try {
            return Integer.parseInt(props.getProperty("server.port", "5000"));
        } catch (NumberFormatException e) {
            return 5000;
        }
    }
}
