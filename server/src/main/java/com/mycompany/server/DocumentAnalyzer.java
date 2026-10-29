package com.mycompany.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.Normalizer;

/**
 * Analizza documenti di testo per estrarre statistiche di frequenza delle parole
 * e suddividere il testo in frasi utilizzabili come sfide di gioco.
 *
 * Utilizza Stream API per il calcolo della Term Frequency (TF).
 */
public class DocumentAnalyzer {

    /**
     * Calcola la Term Frequency (TF) di tutte le parole nella frase indicata.
     * Le parole vengono normalizzate in maiuscolo e filtrate: solo sequenze
     * di lettere alfabetiche vengono considerate.
     *
     * @param frase frase da analizzare
     * @return AnalisiFrase contenente la frase e una mappa con le frequenze di ogni parola al suo interno
    */
    public AnalisiFrase analizza(String frase) throws IOException {
    String fraseNormalizzata = rimuoviAccenti(frase.toUpperCase());

    // Estrae e filtra la lista delle parole valide
    List<String> parole = Arrays.stream(fraseNormalizzata.split("[\\W']+"))
            .filter(w -> w.matches("[A-Z]+"))
            .collect(Collectors.toList());

    long totaleParole = parole.size();
    Map<String, Long> tfFrase = new HashMap<>();

    if (totaleParole > 0) {
        // Raggruppa per parola e calcola la percentuale arrotondata (da 0 a 100)
        tfFrase = parole.stream().collect(Collectors.groupingBy(
                        w -> w,
                        Collectors.collectingAndThen(Collectors.counting(),
                                conteggio -> Math.round(((double) conteggio / totaleParole) * 100.0)
                        )
                ));
    }

    return new AnalisiFrase(frase, tfFrase);
}

    /**
     * Estrae le frasi dal file indicato (separate da {@code [.!?\\r\\n]+}).
     *
     * @param percorsoFile percorso assoluto del file di testo da analizzare
     * @return lista di frasi estratte dal documento
     * @throws IOException se il file non esiste o non è leggibile
     */
    public List<String> estraiFrasi(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");

        String[] suddivisione = testo.split("[.!?\\r\\n]+");
        List<String> frasi = new ArrayList<>();
        for (String s : suddivisione) {
            String pulita = s.trim();
            if (!pulita.isEmpty()) {
                frasi.add(pulita);
            }
        }
        
        return frasi;
    }

    /**
     * Sostituisce le lettere accentate in una parola o frase.
     *
     * @param daTogliere Stringa che rappresenta la parola o la frase da cui togliere la/le lettera/e accentata/e
     * @return La parola o frase normalizzata senza lettere accentate
     */
    public static String rimuoviAccenti(String daTogliere) {
        if (daTogliere == null) {
            return null;
        }

        String risultato = daTogliere.replaceAll("['’]", " ");

        risultato = Normalizer.normalize(risultato, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        return risultato;
    }
    
}