package it.unipi.studenti.wordquizzles.server.challenge;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import it.unipi.studenti.wordquizzles.server.services.AccountService;
import it.unipi.studenti.wordquizzles.server.services.WordService;
import it.unipi.studenti.wordquizzles.shared.Configuration;
import it.unipi.studenti.wordquizzles.shared.StaticUtilities;
import it.unipi.studenti.wordquizzles.shared.wqp.WQPException;

/**
 * ChallengeTask
 */
public class ChallengeServer implements Runnable {

    // Sistema di gestione degli account
    private AccountService accountService;

    // Chiave dell'utente sfidante
    private SelectionKey userKey;

    // Chiave dell'utente sfidato
    private SelectionKey friendKey;

    // Riferimento al selettore principale
    private Selector mainSelector;

    // Mappa che associa ad ogni utente la struttura dati per la partita
    private Map<SelectionKey, MatchInformations> info;

    // Mappa che associa ad ogni parola la traduzione
    private Map<String, String> translations;

    // Lista delle parole da tradurre (per semplicità di consultazione)
    private List<String> words;

    // Flag che indica se c'è stata una disconnessione improvvisa
    private boolean disconnected;

    /**
     * Inizializza i dati
     * 
     * @param userKey      Chiave dello sfidante.
     * @param friendKey    Chiave dello sfidato.
     * @param mainSelector Selettore su cui registrare di nuovo i
     * @throws IOException Se non è possibile reperire le traduzioni.
     */
    public ChallengeServer(SelectionKey userKey, SelectionKey friendKey, Selector mainSelector) throws IOException {
        this.userKey = userKey;
        this.friendKey = friendKey;
        this.mainSelector = mainSelector;
        disconnected = false;
        info = new HashMap<>(2);
        translations = WordService.getWords();
        words = new ArrayList<>(translations.keySet());
        accountService = AccountService.getInstance();
    }

    /**
     * Scrive i dati allegati alla chiave sul canale. Quando ha finito si mette in
     * attesa dei dati da leggere.
     * 
     * @param key Chiave contenente il canale su cui scrivere e i dati da scrivere.
     * @throws IOException Se non è possibile scrivere sul socket.
     */
    private void writeKey(SelectionKey key) throws IOException {
        // Socket su cui scrivere
        SocketChannel channel = (SocketChannel) key.channel();
        // Buffer da cui prendere i dati da scrivere
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        // Scrive i dati sul canale
        channel.write(buffer);
        // Se ci sono altri dati da scrivere esce
        if (buffer.hasRemaining())
            return;
        System.out.printf("[CHALLENGE] Just wrote message %d bytes long to %s\n", buffer.capacity(), channel.getRemoteAddress());
        // Buffer che contiene la dimensione del prossimo dato da leggere
        buffer = ByteBuffer.allocate(Integer.BYTES);
        // Registra il canale per la prossima lettura
        key.interestOps(SelectionKey.OP_READ);
        key.attach(buffer);
        System.out.printf("[CHALLENGE] %s is back to read\n", channel.getRemoteAddress());
    }

