package it.unipi.studenti.wordquizzles.server.wqp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import com.google.gson.Gson;

import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;
import it.unipi.studenti.wordquizzles.shared.StaticUtilities;

/**
 * DispatcherTask è la classe che si occupa di distinguere il tipo di messaggio e di eseguire la computazione.
 */
public class DispatcherTask implements Callable<WriteInformations> {

    // Oggetto che esegue le operazioni per il client
    private WorkerTask task;

    // Oggetto che serializza i dati in JSON
    private Gson gson;

    // Buffer contenente i dati letti dal client
    private ByteBuffer buffer;

    // Eventuale chiave dell'amico sfidato
    private SelectionKey friendKey;

    /**
     * Crea un nuovo dispatcher.
     * @param buffer Buffer contenente i dati letti dal client
     * @param key Chiave del client
     */
    public DispatcherTask(ByteBuffer buffer, SelectionKey key) {
        this.buffer = buffer;
        this.task = new WorkerTask(key);
        this.gson = new Gson();
        friendKey = null;
    }
    
    /**
     * Legge la stringa, interpreta il comando ed esegue l'azione associata.
     * @param command       Comando invocato dal client.
     * @param request       Stringa contenente la richiesta da parte del client.
     * @param tokenizer     Tokenizer che divide la stringa in parole.
     * @return              Stringa restituita dopo l'elaborazione da parte del server.
     * @throws WQPException Se l'elaborazione non è andata a buon fine.
     */
    private String routeRequest(String command, String request, StringTokenizer tokenizer) throws WQPException {
        // Stringa da scrivere al client sul socket
        String response = "";
        try {
            // Interpreta il comando
            if (command.equals("LOGIN")) {
                // Username dell'utente
                String username = tokenizer.nextToken();
                // Password dell'utente
                String password = tokenizer.nextToken();
                task.doLogin(username, password);
                response = username;
            }
            else if (command.equals("LOGOUT")) {
                // Comunica al server di rimuovere la chiave associata a questo username
                response = task.doLogout();
            }
            else if (command.equals("FRIEND")) {
                // Username dell'utente da aggiungere come amico
                String friendUsername = tokenizer.nextToken();
                task.doFriendRequest(friendUsername);
            }
            else if (command.equals("FRIENDSLIST")) {
                List<String> list = task.doFriendsList();
                response = gson.toJson(list);
            }
            else if (command.equals("RANKING")) {
                Map<String, Integer> rank = task.doRankingRequest();
                response = gson.toJson(rank);
            }
            else if (command.equals("POINTS")) {
                int points = task.doPoints();
                response = String.format("%d", points);
            }
            else if (command.equals("CHALLENGE")) {
                // Username dell'utente da sfidare
                String friendUsername = tokenizer.nextToken();
                // Chiave dello sfidato se questo ha accettato o meno la sfida
                friendKey = task.doChallengeRequest(friendUsername);
                response = (friendKey != null) ? "ACCEPTED" : "REFUSED";
            }
            else
                throw new WQPException("Command not recognized");
        // Se l'estrazione di un token non va a buon fine lancia una eccezione
        } catch (NoSuchElementException e) {
            throw new WQPException("Malformed request");
        }
        return response;
    }

    /**
     * @return Dati da scrivere al client.
     */
    @Override
    public WriteInformations call() {
        // Stringa inviata dal client
        String request = StaticUtilities.stringifyBuffer(buffer);
        System.out.printf("[DISPATCHER] Received:\n%s\n", request);
        // Tokenizer sulla stringa
        StringTokenizer tokenizer = new StringTokenizer(request);
        // Comando inviato dal client
        String command = tokenizer.nextToken();
        // Esito dell'operazione
        String outcome;
        // Messaggio ricevuto
        String message;
        try {
            // Legge la richiesta ed esegue il metodo necessario
            message = routeRequest(command, request, tokenizer);
            // Costruisce la risposta
            outcome = "OK";
        } catch (WQPException e) {
            // In caso di eccezione costruisce la risposta usando i parametri dell'eccezione
            message = e.getMessage();
            outcome = "KO";
        }
        // Costruisce la risposta da inviare al client
        String response = StaticUtilities.concat("\n", command, outcome, message);
        System.out.printf("[DISPATCHER] Sending response:\n%s\n", response);
        // Buffer che contiene la stringa e la dimensione
        ByteBuffer buffer = StaticUtilities.bufferizeString(response);
        // Restituisce tutte le informazioni necessarie al server
        return new WriteInformations(buffer, friendKey);
    }
}