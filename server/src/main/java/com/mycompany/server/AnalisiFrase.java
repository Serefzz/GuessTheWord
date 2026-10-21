package com.mycompany.server;

import java.io.Serializable;
import java.util.Map;

/**
 * Modella l'analisi testuale di una singola frase estratta da un documento.
 *
 * <p>Contiene il testo della frase e la mappa delle relative frequenze delle parole (Term Frequency),
 * generalmente calcolate da {@link DocumentAnalyzer#analizza(String)}.</p>
 *
 * <p>Implementa {@link Serializable} per consentire la serializzazione delle analisi.</p>
 */
public class AnalisiFrase implements Serializable {
    
    private final String frase; /** Testo della frase estratta dal documento. */
    private final Map<String, Long> tf; /** Mappa delle frequenze (Term Frequency) per ciascuna parola valida della frase. */
    
    /**
     * Costruisce un'istanza di {@code AnalisiFrase} con la frase indicata e la relativa mappa TF.
     *
     * @param frase testo della frase estratta dal documento
     * @param tf    mappa contenente le parole della frase e la loro frequenza (Term Frequency)
     */
    public AnalisiFrase(String frase, Map<String, Long> tf) {
        this.frase = frase;
        this.tf = tf;
    }

    /**
     * Restituisce il testo della frase analizzata.
     *
     * @return testo della frase
     */
    public String getFrase() { 
        return frase;
    }

    /**
     * Restituisce la mappa delle frequenze delle parole (Term Frequency) della frase.
     *
     * @return mappa con le parole come chiavi e i relativi valori di frequenza percentuale
     */
    public Map<String, Long> getTf() {
        return tf;
    }
    
}