    /**
     * Legge prima la dimensione poi i dati dal canale. Quando ha finito li
     * processa, prende il buffer da scrivere e si registra per la scrittura.
     * 
     * @param key Chiave contenente il canale da cui leggere e il buffer su cui
     *            salvare lunghezza e dati.
     * @throws IOException Se non è possibile leggere dal buffer.
     * @return Flag che indica se il client ha terminato.
     */
    private boolean readKey(SelectionKey key) throws IOException {
        // Socket su cui scrivere
        SocketChannel channel = (SocketChannel) key.channel();
        // Buffer in cui inserire i dati letti
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        // Legge i dati sul canale
        int bytesRead = channel.read(buffer);
        // Se ha letto -1 bytes il client ha chiuso quindi cancella la chiave
        if (bytesRead == -1) {
            StaticUtilities.removeKey(key);
            // Rimuove la chiave dal selettore principale
            if (key.channel() == userKey.channel())
                userKey.cancel();
            else
                this.friendKey.cancel();
            disconnected = true;
            return true;
        }
        // Se c'è ancora spazio nel buffer esce
        if (buffer.hasRemaining())
            return false;
        buffer.flip();
        System.out.printf("[CHALLENGE] Just read %d bytes from %s\n", buffer.capacity(), channel.getRemoteAddress());
        // Se il buffer contiene una lunghezza la estrae
        if (buffer.capacity() == Integer.BYTES) {
            int length = buffer.getInt();
            buffer = ByteBuffer.allocate(length);
            key.attach(buffer);
            System.out.printf("[CHALLENGE] Waiting for a message %d bytes long from %s\n", length, channel.getRemoteAddress());
        } else {
            // Processa il buffer restituendo il prossimo buffer da scrivere
            buffer = parse(buffer, key);
            // Se il buffer è null l'utente ha terminato
            if (buffer == null) {
                key.cancel();
                System.out.printf("[CHALLENGE] Client %s terminated the challenge\n", channel.getRemoteAddress());
                return true;
            }
            key.attach(buffer);
            key.interestOps(SelectionKey.OP_WRITE);
            System.out.printf("[CHALLENGE] Client %s is back to write\n", channel.getRemoteAddress());
        }
        return false;
    }

    /**
     * Elabora il buffer inviato da un utente e restituisce l'eventuale successivo buffer da scrivere.
     * 
     * @param buffer Buffer inviato dal client.
     * @param key Chiave che identifica il client.
     * @return Buffer da scrivere al client, oppure null in caso di ultima parola o richiesta di logout.
     */
    private ByteBuffer parse(ByteBuffer buffer, SelectionKey key) {
        // Stringa arrivata dal client
        String message = StaticUtilities.stringifyBuffer(buffer);
        // Tokenizer che analizza il messaggio
        StringTokenizer tokenizer = new StringTokenizer(message);
        // Comando inviato dal client
        String command = tokenizer.nextToken();
        // Risultato dell'operazione richiesta dal client
        String outcome = null;
        // Dati contenuti nella risposta
        String response = null;
        try {
            // Se il comando è l'invio di una parola la estrae
            if (command.equals("WORD")) {
                String word = tokenizer.nextToken();
                String submission = tokenizer.nextToken();
                response = submit(word, submission, key);
                // Se ho finito le parole restituisce null
                if (response == null)
                    return null;
            }
            // Se il comando è di logout lo esegue
            else if (command.equals("LOGOUT")) {
                // Chiave dell'utente nel selettore principale
                SelectionKey mainKey = info.get(key).getKey();
                response = StaticUtilities.logoutUser(mainKey);
            }
            // Altrimenti non è possibile gestire il comando
            else
                throw new WQPException("Unable to execute command");
            outcome = "OK";
        // Se la suddivisione del messaggio in token non è stata fatta nel modo corretto la risposta non è ben formata
        } catch (NoSuchElementException e) {
            outcome = "KO";
            response = "Malformed message";
        } catch (WQPException e) {
            outcome = "KO";
            response = e.getMessage();
        }
        // Compone la risposta da inviare al client
        message = StaticUtilities.concat("\n", command, outcome, response);
        // Buffer da inviare al client
        buffer = StaticUtilities.bufferizeString(message);
        return buffer;
    }

    /**
     * Inserisce una nuova parola nella lista delle sottomissioni dell'utente.
     * 
     * @param word Parola da tradurre.
     * @param submission Traduzione inviata dall'utente.
     * @param key Chiave che rappresenta la connessione dell'utente.
     * @return Prossima parola da inviare all'utente, oppure null se le parole sono esaurite.
     */
    private String submit(String word, String submission, SelectionKey key) {
        // Informazioni sulla partita dell'utente
        MatchInformations userInfo = info.get(key);
        // Traduzione corretta
        String translation = translations.get(word);
        // Controlla che la parola sia corretta
        boolean correct = submission.toLowerCase().equals(translation);
        // Indice della prossima parola da sottomettere
        int nextIndex = -1;
        if (correct)
            nextIndex = userInfo.correct();
        else
            nextIndex = userInfo.incorrect();
        // Se l'indice è -1 ha esaurito le parole
        if (nextIndex == -1)
            return null;
        // Prossima parola da mandare all'utente
        String nextWord = words.get(nextIndex);
        return nextWord;
    }

