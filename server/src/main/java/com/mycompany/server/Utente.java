package com.mycompany.server;

import com.mycompany.common.Ruolo;

/**
 * Modello immutabile che rappresenta un utente registrato nel sistema.
 *
 * Ogni utente ha un ruolo ({@link Ruolo#GIOCATORE} o {@link Ruolo#ADMIN})
 * che determina le funzionalità accessibili nell'applicazione.
 */
public class Utente {

    private final String username;
    private final String password; // hash SHA-256
    private final Ruolo  ruolo;
    private int punteggioGlobale;
    
    /**
     * Costruisce un utente con tutti i campi.
     *
     * @param username nome utente univoco
     * @param password hash SHA-256 della password
     * @param ruolo ruolo dell'utente ({@code GIOCATORE} o {@code ADMIN})
     * @param punteggioGlobale punteggio dell'utente nella classifica globale
     */
    public Utente(String username, String password, Ruolo ruolo, int punteggioGlobale) {
        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
        this.punteggioGlobale = punteggioGlobale;
    }

    /**
     * Costruisce un utente con tutti i campi e il punteggio globale a 0.
     *
     * @param username nome utente univoco
     * @param password hash SHA-256 della password
     * @param ruolo    ruolo dell'utente ({@code GIOCATORE} o {@code ADMIN})
     */
    public Utente(String username, String password, Ruolo ruolo) {
        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
        this.punteggioGlobale = 0;
    }

    /**
     * @return nome utente univoco
     */
    public String getUsername() { return username; }

    /**
     * @return hash SHA-256 della password dell'utente
     */
    public String getPassword() { return password; }

    /**
     * @return ruolo dell'utente nel sistema ({@code GIOCATORE} o {@code ADMIN})
     */
    public Ruolo getRuolo() { return ruolo; }
    
    /**
     * @return punteggio globale dell'utente nel sistema
     */
    public int getPunteggioGlobale() { return punteggioGlobale; }

    /**
     * @return rappresentazione testuale dell'utente con username e ruolo
     */
    @Override
    public String toString() {
        return "Utente{" + username + ": " + ruolo + "}";
    }
    
}
