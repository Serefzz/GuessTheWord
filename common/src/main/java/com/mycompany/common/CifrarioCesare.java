package com.mycompany.common;

/**
 * Implementa il cifrario di Cesare.
 *
 * Ogni lettera viene sostituita con quella che si trova 'shift' posizioni
 * più avanti nell'alfabeto (con ritorno ciclico da Z ad A).
 * I caratteri non alfabetici (spazi, punteggiatura) restano invariati.
 *
 * Nota: la cifratura lavora su lettere MAIUSCOLE; le lettere minuscole
 * della parola originale vengono convertite prima dello shift.
 */
public class CifrarioCesare {

    private CifrarioCesare() {
        // classe di soli metodi statici — non istanziabile
    }

    /**
     * Cifra una stringa applicando lo shift dato.
     *
     * @param testo  la stringa da cifrare (può contenere spazi e punteggiatura)
     * @param shift  numero di posizioni da shiftare (1–25)
     * @return       la stringa cifrata (lettere in maiuscolo)
     */
    public static String cifra(String testo, int shift) {
        StringBuilder sb = new StringBuilder();
        int s = shift % 26; // normalizza lo shift in un range 0-25
        String testoMaiuscolo = testo.toUpperCase();
        for (int i = 0; i < testoMaiuscolo.length(); i++) {
            char c = testoMaiuscolo.charAt(i);
            if (Character.isLetter(c)) {
                // 1) c - 'A'        → numero della lettera (A=0, B=1, ..., Z=25)
                // 2) + s            → applica lo shift
                // 3) % 26           → riporta nel range 0-25 se sfora (es. Z+3 → 2)
                // 4) 'A' + ...      → riconverte il numero in lettera
                // 5) (char)         → cast da int a char
                int posizione = c - 'A';
                int posizioneSpostata = (posizione + s) % 26;
                char cifrata = (char) ('A' + posizioneSpostata);
                sb.append(cifrata);
            } else {
                sb.append(c); 
            }
        }
        return sb.toString();
    }

   
}