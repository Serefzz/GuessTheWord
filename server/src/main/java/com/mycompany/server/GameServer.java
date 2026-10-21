package com.mycompany.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Server di gioco che accetta le connessioni dei client.
 *
 * Gira in un thread daemon separato avviato da {@link ServerApp}.
 * Il ciclo principale accetta due socket, crea i rispettivi
 * {@link ClientHandler}, li collega come avversari e li avvia in thread separati.
 * Il ciclo si ripete indefinitamente per supportare sessioni multiple.
 *
 * La porta viene letta da {@code server.properties} (chiave {@code server.port});
 * se il file non esiste usa la porta di default {@code 5000}.
 */
public class GameServer implements Runnable {

    private ServerSocket serverSocket;
    private List<ClientHandler> clientCollegati = new ArrayList<ClientHandler>();

    /**
     * Loop principale del server.
     * Apre la {@link ServerSocket} sulla porta configurata e accetta
     * due client in ciclo continuo.
     */
    @Override
    public void run() {
        int porta = leggiPorta();
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Server avviato sulla porta " + porta);

            while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, this);
            new Thread(handler).start();

            synchronized (clientCollegati) {
                // Se ci sono già 2 client, rifiuta la connessione
                if (clientCollegati.size() >= 2) {
                    System.out.println("Connessione rifiutata: 2 client già presenti.");
                    socket.close();
                    continue; // Torna su accept() per il prossimo
                }

                // Se c'è posto, crea l'handler e lo avvia
                clientCollegati.add(handler);
                System.out.println("Client connesso. Client collegati: " + clientCollegati.size() + "/2" );

                // Se è appena entrato il secondo client, li accoppia
                if (clientCollegati.size() == 2) {
                    clientCollegati.get(0).setAvversario(clientCollegati.get(1));
                    clientCollegati.get(1).setAvversario(clientCollegati.get(0));
                }
            }
        }

        } catch (IOException e) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.err.println("Errore server: " + e.getMessage());
            } else if (serverSocket == null) {
                System.err.println("Impossibile avviare il ServerSocket: " + e.getMessage());
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
     * Scollega il client che si vuole disconnettere.
     * 
     * @param handler client che si vuole disconnettere
     */
    public synchronized void rimuoviClient(ClientHandler handler) {
        synchronized (clientCollegati) {
            clientCollegati.remove(handler);
            System.out.println("Client disconnesso. Client collegati: " + clientCollegati.size() + "/2");

            // Se e' rimasto un client connesso dopo che è uscito
            if (clientCollegati.size() == 1) {
                ClientHandler rimasto = clientCollegati.get(0);
                rimasto.unsetAvversario();
                rimasto.setPartita(null);
            }
        }
    }
    
    /**
     * Restituisce la lista dei client connessi.
     * 
     * @return lista di client collegati{@code clientCollegati}
     */
    public List<ClientHandler> getClientCollegati() {
        return clientCollegati;
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
