package com.mycompany.common;

/**
 * Messaggio scambiato tra client e server via socket.
 *
 * Ogni messaggio viene convertito in una riga di testo con toLine()
 * e ricostruito dalla riga con fromLine().
 * Compatibile con BufferedWriter / BufferedReader.
 *
 * Formato: TIPO|param0|param1|...
 * Separatore: |
 */
public class Messaggio {

    public enum Tipo {
        // Client → Server
        LOGIN,              // params: username, password
        REGISTRAZIONE,      // params: username, password
        AVVIA_PARTITA,      // il giocatore vuole giocare
        RISPOSTA,           // params: parola decifrata
        STORICO_REQUEST,    // nessun param
        CLASSIFICA_REQUEST, // nessun param
        DISCONNETTI,        // nessun param

        // Server → Client
        OK,                 // operazione riuscita
        ERRORE,             // params: messaggio di errore
        ATTESA,             // aspetta avversario
        SFIDA,              // params: testo, shift, secondi
        RISPOSTA_ERRATA,    // risposta sbagliata
        RISULTATO_VINTO,    // params: usernameAvversario, tempoMs
        RISULTATO_PERSO,    // params: usernameAvversario, tempoMs
        PAREGGIO,           // nessun param
        STORICO_RESPONSE,   // params: righe separate da '|'
        CLASSIFICA_RESPONSE // params: righe separate da '|'
    }

    private final Tipo tipo;
    private final String[] params;

    public Messaggio(Tipo tipo, String... params) {
        this.tipo = tipo;
        this.params = (params != null) ? params : new String[0];
    }

    // ------------------------------------------------------------------
    //  toLine: converte il messaggio in una riga da mandare via socket
    // ------------------------------------------------------------------

    public String toLine() {
        if (params.length == 0) {
            return tipo.name();
        }
        // costruisce "TIPO|param0|param1|..."
        StringBuilder sb = new StringBuilder();
        sb.append(tipo.name());
        for (int i = 0; i < params.length; i++) {
            sb.append("|");
            sb.append(params[i]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //  fromLine: ricostruisce il messaggio dalla riga letta dal socket
    // ------------------------------------------------------------------

    public static Messaggio fromLine(String linea) {
        String[] parti = linea.split("\\|", -1);
        Tipo tipo = Tipo.valueOf(parti[0]);

        if (parti.length == 1) {
            return new Messaggio(tipo);
        }

        // copia i parametri (tutto dopo il primo elemento)
        String[] params = new String[parti.length - 1];
        for (int i = 1; i < parti.length; i++) {
            params[i - 1] = parti[i];
        }
        return new Messaggio(tipo, params);
    }

    // ------------------------------------------------------------------
    //  Getter
    // ------------------------------------------------------------------

    public Tipo getTipo() {
        return tipo;
    }

    public String getParam(int index) {
        if (index >= 0 && index < params.length) {
            return params[index];
        }
        return "";
    }

    public String[] getParams() {
        return params;
    }

    @Override
    public String toString() {
        return toLine();
    }
}