package com.mycompany.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestisce il database: URL di connessione e creazione delle tabelle.
 * Va chiamato una volta sola all'avvio del server tramite inizializza().
 * I DAO usano URL per aprire le proprie connessioni.
 */
public class DatabaseManager {

    public static final String URL = "jdbc:sqlite:guesstheworld.db";

    private DatabaseManager() {}

    /**
     * Crea le tabelle se non esistono ancora.
     * Da chiamare all'avvio del server.
     */
    public static void inizializza() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS utenti (" +
                "  id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT    UNIQUE NOT NULL," +
                "  password TEXT    NOT NULL," +
                "  ruolo    TEXT    NOT NULL DEFAULT 'GIOCATORE'" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS partite (" +
                "  id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  giocatore1     TEXT    NOT NULL," +
                "  giocatore2     TEXT    NOT NULL," +
                "  vincitore      TEXT," +
                "  data           TEXT    NOT NULL," +
                "  tempo_risposta INTEGER NOT NULL DEFAULT 0" +
                ")"
            );

            // Crea utente admin di default (password: "admin") se non esiste già.
            // L'hash è SHA-256 di "admin" calcolato offline — INSERT OR IGNORE non tocca
            // la riga se l'username esiste già (constraint UNIQUE).
            stmt.execute(
                "INSERT OR IGNORE INTO utenti (username, password, ruolo) VALUES (" +
                "'admin'," +
                "'8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918'," +
                "'ADMIN')"
            );

            System.out.println("Database inizializzato.");

        } catch (SQLException e) {
            throw new RuntimeException("Errore inizializzazione database", e);
        }
    }
}