package it.unipi.studenti.wordquizzles.client.clients;

import java.io.IOException;

/**
 * Client è l'interfaccia che rappresenta il generico client che interagisce con il server.
 */
public interface Client extends AutoCloseable {

    /**
     * Invia una stirnga al server.
     * 
     * @param message Stringa da inviare al server.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void send(String message) throws IOException;

    /**
     * Riceve una stringa dal server.
     * 
     * @return Stringa inviata dal server.
     * @throws IOException Se non è possibile leggere dal socket.
     */
    public String receive() throws IOException;

}