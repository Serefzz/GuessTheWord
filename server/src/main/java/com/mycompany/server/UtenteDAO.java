package com.mycompany.server;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per la tabella {@code utenti} del database SQLite.
 *
 * Implementa {@link DAO}{@code <String[]>} dove ogni array rappresenta
 * una riga della tabella nel formato {@code [id, username, ruolo]}.
 *
 * Le password vengono sempre salvate come hash SHA-256 tramite
 * {@link #hashPassword(String)}. Ogni metodo apre e chiude la propria
 * connessione JDBC con try-with-resources (pattern dalle slide JDBC).
 */
public class UtenteDAO implements DAO<String[]> {

    private static final String URL = DatabaseManager.URL;

    // ------------------------------------------------------------------
    //  Metodi CRUD (interfaccia DAO<String[]>)
    // ------------------------------------------------------------------

    /**
     * Restituisce tutti gli utenti registrati nel sistema.
     * Ogni elemento dell'array: {@code [id, username, ruolo]}.
     *
     * @return lista di tutti gli utenti; lista vuota se la tabella è vuota
     */
    @Override
    public List<String[]> selectAll() {
        List<String[]> lista = new ArrayList<String[]>();
        String sql = "SELECT id, username, ruolo FROM utenti";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("username"),
                    rs.getString("ruolo")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Cerca un utente per identificatore numerico.
     * L'array restituito ha forma {@code [id, username, ruolo]}.
     *
     * @param id identificatore univoco dell'utente
     * @return {@link Optional} con l'utente se trovato, vuoto altrimenti
     */
    @Override
    public Optional<String[]> selectById(int id) {
        String sql = "SELECT id, username, ruolo FROM utenti WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("username"),
                    rs.getString("ruolo")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Inserisce un nuovo utente nel database.
     * Il parametro {@code row} deve avere forma {@code [username, passwordHash, ruolo]}.
     *
     * @param row array con username, hash password e ruolo dell'utente
     * @return {@code true} se l'inserimento è riuscito; {@code false} se l'username è già in uso
     */
    @Override
    public boolean insert(String[] row) {
        String sql = "INSERT INTO utenti (username, password, ruolo) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row[0]);
            ps.setString(2, row[1]);
            ps.setString(3, row[2]);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Aggiorna password e ruolo di un utente esistente.
     * Il parametro {@code row} deve avere forma {@code [id, username, passwordHash, ruolo]}.
     *
     * @param row array con id, username, nuova hash password e nuovo ruolo
     * @return {@code true} se la modifica ha coinvolto almeno una riga; {@code false} altrimenti
     */
    @Override
    public boolean update(String[] row) {
        String sql = "UPDATE utenti SET password = ?, ruolo = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row[2]);
            ps.setString(2, row[3]);
            ps.setInt(3, Integer.parseInt(row[0]));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina l'utente con l'identificatore specificato.
     *
     * @param id identificatore univoco dell'utente da eliminare
     * @return {@code true} se l'eliminazione ha coinvolto almeno una riga; {@code false} altrimenti
     */
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM utenti WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    //  Metodi specifici per autenticazione e classifiche
    // ------------------------------------------------------------------

    /**
     * Registra un nuovo giocatore con ruolo {@code GIOCATORE}.
     * La password viene automaticamente hashata con SHA-256.
     *
     * @param username nome utente desiderato
     * @param password password in chiaro scelta dall'utente
     * @return {@code true} se la registrazione è riuscita; {@code false} se l'username è già in uso
     */
    public boolean registra(String username, String password) {
        return insert(new String[]{ username, hashPassword(password), "GIOCATORE" });
    }

    /**
     * Verifica le credenziali di un utente confrontando la password fornita
     * (hashata) con quella memorizzata nel database.
     *
     * @param username nome utente da verificare
     * @param password password in chiaro da verificare
     * @return {@code true} se le credenziali sono corrette; {@code false} altrimenti
     */
    public boolean verificaLogin(String username, String password) {
        String sql = "SELECT password FROM utenti WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(hashPassword(password));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Restituisce la classifica globale dei giocatori ordinata per vittorie decrescenti.
     * Ogni elemento: {@code [username, vittorie, sconfitte, pareggi, tempoMedioMs]}.
     *
     * @return lista di righe classifica; lista vuota se non ci sono giocatori
     */
    public List<String[]> getClassifica() {
        List<String[]> classifica = new ArrayList<String[]>();
        String sql =
            "SELECT u.username, " +
            "  SUM(CASE WHEN p.vincitore = u.username THEN 1 ELSE 0 END) AS vittorie, " +
            "  SUM(CASE WHEN p.vincitore != u.username AND p.vincitore IS NOT NULL THEN 1 ELSE 0 END) AS sconfitte, " +
            "  SUM(CASE WHEN p.vincitore IS NULL THEN 1 ELSE 0 END) AS pareggi, " +
            "  COALESCE(AVG(CASE WHEN p.vincitore = u.username THEN p.tempo_risposta END), 0) AS tempo_medio " +
            "FROM utenti u " +
            "LEFT JOIN partite p ON p.giocatore1 = u.username OR p.giocatore2 = u.username " +
            "WHERE u.ruolo = 'GIOCATORE' " +
            "GROUP BY u.username " +
            "ORDER BY vittorie DESC";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                classifica.add(new String[]{
                    rs.getString("username"),
                    String.valueOf(rs.getInt("vittorie")),
                    String.valueOf(rs.getInt("sconfitte")),
                    String.valueOf(rs.getInt("pareggi")),
                    String.valueOf(rs.getLong("tempo_medio"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classifica;
    }

    /**
     * Verifica se l'utente specificato ha ruolo {@code ADMIN}.
     *
     * @param username nome utente da verificare
     * @return {@code true} se l'utente è amministratore; {@code false} altrimenti
     */
    public boolean isAdmin(String username) {
        String sql = "SELECT ruolo FROM utenti WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "ADMIN".equals(rs.getString("ruolo"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  Utility
    // ------------------------------------------------------------------

    /**
     * Calcola l'hash SHA-256 della password fornita in chiaro.
     *
     * @param password password in chiaro da hashare
     * @return stringa esadecimale di 64 caratteri rappresentante l'hash SHA-256
     * @throws RuntimeException se l'algoritmo SHA-256 non è disponibile nella JVM
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Errore hashing password", e);
        }
    }
}
