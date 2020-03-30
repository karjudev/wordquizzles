package it.unipi.studenti.wordquizzles.server.wqp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import it.unipi.studenti.wordquizzles.server.challenge.ChallengeServer;
import it.unipi.studenti.wordquizzles.shared.StaticUtilities;

/**
 * SocketServer è il server che tramite un selettore gestisce le nuove
 * connessioni TCP.
 */
public class SocketServer implements AutoCloseable {

    // Selettore che legge i socket
    private Selector selector;

    // Server socket su cui accettare connessioni
    private ServerSocketChannel channel;

    // Thread pool a cui vengono assegnati i task
    private ThreadPoolExecutor threadPool;

    /**
     * Istanzia il selettore
     * 
     * @param portNumber
     * @throws IOException
     */
    public SocketServer(int portNumber) throws IOException {
        // Thread pool che esegue le computazioni
        threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        // Selettore che legge le connessioni
        selector = Selector.open();
        // Server socket su cui accettare le connessioni
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress("localhost", portNumber));
        // Registra il canale per essere letto tramite il selettore
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Passa la computazione al thread pool e restituisce un attachment che dovrà
     * essere scritto
     * 
     * @param buffer Buffer contenente.
     * @param key    Chiave che identifica il client.
     * @return Future che rappresenta i dati da scrivere e le operazioni da eseguire
     *         prossimamente
     */
    private Future<WriteInformations> elaborateRequest(ByteBuffer buffer, SelectionKey key) {
        Future<WriteInformations> future = threadPool.submit(new DispatcherTask(buffer, key));
        return future;
    }

    /**
     * Alloca il buffer per leggere un intero e lo allega alla chiave per la lettura
     * 
     * @param key Chiave del client appena connesso
     * @throws IOException Se non riesce ad accettare il nuovo client
     */
    private void registerClient(SelectionKey key) throws IOException {
        // Buffer atto a contenere la dimensione del messaggio
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        // Socket del server
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        // Socket del client appena connesso
        SocketChannel clientChannel = serverChannel.accept();
        // Configura il client come non bloccante
        clientChannel.configureBlocking(false);
        // Registra il canale per la lettura
        SelectionKey readKey = clientChannel.register(selector, SelectionKey.OP_READ);
        // Allega il buffer alla chiave
        readKey.attach(lengthBuffer);

        System.out.printf("[SERVER] %s just connected to server\n", clientChannel.getRemoteAddress());
    }

    /**
     * Legge il messaggio dell'utente
     * 
     * @param key Chiave relativa al client
     * @throws IOException
     */
    private void readMessage(SelectionKey key) throws IOException {
        // Socket del client
        SocketChannel client = (SocketChannel) key.channel();
        // Buffer associato al client
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        // Legge i dati
        int bytesRead = client.read(buffer);
        System.out.printf("[SERVER] Read %d bytes from %s\n", bytesRead, client.getRemoteAddress());
        // Se ha letto -1 significa che la connessione è stata chiusa
        if (bytesRead == -1) {
            StaticUtilities.removeKey(key);
            return;
        }
        // Se ha altro da leggere esce (tornerà quando potrà leggere di nuovo)
        if (buffer.hasRemaining()) {
            System.out.println("[SERVER] Read not finished");
            return;
        }
        // Mette il buffer in modalità di lettura
        buffer.flip();
        // Se ha appena letto la dimensione rialloca l'array e lo allega
        if (buffer.array().length == Integer.BYTES) {
            // Lunghezza del messaggio
            int size = buffer.getInt();
            System.out.printf("[SERVER] Waiting for a message %d bytes long\n", size);
            // Buffer che deve contenere il messaggio
            buffer = ByteBuffer.allocate(size);
            key.attach(buffer);
        }
        // Altrimenti legge il messaggio e registra il canale per la scrittura
        else {
            Future<WriteInformations> writeBuffer = elaborateRequest(buffer, key);
            key.interestOps(SelectionKey.OP_WRITE);
            key.attach(writeBuffer);
            System.out.printf("[SERVER] Just read message from %s\n", client.getRemoteAddress());
        }
    }

