package com.mycompany.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO per la tabella "Documento".
 * Gestisce l'inserimento dei documenti nel DB e la copia dei file sorgente
 * nella cartella locale data/documenti/.
 */
public class DocumentoDAO implements DAOid<String[]> {

    private static final String URL = DatabaseManager.URL;

    /** Cartella locale dove vengono copiati i documenti sorgente. */
    private static final Path CARTELLA_DOCUMENTI = Paths.get("data", "documenti");

    public DocumentoDAO() {
        // Crea la cartella se non esiste
        try {
            Files.createDirectories(CARTELLA_DOCUMENTI);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare cartella documenti", e);
        }
    }

    // ------------------------------------------------------------------
    //  CRUD
    // ------------------------------------------------------------------

    /**
     * Restituisce tutte le informazioni su i documenti.
     * Ogni riga: [id, path_sorgente]
     */
    @Override
    public List<String[]> selectAll() {
        List<String[]> lista = new ArrayList<String[]>();
        String query = "SELECT id, path_sorgente " +
                               "FROM Documento";
        try (Connection conn = DriverManager.getConnection(URL);
               PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("path_sorgente")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Restituisce tutte le informazioni sul documento relativo all'id corrispondente o un optional vuoto.
     * Ogni riga: [id, path_sorgente]
     */
    @Override
    public Optional<String[]> selectById(int...id) {
        String query = "SELECT id, path_sorgente " +
                                "FROM Documento " +
                                "WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
               PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id[0]);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("path_sorgente")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Inserisce un documento: row[0] = path sorgente originale.
     * Copia il file in data/documenti/ e registra il record nel DB.
     */
    @Override
    public boolean insert(String[] row) {
        String query = "INSERT INTO Documento (path_sorgente) " + 
                               "VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL);
               PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, row[0]);
            ps.executeUpdate();

            // Recupera l'id generato e copia il file
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                Path destinazione = CARTELLA_DOCUMENTI.resolve("documento_" + id + ".txt");
                Files.copy(Paths.get(row[0]), destinazione, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Inserisce un documento nel DB e salva le analisi TF
     * come file serializzati in data/analisi/.
     *
     * @param pathSorgente del documento da salvare
     * 
     * @return intero corrispondente all'esito dell'operazione: >= 0 se e' andata bene, 
     * -1 se il documento era gia' presente nel db o -2 se c'e' stato un altro problema
     */
    public int inserisciDocumento(String pathSorgente) {
        String query = "INSERT INTO Documento (path_sorgente) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL);
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pathSorgente);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                Path destinazione = CARTELLA_DOCUMENTI.resolve("documento_" + id + ".txt");
                Files.copy(Paths.get(pathSorgente), destinazione, StandardCopyOption.REPLACE_EXISTING);
                return id;
            }
        } catch (SQLException | IOException e) {
            if (e.getMessage() != null && e.getMessage().toUpperCase().contains("UNIQUE")) {
                return -1;
            }
        }
        return -2;
    }

    @Override
    public boolean update(String[] row) {
        // I documenti non vengono aggiornati
        return false;
    }

    /**
     * Elimina il documento relativo all'id.
     */
    @Override
    public boolean delete(int id) {
        // Elimina anche il file locale
        Path file = CARTELLA_DOCUMENTI.resolve("documento_" + id + ".txt");
        try { Files.deleteIfExists(file); } catch (IOException e) { e.printStackTrace(); }

        String query = "DELETE FROM documento WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
