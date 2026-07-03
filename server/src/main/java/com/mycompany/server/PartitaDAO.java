package com.mycompany.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per la tabella "partite".
 * 
 */
public class PartitaDAO implements DAO<RisultatoPartita> {

    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public List<RisultatoPartita> selectAll() {
        List<RisultatoPartita> lista = new ArrayList<RisultatoPartita>();
        String sql = "SELECT id, giocatore1, giocatore2, vincitore, data, tempo_risposta " +
                     "FROM partite ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(creaRisultato(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public Optional<RisultatoPartita> selectById(int id) {
        String sql = "SELECT id, giocatore1, giocatore2, vincitore, data, tempo_risposta " +
                     "FROM partite WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(creaRisultato(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public boolean insert(RisultatoPartita r) {
        String sql = "INSERT INTO partite (giocatore1, giocatore2, vincitore, data, tempo_risposta) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getGiocatore1());
            ps.setString(2, r.getGiocatore2());
            ps.setString(3, r.getVincitore());          
            ps.setString(4, r.getData());
            ps.setLong(5, r.getTempoRisposta());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(RisultatoPartita r) {
        // Le partite non si modificano — metodo non usato
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM partite WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    //  Metodi aggiuntivi specifici per la logica di gioco
    // ------------------------------------------------------------------

    /**
     * Salva il risultato di una partita appena conclusa.
     *
     * @param giocatore1    username primo giocatore
     * @param giocatore2    username secondo giocatore
     * @param vincitore     username del vincitore, null se pareggio
     * @param tempoRisposta millisecondi trascorsi dalla sfida alla risposta corretta; 0 se pareggio
     */
    public void salva(String giocatore1, String giocatore2, String vincitore, long tempoRisposta) {
        String data = LocalDateTime.now().format(FORMATO_DATA);
        RisultatoPartita r = new RisultatoPartita(0, giocatore1, giocatore2, vincitore, data, tempoRisposta);
        insert(r);
    }

    /**
     * Restituisce lo storico partite di un giocatore.
     * Ogni elemento: [data, avversario, risultato ("VINTO"/"PERSO"/"PAREGGIO")]
     */
    public List<String[]> getStorico(String username) {
        List<String[]> storico = new ArrayList<String[]>();
        String sql =
            "SELECT giocatore1, giocatore2, vincitore, data FROM partite " +
            "WHERE giocatore1 = ? OR giocatore2 = ? " +
            "ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Determina l'avversario
                String avversario = rs.getString("giocatore1").equals(username)
                    ? rs.getString("giocatore2")
                    : rs.getString("giocatore1");
                // Determina il risultato
                String vincitore = rs.getString("vincitore");
                String risultato;
                if (vincitore == null) {
                    risultato = "PAREGGIO";
                } else if (vincitore.equals(username)) {
                    risultato = "VINTO";
                } else {
                    risultato = "PERSO";
                }
                storico.add(new String[]{ rs.getString("data"), avversario, risultato });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return storico;
    }

 
    /** Costruisce un RisultatoPartita da una riga del ResultSet. */
    private RisultatoPartita creaRisultato(ResultSet rs) throws SQLException {
        return new RisultatoPartita(
            rs.getInt("id"),
            rs.getString("giocatore1"),
            rs.getString("giocatore2"),
            rs.getString("vincitore"),  // può essere null
            rs.getString("data"),
            rs.getLong("tempo_risposta")
        );
    }
}