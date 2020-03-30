package it.unipi.studenti.wordquizzles.client.clients;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import it.unipi.studenti.wordquizzles.shared.StaticUtilities;

/**
 * TCPClient si occupa di inviare e ricevere stringhe via TCP.
 */
public class TCPClient implements Client {

    // Canale di comunicazione con il server
    private SocketChannel channel;

    /**
     * Inizializza il client per connettersi all'indirizzo passato.
     * 
     * @param address Indirizzo del server.
     * @throws IOException Se non è possibile aprire il canale.
     */
    public TCPClient(SocketAddress address) throws IOException {
        channel = SocketChannel.open();
        channel.connect(address);
        System.out.printf("[TCP] Connected to %s\n", address);
    }

    /**
     * Riceve un buffer data la lunghezza.
     * 
     * @param length Lunghezza del buffer da ricevere.
     * @return Buffer pronto per la lettura
     * @throws IOException Se non è possibile leggere dal socket.
     */
    private ByteBuffer receiveBuffer(int length) throws IOException {
        // Buffer della lunghezza prefissata
        ByteBuffer buffer = ByteBuffer.allocate(length);
        // Legge il buffer
        while (buffer.hasRemaining())
            channel.read(buffer);
        // Mette il buffer in modo che sia pronto per la lettura
        buffer.flip();
        return buffer;
    }

    @Override
    public String receive() throws IOException {
        // Buffer contenente la lunghezza
        ByteBuffer lengthBuffer = receiveBuffer(Integer.BYTES);
        // Lunghezza dei dati in arrivo
        int length = lengthBuffer.getInt();
        // Buffer contenente i dati
        ByteBuffer buffer = receiveBuffer(length);
        // Stringa estratta dal buffer
        String message = new String(buffer.array(), StandardCharsets.UTF_8);
        System.out.printf("[TCP] Received string:\n%s\n", message);
        return message;
    }

    @Override
    public void send(String message) throws IOException {
        // Buffer contenente lunghezza e messaggio
        ByteBuffer buffer = StaticUtilities.bufferizeString(message);
        // Invia il buffer
        while (buffer.hasRemaining())
            channel.write(buffer);
        System.out.printf("[TCP] Sent string: %s\n", message);
    }

    /**
     * Restituisce l'indirizzo locale con cui il client comunica con il server.
     * 
     * @return Indirizzo locale con cui il client comunica con il server.
     * @throws IOException Se non è possibile reperire l'indirizzo.
     */
    public SocketAddress getLocalAddress() throws IOException {
        return channel.getLocalAddress();
    }
    
    @Override
    public void close() throws IOException {
        channel.close();
        System.out.println("[TCP] Client closed");
    }
}