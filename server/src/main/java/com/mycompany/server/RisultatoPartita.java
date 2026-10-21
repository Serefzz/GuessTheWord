package com.mycompany.server;

/**
 * Modello immutabile che rappresenta il risultato di una partita conclusa.
 *
 * Viene popolato da {@link PartitaDAO} leggendo la tabella {@code partite}
 * e utilizzato dal pannello Storico del server admin e del client.
 */
public class RisultatoPartita {

    private final int id;
    private final int frase; /** Id della frase utilizzata nella partita. */
    private final int documento; /** Id del documento della frase utilizzata nella partita. */
    private final String giocatore1; /** Username del giocatore1. */
    private final String giocatore2;  /** Username del giocatore2. */
    private final int vincitore; /** Intero corrispondente al vincitore: 1 se giocatore1, 2 se giocatore 2 e 0 se pareggio. */
    private final String data; /** Data e ora della partita nel formato {@code dd/MM/yyyy HH:mm}. */
    private final long durata; /** Millisecondi trascorsi dalla sfida alla risposta corretta; durata massima della partita se pareggio. */


    /**
     * Costruisce un risultato partita con tutti i campi.
     *
     * @param id identificatore univoco della partita nel DB
     * @param giocatore1 username del giocatore1
     * @param giocatore2 username del giocatore2
     * @param data data e ora della partita (formato {@code dd/MM/yyyy HH:mm})
     * @param durata millisecondi impiegati per rispondere correttamente; durata massima della partita se pareggio
     */
    public RisultatoPartita(int id, int frase, int documento, String giocatore1, String giocatore2, int vincitore, String data, long durata) {
        this.id = id;
        this.frase = frase;
        this.documento = documento;
        this.giocatore1 = giocatore1;
        this.giocatore2 = giocatore2;
        this.vincitore = vincitore;
        this.data = data;
        this.durata = durata;
        
    }

    /**
     * @return identificatore univoco della partita nel database
     */
    public int getId() { return id; }
    
    /**
     * @return identificatore univoco della frase della partita nel database
     */
    public int getFrase() { return frase; }
    
    /**
     * @return identificatore univoco del documento della frase della partita nel database
     */
    public int getDocumento() { return documento; }
    
    /**
     * @return username del giocatore1
     */
    public String getGiocatore1() { return giocatore1; }
    
    /**
     * @return username del giocatore2
     */
    public String getGiocatore2() { return giocatore2; }

    /**
     * @return numero del vincitore, o {@code 0} se la partita è terminata in pareggio
     */
    public int getVincitore() { return vincitore; }

    /**
     * @return data e ora della partita nel formato {@code dd/MM/yyyy HH:mm}
     */
    public String getData() { return data; }

    /**
     * @return millisecondi trascorsi dalla sfida alla risposta corretta; 0 se pareggio
     */
    public long getDurata() { return durata; }
    
}