    @Override
    public void run() {
        // Socket dell'utente sfidante
        SocketChannel challengerChannel = (SocketChannel) userKey.channel();
        // Socket dell'utente sfidato
        SocketChannel challengedChannel = (SocketChannel) friendKey.channel();
        // Selettore specifico della partita
        try (Selector selector = Selector.open()) {
            // Username degli sfidanti
            String userName = accountService.getUsername(userKey);
            String friendName = accountService.getUsername(friendKey);
            // Registra i due socket sul nuovo selettore
            SelectionKey challengerKey = challengerChannel.register(selector, SelectionKey.OP_WRITE);
            SelectionKey challengedKey = challengedChannel.register(selector, SelectionKey.OP_WRITE);
            System.out.println("[CHALLENGE] Registered clients in the challenge selector");
            // Crea le strutture dati per il client
            MatchInformations challengerInfo = new MatchInformations(userKey, userName);
            info.put(challengerKey, challengerInfo);
            MatchInformations challengedInfo = new MatchInformations(friendKey, friendName);
            info.put(challengedKey, challengedInfo);
            // Prende i primi buffer degli utenti
            ByteBuffer challengerBuffer = firstWord();
            ByteBuffer challengedBuffer = firstWord();
            // Allega i buffer alle chiavi
            challengerKey.attach(challengerBuffer);
            challengedKey.attach(challengedBuffer);
            // Numero di client terminati
            short terminated = (short) 0;
            // Tempo rimasto per la sfida
            long remainingTime = Configuration.CHALLENGE_TIME;
            // Loop di gestione delle connessioni
            while (!((remainingTime <= 0) || (terminated >= 2) || disconnected || Thread.interrupted())) {
                // Timestamp prima della selezione degli eventi
                long start = System.currentTimeMillis();
                // Selezione degli eventi
                selector.select(remainingTime);
                // Timestamp dopo la selezione degli eventi
                long end = System.currentTimeMillis();
                // Aggiorna il tempo rimanente
                remainingTime -= (end - start);
                System.out.printf("[CHALLENGE] Remaining time: %dms\n", remainingTime);
                // Iteratore sugli eventi correnti
                Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
                // Gestisce gli eventi
                terminated += handleEvents(keysIterator);
            }
            // Report da inviare all'utente
            String reportString;
            // Se c'è stata una interruzione manda un report
            if (disconnected)
                reportString = report("KO", "Un utente si è disconnesso, il match è stato cancellato");
            // Altrimenti assegna il bonus, registra i punti e costruisce il report dei punteggi
            else {
                assignBonus();
                registerPoints(challengerKey);
                registerPoints(challengedKey);
                reportString = report();
            }
            // Invia i report
            sendReport(reportString);
        } catch (IOException | WQPException e) {
            System.err.println("[CHALLENGE] Error handling the challenge, exiting");
        }
        // Allega un buffer per la lettura
        ByteBuffer userBuffer = ByteBuffer.allocate(Integer.BYTES);
        userKey.attach(userBuffer);
        ByteBuffer friendBuffer = ByteBuffer.allocate(Integer.BYTES);
        friendKey.attach(friendBuffer);
        // Registra le due chiavi per la prossima lettura sul socket principale
        if (userKey.isValid())
            userKey.interestOps(SelectionKey.OP_READ);
        if (friendKey.isValid())
            friendKey.interestOps(SelectionKey.OP_READ);
        System.out.println("[CHALLENGE] Registered clients on the main selector");
        // Risveglia il selettore principale
        mainSelector.wakeup();
        System.out.println("[CHALLENGE] Main selector waked up");
    }

