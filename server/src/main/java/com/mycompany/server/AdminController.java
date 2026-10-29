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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Font;

/**
 * Controller del pannello amministratore.
 *
 * Gestisce 3 viste navigate dai bottoni in alto:
 *  - Documenti: seleziona file .txt e avvia analisi
 *  - Classifica: mostra la classifica dei giocatori
 *  - Storico: mostra lo storico di tutte le partite
 */
public class AdminController {
    
    @FXML private Label titoloLabel;

    // Navbar
    @FXML private Button btnDocumenti;
    @FXML private Button btnClassifica;
    @FXML private Button btnStorico;

    @FXML private VBox panelDocumenti;
    @FXML private VBox panelClassifica;
    @FXML private VBox panelStorico;

    // Pannello Documenti 
    @FXML private TextArea fileArea;
    @FXML private TextArea risultatiArea;
    @FXML private Label statoDocumentiLabel;
    @FXML private Label statoAnalisiLabel;

    // Pannello Classifica
    @FXML private TextField barCercaClassifica;
    @FXML private TableView<String[]> classificaTable;
    @FXML private TableColumn<String[], String> colPunteggio;
    @FXML private TableColumn<String[], String> colNickname;
    @FXML private TableColumn<String[], String> colVittorie;
    @FXML private TableColumn<String[], String> colSconfitte;
    @FXML private TableColumn<String[], String> colPareggi;
    @FXML private TableColumn<String[], String> colTempoMedio;

    // Pannello Storico
    @FXML private TextField barCercaStorico;
    @FXML private TableView<RisultatoPartita> storicoTable;
    @FXML private TableColumn<RisultatoPartita, String> colData;
    @FXML private TableColumn<RisultatoPartita, String> colGiocatore1;
    @FXML private TableColumn<RisultatoPartita, String> colGiocatore2;
    @FXML private TableColumn<RisultatoPartita, String> colVincitore;
    @FXML private TableColumn<RisultatoPartita, String> colDurata;

    // DAO
    private final UtenteDAO  utenteDAO  = new UtenteDAO();
    private final PartitaDAO partitaDAO = new PartitaDAO();
    private final FraseDAO fraseDAO = new FraseDAO();
    private final DocumentoDAO documentoDAO = new DocumentoDAO();

    private List<File> fileSelezionati;

    // ------------------------------------------------------------------
    //  Inizializzazione
    // ------------------------------------------------------------------

