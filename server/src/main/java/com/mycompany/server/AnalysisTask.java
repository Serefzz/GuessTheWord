package com.mycompany.server;

import javafx.concurrent.Task;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task JavaFX che analizza i documenti di testo in background.
 *
 * Al termine:
 *  - imposta le frasi disponibili in ClientHandler (usate come sfide di gioco)
 *  - restituisce una stringa con il riepilogo dell'analisi (per l'area risultati)
 *
 * Usa Task<String> per aggiornare l'UI senza bloccare il thread JavaFX.
 * (Pattern da slide JavaFX — Task<T> per operazioni lunghe)
 */
public class AnalysisTask extends Task<String> {

    private final List<File> files;

    public AnalysisTask(List<File> files) {
        this.files = files;
    }

    @Override
    protected String call() throws Exception {
        DocumentAnalyzer analyzer = new DocumentAnalyzer();
        List<String> tutteFrasi    = new ArrayList<String>();
        Map<String, Long> tfTotale = new HashMap<String, Long>();
        StringBuilder riepilogo    = new StringBuilder();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            updateMessage("Analisi " + (i + 1) + "/" + files.size() + ": " + file.getName());
            updateProgress(i, files.size());

            // 1. Term Frequency (Stream API)
            Map<String, Long> tf = analyzer.analizza(file.getAbsolutePath());
            for (Map.Entry<String, Long> entry : tf.entrySet()) {
                Long attuale = tfTotale.get(entry.getKey());
                if (attuale == null) {
                    tfTotale.put(entry.getKey(), entry.getValue());
                } else {
                    tfTotale.put(entry.getKey(), attuale + entry.getValue());
                }
            }

            // 2. Frasi per le sfide
            List<String> frasi = analyzer.estraiFrasi(file.getAbsolutePath());
            tutteFrasi.addAll(frasi);

            riepilogo.append(file.getName())
                     .append(": ").append(tf.size()).append(" parole distinte, ")
                     .append(frasi.size()).append(" frasi\n");
        }

        // Rende disponibili frasi e mappa TF per il gioco
        ClientHandler.setFrasi(tutteFrasi);
        ClientHandler.setTfMap(tfTotale);

        updateProgress(files.size(), files.size());
        updateMessage("Analisi completata.");

        riepilogo.append("\nTotale frasi: ").append(tutteFrasi.size());
        riepilogo.append("\nTotale parole distinte: ").append(tfTotale.size()).append("\n");

        // Top 10 parole più frequenti
        riepilogo.append("\n--- Top 10 parole più frequenti ---\n");
        List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(tfTotale.entrySet());
        // Ordina per frequenza decrescente (bubble sort — metodo base)
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