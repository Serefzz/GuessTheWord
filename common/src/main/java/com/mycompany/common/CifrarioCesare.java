package com.mycompany.common;

/**
 * Implementazione del Cifrario di Cesare per lettere maiuscole.
 *
 * Ogni lettera viene spostata in avanti (cifratura) o indietro (decifratura)
 * di un numero fisso di posizioni nell'alfabeto. I caratteri non alfabetici
 * rimangono invariati.
 *
 * Questa classe non è istanziabile (costruttore privato).
 */
public class CifrarioCesare {

    /** Impedisce l'istanziazione della classe di utilità. */
    private CifrarioCesare() {}

    /**
     * Cifra il testo applicando lo shift indicato.
     * Il testo viene prima convertito in maiuscolo.
     *
     * @param testo testo in chiaro da cifrare
     * @param shift numero di posizioni di spostamento nell'alfabeto (modulo 26)
     * @return testo cifrato in lettere maiuscole
     */
    public static String cifra(String testo, int shift) {
        StringBuilder sb = new StringBuilder();
        int s = shift % 26;
        String upper = testo.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (Character.isLetter(c)) {
                sb.append((char) ('A' + (c - 'A' + s) % 26));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Decifra il testo applicando lo stesso shift usato per cifrarlo.
     *
     * @param testoCifrato testo cifrato da decifrare
     * @param shift        numero di posizioni usato in fase di cifratura
     * @return testo in chiaro in lettere maiuscole
     */
    public static String decifra(String testoCifrato, int shift) {
        return cifra(testoCifrato, 26 - (shift % 26));
    }
}