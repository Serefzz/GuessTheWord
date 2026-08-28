package com.mycompany.client;

import com.mycompany.common.Messaggio;

/**
 * Callback chiamata dal thread di ricezione quando arriva un messaggio dal server.
 * L'implementazione (MainController) viene invocata su Platform.runLater().
 */
public interface MessageListener {

    /**
     * Chiamato quando il server invia un messaggio al client.
     *
     * @param msg messaggio ricevuto dal server
     */
    void onMessage(Messaggio msg);
}