package com.mycompany.server;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/**
 * Controller del pannello amministratore.
 *
 * Gestisce 3 viste navigate dai bottoni in alto:
 *  - Documenti: seleziona file .txt e avvia analisi
 *  - Classifica: mostra la classifica dei giocatori
 *  - Storico: mostra lo storico di tutte le partite
 */
public class AdminController {

    // ── Navigazione ──────────────────────────────────────────────────────
    @FXML private Button btnDocumenti;
    @FXML private Button btnClassifica;
    @FXML private Button btnStorico;

    @FXML private VBox panelDocumenti;
    @FXML private VBox panelClassifica;
    @FXML private VBox panelStorico;

    // ── Pannello Documenti ────────────────────────────────────────────────
    @FXML private TextArea fileArea;
    @FXML private TextArea risultatiArea;
    @FXML private Label    statoLabel;

    // ── Pannello Classifica ───────────────────────────────────────────────
    @FXML private TextField barCercaClassifica;
    @FXML private TableView<String[]>       classificaTable;
    @FXML private TableColumn<String[], String> colPos;
    @FXML private TableColumn<String[], String> colNickname;
    @FXML private TableColumn<String[], String> colVittorie;
    @FXML private TableColumn<String[], String> colSconfitte;
    @FXML private TableColumn<String[], String> colPareggi;
    @FXML private TableColumn<String[], String> colTempoMedio;

    // ── Pannello Storico ──────────────────────────────────────────────────
    @FXML private TextField barCercaStorico;
    @FXML private TableView<RisultatoPartita>       storicoTable;
    @FXML private TableColumn<RisultatoPartita, String> colData;
    @FXML private TableColumn<RisultatoPartita, String> colGiocatore1;
    @FXML private TableColumn<RisultatoPartita, String> colGiocatore2;
    @FXML private TableColumn<RisultatoPartita, String> colVincitore;
    @FXML private TableColumn<RisultatoPartita, String> colDurata;

    // ── DAO ──────────────────────────────────────────────────────────────
    private final UtenteDAO  utenteDAO  = new UtenteDAO();
    private final PartitaDAO partitaDAO = new PartitaDAO();

    private List<File> fileSelezionati;

    // ------------------------------------------------------------------
    //  Inizializzazione
    // ------------------------------------------------------------------

