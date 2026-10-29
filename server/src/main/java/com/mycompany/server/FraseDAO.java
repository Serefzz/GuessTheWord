package com.mycompany.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * DAO per la tabella "Frase".
 * Gestisce le frasi nel DB (colonna contenuto) e salva le analisi TF
 * come file serializzati (.ser) nella cartella locale data/analisi/.
 */
public class FraseDAO implements DAOid<String[]> {

    private static final String URL = DatabaseManager.URL;

    /** Cartella locale dove vengono salvati i file di analisi serializzati. */
    private static final Path CARTELLA_ANALISI = Paths.get("data", "analisi");

    public FraseDAO() {
        // Crea la cartella se non esiste
        try {
            Files.createDirectories(CARTELLA_ANALISI);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare cartella analisi", e);
        }
    }

    // ------------------------------------------------------------------
    //  CRUD
    // ------------------------------------------------------------------

    /**
     * Restituisce tutte le informazioni su le frasi.
     * Ogni riga: [id, documento, contenuto, path_analisi]
     */
    @Override
    public List<String[]> selectAll() {
        List<String[]> lista = new ArrayList<String[]>();
        String query = "SELECT id, documento, contenuto, path_analisi " +
                               "FROM Frase";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    String.valueOf(rs.getInt("documento")),
                    rs.getString("contenuto"),
                    rs.getString("path_analisi")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Restituisce le informazioni su la frase con l'id e il documento corrispondenti.
     * Ogni riga: [id, documento, contenuto, path_analisi]
     */
    @Override
    public Optional<String[]> selectById(int...id) {
        String sql = "SELECT id, documento, contenuto, path_analisi " + 
                           "FROM Frase " +
                           "WHERE id = ? AND documento = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id[0]);
            ps.setInt(2, id[1]);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new String[]{
                    String.valueOf(rs.getInt("id")),
                    String.valueOf(rs.getInt("documento")),
                    rs.getString("contenuto"),
                    rs.getString("path_analisi")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    /**
     * Restituisce tutte le informazioni sulle frasi di un documento.
     * Ogni riga: [id, documento, contenuto, path_analisi]
     */
    public List<String[]> selectByDocumento(int docId) {
        List<String[]> lista = new ArrayList<String[]>();
        String sql = "SELECT id, documento, contenuto, path_analisi " +
                           "FROM Frase " +
                           "WHERE documento = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, docId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    String.valueOf(rs.getInt("documento")),
                    rs.getString("contenuto"),
                    rs.getString("path_analisi")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public boolean insert(String[] row) {
        return false;
    }
    
     /**
     * Inserisce tutte le frasi di un documento nel DB e salva le analisi TF
     * come file serializzati in data/analisi/.
     *
     * @param documento id del documento a cui appartengono le frasi
     * @param analisiFrasi lista delle mappe TF corrispondenti a ciascuna frase
     */
    public void inserisciFrasi(int documento, List<AnalisiFrase> analisiFrasi) {
        String sql = "INSERT OR IGNORE INTO Frase (documento, contenuto, path_analisi) " +
                           "VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < analisiFrasi.size(); i++) {
                int numeroFrase = i + 1;
                String pathAnalisi = CARTELLA_ANALISI.resolve("analisi_" + documento + "_" + numeroFrase + ".ser").toString();

                // Serializza le mappe TF su i file .ser
                salvaAnalisi(pathAnalisi, analisiFrasi.get(i));

                // Inserisce nel DB
                ps.setInt(1, documento);
                ps.setString(2, analisiFrasi.get(i).getFrase());
                ps.setString(3, pathAnalisi);
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean update(String[] row) {
        return false;
    }
    
    /**
     * Cancella le informazioni della frase di cui passiamo l'id.
     */
    @Override
    public boolean delete(int id) {
        // Prima recupera il path_analisi per eliminare il file
        Optional<String[]> frase = selectById(id);
        if (frase.isPresent()) {
            try { Files.deleteIfExists(Paths.get(frase.get()[3])); }
            catch (IOException e) { e.printStackTrace(); }
        }

        String sql = "DELETE FROM Frase " +
                           "WHERE id = ?";
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
    //  Metodi specifici
    // ------------------------------------------------------------------

    /**
     * Restituisce le informazioni di una frase casuale dal DB.
     * Risultato: [id, documento, contenuto, path_analisi]
     *
     * @return la frase casuale, o null se non ci sono frasi
     */
    public String[] getFraseRandom() {
        String sql = "SELECT id, documento, contenuto, path_analisi " +
                           "FROM Frase " +
                           "ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                    String.valueOf(rs.getInt("id")),
                    String.valueOf(rs.getInt("documento")),
                    rs.getString("contenuto"),
                    rs.getString("path_analisi")
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Restituisce solo i contenuti di tutte le frasi.
     */
    public List<String> getContenutiFrasi() {
        List<String> contenuti = new ArrayList<String>();
        String sql = "SELECT contenuto " +
                           "FROM Frase";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contenuti.add(rs.getString("contenuto"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contenuti;
    }

    // ------------------------------------------------------------------
    //  Serializzazione analisi
    // ------------------------------------------------------------------

    /**
     * Serializza un'analisi su file .ser tramite ObjectOutputStream.
     */
    private void salvaAnalisi(String path, AnalisiFrase analisi) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(path))) {
            oos.writeObject(analisi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserializza un file .ser e restituisce l'oggetto analisi.
     *
     * @param pathAnalisi percorso del file .ser
     * @return l'analisi contenente la frase e la mappa parola → frequenza, o un'analisi vuota
     */
    public AnalisiFrase leggiAnalisi(String pathAnalisi) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(pathAnalisi))) {
            AnalisiFrase a = (AnalisiFrase) ois.readObject();
            return a;
        } catch (Exception e) {
            e.printStackTrace();
            return new AnalisiFrase("", new HashMap<String, Long>());
        }
    }
    
}
