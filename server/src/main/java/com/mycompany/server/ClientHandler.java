package com.mycompany.server;

import com.mycompany.common.Messaggio;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

/**
 * Gestisce la comunicazione con un singolo client connesso.
 *
 * Ogni istanza gira in un thread separato avviato da {@link GameServer}.
 * L'avversario viene impostato tramite {@link #setAvversario(ClientHandler)}
 * prima dell'avvio del thread.
 *
 * Le frasi e le TF sono condivise staticamente tra tutti gli handler
 * e vengono impostate dall'admin tramite {@link AdminController}.
 */
public class ClientHandler implements Runnable {

    // Stato statico condiviso
    
    private static List<List<AnalisiFrase>> tfDocumenti = null;
    private static List<AnalisiFrase> tfFrasi = null;
    
    // Stato di istanza
    
    private final Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    private String username;
    private ClientHandler avversario;
    private Partita partitaCorrente;
    private boolean connesso = true;
    
    private final UtenteDAO utenteDAO  = new UtenteDAO();
    private final PartitaDAO partitaDAO = new PartitaDAO();
    
    // Riferimento al server
    
    private final GameServer server;

    /**
     * @param socket socket TCP del client connesso
     */
    public ClientHandler(Socket socket, GameServer server) { 
        this.socket = socket; 
        this.server = server;
    }

    /**
     * Loop principale di lettura messaggi dal client.
     * Inizializza i flussi UTF-8 e legge una riga alla volta finché la connessione è attiva.
     */
    @Override
    public void run() {
        try {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            while (connesso) {
                String linea = in.readLine();
                if (linea == null) break;
                try {
                    gestisciMessaggio(Messaggio.fromLine(linea));
                } catch (Exception e) {
                    System.err.println("[Handler] Errore gestione messaggio: " + e);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // client disconnesso
        } catch (Exception e) {
            System.err.println("[Handler] Errore imprevisto: " + e);
            e.printStackTrace();
        } finally {
            disconnetti();
        }
    }

    /**
     * Smista il messaggio ricevuto al metodo di gestione appropriato.
     * @param msg messaggio deserializzato dal protocollo
     */
    private void gestisciMessaggio(Messaggio msg) {
        switch (msg.getTipo()) {
            case LOGIN: {
                gestisciLogin(msg.getParam(0), msg.getParam(1)); 
                break;
            }
            case REGISTRAZIONE: {
                gestisciRegistrazione(msg.getParam(0), msg.getParam(1)); 
                break;
            }
            case AVVIA_PARTITA: {
                gestisciAvviaPartita(msg); 
                break;
            }
            case RISPOSTA: {
                if (partitaCorrente != null) partitaCorrente.verificaRisposta(this, msg.getParam(0));
                break;
            }
            case FORFAIT : {
                if (partitaCorrente != null) partitaCorrente.resa(this);
                break;
            }
            case STORICO_REQUEST: {
                gestisciStorico();
                break;
            }
            case CLASSIFICA_REQUEST: {
                gestisciClassifica();
                break;
            }
            case DISCONNETTI: {
                disconnetti();
                break;
            }
            default: break;
        }
    }

    /**
     * Verifica le credenziali e invia {@code OK} o {@code ERRORE}.
     * @param username nome utente
     * @param password password in chiaro
     */
    private void gestisciLogin(String username, String password) {
        boolean giaConnesso = false;

        for (ClientHandler ch : server.getClientCollegati()) {
            if (ch != this && ch.getUsername() != null && ch.getUsername().equals(username)) {
                giaConnesso = true;
                break;
            }
        }

        if (utenteDAO.verificaLogin(username, password) && !giaConnesso && !utenteDAO.isAdmin(username)) {
            this.username = username;
            invia(new Messaggio(Messaggio.Tipo.OK));
        } else {
            if (giaConnesso) {
                invia(new Messaggio(Messaggio.Tipo.ERRORE, "Giocatore già connesso."));
            } else if (utenteDAO.isAdmin(username)) {
                invia(new Messaggio(Messaggio.Tipo.ERRORE, "Admin accede differentemente."));
            } else {
                invia(new Messaggio(Messaggio.Tipo.ERRORE, "Credenziali errate."));
            }
            disconnetti();
        }
    }

    /**
     * Registra un nuovo utente e invia {@code OK} o {@code ERRORE}.
     * @param username nome utente desiderato
     * @param password password in chiaro
     */
    private void gestisciRegistrazione(String username, String password) {
        if (utenteDAO.registra(username, password)) {
            this.username = username;
            invia(new Messaggio(Messaggio.Tipo.OK));
        } else {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Username gia in uso"));
            disconnetti();
        }
    }

    /**
     * Crea e avvia una {@link Partita} sincronizzando l'accesso con l'avversario.
     * Se la partita è già stata creata dall'avversario, non fa nulla.
     * @param msg messaggio contenente la difficoltà scelta (parametro 0)
     */
    private void gestisciAvviaPartita(Messaggio msg) {
        if (avversario == null) {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "In attesa del secondo giocatore..."));
            return;
        }
        
        if (tfFrasi == null || tfFrasi.isEmpty()) {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Nessun documento caricato"));
            return;
        }
        
        int difficolta = Integer.parseInt(msg.getParam(0));
        synchronized (ClientHandler.class) {
            if (partitaCorrente != null) return;
            Partita p = new Partita(this, avversario, difficolta);
            this.setPartita(p);
            avversario.setPartita(p);
            p.avvia();
        }
    }

