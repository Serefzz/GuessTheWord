package com.mycompany.server;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia generica DAO (Data Access Object) versione con identificatore testuale.
 *
 * Definisce le operazioni CRUD standard che ogni DAO del sistema
 * deve implementare. Il tipo generico {@code T} rappresenta il modello
 * gestito dalla specifica implementazione.
 *
 * Implementazioni presenti:
 * 
 *   {@link UtenteDAO} — gestisce la tabella {@code Utente}
 * 
 * @param <T> il tipo di entità gestita dal DAO
 */
public interface DAOusername<T> {

    /**
     * Restituisce tutti i record presenti nella tabella.
     *
     * @return lista (eventualmente vuota) di tutti i record
     */
    List<T> selectAll();

    /**
     * Cerca un record tramite il suo identificatore numerico.
     *
     * @param username identificatore univoco del record
     * @return {@link Optional} contenente il record se trovato, vuoto altrimenti
     */
    Optional<T> selectByUsername(String username);

    /**
     * Inserisce un nuovo record nel database.
     *
     * @param t entità da inserire
     * @return {@code true} se l'inserimento è riuscito, {@code false} altrimenti
     */
    boolean insert(T t);

    /**
     * Aggiorna un record esistente nel database.
     *
     * @param t entità aggiornata (deve contenere l'id del record da modificare)
     * @return {@code true} se la modifica è riuscita, {@code false} altrimenti
     */
    boolean update(T t);

    /**
     * Elimina il record con l'identificatore specificato.
     *
     * @param username identificatore univoco del record da eliminare
     * @return {@code true} se la cancellazione è riuscita, {@code false} altrimenti
     */
    boolean delete(String username);
    
}
