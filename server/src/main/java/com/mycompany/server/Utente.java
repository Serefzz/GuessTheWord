package com.mycompany.server;

import com.mycompany.common.Ruolo;

/**
 * Modello immutabile che rappresenta un utente registrato nel sistema.
 *
 * Ogni utente ha un ruolo ({@link Ruolo#GIOCATORE} o {@link Ruolo#ADMIN})
 * che determina le funzionalità accessibili nell'applicazione.
 */
public class Utente {

    private final int    id;
    private final String username;
    private final String password; // hash SHA-256
    private final Ruolo  ruolo;

    /**
     * Costruisce un utente con tutti i campi.
     *
     * @param id       identificatore univoco nel database
     * @param username nome utente univoco
     * @param password hash SHA-256 della password
     * @param ruolo    ruolo dell'utente ({@code GIOCATORE} o {@code ADMIN})
     */
    public Utente(int id, String username, String password, Ruolo ruolo) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.ruolo    = ruolo;
    }

    /**
     * @return identificatore univoco dell'utente nel database
     */
    public int getId() { return id; }

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
     * @return rappresentazione testuale dell'utente con username e ruolo
     */
    @Override
    public String toString() {
        return "Utente{" + username + ", " + ruolo + "}";
    }
}
