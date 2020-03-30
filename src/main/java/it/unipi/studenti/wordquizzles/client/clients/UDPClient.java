package it.unipi.studenti.wordquizzles.client.clients;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

import it.unipi.studenti.wordquizzles.shared.Configuration;

/**
 * UDPClient si occupa di ricevere e inviare stringhe tramite UDP.
 */
public class UDPClient implements Client {

    // Canale che si occupa delle comunicazioni tramite UDP
    private DatagramChannel channel;

    // Indirizzo del server
    private SocketAddress serverAddress;

    // Buffer che riceve i datagrammi
    private ByteBuffer buffer;

    /**
     * Inizializza il client collegandolo ad un indirizzo locale.
     * @param address Indirizzo locale a cui collegarsi.
     * @throws IOException Se non è possibile aprire il canale.
     */
    public UDPClient(SocketAddress address) throws IOException {
        channel = DatagramChannel.open();
        channel.bind(address);
        // Buffer della dimensione massima delle stringhe UDP.
        buffer = ByteBuffer.allocate(Configuration.USERNAME_MAX_LENGTH);

        System.out.printf("[UDP] Binded to %s\n", address);
    }

    @Override
    public void send(String message) throws IOException {
        // Buffer estratto dalla stringa
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        // Invia il buffer sul canale al server
        channel.send(buffer, serverAddress);
    }

    @Override
    public String receive() throws IOException {
        // Riceve un datagramma registrando la provenienza
        serverAddress = channel.receive(buffer);
        // Elabora la stringa
        String message = new String(buffer.array(), StandardCharsets.UTF_8);
        System.out.printf("[UDP] Received string:\n%s\n", message);
        return message;
    }

    @Override
    public void close() throws IOException {
        channel.close();
        System.out.println("[UDP] Client closed");
    }
}