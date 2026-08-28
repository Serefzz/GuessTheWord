package com.mycompany.server;

import com.mycompany.common.Messaggio;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Gestisce la comunicazione con un singolo client connesso.
 *
 * Ogni istanza gira in un thread separato avviato da {@link GameServer}.
 * L'avversario viene impostato tramite {@link #setAvversario(ClientHandler)}
 * prima dell'avvio del thread.
 *
 * Le frasi e la mappa TF sono condivise staticamente tra tutti gli handler
 * e vengono impostate dall'admin tramite {@link AdminController}.
 */
public class ClientHandler implements Runnable {

    // ── Stato statico condiviso ────────────────────────────────────────────
    private static List<String>      frasi = null;
    private static Map<String, Long> tfMap = null;

    /**
     * Imposta le frasi disponibili per le sfide di gioco.
     * @param f lista di frasi estratte dall'analisi del documento
     */
    public static void setFrasi(List<String> f) { frasi = f; }

    /**
     * Imposta la mappa Term Frequency usata per scegliere la parola da cifrare.
     * @param m mappa {@code parola -> frequenza}
     */
    public static void setTfMap(Map<String, Long> m) { tfMap = m; }

    /** @return mappa TF correntemente caricata, o {@code null} se non disponibile */
    public static Map<String, Long> getTfMap() { return tfMap; }

    /** @return lista di frasi correntemente caricata, o {@code null} se non disponibile */
    public static List<String> getFrasi() { return frasi; }

    // ── Stato di istanza ───────────────────────────────────────────────────
    private final Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    private String        username;
    private ClientHandler avversario;
    private Partita       partitaCorrente;
    private boolean       connesso = true;

    private final UtenteDAO  utenteDAO  = new UtenteDAO();
    private final PartitaDAO partitaDAO = new PartitaDAO();

    /**
     * @param socket socket TCP del client connesso
     */
    public ClientHandler(Socket socket) { this.socket = socket; }

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
            case LOGIN:          gestisciLogin(msg.getParam(0), msg.getParam(1)); break;
            case REGISTRAZIONE:  gestisciRegistrazione(msg.getParam(0), msg.getParam(1)); break;
            case AVVIA_PARTITA:  gestisciAvviaPartita(msg); break;
            case RISPOSTA:
                if (partitaCorrente != null) partitaCorrente.verificaRisposta(this, msg.getParam(0));
                break;
            case STORICO_REQUEST:    gestisciStorico(); break;
            case CLASSIFICA_REQUEST: gestisciClassifica(); break;
            case DISCONNETTI:        connesso = false; break;
            default: break;
        }
    }

    /**
     * Verifica le credenziali e invia {@code OK} o {@code ERRORE}.
     * @param username nome utente
     * @param password password in chiaro
     */
    private void gestisciLogin(String username, String password) {
        if (utenteDAO.verificaLogin(username, password)) {
            this.username = username;
            invia(new Messaggio(Messaggio.Tipo.OK));
        } else {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Credenziali errate"));
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
        }
    }

    /**
     * Crea e avvia una {@link Partita} sincronizzando l'accesso con l'avversario.
     * Se la partita è già stata creata dall'avversario, non fa nulla.
     * @param msg messaggio contenente la difficoltà scelta (parametro 0)
     */
    private void gestisciAvviaPartita(Messaggio msg) {
        if (frasi == null || frasi.isEmpty()) {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Nessun documento caricato"));
            return;
        }
        int difficolta = parseDifficolta(msg.getParam(0));
        synchronized (ClientHandler.class) {
            if (partitaCorrente != null) return;
            String frase = frasi.get((int) (Math.random() * frasi.size()));
            Partita p = new Partita(this, avversario, frase, difficolta);
            this.setPartita(p);
            avversario.setPartita(p);
            p.avvia();
        }
    }

    /**
     * Converte il parametro di difficoltà in un intero 1-3.
     * @param param stringa ricevuta dal client
     * @return livello di difficoltà (1=facile, 2=medio, 3=difficile); default 1
     */
    private int parseDifficolta(String param) {
        try {
            int d = Integer.parseInt(param);
            if (d >= 1 && d <= 3) return d;
        } catch (NumberFormatException e) {}
        return 1;
    }

    /**
     * Recupera e invia lo storico partite dell'utente corrente.
     */
    private void gestisciStorico() {
        List<String[]> storico = partitaDAO.getStorico(username);
        StringBuilder sb = new StringBuilder();
        for (String[] riga : storico) {
            if (sb.length() > 0) sb.append("|");
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
            if (sb.length() > 0) sb.append("|");
            sb.append(riga[0]).append(";").append(riga[1]).append(";")
              .append(riga[2]).append(";").append(riga[3]).append(";").append(riga[4]);
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
    private void disconnetti() {
        connesso = false;
        try { socket.close(); } catch (IOException e) {}
    }

    /** @return username dell'utente autenticato */
    public String getUsername() { return username; }

    /**
     * Imposta il riferimento all'handler dell'avversario.
     * @param a handler del client avversario
     */
    public void setAvversario(ClientHandler a) { this.avversario = a; }

    /**
     * Imposta la partita corrente dell'utente.
     * @param p partita in corso
     */
    public void setPartita(Partita p) { this.partitaCorrente = p; }
}