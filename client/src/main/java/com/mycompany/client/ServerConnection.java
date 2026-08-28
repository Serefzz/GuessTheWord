package com.mycompany.client;

import com.mycompany.common.Messaggio;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Gestisce la connessione socket con il server di gioco.
 *
 * <p>Offre due modalità di lettura: {@link #leggiMessaggio()} per la lettura
 * sincrona usata solo durante il login, e {@link #avviaRicezione()} per il
 * thread daemon in background che notifica il {@link MessageListener}
 * tramite {@code Platform.runLater()}.</p>
 */ 
public class ServerConnection {

    private final Socket        socket;
    private final BufferedWriter out;
    private final BufferedReader in;
    private MessageListener listener;

    /**
     * Apre una connessione TCP verso il server e inizializza i flussi UTF-8.
     *
     * @param host indirizzo del server (es. {@code "localhost"})
     * @param port porta del server (es. {@code 5000})
     * @throws IOException se la connessione non può essere stabilita
     */
    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
    }

    /**
     * Imposta il listener che riceverà i messaggi in modalità asincrona.
     *
     * @param listener oggetto che implementa {@link MessageListener}
     */
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    /**
     * Legge un singolo messaggio dal server in modo sincrono.
     * Usato solo durante la fase di login/registrazione.
     *
     * @return messaggio ricevuto dal server
     * @throws IOException se la connessione viene chiusa o si verifica un errore di rete
     */
    public Messaggio leggiMessaggio() throws IOException {
        String linea = in.readLine();
        if (linea == null) throw new IOException("Connessione chiusa dal server");
        return Messaggio.fromLine(linea);
    }

    /**
     * Avvia un thread daemon che legge continuamente i messaggi dal server
     * e notifica il {@link MessageListener} su {@code Platform.runLater()}.
     * Deve essere chiamato dopo il login avvenuto con successo.
     */
    public void avviaRicezione() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        if (listener != null) {
                            final Messaggio msg = Messaggio.fromLine(linea);
                            javafx.application.Platform.runLater(new Runnable() {
                                @Override
                                public void run() { listener.onMessage(msg); }
                            });
                        }
                    }
                } catch (IOException e) {
                    // connessione chiusa
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Invia un messaggio al server serializzandolo come riga di testo.
     *
     * @param msg messaggio da inviare
     */
    public void invia(Messaggio msg) {
        try {
            out.write(msg.toLine());
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chiude il socket e i flussi associati.
     */
    public void chiudi() {
        try { 
            
           socket.close(); 
        
        } catch (IOException ignored) {}
    }
}