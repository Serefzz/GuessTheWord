package com.mycompany.server;

import javafx.concurrent.Task;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Task JavaFX che analizza in background uno o più documenti di testo.
 *
 * Estende {@link Task}{@code <String>} per eseguire l'analisi su un thread
 * separato senza bloccare il thread JavaFX, aggiornando {@code messageProperty}
 * e {@code progressProperty} in tempo reale.
 *
 * Al termine dell'elaborazione imposta le frasi in
 * {@link ClientHandler#setFrasi(List)} per le sfide di gioco,
 * imposta la mappa TF in {@link ClientHandler#setTfMap(Map)} per la scelta
 * della parola, e restituisce una stringa di riepilogo mostrata nel pannello admin.
 *
 */
public class AnalysisTask extends Task<String> {

    private final List<File> files;

    /**
     * Costruisce il task con la lista di file da analizzare.
     *
     * @param files lista di file {@code .txt} da elaborare
     */
    public AnalysisTask(List<File> files) {
        this.files = files;
    }

    /**
     * Esegue l'analisi di tutti i file nel thread in background.
     *
     * Per ogni file:
     * 
     *   Estrae le frasi tramite {@link DocumentAnalyzer#estraiFrasi(String)}
     *   Calcola la Term Frequency di ogni frase tramite {@link DocumentAnalyzer#analizza(String)}
     *   Accumula i risultati nelle strutture globali
     * 
     * @return stringa di riepilogo con statistiche per file e top 10 parole più frequenti
     * @throws Exception se la lettura di uno dei file fallisce
     */
    @Override
    protected String call() throws Exception {
        List<List<AnalisiFrase>> tfFrasiDocumenti = new ArrayList<List<AnalisiFrase>>();
        DocumentAnalyzer analyzer = new DocumentAnalyzer();
        int numeroTotaleFrasi = 0;
        StringBuilder riepilogo  = new StringBuilder();

        for (int i = 0; i < files.size(); i++) {
            List<AnalisiFrase> tfFrasi = new ArrayList<AnalisiFrase>();
            List<String> tutteFrasiDelFile = new ArrayList<String>();
            File file = files.get(i);
            updateMessage("Analisi " + (i + 1) + "/" + files.size() + ": " + file.getName());
            updateProgress(i, files.size());
            
            List<String> frasi = analyzer.estraiFrasi(file.getAbsolutePath());
            tutteFrasiDelFile.addAll(frasi);
            numeroTotaleFrasi += tutteFrasiDelFile.size();

            int numeroFrase = 1;
            for(String frase : tutteFrasiDelFile){
                AnalisiFrase tf = analyzer.analizza(frase);
                tfFrasi.add(tf);
                riepilogo.append(file.getName() + " frase n." + numeroFrase).append(": ").append(tf.getTf().size()).append(" parole distinte, ").append(tutteFrasiDelFile.size()).append(" frasi\n");
                numeroFrase++;
            }
            tfFrasiDocumenti.add(tfFrasi);
        }
        
        ClientHandler.setTfDocumenti(tfFrasiDocumenti);
        
        updateProgress(files.size(), files.size());
        updateMessage("Analisi completata.");
        riepilogo.append("\nTotale frasi: ").append(numeroTotaleFrasi);

        return riepilogo.toString();
    }
    
}
