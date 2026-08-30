package com.mycompany.client;

import com.mycompany.common.Messaggio;
import com.mycompany.common.Messaggio.Tipo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/*
 * Controller della schermata principale del client.
 * FXML associato: {@code /main.fxml}
 *
 * <p>Implementa {@link MessageListener} per ricevere i messaggi dal server
 * sul thread JavaFX tramite {@code Platform.runLater()}.</p>
 *
 * <p>Gestisce quattro pannelli navigabili: {@code panelPartita} per la selezione
 * della difficolta e avvio partita, {@code panelGioco} per la schermata di gioco
 * con timer e campo risposta, {@code panelStorico} per lo storico partite
 * dell'utente con ricerca, e {@code panelClassifica} per la classifica globale
 * con ricerca.</p>
 *
 * <p>Formato storico ricevuto dal server:
 * {@code data;avversario;risultato|data;avversario;risultato|...}</p>
 *
 * <p>Formato classifica ricevuto dal server:
 * {@code username;vittorie;sconfitte;pareggi;tempoMedio|...}</p>
 */
public class MainController implements MessageListener {

    @FXML private Label  labelUsername;
    @FXML private Button btnPartita;
    @FXML private Button btnStorico;
    @FXML private Button btnClassifica;

    @FXML private VBox panelPartita;
    @FXML private VBox panelStorico;
    @FXML private VBox panelClassifica;
    @FXML private VBox panelGioco;

    @FXML private RadioButton rbFacile;
    @FXML private RadioButton rbMedio;
    @FXML private RadioButton rbDifficile;
    @FXML private Label       statoPartitaLabel;

    @FXML private Label     labelTestoSfida;
    @FXML private Label     labelTimer;
    @FXML private Label     labelEsito;
    @FXML private TextField inputRisposta;
    @FXML private Button    btnOk;
    @FXML private Button    btnGiocaAncora;

    @FXML private TableView<String[]>           storicoTable;
    @FXML private TableColumn<String[], String>  colStData;
    @FXML private TableColumn<String[], String>  colStAvversario;
    @FXML private TableColumn<String[], String>  colStRisultato;
    @FXML private TextField                      cercaStorico;

    @FXML private TableView<String[]>           classificaTable;
    @FXML private TableColumn<String[], String>  colClPosizione;
    @FXML private TableColumn<String[], String>  colClUsername;
    @FXML private TableColumn<String[], String>  colClVittorie;
    @FXML private TableColumn<String[], String>  colClSconfitte;
    @FXML private TableColumn<String[], String>  colClPareggi;
    @FXML private TableColumn<String[], String>  colClTempo;
    @FXML private TextField                      cercaClassifica;

    private ServerConnection connection;
    private String           username;
    private Timeline         timerLine;
    private int              secondiRimasti;

    private List<String[]> tuttoStorico    = new ArrayList<String[]>();
    private List<String[]> tuttaClassifica = new ArrayList<String[]>();