    /**
     * Attende che il messaggio sia pronto e poi lo scrive a blocchi
     * 
     * @param key Chiave che rappresenta il client pronto per la scrittura
     * @throws IOException          Se c'è un problema a scrivere il messaggio
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void writeMessage(SelectionKey key) throws IOException, InterruptedException, ExecutionException {
        // Oggetto che rappresenta i dati da scrivere (non è possibile evitare l'unchecked warning)
        @SuppressWarnings("unchecked")
        Future<WriteInformations> future = (Future<WriteInformations>) key.attachment();
        // Se i dati non sono ancora stati elaborati non fa niente
        if (!future.isDone())
            return;
        // Client a cui scrivere
        SocketChannel client = (SocketChannel) key.channel();
        // Informazioni contenute nel client
        WriteInformations informations = future.get();
        // Buffer contenente la risposta da inviare al client
        ByteBuffer response = informations.getBuffer();
        // Scrive sul canale
        client.write(response);
        // Se non ha ancora finito esce (rientrerà nella funzione per scrivere ancora)
        if (response.hasRemaining())
            return;
        System.out.printf("[SERVER] Just sent message to %s\n", client.getRemoteAddress());
        // Eventuale chiave dello sfidante
        SelectionKey friendKey = informations.getFriendKey();
        boolean successifullyRegistered = false;
        // Se la chiave è non nulla registra i due client per la sfida
        if (friendKey != null)
            successifullyRegistered = registerChallenge(key, friendKey);
        // Se la registrazione ha avuto successo ha terminato
        if (successifullyRegistered)
            return;
        // Altrimenti registra di nuovo il client per la lettura
        key.interestOps(SelectionKey.OP_READ);
        // Buffer necessario a leggere la dimensione del messaggio
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        // Usa il buffer per la lettura
        key.attach(lengthBuffer);
    }

    /**
     * Registra i due client per una sfida.
     * 
     * @param key       Chiave dell'utente sfidante.
     * @param friendKey Chiave dell'utente sfidato.
     * 
     * @return Flag che indica se la registrazione è andata a buon fine.
     */
    private boolean registerChallenge(SelectionKey key, SelectionKey friendKey) {
        // Istanzia una nuova challenge
        ChallengeServer challengeServer;
        try {
            challengeServer = new ChallengeServer(key, friendKey, selector);
            // Deregistra le chiavi dal selettore principale
            key.interestOps(0);
            friendKey.interestOps(0);
            System.out.println("[SERVER] Deregistered keys from the main selector");
            // Invia la challenge al threadpool per l'esecuzione
            threadPool.execute(challengeServer);
        } catch (IOException e) {
            System.err.println("[SERVER] Challenge will not start - unable to retrieve translations");
            return false;
        }
        return true;
    }

    /**
     * Entra in un loop in cui accetta le connessioni, legge e scrive i dati.
     * 
     * @throws IOException          Se c'è un errore nella lettura di una chiave.
     * @throws ExecutionException   Se non è possibile eseguire il thread della
     *                              partita.
     * @throws InterruptedException Se il thread principale viene interrotto.
     */
    public void start() throws IOException, InterruptedException, ExecutionException {
        System.out.printf("[SERVER] TCP Server started on local address %s\n", channel.getLocalAddress());
        while (!Thread.interrupted()) {
            // Seleziona le chiavi attive
            selector.select();
            // Iteratore sulle chiavi
            Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
            while (keysIterator.hasNext()) {
                // Chiave corrente che viene rimossa dal selected set
                SelectionKey key = keysIterator.next();
                keysIterator.remove();
                // Se la chiave non è valida esce
                if (!key.isValid())
                    return;
                // Quando il client si connette lo registra per la lettura
                if (key.isAcceptable())
                    registerClient(key);
                // Quando il client è pronto a leggere legge il messaggio
                else if (key.isReadable())
                    readMessage(key);
                // Quando il client è pronto a scrivere scrive il messaggio
                else if (key.isWritable())
                    writeMessage(key);
            }
        }
        System.out.println("[SERVER] Server stopped, goodbye.");
    }

    /**
     * In caso di eccezione chiude il selettore e il server socket.
     */
    @Override
    public void close() throws IOException {
        selector.close();
        channel.close();
    }

    
}