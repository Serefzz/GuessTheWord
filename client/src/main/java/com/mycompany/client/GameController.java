package com.mycompany.client;

import com.mycompany.common.Messaggio;
import com.mycompany.common.Messaggio.Tipo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Controller principale del client.
 * FXML associato: {@code /game.fxml}
 */
public class GameController implements MessageListener {

    // Navbar
    @FXML private Label labelUsername;
    @FXML private Label topTitleLabel;

    // Pannelli dello StackPane
    @FXML private VBox panelMenu;
    @FXML private VBox panelPartita;
    @FXML private VBox panelGioco;
    @FXML private VBox panelStorico;
    @FXML private VBox panelClassifica;

    // Elementi del Menù Videogioco
    @FXML private Label titoloMenuLabel;

    // Elementi Pannello Partita / Matchmaking
    @FXML private RadioButton rbFacile;
    @FXML private RadioButton rbMedio;
    @FXML private RadioButton rbDifficile;
    @FXML private Label statoPartitaLabel;
    @FXML private ProgressIndicator attesaIndicator;

    // Elementi Pannello Gioco
    @FXML private TextFlow textFlowSfida;
    @FXML private Label labelTimer;
    @FXML private Label labelEsito;
    @FXML private TextField inputRisposta;
    @FXML private Button btnOk;
    @FXML private Button btnGiocaAncora;
    @FXML private Button btnForfait;
    @FXML private Button btnTornaMenuFine;

    // Tabelle e Ricerca Storico
    @FXML private TableView<String[]> storicoTable;
    @FXML private TableColumn<String[], String> colStData;
    @FXML private TableColumn<String[], String> colStAvversario;
    @FXML private TableColumn<String[], String> colStRisultato;
    @FXML private TextField cercaStorico;

    // Tabelle e Ricerca Classifica
    @FXML private TableView<String[]> classificaTable;
    @FXML private TableColumn<String[], String> colClPunteggio;
    @FXML private TableColumn<String[], String> colClUsername;
    @FXML private TableColumn<String[], String> colClVittorie;
    @FXML private TableColumn<String[], String> colClSconfitte;
    @FXML private TableColumn<String[], String> colClPareggi;
    @FXML private TableColumn<String[], String> colClTempo;
    @FXML private TextField cercaClassifica;

    private ServerConnection connection;
    private String username;
    private Timeline timerLine;
    private int secondiRimasti;

    private final List<String[]> tuttoStorico = new ArrayList<>();
    private final List<String[]> tuttaClassifica = new ArrayList<>();

