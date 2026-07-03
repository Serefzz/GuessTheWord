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
 * Gestisce la comunicazione con un singolo client.
 * L'avversario viene impostato da GameServer prima di avviare il thread.
 */
public class ClientHandler implements Runnable {

    // impostati dall'AdminController dopo l'analisi del documento
    private static List<String>       frasi = null;
    private static Map<String, Long>  tfMap = null;

    public static void setFrasi(List<String> f) { 
        frasi = f; 
    }
    public static void setTfMap(Map<String, Long> m)
    { 
        tfMap = m;
    }
    public static Map<String, Long> getTfMap() { 
        return tfMap; 
    }
    public static List<String> getFrasi(){
        return frasi;
    }

    // --- istanza ---
    private final Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    private String username;
    private ClientHandler avversario;   // impostato da GameServer
    private Partita partitaCorrente;
    private boolean connesso = true;

    private final UtenteDAO utenteDAO   = new UtenteDAO();
    private final PartitaDAO partitaDAO = new PartitaDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ------------------------------------------------------------------
    //  Loop principale
    // ------------------------------------------------------------------

    @Override
    public void run() {
        try {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));

            while (connesso) {
                String linea = in.readLine();
                if (linea == null) break;
                gestisciMessaggio(Messaggio.fromLine(linea));
            }
        } catch (IOException e) {
            // client disconnesso
        } finally {
            disconnetti();
        }
    }

    // ------------------------------------------------------------------
    //  Smistamento messaggi
    // ------------------------------------------------------------------

    private void gestisciMessaggio(Messaggio msg) {
        switch (msg.getTipo()) {
            case LOGIN:
                gestisciLogin(msg.getParam(0), msg.getParam(1));
                break;
            case REGISTRAZIONE:
                gestisciRegistrazione(msg.getParam(0), msg.getParam(1));
                break;
            case AVVIA_PARTITA:
                gestisciAvviaPartita(msg);
                break;
            case RISPOSTA:
                if (partitaCorrente != null) partitaCorrente.verificaRisposta(this, msg.getParam(0));
                break;
            case STORICO_REQUEST:
                gestisciStorico();
                break;
            case CLASSIFICA_REQUEST:
                gestisciClassifica();
                break;
            case DISCONNETTI:
                connesso = false;
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------
    //  Gestione singoli messaggi
    // ------------------------------------------------------------------

    private void gestisciLogin(String username, String password) {
        if (utenteDAO.verificaLogin(username, password)) {
            this.username = username;
            invia(new Messaggio(Messaggio.Tipo.OK));
        } else {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Credenziali errate"));
        }
    }

    private void gestisciRegistrazione(String username, String password) {
        if (utenteDAO.registra(username, password)) {
            this.username = username;
            invia(new Messaggio(Messaggio.Tipo.OK));
        } else {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Username gia in uso"));
        }
    }

    private void gestisciAvviaPartita(Messaggio msg) {
        if (frasi == null || frasi.isEmpty()) {
            invia(new Messaggio(Messaggio.Tipo.ERRORE, "Nessun documento caricato"));
            return;
        }
        // Il client invia la difficoltà come parametro: 1=facile, 2=medio, 3=difficile
        int difficolta = parseDifficolta(msg.getParam(0));
        // synchronized: evita che entrambi i thread creino la partita contemporaneamente
        synchronized (ClientHandler.class) {
            if (partitaCorrente != null) return; // già creata dall'avversario
            String frase = frasi.get((int) (Math.random() * frasi.size()));
            Partita p = new Partita(this, avversario, frase, difficolta);
            this.setPartita(p);
            avversario.setPartita(p);
            p.avvia();
        }
    }

    /** Converte il parametro inviato dal client in un livello di difficoltà (1/2/3). */
    private int parseDifficolta(String param) {
        try {
            int d = Integer.parseInt(param);
            if (d >= 1 && d <= 3) return d;
        } catch (NumberFormatException e) {
            // usa default
        }
        return 1; // facile (default)
    }

    private void gestisciStorico() {
        List<String[]> storico = partitaDAO.getStorico(username);
        StringBuilder sb = new StringBuilder();
        for (String[] riga : storico) {
            if (sb.length() > 0) sb.append("|");
            sb.append(riga[0]).append(";").append(riga[1]).append(";").append(riga[2]);
        }
        invia(new Messaggio(Messaggio.Tipo.STORICO_RESPONSE, sb.toString()));
    }

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

    // ------------------------------------------------------------------
    //  Invio e disconnessione
    // ------------------------------------------------------------------

    public synchronized void invia(Messaggio msg) {
        try {
            out.write(msg.toLine());
            out.newLine();
            out.flush();
        } catch (IOException e) {
            connesso = false;
        }
    }

    private void disconnetti() {
        connesso = false;
        try { socket.close(); } catch (IOException e) {}
    }

    // ------------------------------------------------------------------
    //  Getter / setter
    // ------------------------------------------------------------------

    public String getUsername(){ 
        return username;
    }
    public void setAvversario(ClientHandler a) {
        this.avversario = a;
    }
    public void setPartita(Partita p){
        this.partitaCorrente = p; 
    }
}