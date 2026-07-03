package com.mycompany.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Apre il ServerSocket e aspetta esattamente 2 client.
 * Ogni client viene gestito da un Thread separato.
 *
 * La porta viene letta da server.properties (chiave "server.port", default 5000).
 */
public class GameServer implements Runnable {

    private ServerSocket serverSocket;

    @Override
    public void run() {
        int porta = leggiPorta();
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Server avviato sulla porta " + porta);

            // Aspetta il primo client
            Socket socket1 = serverSocket.accept();
            System.out.println("Giocatore 1 connesso: " + socket1.getInetAddress());
            new Thread(new ClientHandler(socket1)).start();

            // Aspetta il secondo client
            Socket socket2 = serverSocket.accept();
            System.out.println("Giocatore 2 connesso: " + socket2.getInetAddress());
            new Thread(new ClientHandler(socket2)).start();

        } catch (IOException e) {
            System.err.println("Errore server: " + e.getMessage());
        }
    }

    /** Ferma il server (chiamato dalla UI admin alla chiusura). */
    public void ferma() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Legge la porta da server.properties.
     * Se il file manca o la chiave non c'è, usa il default 5000.
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