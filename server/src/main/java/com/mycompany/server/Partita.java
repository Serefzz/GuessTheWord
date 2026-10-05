package com.mycompany.server;

import com.mycompany.common.CifrarioCesare;
import com.mycompany.common.Messaggio;
import com.mycompany.common.StatoPartita;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final String testoSfida; // frase con la parola cifrata
    private final int shift; // spostamento del cifrario
    private final int durata = 0; // durata della partita, quanto tempo viene impiegato per trovare la parola cifrata

    private StatoPartita stato;

    private final PartitaDAO partitaDAO = new PartitaDAO();

    /**
     * Costruisce una nuova partita scegliendo frase, parola e parametri
     * in base alla difficoltà selezionata.
     *
     * @param g1 handler del primo giocatore
     * @param g2 handler del secondo giocatore
     * @param frase frase estratta dall'analisi del documento
     * @param difficolta livello di difficoltà: 1=facile, 2=medio, 3=difficile
     */
    public Partita(ClientHandler g1, ClientHandler g2, String frase, int difficolta) {
        this.giocatore1 = g1;
        this.giocatore2 = g2;

        Random rnd = new Random();
        if (difficolta == 1) { 
            this.shift = rnd.nextInt(5) + 1; 
        } else if (difficolta == 2) { 
            this.shift = rnd.nextInt(10) + 6;
        } else { 
            this.shift = rnd.nextInt(10) + 16;
        }

        this.parolaOriginale = scegliParola(frase, difficolta);
        this.testoSfida = costruisciTestoSfida(frase, parolaOriginale, shift);
        this.stato = StatoPartita.IN_CORSO;
    }

    /**
     * Invia la sfida ad entrambi i giocatori e avvia il timer server-side.
     */
    public void avvia() {
        Messaggio sfida = new Messaggio(Messaggio.Tipo.SFIDA, testoSfida, String.valueOf(shift));
        giocatore1.invia(sfida);
        giocatore2.invia(sfida);

        Timer timer = new Timer(true);
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
     * Sceglie la parola da cifrare nella frase in base alla difficoltà e alla TF map. Utilizza una classifica di ranking per stabilire quali parole siano le più adatta in base alla dificolta.
     * Facile → una delle parole più comuni e più semplici; Medio →una parola casuale; Difficile → una delle parole più rare e più lunghe.
     *
     * @param frase frase da cui scegliere la parola
     * @param difficolta livello di difficoltà (1/2/3)
     * @return parola scelta in maiuscolo (almeno 4 lettere alfabetiche)
     */
    private String scegliParola(String frase, int difficolta) {
        String[] paroleFrase = frase.toUpperCase().split("\\W+");
        Map<String, Long> tf = ClientHandler.getTfMap();
        Map<String, Integer> candidati = new TreeMap<>();
        String scelta;
        long freqScelta;
        for (String p : paroleFrase) {
            switch (difficolta) {
                case 0: {
                    tf.keySet().stream().collect(Collectors.toMap((parola) -> parola , valueMapper));
                }
                case 1: {
                    
                }
                case 2: {
                    
                }
            }
            if (p.matches("[A-Z]+") && p.length() >= 4) candidati.add(p);
        }
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