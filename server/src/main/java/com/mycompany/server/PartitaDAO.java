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
 * DAO per la tabella {@code partite} del database SQLite.
 *
 * <p>Implementa {@link DAO}{@code <RisultatoPartita>} e aggiunge metodi
 * specifici per la logica di gioco: salvataggio rapido di un risultato
 * e recupero dello storico partite di un singolo giocatore.</p>
 *
 * <p>Ogni metodo apre e chiude la propria connessione JDBC con
 * try-with-resources (pattern dalle slide JDBC).</p>
 */
public class PartitaDAO implements DAO<RisultatoPartita> {

    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Restituisce tutte le partite registrate, ordinate dalla più recente.
     *
     * @return lista di {@link RisultatoPartita}; lista vuota se non ci sono partite
     */
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

    /**
     * Cerca una partita tramite il suo identificatore numerico.
     *
     * @param id identificatore univoco della partita
     * @return {@link Optional} contenente la partita se trovata, vuoto altrimenti
     */
    @Override
    public Optional<RisultatoPartita> selectById(int id) {
        String sql = "SELECT id, giocatore1, giocatore2, vincitore, data, tempo_risposta " +
                     "FROM partite WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(creaRisultato(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Inserisce un nuovo risultato di partita nel database.
     *
     * @param r risultato della partita da inserire
     * @return {@code true} se l'inserimento è riuscito; {@code false} altrimenti
     */
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

    /**
     * Non implementato: le partite non vengono modificate dopo la registrazione.
     *
     * @param r parametro ignorato
     * @return sempre {@code false}
     */
    @Override
    public boolean update(RisultatoPartita r) { return false; }

    /**
     * Elimina la partita con l'identificatore specificato.
     *
     * @param id identificatore univoco della partita da eliminare
     * @return {@code true} se l'eliminazione ha coinvolto almeno una riga; {@code false} altrimenti
     */
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

    /**
     * Salva il risultato di una partita appena conclusa impostando automaticamente
     * la data e l'ora corrente.
     *
     * @param giocatore1    username del primo giocatore
     * @param giocatore2    username del secondo giocatore
     * @param vincitore     username del vincitore, o {@code null} se pareggio
     * @param tempoRisposta millisecondi dalla sfida alla risposta corretta; {@code 0} se pareggio
     */
    public void salva(String giocatore1, String giocatore2, String vincitore, long tempoRisposta) {
        String data = LocalDateTime.now().format(FORMATO_DATA);
        insert(new RisultatoPartita(0, giocatore1, giocatore2, vincitore, data, tempoRisposta));
    }

    /**
     * Restituisce lo storico delle partite di un giocatore specifico.
     * Ogni elemento: {@code [data, avversario, risultato]} dove risultato
     * è {@code "VINTO"}, {@code "PERSO"} o {@code "PAREGGIO"}.
     *
     * @param username nome utente di cui recuperare lo storico
     * @return lista di righe storico ordinate dalla partita più recente
     */
    public List<String[]> getStorico(String username) {
        List<String[]> storico = new ArrayList<String[]>();
        String sql =
            "SELECT giocatore1, giocatore2, vincitore, data FROM partite " +
            "WHERE giocatore1 = ? OR giocatore2 = ? ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DatabaseManager.URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String avversario = rs.getString("giocatore1").equals(username)
                    ? rs.getString("giocatore2") : rs.getString("giocatore1");
                String vincitore = rs.getString("vincitore");
                String risultato = vincitore == null ? "PAREGGIO"
                    : vincitore.equals(username) ? "VINTO" : "PERSO";
                storico.add(new String[]{ rs.getString("data"), avversario, risultato });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return storico;
    }

    /**
     * Costruisce un oggetto {@link RisultatoPartita} dalla riga corrente del {@link ResultSet}.
     *
     * @param rs ResultSet posizionato sulla riga da leggere
     * @return istanza di {@link RisultatoPartita} con i dati della riga
     * @throws SQLException se si verifica un errore nell'accesso alle colonne
     */
    private RisultatoPartita creaRisultato(ResultSet rs) throws SQLException {
        return new RisultatoPartita(
            rs.getInt("id"),
            rs.getString("giocatore1"),
            rs.getString("giocatore2"),
            rs.getString("vincitore"),
            rs.getString("data"),
            rs.getLong("tempo_risposta")
        );
    }
}
