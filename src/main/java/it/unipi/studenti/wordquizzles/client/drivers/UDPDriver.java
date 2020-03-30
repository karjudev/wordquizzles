package it.unipi.studenti.wordquizzles.client.drivers;

import java.io.IOException;

import it.unipi.studenti.wordquizzles.client.clients.UDPClient;

/**
 * UDPDriver è la classe che si occupa di inviare dati al server tramite UDP.
 */
public class UDPDriver implements AutoCloseable {

    // Client che si occupa di comunicare con il server tramite UDP
    private UDPClient client;

    /**
     * Inizializza il driver.
     * 
     * @param client Client che comunica tramite UDP con il server.
     */
    public UDPDriver(UDPClient client) {
        this.client = client;
    }

    /**
     * Risponde ad un invito inviato da un utente.
     * 
     * @param username Nome dell'utente che ha inviato l'invito.
     * @throws IOException Se non è possibile scrivere sul soket.
     */
    public void acceptInvitation(String username) throws IOException {
        client.send(username);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}