package com.mycompany.server;

import com.mycompany.common.CifrarioCesare;
import com.mycompany.common.Messaggio;
import com.mycompany.common.StatoPartita;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final int difficolta;
    private final String fraseOriginale; // frase scelta
    private final String parolaOriginale; // parola scelta (MAIUSCOLO)
    private final String fraseSfida; // frase con la parola cifrata
    private final String parolaSfida; // parola scelta cifrata
    private final int shift; // spostamento del cifrario
    private final int durata; // durata della partita, quanto tempo viene impiegato per trovare la parola cifrata

    private StatoPartita stato;
    private long tempoInizio = System.currentTimeMillis();
    
    private final PartitaDAO partitaDAO = new PartitaDAO();
    private final UtenteDAO utenteDAO = new UtenteDAO();
    private final FraseDAO fraseDAO = new FraseDAO();
    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    

    /**
     * Costruisce una nuova partita scegliendo frase, parola e parametri
     * in base alla difficoltà selezionata.
     *
     * @param g1 handler del primo giocatore
     * @param g2 handler del secondo giocatore
     * @param difficolta livello di difficoltà: 1 = facile, 2 = medio, 3 = difficile
     */
    public Partita(ClientHandler g1, ClientHandler g2, int difficolta) {
        this.giocatore1 = g1;
        this.giocatore2 = g2;
        this.difficolta = difficolta;
        this.durata = scegliTempo(difficolta);

        Random rnd = new Random();
        
        if (difficolta == 1) { 
            this.shift = rnd.nextInt(5) + 1; 
        } else if (difficolta == 2) { 
            this.shift = rnd.nextInt(10) + 6;
        } else { 
            this.shift = rnd.nextInt(10) + 16;
        }
        
        String[] elementiSfida = scegliParolaFrase(difficolta);
        
        this.fraseOriginale = elementiSfida[0];
        this.parolaOriginale = elementiSfida[1];
        this.fraseSfida = costruisciTestoSfida(fraseOriginale, parolaOriginale, shift);
        this.parolaSfida = costruisciTestoSfida(parolaOriginale, parolaOriginale, shift);
        this.stato = StatoPartita.IN_CORSO;
    }

    /**
     * Invia la sfida ad entrambi i giocatori e avvia il timer server-side.
     */
    public void avvia() {
        Messaggio sfida = new Messaggio(Messaggio.Tipo.SFIDA, fraseSfida, parolaSfida, String.valueOf(durata));
        giocatore1.invia(sfida);
        giocatore2.invia(sfida);

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override 
            public void run() { 
                tempoScaduto(); 
            }
        }, durata* 1000L);
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
            giocatore.invia(new Messaggio(Messaggio.Tipo.RISULTATO_VINTO, avversario.getUsername(), String.valueOf(tempoRisposta), parolaOriginale));
            avversario.invia(new Messaggio(Messaggio.Tipo.RISULTATO_PERSO, giocatore.getUsername(), String.valueOf(tempoRisposta), parolaOriginale));
            int idFrase = 0;
            int idDocumento = 0;
            for(String[] s : fraseDAO.selectAll()){
                if (s[2].equals(fraseOriginale)) {
                    idFrase = Integer.valueOf(s[0]);
                    idDocumento = Integer.valueOf(s[1]);
                }
            }
            partitaDAO.salva(idFrase, idDocumento, giocatore.getUsername(), avversario.getUsername(), 1, tempoRisposta);
            utenteDAO.aggiornaPunteggio(giocatore.getUsername(), difficolta);
            utenteDAO.aggiornaPunteggio(avversario.getUsername(), -difficolta);
            giocatore1.setPartita(null);
            giocatore2.setPartita(null);
        } else {
            giocatore.invia(new Messaggio(Messaggio.Tipo.RISPOSTA_ERRATA));
        }
    }
    
    /**
     * Finalizza la resa di un giocatore.
     * Sincronizzato per gestire le risposte concorrenti dei due thread.
     * La partita viene terminata con la vittoria dell'avversario e viene salvato il risultato nel DB
     *
     * @param giocatore handler del giocatore che vuole arrendersi
     */
    public synchronized void resa(ClientHandler giocatore) {
        if (stato != StatoPartita.IN_CORSO) return;
        long tempoRisposta = System.currentTimeMillis() - tempoInizio;
        stato = StatoPartita.TERMINATA;
        ClientHandler avversario = (giocatore == giocatore1) ? giocatore2 : giocatore1;
        giocatore.invia(new Messaggio(Messaggio.Tipo.RISULTATO_PERSO, avversario.getUsername(), String.valueOf(tempoRisposta), parolaOriginale));
        avversario.invia(new Messaggio(Messaggio.Tipo.RISULTATO_VINTO, giocatore.getUsername(), String.valueOf(tempoRisposta), parolaOriginale));
        int idFrase = 0;
        int idDocumento = 0;
        for (String[] s : fraseDAO.selectAll()) {
            if (s[2].equals(fraseOriginale)) {
                idFrase = Integer.valueOf(s[0]);
                idDocumento = Integer.valueOf(s[1]);
            }
        }
        partitaDAO.salva(idFrase, idDocumento, giocatore.getUsername(), avversario.getUsername(), 2, tempoRisposta);
        utenteDAO.aggiornaPunteggio(giocatore.getUsername(), -difficolta);
        utenteDAO.aggiornaPunteggio(avversario.getUsername(), difficolta);
        giocatore1.setPartita(null);
        giocatore2.setPartita(null);
    }
    
    /**
    * Chiamato dal timer quando il tempo limite scade senza che nessuno abbia indovinato.
    * Decreta il pareggio, notifica entrambi i giocatori e chiude la partita.
    */
    private synchronized void tempoScaduto() {
        // Se la partita è già terminata (es. qualcuno ha risposto prima dello scadere), non fare nulla
        if (stato != StatoPartita.IN_CORSO) {
            return;
        }

        int idFrase = 0;
        int idDocumento = 0;
        stato = StatoPartita.TERMINATA;

        Messaggio msgPareggio = new Messaggio(Messaggio.Tipo.PAREGGIO, parolaOriginale);
        giocatore1.invia(msgPareggio);
        giocatore2.invia(msgPareggio);
        
        for (String[] s : fraseDAO.selectAll()) {
            if (s[2].equals(fraseOriginale)) {
                idFrase = Integer.valueOf(s[0]);
                idDocumento = Integer.valueOf(s[1]);
            }
        }
        partitaDAO.salva(idFrase, idDocumento, giocatore1.getUsername(), giocatore2.getUsername(), 0, durata * 1000);

        giocatore1.setPartita(null);
        giocatore2.setPartita(null);
    }
    
    /**
     * Sceglie la frase e la parola da cifrare in base alla difficoltà e alle TF map.
     * Facile → una frase contenente una parola con una frequenza maggiore di 66; 
     * Medio → una frase contenente una parola con una frequenza maggiore di 33 ma minore o uguale di 66; 
     * Difficile → una frase contenente una parola con una frequenza maggiore di 0 ma minore o uguale di 33;
     * Se la lista dei candidati e' vuota allora viene aggiunta una tolleranza sulla frequenza di ogni difficolta e vengono ricercati i candidati
     *
     * @param difficolta livello di difficoltà (1/2/3)
     * @return frase e parola della sfida 
     */
    private String[] scegliParolaFrase(int difficolta) {
        //String[] paroleFrase = frase.toUpperCase().split("\\W+");
        List<AnalisiFrase> tf = ClientHandler.getTfFrasi();
        List<String[]> candidati = new ArrayList<String[]>();
        
        String fraseScelta = null;
        String parolaScelta = null;
        
        long tolleranza = 0;
        
        while(candidati.isEmpty() && candidati.size() <= 10){
            for (AnalisiFrase a : tf) {
                int paroleTotaliFrase = a.getTf().size();
                for (Map.Entry<String, Long> e : a.getTf().entrySet()){
                    long frequenza = e.getValue();
                    String parola = e.getKey();

                    boolean valida = false;
                    switch (difficolta) {
                            case 1:
                                valida = (frequenza > (66 - tolleranza));
                                break;
                            case 2:
                                valida = (frequenza > (33 - tolleranza) && frequenza <= (66 + tolleranza));
                                break;
                            case 3:
                                valida = (frequenza > 0 && frequenza <= (33 + tolleranza));
                                break;
                    }

                    if (valida && parola.length() > 1) {
                        candidati.add(new String[]{a.getFrase(), parola});
                    }
                }
            }
            tolleranza++;
        }
        
        Random r = new Random();
        
        String[] coppiaScelta = candidati.get(r.nextInt(candidati.size()));
        fraseScelta = coppiaScelta[0];
        parolaScelta = coppiaScelta[1];
        
        return new String[]{fraseScelta, parolaScelta};
    }

    /**
     * Sostituisce una singola occorrenza casuale della parola originale nella frase
     * con la versione cifrata.
     *
     * @param frase frase originale
     * @param parolaOriginale parola da sostituire (in maiuscolo)
     * @param shift spostamento del cifrario di Cesare
     * @return frase con la parola cifrata al posto di quella originale
     */
    private String costruisciTestoSfida(String frase, String parolaOriginale, int shift) {
        String parolaCifrata = CifrarioCesare.cifra(parolaOriginale, shift);
        String testoOriginale = frase.toUpperCase();

        // Normalizziamo sia la frase che la parola per ignorare gli accenti durante la ricerca
        String testoNormalizzato = DocumentAnalyzer.rimuoviAccenti(testoOriginale);
        String parolaPulita = DocumentAnalyzer.rimuoviAccenti(parolaOriginale.toUpperCase());

        // Compiliamo il pattern con il supporto Unicode per i confini di parola \b
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(parolaPulita) + "\\b", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(testoNormalizzato);

        List<int[]> occorrenze = new ArrayList<>();
        while (matcher.find()) {
            occorrenze.add(new int[]{matcher.start(), matcher.end()});
        }

        if (occorrenze.isEmpty()) {
            return testoOriginale; // Nessuna corrispondenza trovata
        }

        // Scegliamo un'occorrenza casuale
        Random r = new Random();
        int[] scelta = occorrenze.get(r.nextInt(occorrenze.size()));

        // Sostituiamo nel testo originale usando gli indici trovati 
        return testoOriginale.substring(0, scelta[0]) + parolaCifrata + testoOriginale.substring(scelta[1]);
    }
    
    /**
     * Sceglie la durata della partita in base alla difficoltà.
     * Facile → 90 secondi; Medio → 60 secondi; Difficile → 30 secondi.
     *
     * @param difficolta livello di difficoltà (1/2/3)
     * @return numero di secondi della durata della partita
     */
    private int scegliTempo(int difficolta) {
        int tempo;
        switch (difficolta) {
            case 1:
                tempo = 90;
                break;
            case 2:
                tempo = 60;
                break;
            case 3:
                tempo = 30;
                break;
            default:
                tempo = 90;
        }
        return tempo;
    }

    /** @return stato corrente della partita */
    public StatoPartita getStato() { return stato; }
    /** @return parola originale da indovinare (in maiuscolo) */
    public String getParolaOriginale() { return parolaOriginale; }
    /** @return testo della sfida con la parola cifrata */
    public String getFraseSfida() { return fraseSfida; }
    
}