    public void initialize() {
        
        fileArea.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
        risultatiArea.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
        
        Font fontPersonalizzato = Font.loadFont(getClass().getResourceAsStream("/fonts/ZagDropsThin.ttf"), 38);
        if (fontPersonalizzato != null) {
            titoloLabel.setFont(fontPersonalizzato);
        }
        
        // Classifica: colonne
        // getClassifica() [punteggioGlobale, username, vittorie, sconfitte, pareggi, tempoMedio]
        colPunteggio.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue()[0]));
        colNickname.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[1]));
        colVittorie.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[2]));
        colSconfitte.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[3]));
        colPareggi.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[4]));
        colTempoMedio.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue()[5]));

        // Storico: colonne
        // selectAll() RisultatoPartita con getData(), getGiocatore1(), getGiocatore2(), getVincitore(), getDurata()
        colData.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getData()));
        colGiocatore1.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getGiocatore1()));
        colGiocatore2.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getGiocatore2()));
        colVincitore.setCellValueFactory(data -> {
            switch (data.getValue().getVincitore()) {
                case 0 : return new SimpleStringProperty("Nessuno");
                case 1 : return new SimpleStringProperty(data.getValue().getGiocatore1());
                case 2 : return new SimpleStringProperty(data.getValue().getGiocatore2());
                default : return new SimpleStringProperty("Sconosciuto");
            }
        });
        colDurata.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getDurata())));
        
        
        
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
            boolean attivo = (p == pannelloAttivo);
            p.setVisible(attivo);
            p.setManaged(attivo);
        }
    }

    /** Evidenzia il bottone di navigazione attivo. */
    private void aggiornaBtnNaviga(Button attivo) {
        btnDocumenti.getStyleClass().remove("nav-button-active");
        btnClassifica.getStyleClass().remove("nav-button-active");
        btnStorico.getStyleClass().remove("nav-button-active");
    
        if (!attivo.getStyleClass().contains("nav-button-active")) {
            attivo.getStyleClass().add("nav-button-active");
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
            statoDocumentiLabel.setText(files.size() + " file selezionati.");
            risultatiArea.clear();
        }
    }

    @FXML
    private void rilasciaFile(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean successo = false;
        if (db.hasFiles()) {
            List<File> fileTxt = db.getFiles().stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".txt"))
                    .collect(Collectors.toList());

            if (!fileTxt.isEmpty()) {
                if (fileSelezionati == null) {
                    fileSelezionati = new ArrayList<>();
                }
                // Aggiunge solo i file non già presenti
                for (File f : fileTxt) {
                    if (!fileSelezionati.contains(f)) {
                        fileSelezionati.add(f);
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (File f : fileSelezionati) {
                    sb.append(f.getAbsolutePath()).append("\n");
                }
                fileArea.setText(sb.toString());
                statoDocumentiLabel.setText(fileSelezionati.size() + " file totali.");
                risultatiArea.clear();
                successo = true;
            } else {
                statoDocumentiLabel.setText("Nessun file .txt valido trovato.");
            }
        }
        event.setDropCompleted(successo);
    }

    @FXML
    private void trascinaFile(DragEvent event) {
        // Controlla se l'oggetto trascinato contiene effettivamente dei file
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @FXML
    private void rimuoviDocumenti(){
        if(fileSelezionati == null || fileSelezionati.isEmpty()){
            statoDocumentiLabel.setText("Nessun documento da rimuovere");
            return;
        }
        fileSelezionati = null;
        fileArea.clear();
        risultatiArea.clear();
        statoDocumentiLabel.setText("Documenti rimossi. Seleziona altri documenti...");
    }

    @FXML
    private void avviaAnalisi() {
        if (fileSelezionati == null || fileSelezionati.isEmpty()) {
            statoAnalisiLabel.setText("Seleziona prima i documenti.");
            return;
        }

        risultatiArea.clear();
        statoAnalisiLabel.setText("Analisi in corso...");

        AnalysisTask task = new AnalysisTask(fileSelezionati);

        // Aggiorna lo stato in tempo reale mentre il task gira
        statoAnalisiLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            statoAnalisiLabel.textProperty().unbind();
            statoAnalisiLabel.setText("Analisi completata.");
            risultatiArea.setText(task.getValue());
        });

        task.setOnFailed(event -> {
            statoAnalisiLabel.textProperty().unbind();
            statoAnalisiLabel.setText("Errore: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void salvaAnalisi() {
        // Controlla presenza file selezionati
        if (fileSelezionati == null || fileSelezionati.isEmpty()) {
            statoAnalisiLabel.setText("Nessun documento selezionato.");
            return;
        }

        // Recupera analisi da ClientHandler
        List<List<AnalisiFrase>> tf = ClientHandler.getTfDocumenti();
        if (tf == null || tf.isEmpty()) {
            statoAnalisiLabel.setText("Nessuna analisi da salvare. Avvia prima l'analisi.");
            return;
        }
        
        // Controlla se il totale delle frasi estratte da tutti i documenti è 0
        int totaleFrasi = tf.stream().mapToInt(List::size).sum();
        if (totaleFrasi == 0) {
            statoAnalisiLabel.setText("I documenti analizzati sono vuoti (0 frasi valide). Nessun documento salvato.");
            return;
        }

        try {
            int documentiSalvati = 0;
            int frasiSalvate = 0;
            for (int i = 0; i < fileSelezionati.size(); i++) {
                File file = fileSelezionati.get(i);
                List<AnalisiFrase> frasiDelDocumento = tf.get(i);
                
                // Se un singolo documento (caricato insieme ad altri) è vuoto, non salvarlo nel DB
                if (frasiDelDocumento == null || frasiDelDocumento.isEmpty()) {
                    continue;
                }
                
                // Inserisce il documento nel DB e lo copia in data/documenti/
                int idDocumento = documentoDAO.inserisciDocumento(file.getAbsolutePath());
                
                if (idDocumento >= 0) {
                    // Inserisce le frasi nel DB e salva i file .ser in data/analisi/
                    fraseDAO.inserisciFrasi(idDocumento,frasiDelDocumento);
                    documentiSalvati++;
                    frasiSalvate += frasiDelDocumento.size();
                    statoAnalisiLabel.setText("Salvataggio completato: " + documentiSalvati + " documento/i e " + frasiSalvate + " frasi salvate.");
                } else if (idDocumento == -1) {
                    statoAnalisiLabel.setText("Documento già salvato: " + file.getName());
                } else if (idDocumento == -2){
                    statoAnalisiLabel.setText("Errore durante il salvataggio del documento: " + file.getName());
                    return;
                }
            }
        } catch (Exception e) {
            statoAnalisiLabel.setText("Errore durante il salvataggio dell'analisi: " + e.getMessage());
        }
    }


    @FXML
    private void caricaAnalisi() {
        List<AnalisiFrase> lista = new ArrayList<AnalisiFrase>();
        Path cartellaAnalisi = Paths.get("data", "analisi");
        try(Stream<Path> stream = Files.list(cartellaAnalisi)){
            stream.filter(p -> p.toString().endsWith(".ser")).forEach(p -> {
                try(ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(p))){
                    lista.add((AnalisiFrase) ois.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClientHandler.setTfFrasi(lista);
        statoAnalisiLabel.setText("Analisi in memoria caricate");
    }

    // ------------------------------------------------------------------
    //  Pannello Classifica
    // ------------------------------------------------------------------

    @FXML
    private void cercaGiocatoreClassifica(KeyEvent event) {
        String utenteCercato = barCercaClassifica.getText();
        List<String[]> dati = utenteDAO.getClassifica().stream()
                .filter(u -> u[1].contains(utenteCercato))
                .collect(Collectors.toList());
        classificaTable.setItems(FXCollections.observableArrayList(dati));
    }
    
    /** Aggiorna la lista di item legati alla table della classifica. */
    private void aggiornaClassifica() {
        ObservableList<String[]> items = FXCollections.observableArrayList();
        List<String[]> dati = utenteDAO.getClassifica();
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        classificaTable.setItems(items);
    }

    // ------------------------------------------------------------------
    //  Pannello Storico
    // ------------------------------------------------------------------

    @FXML
    private void cercaGiocatoreStorico(KeyEvent event) {
        String utenteCercato = barCercaStorico.getText();
        List<RisultatoPartita> dati = partitaDAO.selectAll().stream()
                .filter(p -> (p.getGiocatore1().contains(utenteCercato) || p.getGiocatore2().contains(utenteCercato)))
                .collect(Collectors.toList());
        storicoTable.setItems(FXCollections.observableArrayList(dati));
    }
    
    /** Aggiorna la lista di item legati alla table dello storico. */
    private void aggiornaStorico() {
        ObservableList<RisultatoPartita> items = FXCollections.observableArrayList();
        List<RisultatoPartita> dati = partitaDAO.selectAll();
        for (int i = 0; i < dati.size(); i++) {
            items.add(dati.get(i));
        }
        storicoTable.setItems(items);
    }
    
}