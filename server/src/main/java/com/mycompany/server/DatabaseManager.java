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
 *
 * Utente admin di default: username {@code admin}, password {@code admin}
 * (hash SHA-256). Viene inserito con {@code INSERT OR IGNORE}, quindi non
 * sovrascrive un admin già presente.
 *
 */
public class DatabaseManager {
    
    public static final String URL = "jdbc:sqlite:database/guesstheword.db";  /** URL JDBC del database SQLite. Usato da tutti i DAO. */

    private DatabaseManager() {}

    /**
     * Inizializza il database: inserisce l'utente admin di default se non presente.
     *
     * Deve essere chiamato una sola volta all'avvio, prima di qualsiasi
     * operazione sui DAO.
     *
     * @throws RuntimeException se la connessione al database fallisce
     */
    public static void inizializza() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // SHA-256 di "admin" calcolato offline
            stmt.execute(
                "INSERT OR IGNORE INTO Utente (username, password, ruolo) VALUES (" +
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
