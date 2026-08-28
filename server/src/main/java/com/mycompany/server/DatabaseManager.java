package com.mycompany.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestore centralizzato del database SQLite dell'applicazione.
 *
 * Fornisce l'URL di connessione JDBC usato da tutti i DAO e il metodo
 * {@link #inizializza()} da chiamare una sola volta all'avvio del server.
 * Le tabelle vengono create con {@code CREATE TABLE IF NOT EXISTS}, quindi
 * chiamate successive sono idempotenti.
 *
 * Utente admin di default: username {@code admin}, password {@code admin}
 * (hash SHA-256). Viene inserito con {@code INSERT OR IGNORE}, quindi non
 * sovrascrive un admin già presente.
 *
 * Questa classe non è istanziabile (costruttore privato).
 */
public class DatabaseManager {

    /** URL JDBC del database SQLite. Usato da tutti i DAO. */
    public static final String URL = "jdbc:sqlite:guesstheworld.db";

    /** Impedisce l'istanziazione della classe di utilità. */
    private DatabaseManager() {}

    /**
     * Inizializza il database: crea le tabelle {@code utenti} e {@code partite}
     * se non esistono, e inserisce l'utente admin di default se non presente.
     *
     * <p>Deve essere chiamato una sola volta all'avvio, prima di qualsiasi
     * operazione sui DAO.</p>
     *
     * @throws RuntimeException se la connessione al database fallisce
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

            // SHA-256 di "admin" calcolato offline
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