    @FXML
    public void initialize() {
        Font fontPersonalizzatoMenu = Font.loadFont(getClass().getResourceAsStream("/fonts/ZagDropsThin.ttf"), 60);
        titoloMenuLabel.setFont(fontPersonalizzatoMenu);
        Font fontPersonalizzatoSchede = Font.loadFont(getClass().getResourceAsStream("/fonts/ZagDropsThin.ttf"), 38);
        topTitleLabel.setFont(fontPersonalizzatoSchede);

        // Radio button della difficoltà
        ToggleGroup tg = new ToggleGroup();
        rbFacile.setToggleGroup(tg);
        rbMedio.setToggleGroup(tg);
        rbDifficile.setToggleGroup(tg);
        rbFacile.setSelected(true);

        // Colonne Storico
        colStData.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colStAvversario.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStRisultato.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));

        // Colonne Classifica
        colClPunteggio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colClUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colClVittorie.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colClSconfitte.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colClPareggi.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        colClTempo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[5]));

        // Ricerca 
        cercaStorico.textProperty().addListener((obs, o, n) -> filtraStorico(n));
        cercaClassifica.textProperty().addListener((obs, o, n) -> filtraClassifica(n));

        // Schermata iniziale: Menù Principale
        tornaAlMenu();
    }

    public void inizializza(ServerConnection conn, String username) {
        this.connection = conn;
        this.username = username;
        labelUsername.setText(this.username);
        connection.setListener(this);
        connection.avviaRicezione();
        tornaAlMenu();
    }

    // ------------------------------------------------------------------
    //  Navigazione tra i pannelli
    // ------------------------------------------------------------------

    private void mostra(VBox pannelloAttivo) {
        topTitleLabel.setVisible(true);
        VBox[] tutti = { panelMenu, panelPartita, panelGioco, panelStorico, panelClassifica };
        for (VBox p : tutti) {
            boolean visibile = (p == pannelloAttivo);
            p.setVisible(visibile);
            p.setManaged(visibile);
        }
    }

    @FXML
    public void tornaAlMenu() {
        fermaTimer();
        if (attesaIndicator != null) attesaIndicator.setVisible(false);
        if (statoPartitaLabel != null) statoPartitaLabel.setText("");
        mostra(panelMenu);
        topTitleLabel.setVisible(false);
    }

    @FXML
    public void apriSceltaDifficolta() {
        statoPartitaLabel.setText("");
        attesaIndicator.setVisible(false);
        mostra(panelPartita);
    }

    @FXML
    public void mostraClassifica() {
        mostra(panelClassifica);
        if (connection != null) connection.invia(new Messaggio(Tipo.CLASSIFICA_REQUEST));
    }

    @FXML
    public void mostraStorico() {
        mostra(panelStorico);
        if (connection != null) connection.invia(new Messaggio(Tipo.STORICO_REQUEST));
    }

    @FXML
    public void esciDalGioco() {
        if (connection != null) {
            connection.invia(new Messaggio(Tipo.DISCONNETTI));
            connection.chiudi();
        }
        System.exit(0);
    }

    // ------------------------------------------------------------------
    //  Avvio Partita e Matchmaking
    // ------------------------------------------------------------------

    @FXML
    private void avviaPartita() {
        int diff = rbDifficile.isSelected() ? 3 : rbMedio.isSelected() ? 2 : 1;
        if (connection != null) {
            connection.invia(new Messaggio(Tipo.AVVIA_PARTITA, String.valueOf(diff)));
        }
        attesaIndicator.setVisible(true);
        statoPartitaLabel.setText("Richiesta partita inviata...");
    }
    
    private void mostraGioco(String testoSfida, String parolaCifrata, int secondi) {
        mostra(panelGioco);
        visualizzaTestoSfida(testoSfida, parolaCifrata);
        labelEsito.setText("");
        inputRisposta.clear();
        inputRisposta.setDisable(false);
        btnForfait.setDisable(false);
        btnOk.setDisable(false);
        btnGiocaAncora.setVisible(false);
        btnGiocaAncora.setManaged(false);
        btnTornaMenuFine.setVisible(false);
        btnTornaMenuFine.setManaged(false);
        avviaTimer(secondi);
    }
    
     private void visualizzaTestoSfida(String frase, String parolaCifrata) {
         textFlowSfida.getChildren().clear();
         if (frase == null || frase.isEmpty()) {
             return;
         }

         int indiceInizioParola = -1;
         if (parolaCifrata != null && !parolaCifrata.trim().isEmpty()) {
             // Cerchiamo la parola normalizzata usando i confini \b
             Pattern pattern = Pattern.compile("\\b" + parolaCifrata + "\\b", Pattern.UNICODE_CHARACTER_CLASS);
             Matcher matcher = pattern.matcher(frase);
             if (matcher.find()) {
                 indiceInizioParola = matcher.start(); // Trova l'indice corretto
             }
         }

         Font fontBase = Font.font("System", FontWeight.NORMAL, 14);
         Font fontEvidenziato = Font.font("System", FontWeight.BOLD, 14);
         Color coloreBase = Color.web("#074338");
         Color coloreEvidenziato = Color.web("#f5ab2a");

         if (indiceInizioParola == -1) {
             Text t = new Text(frase);
             t.setFont(fontBase);
             t.setFill(coloreBase);
             textFlowSfida.getChildren().add(t);
             return;
         }

         // Segmento prima della parola (preservando gli accenti originali della frase)
         if (indiceInizioParola > 0) {
             Text tPrima = new Text(frase.substring(0, indiceInizioParola));
             tPrima.setFont(fontBase);
             tPrima.setFill(coloreBase);
             textFlowSfida.getChildren().add(tPrima);
         }

         // Parola colorata
         Text tParola = new Text(frase.substring(indiceInizioParola, indiceInizioParola + parolaCifrata.length()));
         tParola.setFont(fontEvidenziato);
         tParola.setFill(coloreEvidenziato);
         textFlowSfida.getChildren().add(tParola);

         // Segmento dopo la parola
         if (indiceInizioParola + parolaCifrata.length() < frase.length()) {
             Text tDopo = new Text(frase.substring(indiceInizioParola + parolaCifrata.length()));
             tDopo.setFont(fontBase);
             tDopo.setFill(coloreBase);
             textFlowSfida.getChildren().add(tDopo);
         }
    }

    // ------------------------------------------------------------------
    //  Partita in corso: Risposta e Forfait
    // ------------------------------------------------------------------

    @FXML
    private void inviaRisposta() {
        String r = inputRisposta.getText().trim().toUpperCase();
        if (!r.isEmpty()) {
            if (connection != null) {
                connection.invia(new Messaggio(Tipo.RISPOSTA, r));
            }
            inputRisposta.clear();
        }
    }

    @FXML
    private void abbandonaPartita() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Forfait");
        alert.setHeaderText("Abbandonare la partita?");
        alert.setContentText("Sei sicuro di voler dichiarare forfait? La partita verrà considerata persa.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                connection.invia(new Messaggio(Tipo.FORFAIT));
                
            }
        });
    }

    @FXML
    private void giocaAncora() {
        fermaTimer();
        apriSceltaDifficolta();
    }

    private void finePartita(String messaggio) {
        fermaTimer();
        labelEsito.setText(messaggio);
        inputRisposta.setDisable(true);
        btnForfait.setDisable(true);
        btnOk.setDisable(true);
        btnGiocaAncora.setVisible(true);
        btnGiocaAncora.setManaged(true);
        btnTornaMenuFine.setVisible(true);
        btnTornaMenuFine.setManaged(true);
    }

    // ------------------------------------------------------------------
    //  Timer
    // ------------------------------------------------------------------

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

    private void fermaTimer() {
        if (timerLine != null) {
            timerLine.stop();
            secondiRimasti = 0;
            aggiornaLabelTimer();
        }
    }

    private void aggiornaLabelTimer() {
        labelTimer.setText(String.format("%02d:%02d", secondiRimasti / 60, secondiRimasti % 60));
    }

    // ------------------------------------------------------------------
    //  Dati Storico e Classifica
    // ------------------------------------------------------------------

    private void aggiornaStorico(String dati) {
        tuttoStorico.clear();
        if (dati != null && !dati.trim().isEmpty()) {
            for (String riga : dati.split("~")) {
                if (!riga.trim().isEmpty()) tuttoStorico.add(riga.split(";", -1));
            }
        }
        mostraRigheStorico(tuttoStorico);
    }

    private void filtraStorico(String testo) {
        if (testo == null || testo.isEmpty()) {
            mostraRigheStorico(tuttoStorico);
            return;
        }
        String lower = testo.toLowerCase();
        List<String[]> f = new ArrayList<>();
        for (String[] r : tuttoStorico) {
            if (r.length > 1 && r[1].toLowerCase().contains(lower)) f.add(r);
        }
        mostraRigheStorico(f);
    }

    private void mostraRigheStorico(List<String[]> lista) {
        storicoTable.setItems(FXCollections.observableArrayList(lista));
    }

    private void aggiornaClassifica(String dati) {
        tuttaClassifica.clear();
        if (dati != null && !dati.trim().isEmpty()) {
            for (String riga : dati.split("~")) {
                if (!riga.trim().isEmpty()) tuttaClassifica.add(riga.split(";", -1));
            }
        }
        mostraRigheClassifica(tuttaClassifica);
    }

    private void filtraClassifica(String testo) {
        if (testo == null || testo.isEmpty()) {
            mostraRigheClassifica(tuttaClassifica);
            return;
        }
        String lower = testo.toLowerCase();
        List<String[]> f = new ArrayList<>();
        for (String[] r : tuttaClassifica) {
            if (r.length > 0 && r[0].toLowerCase().contains(lower)) f.add(r);
        }
        mostraRigheClassifica(f);
    }

    private void mostraRigheClassifica(List<String[]> lista) {
        classificaTable.setItems(FXCollections.observableArrayList(lista));
    }

    // ------------------------------------------------------------------
    //  MessageListener (Callback da ServerConnection)
    // ------------------------------------------------------------------

    @Override
    public void onMessage(Messaggio msg) {
        Platform.runLater(() -> {
            switch (msg.getTipo()) {
                case ATTESA:
                    if (attesaIndicator != null) {
                        attesaIndicator.setVisible(true);
                    }
                    statoPartitaLabel.setText(msg.getParam(0));
                    break;
                case SFIDA:
                    int sec = 60;
                    try {
                        sec = Integer.parseInt(msg.getParam(2));
                    } catch (Exception ignored) {
                    }
                    mostraGioco(msg.getParam(0), msg.getParam(1),  sec);
                    break;
                case RISPOSTA_ERRATA:
                    labelEsito.setText("Risposta errata, riprova!");
                    break;
                case RISULTATO_VINTO:
                    finePartita("Hai vinto! Hai battuto " + msg.getParam(0) + " in: " + msg.getParam(1) + " ms. La parola originale era \"" + msg.getParam(2) + "\"");
                    break;
                case RISULTATO_PERSO:
                    finePartita("Hai perso... Ha vinto: " + msg.getParam(0) + ". La parola originale era \"" + msg.getParam(2) + "\"");
                    break;
                case PAREGGIO:
                    finePartita("Pareggio — Tempo scaduto. La parola originale era \"" + msg.getParam(0) + "\"");
                    break;
                case STORICO_RESPONSE:
                    aggiornaStorico(msg.getParam(0));
                    break;
                case CLASSIFICA_RESPONSE:
                    aggiornaClassifica(msg.getParam(0));
                    break;
                case ERRORE:
                    statoPartitaLabel.setText("Errore: " + msg.getParam(0));
                    break;
                default:
                    break;
            }
        });
    }
    
}
