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
 * DAO per la tabella "utenti".
 * Ogni metodo apre e chiude la propria connessione con try-with-resources
 * (pattern dalle slide JDBC).
 *
 * Ogni riga è rappresentata come String[]: [id, username, ruolo]
 */
public class UtenteDAO implements DAO<String[]> {

    // URL preso da DatabaseManager — unica fonte di verità
    private static final String URL = DatabaseManager.URL;

    // ------------------------------------------------------------------
    //  Metodi CRUD (interfaccia DAO<String[]>)
    //  Ogni String[] = [id, username, ruolo]
    // ------------------------------------------------------------------

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

    /** row = [username, passwordHash, ruolo] */
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
            return false; // username già esistente
        }
    }

    /** row = [id, username, passwordHash, ruolo] */
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
    //  Metodi specifici per la logica di autenticazione
    // ------------------------------------------------------------------

    /** Registra un nuovo giocatore. Restituisce true se riuscito. */
    public boolean registra(String username, String password) {
        return insert(new String[]{ username, hashPassword(password), "GIOCATORE" });
    }

    /** Verifica le credenziali. Restituisce true se corrette. */
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
     * Restituisce la classifica dei giocatori.
     * Ogni riga: [username, vittorie, sconfitte, pareggi, tempoMedioMs]
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
     * Restituisce true se l'utente ha ruolo ADMIN.
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