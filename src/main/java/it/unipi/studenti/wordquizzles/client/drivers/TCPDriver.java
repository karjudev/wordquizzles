package it.unipi.studenti.wordquizzles.client.drivers;

import java.io.IOException;

import it.unipi.studenti.wordquizzles.client.clients.TCPClient;
import it.unipi.studenti.wordquizzles.shared.StaticUtilities;

/**
 * TCPDriver è la classe che si occupa di inviare comandi al server via TCP.
 */
public class TCPDriver implements AutoCloseable {

    // Client che scrive stringhe al server
    private TCPClient client;

    /**
     * Inizializza il driver passandogli il client.
     * 
     * @param client Client che comunica con il server.
     */
    public TCPDriver(TCPClient client) {
        this.client = client;
    }

    /**
     * Costruisce la stringa da inviare assemblando gli argomenti.
     * 
     * @param args    Argomenti che compongono il comando.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    private void sendCommand(String... args) throws IOException {
        // Costruisce la stringa
        String message = StaticUtilities.concat("\n", args);
        // Invia la stringa sul socket
        client.send(message);
    }

    /**
     * Invia una richiesta di sfida al server.
     * 
     * @param username Username dell'utente.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void challengeRequest(String username) throws IOException {
        sendCommand("CHALLENGE", username);
    }

    /**
     * Invia una richiesta della lista degli amici al server.
     * 
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void friendsListRequest() throws IOException {
        sendCommand("FRIENDSLIST");
    }

    /**
     * Invia una richiesta di amicizia al server.
     * 
     * @param username Nome dell'utente da aggiungere.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void friendRequest(String username) throws IOException {
        sendCommand("FRIEND", username);
    }
    
    /**
     * Invia una richiesta di classifica al server.
     * 
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void rankingRequest() throws IOException {
        sendCommand("RANKING");
    }

    /**
     * Invia una richiesta di punti al server.
     * 
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void pointsRequest() throws IOException {
        sendCommand("POINTS");
    }

    /**
     * Invia una traduzione al server.
     * 
     * @param word Parola da tradurre.
     * @param submission Traduzione inviata dal client al server.
     * @throws IOException Se non è possibile scrivere sul sockte.
     */
    public void submit(String word, String submission) throws IOException {
        sendCommand("WORD", word, submission);
	}

    @Override
    public void close() throws IOException {
        client.close();
    }

    /**
     * Invia una richiesta di login al server.
     * 
     * @param username Nome dell'utente da identificare.
     * @param password Password dell'utente associata allo username.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void loginRequest(String username, String password) throws IOException {
        sendCommand("LOGIN", username, password);
	}

    /**
     * Invia una richiesta di logout al server.
     * 
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    public void logoutRequest() throws IOException {
        sendCommand("LOGOUT");
	}
}