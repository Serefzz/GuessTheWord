package com.mycompany.server;

import com.mycompany.common.Ruolo;

/**
 * Modello che rappresenta un utente registrato nel sistema.
 */
public class Utente {

    private final int id;
    private final String username;
    private final String password;
    private final Ruolo ruolo;

    public Utente(int id, String username, String password, Ruolo ruolo) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
    }

    public int getId() { 
        return id; 
    }
    public String getUsername(){ 
        return username;
    }
    public String getPassword(){
        return password;
    }
    public Ruolo getRuolo(){
        return ruolo; 
    }

    @Override
    public String toString() {
        return "Utente{" + username + ", " + ruolo + "}";
    }
}