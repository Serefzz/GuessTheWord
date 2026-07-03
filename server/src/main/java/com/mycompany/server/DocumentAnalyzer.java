package com.mycompany.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analizza un documento di testo e produce:
 *  1. Una mappa TF  (parola → frequenza) — restituita da analizza()
 *  2. Una lista di frasi/estratti        — restituita da estraiFrasi()
 *
 * La mappa TF può essere salvata su disco tramite ObjectOutputStream
 * (Map<String,Long> implementa Serializable).
 *
 * Usa le Stream API di Java 8.
 */
public class DocumentAnalyzer {

    /** Lunghezza minima perché una parola sia considerata significativa. */
    private static final int MIN_LUNGHEZZA = 4;

    /** Numero massimo di frasi estratte dal documento (usate come sfide). */
    private static final int MAX_FRASI = 100;

    // ------------------------------------------------------------------
    //  Analisi TF
    // ------------------------------------------------------------------

    /**
     * Legge il documento e calcola la Term Frequency di ogni parola.
     *
     * Pipeline Stream:
     *   testo → split in token → toUpperCase → filtra lettere → filtra lunghezza
     *   → raggruppa per parola → conta occorrenze
     *
     * @param percorsoFile percorso del file .txt da analizzare
     * @return Map<String, Long> parola → numero di occorrenze, ordinata per frequenza
     * @throws IOException se il file non può essere letto
     */
    public Map<String, Long> analizza(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");

        // Stream API Java 8:
        // 1. split("\\W+")  → divide su caratteri non-lettera (spazi, punteggiatura...)
        // 2. map(toUpperCase) → tutto maiuscolo
        // 3. filter(solo lettere) → scarta token con numeri o simboli
        // 4. filter(lunghezza) → scarta parole troppo corte
        // 5. groupingBy + counting → costruisce la mappa parola → frequenza
        Map<String, Long> frequenze = Arrays.stream(testo.split("\\W+"))
            .map(String::toUpperCase)
            .filter(w -> w.matches("[A-Z]+"))
            .filter(w -> w.length() >= MIN_LUNGHEZZA)
            .collect(Collectors.groupingBy(
                w -> w,
                Collectors.counting()
            ));

        return frequenze;
    }

    // ------------------------------------------------------------------
    //  Estrazione frasi
    // ------------------------------------------------------------------

    /**
     * Estrae le frasi del documento per usarle come sfide di gioco.
     * Ogni frase è un estratto di testo che contiene almeno una parola
     * significativa (lunghezza >= MIN_LUNGHEZZA).
     *
     * @param percorsoFile percorso del file .txt
     * @return lista di frasi (massimo MAX_FRASI)
     * @throws IOException se il file non può essere letto
     */
    public List<String> estraiFrasi(String percorsoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(percorsoFile));
        String testo = new String(bytes, "UTF-8");

        // Divide il testo in frasi usando . ! ? come delimitatori
        String[] frammenti = testo.split("[.!?]+");

        List<String> frasi = new ArrayList<String>();
        for (int i = 0; i < frammenti.length; i++) {
            String frase = frammenti[i].trim();
            // Includi solo frasi abbastanza lunghe da contenere almeno una parola sfidante
            if (frase.length() >= MIN_LUNGHEZZA * 2) {
                frasi.add(frase);
                if (frasi.size() >= MAX_FRASI) break;
            }
        }

        // Se non ci sono abbastanza frasi, divide il testo in righe
        if (frasi.size() < 5) {
            frasi.clear();
            String[] righe = testo.split("\\n+");
            for (int i = 0; i < righe.length; i++) {
                String riga = righe[i].trim();
                if (riga.length() >= MIN_LUNGHEZZA * 2) {
                    frasi.add(riga);
                    if (frasi.size() >= MAX_FRASI) break;
                }
            }
        }

        return frasi;
    }
}