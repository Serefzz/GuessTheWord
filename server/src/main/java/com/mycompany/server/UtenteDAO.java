package com.mycompany.server;

import com.mycompany.common.Ruolo;
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
 * DAO per la tabella {@code Utente} del database SQLite.
 *
 * Implementa {@link DAOusername}{@code Utente}
 *
 * Le password vengono sempre salvate come hash SHA-256 tramite
 * {@link #hashPassword(String)}. Ogni metodo apre e chiude la propria
 * connessione JDBC con try-with-resources (versione con connessione non persistente).
 */
public class UtenteDAO implements DAOusername<Utente> {

    private static final String URL = DatabaseManager.URL;

    // ------------------------------------------------------------------
    //  Metodi CRUD (interfaccia DAO<Utente>)
    // ------------------------------------------------------------------

    /**
     * Restituisce tutti gli utenti registrati nel sistema.
     *
     * @return lista di tutti gli utenti; lista vuota se la tabella è vuota
     */
    @Override
    public List<Utente> selectAll() {
        List<Utente> lista = new ArrayList<Utente>();
        String sql = "SELECT username, ruolo, punteggio_globale FROM Utente";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ruolo ruoloUtente = (rs.getString("ruolo").equals("GIOCATORE")) ? Ruolo.GIOCATORE : Ruolo.ADMIN;
                lista.add(new Utente(rs.getString("username"), "", ruoloUtente, rs.getInt("punteggio_globale")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Cerca un utente per username.
     *
     * @param username identificatore univoco dell'utente
     * @return {@link Optional} con l'utente se trovato, vuoto altrimenti
     */
    @Override
    public Optional<Utente> selectByUsername(String username) {
        String sql = "SELECT username, ruolo, punteggio_globale FROM Utente WHERE username = ? AND ruolo = 'GIOCATORE'";
        try (Connection conn = DriverManager.getConnection(URL);
               PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Ruolo ruoloUtente = (rs.getString("ruolo").equals("GIOCATORE")) ? Ruolo.GIOCATORE : Ruolo.ADMIN;
                return Optional.of(new Utente(rs.getString("username"),"", ruoloUtente, rs.getInt("punteggio_globale")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Inserisce un nuovo utente nel database.
     *
     * @param u utente da inserire
     * @return {@code true} se l'inserimento è riuscito; {@code false} se l'username è già in uso
     */
    @Override
    public boolean insert(Utente u) {
        String sql = "INSERT INTO Utente (username, password, ruolo, punteggio_globale) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRuolo().name().toUpperCase());
            ps.setInt(4, u.getPunteggioGlobale());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }
    }

    /**
     * Aggiorna password e punteggio globale di un utente esistente.
     *
     * @param u utente con l'username di un utente gia' presente nel db e con il nuovo stato da aggiornare
     * @return {@code true} se la modifica ha coinvolto almeno una riga; {@code false} altrimenti
     */
    @Override
    public boolean update(Utente u) {
        String sql = "UPDATE Utente SET ruolo = ?, punteggio_globale = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getRuolo().name().toUpperCase());
            ps.setInt(2, u.getPunteggioGlobale());
            ps.setString(3, u.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina l'utente con l'identificatore specificato.
     *
     * @param username identificatore univoco dell'utente da eliminare
     * @return {@code true} se l'eliminazione ha coinvolto almeno una riga; {@code false} altrimenti
     */
    @Override
    public boolean delete(String username) {
        String sql = "DELETE FROM Utente WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
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
     * Verifica le credenziali di un utente confrontando la password fornita
     * (hashata) con quella memorizzata nel database.
     *
     * @param username nome utente da verificare
     * @param password password in chiaro da verificare
     * @return {@code true} se le credenziali sono corrette; {@code false} altrimenti
     */
    public boolean verificaLogin(String username, String password) {
        String sql = "SELECT password FROM Utente WHERE username = ?";
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
     * Registra un nuovo giocatore con ruolo {@code GIOCATORE}.
     * La password viene automaticamente hashata con SHA-256.
     *
     * @param username nome utente desiderato
     * @param password password in chiaro scelta dall'utente
     * @return {@code true} se la registrazione è riuscita; {@code false} se l'username è già in uso
     */
    public boolean registra(String username, String password) {
        return insert(new Utente(username, hashPassword(password), Ruolo.GIOCATORE, 0));
    }
    
    public boolean aggiornaPunteggio(String username, int punti) {
        Utente uVecchioPunteggio = selectByUsername(username).get();
        Utente uNuovoPunteggio;
        if(uVecchioPunteggio != null ) {
            if(uVecchioPunteggio.getPunteggioGlobale() + punti >= 0) {
                uNuovoPunteggio = new Utente(uVecchioPunteggio.getUsername(), "", uVecchioPunteggio.getRuolo(), uVecchioPunteggio.getPunteggioGlobale() + punti);
            } else {
                uNuovoPunteggio = new Utente(uVecchioPunteggio.getUsername(), "", uVecchioPunteggio.getRuolo(), 0);
            }
        } else {
            return false;
        }
         return update(uNuovoPunteggio);
    }

    /**
     * Restituisce la classifica globale dei giocatori ordinata per punteggio globale decrescente.
     * Ogni elemento: {@code [punteggioGlobale, username, vittorie, sconfitte, pareggi, tempoMedioMs]}.
     *
     * @return lista di righe della classifica; lista vuota se non ci sono giocatori
     */
    public List<String[]> getClassifica() {
        List<String[]> classifica = new ArrayList<String[]>();
        String sql =
            "SELECT U.punteggio_globale AS punteggio_globale, U.username AS username, "
                + "  COUNT(CASE WHEN (P.giocatore1 = U.username AND P.vincitore = 1) OR (P.giocatore2 = U.username AND P.vincitore = 2) THEN 1 END) AS vittorie, "
                + "  COUNT(CASE WHEN (P.giocatore1 = U.username AND P.vincitore = 2) OR (P.giocatore2 = U.username AND P.vincitore = 1) THEN 1 END) AS sconfitte, "
                + "  COUNT(CASE WHEN P.id IS NOT NULL AND P.vincitore IS 0 THEN 1 END) AS pareggi, "
                + "  COALESCE(AVG(CASE WHEN (P.giocatore1 = U.username AND P.vincitore = 1) OR (P.giocatore2 = U.username AND P.vincitore = 2) THEN P.durata END), 0) AS tempo_medio "
                + "FROM Utente U LEFT JOIN Partita P ON P.giocatore1 = U.username OR P.giocatore2 = U.username "
                + "WHERE U.ruolo = 'GIOCATORE' "
                + "GROUP BY U.username "
                + "ORDER BY U.punteggio_globale DESC";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                classifica.add(new String[]{
                    String.valueOf(rs.getInt("punteggio_globale")),
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

    // ------------------------------------------------------------------
    //  Utility
    // ------------------------------------------------------------------
    
    /**
     * Verifica se l'utente specificato ha ruolo {@code ADMIN}.
     *
     * @param username nome utente da verificare
     * @return {@code true} se l'utente è amministratore; {@code false} altrimenti
     */
    public boolean isAdmin(String username) {
        String sql = "SELECT ruolo FROM Utente WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("ruolo").equals("ADMIN");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

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