    /**
     * Recupera e invia lo storico partite dell'utente corrente.
     */
    private void gestisciStorico() {
        List<String[]> storico = partitaDAO.getStorico(username);
        StringBuilder sb = new StringBuilder();
        for (String[] riga : storico) {
            if (sb.length() > 0) sb.append("~");
            sb.append(riga[0]).append(";").append(riga[1]).append(";").append(riga[2]);
        }
        invia(new Messaggio(Messaggio.Tipo.STORICO_RESPONSE, sb.toString()));
    }

    /**
     * Recupera e invia la classifica globale dei giocatori.
     */
    private void gestisciClassifica() {
        List<String[]> classifica = utenteDAO.getClassifica();
        StringBuilder sb = new StringBuilder();
        for (String[] riga : classifica) {
            if (sb.length() > 0) sb.append("~");
            sb.append(riga[0] + ";").append(riga[1] + ";").append(riga[2] + ";").append(riga[3] + ";").append(riga[4] + ";").append(riga[5]);
        }
        invia(new Messaggio(Messaggio.Tipo.CLASSIFICA_RESPONSE, sb.toString()));
    }

    /**
     * Invia un messaggio al client serializzandolo come riga di testo UTF-8.
     * @param msg messaggio da inviare
     */
    public synchronized void invia(Messaggio msg) {
        try {
            out.write(msg.toLine());
            out.newLine();
            out.flush();
        } catch (IOException e) {
            connesso = false;
        }
    }

    /** Chiude il socket e imposta {@code connesso = false}. */
    public synchronized void disconnetti() {
        if (!connesso) return;
        
        connesso = false;

        try { 
            if (!socket.isClosed()) socket.close(); 
        } catch (IOException ignored) {}

        if (server != null) {
            server.rimuoviClient(this);
        }
    }

    /** @return username dell'utente autenticato */
    public String getUsername() { return username; }
    
    /** @return lista delle TF di ogni documento analizzato, o {@code null} se non disponibile */
    public static List<List<AnalisiFrase>> getTfDocumenti() {return tfDocumenti;}

    /** @return lista delle TF correntemente caricata, o {@code null} se non disponibile */
    public static List<AnalisiFrase> getTfFrasi() { return tfFrasi; }

    /**
     * Imposta la lista delle Term Frequency di ogni documento analizzato.
     * @param n lista delle liste rappresentanti le tf delle frase di ogni documento
     */
    public static void setTfDocumenti(List<List<AnalisiFrase>> n) { tfDocumenti = n; }
    
    /**
     * Imposta la lista delle Term Frequency usata per scegliere la parola da cifrare.
     * @param m lista delle tf caricate per la sfida
     */
    public static void setTfFrasi(List<AnalisiFrase> m) { tfFrasi = m; }
    
    /**
     * Imposta il riferimento all'handler dell'avversario.
     * @param a handler del client avversario
     */
    public void setAvversario(ClientHandler a) { this.avversario = a; }
    
    /**
     * Rimuove il riferimento all'handler dell'avversario.
     */
    public void unsetAvversario() {
        this.avversario = null;
        invia(new Messaggio(Messaggio.Tipo.ATTESA, "L'avversario si è disconnesso. In attesa di un nuovo giocatore..."));
    }

    /**
     * Imposta la partita corrente dell'utente.
     * @param p partita in corso
     */
    public void setPartita(Partita p) { this.partitaCorrente = p; }
    
}