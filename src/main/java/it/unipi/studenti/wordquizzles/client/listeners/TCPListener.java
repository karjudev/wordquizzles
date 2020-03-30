package it.unipi.studenti.wordquizzles.client.listeners;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import it.unipi.studenti.wordquizzles.client.clients.TCPClient;
import it.unipi.studenti.wordquizzles.client.gui.App;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * TCPListener è il thread che si occupa di attendere stringhe tramite TCP e
 * notificare l'applicazione.
 */
public class TCPListener extends Thread {

    // Client che comunica tramite TCP
    private TCPClient client;

    // Applicazione da notificare
    private App app;

    // Oggetto che deserializza i JSON arrivati dal socket
    private Gson gson;

    // Tipo necessario a deserializzare liste di stringhe
    private Type stringListType;

    // Tipo necessario a deserializzare dizionari JSON Stringa - Intero
    private Type stringIntegerMapType;

    /**
     * Inizializza il thread passandogli i parametri necessari.
     * 
     * @param client Client che comunica con il server via TCP.
     * @param app    Applicazione grafica a cui notificare i cambiamenti.
     */
    public TCPListener(TCPClient client, App app) {
        this.client = client;
        this.app = app;
        gson = new Gson();
        stringListType = new TypeToken<ArrayList<String>>(){}.getType();
        stringIntegerMapType = new TypeToken<HashMap<String, Integer>>(){}.getType();
    }

    @Override
    public void run() {
        // Loop di attesa
        try {
            System.out.println("[TCP] Listener started");
            while (!Thread.interrupted()) {
                // Messaggio arrivato dal server
                String message = client.receive();
                // Elabora il messaggio
                try {
                    parse(message);
                } catch (WQPException e) {
                    app.showError(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("[TCP] Unable to communicate with socket, exiting: %s\n", e.getMessage());
            app.close();
        }
    }

    /**
     * Elabora la stringa passata.
     * 
     * @param message Messaggio inviato dal server.
     * @throws WQPException Se la risposta contiente un errore o non è ben formata.
     */
    private void parse(String message) throws WQPException {
        // Tokenizer che divide la stringa in token
        StringTokenizer tokenizer = new StringTokenizer(message, "\n");
        try {
            // Comando che ha originato la risposta ricevuta
            String command = tokenizer.nextToken();
            // Esito dell'operazione
            String outcome = tokenizer.nextToken();
            // Dati allegati alla risposta (parte rimanente della stringa)
            String data = tokenizer.nextToken("").trim();
            // Se l'esito è negativo analizza cosa deve fare e lancia l'eccezione
            if (outcome.equals("KO")) {
                // Valuta come gestire l'errore
                handleError(command);
                // Lancia una eccezione da mostrare a video
                throw new WQPException(data);
            }
            // Se l'esito è positivo elabora le operazioni
            else if (outcome.equals("OK"))
                handleResponse(command, data);
            // Altrimenti considera l'eccezione come risposta malformata
            else
                throw new NoSuchElementException();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            // Se non trova l'elemento che cerca lancia una eccezione
            String errorMessage = String.format("Malformed response:\n%s\n", message);
            throw new WQPException(errorMessage);
        }
    }

    /**
     * Elabora una stringa tokenizzata.
     * 
     * @param command Comando inviato dal server.
     * @param data Dati allegati alla richiesta.
     */
    private void handleResponse(String command, String data) {
        // Se il comando è di login deve fare il setup di tutta l'interfaccia
        if (command.equals("LOGIN")) {
            String username = data;
            app.setup(username);
        }
        // Se il comando è di logout chiude l'applicazione e questo thread
        else if (command.equals("LOGOUT")) {
            app.close();
            this.interrupt();
        }
        // Se il comando è una richiesta di amicizia aggiorna la lista degli amici aggiungendo quello appena associato e richiede di scaricare di nuovo la classifica
        else if (command.equals("FRIEND")) {
            String friendUsername = data;
            app.addFriend(friendUsername);
        }
        // Se il comando è la lista degli amici imposta la lista nell'interfaccia
        else if (command.equals("FRIENDSLIST")) {
            String friendsString = data;
            List<String> friends = gson.fromJson(friendsString, stringListType);
            app.setFriends(friends);
        }
        // Se il comando è la classifica imposta la classifica nell'applicazione
        else if (command.equals("RANKING")) {
            String rankingString = data;
            Map<String, Integer> ranking = gson.fromJson(rankingString, stringIntegerMapType);
            app.setRanking(ranking);
        }
        // Se il comando è una richiesta di punti imposta i punti nella grafica
        else if (command.equals("POINTS")) {
            String pointsString = data;
            int points = Integer.parseInt(pointsString);
            app.setPoints(points);
        }
        // Se il comando era una richiesta di sfida deve essere gestita
        else if (command.equals("CHALLENGE")) {
            String outcome = data;
            // Se la richiesta è stata rifiutata visualizza un messaggio
            if (outcome.equals("REFUSED"))
                app.challengeRefused();
            // Altrimenti gestisce la challenge
            else if (outcome.equals("ACCEPTED"))
                app.setupChallenge();
        }
        // Se è stata inviata una parola la mostra a video
        else if (command.equals("WORD")) {
            String word = data;
            app.setWord(word);
        }
        // Se è stato inviato un report che termina la partita lo mostra e disabilita i controlli
        else if (command.equals("REPORT")) {
            String report = data;
            app.showReport(report);
        }
    }

    /**
     * Gestisce la presenza di un errore nell'applicazione differenziando a seconda
     * del comando arrivato.
     * 
     * @param command Comando che ha generato l'errore.
     */
    private void handleError(String command) {
        // Se il login non è andato a buon fine chiude la finestra
        if (command.equals("LOGIN"))
            app.close();
        // Se la richiesta di challenge non è andata a buon fine disabilita la finestra
        if (command.equals("CHALLENGE"))
            app.stopChallenge();
    }
}