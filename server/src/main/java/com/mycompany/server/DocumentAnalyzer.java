package com.mycompany.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analizza documenti di testo per estrarre statistiche di frequenza delle parole
 * e suddividere il testo in frasi utilizzabili come sfide di gioco.
 *
 * Utilizza Stream API Java 8 per il calcolo della Term Frequency (TF).
 */
public class DocumentAnalyzer {

    /**
     * Calcola la Term Frequency (TF) di tutte le parole nel file indicato.
     * Le parole vengono normalizzate in maiuscolo e filtrate: solo sequenze
     * di lettere alfabetiche vengono considerate.
     *
     * @param percorsoFile percorso assoluto del file di testo da analizzare
     * @return mappa {@code parola -> conteggio} con le frequenze di ogni parola
     * @throws IOException se il file non esiste o non è leggibile
     */
    public Map<String, Long> analizza(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");
        return Arrays.stream(testo.split("\\W+"))
            .map(String::toUpperCase)
            .filter(w -> w.matches("[A-Z]+"))
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
    }

    /**
     * Estrae le frasi dal file indicato.
     * Se il testo contiene almeno 5 frasi (separate da {@code . ! ?}),
     * usa quella suddivisione; altrimenti suddivide per righe.
     *
     * @param percorsoFile percorso assoluto del file di testo da analizzare
     * @return lista di frasi estratte dal documento
     * @throws IOException se il file non esiste o non è leggibile
     */
    public List<String> estraiFrasi(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");

        String[] frammenti = testo.split("[.!?]+");
        if (frammenti.length >= 5) {
            return Arrays.asList(frammenti);
        }
        return Arrays.asList(testo.split("\\n+"));
    }
}
