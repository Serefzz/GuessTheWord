package com.mycompany.server;

/**
 * Modello immutabile che rappresenta il risultato di una partita conclusa.
 *
 * Viene popolato da {@link PartitaDAO} leggendo la tabella {@code partite}
 * e utilizzato dal pannello Storico del server admin e del client.
 */
public class RisultatoPartita {

    private final int    id;
    private final String giocatore1;
    private final String giocatore2;
    /** Username del vincitore, oppure {@code null} in caso di pareggio. */
    private final String vincitore;
    /** Data e ora della partita nel formato {@code dd/MM/yyyy HH:mm}. */
    private final String data;
    /** Millisecondi trascorsi dalla sfida alla risposta corretta; 0 se pareggio. */
    private final long   tempoRisposta;

    /**
     * Costruisce un risultato partita con tutti i campi.
     *
     * @param id            identificatore univoco della partita nel DB
     * @param giocatore1    username del primo giocatore
     * @param giocatore2    username del secondo giocatore
     * @param vincitore     username del vincitore, o {@code null} se pareggio
     * @param data          data/ora della partita (formato {@code dd/MM/yyyy HH:mm})
     * @param tempoRisposta millisecondi impiegati per rispondere correttamente; 0 se pareggio
     */
    public RisultatoPartita(int id, String giocatore1, String giocatore2,
                            String vincitore, String data, long tempoRisposta) {
        this.id            = id;
        this.giocatore1    = giocatore1;
        this.giocatore2    = giocatore2;
        this.vincitore     = vincitore;
        this.data          = data;
        this.tempoRisposta = tempoRisposta;
    }

    /**
     * @return identificatore univoco della partita nel database
     */
    public int getId() { return id; }

    /**
     * @return username del primo giocatore
     */
    public String getGiocatore1() { return giocatore1; }

    /**
     * @return username del secondo giocatore
     */
    public String getGiocatore2() { return giocatore2; }

    /**
     * @return username del vincitore, o {@code null} se la partita è terminata in pareggio
     */
    public String getVincitore() { return vincitore; }

    /**
     * @return data e ora della partita nel formato {@code dd/MM/yyyy HH:mm}
     */
    public String getData() { return data; }

    /**
     * @return millisecondi trascorsi dalla sfida alla risposta corretta; 0 se pareggio
     */
    public long getTempoRisposta() { return tempoRisposta; }
}
