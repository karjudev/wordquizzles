package it.unipi.studenti.wordquizzles.server.wqp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * WriteInformations contiene il buffer da scrivere all'utente ed eventualmente il riferimento alla chiave dello sfidante da spostare in un thread dedicato.
 */
public class WriteInformations {

    // Buffer contenente le informazioni da scrivere
    private ByteBuffer buffer;

    // Chiave eventuale dello sfidante
    private SelectionKey friendKey;

    /**
     * Inizializza l'oggetto passando i parametri
     * @param buffer    Buffer da scrivere al client.
     * @param friendKey Chiave dell'amico
     */
    public WriteInformations(ByteBuffer buffer, SelectionKey friendKey) {
        this.buffer = buffer;
        this.friendKey = friendKey;
    }

    /**
     * @return Buffer con i dati da scrivere all'utente.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * @return Eventuale chiave della connessione dell'amico.
     */
    public SelectionKey getFriendKey() {
        return friendKey;
    }
}