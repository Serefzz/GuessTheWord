package com.mycompany.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
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
     * Calcola la Term Frequency (TF) di tutte le parole nelle frasi del file indicato.
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
        return Arrays.stream(testo.split("\\W+ "))
                .map(String::toUpperCase)
                .map(DocumentAnalyzer::rimuoviAccenti)
                .filter(w -> w.matches("[A-Z]+"))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
    }

    /**
     * Estrae le frasi dal file indicato (separate da {@code . ! ?}).
     *
     * @param percorsoFile percorso assoluto del file di testo da analizzare
     * @return lista di frasi estratte dal documento
     * @throws IOException se il file non esiste o non è leggibile
     */
    public List<String> estraiFrasi(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");

        String[] suddivisione = testo.split("[.!?]+");
        List<String> frasi = new ArrayList<>();
        for (String s : suddivisione) {
            frasi.add(s);
        }
        
        return frasi;
    }

    /**
     * Sostituisce le lettere accentate in una parola o frase.
     *
     * @param daTogliere Stringa che rappresenta la parola o la frase da cui togliere la/le lettera/e accentata/e
     * @return La parola o frase normalizzata senza lettere accentate
     */
    private static String rimuoviAccenti(String daTogliere) {
        // Decompone i caratteri accentati (es. 'È' -> 'E' + accento)
        String normalizzato = Normalizer.normalize(daTogliere, Normalizer.Form.NFD);
        // Rimuove tutti i segni aggiunti dalla normalizzazione (accenti/combinanti)
        return normalizzato.replaceAll("\\p{M}", "");
    }

}
