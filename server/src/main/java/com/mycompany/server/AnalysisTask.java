package com.mycompany.server;

import javafx.concurrent.Task;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
     *   Calcola la Term Frequency tramite {@link DocumentAnalyzer#analizza(String)}
     *   Estrae le frasi tramite {@link DocumentAnalyzer#estraiFrasi(String)}
     *   Accumula i risultati nelle strutture globali
     * 
     * Alla fine ordina le parole per frequenza e include le top 10 nel riepilogo.
     *
     * @return stringa di riepilogo con statistiche per file e top 10 parole più frequenti
     * @throws Exception se la lettura di uno dei file fallisce
     */
    @Override
    protected String call() throws Exception {
        DocumentAnalyzer analyzer = new DocumentAnalyzer();
        List<String>      tutteFrasi = new ArrayList<String>();
        Map<String, Long> tfTotale   = new HashMap<String, Long>();
        StringBuilder     riepilogo  = new StringBuilder();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            updateMessage("Analisi " + (i + 1) + "/" + files.size() + ": " + file.getName());
            updateProgress(i, files.size());

            Map<String, Long> tf = analyzer.analizza(file.getAbsolutePath());
            for (Map.Entry<String, Long> entry : tf.entrySet()) {
                Long attuale = tfTotale.get(entry.getKey());
                tfTotale.put(entry.getKey(),
                    attuale == null ? entry.getValue() : attuale + entry.getValue());
            }

            List<String> frasi = analyzer.estraiFrasi(file.getAbsolutePath());
            tutteFrasi.addAll(frasi);

            riepilogo.append(file.getName())
                     .append(": ").append(tf.size()).append(" parole distinte, ")
                     .append(frasi.size()).append(" frasi\n");
        }

        ClientHandler.setFrasi(tutteFrasi);
        ClientHandler.setTfMap(tfTotale);

        updateProgress(files.size(), files.size());
        updateMessage("Analisi completata.");

        riepilogo.append("\nTotale frasi: ").append(tutteFrasi.size());
        riepilogo.append("\nTotale parole distinte: ").append(tfTotale.size()).append("\n");

        riepilogo.append("\n--- Top 10 parole più frequenti ---\n");
        List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(tfTotale.entrySet());
        for (int i = 0; i < entries.size() - 1 && i < 10; i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                if (entries.get(j).getValue() > entries.get(i).getValue()) {
                    Map.Entry<String, Long> tmp = entries.get(i);
                    entries.set(i, entries.get(j));
                    entries.set(j, tmp);
                }
            }
        }
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            riepilogo.append(entries.get(i).getKey())
                     .append(": ").append(entries.get(i).getValue()).append("\n");
        }

        return riepilogo.toString();
    }
}