    /**
     * Registra i punti dell'utente passato.
     * 
     * @param key Chiave che identifica l'utente per il match.
     * @throws WQPException Se non è possibile incrementare i punti.
     */
    private void registerPoints(SelectionKey key) throws WQPException {
        // Dati dell'utente per la partita
        MatchInformations userInfo = info.get(key);
        // Punti dell'utente
        int points = userInfo.getPoints();
        // Chiave dell'utente nel selettore principale
        SelectionKey mainKey = userInfo.getKey();
        // Incrementa i punti dell'utente
        accountService.incrementPoints(mainKey, points);
    }

    /**
     * Invia un report ad ogni sfidante, saltando quelli disconnessi.
     * 
     * @param reportString Stringa da inviare ai client.
     */
    private void sendReport(String reportString) {
        // Buffer che contiene la stringa
        ByteBuffer buffer = StaticUtilities.bufferizeString(reportString);
        // Scorre i due sfidanti
        for (SelectionKey key : info.keySet()) {
            SocketChannel channel = (SocketChannel) key.channel();
            SocketAddress address = null;
            try {
                address = channel.getRemoteAddress();
                while (buffer.hasRemaining())
                    channel.write(buffer);
            } catch (IOException e) {
                System.err.printf("[CHALLENGE] Failed sending report to %s\n", address);
            }
            buffer.flip();
        }
    }

    /**
     * Assegna il bonus all'utente vincitore.
     */
    private void assignBonus() {
        // Informazioni dell'utente con il massimo punteggio
        MatchInformations maxInfo = null;
        // Massimo punteggio dell'utente
        int max = 0;
        // Scorre tutti i punteggi
        for (MatchInformations userInfo : info.values())
            if (userInfo.getPoints() > max) {
                max = userInfo.getPoints();
                maxInfo = userInfo;
            }
        // Assegna il bonus all'utente con più punti (se esiste)
        if (maxInfo != null)
            maxInfo.bonus();
    }

    /**
     * Costruisce una stringa di report da inviare al client.
     * 
     * @param outcome Esito dell'operazione.
     * @param message Messaggio di errore.
     * @return Stringa da inviare al client.
     */
    private String report(String outcome, String message) {
        return StaticUtilities.concat("\n", "REPORT", outcome, message);
    }

    /**
     * Per ogni client genera un report e concatena la stringa.
     * 
     * @return Report della partita.
     */
    private String report() {
        // Concatenatore di stringhe
        StringBuilder builder = new StringBuilder();
        // Concatena i report di ogni utente
        for (SelectionKey key : info.keySet()) {
            // Informazioni dell'utente
            MatchInformations userInfo = info.get(key);
            // Genera il report per l'utente
            String report = userInfo.toString();
            // Concatena il report all'input
            builder.append(report);
            builder.append("\n");
        }
        return report("OK", builder.toString());
    }

    /**
     * Restituisce il buffer che contiene il primo comando contenente la parola da
     * inviare al server.
     * 
     * @return Buffer contenente la prima parola.
     */
    private ByteBuffer firstWord() {
        String firstWordString = StaticUtilities.concat("\n", "WORD", "OK", words.get(0));
        ByteBuffer buffer = StaticUtilities.bufferizeString(firstWordString);
        return buffer;
    }

    /**
     * Gestisce gli eventi selezionati.
     * 
     * @param keysIterator Iteratore sulle chiavi rappresentanti i client pronti.
     * @throws IOException Se non è possibile comunicare con i client.
     * @return Numero di client che hanno terminato.
     */
    private short handleEvents(Iterator<SelectionKey> keysIterator) throws IOException {
        // Numero di client che hanno terminato in questa iterazione
        short terminated = (short) 0;
        while (keysIterator.hasNext()) {
            SelectionKey key = keysIterator.next();
            // Rimuove la chiave dal set
            keysIterator.remove();
            // Se la chiave non è valida esce
            if (!key.isValid())
                continue;
            if (key.isWritable())
                writeKey(key);
            else if (key.isReadable()) {
                boolean clientTerminated = readKey(key);
                // Se il client ha terminato aumenta il totale
                if (clientTerminated)
                    terminated++;
            }
        }
        return terminated;
    }
    
}