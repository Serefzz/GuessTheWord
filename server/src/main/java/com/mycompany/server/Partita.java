package com.mycompany.server;

import com.mycompany.common.CifrarioCesare;
import com.mycompany.common.Messaggio;
import com.mycompany.common.StatoPartita;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Rappresenta una partita in corso tra due giocatori.
 *
 *  Flusso di una partita:
 * 
 *   Costruttore: sceglie una frase e una parola in base alla difficoltà,
 *       cifra la parola con il {@link CifrarioCesare}
 *   {@link #avvia()}: invia la sfida a entrambi i giocatori e avvia
 *       un timer server-side
 *   {@link #verificaRisposta(ClientHandler, String)}: chiamato da
 *       {@link ClientHandler} ad ogni risposta; sincronizzato per gestire
 *       le risposte concorrenti dei due thread
 *   {@link #tempoScaduto()}: chiamato dal timer se nessuno ha risposto
 *       correttamente entro il tempo limite → pareggio
 * 
 * 
 */
public class Partita {

    private final ClientHandler giocatore1;
    private final ClientHandler giocatore2;

    private final String parolaOriginale; // parola scelta (MAIUSCOLO)
    private final String testoSfida;      // frase con la parola cifrata
    private final int    shift;           // spostamento del cifrario
    private final int    secondi;         // tempo limite

    private StatoPartita stato;
    private long         tempoInizio;

    private final PartitaDAO partitaDAO = new PartitaDAO();

    /**
     * Costruisce una nuova partita scegliendo frase, parola e parametri
     * in base alla difficoltà selezionata.
     *
     * @param g1         handler del primo giocatore
     * @param g2         handler del secondo giocatore
     * @param frase      frase estratta dall'analisi del documento
     * @param difficolta livello di difficoltà: 1=facile, 2=medio, 3=difficile
     */
    public Partita(ClientHandler g1, ClientHandler g2, String frase, int difficolta) {
        this.giocatore1 = g1;
        this.giocatore2 = g2;

        Random rnd = new Random();
        if (difficolta == 1)      { this.shift = rnd.nextInt(5) + 1;  this.secondi = 60; }
        else if (difficolta == 2) { this.shift = rnd.nextInt(10) + 6; this.secondi = 40; }
        else                      { this.shift = rnd.nextInt(10) + 16; this.secondi = 20; }

        this.parolaOriginale = scegliParola(frase, difficolta);
        this.testoSfida      = costruisciTestoSfida(frase, parolaOriginale, shift);
        this.stato           = StatoPartita.IN_CORSO;
    }

    /**
     * Invia la sfida ad entrambi i giocatori e avvia il timer server-side.
     * Il timer chiama {@link #tempoScaduto()} allo scadere dei secondi.
     */
    public void avvia() {
        tempoInizio = System.currentTimeMillis();
        Messaggio sfida = new Messaggio(Messaggio.Tipo.SFIDA, testoSfida,
            String.valueOf(shift), String.valueOf(secondi));
        giocatore1.invia(sfida);
        giocatore2.invia(sfida);

        java.util.Timer timer = new java.util.Timer(true);
        timer.schedule(new java.util.TimerTask() {
            @Override public void run() { tempoScaduto(); }
        }, secondi * 1000L);
    }

    /**
     * Verifica la risposta di un giocatore.
     * Sincronizzato per gestire le risposte concorrenti dei due thread.
     * Se la risposta è corretta, decreta il vincitore e salva nel DB.
     *
     * @param giocatore handler del giocatore che ha risposto
     * @param risposta  parola inviata dal giocatore (verrà confrontata in maiuscolo)
     */
    public synchronized void verificaRisposta(ClientHandler giocatore, String risposta) {
        if (stato != StatoPartita.IN_CORSO) return;
        if (risposta.toUpperCase().equals(parolaOriginale)) {
            long tempoRisposta = System.currentTimeMillis() - tempoInizio;
            stato = StatoPartita.TERMINATA;
            ClientHandler avversario = (giocatore == giocatore1) ? giocatore2 : giocatore1;
            giocatore.invia(new Messaggio(Messaggio.Tipo.RISULTATO_VINTO,
                avversario.getUsername(), String.valueOf(tempoRisposta)));
            avversario.invia(new Messaggio(Messaggio.Tipo.RISULTATO_PERSO,
                giocatore.getUsername(), String.valueOf(tempoRisposta)));
            partitaDAO.salva(giocatore1.getUsername(), giocatore2.getUsername(),
                giocatore.getUsername(), tempoRisposta);
            giocatore1.setPartita(null);
            giocatore2.setPartita(null);
        } else {
            giocatore.invia(new Messaggio(Messaggio.Tipo.RISPOSTA_ERRATA));
        }
    }

    /**
     * Chiamato allo scadere del tempo: nessuno ha indovinato → pareggio.
     * Sincronizzato per evitare race condition con {@link #verificaRisposta}.
     */
    public synchronized void tempoScaduto() {
        if (stato != StatoPartita.IN_CORSO) return;
        stato = StatoPartita.TERMINATA;
        giocatore1.invia(new Messaggio(Messaggio.Tipo.PAREGGIO));
        giocatore2.invia(new Messaggio(Messaggio.Tipo.PAREGGIO));
        partitaDAO.salva(giocatore1.getUsername(), giocatore2.getUsername(), null, 0);
        giocatore1.setPartita(null);
        giocatore2.setPartita(null);
    }

    /**
     * Sceglie la parola da cifrare nella frase in base alla difficoltà e alla TF map.
     * Facile → parola più comune; Difficile → parola più rara; Medio → prima disponibile.
     *
     * @param frase      frase da cui scegliere la parola
     * @param difficolta livello di difficoltà (1/2/3)
     * @return parola scelta in maiuscolo (almeno 4 lettere alfabetiche)
     */
    private String scegliParola(String frase, int difficolta) {
        String[] paroleFrase = frase.toUpperCase().split("\\W+");
        List<String> candidati = new ArrayList<String>();
        for (String p : paroleFrase)
            if (p.matches("[A-Z]+") && p.length() >= 4) candidati.add(p);
        if (candidati.isEmpty()) return paroleFrase.length > 0 ? paroleFrase[0] : "PAROLA";

        Map<String, Long> tf = ClientHandler.getTfMap();
        if (tf == null || tf.isEmpty()) return candidati.get(0);

        String scelta = candidati.get(0);
        long freqScelta = tf.containsKey(scelta) ? tf.get(scelta) : 0L;
        for (int i = 1; i < candidati.size(); i++) {
            String parola = candidati.get(i);
            long freq = tf.containsKey(parola) ? tf.get(parola) : 0L;
            if (difficolta == 1 && freq > freqScelta) { scelta = parola; freqScelta = freq; }
            else if (difficolta == 3 && freq < freqScelta) { scelta = parola; freqScelta = freq; }
        }
        return scelta;
    }

    /**
     * Sostituisce la prima occorrenza della parola originale nella frase
     * con la versione cifrata.
     *
     * @param frase          frase originale
     * @param parolaOriginale parola da sostituire (in maiuscolo)
     * @param shift          spostamento del cifrario di Cesare
     * @return frase con la parola cifrata al posto di quella originale
     */
    private String costruisciTestoSfida(String frase, String parolaOriginale, int shift) {
        String parolaCifrata = CifrarioCesare.cifra(parolaOriginale, shift);
        return frase.toUpperCase().replaceFirst(parolaOriginale, parolaCifrata);
    }

    /** @return stato corrente della partita */
    public StatoPartita getStato() { return stato; }
    /** @return parola originale da indovinare (in maiuscolo) */
    public String getParolaOriginale() { return parolaOriginale; }
    /** @return testo della sfida con la parola cifrata */
    public String getTestoSfida() { return testoSfida; }
}