package com.mycompany.server;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia generica DAO (Data Access Object).
 * Ogni implementazione gestisce le operazioni CRUD per un tipo specifico.
 *
 
 *
 * @param <T> il tipo di entità gestita (es. Utente, RisultatoPartita)
 */
public interface DAO<T> {

    /** Restituisce tutti i record della tabella. */
    List<T> selectAll();

    /** Restituisce il record con l'id dato, o Optional.empty() se non esiste. */
    Optional<T> selectById(int id);

    /** Inserisce un nuovo record. Restituisce true se l'inserimento è riuscito. */
    boolean insert(T t);

    /** Aggiorna un record esistente. Restituisce true se la modifica è riuscita. */
    boolean update(T t);

    /** Elimina il record con l'id dato. Restituisce true se la cancellazione è riuscita. */
    boolean delete(int id);
}