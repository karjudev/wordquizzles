package it.unipi.studenti.wordquizzles.server.wqp;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import it.unipi.studenti.wordquizzles.server.services.AccountService;
import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.StaticUtilities;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * WorkerTask è la classe che esegue tutte le operazioni necessarie data una
 * Request a restituire una Response.
 */
public class WorkerTask {

    // Client client che ha inviato la richieste
    private SelectionKey key;

    // Gestore degli account
    private AccountService accountService;

    /**
     * Crea un nuovo worker.
     * 
     * @param key Chiave del client
     */
    public WorkerTask(SelectionKey key) {
        this.key = key;
        accountService = AccountService.getInstance();
    }

    /**
     * Esegue il login dell'utente.
     * 
     * @param username Username dell'utente
     * @param password Password associata
     * @throws WQPException Se non è possibile loggarsi con questo account.
     */
    public void doLogin(String username, String password) throws WQPException {
        accountService.login(username, password, key);
    }

    /**
     * Esegue il login dell'utente precedente loggato.
     * 
     * @return Username con cui l'utente era loggato nel sistema.
     * @throws WQPException Se non è possibile fare il logout con questo account.
     */
    public String doLogout() throws WQPException {
        return StaticUtilities.logoutUser(key);
    }

    /**
     * @return Lista di username degli amici dell'utente.
     * @throws WQPException Se non è possibile leggere la lista degli amici.
     */
    public List<String> doFriendsList() throws WQPException {
        return accountService.getFriendsUsernames(key);
    }

    /**
     * Aggiunge una amicizia tra l'utente corrente e quello dato.
     * 
     * @param friendUsername Nome dell'utente da aggiungere.
     * @throws WQPException Se non è possibile aggiungere l'utente come amico.
     */
    public void doFriendRequest(String friendUsername) throws WQPException {
        accountService.addFriend(key, friendUsername);
    }

    /**
     * @return Punti dell'utente corrente
     * @throws WQPException Se non è possibile reperire i punti dell'utente.
     */
    public int doPoints() throws WQPException {
        return accountService.getPoints(key);
    }

    /**
     * @return Classifica dei punteggi dell'utente corrente.
     * @throws WQPException Se non è possibile effettuare l'
     */
    public Map<String, Integer> doRankingRequest() throws WQPException {
        Map<String, Integer> ranking = accountService.getRanking(key);
        return ranking;
    }

    /**
     * Esegue una richiesta di sfida dall'utente corrente allo username dato.
     * 
     * @param friendUsername Nome dell'utente da sfidare.
     * @return Se l'utente ha accettato la sfida, la chiave che lo identifica. Altrimenti null.
     * @throws WQPException Se non è possibile sfidare l'utente.
     */
    public SelectionKey doChallengeRequest(String friendUsername) throws WQPException {
        // Chiave dell'amico da sfidare
        SelectionKey friendKey = accountService.getSelectionKey(friendUsername);
        // Invita l'utente amico
        boolean accepted = inviteUser(friendKey);
        // Se l'invito è stato accettato restituisce la chiave
        if (accepted)
            return friendKey;
        // Altrimenti restituisce null
        return null;
    }

    /**
     * Invita l'utente dalla chiave passata da parte dell'utente corrente.
     * 
     * @param friendKey Amico da sfidare
     * @return Flag che indica se l'invito è stato accettato.
     * @throws WQPException Se non è possibile inviare l'invito.
     */
    private boolean inviteUser(SelectionKey friendKey) throws WQPException {
        // Username dell'utente corrente
        String username = accountService.getUsername(key);
        // Username dell'utente da sfidare
        String friendUsername = accountService.getUsername(friendKey);
        // Controlla che gli username non siano uguali
        if (username.equals(friendUsername))
            throw new WQPException("You can't invite yourself");
        // Indirizzo dell'utente da sfidare
        SocketAddress friendAddress;
        try {
            friendAddress = ((SocketChannel) friendKey.channel()).getRemoteAddress();
        } catch (IOException e) {
            throw new WQPException("Unable to retrieve friend address");
        }
        try (DatagramSocket socket = new DatagramSocket()) {
            // Pacchetto che contiene lo username
            DatagramPacket packet = new DatagramPacket(username.getBytes(StandardCharsets.UTF_8), username.length(), friendAddress);
            // Invia lo username sul socket
            socket.send(packet);
            // Setta il timeout per la ricezione
            socket.setSoTimeout(Configuration.TIMEOUT);
            // Riceve il pacchetto
            socket.receive(packet);
            System.out.println("[WORKER] Invitation accepted");
            return true;
        // Se il pacchetto non arriva entro il timeout si considera che l'invito sia stato rifiutato
        } catch (SocketTimeoutException e) {
            System.out.println("[WORKER] Invitation refused");
            return false;
        } catch (IOException e) {
            throw new WQPException("Unable to send invitation");
        }
    }
}