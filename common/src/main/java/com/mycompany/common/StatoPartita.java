package com.mycompany.common;

/**
 * Enumerazione degli stati possibili di una partita.
 */
public enum StatoPartita {
    /** La partita è attiva e i giocatori possono inviare risposte. */
    IN_CORSO,
    /** La partita è conclusa: un giocatore ha vinto o il tempo è scaduto. */
    TERMINATA
}