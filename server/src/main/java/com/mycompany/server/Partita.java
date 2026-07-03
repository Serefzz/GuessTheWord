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
 * Flusso:
 *  1. Riceve una frase di testo e la mappa TF dal GameManager
 *  2. Sceglie la parola "migliore" nella frase (più frequente nel documento)
 *  3. Cifra quella parola con il cifrario di Cesare
 *  4. Sostituisce la parola originale con la versione cifrata nel testo
 *  5. Manda il testo modificato a entrambi i giocatori (messaggio SFIDA)
 *  6. Attende le risposte e decreta il vincitore
 *  7. Salva il risultato nel DB con il tempo di risposta
 */
public class Partita {

    private final ClientHandler giocatore1;
    private final ClientHandler giocatore2;

    private final String parolaOriginale;  // parola scelta nel testo (MAIUSCOLO)
    private final String testoSfida;       // frase con la parola sostituita dalla versione cifrata
    private final int shift;               // spostamento usato nel cifrario
    private final int secondi;             // tempo limite in secondi

    private StatoPartita stato;
    private long tempoInizio;              // System.currentTimeMillis() quando avvia()

    private final PartitaDAO partitaDAO = new PartitaDAO();

    public Partita(ClientHandler g1, ClientHandler g2, String frase, int difficolta) {
        this.giocatore1 = g1;
        this.giocatore2 = g2;

        // shift e tempo dipendono dalla difficoltà
        Random rnd = new Random();
        if (difficolta == 1) {
            this.shift   = rnd.nextInt(5) + 1;    // shift 1-5
            this.secondi = 60;
        } else if (difficolta == 2) {
            this.shift   = rnd.nextInt(10) + 6;   // shift 6-15
            this.secondi = 40;
        } else {
            this.shift   = rnd.nextInt(10) + 16;  // shift 16-25
            this.secondi = 20;
        }

        // Sceglie la parola dalla frase in base alla difficoltà e alla tfMap
        this.parolaOriginale = scegliParola(frase, difficolta);
        this.testoSfida      = costruisciTestoSfida(frase, parolaOriginale, shift);
        this.stato           = StatoPartita.IN_CORSO;
    }

    // ------------------------------------------------------------------
    //  Avvio partita
    // ------------------------------------------------------------------

    /**
     * Invia la sfida ad entrambi i giocatori e avvia il timer.
     * Formato SFIDA: testoConParolaCifrata | shift | secondiDisponibili
     */
    public void avvia() {
        tempoInizio = System.currentTimeMillis();

        Messaggio sfida = new Messaggio(
            Messaggio.Tipo.SFIDA,
            testoSfida,
            String.valueOf(shift),
            String.valueOf(secondi)
        );
        giocatore1.invia(sfida);
        giocatore2.invia(sfida);
    }

    // ------------------------------------------------------------------
    //  Verifica risposta
    // ------------------------------------------------------------------

    /**
     * Chiamato da ClientHandler quando un giocatore invia una risposta.
     * synchronized: due thread possono chiamarlo contemporaneamente.
     */
    public synchronized void verificaRisposta(ClientHandler giocatore, String risposta) {
        if (stato != StatoPartita.IN_CORSO) {
            return; // partita già conclusa
        }

        if (risposta.toUpperCase().equals(parolaOriginale)) {
            // Risposta corretta — calcola il tempo di risposta
            long tempoRisposta = System.currentTimeMillis() - tempoInizio;
            stato = StatoPartita.TERMINATA;

            ClientHandler avversario = (giocatore == giocatore1) ? giocatore2 : giocatore1;

            giocatore.invia(new Messaggio(
                Messaggio.Tipo.RISULTATO_VINTO,
                avversario.getUsername(),
                String.valueOf(tempoRisposta)
            ));
            avversario.invia(new Messaggio(
                Messaggio.Tipo.RISULTATO_PERSO,
                giocatore.getUsername(),
                String.valueOf(tempoRisposta)
            ));

            // Salva nel DB con il tempo di risposta
            partitaDAO.salva(
                giocatore1.getUsername(),
                giocatore2.getUsername(),
                giocatore.getUsername(),
                tempoRisposta
            );
        } else {
            // Risposta sbagliata — il giocatore può riprovare
            giocatore.invia(new Messaggio(Messaggio.Tipo.RISPOSTA_ERRATA));
        }
    }

