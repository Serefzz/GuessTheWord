package com.mycompany.common;

/**
 * Messaggio scambiato tra client e server via socket.
 *
 * Formato stringa: {@code TIPO|param0|param1|...}
 * Compatibile con {@link java.io.BufferedWriter} / {@link java.io.BufferedReader}.
 *
 * Ogni messaggio è immutabile: tipo e parametri vengono impostati
 * nel costruttore e non possono essere modificati.
 */
public class Messaggio {

    /**
     * Tipi di messaggio del protocollo client-server.
     *
     * I messaggi {@code Client → Server} vengono inviati dal client:
     * {@link #LOGIN}, {@link #REGISTRAZIONE}, {@link #AVVIA_PARTITA},
     * {@link #RISPOSTA}, {@link #STORICO_REQUEST}, {@link #CLASSIFICA_REQUEST},
     * {@link #DISCONNETTI}.
     *
     * I messaggi {@code Server → Client} vengono inviati dal server:
     * {@link #OK}, {@link #ERRORE}, {@link #ATTESA}, {@link #SFIDA},
     * {@link #RISPOSTA_ERRATA}, {@link #RISULTATO_VINTO}, {@link #RISULTATO_PERSO},
     * {@link #PAREGGIO}, {@link #STORICO_RESPONSE}, {@link #CLASSIFICA_RESPONSE}.
     */
    public enum Tipo {
        // Client → Server
        /** Richiesta di login. Parametri: username, password. */
        LOGIN,
        /** Richiesta di registrazione. Parametri: username, password. */
        REGISTRAZIONE,
        /** Richiesta di avvio partita. Parametri: difficolta (1/2/3). */
        AVVIA_PARTITA,
        /** Risposta del giocatore. Parametri: parola decifrata. */
        RISPOSTA,
        /** Richiesta storico partite dell'utente. */
        STORICO_REQUEST,
        /** Richiesta classifica globale. */
        CLASSIFICA_REQUEST,
        /** Notifica di disconnessione volontaria. */
        DISCONNETTI,

        // Server → Client
        /** Operazione completata con successo. */
        OK,
        /** Errore generico. Parametri: messaggio di errore. */
        ERRORE,
        /** Il server è in attesa del secondo giocatore. */
        ATTESA,
        /** Sfida inviata al giocatore. Parametri: testo, shift, secondi. */
        SFIDA,
        /** La risposta inviata era errata. */
        RISPOSTA_ERRATA,
        /** Il giocatore ha vinto. Parametri: usernameAvversario, tempoMs. */
        RISULTATO_VINTO,
        /** Il giocatore ha perso. Parametri: usernameAvversario, tempoMs. */
        RISULTATO_PERSO,
        /** Pareggio per tempo scaduto. */
        PAREGGIO,
        /** Risposta allo storico. Parametri: dati serializzati. */
        STORICO_RESPONSE,
        /** Risposta alla classifica. Parametri: dati serializzati. */
        CLASSIFICA_RESPONSE
    }

    private final Tipo     tipo;
    private final String[] params;

    /**
     * Costruisce un messaggio con tipo e parametri opzionali.
     *
     * @param tipo   tipo del messaggio
     * @param params parametri aggiuntivi (variadic, può essere vuoto)
     */
    public Messaggio(Tipo tipo, String... params) {
        this.tipo   = tipo;
        this.params = (params != null) ? params : new String[0];
    }

    /**
     * Serializza il messaggio in una riga da inviare via socket.
     * Formato: {@code TIPO} oppure {@code TIPO|param0|param1|...}
     *
     * @return stringa serializzata del messaggio
     */
    public String toLine() {
        if (params.length == 0) return tipo.name();
        StringBuilder sb = new StringBuilder(tipo.name());
        for (int i = 0; i < params.length; i++) {
            sb.append("|").append(params[i]);
        }
        return sb.toString();
    }

    /**
     * Deserializza un messaggio dalla riga letta dal socket.
     *
     * @param linea stringa nel formato {@code TIPO|param0|param1|...}
     * @return istanza di {@link Messaggio} ricostruita dalla stringa
     * @throws IllegalArgumentException se il tipo non è riconosciuto
     */
    public static Messaggio fromLine(String linea) {
        String[] parti = linea.split("\\|", -1);
        Tipo tipo = Tipo.valueOf(parti[0]);
        if (parti.length == 1) return new Messaggio(tipo);
        String[] params = new String[parti.length - 1];
        for (int i = 1; i < parti.length; i++) params[i - 1] = parti[i];
        return new Messaggio(tipo, params);
    }

    /**
     * @return tipo del messaggio
     */
    public Tipo getTipo() { 
        return tipo; 
    }

    /**
     * Restituisce il parametro all'indice specificato.
     *
     * @param i indice del parametro (base 0)
     * @return valore del parametro, o stringa vuota se l'indice è fuori range
     */
    public String getParam(int i) {
        return (i >= 0 && i < params.length) ? params[i] : "";
    }

    /**
     * @return array di tutti i parametri del messaggio
     */
    public String[] getParams() { 
        return params; 
    }

    /**
     * @return rappresentazione testuale del messaggio (equivalente a {@link #toLine()})
     */
    @Override
    public String toString() { 
        return toLine(); 
    }
}