    /**
     * Inizializzazione automatica di JavaFX.
     * Configura le colonne delle tabelle e i listener di ricerca live.
     */
    @FXML
    public void initialize() {
        ToggleGroup tg = new ToggleGroup();
        rbFacile.setToggleGroup(tg);
        rbMedio.setToggleGroup(tg);
        rbDifficile.setToggleGroup(tg);
        rbFacile.setSelected(true);

        colStData.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
        colStAvversario.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        colStRisultato.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 2 ? d.getValue()[2] : ""));

        colClPosizione.setCellValueFactory(d -> new SimpleStringProperty(
            String.valueOf(classificaTable.getItems().indexOf(d.getValue()) + 1)));
        colClUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 0 ? d.getValue()[0] : ""));
        colClVittorie.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 1 ? d.getValue()[1] : ""));
        colClSconfitte.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 2 ? d.getValue()[2] : ""));
        colClPareggi.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 3 ? d.getValue()[3] : ""));
        colClTempo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().length > 4 ? d.getValue()[4] : ""));

        cercaStorico.textProperty().addListener((obs, o, n) -> filtraStorico(n));
        cercaClassifica.textProperty().addListener((obs, o, n) -> filtraClassifica(n));
    }

    /**
     * Inizializza il controller con la connessione e l'username autenticato.
     * Avvia il thread di ricezione messaggi e mostra il pannello partita.
     *
     * @param conn     connessione socket già aperta con il server
     * @param username nome utente autenticato
     */
    public void inizializza(ServerConnection conn, String username) {
        this.connection = conn;
        this.username   = username;
        labelUsername.setText(username);
        connection.setListener(this);
        connection.avviaRicezione();
        mostraPartita();
    }

    /** Mostra il pannello di selezione difficoltà e avvio partita. */
    @FXML private void mostraPartita() { mostra(panelPartita); evidenzia(btnPartita); }

    /**
     * Mostra il pannello storico e richiede i dati aggiornati al server.
     */
    @FXML private void mostraStorico() {
        mostra(panelStorico); evidenzia(btnStorico);
        connection.invia(new Messaggio(Tipo.STORICO_REQUEST));
    }

    /**
     * Mostra il pannello classifica e richiede i dati aggiornati al server.
     */
    @FXML private void mostraClassifica() {
        mostra(panelClassifica); evidenzia(btnClassifica);
        connection.invia(new Messaggio(Tipo.CLASSIFICA_REQUEST));
    }

    /**
     * Rende visibile solo il pannello indicato, nascondendo gli altri.
     *
     * @param pannello pannello da mostrare
     */
    private void mostra(VBox pannello) {
        VBox[] tutti = { panelPartita, panelStorico, panelClassifica, panelGioco };
        for (VBox p : tutti) { p.setVisible(p == pannello); p.setManaged(p == pannello); }
    }

    /**
     * Evidenzia il pulsante di navigazione attivo nella barra superiore.
     *
     * @param attivo pulsante corrispondente al pannello corrente
     */
    private void evidenzia(Button attivo) {
        Button[] btns = { btnPartita, btnStorico, btnClassifica };
        for (Button b : btns) {
            b.setStyle(b == attivo
                ? "-fx-font-weight: bold; -fx-text-fill: white; -fx-underline: true;"
                : "-fx-font-weight: normal; -fx-text-fill: white;");
        }
    }

    /**
     * Invia al server la richiesta di avvio partita con la difficoltà selezionata.
     * Difficoltà: 1=Facile, 2=Medio, 3=Difficile.
     */
    @FXML
    private void avviaPartita() {
        int diff = rbDifficile.isSelected() ? 3 : rbMedio.isSelected() ? 2 : 1;
        connection.invia(new Messaggio(Tipo.AVVIA_PARTITA, String.valueOf(diff)));
        statoPartitaLabel.setText("In attesa di un avversario...");
    }

    /**
     * Invia la risposta digitata dal giocatore al server.
     * La risposta viene convertita in maiuscolo prima dell'invio.
     */
    @FXML
    private void inviaRisposta() {
        String r = inputRisposta.getText().trim().toUpperCase();
        if (!r.isEmpty()) { connection.invia(new Messaggio(Tipo.RISPOSTA, r)); inputRisposta.clear(); }
    }

    /** Torna al pannello partita per iniziare una nuova sessione. */
    @FXML
    private void giocaAncora() { fermaTimer(); mostraPartita(); statoPartitaLabel.setText(""); }

    /**
     * Prepara e mostra il pannello di gioco con la sfida ricevuta.
     *
     * @param testoSfida testo con la parola cifrata evidenziata
     * @param secondi    secondi disponibili per rispondere
     */
    private void mostraGioco(String testoSfida, int secondi) {
        mostra(panelGioco);
        labelTestoSfida.setText(testoSfida);
        labelEsito.setText("");
        inputRisposta.clear();
        inputRisposta.setDisable(false);
        btnOk.setDisable(false);
        btnGiocaAncora.setVisible(false);
        avviaTimer(secondi);
    }

    /**
     * Termina la partita mostrando il risultato finale.
     * Disabilita l'input e mostra il pulsante "Gioca ancora".
     *
     * @param messaggio testo da mostrare (vittoria, sconfitta o pareggio)
     */
    private void finePartita(String messaggio) {
        fermaTimer();
        labelEsito.setText(messaggio);
        inputRisposta.setDisable(true);
        btnOk.setDisable(true);
        btnGiocaAncora.setVisible(true);
    }

    /**
     * Avvia il countdown visivo con un {@link Timeline} JavaFX.
     *
     * @param secondi numero di secondi del countdown
     */
    private void avviaTimer(int secondi) {
        fermaTimer();
        secondiRimasti = secondi;
        aggiornaLabelTimer();
        timerLine = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondiRimasti--;
            aggiornaLabelTimer();
            if (secondiRimasti <= 0) fermaTimer();
        }));
        timerLine.setCycleCount(secondi);
        timerLine.play();
    }

    /** Ferma il timer se attivo. */
    private void fermaTimer() { if (timerLine != null) timerLine.stop(); }

    /** Aggiorna la label del timer nel formato {@code MM:SS}. */
    private void aggiornaLabelTimer() {
        labelTimer.setText(String.format("%02d:%02d", secondiRimasti / 60, secondiRimasti % 60));
    }

    /**
     * Aggiorna la lista storico dal payload ricevuto dal server.
     *
     * @param dati stringa {@code data;avversario;risultato|...}
     */
    private void aggiornaStorico(String dati) {
        tuttoStorico.clear();
        if (dati != null && !dati.trim().isEmpty()) {
            for (String riga : dati.split("~")) 
                if (!riga.trim().isEmpty()) tuttoStorico.add(riga.split(";", -1));
        }
        mostraRigheStorico(tuttoStorico);
    }

    /**
     * Filtra lo storico per avversario (ricerca per prefisso, case-insensitive).
     *
     * @param testo testo digitato nel campo di ricerca
     */
    private void filtraStorico(String testo) {
        if (testo == null || testo.isEmpty()) { mostraRigheStorico(tuttoStorico); return; }
        String lower = testo.toLowerCase();
        List<String[]> f = new ArrayList<String[]>();
        for (String[] r : tuttoStorico) if (r.length > 1 && r[1].toLowerCase().startsWith(lower)) f.add(r);
        mostraRigheStorico(f);
    }

    private void mostraRigheStorico(List<String[]> lista) {
        ObservableList<String[]> items = FXCollections.observableArrayList(lista);
        storicoTable.setItems(items);
    }

    /**
     * Aggiorna la classifica dal payload ricevuto dal server.
     *
     * @param dati stringa {@code username;vittorie;sconfitte;pareggi;tempoMedio|...}
     */
    private void aggiornaClassifica(String dati) {
        tuttaClassifica.clear();
        if (dati != null && !dati.trim().isEmpty()) {
           for (String riga : dati.split("~")) {
                if (!riga.trim().isEmpty()) tuttaClassifica.add(riga.split(";", -1));
        }
        mostraRigheClassifica(tuttaClassifica);
    }
    }

    /**
     * Filtra la classifica per username (ricerca per prefisso, case-insensitive).
     *
     * @param testo testo digitato nel campo di ricerca
     */
    private void filtraClassifica(String testo) {
        if (testo == null || testo.isEmpty()) { mostraRigheClassifica(tuttaClassifica); return; }
        String lower = testo.toLowerCase();
        List<String[]> f = new ArrayList<String[]>();
        for (String[] r : tuttaClassifica) if (r.length > 0 && r[0].toLowerCase().startsWith(lower)) f.add(r);
        mostraRigheClassifica(f);
    }

    private void mostraRigheClassifica(List<String[]> lista) {
        classificaTable.setItems(FXCollections.observableArrayList(lista));
    }

    /**
     * Riceve e gestisce i messaggi inviati dal server.
     * Questo metodo viene chiamato sul thread JavaFX da {@link ServerConnection}.
     *
     * @param msg messaggio ricevuto dal server
     */
    @Override
    public void onMessage(Messaggio msg) {
        switch (msg.getTipo()) {
            case ATTESA:          statoPartitaLabel.setText("In attesa di un avversario..."); break;
            case SFIDA:
                int sec = 60;
                try { sec = Integer.parseInt(msg.getParam(2)); } catch (Exception ignored) {}
                mostraGioco(msg.getParam(0), sec);
                break;
            case RISPOSTA_ERRATA: labelEsito.setText("Risposta errata, riprova!"); break;
            case RISULTATO_VINTO: finePartita("Hai vinto! Tempo: " + msg.getParam(1) + " ms"); break;
            case RISULTATO_PERSO: finePartita("Hai perso. Ha vinto: " + msg.getParam(0)); break;
            case PAREGGIO:        finePartita("Pareggio — tempo scaduto."); break;
            case STORICO_RESPONSE:    aggiornaStorico(msg.getParam(0)); break;
            case CLASSIFICA_RESPONSE: aggiornaClassifica(msg.getParam(0)); break;
            case ERRORE: statoPartitaLabel.setText("Errore: " + msg.getParam(0)); break;
            default: break;
        }
    }
}