    /**
     * Chiamato allo scadere del tempo: nessuno ha indovinato → pareggio.
     */
    public synchronized void tempoScaduto() {
        if (stato != StatoPartita.IN_CORSO) return;
        stato = StatoPartita.TERMINATA;

        giocatore1.invia(new Messaggio(Messaggio.Tipo.PAREGGIO));
        giocatore2.invia(new Messaggio(Messaggio.Tipo.PAREGGIO));

        // Pareggio: vincitore = null, tempo_risposta = 0
        partitaDAO.salva(giocatore1.getUsername(), giocatore2.getUsername(), null, 0);
    }

    // ------------------------------------------------------------------
    //  Metodi privati: costruzione della sfida
    // ------------------------------------------------------------------

    /**
     * Sceglie la parola da cifrare nella frase in base alla difficoltà e alla TF map.
     *   facile (1)    → parola più comune (alta frequenza nella TF)
     *   medio  (2)    → prima parola valida disponibile
     *   difficile (3) → parola più rara (bassa frequenza nella TF)
     * Se la TF map non è disponibile usa la prima parola con 4+ lettere.
     */
    private String scegliParola(String frase, int difficolta) {
        String[] paroleFrase = frase.toUpperCase().split("\\W+");

        // Raccoglie candidati: solo lettere, lunghezza >= 4
        List<String> candidati = new ArrayList<String>();
        for (int i = 0; i < paroleFrase.length; i++) {
            if (paroleFrase[i].matches("[A-Z]+") && paroleFrase[i].length() >= 4) {
                candidati.add(paroleFrase[i]);
            }
        }
        if (candidati.isEmpty()) return paroleFrase.length > 0 ? paroleFrase[0] : "PAROLA";

        Map<String, Long> tfMap = ClientHandler.getTfMap();
        if (tfMap == null || tfMap.isEmpty()) return candidati.get(0);

        // Cerca la parola con frequenza max (facile) o min (difficile)
        String scelta    = candidati.get(0);
        long   freqScelta = tfMap.containsKey(scelta) ? tfMap.get(scelta) : 0L;

        for (int i = 1; i < candidati.size(); i++) {
            String parola = candidati.get(i);
            long   freq   = tfMap.containsKey(parola) ? tfMap.get(parola) : 0L;
            if (difficolta == 1 && freq > freqScelta) {
                scelta = parola; freqScelta = freq;       // facile: più comune
            } else if (difficolta == 3 && freq < freqScelta) {
                scelta = parola; freqScelta = freq;       // difficile: più rara
            }
            // medio (2): resta il primo candidato
        }
        return scelta;
    }

    /**
     * Sostituisce la prima occorrenza di parolaOriginale nella frase
     * con la sua versione cifrata.
     * Esempio: "Il SOLE sorge" → "Il VROH sorge" (shift=3)
     */
    private String costruisciTestoSfida(String frase, String parolaOriginale, int shift) {
        String parolaCifrata = CifrarioCesare.cifra(parolaOriginale, shift);
        // Sostituzione case-insensitive della prima occorrenza
        return frase.toUpperCase().replaceFirst(parolaOriginale, parolaCifrata);
    }

    // ------------------------------------------------------------------
    //  Getter
    // ------------------------------------------------------------------

    public StatoPartita getStato(){
        return stato; 
    }
    public String getParolaOriginale(){
        return parolaOriginale;
    }
    public String getTestoSfida(){
        return testoSfida; 
    }
}