    public void initialize() {
        // ── Classifica: colonne ──
        // getClassifica() → [username, vittorie, sconfitte, pareggi, tempoMedio]
        colPos.setCellValueFactory(data -> {
            int idx = classificaTable.getItems().indexOf(data.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        colNickname.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[0]));
        colVittorie.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[1]));
        colSconfitte.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[2]));
        colPareggi.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[3]));
        colTempoMedio.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[4]));

        // ── Storico: colonne ──
        // selectAll() → RisultatoPartita con getData(), getGiocatore1/2(), getVincitore(), getTempoRisposta()
        colData.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getData()));
        colGiocatore1.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getGiocatore1()));
        colGiocatore2.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getGiocatore2()));
        colVincitore.setCellValueFactory(data -> {
            String v = data.getValue().getVincitore();
            return new SimpleStringProperty(v != null ? v : "Pareggio");
        });
        colDurata.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getTempoRisposta())));
        
        
        
    }

    // ------------------------------------------------------------------
    //  Navigazione tra pannelli
    // ------------------------------------------------------------------

    @FXML
    private void mostraDocumenti() {
        mostra(panelDocumenti);
        aggiornaBtnNaviga(btnDocumenti);
    }

    @FXML
    private void mostraClassifica() {
        mostra(panelClassifica);
        aggiornaBtnNaviga(btnClassifica);
        aggiornaClassifica();
    }

    @FXML
    private void mostraStorico() {
        mostra(panelStorico);
        aggiornaBtnNaviga(btnStorico);
        aggiornaStorico();
    }

    /** Rende visibile solo il pannello indicato. */
    private void mostra(VBox pannelloAttivo) {
        VBox[] pannelli = { panelDocumenti, panelClassifica, panelStorico };
        for (VBox p : pannelli) {
            boolean attivo = p == pannelloAttivo;
            p.setVisible(attivo);
            p.setManaged(attivo);
        }
    }

    /** Evidenzia il bottone di navigazione attivo. */
    private void aggiornaBtnNaviga(Button attivo) {
        Button[] btns = { btnDocumenti, btnClassifica, btnStorico };
        for (Button b : btns) {
            if (b == attivo) {
                b.setStyle("-fx-background-color: white; -fx-font-weight: bold;");
            } else {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
            }
        }
    }

    // ------------------------------------------------------------------
    //  Pannello Documenti
    // ------------------------------------------------------------------

    @FXML
    private void selezionaDocumenti() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona documenti di testo");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt")
        );
        List<File> files = chooser.showOpenMultipleDialog(
            fileArea.getScene().getWindow()
        );
        if (files != null && !files.isEmpty()) {
            fileSelezionati = files;
            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                sb.append(f.getAbsolutePath()).append("\n");
            }
            fileArea.setText(sb.toString());
            statoLabel.setText(files.size() + " file selezionati.");
            risultatiArea.clear();
        }
    }

    @FXML
    private void avviaAnalisi() {
        if (fileSelezionati == null || fileSelezionati.isEmpty()) {
            statoLabel.setText("Seleziona prima i documenti.");
            return;
        }

        risultatiArea.clear();
        statoLabel.setText("Analisi in corso...");

        AnalysisTask task = new AnalysisTask(fileSelezionati);

        // Aggiorna lo stato in tempo reale mentre il task gira
        statoLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            statoLabel.textProperty().unbind();
            statoLabel.setText("Analisi completata.");
            risultatiArea.setText(task.getValue());
        });

        task.setOnFailed(event -> {
            statoLabel.textProperty().unbind();
            statoLabel.setText("Errore: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void salvaAnalisi() {
        Map<String, Long> tf   = ClientHandler.getTfMap();
        List<String>      frasi = ClientHandler.getFrasi();
        if (tf == null || frasi == null) {
            statoLabel.setText("Nessuna analisi da salvare. Avvia prima l'analisi.");
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("analisi.ser"))) {
            oos.writeObject(tf);
            oos.writeObject(frasi);
            statoLabel.setText("Analisi salvata in analisi.ser (" + frasi.size() + " frasi).");
        } catch (IOException e) {
            statoLabel.setText("Errore salvataggio: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void caricaAnalisi() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("analisi.ser"))) {
            Map<String, Long> tf   = (Map<String, Long>) ois.readObject();
            List<String>      frasi = (List<String>)      ois.readObject();
            ClientHandler.setTfMap(tf);
            ClientHandler.setFrasi(frasi);
            statoLabel.setText("Analisi caricata: " + frasi.size() + " frasi, "
                    + tf.size() + " parole distinte.");
        } catch (Exception e) {
            statoLabel.setText("Nessuna analisi salvata trovata (analisi.ser).");
        }
    }

    // ------------------------------------------------------------------
    //  Pannello Classifica
    // ------------------------------------------------------------------

    private void aggiornaClassifica() {
        ObservableList<String[]> items = FXCollections.observableArrayList();
        List<String[]> dati = utenteDAO.getClassifica();
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        classificaTable.setItems(items);
    }
    
    @FXML
    private void rilasciaFile(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean successo = false;

        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            
           //Filtraggio dile .txt
            List<File> fileTxt = files.stream()
                                     .filter(f -> f.getName().toLowerCase().endsWith(".txt"))
                                     .collect(Collectors.toList());

            if (!fileTxt.isEmpty()) {
                fileSelezionati = fileTxt;
                
                StringBuilder sb = new StringBuilder();
                for (File f : fileTxt) {
                    sb.append(f.getAbsolutePath()).append("\n");
                }
                
                fileArea.setText(sb.toString());
                statoLabel.setText(fileTxt.size() + " file inseriti tramite Drag & Drop.");
                risultatiArea.clear();
                successo = true;
            } else {
                statoLabel.setText("Nessun file .txt valido trovato.");
            }
        }
        
        // Comunica a JavaFX se il rilascio è andato a buon fine
        event.setDropCompleted(successo);
    }

    @FXML
    private void trascinaFile(DragEvent event) {
        // Controlliamo se l'oggetto trascinato contiene effettivamente dei file
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void cercaGiocatoreClassifica(KeyEvent event) {
        ObservableList<String[]> items = FXCollections.observableArrayList();
        List<String[]> dati = utenteDAO.getClassifica().stream().filter((u) -> u[0].contains(event.getText())).collect(Collectors.toList());
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        classificaTable.setItems(items);
    }

    // ------------------------------------------------------------------
    //  Pannello Storico
    // ------------------------------------------------------------------

    private void aggiornaStorico() {
        ObservableList<RisultatoPartita> items = FXCollections.observableArrayList();
        List<RisultatoPartita> dati = partitaDAO.selectAll();
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        storicoTable.setItems(items);
    }

    @FXML
    private void cercaGiocatoreStorico(KeyEvent event) {
        ObservableList<RisultatoPartita> items = FXCollections.observableArrayList();
        List<RisultatoPartita> dati = partitaDAO.selectAll().stream().filter((u) -> u.getVincitore().contains(event.getText())).collect(Collectors.toList());
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        storicoTable.setItems(items);
    }
}